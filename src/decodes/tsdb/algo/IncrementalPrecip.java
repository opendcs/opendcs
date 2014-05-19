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
Compute Incremental Precip from Cumulative Precip over a specified period.
Period determined by the interval of the output parameter, specified in computation record.

 */
//AW:JAVADOC_END
public class IncrementalPrecip
	extends AW_AlgorithmBase
{
//AW:INPUTS
	public double cumulativePrecip;	//AW:TYPECODE=i
	String _inputNames[] = { "cumulativePrecip" };
//AW:INPUTS_END

//AW:LOCALVARS
	double previousValue = -1.0;
	double tally = 0.0;
	int count = 0;

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable incrementalPrecip = new NamedVariable("incrementalPrecip", 0);
	String _outputNames[] = { "incrementalPrecip" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean aggLowerBoundClosed = true;
	public boolean aggUpperBoundClosed = true;
	String _propertyNames[] = { "aggLowerBoundClosed", "aggUpperBoundClosed" };
//AW:PROPERTIES_END

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "incrementalPrecip";
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
		previousValue = -1.0;
		tally = 0.0;
		count = 0;
		
		// Output units will be the same as input.
		String inUnits = getInputUnitsAbbr("cumulativePrecip");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("incrementalPrecip", inUnits);
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
debug3("cumulativePrecip = " + cumulativePrecip + ", at time " + debugSdf.format(_timeSliceBaseTime));
		if (cumulativePrecip < 0.0)                       // Sanity check
			warning("Negative cumulativePrecip (" + cumulativePrecip + ") " +
					" in " + getParmTsUniqueString("cumulativePrecip")
					+ " at time " + debugSdf.format(_timeSliceBaseTime)
					+ " -- ignored.");
		else
		{
			if (previousValue >= 0.0)                   // Not first value in period
			{
				if (cumulativePrecip < previousValue)   // Reset occurred
					info("cumulativePrecip reset detected in "
						+ getParmTsUniqueString("cumulativePrecip")
						+ " at time " + debugSdf.format(_timeSliceBaseTime)
						+ ". New value = " + cumulativePrecip);
				else
					tally += (cumulativePrecip - previousValue);
			}
			previousValue = cumulativePrecip;
			count++;
		}
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		if (count >= 2)
			setOutput(incrementalPrecip, tally);
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
