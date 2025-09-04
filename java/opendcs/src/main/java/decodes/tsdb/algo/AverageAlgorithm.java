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

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(
		description = "AverageAlgorithm averages single 'input' parameter to a single 'average' \n" +
				"parameter. The averaging period is determined by the interval of the output\n" +
				"parameter."
)

public class AverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final String AVERAGESTRING = "average";
	@Input
	double input;

	double tally;
	int count;

	@Output
	NamedVariable average = new NamedVariable(AVERAGESTRING, 0);

	@org.opendcs.annotations.PropertySpec(value = "1")
	public long minSamplesNeeded = 1;
	@PropertySpec(value = "Double.NEGATIVE_INFINITY")
	public double negativeReplacement = Double.NEGATIVE_INFINITY;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = AVERAGESTRING;
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

		// Normally for average, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr(AVERAGESTRING, inUnits);
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
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		if (count >= minSamplesNeeded && count > 0)
		{
			double ave = tally / (double)count;

			// Added for HDB issue 386
			if (ave < 0.0 && negativeReplacement != Double.NEGATIVE_INFINITY)
			{
				log.debug("Computed average={}, will use negativeReplacement={}", ave, negativeReplacement);
				ave = negativeReplacement;
			}
			setOutput(average, ave);
		}
		else
		{
			log.warn("Do not have minimum # samples ({}) -- not producing an average.", minSamplesNeeded);
			if (_aggInputsDeleted)
				deleteOutput(average);
		}
	}
}
