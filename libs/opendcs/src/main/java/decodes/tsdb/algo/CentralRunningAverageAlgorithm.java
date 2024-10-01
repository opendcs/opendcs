/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.algo;

import java.util.Date;
import java.util.Calendar;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.IntervalCodes;

//AW:JAVADOC
/**
CentralRunningAverageAlgorithm averages single 'input' parameter to a single 'average' 
parameter. A separate aggPeriodInterval property should be supplied.
Example, input=Hourly Water Level, output=Daily Running Average, computed hourly,
so each hour's output is the average of values at [t-23h ... t].

This algorithm differs from RunningAverage algorithm in that the output is placed
at the center of the period, rather than at the beginning.
 */
//AW:JAVADOC_END
public class CentralRunningAverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	double tally;
	int count;
	Date lastTimeSlice = null;
	int aggregateInterval; 
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable average = new NamedVariable("average", 0);
	String _outputNames[] = { "average" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public long minSamplesNeeded = 1;
	public boolean outputFutureData = false;
	String _propertyNames[] = { "minSamplesNeeded", "outputFutureData" };
//AW:PROPERTIES_END

	public CentralRunningAverageAlgorithm()
	{
		super();
		aggLowerBoundClosed = false;
		aggUpperBoundClosed = true;
	}
	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.RUNNING_AGGREGATE;
		_aggPeriodVarRoleName = "average";
//AW:INIT_END

//AW:USERINIT
		aggregateInterval = IntervalCodes.getIntervalSeconds(aggPeriodInterval);
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
	{
//AW:BEFORE_TIMESLICES
		// Zero out the tally & count for this agg period.
		tally = 0.0;
		count = 0;
		lastTimeSlice = null;

		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("average", inUnits);
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
//		debug2("AverageAlgorithm:doAWTimeSlice, input=" + input);
		if (!isMissing(input))
		{
			tally += input;
			count++;
			lastTimeSlice = _timeSliceBaseTime;
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
debug1("CentralRunningAverageAlgorithm:afterTimeSlices, count=" + count
+ ", lastTimeSlice=" + 
(lastTimeSlice==null ? "null" : debugSdf.format(lastTimeSlice)));

		boolean doOutput = true;
		if (count < minSamplesNeeded)
		{
//			info("Not Producing Output: Do not have minimum # samples (" + minSamplesNeeded
//				+ ") have only " + count);
			doOutput = false;
		}
		else if (!outputFutureData && _aggregatePeriodEnd.after(lastTimeSlice))
		{
//			info("Not Producing Output: outputFutureData=false and period end ("
//				+ debugSdf.format(_aggregatePeriodEnd) + " is after last time-slice ("
//				+ debugSdf.format(lastTimeSlice));
			doOutput = false;
		}
		
                debug3("Start " + _aggregatePeriodBegin + " end " + _aggregatePeriodEnd);
		Calendar cal = Calendar.getInstance();
		cal.setTime(_aggregatePeriodBegin);
		cal.add(Calendar.SECOND,aggregateInterval/2);
		debug3("Setting output at : " + cal.getTime());
		if (doOutput)
			setOutput(average, tally / (double)count,cal.getTime());
		else 
		{
			if (_aggInputsDeleted)
				deleteOutput(average,cal.getTime());
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
