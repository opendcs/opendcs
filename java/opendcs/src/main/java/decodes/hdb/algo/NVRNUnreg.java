package decodes.hdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
// this new import was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
 * Navajo Unregulated Inflow Computation
Sums Vallecito Delta Storage and Evaporation at t-1
adds it to Navajo Inflow to get Unregulated Inflow
Adds Azotea tunnel volume to Navajo Unregulated flow to get Mod Unregulated Flow
 */
//AW:JAVADOC_END
public class NVRNUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double VCRCDeltaStorage;	//AW:TYPECODE=i
	public double VCRCEvap;			//AW:TYPECODE=i
	public double NVRNInflow;			//AW:TYPECODE=i
	public double SJANVolume;			//AW:TYPECODE=i
	String _inputNames[] = { "VCRCDeltaStorage", "VCRCEvap", "NVRNInflow", "SJANVolume" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	public NamedVariable modunreg = new NamedVariable("modunreg", 0);
	String _outputNames[] = { "unreg", "modunreg" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String VCRCDeltaStorage_missing = "fail";
	public String VCRCEvap_missing 		= "fail";
	public String NVRNInflow_missing 		= "fail";
	public String SJANVolume_missing 		= "fail";
	String _propertyNames[] = { "VCRCDeltaStorage_missing", "VCRCEvap_missing", "NVRNInflow_missing", "SJANVolume_missing" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
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
		if (_sliceInputsDeleted) { //handle deleted values, since we have more than one output
			deleteAllOutputs();
			return;
		}
		double sum = VCRCDeltaStorage + VCRCEvap;
		
debug3("doAWTimeSlice, VCRCDeltaStorage="+VCRCDeltaStorage+" VCRCEvap="+VCRCEvap+
		" NVRNInflow="+NVRNInflow+" sum="+sum+" SJANVolume="+SJANVolume);

		setOutput(unreg, NVRNInflow + sum);
		setOutput(modunreg, NVRNInflow + sum + SJANVolume);

//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
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
