package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.MissingAction;
import decodes.tsdb.ParmRef;
import decodes.tsdb.VarFlags;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description ="Takes up to 10 input values labeled input1 ... input10. Multiplies\n" +
"them by coefficients supplied in properties coeff1 ... coeff10.\n" +
"Adds them together and produces a single output labeled 'output'.\n" +
"Values not assigned by computation are ignored.\n" +
"All coefficients default to 1.0 if not supplied." )
public class ScalerAdder extends AW_AlgorithmBase
{
	@Input public double input1;	//AW:TYPECODE=i
	@Input public double input2;	//AW:TYPECODE=i
	@Input public double input3;	//AW:TYPECODE=i
	@Input public double input4;	//AW:TYPECODE=i
	@Input public double input5;	//AW:TYPECODE=i
	@Input public double input6;	//AW:TYPECODE=i
	@Input public double input7;	//AW:TYPECODE=i
	@Input public double input8;	//AW:TYPECODE=i
	@Input public double input9;	//AW:TYPECODE=i
	@Input public double input10;//AW:TYPECODE=i

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec public double coeff1 = 1.0;
	@PropertySpec public double coeff2 = 1.0;
	@PropertySpec public double coeff3 = 1.0;
	@PropertySpec public double coeff4 = 1.0;
	@PropertySpec public double coeff5 = 1.0;
	@PropertySpec public double coeff6 = 1.0;
	@PropertySpec public double coeff7 = 1.0;
	@PropertySpec public double coeff8 = 1.0;
	@PropertySpec public double coeff9 = 1.0;
	@PropertySpec public double coeff10 = 1.0;
	@PropertySpec public String input1_MISSING = "ignore";
	@PropertySpec public String input2_MISSING = "ignore";
	@PropertySpec public String input3_MISSING = "ignore";
	@PropertySpec public String input4_MISSING = "ignore";
	@PropertySpec public String input5_MISSING = "ignore";
	@PropertySpec public String input6_MISSING = "ignore";
	@PropertySpec public String input7_MISSING = "ignore";
	@PropertySpec public String input8_MISSING = "ignore";
	@PropertySpec public String input9_MISSING = "ignore";
	@PropertySpec public String input10_MISSING = "ignore";

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
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
	@Override
	protected void doAWTimeSlice()
		throws DbCompException
	{
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
			debug2("Skipping time slice with base time " + debugSdf.format(_timeSliceBaseTime)
			+ " because of missing value for param " + t);
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
debug3("doAWTimeSlice baseTime=" + debugSdf.format(_timeSliceBaseTime)
	+ ", input1=" + input1 + ", coeff1=" + coeff1
+", input2=" + input2 + ", coeff2=" + coeff2 + ", tally=" + tally);
		setOutput(output, tally);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
