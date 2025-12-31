/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package decodes.tsdb;

import java.util.Date;
import java.util.List;

import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class ComputationExecution
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private final TimeSeriesDb db;
	private int numErrors = 0;
	private int computesTried = 0;

	public ComputationExecution(TimeSeriesDb db)
	{
		this.db = db;
	}

	public DataCollection execute(List<DbComputation> toRun, DataCollection theData)
	{
		// Execute the computations
		for(DbComputation comp :toRun)
		{
			try
			{
				log.debug("Executing computation '{}' #trigs={}",
						comp.getName(), comp.getTriggeringRecNums().size());
				computesTried++;
				comp.prepareForExec(db);
				comp.apply(theData, db);
			}
			catch(Exception ex)
			{
				if (ex instanceof DbCompException)
				{
					log.atWarn().setCause(ex).log("Computation '{}' failed.", comp.getName());
				}
				else
				{
					log.atError().setCause(ex).log("Unexpected error in computation '{}'.", comp.getName());
				}
				numErrors++;
				for (Integer rn : comp.getTriggeringRecNums())
				{
					theData.getTasklistHandle().markComputationFailed(rn);
				}
			}
			comp.getTriggeringRecNums().clear();
			log.debug("End of computation '{}'", comp.getName());
		}
		return theData;
	}

	public int getNumErrors()
	{
		return numErrors;
	}

	public int getComputesTried()
	{
		return computesTried;
	}

	public void execute(TimeSeriesDAI timeSeriesDAO, DbComputation computation,
			List<TimeSeriesIdentifier> tsIds, Date since, Date until)
		throws DbIoException
	{
		DataCollection theData = new DataCollection();
		for(TimeSeriesIdentifier tsid : tsIds)
		{
			try
			{
				CTimeSeries cts = timeSeriesDAO.makeTimeSeries(tsid);
				int n = timeSeriesDAO.fillTimeSeries(cts, since, until);
				String msg = String.format("Read tsid '%s' since=%s, until=%s, result=%d values.",
						tsid.getUniqueString(), since, until, n);
				log.info(msg);
				// Set the flag so that every value read is treated as a trigger.
				for(int idx = 0; idx < n; idx++)
				{
					VarFlags.setWasAdded(cts.sampleAt(idx));
				}
				theData.addTimeSeries(cts);
			}
			catch(Exception ex)
			{
				String msg = "Error fetching input data.";
				log.atWarn().setCause(ex).log(msg);
				numErrors++;
			}
		}
		executeSingleComp(computation, since, until, theData, timeSeriesDAO);
	}

	public void executeSingleComp(DbComputation tc, Date since, Date until, DataCollection dataCollection,
			TimeSeriesDAI timeSeriesDAO)
			throws DbIoException
	{
		// Make a data collection with inputs filled from ... until
		ParmRef parmRef = null;
		try
		{
			// The prepare method maps all input parms
			tc.prepareForExec(db);
			for(DbCompParm parm : tc.getParmList())
			{
				if (!parm.isInput())
					continue;
				// 'prepare' method doesn't actually create the CTimeSeries. Do that now.
				tc.getExecutive().setDc(dataCollection);
				tc.getExecutive().addTsToParmRef(parm.getRoleName(), false);
				parmRef = tc.getExecutive().getParmRef(parm.getRoleName());
				CTimeSeries cts = parmRef.timeSeries;

				// Read values between previous and this run. Then flag them as DB_ADDED
				// Thus, they will be treated as triggers by the computation.
				int numRead = timeSeriesDAO.fillTimeSeries(cts, since, until, true, true, false);
				if (numRead > 0)
				{
					for(int idx = 0; idx < cts.size(); idx++)
					{
						TimedVariable tv = cts.sampleAt(idx);
						if (!tv.getTime().before(since) && !tv.getTime().after(until))
							VarFlags.setWasAdded(tv);
					}
					if (dataCollection.getTimeSeriesByUniqueSdi(cts.getTimeSeriesIdentifier().getKey()) == null)
						try { dataCollection.addTimeSeries(cts); }
						catch (DuplicateTimeSeriesException ex)
						{
							// Should not happen! We checked first.
							String msg = "Unexpected duplicate time series.";
							log.atError().setCause(ex).log(msg);
						}
				}
			}

			tc.apply(dataCollection, db);
		}
		catch (DbCompException ex)
		{
			String msg = String.format("Cannot initialize computation '%s'", tc.getName());
			log.atWarn().setCause(ex).log(msg);
		}
		catch (BadTimeSeriesException ex)
		{
			String msg = "Error in running computation " + tc.getKey() + ":" + tc.getName() + " -- ";
			msg = msg + "No such input time series for parm '" + parmRef.role + "'";
			if (parmRef.tsid == null)
				msg = msg + " -- No TSID assigned.";
			else
				msg = msg + " -- TSID '" + parmRef.tsid.getUniqueString() + "' does not exist in db.";
			log.atWarn().setCause(ex).log(msg);
		}
	}
}
