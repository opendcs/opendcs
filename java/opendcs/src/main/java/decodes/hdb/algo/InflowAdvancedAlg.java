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
import decodes.hdb.HdbFlags;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "This algorithm is an Advanced  mass balance calculation for inflow as: \n" +
"Delta Storage + Total Release + Delta Bank Storage + evaporation  \n" +
"- all incoming diversions + all outgoing diversions \n\n" +

"(up to 5 diversions of each type) \n" +

"If any of the input properties are set to \"fail\" then the inflow \n" +
"will not be calculated and/or the inflow will be deleted. \n\n" +

"If all of the inputs do not exist because of a delete the inflow will \n" +
"be deleted if the output exists regardless of the property settings. \n\n" +

"This algorithm written by M. Bogner, August 2008 \n" +
"Modified by M. Bogner May 2009 to add additional delete logic and version control")
public class InflowAdvancedAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double total_release;
	@Input
	public double delta_storage;
	@Input
	public double delta_bs;
	@Input
	public double evap;
	@Input
	public double diver_in1;
	@Input
	public double diver_in2;
	@Input
	public double diver_in3;
	@Input
	public double diver_in4;
	@Input
	public double diver_in5;
	@Input
	public double diver_out1;
	@Input
	public double diver_out2;
	@Input
	public double diver_out3;
	@Input
	public double diver_out4;
	@Input
	public double diver_out5;

// Version 1.0.03 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.03";
        boolean do_setoutput = true;
	double inflow_calculation = 0.0;

	@Output
	public NamedVariable inflow = new NamedVariable("inflow", 0);

	@PropertySpec(value = "ignore") 
	public String total_release_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String delta_storage_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String evap_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String delta_bs_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_in1_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_in2_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_in3_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_in4_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_in5_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_out1_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_out2_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_out3_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_out4_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String diver_out5_missing = "ignore";
	@PropertySpec(value = "") 
    public String validation_flag = "";

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
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
	   inflow_calculation = 0.0;
	   do_setoutput = true;
	   if (!isMissing(total_release))
             inflow_calculation = inflow_calculation + total_release;
	   if (!isMissing(delta_storage))
             inflow_calculation = inflow_calculation + delta_storage;
	   if (!isMissing(evap))
             inflow_calculation = inflow_calculation + evap;
	   if (!isMissing(delta_bs))
             inflow_calculation = inflow_calculation + delta_bs;
	   if (!isMissing(diver_in1))
             inflow_calculation = inflow_calculation - diver_in1;
	   if (!isMissing(diver_in2))
             inflow_calculation = inflow_calculation - diver_in2;
	   if (!isMissing(diver_in3))
             inflow_calculation = inflow_calculation - diver_in3;
	   if (!isMissing(diver_in4))
             inflow_calculation = inflow_calculation - diver_in4;
	   if (!isMissing(diver_in5))
             inflow_calculation = inflow_calculation - diver_in5;
	   if (!isMissing(diver_out1))
             inflow_calculation = inflow_calculation + diver_out1;
	   if (!isMissing(diver_out2))
             inflow_calculation = inflow_calculation + diver_out2;
	   if (!isMissing(diver_out3))
             inflow_calculation = inflow_calculation + diver_out3;
	   if (!isMissing(diver_out4))
             inflow_calculation = inflow_calculation + diver_out4;
	   if (!isMissing(diver_out5))
             inflow_calculation = inflow_calculation + diver_out5;
	   if (	
		isMissing(total_release) && isMissing(delta_storage)  && 
		isMissing(evap) && isMissing(delta_bs)  && 
		isMissing(diver_in1) && isMissing(diver_in2)  && isMissing(diver_in3) &&
		isMissing(diver_in4) && isMissing(diver_in5)  && 
		isMissing(diver_out1) && isMissing(diver_out2)  && isMissing(diver_out3) &&
		isMissing(diver_out4) && isMissing(diver_out5)
	   ) do_setoutput = false; 

	   if (do_setoutput)
	   {

		debug3("InflowAdvancedAlg-" + alg_ver + ": total_release=" + total_release +", delta_storage=" + delta_storage);

		/* added to allow users to automatically set the Validation column  */
		if (validation_flag.length() > 0) setHdbValidationFlag(inflow,validation_flag.charAt(1));
		setOutput(inflow,inflow_calculation);
	   }
	   else
	   {
		debug3("InflowAdvancedAlg-" + alg_ver + ": Deleting inflow");
		deleteOutput(inflow);
	   }
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
