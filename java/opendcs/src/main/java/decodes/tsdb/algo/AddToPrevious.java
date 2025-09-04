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
package decodes.tsdb.algo;

import opendcs.dai.TimeSeriesDAI;

import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(
		description = "Adds the current value to the previous value in the database\n" +
				"\t\tand outputs the sum. Works on any time-series, any interval.\n" +
				"\t\tThis algorithm does assume that you are calling it with a \n" +
				"\t\tseries of contiguous values, like you would get out of a DCP\n" +
				"\t\tmessage.")

public class AddToPrevious extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;
	private double prevVal = 0.0;
	private boolean justStarted = true;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm()
			throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
			throws DbCompException
	{
		justStarted = true;
	}

	/**
	 * Do the algorithm for a single time slice.
	 * AW will fill in user-supplied code here.
	 * Base class will set inputs prior to calling this method.
	 * User code should call one of the setOutput methods for a time-slice
	 * output variable.
	 *
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	@Override
	protected void doAWTimeSlice()
			throws DbCompException
	{
		if (justStarted)
		{
			ParmRef inputParmRef = getParmRef("input");
			CTimeSeries inputTS = inputParmRef.timeSeries;
			try (TimeSeriesDAI timeSeriesDAO = tsdb.makeTimeSeriesDAO())
			{
				TimedVariable prevInput =
						timeSeriesDAO.getPreviousValue(inputTS,
								inputParmRef.compParm.baseTimeToParamTime(
										this._timeSliceBaseTime, aggCal));
				prevVal = prevInput.getDoubleValue();
			}
			catch (Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Can't get prev value, time-slice at {} -- defaulting to 0.0",
				   		_timeSliceBaseTime);
				prevVal = 0.0;
			}
			justStarted = false;
		}
		setOutput(output, input + prevVal);
		prevVal = input;
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
			throws DbCompException
	{
	}
}
