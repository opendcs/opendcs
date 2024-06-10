package org.opendcs.algorithms;

import java.util.Date;

import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;

import ilex.var.NamedVariable;

import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
 * TrapezoidalIntegrationAverage implements the trapezoidal integration method
 * of average current used in DSS for instantaneous data. It makes sense but
 * the only reference I've found is a statement in a USGS manual that it will
 * be used.
 *
 * See any calculus text section on trapezoidal integration for more detail.
 *
 * @author L2EDDMAN
 *
 */
//AW:JAVADOC_END
@Algorithm(description = "TrapezoidalIntegrationAverage implements the trapezoidal integration method\n"
                       + "of average current used in DSS for instantaneous data. It makes sense but\n"
                       + "the only reference I've found is a statement in a USGS manual that it will\n"
                       + "be used.\n"
                       + "\n"
                       + "See any calculus text section on trapezoidal integration for more detail.")
public class TrapezoidalIntegrationAverage extends decodes.tsdb.algo.AW_AlgorithmBase
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TrapezoidalIntegrationAverage.class);
    @Input
    public double input;
    String _inputNames[] = { "input" };

    int count;
    Date previous_time;
    Date first_time;
    double previous_value;
    double tally;
    long start_t;
    long end_t;

    @Output
    public NamedVariable average = new NamedVariable("average", 0);
    String _outputNames[] = { "average" };


    @org.opendcs.annotations.PropertySpec(value="2", description = "Minimum samples needs for a given output average to be valid.")
    public long minSamplesNeeded = 2;
    String _propertyNames[] = { "minSamplesNeeded" };


    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    protected void initAWAlgorithm() throws DbCompException
    {
        _awAlgoType = AWAlgoType.AGGREGATING;
        _aggPeriodVarRoleName = "average";
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
        // Zero out the tally & count for this agg period.
        count = 0;
        tally = 0.0;
        // Normally for average, output units will be the same as input.
        String inUnits = getInputUnitsAbbr("input");
        if (inUnits != null && inUnits.length() > 0)
        {
            setOutputUnitsAbbr("average", inUnits);
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
    protected void doAWTimeSlice() throws DbCompException
    {
        double average_height;
        double time_diff;
        double volume;
        log.debug("TrapezoidalIntegrationAverage:doAWTimeSlice, input={}", input);
        if (!isMissing(input))
        {
            if( count == 0 )
            {
                log.debug("First valid value of time slice, setting initial parameters");
                //this is the first point in the time slice
                previous_time = _timeSliceBaseTime;
                first_time = _timeSliceBaseTime;
                previous_value = input;
            }
            else
            {
                log.trace("Next valid value of the time window, finding new volume");
                // perform the section of trapezoidal integration
                time_diff = (_timeSliceBaseTime.getTime() - previous_time.getTime())/1000.0;
                average_height = 0.5*(input+previous_value);
                volume = average_height*time_diff;
                log.trace("Additional Volume: {}", volume);
                tally += average_height*time_diff; // increase volume by this sets ammount
                log.trace("New Total Value: {}", tally);
                previous_time = _timeSliceBaseTime;
                previous_value = input;
            }
            count++;
        }
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {

        log.debug("AverageAlgorithm:afterTimeSlices, count={}", count);
        if (count >= minSamplesNeeded)
        {
            double time_diff = (previous_time.getTime() - first_time.getTime())/1000.0;
            log.trace("Final Volume is {}", tally );
            log.trace("Averaging over {} seconds", time_diff);
            setOutput(average, tally/time_diff );
        }
        else
        {
            log.warn("Do not have minimum # samples ({}) -- not producing an average.", minSamplesNeeded);
            if (_aggInputsDeleted)
            {
                deleteOutput(average);
            }
        }
        end_t = java.lang.System.nanoTime();
        long diff = end_t - start_t;
        double diff_ms = diff/1000000.0;
        log.trace(" Elapsed Time (ns) " + diff );
        log.trace("              (ms) " + diff_ms);
    }
}
