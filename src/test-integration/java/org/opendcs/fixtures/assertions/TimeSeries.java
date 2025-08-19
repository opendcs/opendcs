package org.opendcs.fixtures.assertions;

import java.util.Date;

import org.junit.jupiter.api.AssertionFailureBuilder;

import decodes.tsdb.CTimeSeries;
import ilex.var.TimedVariable;

public class TimeSeries
{
    public static void assertEquals(CTimeSeries expected, CTimeSeries actual, String message)
    {
        try
        {
            if (!expected.getTimeSeriesIdentifier().equals(actual.getTimeSeriesIdentifier()))
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
                    if (!expectedVar.getStringValue().equals(actualVar.getStringValue()))
                    {
                        AssertionFailureBuilder.assertionFailure()
                                            .reason("Value at position " + i + " do not match.")
                                            .message(message)
                                            .actual(actualVar.getStringValue())
                                            .expected(expectedVar.getStringValue())
                                            .buildAndThrow();
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
