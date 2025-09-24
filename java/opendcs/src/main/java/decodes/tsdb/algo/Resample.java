package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Date;

import ilex.util.TextUtil;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description ="Resample an input to an output with a different interval. Output must not be irregular. Input may be\n" + 
"irregular or any interval greater than or less than the output." )
public class Resample extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;
	
	/** The next output time */
	private Date nextOutputTime = null;
	private Date lastOutputTime = null;
	private IntervalIncrement outputIncr = null;
	private Date lastTimeSlice = null;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec
	public String method = "interp";


	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
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
		}
		catch(Exception ex)
		{
			String msg = "Error accessing input/output time series: " + ex;
			throw new DbCompException(msg, ex);
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
		log.debug("method='{}'", method);
		log.debug("first output time: {}, output interval = {} *Calendar.{}",
				  nextOutputTime, outputIncr.getCount(), outputIncr.getCalConstant());
		log.debug("first input time: {}, last: {}", firstInputT, lastInputT);
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
		tryProduceOutput(_timeSliceBaseTime, input);
		lastTimeSlice = _timeSliceBaseTime;
	}

	private void tryProduceOutput(Date t, double v)
	{
		ParmRef inputParmRef = getParmRef("input");
		CTimeSeries inputTS = inputParmRef.timeSeries;
		
		log.debug("TryProduceOutput t = {}", t);
		
		while(!nextOutputTime.after(t))
		{
			// If slice time == nextOutputTime, copy input to output
			// else if slice time > nextOutputTime, interpolate
			if (t.equals(nextOutputTime))
			{
				log.trace("Setting output at time {} to {} method=fill value at same slice time",
						  t.getTime(), v);
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
					log.trace("Setting output at time {} to {} method={}",
							  tv.getTime(), tv.getStringValue(), method);
					try { setOutput(output, tv.getDoubleValue(), nextOutputTime); }
					catch (NoConversionException ex)
					{
						log.atWarn().setCause(ex).log("Interpolation resulted in invalid var.");
					}
				}
			}
	
			// then advance next output time
			aggCal.setTime(nextOutputTime);
			aggCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
			nextOutputTime = aggCal.getTime();
		}
	}
	
	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
		throws DbCompException
	{
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
					log.atWarn().setCause(ex).log("After timeslices: unable to produce output.");
				}
			}
		}
	}
}
