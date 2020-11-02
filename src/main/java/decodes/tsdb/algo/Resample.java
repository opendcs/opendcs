package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Date;

import ilex.util.TextUtil;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Resample an input to an output with a different interval.
Output must not be irregular.
Input may be irregular or any interval greater than or less than the output.
 */
//AW:JAVADOC_END
public class Resample
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	/** The next output time */
	private Date nextOutputTime = null;
	private Date lastOutputTime = null;
	private IntervalIncrement outputIncr = null;
	private Date lastTimeSlice = null;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String method = "interp";
	String _propertyNames[] = { "method" };
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
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// output must be a regular interval time-series!
		ParmRef outputParmRef = getParmRef("output");
		outputIncr = IntervalCodes.getIntervalCalIncr(
			outputParmRef.compParm.getInterval());
		if (outputIncr == null || outputIncr.getCount() == 0)
			throw new DbCompException("Resample requires regular interval output!");
		
		// Get first & last trigger times.
		Date firstInputT = baseTimes.first();
		Date lastInputT = baseTimes.last();

		// Get the previous & next input so we can interpolate before &
		// after the trigger times.
		ParmRef inputParmRef = getParmRef("input");
		if (inputParmRef == null)
			throw new DbCompException("No 'input' param defined!");
		CTimeSeries inputTS = inputParmRef.timeSeries;
		TimedVariable prevInput = inputTS.findPrev(firstInputT);
		TimedVariable nextInput = null;
		try
		{
			if (prevInput == null)
				prevInput = tsdb.getPreviousValue(inputTS, firstInputT);
			nextInput = inputTS.findNext(lastInputT);
			if (nextInput == null)
				nextInput = tsdb.getNextValue(inputTS, lastInputT);
			debug1("firstInputT=" + debugSdf.format(firstInputT)
				+ ", prevInputT=" + (prevInput != null ? debugSdf.format(prevInput.getTime()) : " null ")
				+ ", nextInput=" + debugSdf.format(nextInput.getTime()));
		}
		catch(Exception ex)
		{
			String msg = "Error accessing input/output time series: " + ex;
			warning(msg);
			throw new DbCompException(msg);
		}
		// Note: prev & next may still be null!
		
		// 1st output time = 1st possible output time that is AFTER the 
		// prevInput, or if prevInput==null, AFTER or Equal to first input.
		// Last output time = last possible time before or equal to the
		// nextInput, or if nextInput==null, the last triggering input.
		
		aggCal.setTime(prevInput != null ? prevInput.getTime() : firstInputT);
		aggCal.set(Calendar.MILLISECOND, 0);
		aggCal.set(Calendar.SECOND, 0);
		Date firstOutputT = null;
		lastOutputTime = null;
		if (outputIncr.getCalConstant() == Calendar.MINUTE)
		{
			// output interval is in # of minutes
			int min = aggCal.get(Calendar.MINUTE);
			min = (min / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.MINUTE, min);
			if (prevInput != null)
				aggCal.add(Calendar.MINUTE, outputIncr.getCount());
			firstOutputT = aggCal.getTime();

			aggCal.setTime(nextInput != null ? nextInput.getTime() : lastInputT);
			min = aggCal.get(Calendar.MINUTE);
			min = (min / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.MINUTE, min);
			lastOutputTime = aggCal.getTime();
		}
		else if (outputIncr.getCalConstant() == Calendar.HOUR_OF_DAY)
		{
			aggCal.set(Calendar.MINUTE, 0);
			int hr = aggCal.get(Calendar.HOUR_OF_DAY);
			hr = (hr / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.HOUR_OF_DAY, hr);
			if (prevInput != null)
				aggCal.add(Calendar.HOUR_OF_DAY, outputIncr.getCount());
			firstOutputT = aggCal.getTime();
			
			aggCal.setTime(nextInput != null ? nextInput.getTime() : lastInputT);
			aggCal.set(Calendar.MINUTE, 0);
			hr = aggCal.get(Calendar.HOUR_OF_DAY);
			hr = (hr / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.HOUR_OF_DAY, hr);
			lastOutputTime = aggCal.getTime();
		}
		else if (outputIncr.getCalConstant() >= Calendar.DAY_OF_MONTH)
		{
			aggCal.set(Calendar.MINUTE, 0);
			aggCal.set(Calendar.HOUR_OF_DAY, 0);
			int day = aggCal.get(Calendar.DAY_OF_MONTH);
			day = (day / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.DAY_OF_MONTH, day);
			if (prevInput != null)
				aggCal.add(Calendar.DAY_OF_MONTH, outputIncr.getCount());
			firstOutputT = aggCal.getTime();
			
			aggCal.setTime(nextInput != null ? nextInput.getTime() : lastInputT);
			aggCal.set(Calendar.MINUTE, 0);
			aggCal.set(Calendar.HOUR_OF_DAY, 0);
			day = aggCal.get(Calendar.DAY_OF_MONTH);
			day = (day / outputIncr.getCount()) * outputIncr.getCount();
			aggCal.set(Calendar.DAY_OF_MONTH, day);
			lastOutputTime = aggCal.getTime();
		}
		else
		{
			throw new DbCompException("Invalid output interval: " + 
				outputParmRef.compParm.getInterval());
		}

		
		// Set 'nextOutputTime' for the time-slice method
		nextOutputTime = firstOutputT; 
		debug1("method='" + method + "'");
		debug1("first output time: " + debugSdf.format(nextOutputTime)
			+ ", output interval = " + outputIncr.getCount() 
			+ "*Calendar." + outputIncr.getCalConstant());
		debug1("first input time: " + debugSdf.format(firstInputT)
			+ ", last: " + debugSdf.format(lastInputT));
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
		tryProduceOutput(_timeSliceBaseTime, input);
		lastTimeSlice = _timeSliceBaseTime;
//AW:TIMESLICE_END
	}

	private void tryProduceOutput(Date t, double v)
	{
		ParmRef inputParmRef = getParmRef("input");
		CTimeSeries inputTS = inputParmRef.timeSeries;
		
		debug1("TryProduceOutput t = " + debugSdf.format(t));
		
		while(!nextOutputTime.after(t))
		{
			// If slice time == nextOutputTime, copy input to output
			// else if slice time > nextOutputTime, interpolate
			if (t.equals(nextOutputTime))
			{
				debug3("Setting output at time " + debugSdf.format(t.getTime())
						+ " to " + v + " method=fill value at same slice time");
				setOutput(output, v, nextOutputTime);
			}
			else // do either fill or interpolation 
			{
				TimedVariable tv = null;
				if (TextUtil.startsWithIgnoreCase(method, "fill"))
					tv = inputTS.findPrev(nextOutputTime);
				else
					tv = inputTS.findInterp(nextOutputTime.getTime()/1000L);
				if (tv != null)
				{
					debug3("Setting output at time " + debugSdf.format(tv.getTime())
						+ " to " + tv.getStringValue() + " method=" + method);
					try { setOutput(output, tv.getDoubleValue(), nextOutputTime); }
					catch (NoConversionException ex)
					{
						warning("Interpolation resulted in invalid var: " + ex);
					}
				}
			}
	
			// then advance next output time
			aggCal.setTime(nextOutputTime);
			aggCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
			nextOutputTime = aggCal.getTime();
//			debug1("Advanced nextOutputTime = " + debugSdf.format(nextOutputTime));
		}
	}
	
	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		// Find the first input with time >= the last output, and
		// execute it like it is a time-slice.
		if (lastTimeSlice != null && lastOutputTime.after(lastTimeSlice))
		{
			ParmRef inputParmRef = getParmRef("input");
			CTimeSeries inputTS = inputParmRef.timeSeries;
			TimedVariable tv = inputTS.findWithin(lastOutputTime, 0);
			if (tv == null)
				tv = inputTS.findNext(lastOutputTime);
			if (tv != null)
			{
				try { tryProduceOutput(tv.getTime(), tv.getDoubleValue()); }
				catch (NoConversionException ex)
				{
					warning("After timeslices: " + ex);
				}
			}
		}
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
