package decodes.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import decodes.datasource.RawMessage;
import decodes.db.ConfigSensor;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import fixtures.StringConsumer;
import ilex.var.Variable;

class HydrometDMS3FormatterTest
{

    @Test
    void test_formatter() throws Exception
    {
        Site site = new Site();
        site.timeZoneAbbr = "UTC";
        site.addName(new SiteName(site, "CWMS", "TESTSITE"));
        PlatformConfig config = new PlatformConfig("TESTSITE_FORMATTER_CONFIG");
        ConfigSensor sensor = new ConfigSensor(config, 1);
        sensor.sensorName = "HG";
        sensor.addDataType(new DataType("cbtt", "HG"));
        config.addSensor(sensor);
        DecodesScript script = DecodesScript.empty()
                .scriptName("HG")
                .platformConfig(config)
                .addDefaultSensors().build();
        config.addScript(script);

        Platform platform = new Platform();
        platform.setSite(site);

        TransportMedium tm = new TransportMedium(platform, "goes", "TESTSITE");
        tm.scriptName = "HG";
        tm.setDecodesScript(script);
        platform.transportMedia.add(tm);
        platform.setConfig(config);
        RawMessage rawMessage = new RawMessage("TESTSITE 5.0".getBytes());


        rawMessage.setTransportMedium(platform.getTransportMedium("goes"));
        rawMessage.setPlatform(platform);
        rawMessage.setMediumId("TESTSITE");
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(2026, 6, 21, 0, 0, 0);
        rawMessage.setTimeStamp(calendar.getTime());

        HydrometDMS3Formatter formatter = new HydrometDMS3Formatter();
        StringConsumer consumer = new StringConsumer();

        DecodedMessage message = new DecodedMessage(rawMessage);
        message.addSample(1, new Variable(5.0), 0);

        formatter.formatMessage(message, consumer);
        final String expected = String.format(
            "yyyyMMMdd hhmm cbtt     PC        NewValue   OldValue   Flag user:%s # DECODES output%n" + //
            "2026JUL21 0000 TESTSITE HG        5.00       998877.00  -01%n", System.getProperty("user.name"));
        assertEquals(expected, consumer.getOutput());
    }
}
