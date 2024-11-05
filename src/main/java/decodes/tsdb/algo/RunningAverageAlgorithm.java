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

//AW:JAVADOC
/**
RunningAverageAlgorithm averages single 'input' parameter to a single 'average' 
parameter. A separate aggPeriodInterval property should be supplied.
Example, input=Hourly Water Level, output=Daily Running Average, computed hourly,
so each hour's output is the average of values at [t-23h ... t].
 */
//AW:JAVADOC_END
public class RunningAverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	double tally;
	int count;
	Date lastTimeSlice = null;

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
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.RUNNING_AGGREGATE;
		_aggPeriodVarRoleName = "average";
//AW:INIT_END

//AW:USERINIT
		// No one-time init required.
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
