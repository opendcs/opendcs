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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ParmRef;
import decodes.util.PropertySpec;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Convert a short interval to a longer interval by taking the first value equal-to or after the longer-period timestamp.
Example: Convert 10min data to 30min data by taking data on the hour and half-hour

 */
//AW:JAVADOC_END
public class SubSample extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double inputShortInterval;	//AW:TYPECODE=i
	String _inputNames[] = { "inputShortInterval" };
//AW:INPUTS_END

//AW:LOCALVARS
	private IntervalIncrement outputIncr = null;
	private GregorianCalendar outputCal = null;
	private PropertySpec subsampPropertySpecs[] =
	{
		new PropertySpec("samplingTimeOffset", PropertySpec.STRING,
			"(optional) E.g. for a daily subsample: '6 Hours' to grab the 6AM value.")
	};

	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return subsampPropertySpecs;
	}
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable outputLongInterval = new NamedVariable("outputLongInterval", 0);
	String _outputNames[] = { "outputLongInterval" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String samplingTimeOffset = "";
	String _propertyNames[] = { "samplingTimeOffset" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
		// Code here will be run once, after the algorithm object is created.
//AW:USERINIT_END
	}

	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES

		// Note aggTZ may be set either globally of specifically for this algo or comp.
		outputCal = new GregorianCalendar(aggCal.getTimeZone());

		// We will use outputCal to keep track of the next output time.
		// Initialize it to the first time >= the first input time.
		Date firstInputT = baseTimes.first();
		outputCal.setTime(firstInputT);
		ParmRef outputParmRef = getParmRef("outputLongInterval");

		outputIncr = IntervalCodes.getIntervalCalIncr(
			outputParmRef.compParm.getInterval());
		if (outputIncr == null || outputIncr.getCount() == 0)
			throw new DbCompException("SubSample requires regular interval output!");

		log.trace("beforeTimeSlices firstInputT={} outputIncr = {}{}",
				  firstInputT, outputIncr,
				  (samplingTimeOffset!= null ? (", samplingTimeOffset=" + samplingTimeOffset) : ""));

		// Always get rid of seconds and msecs.
		outputCal.set(Calendar.MILLISECOND, 0);
		outputCal.set(Calendar.SECOND, 0);

		// MJM Added samplingTimeOffset processing for OpenDCS 6.2 RC03
		IntervalIncrement offsetIncr[] = null;
		if (samplingTimeOffset != null && samplingTimeOffset.trim().length() > 0)
		{
			try
			{
				offsetIncr = IntervalIncrement.parseMult(samplingTimeOffset);
				log.debug("Honoring sampling time offset '{}'", samplingTimeOffset);
			}
			catch (NoSuchObjectException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid samplingTimeOffset property '{}' -- ignored.", samplingTimeOffset);
				offsetIncr = null;
			}
		}

		if (outputIncr.getCalConstant() == Calendar.MINUTE)
		{
			// output interval is in # of minutes
			int min = outputCal.get(Calendar.MINUTE);
			min = (min / outputIncr.getCount()) * outputIncr.getCount();
			outputCal.set(Calendar.MINUTE, min);
		}
		else if (outputIncr.getCalConstant() == Calendar.HOUR_OF_DAY)
		{
			outputCal.set(Calendar.MINUTE, 0);
			int hr = outputCal.get(Calendar.HOUR_OF_DAY);
			hr = (hr / outputIncr.getCount()) * outputIncr.getCount();
			outputCal.set(Calendar.HOUR_OF_DAY, hr);
		}
		else if (outputIncr.getCalConstant() == Calendar.DAY_OF_MONTH)
		{
			outputCal.set(Calendar.MINUTE, 0);
			outputCal.set(Calendar.HOUR_OF_DAY, 0);
			int day = outputCal.get(Calendar.DAY_OF_MONTH);
			day = (day / outputIncr.getCount()) * outputIncr.getCount();
			outputCal.set(Calendar.DAY_OF_MONTH, day);
		}
		else if (outputIncr.getCalConstant() == Calendar.MONTH)
		{
			// Note count should be 1, 2, 3, 4, or 6. Anything else will give weird results.
			outputCal.set(Calendar.MINUTE, 0);
			outputCal.set(Calendar.HOUR_OF_DAY, 0);
			outputCal.set(Calendar.DAY_OF_MONTH, 1);
			int month = outputCal.get(Calendar.MONTH);
			month = (month / outputIncr.getCount()) * outputIncr.getCount();
			outputCal.set(Calendar.MONTH, month);
		}
		else
		{
			throw new DbCompException("Invalid output interval: " +
				outputParmRef.compParm.getInterval());
		}

		// If an offset was supplied, add it. Note: it's up to the user to make sure
		// it makes sense. Good: output interval = 1Day, offsetIncr = 6 hours.
		// Bad: output interval = 1 hour, offsetIncr = 1 week.
		if (offsetIncr != null)
			for(IntervalIncrement ii : offsetIncr)
			{
				outputCal.set(ii.getCalConstant(), ii.getCount());
			}

		// Because of the added increment, I could end up with an outputCal time that
		// is before the first input time.
		// Example Daily average at 6 AM from hourly inputs, and the first value I'm given is 7 AM.
		// The above code will set outputCal to 6AM. I need to add the output increment so that
		// the outputCal is always >= the first input time.
		while(outputCal.getTime().before(firstInputT))
		{
			log.trace("beforeTimeSlices firstInputT={}, outputCal={}, incr={}",
					  firstInputT, outputCal.getTime(), outputIncr);
			// should the following be -outputIncr.getCount()
			outputCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
		}

		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("inputShortInterval");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("outputLongInterval", inUnits);

		log.debug("first input={}, first output={} outputIncr={}",
				  firstInputT, outputCal.getTime(), outputIncr.toString());
//AW:BEFORE_TIMESLICES_END
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
//AW:TIMESLICE
		Date nextOutputT = outputCal.getTime();
		long deltaSec = (_timeSliceBaseTime.getTime() - nextOutputT.getTime()) / 1000L;
		if (deltaSec <= roundSec && deltaSec >= -roundSec)
		{
			log.debug("Outputting value at {}, deltaSec={}, timeSlice={}",
					  nextOutputT, deltaSec, _timeSliceBaseTime);
			setOutput(outputLongInterval, inputShortInterval, nextOutputT);
		}

		// Regardless of whether te above produced an output, the
		// next output time should always be > current time slice
		while (!outputCal.getTime().after(_timeSliceBaseTime))
		{
			outputCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
		}
		log.debug("Advanced nextOutput to be at {}", outputCal.getTime());
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
//AW:AFTER_TIMESLICES_END
	}

	/**
	 * Required method returns a list of all input time series names.
	 */
	public String[] getInputNames()
	{
		return _inputNames;
	}

	/**
	 * Required method returns a list of all output time series names.
	 */
	public String[] getOutputNames()
	{
		return _outputNames;
	}

	/**
	 * Required method returns a list of properties that have meaning to
	 * this algorithm.
	 */
	public String[] getPropertyNames()
	{
		return _propertyNames;
	}
}
