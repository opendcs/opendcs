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
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Lake Powell Unregulated Inflow Computation \n" +
"Sums Delta Storages and Evaporations at the indicated timestep from these reservoirs \n" +
"Fontenelle (t-8) \n" +
"Flaming Gorge (t-6) \n" +
"Blue Mesa (t-4) \n" +
"Navajo (t-5) \n" +
"Vallecito (t-6) \n\n" +

"Also adds Taylor Park Delta Storage from t-5, and \n" +
"Flaming Gorge Delta Bank Storage from t-6 \n\n" +

"adds it to Lake Powell Inflow to get Unregulated Inflow")
public class GLDAUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double FTRWDeltaStorage;
	@Input
	public double FTRWEvap;
	@Input
	public double FLGUDeltaStorage;
	@Input
	public double FLGUEvap;
	@Input
	public double FLGUDeltaBS;
	@Input
	public double VCRCDeltaStorage;
	@Input
	public double VCRCEvap;
	@Input
	public double TPRCDeltaStorage;
	@Input
	public double NVRNDeltaStorage;
	@Input
	public double NVRNEvap;
	@Input
	public double BMDCDeltaStorage;
	@Input
	public double BMDCEvap;
	@Input
	public double GLDAInflow;

	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);

	@PropertySpec(value = "fail") 
	public String FTRWDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String FTRWEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String FLGUDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String FLGUEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String FLGUDeltaBS_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String VCRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String VCRCEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String TPRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String NVRNDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String NVRNEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String BMDCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail") 
	public String BMDCEvap_missing 		= "fail";
	@PropertySpec(value = "fail") 
	public String GLDAInflow_missing 		= "fail";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
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
		double sum = 0.0;
		sum = FTRWDeltaStorage + FTRWEvap;
		sum += FLGUDeltaStorage + FLGUEvap + FLGUDeltaBS;
		sum += VCRCDeltaStorage + VCRCEvap;
		sum += TPRCDeltaStorage;
		sum += NVRNDeltaStorage + NVRNEvap;
		sum += BMDCDeltaStorage + BMDCEvap;
		
debug3("doAWTimeSlice, sum="+sum+" unreg="+ (GLDAInflow + sum));

		setOutput(unreg, GLDAInflow + sum);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
