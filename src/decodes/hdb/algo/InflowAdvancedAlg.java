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

//AW:IMPORTS_END

//AW:JAVADOC
/**
This algorithm is an Advanced  mass balance calculation for inflow as: 
Delta Storage + Total Release + Delta Bank Storage + evaporation 
- all incoming diversions + all outgoing diversions

(up to 5 diversions of each type)

If any of the input properties are set to "fail" then the inflow 
will not be calculated and/or the inflow will be deleted.

If all of the inputs do not exist because of a delete the inflow will 
be deleted if the output exists regardless of the property settings.

This algorithm written by M. Bogner, August 2008
Modified by M. Bogner May 2009 to add additional delete logic and version control

 */
//AW:JAVADOC_END
public class InflowAdvancedAlg extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double total_release;	//AW:TYPECODE=i
	public double delta_storage;	//AW:TYPECODE=i
	public double delta_bs;	//AW:TYPECODE=i
	public double evap;		//AW:TYPECODE=i
	public double diver_in1;	//AW:TYPECODE=i
	public double diver_in2;	//AW:TYPECODE=i
	public double diver_in3;	//AW:TYPECODE=i
	public double diver_in4;	//AW:TYPECODE=i
	public double diver_in5;	//AW:TYPECODE=i
	public double diver_out1;	//AW:TYPECODE=i
	public double diver_out2;	//AW:TYPECODE=i
	public double diver_out3;	//AW:TYPECODE=i
	public double diver_out4;	//AW:TYPECODE=i
	public double diver_out5;	//AW:TYPECODE=i
	String _inputNames[] = {"total_release","delta_storage","delta_bs","evap",
			  	"diver_in1","diver_in2","diver_in3","diver_in4","diver_in5",
				"diver_out1","diver_out2","diver_out3","diver_out4","diver_out5"} ;
//AW:INPUTS_END

//AW:LOCALVARS
// Version 1.0.03 was added by M. Bogner Aug 2012 for the 3.0 CP upgrade project
	String alg_ver = "1.0.03";
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
	public String evap_missing = "ignore";
	public String delta_bs_missing = "ignore";
	public String diver_in1_missing = "ignore";
	public String diver_in2_missing = "ignore";
	public String diver_in3_missing = "ignore";
	public String diver_in4_missing = "ignore";
	public String diver_in5_missing = "ignore";
	public String diver_out1_missing = "ignore";
	public String diver_out2_missing = "ignore";
	public String diver_out3_missing = "ignore";
	public String diver_out4_missing = "ignore";
	public String diver_out5_missing = "ignore";
        public String validation_flag = "";
 
	String _propertyNames[] = { "total_release_missing", "delta_storage_missing", "validation_flag",
		"evap_missing","delta_bs_missing","diver_in1_missing","diver_in2_missing","diver_in3_missing",
		"diver_in4_missing","diver_in5_missing","diver_out1_missing","diver_out2_missing",
		"diver_out3_missing","diver_out4_missing","diver_out5_missing" };
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
