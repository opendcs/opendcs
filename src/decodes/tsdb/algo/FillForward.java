package decodes.tsdb.algo;

import java.util.Calendar;
import java.util.Date;

import ilex.var.NamedVariableList;
import ilex.var.NamedVariable;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.VarFlags;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ParmRef;
import ilex.var.TimedVariable;
import decodes.tsdb.TimeSeriesIdentifier;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
Project an input value by copying it forward in time for the specified number of intervals.

 */
//AW:JAVADOC_END
public class FillForward
	extends decodes.tsdb.algo.AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
	Date latestTimeSlice = null;
	double latestInputValue = 0.0;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	public long NumIntervals = 4;
	String _propertyNames[] = { "NumIntervals" };
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
		latestTimeSlice = null;
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
		latestTimeSlice = new Date(_timeSliceBaseTime.getTime());
		latestInputValue = input;
		// Enter code to be executed at each time-slice.
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
		if (latestTimeSlice == null)
			return;
		
		// Make sure that latestTimeSlice is the latest input in the database.
		try
		{
			TimedVariable tv;
			tv = tsdb.getNextValue(getParmRef("input").timeSeries, latestTimeSlice);
			if (tv != null)
			{
				debug1("Exiting because there is data after latest time slice: "
					+ "latest seen this run=" + debugSdf.format(latestTimeSlice)
					+ ", latest in DB=" + debugSdf.format(tv.getTime()));
				return;
			}
		}
		catch (Exception ex)
		{
			String msg = "Error reading latest TV from DB: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			warning(msg);
			throw new DbCompException(msg);
		}
		
		// latestTimeSlice is the last _input_ time in the data.
		// Find the first _output_ time >= that time.
		
		// Strategy is to use the existing aggregate period logic
		// where the "aggregate period" is simply the output interval.
		// Find the start of the "aggregate period" that
		// contains the latest input time, with bounds set to (...]
		// Now I am guaranteed that agg period END will be => the latest input time.
		// This also automatically takes the aggregate time zone and offset into
		// consideration.
		ParmRef outputParmRef = getParmRef("output");
		aggLowerBoundClosed = false;
		aggUpperBoundClosed = true;
		AggregatePeriod aggPeriod = determineAggPeriod(latestTimeSlice, 
			outputParmRef.compParm.getInterval());

		// Loop forward specified number of increments.
		aggCal.setTime(aggPeriod.getEnd());
		IntervalIncrement outputIncr = IntervalCodes.getIntervalCalIncr(
			outputParmRef.compParm.getInterval());
		for(int i = 0; i<NumIntervals; i++)
		{
			Date outputTime = aggCal.getTime();
			setOutput(output, latestInputValue, outputTime);
			aggCal.add(outputIncr.getCalConstant(), outputIncr.getCount());
		}
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
