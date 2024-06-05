package org.opendcs.algorithms;

import java.util.Date;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;

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

import org.slf4j.LoggerFactory;

//AW:JAVADOC
/**
Implements the equation: FCP[t] = BasinPrecip[t] + Decay*FCP[t-1]
FCP = flood control parameter
Decay = Constant provided by water control diagram.
t = time ( daily time step )
the FCP value may reset on a specific date

DSSMATH Called this DECPAR, so we are spelling it out for our usage.

 */
//AW:JAVADOC_END
public class DecayingParameter extends AW_AlgorithmBase
{
	public static final org.slf4j.Logger log = LoggerFactory.getLogger(DecayingParameter.class);
//AW:INPUTS
	public double input;	//AW:TYPECODE=i	
	String _inputNames[] = { "input" }; //, "PreviousFCP" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
	double previous_fcp; // fcp from the last time slice
	double fcp;  // the fcp we just calculated
	boolean first_run; // if this is the first execution of the algorithm, we must check for an existing FCP value
	SimpleDateFormat df;
	Date resetDate = null;
	GregorianCalendar cal = null;
	GregorianCalendar tmp = null;
	BufferedWriter extralog = null;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public double Decay = 0.0;
	public String ResetDate = "";
	public double ResetValue = 0.0;
	String _propertyNames[] = { "Decay", "ResetDate", "ResetValue" };
//AW:PROPERTIES_END

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm() throws DbCompException
	{
//AW:INIT
		_awAlgoType = AWAlgoType.TIME_SLICE;
//AW:INIT_END

//AW:USERINIT
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
//AW:USERINIT_END
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices() throws DbCompException
	{
//AW:BEFORE_TIMESLICES
		// This code will be executed once before each group of time slices.
		// For TimeSlice algorithms this is done once before all slices.
		// For Aggregating algorithms, this is done before each aggregate
		// period.                
		first_run = true;
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
	protected void doAWTimeSlice() throws DbCompException
	{
//AW:TIMESLICE
		// Enter code to be executed at each time-slice.
		/*
		 * TODO: Consider changing this to always pull the previous value
		 *  this is faster, just...feels iffy....It is assumed that all values exist.
		 */
		if (first_run)
		{
			log.trace("begining first run procedures");
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
