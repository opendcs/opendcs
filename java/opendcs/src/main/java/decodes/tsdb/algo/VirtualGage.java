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


@Algorithm(description = "Compute a virtual elevation at an intermediate point between two other gages.\n" +
"Inputs are the upstream and downstream elevation,\n" +
"Properties specify Upstream position (e.g. Mile number), Downstream position, and Virtual Gage position.\n" +
"The positions are required to do proper interpolation. Default values place the virtual gage halfway between up & downstream gages.\n" +
"If provided, you may set gagezero properties for each of the locations, thus the output can be in gage height or elevation.
")
public class VirtualGage
	extends decodes.tsdb.algo.AW_AlgorithmBase
{

	@Input
	public double upstreamGage;	//AW:TYPECODE=i
	@Input
	public double downstreamGage;	//AW:TYPECODE=i


	@Output
	public NamedVariable virtualGage = new NamedVariable("virtualGage", 0);

	@PropertySpec
	public double upstreamPosition = 0;
	@PropertySpec
	public double downstreamPosition = 10;
	@PropertySpec
	public double virtualPosition = 5;
	@PropertySpec
	public double upstreamGageZero = 0;
	@PropertySpec
	public double downstreamGageZero = 0;
	@PropertySpec
	public double virtualGageZero = 0;

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
		if (upstreamPosition != downstreamPosition)
		{
			double positionRatio = (virtualPosition - upstreamPosition)
				/ (downstreamPosition - upstreamPosition);
			double upstreamElevation = upstreamGage + upstreamGageZero;
			double downstreamElevation = downstreamGage + downstreamGageZero;
			double elevationRange = downstreamElevation - upstreamElevation;
			double virtualElevation = upstreamElevation + 
				elevationRange * positionRatio;
			setOutput(virtualGage, virtualElevation - virtualGageZero);
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
