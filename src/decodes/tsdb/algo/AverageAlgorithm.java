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
AverageAlgorithm averages single 'input' parameter to a single 'average' 
parameter. The averaging period is determined by the interval of the output
parameter.

 */
//AW:JAVADOC_END
public class AverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	double tally;
	int count;

//AW:LOCALVARS_END

//AW:OUTPUTS
	NamedVariable average = new NamedVariable("average", 0);
	String _outputNames[] = { "average" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	long minSamplesNeeded = 1;
	double negativeReplacement = Double.NEGATIVE_INFINITY;
	String _propertyNames[] = { "minSamplesNeeded", "negativeReplacement" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.AGGREGATING;
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

		// Normally for average, output units will be the same as input.
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
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
//		debug2("AverageAlgorithm:afterTimeSlices, count=" + count);
//debug1("AverageAlgorithm:afterTimeSlices, per begin="
//+ debugSdf.format(_aggregatePeriodBegin) + ", end=" + debugSdf.format(_aggregatePeriodEnd));
		if (count >= minSamplesNeeded && count > 0)
		{
			double ave = tally / (double)count;
			
			// Added for HDB issue 386
			if (ave < 0.0 && negativeReplacement != Double.NEGATIVE_INFINITY)
				ave = negativeReplacement;
			else
				setOutput(average, ave);
		}
		else 
		{
			warning("Do not have minimum # samples (" + minSamplesNeeded
				+ ") -- not producing an average.");
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
