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
Lake Powell Unregulated Inflow Computation
Sums Delta Storages and Evaporations at the indicated timestep from these reservoirs
Fontenelle (t-8)
Flaming Gorge (t-6)
Blue Mesa (t-4)
Navajo (t-5)
Vallecito (t-6)

Also adds Taylor Park Delta Storage from t-5, and
Flaming Gorge Delta Bank Storage from t-6

adds it to Lake Powell Inflow to get Unregulated Inflow
 */
//AW:JAVADOC_END
public class GLDAUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double FTRWDeltaStorage;	//AW:TYPECODE=i
	public double FTRWEvap;			//AW:TYPECODE=i
	public double FLGUDeltaStorage;	//AW:TYPECODE=i
	public double FLGUEvap;			//AW:TYPECODE=i
	public double FLGUDeltaBS;			//AW:TYPECODE=i
	public double VCRCDeltaStorage;	//AW:TYPECODE=i
	public double VCRCEvap;			//AW:TYPECODE=i
	public double TPRCDeltaStorage;	//AW:TYPECODE=i
	public double NVRNDeltaStorage;	//AW:TYPECODE=i
	public double NVRNEvap;			//AW:TYPECODE=i
	public double BMDCDeltaStorage;	//AW:TYPECODE=i
	public double BMDCEvap;			//AW:TYPECODE=i
	public double GLDAInflow;			//AW:TYPECODE=i
	
	String _inputNames[] = { "FTRWDeltaStorage", "FTRWEvap", "FLGUDeltaStorage", "FLGUEvap", "FLGUDeltaBS",
						     "VCRCDeltaStorage", "VCRCEvap", "TPRCDeltaStorage",
			                 "NVRNDeltaStorage", "NVRNEvap", "BMDCDeltaStorage", "BMDCEvap", "GLDAInflow" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	String _outputNames[] = { "unreg" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String FTRWDeltaStorage_missing = "fail";
	public String FTRWEvap_missing 		= "fail";
	public String FLGUDeltaStorage_missing = "fail";
	public String FLGUEvap_missing 		= "fail";
	public String FLGUDeltaBS_missing 		= "fail";
	public String VCRCDeltaStorage_missing = "fail";
	public String VCRCEvap_missing 		= "fail";
	public String TPRCDeltaStorage_missing = "fail";
	public String NVRNDeltaStorage_missing = "fail";
	public String NVRNEvap_missing 		= "fail";
	public String BMDCDeltaStorage_missing = "fail";
	public String BMDCEvap_missing 		= "fail";
	public String GLDAInflow_missing 		= "fail";
	String _propertyNames[] = { "FTRWDeltaStorage_missing", "FTRWEvap_missing", "FLGUDeltaStorage_missing", "FLGUEvap_missing",
				 			  "FLGUDeltaBS_missing", "VCRCDeltaStorage_missing", "VCRCEvap_missing", "TPRCDeltaStorage_missing",
				 			  "NVRNDeltaStorage_missing", "NVRNEvap_missing", "BMDCDeltaStorage_missing", "BMDCEvap_missing", "GLDAInflow_missing"};
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
		double sum = 0.0;
		sum = FTRWDeltaStorage + FTRWEvap;
		sum += FLGUDeltaStorage + FLGUEvap + FLGUDeltaBS;
		sum += VCRCDeltaStorage + VCRCEvap;
		sum += TPRCDeltaStorage;
		sum += NVRNDeltaStorage + NVRNEvap;
		sum += BMDCDeltaStorage + BMDCEvap;
		
debug3("doAWTimeSlice, sum="+sum+" unreg="+ (GLDAInflow + sum));

		setOutput(unreg, GLDAInflow + sum);
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
