package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "SumOverTimeAlgorithm sums single 'input' parameter to a single 'sum' \n" +
"parameter. The summing period is determined by the interval of the output\n" +
"parameter.")
public class SumOverTimeAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;	//AW:TYPECODE=i

	double tally;
	int count;


	@Output(type = Double.class)
	public NamedVariable sum = new NamedVariable("sum", 0);

	@PropertySpec
	public long minSamplesNeeded = 1;

	// Allow javac to generate a no-args constructor.
	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "sum";
		aggUpperBoundClosed = true;
		aggLowerBoundClosed = false;
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
//		debug2("SumOverTime:doAWTimeSlice, input=" + input);
		if (!isMissing(input))
		{
			tally += input;
			count++;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
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
	}
}
