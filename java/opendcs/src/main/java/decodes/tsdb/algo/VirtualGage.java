package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Compute a virtual elevation at an intermediate point between two other gages.
Inputs are the upstream and downstream elevation,
Properties specify Upstream position (e.g. Mile number), Downstream position, and Virtual Gage position.
The positions are required to do proper interpolation. Default values place the virtual gage halfway between up & downstream gages.
If provided, you may set gagezero properties for each of the locations, thus the output can be in gage height or elevation.

 */
//AW:JAVADOC_END
public class VirtualGage
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double upstreamGage;	//AW:TYPECODE=i
	public double downstreamGage;	//AW:TYPECODE=i
	String _inputNames[] = { "upstreamGage", "downstreamGage" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable virtualGage = new NamedVariable("virtualGage", 0);
	String _outputNames[] = { "virtualGage" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double upstreamPosition = 0;
	public double downstreamPosition = 10;
	public double virtualPosition = 5;
	public double upstreamGageZero = 0;
	public double downstreamGageZero = 0;
	public double virtualGageZero = 0;
	String _propertyNames[] = { "upstreamPosition", "downstreamPosition", "virtualPosition", "upstreamGageZero", "downstreamGageZero", "virtualGageZero" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
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
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
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
