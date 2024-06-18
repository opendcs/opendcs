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

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;
import decodes.util.PropertySpec;

//AW:IMPORTS
import java.util.ArrayList;
//AW:IMPORTS_END

//AW:JAVADOC
/**
 * Vector average of wind data.
 *
 * See any calculus text section on trapezoidal integration for more detail.
 *
 * @author L2EDDMAN
 *
 */
//AW:JAVADOC_END
@Algorithm(description = " Vector average of wind data.\n"
                       + "\n"
                       + "See any calculus text section on trapezoidal integration for more detail.\n")
public class AverageWind extends decodes.tsdb.algo.AW_AlgorithmBase
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(AverageWind.class);

    @Input
    public double speed;
    @Input
    public double dir;
    String _inputNames[] = { "speed", "dir" };


    long start_t;
    long end_t;
    final ArrayList<Double> u = new ArrayList<>();
    final ArrayList<Double> v = new ArrayList<>();



    @Output(type = Double.class)
    public NamedVariable average_speed = new NamedVariable("average_speed", 0);
    @Output(type = Double.class)
    public NamedVariable average_dir = new NamedVariable("average_dir", 0);
    String _outputNames[] = { "average_speed", "average_dir" };



    @org.opendcs.annotations.PropertySpec(propertySpecType = PropertySpec.INT,
                                          value = "1",
                                          description = "Minimum number of sample if which present an output will be calculated.")
    public long minSamplesNeeded = 1;
    String _propertyNames[] = { "minSamplesNeeded" };


    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm( )
        throws DbCompException
    {
        _awAlgoType = AWAlgoType.AGGREGATING;
        _aggPeriodVarRoleName = "average_speed";
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
        u.clear();
        v.clear();

        // Normally for average, output units will be the same as input.
        String inUnits = getInputUnitsAbbr("speed");
        if (inUnits != null && inUnits.length() > 0)
        {
            setOutputUnitsAbbr("average_speed", inUnits);
        }

        inUnits = getInputUnitsAbbr("dir");
        if (inUnits != null && inUnits.length() > 0)
        {
            setOutputUnitsAbbr("average_dir", inUnits);
        }

        start_t = java.lang.System.nanoTime();
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
        double _u,_v;
        if (!isMissing(speed) && !isMissing(dir))
        {
            double r = Math.toRadians(dir);
            _u = speed * Math.cos(r);
            _v = speed * Math.sin(r);
            log.trace("Converted input spd,deg ({},{}) to u,v ({},{})", speed,dir,_u,_v);
            u.add(_u);
            v.add(_v);
        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
        int count = u.size();
        log.debug("Have count={}", count);
        //debug1("AverageAlgorithm:afterTimeSlices, per begin="
        //+ debugSdf.format(_aggregatePeriodBegin) + ", end=" + debugSdf.format(_aggregatePeriodEnd));
        if (count >= minSamplesNeeded)
        {
            double u_avg = 0;
            double v_avg = 0;

            for (int i = 0; i < u.size(); i ++)
            {
                u_avg += u.get(i);
                v_avg += v.get(i);
            }
            u_avg= u_avg / u.size();
            v_avg = v_avg/ v.size();

            double avg_speed = Math.sqrt( u_avg*u_avg + v_avg*v_avg );
            double avg_dir = Math.toDegrees( Math.atan2(u_avg, v_avg) );
            if (avg_dir < 0)
            {
                avg_dir += 360.0;
            }
            log.trace("Average spd,dir ({},{}) from avg u,v ({},{})", avg_speed,avg_dir, u_avg,v_avg);
            setOutput( average_speed,  avg_speed);
            setOutput( average_dir, avg_dir);
        }
        else
        {
            warning("Do not have minimum # samples (" + minSamplesNeeded
                + ") -- not producing an average.");
            if (_aggInputsDeleted)
            {
                deleteOutput(average_speed);
                deleteOutput(average_dir);
            }
        }
        end_t = java.lang.System.nanoTime();
        long diff = end_t - start_t;
        double diff_ms = diff/1000000.0;
        log.info(" Elapsed Time (ns) {}", diff );
        log.info("              (ms) {}", diff_ms);
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
