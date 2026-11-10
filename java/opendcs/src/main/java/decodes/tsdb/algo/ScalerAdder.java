package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.MissingAction;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

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
		double inputs[] = { input1, input2, input3, input4, input5,
							input6, input7, input8, input9, input10 };
		double coeffs[] = { coeff1, coeff2, coeff3, coeff4, coeff5,
							coeff6, coeff7, coeff8, coeff9, coeff10 };

		// Only an assigned input can hold up the slice: an input the computation does not
		// map has no value to require, which is what "values not assigned by computation
		// are ignored" in the algorithm description means.
		for(int i = 0; i < inputs.length; i++)
		{
			String role = "input" + (i + 1);
			if (isAssigned(role) && isMissing(inputs[i])
			 && getInputMissingAction(role) != MissingAction.IGNORE)
			{
				log.trace("Skipping time slice with base time {} because of missing value for param {}",
						  _timeSliceBaseTime, role);
				return;
			}
		}

		double tally = 0.0;
		for(int i = 0; i < inputs.length; i++)
			if (!isMissing(inputs[i]))
				tally += (inputs[i] * coeffs[i]);

		LoggingEventBuilder leb = log.atTrace();
		for(int i = 0; i < inputs.length; i++)
			leb.addKeyValue("input" + (i + 1), inputs[i])
			   .addKeyValue("coeff" + (i + 1), coeffs[i]);
		leb.log("tally={}", tally);
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
