package org.opendcs.algorithms;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.var.NamedVariable;
import decodes.tsdb.DbCompException;
import decodes.tsdb.algo.AWAlgoType;

//AW:IMPORTS
//AW:IMPORTS_END

//AW:JAVADOC
/**
 * Over the desired interval, pick the current maximum.
 * previous maximum within the interval should be deleted
 *
 * @author L2EDDMAN
 */
//AW:JAVADOC_END
public class MaxToDate extends decodes.tsdb.algo.AW_AlgorithmBase
{
    private static final Logger log = LoggerFactory.getLogger(MaxToDate.class);
//AW:INPUTS
    public double input; //AW:TYPECODE=i
    String _inputNames[] = { "input" };
//AW:INPUTS_END

//AW:LOCALVARS
    private Date   max_date;
    private double current_max;
    private int    numSamples;
//AW:LOCALVARS_END

//AW:OUTPUTS
    public NamedVariable max = new NamedVariable("max", 0);
    String _outputNames[] = { "max" };
//AW:OUTPUTS_END

//AW:PROPERTIES
    public double minSamples = 0;

    String _propertyNames[] = { "minSamples"  };
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
        _aggPeriodVarRoleName = "max";
//AW:INIT_END

//AW:USERINIT
//AW:USERINIT_END
    }

    /**
     * This method is called once before iterating all time slices.
     */
    protected void beforeTimeSlices()
        throws DbCompException
    {
//AW:BEFORE_TIMESLICES
        // This will allow the first value to be initially selected
        current_max = -Double.MAX_VALUE;
        max_date = new Date(); // just need a starting value, it isn't compared in any way.
        numSamples = 0;
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
        if (isMissing(input ))
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
//AW:TIMESLICE_END
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
//AW:AFTER_TIMESLICES
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
