package org.opendcs.algorithms;

import decodes.tsdb.algo.AWAlgoType;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.util.PropertySpec;
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

import org.opendcs.annotations.PropertySpecAnno;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;
import org.slf4j.LoggerFactory;

import opendcs.dai.TimeSeriesDAI;

//AW:IMPORTS
// Place an import statements you need here.
//AW:IMPORTS_END

//AW:JAVADOC
/**
 * Intended primary for daily data, given a fixed day, go back in time to average that day over a period of time.
 * Algorithm was created to operate on generic intervals but has not been tested for them.
 * @author L2EDDMAN
 */
//AW:JAVADOC_END
@Algorithm(name="Average of This Point in History",
           description = "Intended primary for daily data, given a fixed day, go back in time to average that day over a period of time.\n" +
                         " Algorithm was created to operate on generic intervals but has not been tested for them.")
public class AverageOfThisPointInHistory extends AW_AlgorithmBase
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AverageOfThisPointInHistory.class);
//AW:INPUTS
        // input values, declare a variable, and add the string of the variable name to the _inputNames array
        @Input(type = Double.class)
        public double input;   //AW:TYPECODE=i
    String _inputNames[] = { "input"};
//AW:INPUTS_END

//AW:LOCALVARS
    // Enter any local class variables needed by the algorithm.
        private int interval_cal_costant= -1;
        private GregorianCalendar cal = null;
//AW:LOCALVARS_END

//AW:OUTPUTS
    // created a NameVariable with the name you want, and add the string of that name to the array
        @Output(type = Double.class, typeCode = "o")
        public NamedVariable average = new NamedVariable("average",0);
    String _outputNames[] = { "average" };
//AW:OUTPUTS_END

//AW:PROPERTIES
        @PropertySpecAnno(propertySpecType = PropertySpec.STRING, value = "years", description = "Interval on week we go back in time." )
        public String interval = "years";
        @PropertySpecAnno(propertySpecType = PropertySpec.INT, value = "10", description = "How many intervals we go back.")
        public int    numberOfIntervals = 10;
    String _propertyNames[] = { "interval", "numberOfIntervals" };
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
        interval_cal_costant = IntervalCodes.getInterval(interval).getCalConstant();
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
        // This code will be executed once before each group of time slices.
        // For TimeSlice algorithms this is done once before all slices.
        // For Aggregating algorithms, this is done before each aggregate
        // period.
        cal = new GregorianCalendar( this.aggTZ );
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
        // Enter code to be executed at each time-slice.
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
            int current = cal.get(interval_cal_costant);
            cal.set(interval_cal_costant, current-i);
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
//AW:TIMESLICE_END
    }

    /**
     * This method is called once after iterating all time slices.
     */
    protected void afterTimeSlices()
        throws DbCompException
    {
//AW:AFTER_TIMESLICES
        // This code will be executed once after each group of time slices.
        // For TimeSlice algorithms this is done once after all slices.
        // For Aggregating algorithms, this is done after each aggregate
        // period.

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
}
