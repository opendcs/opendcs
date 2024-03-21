package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
Takes up to 30 input values labeled input1 ... input30. Multiplies
them by coefficients supplied in properties coeff1 ... coeff30.
Adds them together and produces a single output labeled 'output'.
Values not assigned by computation are ignored.
All coefficients default to 1.0 if not supplied.




 */
//AW:JAVADOC_END
public class ScalerAdder30
	extends AW_AlgorithmBase
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
	public double input21;	//AW:TYPECODE=i
	public double input22;	//AW:TYPECODE=i
	public double input23;	//AW:TYPECODE=i
	public double input24;	//AW:TYPECODE=i
	public double input25;	//AW:TYPECODE=i
	public double input26;	//AW:TYPECODE=i
	public double input27;	//AW:TYPECODE=i
	public double input28;	//AW:TYPECODE=i
	public double input29;	//AW:TYPECODE=i
	public double input30;	//AW:TYPECODE=i
	String _inputNames[] = { "input1", "input2", "input3", "input4", "input5", "input6", "input7", "input8", "input9", "input10", "input11", "input12", "input13", "input14", "input15", "input16", "input17", "input18", "input19", "input20", "input21", "input22", "input23", "input24", "input25", "input26", "input27", "input28", "input29", "input30" };
//AW:INPUTS_END

//AW:LOCALVARS

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double coeff1 = 1.0;
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
	public double coeff2 = 1.0;
	public double coeff20 = 1.0;
	public double coeff21 = 1.0;
	public double coeff22 = 1.0;
	public double coeff23 = 1.0;
	public double coeff24 = 1.0;
	public double coeff25 = 1.0;
	public double coeff26 = 1.0;
	public double coeff27 = 1.0;
	public double coeff28 = 1.0;
	public double coeff29 = 1.0;
	public double coeff3 = 1.0;
	public double coeff30 = 1.0;
	public double coeff4 = 1.0;
	public double coeff5 = 1.0;
	public double coeff6 = 1.0;
	public double coeff7 = 1.0;
	public double coeff8 = 1.0;
	public double coeff9 = 1.0;
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
	public String input1_MISSING = "ignore";
	public String input20_MISSING = "ignore";
	public String input21_MISSING = "ignore";
	public String input22_MISSING = "ignore";
	public String input23_MISSING = "ignore";
	public String input24_MISSING = "ignore";
	public String input25_MISSING = "ignore";
	public String input26_MISSING = "ignore";
	public String input27_MISSING = "ignore";
	public String input28_MISSING = "ignore";
	public String input29_MISSING = "ignore";
	public String input2_MISSING = "ignore";
	public String input3_MISSING = "ignore";
    public String input30_MISSING = "ignore";
	public String input4_MISSING = "ignore";
	public String input5_MISSING = "ignore";
	public String input6_MISSING = "ignore";
	public String input7_MISSING = "ignore";
	public String input8_MISSING = "ignore";
	public String input9_MISSING = "ignore";
	String _propertyNames[] = { "coeff1", 
    "coeff10", "coeff11", "coeff12", "coeff13", "coeff14", "coeff15", "coeff16", "coeff17", "coeff18", "coeff19", "coeff2", 
    "coeff20", "coeff20", "coeff21", "coeff22", "coeff23", "coeff24", "coeff25", "coeff26", "coeff27", "coeff28", "coeff29",
    "coeff30","coeff3", "coeff4", "coeff5", "coeff6", "coeff7", "coeff8", "coeff9", 
    "input10_MISSING", "input11_MISSING", "input12_MISSING", "input13_MISSING", "input14_MISSING", "input15_MISSING", "input16_MISSING", "input17_MISSING", "input18_MISSING", "input19_MISSING", "input1_MISSING", 
    "input20_MISSING", "input21_MISSING", "input22_MISSING", "input23_MISSING", "input24_MISSING", "input25_MISSING", "input26_MISSING", "input27_MISSING", "input28_MISSING", "input29_MISSING",
    "input2_MISSING", "input30_MISSING", "input3_MISSING", "input4_MISSING", "input5_MISSING", "input6_MISSING", "input7_MISSING", "input8_MISSING", "input9_MISSING" };
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
		if (!isMissing(input21))
			tally += (input21 * coeff21);
		if (!isMissing(input22))
			tally += (input22 * coeff22);
		if (!isMissing(input23))
			tally += (input23 * coeff23);
		if (!isMissing(input24))
			tally += (input24 * coeff24);
		if (!isMissing(input25))
			tally += (input25 * coeff25);
		if (!isMissing(input26))
			tally += (input26 * coeff26);
		if (!isMissing(input27))
			tally += (input27 * coeff27);
		if (!isMissing(input28))
			tally += (input28 * coeff28);
		if (!isMissing(input29))
			tally += (input29 * coeff29);
		if (!isMissing(input30))
			tally += (input30 * coeff30);
		debug3("doAWTimeSlice input1=" + input1 + ", coeff1=" + coeff1
		+", input2=" + input2 + ", coeff2=" + coeff2 + ", tally=" + tally);
		setOutput(output, tally);
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
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
