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

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description ="RunningAverageAlgorithm averages single 'input' parameter to a single 'average'\n" + 
"parameter. A separate aggPeriodInterval property should be supplied.\n" +
"Example, input=Hourly Water Level, output=Daily Running Average, computed hourly,\n" +
"so each hour's output is the average of values at [t-23h ... t].")
public class RunningAverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;

	double tally;
	int count;
	Date lastTimeSlice = null;

	@Output(type = Double.class)
	public NamedVariable average = new NamedVariable("average", 0);

	@PropertySpec
	public long minSamplesNeeded = 1;
	@PropertySpec
	public boolean outputFutureData = false;

	public RunningAverageAlgorithm()
	{
		super();
		aggLowerBoundClosed = false;
		aggUpperBoundClosed = true;
	}
	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.RUNNING_AGGREGATE;
		_aggPeriodVarRoleName = "average";
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
	{
		// Zero out the tally & count for this agg period.
		tally = 0.0;
		count = 0;
		lastTimeSlice = null;

		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("average", inUnits);
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
		if (!isMissing(input))
		{
			tally += input;
			count++;
			lastTimeSlice = _timeSliceBaseTime;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		log.debug("RunningAverageAlgorithm:afterTimeSlices, count={}, lastTimeSlice={}", count, lastTimeSlice);

		boolean doOutput = true;
		if (count < minSamplesNeeded)
		{
			doOutput = false;
		}
		else if (!outputFutureData && _aggregatePeriodEnd.after(lastTimeSlice))
		{
			doOutput = false;
		}
		
		if (doOutput)
			setOutput(average, tally / (double)count);
		else 
		{
			if (_aggInputsDeleted)
				deleteOutput(average);
		}
	}
}
