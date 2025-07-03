package decodes.tsdb.algo;

import decodes.tsdb.DbCompException;
import ilex.var.NamedVariable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Calculate Weighted Average values (eg Water Temperature) using: sum(input1n*input2n) / total2")
public class WeightedAverage
	extends AW_AlgorithmBase
{
	@Input
	public double weightTotal;
	@Input
	public double input1;
	@Input
	public double input2;
	@Input
	public double input3;
	@Input
	public double input4;
	@Input
	public double input5;
	@Input
	public double input6;
	@Input
	public double input7;
	@Input
	public double input8;
	@Input
	public double weight1;
	@Input
	public double weight2;
	@Input
	public double weight3;
	@Input
	public double weight4;
	@Input
	public double weight5;
	@Input
	public double weight6;
	@Input
	public double weight7;
	@Input
	public double weight8;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);

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
		if(weightTotal > 0)
		{
			double tot = input1*weight1;
			tot += input2*weight2;
			tot += input3*weight3;
			tot += input4*weight4;
			tot += input5*weight5;
			tot += input6*weight6;
			tot += input7*weight7;
			tot += input8*weight8;

			setOutput(output,tot/weightTotal);
		}

	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
		throws DbCompException
	{
	}
}
