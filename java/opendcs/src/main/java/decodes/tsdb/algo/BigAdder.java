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

import decodes.tsdb.DbCompException;
import ilex.var.NamedVariable;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
This algorithm is a modification of Scaler adder
Created by M. Bogner   May 2009
 */
@Algorithm(
		description ="Takes up to 20 input values labeled input1 ... input20. Multiplies\n" +
				"them by coefficients supplied in properties coeff1 ... coeff20.\n" +
				"Adds them together and produces a single output labeled 'output'.\n" +
				"The property input_constant is added to the ouput result.\n" +
				"input_constant defaults to 0 if not supplied\n" +
				"Values not assigned by computation are ignored.\n" +
				"All coefficients default to 1.0 if not supplied." )
public class BigAdder extends decodes.tsdb.algo.AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input1;
	@Input
	public double input2;
	@Input
	public double input3;
	@Input
	public double input4;
	@Input
	public double input5;
	@Input
	public double input6;
	@Input
	public double input7;
	@Input
	public double input8;
	@Input
	public double input9;
	@Input
	public double input10;
	@Input
	public double input11;
	@Input
	public double input12;
	@Input
	public double input13;
	@Input
	public double input14;
	@Input
	public double input15;
	@Input
	public double input16;
	@Input
	public double input17;
	@Input
	public double input18;
	@Input
	public double input19;
	@Input
	public double input20;

	String alg_ver = "1.0.2";

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@org.opendcs.annotations.PropertySpec(value="0.0")
	public double input_constant = 0.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff1 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff2 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff3 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff4 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff5 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff6 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff7 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff8 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff9 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff10 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff11 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff12 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff13 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff14 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff15 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff16 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff17 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff18 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff19 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="1.0")
	public double coeff20 = 1.0;
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input1_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input2_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input3_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input4_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input5_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input6_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input7_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input8_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input9_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input10_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input11_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input12_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input13_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input14_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input15_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input16_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input17_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input18_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input19_MISSING = "ignore";
	@org.opendcs.annotations.PropertySpec(value="ignore")
	public String input20_MISSING = "ignore";


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
	 * @throws DbCompException (or subclass thereof) if execution of this
	 *        algorithm is to be aborted.
	 */
	protected void doAWTimeSlice()
		throws DbCompException
	{
		double tally = input_constant;
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
		if (!isMissing(input11))
			tally += (input11 * coeff11);
		if (!isMissing(input12))
			tally += (input12 * coeff12);
		if (!isMissing(input13))
			tally += (input13 * coeff13);
		if (!isMissing(input14))
			tally += (input14 * coeff14);
		if (!isMissing(input15))
			tally += (input15 * coeff15);
		if (!isMissing(input16))
			tally += (input16 * coeff16);
		if (!isMissing(input17))
			tally += (input17 * coeff17);
		if (!isMissing(input18))
			tally += (input18 * coeff18);
		if (!isMissing(input19))
			tally += (input19 * coeff19);
		if (!isMissing(input20))
			tally += (input20 * coeff20);
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
		   .addKeyValue("input11", input11).addKeyValue("coeff11", coeff11)
		   .addKeyValue("input12", input12).addKeyValue("coeff12", coeff12)
		   .addKeyValue("input13", input13).addKeyValue("coeff13", coeff13)
		   .addKeyValue("input14", input14).addKeyValue("coeff14", coeff14)
		   .addKeyValue("input15", input15).addKeyValue("coeff15", coeff15)
		   .addKeyValue("input16", input16).addKeyValue("coeff16", coeff16)
		   .addKeyValue("input17", input17).addKeyValue("coeff17", coeff17)
		   .addKeyValue("input18", input18).addKeyValue("coeff18", coeff18)
		   .addKeyValue("input19", input19).addKeyValue("coeff19", coeff19)
		   .addKeyValue("input20", input20).addKeyValue("coeff20", coeff20)
		   .log("BigAdder doAWTimeSlice-{}: tally={}", alg_ver, tally);

		setOutput(output, tally);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
	{
	}

}
