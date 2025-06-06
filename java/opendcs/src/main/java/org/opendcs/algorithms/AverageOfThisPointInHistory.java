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

import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.cwms.CwmsFlags;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;

import ilex.var.NamedVariable;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.GregorianCalendar;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;

import opendcs.dai.TimeSeriesDAI;

@Algorithm(name="Average of This Point in History",
           description = "Intended primary for daily data, given a fixed day, go back in time to average that day over a period of time.\n" +
                         " Algorithm was created to operate on generic intervals but has not been tested for them.")
public class AverageOfThisPointInHistory extends AW_AlgorithmBase
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AverageOfThisPointInHistory.class);

        // input values, declare a variable, and add the string of the variable name to the _inputNames array
    @Input(type = Double.class)
    public double input;




    private int intervalCalConstant= -1;
    private GregorianCalendar cal = null;



    @Output(type = Double.class, typeCode = "o")
    public NamedVariable average = new NamedVariable("average",0);


    @org.opendcs.annotations.PropertySpec(value = "years", description = "Interval on week we go back in time." )
    public String interval = "years";
    @org.opendcs.annotations.PropertySpec(value = "10", description = "How many intervals we go back.")
    public int    numberOfIntervals = 10;

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm( )
        throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;
        intervalCalConstant = IntervalCodes.getInterval(interval).getCalConstant();
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
        cal = new GregorianCalendar( this.aggTZ );
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
        int num = 0;
        double storage =0.0;
        if( !isMissing( input ))
        {
            storage = input;
            num++;
        }
        CTimeSeries ts = this.getParmRef("input").timeSeries;

        for (int i=1; i < numberOfIntervals; i++)
        {
            cal.setTime(_timeSliceBaseTime);
            int current = cal.get(intervalCalConstant);
            cal.set(intervalCalConstant, current-i);
            Date time = cal.getTime();
            TimedVariable tv = null;

            try (TimeSeriesDAI tdao = this.tsdb.makeTimeSeriesDAO())
            {
                debug3("filling ts for this time");
                tdao.fillTimeSeries(ts, time, time);

                log.trace("Search for data at time: {}", time.toString() );
                tv = ts.findWithin(cal.getTime(), 1000);

                log.trace("Value at {} = {}", tv.timeString(), tv.getStringValue() );
                storage += tv.getDoubleValue();
                num++;

            }
            catch (NoConversionException ex)
            {
                log.trace("Non valid value: {}", tv.getStringValue());
                setFlagBits(average, CwmsFlags.VALIDITY_QUESTIONABLE);
            }
            catch( NullPointerException ex)
            {
                log.trace("Missing Value in in window of average");
                setFlagBits(average, CwmsFlags.VALIDITY_QUESTIONABLE);
            }
            catch (DbIoException ex)
            {
                log.atTrace()
                   .setCause(ex)
                   .log("Unable to to retrieveMissing Value in window of average.");
                setFlagBits(average, CwmsFlags.VALIDITY_QUESTIONABLE);
            }
            catch (BadTimeSeriesException ex)
            {
                log.atTrace()
                   .setCause(ex)
                   .log("Issue with timeseries, unable to retrieve value");
                setFlagBits(average, CwmsFlags.VALIDITY_QUESTIONABLE);
            }
        }

        double ave = storage/num;
        log.trace("Average ={} based on {} value(s)", ave, num);
        setOutput(average, ave);
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
    }
}
