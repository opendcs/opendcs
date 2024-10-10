package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Divide one time series with another using : (a*input1+b)/(c*input2+d). \n" +
		"If the denominator is zero then output is not set ")

public class Division extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input1;	//AW:TYPECODE=i
	@Input
	public double input2;	//AW:TYPECODE=i

	double numerator;
	double denominator;

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec(value = "1.0")
	public double a = 1.0;
	@PropertySpec(value = "0")
	public double b = 0;
	@PropertySpec(value = "1.0")
	public double c = 1.0;
	@PropertySpec(value = "0")
	public double d = 0;

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
		numerator = a*input1+b;
		denominator = c*input2 + d;
		if(denominator > 0) 
			setOutput(output,numerator/denominator);
		//else
			//setOutput(output,undefined);
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
