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
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.21  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Iterator;

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
			for(CTimeSeries ts : data.getAllTimeSeries())
				if (ts.hasAddedOrDeleted())
				{
					Logger.instance().debug3("ts id=" + ts.getTimeSeriesIdentifier().getUniqueString() 
						+ "#comps = " + ts.getDependentCompIds().size());
					for(DbKey compId : ts.getDependentCompIds())
					{
						Logger.instance().debug1("\t\tdependent compId=" + compId);
						if (isInPythonWrittenQueue(compId, ts.getTimeSeriesIdentifier().getKey()))
						{
							Logger.instance().debug3("\t\t\t--Resolver Skipping because recently written by python.");
							continue;
						}
						DbComputation comp = null;
						try
						{
							comp = computationDAO.getComputationById(compId);
						}
						catch (NoSuchObjectException ex)
						{
							Logger.instance().warning("Resolver: Time Series " 
								+ ts.getDisplayName() + " uses compId " + compId
								+ " which no longer exists in database.");
							continue;
						}
						if (comp.hasGroupInput())
						{
							// Group comps will have partial (abstract) params.
							// Use this time series to make the computation concrete.
							try
							{
								comp = makeConcrete(theDb, ts.getTimeSeriesIdentifier(), 
									comp, true);
							}
							catch(NoSuchObjectException ex)
							{
								continue;
							}
						}

						DbComputation already = searchResults(results, comp);
						if (already != null)
							already.getTriggeringRecNums().addAll(
								ts.getTaskListRecNums());
						else // newly added computation
						{
							comp.getTriggeringRecNums().addAll(ts.getTaskListRecNums());
							results.add(comp);
						}
					}
					continue;
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
Logger.instance().debug3("makeConcrete of computation " + comp.getName()
+ " for key=" + tsid.getKey() + ", compid = " + comp.getId());
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
			Logger.instance().info(
				"Cannot create resolve computation '" + comp.getName()
				+ "' for role '" + parmName + "' ts=" + 
				tsid.getDisplayName() + ": " + ex);
			throw ex;
		}
		catch(BadTimeSeriesException ex)
		{
			Logger.instance().info(
				"Cannot create resolve computation '" + comp.getName()
				+ "' for role '" + parmName + "' ts=" + 
				tsid.getDisplayName() + ": " + ex);
			throw new NoSuchObjectException(ex.getMessage());
		}
		catch(DbIoException ex)
		{
			Logger.instance().info("Cannot resolve computation '" + comp.getName()
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
	private DbComputation 
		searchResults(Vector<DbComputation> results, DbComputation comp)
	{
		for(DbComputation tc : results)
		{
			if (tc.getId() != comp.getId())
				continue;
			DbCompParm tc_in = getFirstInput(tc);
			if (tc_in == null)
				continue;
			DbCompParm comp_in = comp.getParm(tc_in.getRoleName());
			if (comp_in == null)
				// This could happen if they change an algorithm?
				continue;
			if (tc_in.getSiteDataTypeId().equals(comp_in.getSiteDataTypeId()))
			{
				if (theDb.isHdb())
				{
					// For HDB, comparing the SDI is not sufficient.
					// Also have to check interval, tabsel, and modelId
					if (!TextUtil.strEqualIgnoreCase(tc_in.getInterval(), 
						comp_in.getInterval())
					 || !TextUtil.strEqualIgnoreCase(tc_in.getTableSelector(), 
						comp_in.getTableSelector())
					 || tc_in.getModelId() != comp_in.getModelId())
						continue;
				}
				Logger.instance().info("Resolver: Duplicate comp in cycle: "
					+ comp.getName() + ", 1st input: " + tc_in.getSiteDataTypeId());
				return tc;
			}
		}
		return null;
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
	
	private void trimPythonWrittenQueue()
	{
		PythonWritten pw = null;
		Date cutoff = new Date(System.currentTimeMillis() - 120000L); // 2 minutes ago
		while((pw = pythonWrittenQueue.peekFirst()) != null && pw.getTimeWritten().before(cutoff))
			pythonWrittenQueue.remove();
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
		pythonWrittenQueue.addLast(new PythonWritten(compId, tsCode));
	}
}
