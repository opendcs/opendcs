package decodes.tsdb.algo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import ilex.var.NamedVariable;

import decodes.tsdb.DbCompException;

import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ParmRef;

import decodes.util.PropertySpec;
import org.opendcs.annotations.algorithm.Algorithm;
import org.opendcs.annotations.algorithm.Input;
import org.opendcs.annotations.algorithm.Output;

@Algorithm(description = "Convert a short interval to a longer interval by taking the first value equal-to or after the longer-period timestamp.\n" +
"Example: Convert 10min data to 30min data by taking data on the hour and half-hour")
public class SubSample extends decodes.tsdb.algo.AW_AlgorithmBase
{
    @Input
    public double inputShortInterval;

    private LocalDateTime nextOutput = null; // LocalDateTime value used for interval math
    private TemporalAmount outputIncr = null;

    @Output(type = Double.class)
    public NamedVariable outputLongInterval = new NamedVariable("outputLongInterval", 0);


    @org.opendcs.annotations.PropertySpec(propertySpecType = PropertySpec.STRING, 
                                          description = "(optional) E.g. for a daily subsample: '6 Hours' to grab the 6AM value.",
                                          value = "")
    public String samplingTimeOffset = "";

    // Allow javac to generate a no-args constructor.

    /**
     * Algorithm-specific initialization provided by the subclass.
     */
    @Override
    protected void initAWAlgorithm( )
        throws DbCompException
    {
        _awAlgoType = AWAlgoType.TIME_SLICE;
    }

    /**
     * This method is called once before iterating all time slices.
     */
    @Override
    protected void beforeTimeSlices()
        throws DbCompException
    {

        // Initialize it to the first time >= the first input time.
        Date firstInputT = baseTimes.first();
        nextOutput = LocalDateTime.ofInstant(firstInputT.toInstant(), aggTZ.toZoneId());
        LocalDateTime firstInputLDT = nextOutput;

        ParmRef outputParmRef = getParmRef("outputLongInterval");

        final IntervalIncrement outputIncrTmp = IntervalCodes.getIntervalCalIncr(
            outputParmRef.compParm.getInterval());
        if (outputIncrTmp == null || outputIncrTmp.getCount() == 0)
            throw new DbCompException("SubSample requires regular interval output!");

        outputIncr = outputIncrTmp.toTemporalAmount();
        debug3("beforeTimeSlices firstInputT=" + nextOutput.format(debugLDTF) + " outputIncr = " + outputIncr
            + (samplingTimeOffset!= null ? (", samplingTimeOffset=" + samplingTimeOffset) : ""));


        // MJM Added samplingTimeOffset processing for OpenDCS 6.2 RC03
        IntervalIncrement offsetIncr[] = null;
        if (samplingTimeOffset != null && samplingTimeOffset.trim().length() > 0)
        {
            try
            {
                offsetIncr = IntervalIncrement.parseMult(samplingTimeOffset);
                debug1("Honoring sampling time offset '" + samplingTimeOffset + "'");
            }
            catch (NoSuchObjectException ex)
            {
                warning("Invalid samplingTimeOffset property '" + samplingTimeOffset + "': " + ex
                    + " -- ignored.");
                offsetIncr = null;
            }
        }

        // Truncate to top of interval
        Instant currentInstant = nextOutput.toInstant(ZoneOffset.ofHours(0));
        long adjustmentMilliseconds = currentInstant.toEpochMilli() % (outputIncr.get(ChronoUnit.SECONDS)*1000);
        nextOutput = nextOutput.minus(adjustmentMilliseconds, ChronoUnit.MILLIS);
        // If an offset was supplied, add it. Note: it's up to the user to make sure
        // it makes sense. Good: output interval = 1Day, offsetIncr = 6 hours.
        // Bad: output interval = 1 hour, offsetIncr = 1 week.

        if (offsetIncr != null)
        {
            for(IntervalIncrement ii : offsetIncr)
            {

                nextOutput = nextOutput.plus(ii.toTemporalAmount());
            }
        }

        // Because of the added increment, I could end up with an outputCal time that
        // is before the first input time.
        // Example Daily average at 6 AM from hourly inputs, and the first value I'm given is 7 AM.
        // The above code will set outputCal to 6AM. I need to add the output increment so that
        // the outputCal is always >= the first input time.

        while(nextOutput.isBefore(firstInputLDT))
        {
            debug3("beforeTimeSlices firstInputT=" + firstInputLDT.format(debugLDTF)
                + ", outputCal=" + nextOutput.format(debugLDTF)
                + ", incr=" + outputIncr);
            nextOutput = nextOutput.plus(outputIncr);
        }

        // Normally for copy, output units will be the same as input.
        String inUnits = getInputUnitsAbbr("inputShortInterval");
        if (inUnits != null && inUnits.length() > 0)
        {
            setOutputUnitsAbbr("outputLongInterval", inUnits);
        }

        debug1("first input=" + firstInputLDT.format(debugLDTF)
            + ", first output=" + nextOutput.format(debugLDTF)
            + " outputIncr=" + outputIncr.toString());
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
    @Override
    protected void doAWTimeSlice()
        throws DbCompException
    {
        LocalDateTime currentTime = LocalDateTime.ofInstant(_timeSliceBaseTime.toInstant(), aggTZ.toZoneId());
        Duration delta = Duration.between(nextOutput, currentTime);
        final long deltaSec = delta.toSeconds();
        if (deltaSec <= roundSec && deltaSec >= -roundSec)
        {
            ZonedDateTime zdt = currentTime.atZone(aggTZ.toZoneId());
            zdt = zdt.withZoneSameInstant(ZoneId.of("UTC"));
            debug1("Outputting value '" + inputShortInterval + "' at " + nextOutput.format(debugLDTF)
                + ", deltaSec=" + deltaSec + ", timeSlice="
                + currentTime.format(debugLDTF)+"/"+zdt.format(debugZDTF));

            setOutput(outputLongInterval, inputShortInterval,Date.from(zdt.toInstant()));
        }

        // Regardless of whether te above produced an output, the
        // next output time should always be > current time slice
        while (!nextOutput.isAfter(currentTime))
        {
            nextOutput = nextOutput.plus(outputIncr);

        }
        ZonedDateTime zdt = nextOutput.atZone(aggTZ.toZoneId());
        zdt = zdt.withZoneSameInstant(ZoneId.of("UTC"));
        debug1("Advanced nextOutput to be at " + nextOutput.format(debugLDTF) + "/" + zdt.format(debugZDTF));
    }

    /**
     * This method is called once after iterating all time slices.
     */
    @Override
    protected void afterTimeSlices()
        throws DbCompException
    {
    }
}
