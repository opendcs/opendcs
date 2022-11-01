package decodes.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import decodes.datasource.EdlPMParser;
import decodes.datasource.RawMessage;
import decodes.decoder.DecodedSample;
import decodes.util.ResourceFactory;
import ilex.var.NoConversionException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class DecodesScriptSampleTest {

    private static DecodesScript decodesScript;

    @BeforeAll
    public static void setup() throws Exception {
        ResourceFactory.instance().initializeFunctionList();
        DecodesScript.trackDecoding = true;
        PlatformConfig platformConfig = new PlatformConfig();
        decodesScript = new DecodesScript(platformConfig, "WEB");
        FormatStatement skipheader = new FormatStatement(decodesScript, 1);
        skipheader.label = "skip_header";
        skipheader.format = "C('#', col_labels), /, >skip_header";
        decodesScript.formatStatements.add(skipheader);
        FormatStatement collabels = new FormatStatement(decodesScript, 2);
        collabels.label = "col_labels";
        collabels.format = "2/, >timezone";
        decodesScript.formatStatements.add(collabels);
        FormatStatement timezone = new FormatStatement(decodesScript, 3);
        timezone.label = "timezone";
        timezone.format = "3(S(30,'\\t',data), w), F(TZ,A,9D'\\t'), 1P, >datetime";
        decodesScript.formatStatements.add(timezone);
        FormatStatement datetime = new FormatStatement(decodesScript, 4);
        datetime.label = "datetime";
        datetime.format = "2(S(30,'\\t',data), w), F(D,A,10,1), w, F(T,A,5), w, S(30,'\\t',data), w, >data";
        decodesScript.formatStatements.add(datetime);
        FormatStatement data = new FormatStatement(decodesScript, 5);
        data.label = "data";
        data.format = "setMissing(Ssn), F(S,A,5d' \\t',1), /, >timezone";
        decodesScript.formatStatements.add(data);

        ScriptSensor stage = new ScriptSensor(decodesScript, 1);
        stage.rawConverter = new UnitConverterDb("raw", "ft");
        stage.rawConverter.algorithm = Constants.eucvt_none;
        decodesScript.scriptSensors.add(stage);
        ConfigSensor configSensor = new ConfigSensor(decodesScript.platformConfig, 1);

        decodesScript.platformConfig.addSensor(configSensor);


        Platform tmpPlatform = new Platform();
        tmpPlatform.setSite(new Site());
        tmpPlatform.getSite().addName(new SiteName(tmpPlatform.getSite(), "USGS", "dummy"));
        tmpPlatform.setConfig(decodesScript.platformConfig);
        tmpPlatform.setConfigName(decodesScript.platformConfig.configName);
        String mediumType = Constants.medium_Goes;
        TransportMedium tmpMedium = new TransportMedium(tmpPlatform, mediumType, "11111111");
        tmpMedium.scriptName = decodesScript.scriptName;
        tmpMedium.setDecodesScript(decodesScript);
        tmpPlatform.transportMedia.add(tmpMedium);

        String sampleMessage =
            String.join("\n", Files.readAllLines(Paths.get(DecodesScriptSampleTest.class.getResource("sample_message.txt").toURI())));
        int len = sampleMessage.length();
        RawMessage rawMessage = new RawMessage(sampleMessage.getBytes(), len);
        rawMessage.setPlatform(tmpPlatform);
        rawMessage.setTransportMedium(tmpMedium);
        String tz = "UTC";
        tmpMedium.setTimeZone(tz);
        tmpMedium.setMediumType(Constants.medium_EDL);
        rawMessage.setMediumId("11111111");
        EdlPMParser parser = new EdlPMParser();
        parser.parsePerformanceMeasurements(rawMessage);
        Date timeStamp = new Date();
        rawMessage.setTimeStamp(timeStamp);
        decodesScript.prepareForExec();
        tmpMedium.prepareForExec();
        decodesScript.decodeMessage(rawMessage);
        assertEquals(5, decodesScript.getDecodedSamples().size(), "Sample message contains 5 samples");
    }

    @Test
    void testDecodesScriptSetValid() throws NoConversionException {
        DecodedSample sampleValid = decodesScript.getDecodedSamples().get(0);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 16, 30, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleValid.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals(1.23, sampleValid.getSample().getDoubleValue(), 0.0, "The sample message contains a valid double value");
    }

    @Test
    void testDecodesScriptSampleMissingStandard() {
        DecodedSample sampleMissingStandard = decodesScript.getDecodedSamples().get(1);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 16, 45, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleMissingStandard.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleMissingStandard.getTimeSeries().formattedSampleAt(1), "Standard missing symbol is not used correctly");
    }

    @Test
    void testDecodesScriptSampleMissingCustomWithTabs() {
        DecodedSample sampleMissingCustom = decodesScript.getDecodedSamples().get(2);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 0, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleMissingCustom.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleMissingCustom.getTimeSeries().formattedSampleAt(2),
            "Custom missing symbol is not used correctly due to tab delimiter");
    }

    @Test
    void testDecodesScriptSampleMissingCustomWithSpaces() {
        DecodedSample sampleError = decodesScript.getDecodedSamples().get(3);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 15, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleError.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("missing", sampleError.getTimeSeries().formattedSampleAt(3),
            "Custom missing symbol is not used correctly due to space delimiter");
    }

    @Test
    void testDecodesScriptError() {
        DecodedSample sampleError = decodesScript.getDecodedSamples().get(4);
        assertEquals(ZonedDateTime.of(2022, 10, 17, 17, 30, 0, 0, ZoneId.of("UTC")).toInstant(),
            sampleError.getSample().getTime().toInstant(), "Time should match the parsed sample message");
        assertEquals("error", sampleError.getTimeSeries().formattedSampleAt(4), "Error sample not correctly identified");
    }
}
