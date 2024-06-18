/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.algorithms;

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.PropertySpec;

//AW:IMPORTS
// Place an import statements you need here.
import java.text.SimpleDateFormat;
import decodes.tsdb.*;
import java.util.GregorianCalendar;
import java.util.Calendar;
import ilex.var.TimedVariable;

//for getInputData function
import java.io.BufferedWriter;
//AW:IMPORTS_END

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;


@Algorithm(name = "DecayingParameter",
	description = 
		"Implements the equation: FCP[t] = BasinPrecip[t] + Decay*FCP[t-1]\n"
	  + "FCP = flood control parameter\n"
	  +	"Decay = Constant provided by water control diagram.\n"
	  +	"t = time ( daily time step )\n"
	  +	"the FCP value may reset on a specific date\n"
	  +	"\n"
	  +	"DSSMATH Called this DECPAR, so we are spelling it out for our usage.\n"
		)
public class DecayingParameter extends AW_AlgorithmBase
{
	public static final org.slf4j.Logger log = LoggerFactory.getLogger(DecayingParameter.class);

	@Input
	public double input;
	String _inputNames[] = { "input" };



	double previous_fcp; // fcp from the last time slice
	double fcp;  // the fcp we just calculated
	boolean first_run; // if this is the first execution of the algorithm, we must check for an existing FCP value
	SimpleDateFormat df;
	Date resetDate = null;
	GregorianCalendar cal = null;
	GregorianCalendar tmp = null;
	BufferedWriter extralog = null;


	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };


	@org.opendcs.annotations.PropertySpec(value = "0.0", propertySpecType = PropertySpec.NUMBER,
										 description = "Decay rate to apply to the previous value.")
	public double Decay = 0.0;
	@org.opendcs.annotations.PropertySpec(description = "Day of the year (ddMMM format) to reset 'previous' value to the reset value.")
	public String ResetDate = "";
	@org.opendcs.annotations.PropertySpec(value = "0.0", propertySpecType = PropertySpec.NUMBER,
										  description = "Value to which 'previous' value should be reset if reset date is provided.")
	public double ResetValue = 0.0;
	String _propertyNames[] = { "Decay", "ResetDate", "ResetValue" };

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm() throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;

		// Code here will be run once, after the algorithm object is created.
		if (Decay <= 0 || Decay >= 1)
		{   /// the above condition will either cause no FCP or FCP to never decay
			throw new DbCompException("DecayingParameter:init - You must specify a Decay parameter between but not including 0 or 1" );
		}
		
		df = new SimpleDateFormat( "ddMMM");
		try
		{
			if (!ResetDate.equals(""))
			{
				resetDate = df.parse( ResetDate );
				cal = new GregorianCalendar(aggTZ);
				cal.setTime(resetDate);
			}
			tmp = new GregorianCalendar(aggTZ);
		}
		catch (java.text.ParseException e)
		{
			throw new DbCompException("Could not parse reset date, please use format: ddMMMyyyy HHmm", e);
		}
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices() throws DbCompException
	{
		first_run = true;
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
	protected void doAWTimeSlice() throws DbCompException
	{
		/*
		 * TODO: Consider changing this to always pull the previous value
		 *  this is faster, just...feels iffy....It is assumed that all values exist.
		 */
		if (first_run)
		{
			log.trace("beginning first run procedures");
			TimeSeriesIdentifier tsId = this.getParmTsId("input");
			try
			{
				CTimeSeries cts = tsdb.makeTimeSeries(tsId);
				TimedVariable tv = this.tsdb.getPreviousValue(cts, _timeSliceBaseTime);
				previous_fcp = tv.getDoubleValue();
				if (isMissing(previous_fcp))
				{
					previous_fcp=0.0;
				}
			}
			catch (Exception ex)
			{
				throw new DbCompException("Unable to process previous value.", ex);
			}
			first_run = false;                        
		}
		if (resetDate != null)
		{
			cal.set(Calendar.YEAR, _timeSliceBaseTime.getYear()+1900);
			tmp.setTime(_timeSliceBaseTime);
			if ((tmp.get(Calendar.MONTH) == cal.get(Calendar.MONTH) ) && (tmp.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)))
			{	
				previous_fcp = ResetValue; // reset value
			}
		}
		if (!isMissing(input))
		{
			log.trace("Adding {} to {}", input, Decay*previous_fcp );
			fcp = Decay*previous_fcp + input;
			log.trace("New parameter value is {}, setting previous_fcp to this value for next timestep", fcp);
			previous_fcp = fcp;
                                      
			setOutput(output, fcp);
		}
		else
		{
			deleteOutput( output );
		}
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
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
