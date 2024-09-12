package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(
		description = "AverageAlgorithm averages single 'input' parameter to a single 'average' \n" +
				"parameter. The averaging period is determined by the interval of the output\n" +
				"parameter."
)

public class AverageAlgorithm extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final String AVERAGESTRING = "average";
	@Input
	double input;

	double tally;
	int count;
	PropertySpec specs[] =
	{
		new PropertySpec("negativeReplacement", PropertySpec.NUMBER, 
			"(no default) If set, and output would be negative, then replace with the number supplied.")
	};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return specs;
	}

	public double negativeReplacement = Double.NEGATIVE_INFINITY;

	@Output
	NamedVariable average = new NamedVariable(AVERAGESTRING, 0);

	@org.opendcs.annotations.PropertySpec(value = "1")
	public long minSamplesNeeded = 1;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = AVERAGESTRING;
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

		// Normally for average, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr(AVERAGESTRING, inUnits);
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
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
//		debug2("AverageAlgorithm:afterTimeSlices, count=" + count);
//debug1("AverageAlgorithm:afterTimeSlices, per begin="
//+ debugSdf.format(_aggregatePeriodBegin) + ", end=" + debugSdf.format(_aggregatePeriodEnd));
		if (count >= minSamplesNeeded && count > 0)
		{
			double ave = tally / (double)count;
			
			// Added for HDB issue 386
			if (ave < 0.0 && negativeReplacement != Double.NEGATIVE_INFINITY)
			{
				debug1("Computed average=" + ave + ", will use negativeReplacement="
					+ negativeReplacement);
				ave = negativeReplacement;
			}
			setOutput(average, ave);
		}
		else 
		{
			warning("Do not have minimum # samples (" + minSamplesNeeded
				+ ") -- not producing an average.");
			if (_aggInputsDeleted)
				deleteOutput(average);
		}
	}
}
