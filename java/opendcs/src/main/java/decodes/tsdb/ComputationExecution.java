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
import org.opendcs.utils.logging.MDCTimer;
import org.slf4j.MDC;
import org.slf4j.event.Level;

public final class ComputationExecution
{
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

	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end)
	{
		return execute(toRun, theData, start, end, new ProgressListener.LoggingProgressListener());
	}

	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end, ProgressListener listener)
	{
		// Execute the computations
		for(DbComputation comp : toRun)
		{
			try (var mdcComputation = MDC.putCloseable("computation", comp.getName());
				 var compTimer = MDCTimer.startTimer(comp.getName()))
			{
				listener.onProgress(String.format("Executing computation '%s' #trigs=%d",
						comp.getName(), comp.getTriggeringRecNums().size()), Level.DEBUG, null);
				computesTried++;
				executeSingleComp(comp, start, end, theData, true, listener);
			}
			catch(DbIoException ex)
			{
				listener.onProgress(String.format("Computation '%s' failed.", comp.getName()), Level.WARN, ex);
				numErrors++;
				for (Integer rn : comp.getTriggeringRecNums())
				{
					theData.getTasklistHandle().markComputationFailed(rn);
				}
			}
			catch (Exception ex)
			{
				listener.onProgress(String.format("Unexpected error in computation %s", comp.getName()), Level.WARN, ex);
				numErrors++;
				for(Integer rn : comp.getTriggeringRecNums())
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
			List<TimeSeriesIdentifier> tsIds, Date start, Date end, ProgressListener listener)
		throws DbIoException
	{
		DataCollection theData = new DataCollection();
		try (TimeSeriesDAI timeSeriesDAO = getTsDb().makeTimeSeriesDAO();
			 var compTimer = MDCTimer.startTimer(computation.getName()))
		{
			for(TimeSeriesIdentifier tsid : tsIds)
			{
				try (var compTimer2 = MDCTimer.startTimer(tsid.getUniqueString()))
				{
					CTimeSeries cts = timeSeriesDAO.makeTimeSeries(tsid);
					int n = timeSeriesDAO.fillTimeSeries(cts, start, end);
					listener.onProgress(String.format("Read tsid '%s' since=%s, until=%s, result=%d values.",
							tsid.getUniqueString(), start, end, n), Level.INFO, null);
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
		catch (Exception ex)
		{
			listener.onProgress("Unexpected error fetching input data.", Level.WARN, ex);
		}
		computesTried++;
		try (var mdcComputation = MDC.putCloseable("computation", computation.getName()))
		{
			executeSingleComp(computation, start, end, theData, false, listener);
		}
		return new CompResults(numErrors, computesTried);
	}

	public void executeSingleComp(DbComputation comp, Date start, Date end, DataCollection dataCollection)
			throws DbIoException
	{
		executeSingleComp(comp, start, end, dataCollection, false, new ProgressListener.LoggingProgressListener());
	}

	private void executeSingleComp(DbComputation comp, Date start, Date end, DataCollection dataCollection, boolean ignoreTimeWindow, ProgressListener listener)
			throws DbIoException
	{
		try(MDC.MDCCloseable mdc = MDC.putCloseable("computation", comp.getName());
			var compTimer = MDCTimer.startTimer(comp.getName());
			TimeSeriesDAI timeSeriesDAO = getTsDb().makeTimeSeriesDAO())
		{
			// Make a data collection with inputs filled from ... until
			ParmRef parmRef = null;
			try
			{
				// The prepare method maps all input parms
				comp.prepareForExec(getTsDb());
				if(!ignoreTimeWindow)
				{
					for(DbCompParm parm : comp.getParmList())
					{
						if(!parm.isInput())
							continue;
						// 'prepare' method doesn't actually create the CTimeSeries. Do that now.
						comp.getExecutive().setDc(dataCollection);
						comp.getExecutive().addTsToParmRef(parm.getRoleName(), false);
						parmRef = comp.getExecutive().getParmRef(parm.getRoleName());
						CTimeSeries cts = parmRef.timeSeries;

						// Read values between previous and this run. Then flag them as DB_ADDED
						// Thus, they will be treated as triggers by the computation.
						int numRead = timeSeriesDAO.fillTimeSeries(cts, start, end, true, true, false);
						if(numRead > 0)
						{
							for(int idx = 0; idx < cts.size(); idx++)
							{
								TimedVariable tv = cts.sampleAt(idx);
								if(!tv.getTime().before(start) && !tv.getTime().after(end))
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

				comp.apply(dataCollection, getTsDb());
				listener.onProgress("Successfully initiated computation", Level.INFO, null);
			}
			catch(DbCompException ex)
			{
				listener.onProgress(String.format("Cannot initialize computation '%s'", comp.getName()), Level.WARN, ex);
				numErrors++;
				for (Integer rn : comp.getTriggeringRecNums())
				{
					dataCollection.getTasklistHandle().markComputationFailed(rn);
				}
			}
			catch(BadTimeSeriesException ex)
			{
				String msg = "Error in running computation " + comp.getKey() + ":" + comp.getName() + " -- ";
				msg = msg + "No such input time series for parm '" + parmRef.role + "'";
				if(parmRef.tsid == null)
					msg = msg + " -- No TSID assigned.";
				else
					msg = msg + " -- TSID '" + parmRef.tsid.getUniqueString() + "' does not exist in db.";
				listener.onProgress(msg, Level.WARN, ex);
				numErrors++;
			}
		}
		catch (Exception ex)
		{
			listener.onProgress("Unexpected error in computation.", Level.WARN, ex);
		}
	}

	public record CompResults(int numErrors, int computesTried)
	{
	}
}
