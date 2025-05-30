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

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;


@Algorithm(name = "Max To Date", description = "Over the desired interval, pick the current maximum.\n"
                                             + "Previous maximum within the interval should be deleted")
public class MaxToDate extends decodes.tsdb.algo.AW_AlgorithmBase
{
    private static final Logger log = LoggerFactory.getLogger(MaxToDate.class);

    @Input
    public double input;


    private Date   max_date;
    private double current_max;
    private int    numSamples;

    @Output(type = Double.class)
    public NamedVariable max = new NamedVariable("max", 0);

    @org.opendcs.annotations.PropertySpec(value="0.0", description = "Minimum number of samples required before we consider this result valid.")
    public double minSamples = 0.0;


    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm( )
        throws DbCompException
    {
        _awAlgoType = AWAlgoType.AGGREGATING;
        _aggPeriodVarRoleName = "max";

    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
        // This will allow the first value to be initially selected
        current_max = -Double.MAX_VALUE;
        max_date = new Date(); // just need a starting value, it isn't compared in any way.
        numSamples = 0;
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
        if (isMissing(input))
        {
            log.trace("Skipping missing value.");
            return;
        }
        else
        {
            log.trace("Current Value is: {}", input);
            log.trace("Current D/T is  : {}", _timeSliceBaseTime);
            log.trace("Current Max is  : {}", current_max);
            log.trace("Date of Max is  : {}", max_date);
            numSamples++;

            if (input > current_max)
            {
                log.trace("Setting new maximum");
                current_max = input;
                max_date = _timeSliceBaseTime;
            }
        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
        log.trace("Selected Max is: {}", current_max);
        log.trace("Selected Max Date is: {}", max_date);
        if (numSamples >= minSamples)
        {
            setOutput(max, current_max);
        }
        else
        {
            log.trace("Only {} samples found, minium number specified is {}", numSamples, minSamples );
            log.trace("selected min/max will not be saved");
        }
    }
}
