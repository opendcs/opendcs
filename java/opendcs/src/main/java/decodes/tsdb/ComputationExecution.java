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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.event.Level;

public final class ComputationExecution
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private final OpenDcsDatabase db;
	private int numErrors = 0;
	private int computesTried = 0;

	public ComputationExecution(OpenDcsDatabase db)
	{
		this.db = db;
	}

	/**
	 * @return TimeSeriesDb the legacy database.
	 * @deprecated Prefer to use the OpenDcsDatabase interface, not yet supported for computation classes.
	 */
	@Deprecated
	private TimeSeriesDb getTsDb()
	{
		TimeSeriesDb tsdb = null;
		Optional<TimeSeriesDb> dbOpt = db.getLegacyDatabase(TimeSeriesDb.class);
		if(dbOpt.isPresent())
		{
			tsdb = dbOpt.get();
		}
		return tsdb;
	}

	public CompResults execute(List<DbComputation> toRun, DataCollection theData)
	{
		return execute(toRun, theData, Date.from(Instant.MIN), Date.from(Instant.MAX));
	}

	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date since, Date until)
	{
		return	execute(toRun, theData, since, until, new LoggingProgressListener());
	}

	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date since, Date until, ProgressListener listener)
	{
		// Execute the computations
		for(DbComputation comp : toRun)
		{
			try
			{
				listener.onProgress(String.format("Executing computation '%s' #trigs=%d",
						comp.getName(), comp.getTriggeringRecNums().size()), Level.DEBUG, null);
				computesTried++;
				executeSingleComp(comp, since, until, theData, true, listener);
			}
			catch(Exception ex)
			{
				listener.onProgress(String.format("Computation '%s' failed.", comp.getName()), Level.WARN, ex);
				numErrors++;
				for (Integer rn : comp.getTriggeringRecNums())
				{
					theData.getTasklistHandle().markComputationFailed(rn);
				}
			}
			comp.getTriggeringRecNums().clear();
			listener.onProgress(String.format("End of computation '%s'", comp.getName()), Level.DEBUG, null);
		}
		return new CompResults(numErrors, computesTried);
	}

	public CompResults execute(DbComputation computation,
			List<TimeSeriesIdentifier> tsIds, Date since, Date until, ProgressListener listener)
		throws DbIoException
	{
		DataCollection theData = new DataCollection();
		try (TimeSeriesDAI timeSeriesDAO = getTsDb().makeTimeSeriesDAO())
		{
			for(TimeSeriesIdentifier tsid : tsIds)
			{
				try
				{
					CTimeSeries cts = timeSeriesDAO.makeTimeSeries(tsid);
					int n = timeSeriesDAO.fillTimeSeries(cts, since, until);
					listener.onProgress(String.format("Read tsid '%s' since=%s, until=%s, result=%d values.",
							tsid.getUniqueString(), since, until, n), Level.INFO, null);
					// Set the flag so that every value read is treated as a trigger.
					for(int idx = 0; idx < n; idx++)
					{
						VarFlags.setWasAdded(cts.sampleAt(idx));
					}
					theData.addTimeSeries(cts);
				}
				catch(Exception ex)
				{
					listener.onProgress("Error fetching input data.", Level.WARN, ex);
					numErrors++;
				}
			}
		}
		computesTried++;
		executeSingleComp(computation, since, until, theData, false, listener);
		return new CompResults(numErrors, computesTried);
	}

	public void executeSingleComp(DbComputation tc, Date since, Date until, DataCollection dataCollection)
			throws DbIoException
	{
		executeSingleComp(tc, since, until, dataCollection, false, new LoggingProgressListener());
	}

	private void executeSingleComp(DbComputation tc, Date since, Date until, DataCollection dataCollection, boolean ignoreTimeWindow, ProgressListener listener)
			throws DbIoException
	{
		try(MDC.MDCCloseable mdc = MDC.putCloseable("computation", tc.getName());
			TimeSeriesDAI timeSeriesDAO = getTsDb().makeTimeSeriesDAO())
		{
			// Make a data collection with inputs filled from ... until
			ParmRef parmRef = null;
			try
			{
				// The prepare method maps all input parms
				tc.prepareForExec(getTsDb());
				if(!ignoreTimeWindow)
				{
					for(DbCompParm parm : tc.getParmList())
					{
						if(!parm.isInput())
							continue;
						// 'prepare' method doesn't actually create the CTimeSeries. Do that now.
						tc.getExecutive().setDc(dataCollection);
						tc.getExecutive().addTsToParmRef(parm.getRoleName(), false);
						parmRef = tc.getExecutive().getParmRef(parm.getRoleName());
						CTimeSeries cts = parmRef.timeSeries;

						// Read values between previous and this run. Then flag them as DB_ADDED
						// Thus, they will be treated as triggers by the computation.
						int numRead = timeSeriesDAO.fillTimeSeries(cts, since, until, true, true, false);
						if(numRead > 0)
						{
							for(int idx = 0; idx < cts.size(); idx++)
							{
								TimedVariable tv = cts.sampleAt(idx);
								if(!tv.getTime().before(since) && !tv.getTime().after(until))
									VarFlags.setWasAdded(tv);
							}
							if(dataCollection.getTimeSeriesByUniqueSdi(cts.getTimeSeriesIdentifier().getKey()) == null)
							{
								try
								{
									dataCollection.addTimeSeries(cts);
								}
								catch(DuplicateTimeSeriesException ex)
								{
									// Should not happen! We checked first.
									listener.onProgress("Unexpected duplicate time series.", Level.ERROR, ex);
									numErrors++;
								}
							}
						}
					}
				}

				tc.apply(dataCollection, getTsDb());
			}
			catch(DbCompException ex)
			{
				listener.onProgress(String.format("Cannot initialize computation '%s'", tc.getName()), Level.WARN, ex);
				numErrors++;
			}
			catch(BadTimeSeriesException ex)
			{
				String msg = "Error in running computation " + tc.getKey() + ":" + tc.getName() + " -- ";
				msg = msg + "No such input time series for parm '" + parmRef.role + "'";
				if(parmRef.tsid == null)
					msg = msg + " -- No TSID assigned.";
				else
					msg = msg + " -- TSID '" + parmRef.tsid.getUniqueString() + "' does not exist in db.";
				listener.onProgress(msg, Level.WARN, ex);
				numErrors++;
			}
		}
	}

	public record CompResults(int numErrors, int computesTried)
	{
	}

	public interface ProgressListener
	{
		void onProgress(String message, Level logLevel, Throwable cause);
	}

	public static final class LoggingProgressListener implements ProgressListener
	{
		@Override
		public void onProgress(String message, Level logLevel, Throwable cause)
		{
			log.atLevel(logLevel).setCause(cause).log(message);
		}
	}
}
