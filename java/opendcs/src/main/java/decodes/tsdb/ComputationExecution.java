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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.utils.logging.MDCTimer;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import static org.opendcs.utils.logging.ThreadUtils.propagate;

public final class ComputationExecution
{
	private static final String COMPUTATION_KEY = "computation";
	private final OpenDcsDatabase db;
	private AtomicInteger numErrors = new AtomicInteger(0);
	private AtomicInteger computesTried = new AtomicInteger(0);
	private final ExecutorService executor;

	/**
	 * Create instance of ComputationExecution using a new SingleThreadExeuctor
	 * @param db
	 */
	public ComputationExecution(OpenDcsDatabase db)
	{
		this(db, Executors.newSingleThreadExecutor());
	}


	/**
	 * Create instance of ComputationExecution using provided ExecutorService.
	 *
	 * NOTE: at this time we do not think the operations of DbComputation.apply are thread safe.
	 * This is being initially setup to provided chaining operations and too allow for that
	 * follow on work.
	 * @param db
	 * @param executor
	 */
	public ComputationExecution(OpenDcsDatabase db, ExecutorService executor)
	{
		this.db = db;
		this.executor = executor;
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


	/**
	 * Execute computations with given input data.
	 *
	 * @param toRun
	 * @param theData
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData)
	{
		return execute(toRun, theData, null, null, dc -> dc);
	}

	/**
	 * Execute computations with the given input data, after each computation run the afterComp handler
	 * which recieves only the output timeseries for processing.
	 * @param toRun
	 * @param theData
	 * @param afterComp
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Function<DataCollection, DataCollection> afterComp)
	{
		return execute(toRun, theData, null, null, afterComp);
	}

	/**
	 * Run computations using given data and date range, using default pass through afterComp handler
	 * @param toRun
	 * @param theData
	 * @param start
	 * @param end
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end)
	{
		return execute(toRun, theData, start, end, dc -> dc);
	}

	/**
	 * Run computations using the given data and date range using the given afterComp Handler.
	 * @param toRun
	 * @param theData
	 * @param start
	 * @param end
	 * @param afterComp
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end, Function<DataCollection, DataCollection> afterComp)
	{
		return execute(toRun, theData, start, end, new ProgressListener.LoggingProgressListener(), afterComp);
	}

	/**
	 * Run computations using the given data, date rangge, and ProgressListener
	 * @param toRun
	 * @param theData
	 * @param start
	 * @param end
	 * @param listener
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end, ProgressListener listener)
	{
		return execute(toRun, theData, start, end, listener, dc -> dc);
	}

	/**
	 * Run computations using the given data, date range, progress listener, and afterComp handler.
	 * 
	 *
	 * 
	 * @param toRun
	 * @param theData
	 * @param start
	 * @param end
	 * @param listener
	 * @param afterComp run using CompletableFuture::thenApply, if you wish to run the task on a different ExecutorService your handler should pass the data along.
	 * @return
	 */
	public CompResults execute(List<DbComputation> toRun, DataCollection theData, Date start, Date end, ProgressListener listener, Function<DataCollection, DataCollection> afterComp)
	{
		// Execute the computations
		List<CompletableFuture<DataCollection>> comps = new ArrayList<>();
		for(DbComputation comp : toRun)
		{
			var future = CompletableFuture.supplyAsync(
				propagate(() ->
				{
					DataCollection ret = new DataCollection();
					try (var mdcComputation = MDC.putCloseable(COMPUTATION_KEY, comp.getName());
						var compTimer = MDCTimer.startTimer(comp.getName()))
					{
						listener.onProgress(String.format("Executing computation '%s' #trigs=%d",
								comp.getName(), comp.getTriggeringRecNums().size()), Level.DEBUG, null);
						computesTried.incrementAndGet();
						boolean ignoreTimeWindow = start == null && end == null;
						ret = executeSingleComp(comp, start, end, theData, ignoreTimeWindow, listener);
					}
					catch(DbIoException ex)
					{
						listener.onProgress(String.format("Computation '%s' failed.", comp.getName()), Level.WARN, ex);
						numErrors.incrementAndGet();
						for (Integer rn : comp.getTriggeringRecNums())
						{
							theData.getTasklistHandle().markComputationFailed(rn);
						}
					}
					catch (Exception ex)
					{
						listener.onProgress(String.format("Unexpected error in computation %s", comp.getName()), Level.WARN, ex);
						numErrors.incrementAndGet();
						for(Integer rn : comp.getTriggeringRecNums())
						{
							theData.getTasklistHandle().markComputationFailed(rn);
						}
					}
					finally
					{
						comp.getTriggeringRecNums().clear();
						listener.onProgress(String.format("End of computation '%s'", comp.getName()), Level.DEBUG, null);
					}
					return ret;
				}), executor)
				.thenApply(dc ->
				{
					listener.logEvent("will save " + dc.size() + " time series from comp", Level.INFO, null);
					return dc;
				})
				.thenApply(afterComp);
			comps.add(future);
		}
		CompletableFuture.allOf(comps.toArray(new CompletableFuture[0])).join();
		return new CompResults(numErrors.get(), computesTried.get());
	}

	/**
	 * Execute a single computation with the provided inputs, date range, and progress listener.
	 * @param computation
	 * @param tsIds
	 * @param start
	 * @param end
	 * @param listener
	 * @return
	 * @throws DbIoException
	 */
	public CompResults execute(DbComputation computation,
			List<TimeSeriesIdentifier> tsIds, Date start, Date end, ProgressListener listener)
		throws DbIoException
	{
		DataCollection theData = new DataCollection();
		try (TimeSeriesDAI timeSeriesDAO = getTsDb().makeTimeSeriesDAO();
			 var compTimer = MDCTimer.startTimer("fill time series"))
		{
			for(TimeSeriesIdentifier tsid : tsIds)
			{
				try (var compTimer2 = MDCTimer.startTimer("saving: " + tsid.getUniqueString()))
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
					numErrors.incrementAndGet();
				}
			}
		}
		catch (Exception ex)
		{
			listener.onProgress("Unexpected error fetching input data.", Level.WARN, ex);
		}
		computesTried.incrementAndGet();
		try (var mdcComputation = MDC.putCloseable(COMPUTATION_KEY, computation.getName()))
		{
			executeSingleComp(computation, start, end, theData, false, listener);
		}
		return new CompResults(numErrors.get(), computesTried.get());
	}

	/**
	 * Run single computation using the provided data, date range and default LoggingProgressListener
	 * @param comp
	 * @param start
	 * @param end
	 * @param dataCollection
	 * @return
	 * @throws DbIoException
	 */
	public DataCollection executeSingleComp(DbComputation comp, Date start, Date end, DataCollection dataCollection)
			throws DbIoException
	{
		return executeSingleComp(comp, start, end, dataCollection, false, new ProgressListener.LoggingProgressListener());
	}

	/**
	 * Execute a single computation using a data collection.
	 * @param comp
	 * @param start
	 * @param end
	 * @param dataCollection
	 * @param ignoreTimeWindow
	 * @param listener
	 * @return
	 * @throws DbIoException
	 */
	private DataCollection executeSingleComp(DbComputation comp, Date start, Date end, DataCollection dataCollection, boolean ignoreTimeWindow, ProgressListener listener)
			throws DbIoException
	{
		DataCollection ret = new DataCollection();
		try(MDC.MDCCloseable mdc = MDC.putCloseable(COMPUTATION_KEY, comp.getName());
			var compTimer = MDCTimer.startTimer("executing single computation: " + comp.getName());
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
									numErrors.incrementAndGet();
								}
							}
						}
					}
				}

				ret = comp.apply(dataCollection, getTsDb());
				listener.onProgress("Successfully initiated computation", Level.INFO, null);
			}
			catch(DbCompException ex)
			{
				listener.onProgress(String.format("Cannot initialize computation '%s'", comp.getName()), Level.WARN, ex);
				numErrors.incrementAndGet();
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
				numErrors.incrementAndGet();
			}
		}
		catch (Exception ex)
		{
			listener.onProgress("Unexpected error in computation.", Level.WARN, ex);
		}
		return ret;
	}

	public record CompResults(int numErrors, int computesTried)
	{
	}
}
