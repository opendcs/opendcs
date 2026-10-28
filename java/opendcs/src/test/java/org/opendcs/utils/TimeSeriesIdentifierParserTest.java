package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class TimeSeriesIdentifierParserTest
{
    @Test
    void extractsDisplayNameWithoutTrailingIdentifierWhitespace()
    {
        var result = TimeSeriesIdentifierParser.extractDisplayName(
            "Alder Springs.Precip.Total.1Hour.1Hour.comp (Hourly Precip for Alder Springs)");

        assertEquals("Alder Springs.Precip.Total.1Hour.1Hour.comp", result.first);
        assertEquals(Optional.of("Hourly Precip for Alder Springs"), result.second);
    }

    @Test
    void preservesIdentifierWithoutDisplayName()
    {
        String identifier = "Alder Springs.Precip.Total.1Hour.1Hour.comp";
        var result = TimeSeriesIdentifierParser.extractDisplayName(identifier);

        assertEquals(identifier, result.first);
        assertEquals(Optional.empty(), result.second);
    }
}
