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

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.VarFlags;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;

@Algorithm(description = "Project an input value by copying it forward in time for the specified number of intervals.\n" +
		"If NumIntervals <= 0, then project forward until the next input value or until NOW.")
public class FillForward extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	@Input
	public double input;

	// Enter any local class variables needed by the algorithm.
	private CTimeSeries inputTS = null;
	private Interval outputIntv = null;
	private String outputIntvs = null;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@org.opendcs.annotations.PropertySpec(value="4")
	public long numIntervals = 4;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		// Code here will be run once, after the algorithm object is created.
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// Validation
		inputTS = getParmRef("input").timeSeries;
		if (inputTS.size() == 0)
			return;

		outputIntvs = getParmRef("output").compParm.getInterval();
		outputIntv = IntervalCodes.getInterval(outputIntvs);
		if (IntervalCodes.getIntervalSeconds(outputIntvs) == 0 || outputIntv.getCalMultiplier() == 0)
			throw new DbCompException("Output interval may not be 0 length.");

		// Prefill input time series with all values from first trig to last trig and
		// then the next value after last trig.
		Date firstTrig = null, lastTrig = null;
		for(int idx = 0; idx < inputTS.size(); idx++)
		{
			TimedVariable tv = inputTS.sampleAt(idx);
			if (VarFlags.wasAdded(tv) || VarFlags.mustWrite(tv))
			{
				if (firstTrig == null)
					firstTrig = tv.getTime();
				lastTrig = tv.getTime();
			}
		}

		try
		{
			if (!firstTrig.equals(lastTrig))
				tsdb.fillTimeSeries(inputTS, firstTrig, lastTrig, false, false, false);
			else
			{
				log.debug("Skipping fill because only one trigger.");
			}
			TimedVariable tv = tsdb.getNextValue(inputTS, lastTrig);
			if (tv != null)
			{
				log.debug("Retrieved nextval {}", tv);
			}
		}
		catch (Exception ex)
		{
			String msg = "Cannot fill time series for input";
			throw new DbCompException(msg, ex);
		}

		log.trace("outputIntv={}, firstTrig={}, lastTrig={}", outputIntvs, firstTrig, lastTrig);

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
		TimedVariable nextInput = inputTS.findNext(_timeSliceBaseTime);
		if (nextInput != null)
		{
			log.debug("timeSlice={}, next={}", _timeSliceBaseTime, nextInput.getTime());
		}

		Date fillEndTime = nextInput == null ? new Date() : nextInput.getTime();

		// Fill to the next output or now or specified number of intervals, whichever comes first.

		// Strategy is to use the existing aggregate period logic
		// where the "aggregate period" is simply the output interval.
		// Find the start of the "aggregate period" that
		// contains the latest input time, with bounds set to (...]
		// Now I am guaranteed that agg period END will be => the latest input time.
		// This also automatically takes the aggregate time zone and offset into
		// consideration.
		aggLowerBoundClosed = false;
		aggUpperBoundClosed = true;
		AggregatePeriod aggPeriod = determineAggPeriod(_timeSliceBaseTime, outputIntvs);

		// Loop forward specified number of increments.
		aggCal.setTime(aggPeriod.getEnd());

		int numFill = 0;
		for(; numIntervals == 0 || numFill < numIntervals; numFill++)
		{
			Date outputTime = aggCal.getTime();

			log.trace("numFill={}, outputTime={}, fillEndTime={}", numFill, outputTime, fillEndTime);

			if (!outputTime.before(fillEndTime))
				break;

			setOutput(output, input, outputTime);
			aggCal.add(outputIntv.getCalConstant(), outputIntv.getCalMultiplier());
		}
		log.debug("{} values filled.", numFill);

	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
	}

}
