package org.opendcs.fixtures.assertions;

import java.util.Date;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Assertions;

import decodes.tsdb.CTimeSeries;
import ilex.var.TimedVariable;

public class TimeSeries
{
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, String message)
    {
        assertEquals(expected, actual, .0001, message);
    }
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, double delta, String message)
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
            else if(expected.size() != actual.size())
            {
                AssertionFailureBuilder.assertionFailure()
                                    .reason("Time series do not have the same number of elements.")
                                    .message(message)
                                    .actual(actual.size())
                                    .expected((expected.size()))
                                    .buildAndThrow();
            }
            else
            {
                for (int i = 0; i < actual.size(); i++)
                {
                    final TimedVariable expectedVar = expected.sampleAt(i);
                    final TimedVariable actualVar = actual.sampleAt(i);
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
                        Assertions.assertEquals(expectedValue,actualValue, delta, "Values at position " + i + " do not match");
                        
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


}
