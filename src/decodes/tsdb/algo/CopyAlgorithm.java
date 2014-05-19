/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb.algo;

import java.util.Date;

import ilex.util.Logger;
import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;

//AW:JAVADOC
/**
CopyAlgorithm copies a single 'input' parameter to a single 'output' parameter.
 
Modified June 2009 By M. Bogner to add missing property for proper deletes and 
a version
*/
//AW:JAVADOC_END
public class CopyAlgorithm extends AW_AlgorithmBase
{
//AW:INPUTS
	public double input;
	// AW will also define an array of the names like this:
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	String alg_ver = "1.0.01";
	double mult = 1.0;
	boolean multUsed = false;
	double offs = 0.0;
//AW:LOCALVARS_END

//AW:OUTPUTS
	// A single output named 'output':
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public String input_MISSING = "ignore";
	public String multiplier = "";
	public String offset = "";
	String _propertyNames[] = {"input_MISSING", "multiplier", "offset" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
		_aggPeriodVarRoleName = null;
//AW:INIT_END

//AW:USERINIT
		// No initialization needed for Copy.
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
	{
//AW:BEFORE_TIMESLICES
		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input");
		debug1("CopyAlgorithm, setting output units to '"
			+ inUnits + "'");
		if (inUnits != null && inUnits.length() > 0 && !inUnits.equalsIgnoreCase("unknown")
		 && getParmUnitsAbbr("output") == null)
			setOutputUnitsAbbr("output", inUnits);
		if (multiplier != null && multiplier.trim().length() > 0)
		{
			try
			{
				mult = Double.parseDouble(multiplier);
				multUsed = true;
			}
			catch(NumberFormatException ex)
			{
				warning("multiplier property must be a number -- ignored.");
				multUsed = false;
			}
		}
		if (offset != null && offset.trim().length() > 0)
		{
			try
			{
				offs = Double.parseDouble(offset);
			}
			catch(NumberFormatException ex)
			{
				warning("offset property must be a number -- ignored.");
				offs = 0.0;
			}
		}
///AW:BEFORE_TIMESLICES_END
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
		// The only thing needed is to copy the input to the output.
		debug3("CopyAlgorithm doAWTimeSlice- " + alg_ver + " input=" + input);
		double x = input;
		if (multUsed)
			x *= mult;
		x += offs;
		setOutput(output, x);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
//AW:AFTER_TIMESLICES
		// No post-timeslice code needed.
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
