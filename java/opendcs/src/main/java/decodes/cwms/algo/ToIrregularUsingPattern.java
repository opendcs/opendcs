/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.cwms.algo;

import decodes.cwms.CwmsConstants;
import decodes.cwms.CwmsTsId;
import decodes.tsdb.*;
import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "Interpolate input data to fit Irregular time series pattern.") 
public class ToIrregularUsingPattern extends AW_AlgorithmBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	@Input
	public double input;	
	@Input
	public double pattern;	

	// Enter any local class variables needed by the algorithm.
	private CTimeSeries inputTS = null;
	private CTimeSeries patternTS = null;
	private String inUnits = null;

	@Output(type = Double.class)
	public NamedVariable output = new NamedVariable("output", 0);


	// Allow javac to generate a no-args constructor.

	/**
	 * Algorithm-specific initialization provided by the subclass.
	 */
	protected void initAWAlgorithm( )
		throws DbCompException
	{
		_awAlgoType = AWAlgoType.TIME_SLICE;
	}
	
	/**
	 * This method is called once before iterating all time slices.
	 */
	protected void beforeTimeSlices()
		throws DbCompException
	{
		
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
			log.atWarn().setCause(ex).log("Error accessing input/output time series.");
		}

		
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
				log.atWarn().setCause(ex).log("Interpolation resulted in invalid var.");
			}
		}
		//check if input data is Average. copies the average value of the previous input variable.
		else if(CwmsConstants.PARAM_TYPE_AVE.equalsIgnoreCase(tsId.getParamType())){
			try {
				setOutput(output, next.getDoubleValue(), patternDate);
			}
			catch (NoConversionException ex)
			{
				log.atWarn().setCause(ex).log("Interpolation resulted in invalid var.");
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
				log.atWarn().setCause(ex).log("Interpolation resulted in invalid var.");
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
				log.atWarn().setCause(ex).log("Error accessing input/output time series.");
				return;
			}
		}
		if(nextInput == null || prevInput == null){
			return;
		}
		interpolate(_timeSliceBaseTime, prevInput, nextInput);


	}

	/**
	 * This method is called once after iterating all time slices.
	 */
	protected void afterTimeSlices()
		throws DbCompException
	{
	}
}
