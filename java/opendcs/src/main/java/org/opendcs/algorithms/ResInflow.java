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

import ilex.var.NamedVariable;

import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

import decodes.db.EngineeringUnit;
import decodes.db.UnitConverter;
import decodes.util.DecodesException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.opendcs.annotations.PropertySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Algorithm(description=
    "Calculates reservoir inflow with the equation\n" +
    "  inflow = ΔStorage + Evap + Outflow\n" +
    "\n" +
    "Outflow should be provided in cfs\n" +
    "Storage and Evap should be provided in ac-ft and will be converted to cfs based on the\n" +
    "interval of data.\n" +
    "(This comp will need to be created 3 times for each project: 15 minutes, 1 hour, and 1 day.)\n" +
    "There may be a way to group things.\n" +
    "\n" +
    "NOTE: There are ac-ft to cfs conversions built into this comp; do NOT use metric input—the comp\n" +
    "      will provide bogus results."
)
public class ResInflow extends decodes.tsdb.algo.AW_AlgorithmBase
{
    private static final Logger log = LoggerFactory.getLogger(ResInflow.class);

    @Input
    public double ResOut;
    @Input
    public double Evap;
    @Input(typeCode = "id", description = "Change in storage between this and the previous time slice")
    public double deltaStorage;



    // Enter any local class variables needed by the algorithm.
    UnitConverter storConv;
    UnitConverter evapConv;
    UnitConverter flowConv;
    UnitConverter outputConv;

    @Output(type = Double.class)
    public NamedVariable ResIn = new NamedVariable("ResIn", 0);

    @PropertySpec(description = "Should an evaporation volume be included in the calculation.", value = "false")
    public boolean UseEvap = false;

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm( ) throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;

        // Code here will be run once, after the algorithm object is created.
        evapConv = null;
        storConv = null;
        flowConv = null;
        outputConv = null;
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
        EngineeringUnit storUnits = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("Dstor"));
        EngineeringUnit flowUnits = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("ResOut"));
        EngineeringUnit outputUnits = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("ResIn"));

        if (UseEvap)
        {
            EngineeringUnit evapUnits = EngineeringUnit.getEngineeringUnit(getParmUnitsAbbr("Evap"));
            if(evapConv == null)
            {
                evapConv = decodes.db.CompositeConverter.build( evapUnits, EngineeringUnit.getEngineeringUnit("ac-ft"));
            }
        }
        if (storConv == null)
        {
            storConv = decodes.db.CompositeConverter.build(storUnits, EngineeringUnit.getEngineeringUnit("ac-ft"));
        }
        if (flowConv == null)
        {
            flowConv = decodes.db.CompositeConverter.build(flowUnits, EngineeringUnit.getEngineeringUnit("cfs"));
        }
        if (outputConv == null)
        {
            outputConv = decodes.db.CompositeConverter.build(EngineeringUnit.getEngineeringUnit("cfs"), outputUnits );
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
        double in = 0;
        double conversion = 1.0;
        String interval = this.getInterval("ResOut").toLowerCase();
        log.trace("Finding conversion for internval: {}", interval);
        if( interval.equals( "15minutes"))
        {
            // y ac-ft = x cfs * 900 sec * 2.29568411*10^-5 ac-ft/ft^3
            // 900 seconds = 15 minutes
            // we are converting to cfs from ac-ft so divided by this factor
            conversion = 1/.020618557;
        }
        else if( interval.equals( "1hour"))
        {
            conversion = 12.1;
        }
        else if( interval.equals( "1day") || interval.equals( "~1day") )
        {
            //long time_difference = (_aggregatePeriodEnd.getTime() - _aggregatePeriodBegin.getTime())/(1000*60*60);
            //long time_difference = 24;
            GregorianCalendar t1 = new GregorianCalendar();
            GregorianCalendar t2 = new GregorianCalendar();
            t1.setTimeZone(aggTZ);
            t2.setTimeZone(aggTZ);
            t1.setTime(_timeSliceBaseTime);
            t2.setTime(_timeSliceBaseTime);
            /*
             * TODO: This might need to be t1-1day
             */

            t1.add(Calendar.DAY_OF_MONTH, -1);
            log.trace("Start of Interval {}",  t1.getTime());
            log.trace("End of Interval {}", t2.getTime());
            // since these are the UTC values we'll get the right time difference
            long time_difference = (t2.getTimeInMillis() - t1.getTimeInMillis()) / (1000*60*60);

            if( time_difference == 24 )
            {
                conversion = 0.50417;
            }
            else if (time_difference == 23)
            {
                conversion = 0.52609;
            }
            else if (time_difference == 25)
            {
                conversion = 0.48400;
            }
            else
            {
                throw new DbCompException(
                      "You have entered some weird daylight savings that adjusts by more than an hour..."
                    + "if this is true, 1st: sorry. 2nd: see the programmer about expanding this section of the code" );
            }

        }
        log.trace("Conversion factor = {}", conversion);

        try
        {
            log.trace("Convert the units");
            deltaStorage = storConv.convert(deltaStorage);
            ResOut = flowConv.convert(ResOut);

            if (UseEvap)
            {
                // note there are notes with the WC manuals that indicate
                // that on the 23 and 25 hours days (because of daylight savings)
                // other factors should be used. This is an initial test
                // so that will be taken into account later, or
                // i'll keep fighting for doing everything in PST
                // like the memo that was found says to.
                // M. Neilson. 2011Nov16.
                Evap = evapConv.convert(Evap);

                double flow = (deltaStorage+Evap)*conversion;
                log.trace("performing calculation with evap");
                log.trace("Dstor = {}, Evap = {}, Outflow = {}", deltaStorage, Evap, ResOut);
                log.trace("Dstor + Evap = {} -(cfs)-> {}", (deltaStorage+Evap), flow);
                in = flow + ResOut;
            }
            else
            {
                log.trace("performing calculation without evap");
                log.trace("Dstor = {}, Outflow = {}", deltaStorage, ResOut);
                log.trace("Dstor  in cfs = {}" + (deltaStorage*conversion));
                // The 15 minute and hourly calculation do not use evap
                in = deltaStorage*conversion + ResOut;
            }

            in = outputConv.convert(in);
            log.trace("Inflow = {} {}", in, outputConv.getToAbbr());
            setOutput( ResIn, in );
        }
        catch( DecodesException ex)
        {
            throw new DbCompException("There are no conversion from the units you provided to the needed units", ex);
        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
    }
}
