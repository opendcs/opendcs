package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Computes bridge clearance by subtracting waterlevel from constant 'low chord'.\n" +
		"Make sure that the waterlevel and low chord are consistent. If one is a stage above arbitrary datum, " +
		"then they both must be. Likewise, if one is an elevation above sea level, the other must be also.\n"
)

public class BridgeClearance extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double waterLevel;	//AW:TYPECODE=i

	@Output
	public NamedVariable clearance = new NamedVariable("clearance", 0);

	@PropertySpec(value = "0")
	public double lowChord = 0;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// Output units will be the same as water level.
		String inUnits = getInputUnitsAbbr("waterLevel");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("clearance", inUnits);
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
		// Enter code to be executed at each time-slice.
		setOutput(clearance, lowChord - waterLevel);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	@Override
	protected void afterTimeSlices()
		throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
	}
}
