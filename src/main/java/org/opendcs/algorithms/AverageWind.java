package org.opendcs.algorithms;

import org.slf4j.LoggerFactory;

import ilex.var.NamedVariable;

import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

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
public class AverageWind extends decodes.tsdb.algo.AW_AlgorithmBase
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(AverageWind.class);
//AW:INPUTS
    public double speed;    //AW:TYPECODE=i
    public double dir;    //AW:TYPECODE=i
    String _inputNames[] = { "speed", "dir" };
//AW:INPUTS_END

//AW:LOCALVARS
    long start_t;
    long end_t;
    ArrayList<Double> u;
    ArrayList<Double> v;
//AW:LOCALVARS_END

//AW:OUTPUTS
    public NamedVariable average_speed = new NamedVariable("average_speed", 0);
    public NamedVariable average_dir = new NamedVariable("average_dir", 0);
    String _outputNames[] = { "average_speed", "average_dir" };
//AW:OUTPUTS_END

//AW:PROPERTIES
    public long minSamplesNeeded = 1;
    String _propertyNames[] = { "minSamplesNeeded" };
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
        _aggPeriodVarRoleName = "average_speed";
//AW:INIT_END

//AW:USERINIT
        // No one-time init required.
//AW:USERINIT_END
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
//AW:BEFORE_TIMESLICES
        // Zero out the tally & count for this agg period.
        if (u == null)
        {
            u = new ArrayList<Double>();
        }
        else
        {
            u.clear();
        }

        if (v == null)
        {
            v = new ArrayList<Double>();
        }
        else
        {
            v.clear();
        }


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
//AW:TIMESLICE_END
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
//AW:AFTER_TIMESLICES
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
