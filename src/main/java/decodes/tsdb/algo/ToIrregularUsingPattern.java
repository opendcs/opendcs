package decodes.tsdb.algo;

import decodes.cwms.CwmsConstants;
import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.VarFlags;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;

import java.util.Date;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC

/**
Interpolate input data to fit Irregular time series pattern.
 */
//AW:JAVADOC_END
public class ToIrregularUsingPattern
	extends AW_AlgorithmBase
{
//AW:INPUTS
	public double input;	//AW:TYPECODE=i
	public double pattern;	//AW:TYPECODE=i
	String _inputNames[] = { "input", "pattern" };
//AW:INPUTS_END

//AW:LOCALVARS
	// Enter any local class variables needed by the algorithm.
	private CTimeSeries inputTS = null;
	private CTimeSeries patternTS = null;
	private String inUnits = null;
//AW:LOCALVARS_END

//AW:OUTPUTS
	public NamedVariable output = new NamedVariable("output", 0);
	String _outputNames[] = { "output" };
//AW:OUTPUTS_END

//AW:PROPERTIES
	String _propertyNames[] = {};
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
		
		// Validation
		String outputIntvs = getParmRef("output").compParm.getInterval();
		Interval outputIntv = IntervalCodes.getInterval(outputIntvs);
		String patternIntvs = getParmRef("pattern").compParm.getInterval();
		Interval patternIntv = IntervalCodes.getInterval(patternIntvs);

		if(!outputIntv.equals(patternIntv)){
			throw new DbCompException("Output interval does not match Pattern interval");
		}

		inputTS = getParmRef("input").timeSeries;
		patternTS = getParmRef("pattern").timeSeries;

		inUnits = getInputUnitsAbbr("input");
		setOutputUnitsAbbr("output", inUnits);

		//Check for any uninterpolated  pattern dates in the past and interpolate with new inputs
		Date firstInputT = baseTimes.first();
		try {
			TimedVariable prevInput = tsdb.getPreviousValue(inputTS, firstInputT);
			TimedVariable nextInput = tsdb.getNextValue(inputTS, firstInputT);
			boolean lookback = true;
			TimedVariable prevPattern = tsdb.getPreviousValue(patternTS, firstInputT);
			while(lookback){
				if (0 > prevInput.getTime().compareTo(prevPattern.getTime())){
					lookback = false;
				}
				else{
					interpolate(prevPattern.getTime(), prevInput, nextInput);
					prevPattern = tsdb.getPreviousValue(patternTS, prevPattern.getTime());
				}
			}

		}
		catch(Exception ex)
		{
			warning("Error accessing input/output time series: " + ex);
		}

		
//AW:BEFORE_TIMESLICES_END
	}

	/**Interpolation helper function
	 * Date Pattern data - data time which you want to interpolate the value for.
	 * TimedVariable prev - TimedVariable of the nearest in time previous input variable
	 * TimedVariable next - TimedVariable of the nearest in time next input variable
	 * Checks the type of data stored in the input and preforms appropriated interpolation between two points.
	**/
	void interpolate(Date patternDate, TimedVariable prev, TimedVariable next){
		CwmsTsId tsId = (CwmsTsId) inputTS.getTimeSeriesIdentifier();
		//check if input data in instantaneous. linear interpolation between two points.
		if(CwmsConstants.PARAM_TYPE_INST.equalsIgnoreCase(tsId.getParamType())){
			long diff =	patternDate.getTime() - prev.getTime().getTime();
			try {
				double rise = next.getDoubleValue() - prev.getDoubleValue();
				long run = next.getTime().getTime() - prev.getTime().getTime();
				double value = (rise / run) * diff + prev.getDoubleValue();
				setOutput(output, value, patternDate);
			}
			catch (NoConversionException ex)
			{
				warning("Interpolation resulted in invalid var: " + ex);
			}
		}
		//check if input data is Average. copies the average value of the previous input variable.
		else if(CwmsConstants.PARAM_TYPE_AVE.equalsIgnoreCase(tsId.getParamType())){
			try {
				setOutput(output, next.getDoubleValue(), patternDate);
			}
			catch (NoConversionException ex)
			{
				warning("Interpolation resulted in invalid var: " + ex);
			}
		}
		//check if input data is Total. linear interpolation between zero at time of previous value and next value.
		else if(CwmsConstants.PARAM_TYPE_TOTAL.equalsIgnoreCase(tsId.getParamType())){
			long diff = patternDate.getTime() - prev.getTime().getTime();
			try {
				double rise = next.getDoubleValue();
				long run = next.getTime().getTime() - prev.getTime().getTime();
				double value = (rise / run) * diff;
				setOutput(output, value, patternDate);
			}
			catch (NoConversionException ex)
			{
				warning("Interpolation resulted in invalid var: " + ex);
			}
		}
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
		//if no pattern not interpolation needed
		if(isMissing(pattern)){
			return;
		}
		//if pattern and input exist at same timeslice return input value
		if(!isMissing(input)){
			setOutput(output, input);
			return;
		}

		//Find previous and next input TimedVariables with respect to pattern
		TimedVariable prevInput = inputTS.findPrev(_timeSliceBaseTime);
		TimedVariable nextInput = inputTS.findNext(_timeSliceBaseTime);

		Date firstInputT = baseTimes.first();
		if(prevInput == null){
			try {
				prevInput = tsdb.getPreviousValue(inputTS, firstInputT);
			}
			catch(Exception ex)
			{
				warning("Error accessing input/output time series: " + ex);
				return;
			}
		}
		if(nextInput == null || prevInput == null){
			return;
		}
		interpolate(_timeSliceBaseTime, prevInput, nextInput);



		
//AW:TIMESLICE_END
	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
//AW:AFTER_TIMESLICES
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
