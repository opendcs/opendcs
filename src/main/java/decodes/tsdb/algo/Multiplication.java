package decodes.tsdb.algo;

import decodes.tsdb.DbCompException;
import ilex.var.NamedVariable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Multiple one time series with another using : (a*input1+b)*(c*input2+d).")
public class Multiplication
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input1;
	@Input
	public double input2;


	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double a = 1.0;
	@org.opendcs.annotations.PropertySpec(value="0")
	public double b = 0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double c = 1.0;
	@org.opendcs.annotations.PropertySpec(value="0")
	public double d = 0;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;

	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		setOutput(output, (a*input1+b) * (c*input2+d));
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
	}

}
