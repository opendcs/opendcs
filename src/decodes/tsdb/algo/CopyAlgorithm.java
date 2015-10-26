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
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;

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
	private UnitConverter converter = null;
	private boolean enhancedDebug = false;
	private String inUnits = null;
	private String outUnits = null;
	private String debugTsid = "DET.Elev-Forebay.Inst.0.0.MIXED-RAW";
	
	private PropertySpec copyPropertySpecs[] = 
	{
		new PropertySpec("multiplier", PropertySpec.NUMBER,
			"(optional) Multiply input by this amount."),
		new PropertySpec("offset", PropertySpec.NUMBER,
			"(optional) Add this to input (after applying multiplier, if specified).")
	};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return copyPropertySpecs;
	}

//AW:LOCALVARS_END

//AW:OUTPUTS
	// A single output named 'output':
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double multiplier = 1.0;
	public double offset = 0.0;
	public Double x = null;
	String _propertyNames[] = {"multiplier", "offset", "x" };
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
		enhancedDebug = false;
		ParmRef ref = getParmRef("input");
		if (ref != null && ref.timeSeries != null 
		 && ref.timeSeries.getTimeSeriesIdentifier() != null
		 && ref.timeSeries.getTimeSeriesIdentifier().getUniqueString().toLowerCase().contains(
			 debugTsid))
		{
			enhancedDebug = true;
		}

		// Normally for copy, output units will be the same as input.
		converter = null;
		inUnits = getInputUnitsAbbr("input");
		outUnits = getParmUnitsAbbr("output");
		if (enhancedDebug)
			info("CopyAlgorithm, inUnits=" + inUnits + ", outUnits=" + outUnits);
		else
			debug1("CopyAlgorithm, inUnits=" + inUnits + ", outUnits=" + outUnits);
		
		if (comp.getProperty("input_EU") != null || comp.getProperty("output_EU") != null)
		{
			if (enhancedDebug)
				info("Will NOT do implicit unit conversion because unit properties are present.");
			else
				debug1("Will NOT do implicit unit conversion because unit properties are present.");
		}
		else if (inUnits != null && inUnits.length() > 0 && !inUnits.equalsIgnoreCase("unknown"))
		{
			// input units are known.
			if (outUnits != null && outUnits.length() > 0 && !outUnits.equalsIgnoreCase("unknown"))
			{
				// output units are also known.
				if (!inUnits.equalsIgnoreCase(outUnits))
				{
					// We need to convert inputs to the output before copy.
					EngineeringUnit inEU =	EngineeringUnit.getEngineeringUnit(inUnits);
					EngineeringUnit outEU = EngineeringUnit.getEngineeringUnit(outUnits);
					converter = Database.getDb().unitConverterSet.get(inEU, outEU);
				}
			}
			else //output units are unknown. Set them to whatever the inUnits is.
			{
				setOutputUnitsAbbr("output", inUnits);
			}
		}
		// else inUnits is unknown. This shouldn't happen because tasklist records will
		// have a units assignment. In any case we can't do any conversions.

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
		String msg = "CopyAlgorithm doAWTimeSlice input=" + input + ", inUnits=" + inUnits
			+ ", outUnits=" + outUnits;
		if (enhancedDebug)
			info(msg);
		else
			debug3(msg);
		
		double x = input;
		if (converter != null)
		{
			try { x = converter.convert(x); }
			catch(DecodesException ex)
			{
				warning("Exception in converter: " + ex);
			}
		}
		x = (x*multiplier) + offset;
		setOutput(output, x);
		if (enhancedDebug && outUnits != null && outUnits.toLowerCase().contains("f") && x < 500.)
			warning("Setting output to " + x + " " + outUnits);
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

	@Override
	public String getBriefDescription()
	{
		return "Copies input to output with optional multiplier and offset.";
	}
	
}
