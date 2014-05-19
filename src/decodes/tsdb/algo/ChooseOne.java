package decodes.tsdb.algo;

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Given two inputs, output the best one:
If only one is present at the time-slice, output it.
If one is outside the specified upper or lower limit (see properties) output the other.
If both are acceptable, output the first one.
Useful in situations where you have redundant sensors.
 */
//AW:JAVADOC_END
public class ChooseOne
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input1;	//AW:TYPECODE=i
	public double input2;	//AW:TYPECODE=i
	String _inputNames[] = { "input1", "input2" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double upperLimit = 999999999999.9;
	public double lowerLimit = -999999999999.9;
	public String input1_MISSING = "IGNORE";
	public String input2_MISSING = "IGNORE";
	public boolean chooseHigher = true;
	String _propertyNames[] = { "upperLimit", "lowerLimit", "input1_MISSING", "input2_MISSING", "chooseHigher" };
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
		// Code here will be run once, after the algorithm object is created.
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input1");
		if (inUnits == null || inUnits.length() == 0)
			inUnits = getInputUnitsAbbr("input2");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("output", inUnits);
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
		boolean no_1 = isMissing(input1) || input1 > upperLimit || input1 < lowerLimit;
		boolean no_2 = isMissing(input2) || input2 > upperLimit || input2 < lowerLimit;
		if (!no_1 && !no_2) // both present?
		{
			setOutput(output, 
				chooseHigher ? Math.max(input1, input2) : Math.min(input1, input2));
		}
		else if (!no_1)
			setOutput(output, input1);
		else if (!no_2)
			setOutput(output, input2);
		// Otherwise both bad. No output for this slice.
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
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
