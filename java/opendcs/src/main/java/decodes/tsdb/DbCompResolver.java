/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.sql.DbKey;

/**
This class contains the code to look at an input data collection
and determine which computations to attempt.
*/
public class DbCompResolver
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The time series database we're using. */
	private TimeSeriesDb theDb;
	private LinkedList<PythonWritten> pythonWrittenQueue = new LinkedList<PythonWritten>();
	private static String module = "Resolver: ";

	/**
	 * Constructor, saves reference to tsdb for later use.
	 * @param theDb the time series database.
	 */
	public DbCompResolver( TimeSeriesDb theDb )
	{
		this.theDb = theDb;
	}

	/**
	 * Check data in collection against my list of computations and
	 * return all computations to be attempted.
	 * @param data the data collection.
	 */
	public DbComputation[] resolve( DataCollection data )
		throws DbIoException
	{
		trimPythonWrittenQueue();
		final ArrayList<DbComputation> results = new ArrayList<>();
		try (ComputationDAI txDAO = theDb.makeComputationDAO())
		{
			txDAO.inTransaction(dao ->
			{
				try (ComputationDAI computationDAO = theDb.makeComputationDAO();
					 CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();
					 TimeSeriesDAI tsDAI = theDb.makeTimeSeriesDAO();)
				{
					computationDAO.inTransactionOf(dao);
					compDependsDAO.inTransactionOf(dao);
					tsDAI.inTransactionOf(dao);
					for(CTimeSeries trigger : data.getAllTimeSeries())
					{
						if (trigger.hasAddedOrDeleted())
						{
							log.trace("ts id={} #comps={}",
									  trigger.getTimeSeriesIdentifier().getUniqueString(),
									  trigger.getDependentCompIds().size());
							for(DbKey compId : trigger.getDependentCompIds())
							{
								log.trace("dependent compId={}", compId);
								if (isInPythonWrittenQueue(compId, trigger.getTimeSeriesIdentifier().getKey()))
								{
									log.trace("--Resolver Skipping because recently written by python.");
									continue;
								}
								DbComputation origComp = null;
								try
								{
									origComp = computationDAO.getComputationById(compId);
								}
								catch (NoSuchObjectException ex)
								{
									log.atWarn()
									   .setCause(ex)
									   .log("Time Series {} uses compId {} which no longer exists in database.",
									   		trigger.getDisplayName(), compId);
									continue;
								}
								if (!origComp.hasGroupInput())
								{
									addToResults(results, origComp, trigger);
									continue;
								}
								// Else this is a group computation.
								// Group comps will have partial (abstract) params.
								// Use the triggering time series to make the computation concrete.
								try
								{
									DbComputation comp =
										makeConcrete(theDb, tsDAI, trigger.getTimeSeriesIdentifier(), origComp, true);
									addToResults(results, comp, trigger);
								}
								catch(NoSuchObjectException ex)
								{
									if (!theDb.isCwms())
									{
										log.atWarn()
										   .setCause(ex)
										   .log("Failed to make clone for computation {}:{} for time series {}",
										   		compId, origComp.getName(),
												trigger.getTimeSeriesIdentifier().getUniqueString());
										continue;
									}
									// The following handles the wildcards in CWMS sub-parts.

									// This means we failed to create a clone from the triggering TSID because one or
									// more of its input parms did not exist.
									// We have to try all of the other TSIDs that can trigger this computation
									// to see if the result contains THIS trigger.

									// Use Case: Rating with 2 indeps: a common Elev and several Gate Openings.
									// When triggered by Elev, the clone won't be able to create clones for
									// the gate openings because each has a sublocation.
									// E.G:
									//    indep1=BaseLoc.Elev.Inst.15Minutes.0.rev
									//    indep2=BaseLoc-*.Opening.Inst.15Minutes.0.rev
									// Potential Triggers:
									//    BaseLoc.Elev.Inst.15Minutes.0.rev
									//    BaseLoc-Spillway1-Gate1.Opening.Inst.15Minutes.0.rev
									//    BaseLoc-Spillway1-Gate2.Opening.Inst.15Minutes.0.rev
									//    BaseLoc-Spillway2-Gate1.Opening.Inst.15Minutes.0.rev

									ArrayList<TimeSeriesIdentifier> triggers = compDependsDAO.getTriggersFor(origComp.getId());
									log.trace("{} total triggers found:", triggers.size());
									int nAdded = 0, nFailed = 0, nInapplicable = 0;
									for(TimeSeriesIdentifier otherTrig : triggers)
									{
										log.trace("Other trigger '{}'", otherTrig.getUniqueString());
										if (otherTrig.equals(trigger.getTimeSeriesIdentifier()))
											continue;
										try
										{
											DbComputation comp = makeConcrete(theDb, tsDAI, otherTrig, origComp, true);
											if (compContainsInput(comp, trigger.getTimeSeriesIdentifier()))
											{
												addToResults(results, comp, trigger);
												nAdded++;
											}
											else
												nInapplicable++;
										}
										catch (NoSuchObjectException e)
										{
											// Failed to create clone
											nFailed++;
										}
									}
									log.debug("For extended group resolution: {} added from other triggers, " +
											  "{} inapplicable from other triggers, " +
											  "{} failed from other triggers.",
											  nAdded, nInapplicable, nFailed);

								}
							}
						}
					}
				}
			});
			return results.toArray(new DbComputation[0]);
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to resolve computations.", ex);
		}
	}

	/**
	 * Return true if the passed computation contains an input param with the passed TSID.
	 * @param comp the computation
	 * @param timeSeriesIdentifier the TSID
	 * @return
	 */
	private boolean compContainsInput(DbComputation comp, TimeSeriesIdentifier tsid)
	{
		for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext();)
		{
			DbCompParm dcp = parmit.next();
			if (dcp.isInput() && !DbKey.isNull(dcp.getSiteDataTypeId())
			 && tsid.getKey().equals(dcp.getSiteDataTypeId()))
				return true;
		}

		return false;
	}

	/**
	 * This method is called for group-input-comps, along with the time-series
	 * that triggered the computation. Here we make (or retrieve) a clone
	 * of the computation for this particular point.
	 * This also modifies the output parms to be at the same site as the
	 * input, and if unspecified in the DB, sets the interval and tabsel
	 * to be the same as the input.
	 *
	 * @param theDb the time series database.
	 * @param tsid The triggering time series identifier
	 * @param incomp the group-based computation
	 * @param createOutput true if this method is allowed to create output time series
	 * in order to completely specify the computation. This is only done when we are
	 * resolving in order to run the computation.
	 * @return completely-specified computation, containing no group refs,
	 * or null if database inconsistency and concrete params could not be resolved.
	 * @throws NoSuchObjectException
	 * @throws DbIoException
	 */
	public static DbComputation makeConcrete(TimeSeriesDb theDb, TimeSeriesDAI tsDai,
		TimeSeriesIdentifier tsid, DbComputation incomp, boolean createOutput)
		throws NoSuchObjectException, DbIoException
	{
		DbComputation comp = incomp.copyNoId();

		// Has to have ID from original comp so we can detect duplicates.
		comp.setId(incomp.getId());
		log.trace("makeConcrete of computation {} for key={}, '{}', compId={}",
				  comp.getName(), tsid.getKey(), tsid.getUniqueString(), comp.getId());
		String parmName = "";
		try
		{
			comp.setUnPrepared(); // will delete the executive

			for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
			{
				DbCompParm dcp = parmit.next();
				parmName = dcp.getRoleName();
				String nm = comp.getProperty(dcp.getRoleName() + "_tsname");
				String missing = comp.getProperty(parmName + "_MISSING");
				TimeSeriesIdentifier parmTsid =
					theDb.transformTsidByCompParm(tsDai, tsid, dcp,
						createOutput && dcp.isOutput(),    // Yes create TS if this is an output
						true,                              // Yes update the DbCompParm object
						nm);
				// Note in the GUI, createOutput==false. Thus if we fail to
				// transform an output, just leave it undefined.
				if (parmTsid == null && createOutput && !"ignore".equalsIgnoreCase(missing))
				{
					throw new NoSuchObjectException("Cannot resolve parm " + parmName);
				}
			}
			return comp;
		}
		catch(NoSuchObjectException | BadTimeSeriesException ex)
		{
			throw new NoSuchObjectException("Cannot create resolve computation '" + comp.getName()
				+ "' for role '" + parmName + "' ts=" +
				tsid.getDisplayName(), ex);
		}
		catch(DbIoException ex)
		{
			throw new DbIoException("Cannot resolve computation '" + comp.getName()
				+ "' for input " + tsid.getDisplayName(), ex);
		}
	}

	/**
	 * Ensure that only one copy of a given cloned computation is executed
	 * in the cycle. Example: Adding A and B; both A and B are present in the
	 * input data. Clone will be created when we evaluate A. We don't want to
	 * create a separate clone when we evaluate B.
	 * This method adds the computation to the result set only if it is not
	 * already present
	 * @param results the result set
	 * @param comp the concrete cloned computation
	 * @return the computation in the list if exact match is found
	 */
	private DbComputation searchResults(Collection<DbComputation> results, DbComputation comp)
	{
		for(DbComputation tc : results)
		{
			if (isTheSameClone(comp, tc))
				return tc;
		}
		return null;
	}

	public void addToResults(Collection<DbComputation> results, DbComputation comp, CTimeSeries trigger)
	{
		DbComputation already = searchResults(results, comp);
		if (already != null)
		{
			if (trigger != null)
				already.getTriggeringRecNums().addAll(trigger.getTaskListRecNums());
		}
		else // newly added computation
		{
			if (trigger != null)
				comp.getTriggeringRecNums().addAll(trigger.getTaskListRecNums());
			results.add(comp);
		}
	}

	/**
	 * Returns true if the two comps represent the same clone. Used in the above
	 * to make sure only one copy of a given clone is returned.
	 * @param comp1
	 * @param comp2
	 * @return
	 */
	private boolean isTheSameClone(DbComputation comp1, DbComputation comp2)
	{
		if (!comp1.getId().equals(comp2.getId()))
			return false;

		// Special case for GroupAdder algorithm. It expands a single param be the
		// entire group in the algorithm. So all instances of the same compid are the same.
		if (comp1.getAlgorithm().getExecClass().contains("GroupAdder"))
			return true;

		// make sure all the input parms are the same.
		for(Iterator<DbCompParm> parmit = comp1.getParms(); parmit.hasNext();)
		{
			DbCompParm in1 = parmit.next();
			if (in1.isInput())
			{
				DbCompParm in2 = comp2.getParm(in1.getRoleName());
				if (in2 == null)
					return false;

				if (DbKey.isNull(in1.getSiteDataTypeId()) != DbKey.isNull(in2.getSiteDataTypeId()))
					return false;
				if (DbKey.isNull(in1.getSiteDataTypeId()))
					continue;

				if (!in1.getSiteDataTypeId().equals(in2.getSiteDataTypeId()))
					return false;
			}
		}
		return true;
	}


	private void trimPythonWrittenQueue()
	{
		PythonWritten pw = null;
		Date cutoff = new Date(System.currentTimeMillis() - 120000L); // 2 minutes ago
		int n = 0;
		while((pw = pythonWrittenQueue.peekFirst()) != null && pw.getTimeWritten().before(cutoff))
		{
			pythonWrittenQueue.remove();
			n++;
		}
	}

	private boolean isInPythonWrittenQueue(DbKey compId, DbKey tsCode)
	{
		for (PythonWritten pw : pythonWrittenQueue)
			if (compId.equals(pw.getCompId())
			 && tsCode.equals(pw.getTsCode()))
				return true;
		return false;
	}

	public void pythonWrote(DbKey compId, DbKey tsCode)
	{
		// I only need a single instance for a given compId/tsCode pair.
		// Remove the old one if present, and add a new one at the end with current date/time.
		for(PythonWritten pw : pythonWrittenQueue)
			if (pw.getCompId().equals(compId) && pw.getTsCode().equals(tsCode))
			{
				pythonWrittenQueue.remove(pw);
				break;
			}
		pythonWrittenQueue.addLast(new PythonWritten(compId, tsCode));
	}
}
