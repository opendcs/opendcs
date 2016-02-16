package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Takes up to 20 input values labeled input1 ... input20. Multiplies
them by coefficients supplied in properties coeff1 ... coeff20.
Adds them together and produces a single output labeled 'output'.
The property input_constant is added to the ouput result.
input_constant defaults to 0 if not supplied
Values not assigned by computation are ignored.
All coefficients default to 1.0 if not supplied.
 
This algorithm is a modification of Scaler adder
Created by M. Bogner   May 2009
 */
//AW:JAVADOC_END
public class BigAdder extends decodes.tsdb.algo.AW_AlgorithmBase
{
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
	public double input10;	//AW:TYPECODE=i
	public double input11;	//AW:TYPECODE=i
	public double input12;	//AW:TYPECODE=i
	public double input13;	//AW:TYPECODE=i
	public double input14;	//AW:TYPECODE=i
	public double input15;	//AW:TYPECODE=i
	public double input16;	//AW:TYPECODE=i
	public double input17;	//AW:TYPECODE=i
	public double input18;	//AW:TYPECODE=i
	public double input19;	//AW:TYPECODE=i
	public double input20;	//AW:TYPECODE=i
	String _inputNames[] = { "input1", "input2", "input3", "input4", "input5",
				 "input6", "input7", "input8", "input9", "input10",
				 "input11", "input12", "input13", "input14", "input15",
				 "input16", "input17", "input18", "input19", "input20"};
//AW:INPUTS_END

//AW:LOCALVARS
	String alg_ver = "1.0.01";
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
        public double input_constant = 0.0;
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
	public double coeff11 = 1.0;
	public double coeff12 = 1.0;
	public double coeff13 = 1.0;
	public double coeff14 = 1.0;
	public double coeff15 = 1.0;
	public double coeff16 = 1.0;
	public double coeff17 = 1.0;
	public double coeff18 = 1.0;
	public double coeff19 = 1.0;
	public double coeff20 = 1.0;
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
	public String input11_MISSING = "ignore";
	public String input12_MISSING = "ignore";
	public String input13_MISSING = "ignore";
	public String input14_MISSING = "ignore";
	public String input15_MISSING = "ignore";
	public String input16_MISSING = "ignore";
	public String input17_MISSING = "ignore";
	public String input18_MISSING = "ignore";
	public String input19_MISSING = "ignore";
	public String input20_MISSING = "ignore";
	String _propertyNames[] = {
	"coeff1", "coeff2", "coeff3", "coeff4", "coeff5", 
	"coeff6", "coeff7", "coeff8", "coeff9", "coeff10", 
	"coeff11", "coeff12", "coeff13", "coeff14", "coeff15", 
	"coeff16", "coeff17", "coeff18", "coeff19", "coeff20", 
	"input1_MISSING", "input2_MISSING", "input3_MISSING", "input4_MISSING",
	"input5_MISSING", "input6_MISSING", "input7_MISSING", "input8_MISSING",
	"input9_MISSING", "input10_MISSING",
	"input11_MISSING", "input12_MISSING", "input13_MISSING", "input14_MISSING",
	"input15_MISSING", "input16_MISSING", "input17_MISSING", "input18_MISSING",
	"input19_MISSING", "input20_MISSING"
 	};
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
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input1=" + input1 + ", coeff1=" + coeff1
+", input2=" + input2 + ", coeff2=" + coeff2 + " input3=" + input3 + ", coeff3=" + coeff3);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input4=" + input4 + ", coeff4=" + coeff4
+", input5=" + input5 + ", coeff5=" + coeff5 + " input6=" + input6 + ", coeff6=" + coeff6);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input7=" + input7 + ", coeff7=" + coeff7
+", input8=" + input8 + ", coeff8=" + coeff8 + " input9=" + input9 + ", coeff9=" + coeff9);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input11=" + input11 + ", coeff11=" + coeff11
+", input12=" + input12 + ", coeff12=" + coeff12 + " input13=" + input13 + ", coeff13=" + coeff13);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input14=" + input14 + ", coeff14=" + coeff14
+", input15=" + input15 + ", coeff15=" + coeff15 + " input16=" + input16 + ", coeff16=" + coeff16);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input17=" + input17 + ", coeff17=" + coeff17
+", input18=" + input18 + ", coeff18=" + coeff18 + " input19=" + input19 + ", coeff19=" + coeff19);
debug3("BigAdder doAWTimeSlice- " + alg_ver + " input20=" + input20 + ", coeff20=" + coeff20
+":: tally= " + tally);

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
