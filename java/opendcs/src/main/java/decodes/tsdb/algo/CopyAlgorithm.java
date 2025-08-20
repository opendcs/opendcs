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

import ilex.var.NamedVariable;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "CopyAlgorithm copies a single 'input' parameter to a single 'output' parameter.\n" +
		" \n" +
		"Modified June 2009 By M. Bogner to add missing property for proper deletes and \n" +
		"a version")

public class CopyAlgorithm extends AW_AlgorithmBase
{
	private static final String OUTPUTSTRING = "output";

	@Input
	public double input;

	private UnitConverter converter = null;
	private boolean enhancedDebug = false;
	private String inUnits = null;
	private String outUnits = null;

	@Output
	public NamedVariable output = new NamedVariable(OUTPUTSTRING, 0);

	@org.opendcs.annotations.PropertySpec(value = "1")
	public double multiplier = 1.0;
	@org.opendcs.annotations.PropertySpec(value = "0")
	public double offset = 0.0;
	@org.opendcs.annotations.PropertySpec(value = "null")
	public Double x = null;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		_aggPeriodVarRoleName = null;
		// No initialization needed for Copy.
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
	{
		enhancedDebug = false;
		ParmRef ref = getParmRef("input");
		String debugTsid = "DET.Elev-Forebay.Inst.0.0.MIXED-RAW";
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
		outUnits = getParmUnitsAbbr(OUTPUTSTRING);
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
				setOutputUnitsAbbr(OUTPUTSTRING, inUnits);
			}
		}
		// else inUnits is unknown. This shouldn't happen because tasklist records will
		// have a units assignment. In any case we can't do any conversions.
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
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
		// No post-timeslice code needed.
	}
}
