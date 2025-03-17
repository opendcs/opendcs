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

import decodes.hdb.HdbFlags;


@Algorithm(description = "This algorithm is an Side Inflow mass balance calculation for inflow as:\n" +  
"Delta Storage - Total Release Above + Total Release Below + evaporation\n\n" +  
"If inputs Delta Storage or Total Release Above or Total Release Below\n" + 
"or  ithe Evap do not exist or have been deleted and the\n" + 
"Delta_STORAGE_MISSING or the TOTAL_REL_ABOVE_MISSING or EVAP_MISSING,\n" + 
"or TOTAL_REL_BELOW_MISSING properties are set to "fail" then the\n" +  
"inflow will not be calculated and/or the inflow will be deleted.\n\n" + 
"If all of the inputs do not exist because of a delete the inflow will\n" +  
"be deleted if the output exists regardless of the property settings.\n\n" + 
"This algorithm written by M. Bogner, August 2008\n" + 
"Modified by M. Bogner May 2009 to add additional delete logic and version control\n")
public class SideInflowAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double total_rel_above;
	@Input
	public double total_rel_below;
	@Input
	public double delta_storage;
	@Input
	public double evap;

// Version 1.0.03 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.03";
        boolean do_setoutput = true;
	double inflow_calculation = 0.0;

	@Output(type = Double.class)
	public NamedVariable side_inflow = new NamedVariable("side_inflow", 0);

	@PropertySpec(value = "ignore") 
	public String total_rel_above_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String total_rel_below_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String delta_storage_missing = "ignore";
	@PropertySpec(value = "ignore") 
	public String evap_missing = "ignore";
	@PropertySpec(value = "") 
    public String validation_flag = "";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
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
	protected void doAWTimeSlice()
		throws DbCompException
	{
	inflow_calculation = 0.0;
	do_setoutput = true;
	if (!isMissing(total_rel_above))
		inflow_calculation = inflow_calculation - total_rel_above;
	if (!isMissing(total_rel_below))
		inflow_calculation = inflow_calculation + total_rel_below;
	if (!isMissing(delta_storage))
		inflow_calculation = inflow_calculation + delta_storage;
	if (!isMissing(evap))
		inflow_calculation = inflow_calculation + evap;

        // now test existence of inputs and their missing flags and setoutput accordingly
	if (isMissing(total_rel_above) && isMissing(total_rel_below) && isMissing(delta_storage)  && isMissing(evap)) do_setoutput = false;
	if (isMissing(total_rel_above) && total_rel_above_missing.equalsIgnoreCase("fail")) do_setoutput = false;
	if (isMissing(total_rel_below) && total_rel_below_missing.equalsIgnoreCase("fail")) do_setoutput = false;
	if (isMissing(delta_storage) && delta_storage_missing.equalsIgnoreCase("fail")) do_setoutput = false;
	if (isMissing(evap) && evap_missing.equalsIgnoreCase("fail")) do_setoutput = false;

	if (do_setoutput)
	{
		debug3("SideInflowAlg-" + alg_ver + ": total_releaseabove=" + total_rel_above +", delta_storage=" + delta_storage);
		/* added to allow users to automatically set the Validation column  */
		if (validation_flag.length() > 0) setHdbValidationFlag(side_inflow,validation_flag.charAt(1));
		setOutput(side_inflow,inflow_calculation);
	}
	else
	{
		debug3("SideInflowAlg-" + alg_ver + ": Deleting side_inflow output");
		deleteOutput(side_inflow);
	}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
	}
}
