/**
 * $Id$
 * 
 * $Log$
 * Revision 1.11  2012/09/30 15:20:23  mmaloney
 * Removed superfluous info messages.
 *
 * Revision 1.10  2011/06/16 15:14:53  mmaloney
 * dev
 *
 * Revision 1.9  2011/06/16 13:28:54  mmaloney
 * Improved warning message
 *
 * Revision 1.8  2011/05/16 14:51:23  mmaloney
 * reduce debugs
 *
 * Revision 1.7  2011/02/22 14:02:33  mmaloney
 * minor tweaks
 *
 * Revision 1.6  2011/02/07 16:12:31  mmaloney
 * future data is not data >now, it is data >last time-slice.
 *
 * Revision 1.5  2011/02/07 15:17:21  mmaloney
 * Added boolean outputFutureData property with default=false.
 *
 * Revision 1.4  2010/12/21 19:20:35  mmaloney
 * group computations
 *
 */
package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.annotations.PropertySpec;

@Algorithm(description ="RunningAverageAlgorithm averages single 'input' parameter to a single 'average'\n" + 
"parameter. A separate aggPeriodInterval property should be supplied.\n" +
"Example, input=Hourly Water Level, output=Daily Running Average, computed hourly,\n" +
"so each hour's output is the average of values at [t-23h ... t].")
public class RunningAverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;	//AW:TYPECODE=i

	double tally;
	int count;
	Date lastTimeSlice = null;

	@Output(type = Double.class)
	public NamedVariable average = new NamedVariable("average", 0);

	@PropertySpec
	public long minSamplesNeeded = 1;
	@PropertySpec
	public boolean outputFutureData = false;

	public RunningAverageAlgorithm()
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
		_aggPeriodVarRoleName = "average";
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
debug1("RunningAverageAlgorithm:afterTimeSlices, count=" + count
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
		
		if (doOutput)
			setOutput(average, tally / (double)count);
		else 
		{
			if (_aggInputsDeleted)
				deleteOutput(average);
		}
	}
}
