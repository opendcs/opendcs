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


@Algorithm(description = "Implements the USGS Equation:\n" +
"output = A * (B + input)^C + D\n" +
"where A, B, C, and D are constants provided as properties.\n" +
"Defaults: A=1, B=0, C=1, D=0")
public class UsgsEquation
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;	//AW:TYPECODE=i

	@Output
	public NamedVariable output = new NamedVariable("output", 0);


	@PropertySpec
	public double A = 1;
	@PropertySpec
	public double B = 0;
	@PropertySpec
	public double C = 1;
	@PropertySpec
	public double D = 0;

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
		setOutput(output, A * Math.pow(B + input, C) + D);
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
