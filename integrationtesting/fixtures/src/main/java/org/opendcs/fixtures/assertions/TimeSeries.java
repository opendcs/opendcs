package org.opendcs.fixtures.assertions;

import java.util.Date;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Assertions;

import decodes.tsdb.CTimeSeries;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

public class TimeSeries
{
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, String message) throws Exception
    {
        assertEquals(expected, actual, .0001, message, null, null);
    }
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, double delta, String message)
    {
        assertEquals(expected, actual, delta, message, null, null);
    }
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, String message, Date start, Date end)
    {
        assertEquals(expected, actual, .0001, message, start, end);
    }
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, double delta, String message, Date start, Date end)
    {
        try
        {
            /**
             * The CTimeSeries and TimeSeriesIdentifier objects get populated in different ways at different times.
             * As the unique string and key are supposed to be always unique we will assume that here for the comparison.
             * The current TimeSeriesIdentifier equals sets both DbKey and UniqueString and there are
             * several cases were the Unique String is initially used and the DbKey is never correctly populated
             */
            if ( !(expected.getTimeSeriesIdentifier() == actual.getTimeSeriesIdentifier()
                || expected.getTimeSeriesIdentifier().getKey().equals(actual.getTimeSeriesIdentifier().getKey())
                || expected.getTimeSeriesIdentifier().getUniqueString().equals(actual.getTimeSeriesIdentifier().getUniqueString())))
            {
                AssertionFailureBuilder.assertionFailure()
                                    .reason("Time Series Identifiers do not match.")
                                    .message(message)
                                    .actual(actual.getTimeSeriesIdentifier())
                                    .expected(expected.getTimeSeriesIdentifier())
                                    .buildAndThrow();
            }
            else if (!expected.getUnitsAbbr().equals(actual.getUnitsAbbr()))
            {
                AssertionFailureBuilder.assertionFailure()
                                    .reason("Time Series units do not match.")
                                    .message(message)
                                    .actual(actual.getUnitsAbbr())
                                    .expected(expected.getUnitsAbbr())
                                    .buildAndThrow();
            }
            
            int expectedElements = elementsInTimeWindow(expected, start, end);
            int actualElements = elementsInTimeWindow(actual, start, end);
            if(expectedElements != actualElements)
            {
                AssertionFailureBuilder.assertionFailure()
                                    .reason("Time series do not have the same number of elements.")
                                    .message(message)
                                    .actual(actualElements)
                                    .expected(expectedElements)
                                    .buildAndThrow();
            }
            else
            {
                int expectedIndex = 0;
                int actualIndex = 0;
                if(start != null && end != null){
                    expectedIndex = expected.findNextIdx(start);
                    actualIndex = actual.findNextIdx(start);
                }
                for (int i = actualIndex; actualIndex - i < actualElements; expectedIndex++, actualIndex++)
                {
                    final TimedVariable expectedVar = expected.sampleAt(expectedIndex);
                    final TimedVariable actualVar = actual.sampleAt(actualIndex);
                    final Date expectedDate = expectedVar.getTime();
                    final Date actualDate = actualVar.getTime();

                    if (expectedDate.getTime() != actualDate.getTime())
                    {
                        AssertionFailureBuilder.assertionFailure()
                                            .reason("Times at position " + i + " do not match.")
                                            .message(message)
                                            .actual(actualDate)
                                            .expected(expectedDate)
                                            .buildAndThrow();
                    }
                    if (actualVar.isNumeric() )
                    {
                        double expectedValue = expectedVar.getDoubleValue();
                        double actualValue = actualVar.getDoubleValue();
                        Assertions.assertEquals(expectedValue,actualValue, delta, "Values at position " + i + "(" + actualDate +") do not match");

                    }
                }
            }
        }
        catch (Exception ex)
        {
            AssertionFailureBuilder.assertionFailure()
                                   .reason("Exception was thrown during time series processing")
                                   .message(message)
                                   .cause(ex)
                                   .buildAndThrow();
        }
    }

    private static int elementsInTimeWindow(CTimeSeries cts, Date start, Date end) throws NoConversionException{
        if(start == null || end == null){
            return cts.size();
        }
        int elementCount = 0;
        int index = cts.findNextIdx(start);
        TimedVariable tv = cts.sampleAt(index);
        while(tv != null){
            try {
                Date time  = tv.getDateValue();
                if(time.compareTo(end) >= 0){
                    elementCount++;
                    tv = cts.sampleAt(index+elementCount);
                }
                else{
                    break;
                }
            } catch (NoConversionException ex) {
                throw new NoConversionException("Failed to get Date value for timed variable");
            }
        }
        return elementCount;
    }
}
