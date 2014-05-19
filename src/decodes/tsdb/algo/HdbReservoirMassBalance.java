package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;


//AW:IMPORTS

//AW:IMPORTS_END

//AW:JAVADOC
/**
Implements a reservoir mass balance
Output is inflow.
<p>Inputs are:
<ul>
<li>dStor (delta Storage)</li>
<li>rel (total release from reservoir)</li>
<li>bStor (delta Bank Storage)</li>
<li>evap (net reservoir evaporation)</li>
<li>div (diversion)</li>
</ul>
Any of these can be set to be optional by setting a property.
Storage, evaporation, and bank storage should be in units of acre-ft.
Rest in units of cfs.

<p>Properties include: 
<ul> 
<li>dBStor_MISSING - whether delta bank storage is required (set to fail)
</li>
<li>evap_MISSING - whether evaporation is required (set to fail)
</li>
<li>div_MISSING - whether diversion is required (set to fail)
</li>
</ul>
 */
//AW:JAVADOC_END
public class HdbReservoirMassBalance
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	double dStor;	//AW:TYPECODE=id
	double rel;	//AW:TYPECODE=i
	double dBStor;	//AW:TYPECODE=id
	double evap;	//AW:TYPECODE=i
	double div;	//AW:TYPECODE=i
	
	String _inputNames[] = { "dStor", "dBStor", "evap", "rel", "div" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	NamedVariable inflow = new NamedVariable("inflow", 0);
	String _outputNames[] = { "inflow" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	String dBStor_MISSING = "ignore";
	String evap_MISSING = "ignore";
	String div_MISSING = "ignore";
	String _propertyNames[] = { "dBStor_MISSING", "evap_MISSING", "div_MISSING" };
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
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
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
		// inflow = S(t) - S(t-1) + O + E + (BS(t) - BS(t-1)) + D
		// or change in storage, release, evap, bank storage and diversion

		
		double in = dStor/1.98347;
		in += rel;

		// only reason for these to not be missing is if they are required
		if(!isMissing(evap))	in += evap/1.98347;
		if(!isMissing(dBStor))	in += dBStor/1.98347;
		if(!isMissing(div))		in += div;
		
		setOutput(inflow, in);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
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
