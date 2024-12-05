/**
 * $Id$
 * 
 * $Log$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Given two inputs, output the best one:\n" +
		"If only one is present at the time-slice, output it.\n" +
		"Else if input1LowThreshold is defined, output input1 if it is >= the threshold, else input2.\n" +
		"Else (input1LowThreshold not defined) output higher if (chooseHigher) else the lower.\n" +
		"Useful in situations where you have redundant sensors.")

public class ChooseOne extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input1;	//AW:TYPECODE=i
	@Input
	public double input2;	//AW:TYPECODE=i

	@Output
	public NamedVariable output = new NamedVariable("output", 0);

	@PropertySpec(value = "999999999999.9")
	public static final double HIGH_DOUBLE = 999999999999.9;
	@PropertySpec(value = "-999999999999.9")
	public double lowerLimit = -999999999999.9;
	@PropertySpec(value = "IGNORE")
	public String input1_MISSING = "IGNORE";
	@PropertySpec(value = "IGNORE")
	public String input2_MISSING = "IGNORE";
	@PropertySpec(value = "true")
	public boolean chooseHigher = true;
	@PropertySpec(value = "999999999999.9")
	public double input1LowThreshold = 999999999999.9;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	@Override
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
		// Code here will be run once, after the algorithm object is created.
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	@Override
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// Normally for copy, output units will be the same as input.
		String inUnits = getInputUnitsAbbr("input1");
		if (inUnits == null || inUnits.length() == 0)
			inUnits = getInputUnitsAbbr("input2");
		if (inUnits != null && inUnits.length() > 0)
			setOutputUnitsAbbr("output", inUnits);
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
		double upperLimit = HIGH_DOUBLE;
		boolean no_1 = isMissing(input1) || input1 > upperLimit || input1 < lowerLimit;
		boolean no_2 = isMissing(input2) || input2 > upperLimit || input2 < lowerLimit;
		if (!no_1 && !no_2) // both present?
		{
			if (input1LowThreshold < HIGH_DOUBLE)
				setOutput(output, input1 >= input1LowThreshold ? input1 : input2);
			else // use chooseHigher to decide
				setOutput(output, 
					chooseHigher ? Math.max(input1, input2) : Math.min(input1, input2));
		}
		else if (!no_1)
			setOutput(output, input1);
		else if (!no_2)
			setOutput(output, input2);
		// Otherwise both bad. No output for this slice.
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
