/**
 * $Id$
 * 
 * Copyright 2017 United States Government.
 * This software was written by Cove Software, LLC (COVE) under contract to the U.S. Government.
 * No warranty is provided except for the specific contract terms between COVE and the Government.
 * 
 * $Log$
 * Revision 1.1  2017/11/03 19:20:10  mmaloney
 * Created for HDB 312.
 *
 */
package decodes.tsdb.algo;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.ParmRef;
import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Output is the accumulation of the input from the start of the period.\n" +
		"This is an Aggregating algorithm. Unlike most aggregating algorithms,\n" +
		"the aggregate period must be set by property rather than implied by\n" +
		"the period of the output.\n" +
		"The output param, 'periodToDate', must have the same interval as the \n" +
		"input.")
public class PeriodToDate
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
	@Input
	public double input;

	double sum = 0.0;
	boolean firstTriggerSeen = false;
	private PropertySpec ratingPropertySpecs[] = 
	{
		new PropertySpec("goodQualityOnly", PropertySpec.BOOLEAN,
			"(default=false) Only include good quality values in the calculation.")
	};
	@Override
	protected PropertySpec[] getAlgoPropertySpecs()
	{
		return ratingPropertySpecs;
	}
	boolean firstCall = true;


	@Output
	public NamedVariable periodToDate = new NamedVariable("periodToDate", 0);
	@Output
	public NamedVariable determineAggPeriod = new NamedVariable("determineAggPeriod", 0);

	@org.opendcs.annotations.PropertySpec(value="false")
	public boolean goodQualityOnly = false;

	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.AGGREGATING;
		_aggPeriodVarRoleName = "determineAggPeriod";
		// Code here will be run once, after the algorithm object is created.
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		// one-time validation
		if (firstCall)
		{
			ParmRef inref = this.getParmRef("input");
			if (inref == null || inref.tsid == null)
				throw new DbCompException("'input' time series parameter not assigned.");
			ParmRef outref = this.getParmRef("periodToDate");
			if (outref == null || outref.tsid == null)
				throw new DbCompException("'periodToDate' time series parameter not assigned.");
			if (!inref.tsid.getInterval().equalsIgnoreCase(outref.tsid.getInterval()))
				throw new DbCompException("The interval of the input must be the same as the "
					+ "interval of the output 'periodToDate' parameter.");
			firstCall = false;
		}
		
		// This will be called at the beginning of each aggregate period.
		sum = 0.0;
		firstTriggerSeen = false;
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
		// if this value is a trigger
		firstTriggerSeen = true;
		
		if (!goodQualityOnly || isGoodQuality("input"))
			sum += input;
		
		if (!firstTriggerSeen && isTrigger("input"))
			firstTriggerSeen = true;
		
		if (firstTriggerSeen)
			setOutput(periodToDate, sum);
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
		// This code will be executed once after each group of time slices.
		// For TimeSlice algorithms this is done once after all slices.
		// For Aggregating algorithms, this is done after each aggregate
		// period.
	}

}
