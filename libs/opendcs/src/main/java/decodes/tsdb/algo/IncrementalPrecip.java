package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Compute Incremental Precip from Cumulative Precip over a specified period.\n" +
		"Period determined by the interval of the output parameter, specified in computation record.\n")
public class IncrementalPrecip
	extends AW_AlgorithmBase
{
	@Input
	public double cumulativePrecip;

	double previousValue = 0.0;
	private boolean startOfPeriod = true;
	double tally = 0.0;
	int count = 0;
	
	private PropertySpec algoPropertySpecs[] = 
	{
		new PropertySpec("aggLowerBoundClosed", PropertySpec.BOOLEAN,
			"default=true, meaning to include the lower bound of the period."),
		new PropertySpec("aggUpperBoundClosed", PropertySpec.NUMBER,
			"default=true, meaning to include the upper bound of the period."),
		new PropertySpec("allowNegative", PropertySpec.BOOLEAN,
			"default=false, if true, then allow negative precip values. Normally, "
			+ "negative values are ignored.")
	};

	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return algoPropertySpecs;
	}

	@Output
	public NamedVariable incrementalPrecip = new NamedVariable("incrementalPrecip", 0);

	@org.opendcs.annotations.PropertySpec(value="true")
	public boolean aggLowerBoundClosed = true;
	@org.opendcs.annotations.PropertySpec(value="true")
	public boolean aggUpperBoundClosed = true;
	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean allowNegative = false;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "incrementalPrecip";
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		previousValue = 0.0;
		startOfPeriod = true;
		tally = 0.0;
		count = 0;
		
		// Output units will be the same as input.
		String inUnits = getInputUnitsAbbr("cumulativePrecip");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("incrementalPrecip", inUnits);
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
debug3("cumulativePrecip = " + cumulativePrecip + ", at time " + debugSdf.format(_timeSliceBaseTime));

		// Normal case is to ignore negative cumulative inputs. But allow if option set.
		if (cumulativePrecip < 0.0 && !allowNegative) 
			warning("Negative cumulativePrecip (" + cumulativePrecip + ") " +
					" in " + getParmTsUniqueString("cumulativePrecip")
					+ " at time " + debugSdf.format(_timeSliceBaseTime)
					+ " -- ignored.");
		else // good cumulative precip
		{
			if (!startOfPeriod)
			{
				if (cumulativePrecip < previousValue)   // Reset occurred
					info("cumulativePrecip reset detected in "
						+ getParmTsUniqueString("cumulativePrecip")
						+ " at time " + debugSdf.format(_timeSliceBaseTime)
						+ ". New value = " + cumulativePrecip);
				else
					tally += (cumulativePrecip - previousValue);
			}
			else
				startOfPeriod = false;
			previousValue = cumulativePrecip;
			count++;
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		if (count >= 2)
			setOutput(incrementalPrecip, tally);
	}

}
