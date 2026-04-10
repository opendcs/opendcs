/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import decodes.util.DecodesException;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

@Algorithm(description = "CopyAlgorithm copies a single 'input' parameter to a single 'output' parameter.\n" +
		" \n" +
		"Modified June 2009 By M. Bogner to add missing property for proper deletes and \n" +
		"a version")

public class CopyAlgorithm extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		{
			log.info("CopyAlgorithm, inUnits={}, outUnits={}", inUnits, outUnits);
		}
		else
		{
			log.debug("CopyAlgorithm, inUnits={}, outUnits={}", inUnits, outUnits);
		}

		if (comp.getProperty("input_EU") != null || comp.getProperty("output_EU") != null)
		{
			LoggingEventBuilder le = enhancedDebug ? log.atInfo() : log.atDebug();
			le.log("Will NOT do implicit unit conversion because unit properties are present.");
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
		LoggingEventBuilder le = enhancedDebug ? log.atInfo() : log.atTrace();
		le.log("CopyAlgorithm doAWTimeSlice input={}, inUnits={}, outUnits={}", input, inUnits, outUnits);

		double x = input;
		if (converter != null)
		{
			try { x = converter.convert(x); }
			catch(DecodesException ex)
			{
				log.atWarn().setCause(ex).log("Exception in converter.");
			}
		}
		x = (x*multiplier) + offset;
		setOutput(output, x);
		if (enhancedDebug && outUnits != null && outUnits.toLowerCase().contains("f") && x < 500.)
		{
			log.warn("Setting output to {} {}", x, outUnits);
		}
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