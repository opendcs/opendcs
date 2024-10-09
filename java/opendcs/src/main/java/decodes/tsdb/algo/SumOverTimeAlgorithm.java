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
SumOverTimeAlgorithm sums single 'input' parameter to a single 'sum' 
parameter. The summing period is determined by the interval of the output
parameter.

 */
//AW:JAVADOC_END
public class SumOverTimeAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	double tally;
	int count;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable sum = new NamedVariable("sum", 0);
	String _outputNames[] = { "sum" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public long minSamplesNeeded = 1;
	String _propertyNames[] = { "minSamplesNeeded" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.
	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "sum";
		aggUpperBoundClosed = true;
		aggLowerBoundClosed = false;
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
//		debug2("SumOverTime:doAWTimeSlice, input=" + input);
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
		if (count >= minSamplesNeeded)
		{
			setOutputUnitsAbbr("sum", getInputUnitsAbbr("input"));
			setOutput(sum, tally);
		}
		else 
		{
			warning("Do not have minimum # samples (" + minSamplesNeeded
				+ ") -- not producing a sum.");
			if (_aggInputsDeleted)
				deleteOutput(sum);
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
