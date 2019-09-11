package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;
import decodes.tsdb.TimeSeriesIdentifier;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Project an input value by copying it forward in time for the specified number of intervals.
If NumIntervals <= 0, then project forward until the next input value or until NOW.
 */
//AW:JAVADOC_END
public class FillForward
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
	private CTimeSeries inputTS = null;
	private Interval outputIntv = null;
	private String outputIntvs = null;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public long numIntervals = 4;
	String _propertyNames[] = { "numIntervals" };
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
			tsdb.fillTimeSeries(inputTS, firstTrig, lastTrig, false, false, false);
			tsdb.getNextValue(inputTS, lastTrig);
		}
		catch (Exception ex)
		{
			String msg = "Cannot fill time series for input: " + ex;
			warning(msg);
			throw new DbCompException(msg);
		}
		
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
		TimedVariable nextInput = inputTS.findNext(_timeSliceBaseTime);
		
		// Fill to the next output or specified number of intervals, whichever comes first.
		
		//========================================
		// Find the first _output_ time >= that time.
		
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
			if (!outputTime.before(nextInput.getTime()))
				break;
			
			setOutput(output, input, outputTime);
			aggCal.add(outputIntv.getCalConstant(), outputIntv.getCalMultiplier());
		}
		debug1("" + numFill + " values filled.");
		
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
