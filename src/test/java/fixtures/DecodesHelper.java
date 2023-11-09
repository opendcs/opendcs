package fixtures;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import decodes.datasource.ShefPMParser;
import org.junit.jupiter.params.provider.Arguments;
import org.opendcs.utils.ClasspathIO;

import decodes.datasource.EdlPMParser;
import decodes.datasource.GoesPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.IridiumPMParser;
import decodes.datasource.PMParser;
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
import decodes.decoder.DecodedSample;
import decodes.decoder.DecoderException;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import ilex.var.VariableType;

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
     * @throws NoConversionException
     */
    public static Arguments getScript(String testName, String resourcePath) throws DecodesScriptException, IOException,
                                                              URISyntaxException, HeaderParseException,
                                                              UnknownPlatformException, IncompleteDatabaseException,
                                                              InvalidDatabaseException, DecoderException, NoConversionException
    {
        ArrayList<DecodesAssertion> assertions = new ArrayList<>();
        DecodesScript.trackDecoding = true;
        PlatformConfig platformConfig = new PlatformConfig();
        String path = "/" + resourcePath + "/";
        URL script = DecodesScript.class.getResource(path + testName + ".decodescript");
        URL input = getInputResource(path, testName);
        DecodesScript decodesScript = DecodesScript.from(new StreamDecodesScriptReader(script.openStream()))
                                     .platformConfig(platformConfig)
                                     .scriptName(testName)
                                     .build();

        URL sensors = DecodesScript.class.getResource(path + testName + ".sensors");
        URL assertionsURL = DecodesScript.class.getResource(path + testName + ".assertions");

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
                ConfigSensor configSensor = new ConfigSensor(decodesScript.platformConfig, Integer.parseInt(parts[0]));
                configSensor.sensorName = parts[1];
                decodesScript.platformConfig.addSensor(configSensor);
            }
        }

        String assertionLine = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assertionsURL.openStream())))
        {
            while((assertionLine = reader.readLine() ) != null)
            {
                if( assertionLine.trim().startsWith("#"))
                {
                    continue;
                }
                String parts[] = assertionLine.trim().split(",");
                Variable v = null;
                try
                {
                    v = new Variable(Double.parseDouble(parts[2].trim()));
                }
                catch(NumberFormatException ex)
                {
                    /* String was provided, use as-is. */
                    v = new Variable(parts[2].trim());
                }
                DecodesAssertion a = new DecodesAssertion(
                    Integer.parseInt(parts[0].trim()),
                    ZonedDateTime.parse(parts[1].trim()),
                    v,
                    Double.parseDouble(parts[3].trim()),
                    parts[4].trim()
                );
                assertions.add(a);
            }
        }
        
        RawMessage rawMessage = prepareRawMessage(input, decodesScript);
        DecodedMessage decodedMessage = decodesScript.decodeMessage(rawMessage);
        return arguments(testName,decodesScript,rawMessage,decodedMessage,assertions);
    }

    private static URL getInputResource(final String path, final String testName) throws FileNotFoundException
    {
        String[] extensions = {".input_shef", ".input_iridium", ".input_goes", ".input_edl", ".input_data-logger", ".input"};
        for( String ext: extensions)
        {
            URL input = DecodesScript.class.getResource(path + testName + ext);
            if( input != null )
            {
                return input;
            }
        }
        throw new FileNotFoundException(
            String.format("Could not find test input file for %s. It should be present in '%s' and have one of the following extensions: %s", 
            testName, path, String.join(", ", extensions)));
    }

    private static String getMediumFromResource(final URL input)
    {
        String inputStr = input.toString();
        if( inputStr.contains("input_") )
        {
            String[] parts = inputStr.split("_");
            String part = parts[parts.length - 1];

            if( part.equalsIgnoreCase(Constants.medium_Goes) )
            {
                return Constants.medium_Goes;
            }
            else if( part.equalsIgnoreCase(Constants.medium_IRIDIUM) )
            {
                return Constants.medium_IRIDIUM;
            }
            else if( part.equalsIgnoreCase(Constants.medium_EDL) )
            {
                return Constants.medium_EDL;
            }
            else if( part.equalsIgnoreCase("edl") )
            {
                return Constants.medium_EDL;
            }
            else if( part.equalsIgnoreCase("shef"))
            {
                return Constants.medium_SHEF;
            }
        }
        // If there was no medium detected, assume EDL.
        return Constants.medium_EDL;
    }

    private static RawMessage prepareRawMessage(final URL input, DecodesScript decodesScript) throws IOException, URISyntaxException,
                                                                                        HeaderParseException, IncompleteDatabaseException,
                                                                                        InvalidDatabaseException, NoConversionException
    {
        String mediumType = getMediumFromResource(input);
        Platform tmpPlatform = new Platform();
        tmpPlatform.setSite(new Site());
        tmpPlatform.getSite().addName(new SiteName(tmpPlatform.getSite(), "USGS", "dummy"));
        tmpPlatform.setConfig(decodesScript.platformConfig);
        tmpPlatform.setConfigName(decodesScript.platformConfig.configName);
        TransportMedium tmpMedium = new TransportMedium(tmpPlatform, mediumType, "11111111");
        tmpMedium.scriptName = decodesScript.scriptName;
        tmpMedium.setDecodesScript(decodesScript);
        tmpPlatform.transportMedia.add(tmpMedium);

        String sampleMessage =
            String.join("\n", Files.readAllLines(Paths.get(input.toURI())));
        int len = sampleMessage.length();
        RawMessage rawMessage = new RawMessage(sampleMessage.getBytes(), len);
        rawMessage.setPlatform(tmpPlatform);
        rawMessage.setTransportMedium(tmpMedium);
        String tz = "UTC";
        tmpMedium.setTimeZone(tz);

        tmpMedium.setMediumType(mediumType);
        rawMessage.setMediumId("11111111");

        PMParser parser = null;
        if( mediumType.equals(Constants.medium_Goes) )
        {
            parser = new GoesPMParser();
        }
        else if( mediumType.equals(Constants.medium_IRIDIUM) )
        {
            parser = new IridiumPMParser();
        }
        else if( mediumType.equals(Constants.medium_SHEF))
        {
            parser = new ShefPMParser();
        }
        else
        {
            parser = new EdlPMParser();
        }

        parser.parsePerformanceMeasurements(rawMessage);

        // If the message contained a parseable timestamp (e.g. Iridium), use that as the raw timestamp
        Variable ts = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
        if( ts != null )
        {
            rawMessage.setTimeStamp(ts.getDateValue());
        }
        else
        {
            rawMessage.setTimeStamp(new Date());
        }

        decodesScript.prepareForExec();
        tmpMedium.prepareForExec();

        return rawMessage;
    }

    public static Stream<Arguments> decodesTestSets(final String path) throws Exception
    {
        List<URL> scripts = ClasspathIO.getAllResourcesIn(path);
        return scripts.stream()
            .filter( u-> u.toString().endsWith(".decodescript"))
            .map(u-> {
                    String fileName = u.toString();
                    int lastSlash = fileName.lastIndexOf("/");
                    String testName = fileName.substring(lastSlash+1)
                                                                  .replace(".decodescript","");
                    return testName;
            })
            .map(name -> {
                try
                {
                    return getScript(name, path);
                }
                catch(Exception ex)
                {
                    throw new RuntimeException("unable to load test information for: " +name+"  , path="+path, ex);
                }
            });
    }

    public static Stream<Arguments> decodesTestSets() throws Exception
    {
        return decodesTestSets("decodes/db");
    }

    public static DecodedSample sampleFor(int sensor, ZonedDateTime sampleTime,
                                          ArrayList<DecodedSample> samples) throws NoSuchElementException
    {
        for(DecodedSample s: samples)
        {
            TimedVariable tv = s.getSample();
            if( s.getTimeSeries().getSensorId() == sensor 
                && tv.getTime().getTime() == sampleTime.toInstant().toEpochMilli()
            )
            {
                return s;
            }
        }

        throw new NoSuchElementException("Expected Sample for sensor " 
                                     + sensor + " at time "
                                     + sampleTime + " doesn't exist");
    }

    public static class DecodesAssertion
    {
        //sensor number, time ISO UTC, expected value (double or string), precision, message
        private int sensor;
        private ZonedDateTime time;
        private Variable expectedValue;
        private double precision;
        private String message;

        private DecodesAssertion(int sensor,ZonedDateTime time, Variable expectedValue,
                              double precision,String message)
        {
            Objects.requireNonNull(message, "A message must be provided");
            Objects.requireNonNull(expectedValue, "An initialized expected value must be provided.");
            if( message.isEmpty())
            {
                throw new RuntimeException("Message should not be empty");
            }
            if( expectedValue.getStringValue().isEmpty())
            {
                throw new RuntimeException("A non-empty expected value must be provided.");
            }
            this.sensor = sensor;
            this.time = time;
            this.expectedValue = expectedValue;
            this.precision = precision;
            this.message = message;
        }

        public int getSensor()
        {
            return sensor;
        }

        public ZonedDateTime getTime()
        {
            return time;
        }

        public double getPrecision()
        {
            return precision;
        }

        public String getMessage()
        {
            return message;
        }

        public Variable getExpectedValue()
        {
            return expectedValue;
        }
    }
}
