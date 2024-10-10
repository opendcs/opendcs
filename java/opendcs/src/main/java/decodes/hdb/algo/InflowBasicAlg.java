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
import decodes.hdb.HdbFlags;
import java.sql.Connection;
import java.text.SimpleDateFormat;

//AW:IMPORTS_END

//AW:JAVADOC
/**
This algorithm is an Basic mass balance calculation for inflow as:  
Delta Storage + Total Release 

If inputs Delta Storage or Total Release do not exist or have been
deleted and the Delta_STORAGE_MISSING or the TOTAL_RELEASE_MISSING
properties are set to "fail" then the inflow will not be calculated
and/or the inflow will be deleted.

If all of the inputs do not exist because of a delete the inflow will 
be deleted if the output exists regardless of the property settings.

This algorithm written by M. Bogner, August 2008
Modified by M. Bogner May 2009 to add additional delete logic and version control

 */
//AW:JAVADOC_END
public class InflowBasicAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double total_release;	//AW:TYPECODE=i
	public double delta_storage;	//AW:TYPECODE=i
	String _inputNames[] = { "total_release", "delta_storage"};
//AW:INPUTS_END

//AW:LOCALVARS
// Version 1.0.04 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.04";
        boolean do_setoutput = true;
	double inflow_calculation = 0.0;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable inflow = new NamedVariable("inflow", 0);
	String _outputNames[] = { "inflow" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String total_release_missing = "ignore";
	public String delta_storage_missing = "ignore";
        public String validation_flag = "";
	String _propertyNames[] = { "total_release_missing", "delta_storage_missing", 
	"validation_flag" };
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
	 * @throw DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
	inflow_calculation = 0.0;
	do_setoutput = true;
	if (!isMissing(total_release))
		inflow_calculation = inflow_calculation + total_release;
	if (!isMissing(delta_storage))
		inflow_calculation = inflow_calculation + delta_storage;

	// now test existence of inputs and their missing flags and setoutput accordingly
	if (isMissing(total_release) && isMissing(delta_storage)) do_setoutput = false;
	if (isMissing(total_release) && total_release_missing.equalsIgnoreCase("fail")) do_setoutput = false;
	if (isMissing(delta_storage) && delta_storage_missing.equalsIgnoreCase("fail")) do_setoutput = false;

	if (do_setoutput)
	{
		debug3("InflowBasicAlg-" + alg_ver + ": total_release=" + total_release +", delta_storage=" + delta_storage);
		/* added to allow users to automatically set the Validation column  */
		if (validation_flag.length() > 0) setHdbValidationFlag(inflow,validation_flag.charAt(1));
		setOutput(inflow,inflow_calculation);
	}
	else
	{
		debug3("InflowBasicAlg-" + alg_ver + ": Deleting inflow output");
		deleteOutput(inflow);
	}

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
