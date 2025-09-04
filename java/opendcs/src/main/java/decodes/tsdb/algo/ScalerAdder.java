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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.tsdb.DbCompException;
import decodes.tsdb.MissingAction;
import decodes.tsdb.ParmRef;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Takes up to 10 input values labeled input1 ... input10. Multiplies
them by coefficients supplied in properties coeff1 ... coeff10.
Adds them together and produces a single output labeled 'output'.
Values not assigned by computation are ignored.
All coefficients default to 1.0 if not supplied.
 */
//AW:JAVADOC_END
public class ScalerAdder extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
//AW:INPUTS
	public double input1;	//AW:TYPECODE=i
	public double input2;	//AW:TYPECODE=i
	public double input3;	//AW:TYPECODE=i
	public double input4;	//AW:TYPECODE=i
	public double input5;	//AW:TYPECODE=i
	public double input6;	//AW:TYPECODE=i
	public double input7;	//AW:TYPECODE=i
	public double input8;	//AW:TYPECODE=i
	public double input9;	//AW:TYPECODE=i
	public double input10;//AW:TYPECODE=i
	String _inputNames[] = { "input1", "input2", "input3", "input4", "input5",
			"input6", "input7", "input8", "input9", "input10" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double coeff1 = 1.0;
	public double coeff2 = 1.0;
	public double coeff3 = 1.0;
	public double coeff4 = 1.0;
	public double coeff5 = 1.0;
	public double coeff6 = 1.0;
	public double coeff7 = 1.0;
	public double coeff8 = 1.0;
	public double coeff9 = 1.0;
	public double coeff10 = 1.0;
	public String input1_MISSING = "ignore";
	public String input2_MISSING = "ignore";
	public String input3_MISSING = "ignore";
	public String input4_MISSING = "ignore";
	public String input5_MISSING = "ignore";
	public String input6_MISSING = "ignore";
	public String input7_MISSING = "ignore";
	public String input8_MISSING = "ignore";
	public String input9_MISSING = "ignore";
	public String input10_MISSING = "ignore";
	String _propertyNames[] = { "coeff1", "coeff2", "coeff3", "coeff4", "coeff5",
			"coeff6", "coeff7", "coeff8", "coeff9", "coeff10",
			"input1_MISSING", "input2_MISSING", "input3_MISSING", "input4_MISSING", "input5_MISSING",
			"input6_MISSING", "input7_MISSING", "input8_MISSING", "input9_MISSING", "input10_MISSING" };
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
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
//AW:TIMESLICE
		double tally = 0.0;
		ParmRef pr = null;
		String t = null;
		if (((pr = getParmRef(t = "input1")) != null && isAssigned(t) && isMissing(input1) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input2")) != null && isAssigned(t) && isMissing(input2) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input3")) != null && isAssigned(t) && isMissing(input3) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input4")) != null && isAssigned(t) && isMissing(input4) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input5")) != null && isAssigned(t) && isMissing(input5) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input6")) != null && isAssigned(t) && isMissing(input6) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input7")) != null && isAssigned(t) && isMissing(input7) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input8")) != null && isAssigned(t) && isMissing(input8) &&
			pr.missingAction != MissingAction.IGNORE)
		 || ((pr = getParmRef(t = "input0")) != null && isAssigned(t) && isMissing(input9) &&
			pr.missingAction != MissingAction.IGNORE))
		{
			log.trace("Skipping time slice with base time {} because of missing value for param {}",
					  _timeSliceBaseTime, t);
			return;
		}

		if (!isMissing(input1))
			tally += (input1 * coeff1);
		if (!isMissing(input2))
			tally += (input2 * coeff2);
		if (!isMissing(input3))
			tally += (input3 * coeff3);
		if (!isMissing(input4))
			tally += (input4 * coeff4);
		if (!isMissing(input5))
			tally += (input5 * coeff5);
		if (!isMissing(input6))
			tally += (input6 * coeff6);
		if (!isMissing(input7))
			tally += (input7 * coeff7);
		if (!isMissing(input8))
			tally += (input8 * coeff8);
		if (!isMissing(input9))
			tally += (input9 * coeff9);
		if (!isMissing(input10))
			tally += (input10 * coeff10);
		log.atTrace()
		   .addKeyValue("input1", input1).addKeyValue("coeff1", coeff1)
		   .addKeyValue("input2", input2).addKeyValue("coeff2", coeff2)
		   .addKeyValue("input3", input3).addKeyValue("coeff3", coeff3)
		   .addKeyValue("input4", input4).addKeyValue("coeff4", coeff4)
		   .addKeyValue("input5", input5).addKeyValue("coeff5", coeff5)
		   .addKeyValue("input6", input6).addKeyValue("coeff6", coeff6)
		   .addKeyValue("input7", input7).addKeyValue("coeff7", coeff7)
		   .addKeyValue("input8", input8).addKeyValue("coeff8", coeff8)
		   .addKeyValue("input9", input9).addKeyValue("coeff9", coeff9)
		   .addKeyValue("input10", input10).addKeyValue("coeff10", coeff10)
		   .log("tally={}", tally);
		setOutput(output, tally);
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
