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

@Algorithm(description = "Navajo Unregulated Inflow Computation \n" +
"Sums Vallecito Delta Storage and Evaporation at t-1 \n" +
"adds it to Navajo Inflow to get Unregulated Inflow \n" +
"Adds Azotea tunnel volume to Navajo Unregulated flow to get Mod Unregulated Flow")
public class NVRNUnreg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double VCRCDeltaStorage;
	@Input
	public double VCRCEvap;
	@Input
	public double NVRNInflow;
	@Input
	public double SJANVolume;

	@Output(type = Double.class)
	public NamedVariable unreg = new NamedVariable("unreg", 0);
	@Output(type = Double.class)
	public NamedVariable modunreg = new NamedVariable("modunreg", 0);

	@PropertySpec(value = "fail")
	public String VCRCDeltaStorage_missing = "fail";
	@PropertySpec(value = "fail")
	public String VCRCEvap_missing 		= "fail";
	@PropertySpec(value = "fail")
	public String NVRNInflow_missing 		= "fail";
	@PropertySpec(value = "fail")
	public String SJANVolume_missing 		= "fail";

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
	protected void doAWTimeSlice()
		throws DbCompException
	{
		if (_sliceInputsDeleted) { //handle deleted values, since we have more than one output
			deleteAllOutputs();
			return;
		}
		double sum = VCRCDeltaStorage + VCRCEvap;
		
debug3("doAWTimeSlice, VCRCDeltaStorage="+VCRCDeltaStorage+" VCRCEvap="+VCRCEvap+
		" NVRNInflow="+NVRNInflow+" sum="+sum+" SJANVolume="+SJANVolume);

		setOutput(unreg, NVRNInflow + sum);
		setOutput(modunreg, NVRNInflow + sum + SJANVolume);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
