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

import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.PropertySpec;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Output is the accumulation of the input from the start of the period.
This is an Aggregating algorithm. Unlike most aggregating algorithms,
the aggregate period must be set by property rather than implied by
the period of the output.
The output param, 'periodToDate', must have the same interval as the 
input.
 */
//AW:JAVADOC_END
public class PeriodToDate
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
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

//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable periodToDate = new NamedVariable("periodToDate", 0);
	public NamedVariable determineAggPeriod = new NamedVariable("determineAggPeriod", 0);
	String _outputNames[] = { "periodToDate", "determineAggPeriod" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public boolean goodQualityOnly = false;
	String _propertyNames[] = { "goodQualityOnly" };
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
		_aggPeriodVarRoleName = "determineAggPeriod";
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
		// if this value is a trigger
		firstTriggerSeen = true;
		
		if (!goodQualityOnly || isGoodQuality("input"))
			sum += input;
		
		if (!firstTriggerSeen && isTrigger("input"))
			firstTriggerSeen = true;
		
		if (firstTriggerSeen)
			setOutput(periodToDate, sum);
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
