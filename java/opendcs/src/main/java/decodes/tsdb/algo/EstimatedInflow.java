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

import decodes.tsdb.DbCompException;
import ilex.var.NamedVariable;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;

@Algorithm(description = "Estimate inflow over the interval from reservoir storage and outflow.\n" +
		"Estimated Inflow = delta(Storage) + AverageOutflow over period.\n" +
		"Inputs are: Storage (in cubic meters), and outflow (in cubic meters per second)\n" +
		"Output is estimated inflow (in cubic meters per second)")
public class EstimatedInflow extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double storage;
	@Input
	public double outflow;

	double store_t0 = 0.;
	double store_t1 = 0.;
	double out_t0 = 0.;
	double out_t1 = 0.;
	boolean firstInPeriod = true;
	int numSlices = 0;
	Date first = null;
	Date last = null;


    @Output
	public NamedVariable inflow = new NamedVariable("inflow", 0);

	@org.opendcs.annotations.PropertySpec(value="true")
	public boolean aggUpperBoundClosed = true;
	@org.opendcs.annotations.PropertySpec(value="true")
	public boolean aggLowerBoundClosed = true;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "inflow";
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		firstInPeriod = true;
		numSlices = 0;
		first = null;
		last = null;
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		numSlices++;
		if (firstInPeriod)
		{
			store_t0 = storage;
			out_t0 = outflow;
			first = _timeSliceBaseTime;
			firstInPeriod = false;
		}
		else
		{
			store_t1 = storage;
			out_t1 = outflow;
			last = _timeSliceBaseTime;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		if (numSlices >= 2)
		{
			// Determine # of seconds in the aggregate period
			long seconds = (last.getTime() - first.getTime()) / 1000L;
			log.trace("Period Start={}, Period End={}, elapsed seconds = {}", first, last, seconds);
			log.trace("First Sample at {}, storage={}, outflow={}", first, store_t0, out_t0);
			log.trace(" Last Sample at {}, storage={}, outflow={}", last, store_t1, out_t1);
			// Get delta-storage (cubic meters):
			double dStore = (store_t1 - store_t0);
			// Convert output discharge Q to volume over period
			// (cubic meters / sec ===> cubic meters)
			// Then take the average from the start to end of period.
			double ave_out = .5 * (out_t0*seconds + out_t1*seconds);
			log.trace("change in storage is {}, average outflow over period is {}", dStore, ave_out);
			// Add in the storage, and convert to meters per second.
			double volume = dStore + ave_out;
			double discharge = volume / seconds;

			// Output the estimated inflow in cubic meters.
			setOutput(inflow, discharge);
		}
	}

}