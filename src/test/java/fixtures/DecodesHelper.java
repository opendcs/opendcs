package fixtures;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import decodes.datasource.EdlPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptException;
import decodes.db.IncompleteDatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.StreamDecodesScriptReader;
import decodes.db.TransportMedium;
import decodes.db.UnitConverterDb;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;

/**
 * Functions to help make decodes tests easier
 * @since 2022-11-10
 */
public class DecodesHelper {

    /**
     * Given a base name read the appropriate files off the classpath.
     *
     * Given the name "WEB" the following will be searched for
     *
     * - WEB.decodesscript
     * - WEB.input
     * - WEB.sensor
     *
     * @param testName
     * @return an Arguments instance containing the script and prepared Raw message of the input, and the DecodedMessage
     * @throws DecodesScriptException Any errors reading the script in
     * @throws URISyntaxException
     * @throws HeaderParseException
     * @throws DecoderException
     * @throws InvalidDatabaseException
     * @throws IncompleteDatabaseException
     * @throws UnknownPlatformException
     */
    public static Arguments getScript(String testName) throws DecodesScriptException, IOException,
                                                              URISyntaxException, HeaderParseException,
                                                              UnknownPlatformException, IncompleteDatabaseException,
                                                              InvalidDatabaseException, DecoderException
    {
        DecodesScript.trackDecoding = true;
        PlatformConfig platformConfig = new PlatformConfig();
        URL script = DecodesScript.class.getResource("/decodes/db/"+ testName + ".decodescript");
        DecodesScript decodesScript = DecodesScript.from(new StreamDecodesScriptReader(script.openStream()))
                                     .platformConfig(platformConfig)
                                     .scriptName("WEB")
                                     .build();

        URL sensors = DecodesScript.class.getResource("/decodes/db/"+ testName + ".sensors");

        int sensorIndex = 0;
        String sensorLine = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sensors.openStream())))
        {
            while ((sensorLine = reader.readLine()) != null)
            {
                sensorIndex++;
                String parts[] = sensorLine.split(",");
                ScriptSensor stage = new ScriptSensor(decodesScript, sensorIndex);

                stage.rawConverter = new UnitConverterDb("raw", parts[1]);
                // TODO: lookup algo and parts
                stage.rawConverter.algorithm = Constants.eucvt_none;
                decodesScript.scriptSensors.add(stage);
                ConfigSensor configSensor = new ConfigSensor(decodesScript.platformConfig, 1);
                configSensor.sensorName = parts[0];
                decodesScript.platformConfig.addSensor(configSensor);
            }
        }

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
            String.join("\n", Files.readAllLines(Paths.get(DecodesHelper.class.getResource("/decodes/db/WEB.input").toURI())));
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
        DecodedMessage decodedMessage = decodesScript.decodeMessage(rawMessage);

        return arguments(decodesScript,rawMessage,decodedMessage);
    }


}
