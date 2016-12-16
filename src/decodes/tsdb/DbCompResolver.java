/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.3  2016/04/22 14:41:09  mmaloney
*  in pythonWrote, only allow a single unique (tsid,compid) tupple.
*
*  Revision 1.2  2016/03/24 19:13:37  mmaloney
*  Added history stuff needed for Python.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.21  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Iterator;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.sql.DbKey;

/**
This class contains the code to look at an input data collection
and determine which computations to attempt.
*/
public class DbCompResolver
{
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
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		try
		{
			Vector<DbComputation> results = new Vector<DbComputation>();
			for(CTimeSeries trigger : data.getAllTimeSeries())
				if (trigger.hasAddedOrDeleted())
				{
					Logger.instance().debug3(module + "ts id=" + trigger.getTimeSeriesIdentifier().getUniqueString() 
						+ "#comps = " + trigger.getDependentCompIds().size());
					for(DbKey compId : trigger.getDependentCompIds())
					{
						Logger.instance().debug1(module + "\t\tdependent compId=" + compId);
						if (isInPythonWrittenQueue(compId, trigger.getTimeSeriesIdentifier().getKey()))
						{
							Logger.instance().debug3(module + 
								"\t\t\t--Resolver Skipping because recently written by python.");
							continue;
						}
						DbComputation origComp = null;
						try
						{
							origComp = computationDAO.getComputationById(compId);
						}
						catch (NoSuchObjectException ex)
						{
							Logger.instance().warning(module + "Time Series " 
								+ trigger.getDisplayName() + " uses compId " + compId
								+ " which no longer exists in database.");
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
								makeConcrete(theDb, trigger.getTimeSeriesIdentifier(), origComp, true);
							addToResults(results, comp, trigger);
						}
						catch(NoSuchObjectException ex)
						{
							if (!theDb.isCwms())
							{
								Logger.instance().warning(module + "Failed to make clone for computation "
									+ compId + ": " + origComp.getName() + " for time series " 
									+ trigger.getTimeSeriesIdentifier().getUniqueString());
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
					
							CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();
							try
							{
								ArrayList<TimeSeriesIdentifier> triggers = compDependsDAO.getTriggersFor(origComp);
Logger.instance().debug3(module + triggers.size() + " total triggers found:");
								int nAdded = 0, nFailed = 0, nInapplicable = 0;
								for(TimeSeriesIdentifier otherTrig : triggers)
								{
Logger.instance().debug3(module + otherTrig.getUniqueString());
									if (otherTrig.equals(trigger.getTimeSeriesIdentifier()))
										continue;
									try
									{
										DbComputation comp = makeConcrete(theDb, otherTrig, origComp, true);
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
								Logger.instance().debug1(module + "for extended group resolution: "
									+ nAdded + " added from other triggers, " 
									+ nInapplicable + " inapplicable from other triggers, "
									+ nFailed + " failed from other triggers.");
							}
							finally
							{
								compDependsDAO.close();
							}
						}
					}
				}
			DbComputation[] r = new DbComputation[results.size()];
			return results.toArray(r);
		}
		finally
		{
			computationDAO.close();
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
	public static DbComputation makeConcrete(TimeSeriesDb theDb,
		TimeSeriesIdentifier tsid, DbComputation incomp, boolean createOutput)
		throws NoSuchObjectException, DbIoException
	{
		DbComputation comp = incomp.copyNoId();
		
		// Has to have ID from original comp so we can detect duplicates.
		comp.setId(incomp.getId());
Logger.instance().debug3(module + "makeConcrete of computation " + comp.getName()
+ " for key=" + tsid.getKey() + ", '" + tsid.getUniqueString() + "', compid = " + comp.getId());
		String parmName = "";
		try
		{
			comp.setUnPrepared(); // will delete the executive

			for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext(); )
			{
				DbCompParm dcp = parmit.next();
				parmName = dcp.getRoleName();
				String nm = comp.getProperty(dcp.getRoleName() + "_tsname");
				TimeSeriesIdentifier parmTsid = 
					theDb.transformTsidByCompParm(tsid, dcp, 
						createOutput && dcp.isOutput(),    // Yes create TS if this is an output
						true,                              // Yes update the DbCompParm object
						nm);
				// Note in the GUI, createOutput==false. Thus if we fail to
				// transform an output, just leave it undefined.
				if (parmTsid == null && createOutput)
				{
					throw new NoSuchObjectException("Cannot resolve parm");
				}
			}
			return comp;
		}
		catch(NoSuchObjectException ex)
		{
			Logger.instance().info(module + 
				"Cannot create resolve computation '" + comp.getName()
				+ "' for role '" + parmName + "' ts=" + 
				tsid.getDisplayName() + ": " + ex);
			throw ex;
		}
		catch(BadTimeSeriesException ex)
		{
			Logger.instance().info(module + 
				"Cannot create resolve computation '" + comp.getName()
				+ "' for role '" + parmName + "' ts=" + 
				tsid.getDisplayName() + ": " + ex);
			throw new NoSuchObjectException(ex.getMessage());
		}
		catch(DbIoException ex)
		{
			Logger.instance().info(module + "Cannot resolve computation '" + comp.getName()
				+ "' for input " + tsid.getDisplayName() + ": " + ex);
			throw ex;
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
	private DbComputation searchResults(Vector<DbComputation> results, DbComputation comp)
	{
		for(DbComputation tc : results)
		{
			if (isTheSameClone(comp, tc))
				return tc;
			
//			if (tc.getId() != comp.getId())
//				continue;
//			
//			DbCompParm tc_in = getFirstInput(tc);
//			if (tc_in == null)
//				continue;
//			DbCompParm comp_in = comp.getParm(tc_in.getRoleName());
//			if (comp_in == null)
//				// This could happen if they change an algorithm?
//				continue;
//			if (tc_in.getSiteDataTypeId().equals(comp_in.getSiteDataTypeId()))
//			{
//				if (theDb.isHdb())
//				{
//					// For HDB, comparing the SDI is not sufficient.
//					// Also have to check interval, tabsel, and modelId
//					if (!TextUtil.strEqualIgnoreCase(tc_in.getInterval(), 
//						comp_in.getInterval())
//					 || !TextUtil.strEqualIgnoreCase(tc_in.getTableSelector(), 
//						comp_in.getTableSelector())
//					 || tc_in.getModelId() != comp_in.getModelId())
//						continue;
//				}
//				Logger.instance().info("Resolver: Duplicate comp in cycle: "
//					+ comp.getName() + ", 1st input: " + tc_in.getSiteDataTypeId());
//				return tc;
//			}
		}
		return null;
	}
	
	private void addToResults(Vector<DbComputation> results, DbComputation comp, CTimeSeries trigger)
	{
		DbComputation already = searchResults(results, comp);
		if (already != null)
			already.getTriggeringRecNums().addAll(trigger.getTaskListRecNums());
		else // newly added computation
		{
			comp.getTriggeringRecNums().addAll(trigger.getTaskListRecNums());
			results.add(comp);
		}
	}

	private DbCompParm getFirstInput(DbComputation comp)
	{
		for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext();)
		{
			DbCompParm dcp = parmit.next();
			if (dcp.isInput() && !dcp.getSiteDataTypeId().isNull())
				return dcp;
		}
		// Shouldn't happen, the parm will have at least one input defined.
		return null;
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
//		Logger.instance().debug3("resolver.trimPythonWrittenQueue: trimmed to: " + cutoff +
//			", removed " + n + " PythonWritten objects. Q size is now " + pythonWrittenQueue.size());
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
//		Logger.instance().debug3("resolver.pythonWrote(compId=" + compId.getValue() + ", tsCode=" 
//			+ tsCode.getValue() + "). Q size is now " + pythonWrittenQueue.size());
	}
}
