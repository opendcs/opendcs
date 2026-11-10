package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**

 */
@Algorithm(description = "Within a given window select the most recent valid value to be at the end")
public class LastValueFromPeriod
	extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	@Input
	public double input;

	private double lastValue = Double.NEGATIVE_INFINITY;


	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "output";
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		lastValue = Double.NEGATIVE_INFINITY;
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
		if( !isMissing( input ) ){
			lastValue = input;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		// An empty period is not the same as a deleted input. Passing -Inf to setOutput
		// deletes the stored value, which would let a period with no data at all -- or an
		// input role nothing is assigned to -- erase the previous result. The base class
		// still deletes the output when an input was explicitly deleted.
		if (isMissing(lastValue))
		{
			log.debug("No value in the period ending {} -- leaving the output unchanged",
					  _aggregatePeriodEnd);
			return;
		}
		setOutput(output, lastValue);
	}

}
