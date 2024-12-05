/**
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb.algo;

import java.util.Date;
import java.util.Calendar;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "CentralRunningAverageAlgorithm averages single 'input' parameter to a single 'average' \n" +
		"parameter. A separate aggPeriodInterval property should be supplied.\n" +
		"Example, input=Hourly Water Level, output=Daily Running Average, computed hourly,\n" +
		"so each hour's output is the average of values at [t-23h ... t].\n" +
		"\n" +
		"This algorithm differs from RunningAverage algorithm in that the output is placed\n" +
		"at the center of the period, rather than at the beginning.")

public class CentralRunningAverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final String AVERAGESTRING = "average";

	@Input
	public double input;	//AW:TYPECODE=i

	double tally;
	int count;
	Date lastTimeSlice = null;
	int aggregateInterval;

	@Output
	public NamedVariable average = new NamedVariable(AVERAGESTRING, 0);


	@PropertySpec(value = "1")
	public long minSamplesNeeded = 1;
	@PropertySpec(value = "false")
	public boolean outputFutureData = false;

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
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.RUNNING_AGGREGATE;
		_aggPeriodVarRoleName = AVERAGESTRING;

		aggregateInterval = IntervalCodes.getIntervalSeconds(aggPeriodInterval);
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
//		debug2("AverageAlgorithm:doAWTimeSlice, input=" + input);
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
	}
}
