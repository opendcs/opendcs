/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.13  2011/11/08 22:23:24  mmaloney
 * dev
 *
 * Revision 1.12  2011/11/08 22:01:39  mmaloney
 * *** empty log message ***
 *
 * Revision 1.11  2011/11/08 21:53:39  mmaloney
 * dev
 *
 * Revision 1.10  2011/11/08 21:45:38  mmaloney
 * dev
 *
 * Revision 1.9  2011/11/08 21:12:42  mmaloney
 * dev
 *
 * Revision 1.8  2011/05/03 19:06:26  mmaloney
 * dev
 *
 * Revision 1.7  2011/05/03 18:44:57  mmaloney
 * dev
 *
 * Revision 1.6  2011/05/03 18:19:25  mmaloney
 * dev
 *
 * Revision 1.5  2011/05/03 17:23:38  mmaloney
 * Convert to high-performance time-slice algorithm. No fetching.
 *
 * Revision 1.4  2010/12/21 19:20:35  mmaloney
 * group computations
 *
 */
package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;
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
public class SubSample
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
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
//	public boolean aggLowerBoundClosed = true;
//	public boolean aggUpperBoundClosed = false;
//	String _propertyNames[] = { "aggLowerBoundClosed", "aggUpperBoundClosed" };
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

		debug3("beforeTimeSlices firstInputT=" + debugSdf.format(firstInputT) + " outputIncr = " + outputIncr
			+ (samplingTimeOffset!= null ? (", samplingTimeOffset=" + samplingTimeOffset) : ""));

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
				debug1("Honoring sampling time offset '" + samplingTimeOffset + "'");
			}
			catch (NoSuchObjectException ex)
			{
				warning("Invalid samplingTimeOffset property '" + samplingTimeOffset + "': " + ex
					+ " -- ignored.");
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
		else if (outputIncr.getCalConstant() >= Calendar.DAY_OF_MONTH)
		{
			outputCal.set(Calendar.MINUTE, 0);
			outputCal.set(Calendar.HOUR_OF_DAY, 0);
			int day = outputCal.get(Calendar.DAY_OF_MONTH);
			day = (day / outputIncr.getCount()) * outputIncr.getCount();
			outputCal.set(Calendar.DAY_OF_MONTH, day);
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
				outputCal.add(ii.getCalConstant(), ii.getCount());
		
		
		// 20160226
		// MJM -- I think the following code is broken. Why would I do this?
		// It says: if there is a change in time zone between the first data I have and the
		// next expected output, then adjust the next expected output.
		// 
//		TimeZone tz = outputCal.getTimeZone();
//		if (tz.inDaylightTime(firstInputT)
//		 && !tz.inDaylightTime(outputCal.getTime()))
//		{
//			outputCal.add(Calendar.HOUR_OF_DAY, -1);
//		}
//		else if (!tz.inDaylightTime(firstInputT)
//		 	  && tz.inDaylightTime(outputCal.getTime()))
//		{
//			outputCal.add(Calendar.HOUR_OF_DAY, 1);
//		}
		
		while(firstInputT.before(outputCal.getTime()))
		{
			debug3("beforeTimeSlices firstInputT=" + debugSdf.format(firstInputT)
				+ ", outputCal=" + debugSdf.format(outputCal.getTime())
				+ ", incr=" + outputIncr);
			outputCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
		}
			
		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("inputShortInterval");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("outputLongInterval", inUnits);
		
		debug1("first input=" + debugSdf.format(firstInputT)
			+ ", first output=" + debugSdf.format(outputCal.getTime())
			+ " outputIncr=" + outputIncr.toString());
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
			debug1("Outputting value at " + debugSdf.format(nextOutputT)
				+ ", deltaSec=" + deltaSec + ", timeSlice=" 
				+ debugSdf.format(_timeSliceBaseTime));
			setOutput(outputLongInterval, inputShortInterval, nextOutputT);
		}
		
		// Regardless of whether te above produced an output, the
		// next output time should always be > current time slice
		while (!outputCal.getTime().after(_timeSliceBaseTime))
		{
			outputCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
		}
		debug1("Advanced nextOutput to be at " + debugSdf.format(outputCal.getTime()));
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
