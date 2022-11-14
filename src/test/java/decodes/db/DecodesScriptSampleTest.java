package decodes.db;

import static org.junit.jupiter.api.Assertions.*;

import decodes.datasource.EdlPMParser;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedSample;
import decodes.util.ResourceFactory;
import fixtures.DecodesHelper;
import ilex.util.FileLogger;
import ilex.util.Logger;
import ilex.var.NoConversionException;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

final class DecodesScriptSampleTest {

    @BeforeAll
    public static void setup() throws Exception {
        ResourceFactory.instance().initializeFunctionList();
    }

    @Test
    void test_delimiter() throws Exception {
        Arguments testSet = DecodesHelper.getScript("WEB");
        DecodesScript decodesScript = (DecodesScript)testSet.get()[0];
        assertEquals(5, decodesScript.getDecodedSamples().size(), "Sample message contains 5 samples");

        DecodedSample sampleValid = decodesScript.getDecodedSamples().get(0);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 16, 30, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleValid.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals(1.23, sampleValid.getSample().getDoubleValue(), 0.0, "The sample message contains a valid double value");

        // testDecodesScriptSampleMissingStandard
        DecodedSample sampleMissingStandard = decodesScript.getDecodedSamples().get(1);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 16, 45, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleMissingStandard.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleMissingStandard.getTimeSeries().formattedSampleAt(1), "Standard missing symbol is not used correctly");

        // testDecodesScriptSampleMissingCustomWithTabs()
        DecodedSample sampleMissingCustom = decodesScript.getDecodedSamples().get(2);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleMissingCustom.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleMissingCustom.getTimeSeries().formattedSampleAt(2),
            "Custom missing symbol is not used correctly due to tab delimiter");

        // testDecodesScriptSampleMissingCustomWithSpaces()
        DecodedSample sampleError = decodesScript.getDecodedSamples().get(3);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 15, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleError.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleError.getTimeSeries().formattedSampleAt(3),
            "Custom missing symbol is not used correctly due to space delimiter");

        // testDecodesScriptError() {
        sampleError = decodesScript.getDecodedSamples().get(4);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 30, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleError.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("error", sampleError.getTimeSeries().formattedSampleAt(4), "Error sample not correctly identified");
    }
}
