package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.MissingAction;
import decodes.tsdb.ParmRef;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description ="Takes up to 10 input values labeled input1 ... input10. Multiplies\n" +
"them by coefficients supplied in properties coeff1 ... coeff10.\n" +
"Adds them together and produces a single output labeled 'output'.\n" +
"Values not assigned by computation are ignored.\n" +
"All coefficients default to 1.0 if not supplied." )
public class ScalerAdder extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input public double input1;
	@Input public double input2;
	@Input public double input3;
	@Input public double input4;
	@Input public double input5;
	@Input public double input6;
	@Input public double input7;
	@Input public double input8;
	@Input public double input9;
	@Input public double input10;

	@Output(type = Double.class)
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
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
	{
	}
}
