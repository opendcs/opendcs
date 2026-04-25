package fixtures;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import decodes.consumer.OutputFormatter;
import decodes.datasource.ShefPMParser;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.decoder.TimeSeries;
import org.junit.jupiter.params.provider.Arguments;
import org.opendcs.utils.ClasspathIO;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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

/**
 * Functions to help make decodes tests easier
 * @since 2022-11-10
 */
public class DecodesHelper {
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

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
        Artifacts a = loadArtifacts(testName, resourcePath);
        ArrayList<DecodesAssertion> assertions = loadAssertions(requireResource(resourcePath, testName, ".assertions"));
        return arguments(testName, a.script, a.rawMessage, a.decodedMessage, assertions);
    }

    private static ArrayList<DecodesAssertion> loadAssertions(URL assertionsURL) throws IOException
    {
        ArrayList<DecodesAssertion> assertions = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(assertionsURL.openStream())))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#"))
                {
                    continue;
                }
                assertions.add(parseAssertion(line));
            }
        }
        return assertions;
    }

    private static DecodesAssertion parseAssertion(String line)
    {
        String[] parts = line.trim().split(",");
        Variable v;
        try
        {
            v = new Variable(Double.parseDouble(parts[2].trim()));
        }
        catch (NumberFormatException ex)
        {
            /* String was provided, use as-is. */
            v = new Variable(parts[2].trim());
        }
        return new DecodesAssertion(
            Integer.parseInt(parts[0].trim()),
            ZonedDateTime.parse(parts[1].trim()),
            v,
            Double.parseDouble(parts[3].trim()),
            parts[4].trim()
        );
    }

    /**
     * Load the shared decoding artifacts for a test set: .decodescript, .sensors, .input_*.
     * Prepares the RawMessage and invokes DecodesScript.decodeMessage.
     *
     * Used by both the assertion-based test pattern (.assertions) and the
     * formatter-based test pattern (.formatter + .expected).
     */
    private static Artifacts loadArtifacts(String testName, String resourcePath) throws DecodesScriptException, IOException,
                                                              URISyntaxException, HeaderParseException,
                                                              UnknownPlatformException, IncompleteDatabaseException,
                                                              InvalidDatabaseException, DecoderException, NoConversionException
    {
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

        int sensorIndex = 0;
        String sensorLine = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sensors.openStream())))
        {
            while ((sensorLine = reader.readLine()) != null)
            {
                //sensor number, sensor name, units, description, type:code, alogrithm, A:B:C:D:E:F, recording mode, interval
                String trimmed = sensorLine.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#"))
                {
                    continue;
                }
                sensorIndex++;
                String parts[] = sensorLine.split(",");
                ScriptSensor stage = new ScriptSensor(decodesScript, sensorIndex);

                stage.rawConverter = new UnitConverterDb("raw", parts[1]);
                stage.rawConverter.algorithm = lookupAlgo(parts);
                stage.rawConverter.coefficients = lookupCoefficients(parts);
                decodesScript.scriptSensors.add(stage);
                ConfigSensor configSensor = new ConfigSensor(decodesScript.platformConfig, Integer.parseInt(parts[0]));
                setRecordingModeAndInterval(configSensor, parts);
                configSensor.sensorName = parts[1];

                if( parts.length >= 5)
                {
                    //1, Stage, ft, none,SHEF-PE:HGIFF;
                    String[] tokens = parts[4].split(":");
                    if(tokens.length==2) {
                        //configSensor.addDataType(new DataType("SHEF-PE", "HGIFF"));
                        configSensor.addDataType(new DataType(tokens[0], tokens[1]));
                    }
                }
                decodesScript.platformConfig.addSensor(configSensor);
            }
        }

        RawMessage rawMessage = prepareRawMessage(input, decodesScript);
        DecodedMessage decodedMessage = decodesScript.decodeMessage(rawMessage);
        return new Artifacts(decodesScript, rawMessage, decodedMessage);
    }

    /**
     * Build a test-case for the formatter-based pattern. Expects the following
     * files:
     *   - {testName}.decodescript
     *   - {testName}.sensors
     *   - {testName}.input_*
     *   - {testName}.expected    -- golden text to compare whole-output against.
     *   - {testName}.formatter   -- java.util.Properties file. Example:
     * <pre>
     * class=decodes.consumer.EmitAsciiFormatter
     * timezone=UTC
     * # any additional keys are passed to the formatter's initFormatter
     * delimiter=,
     * </pre>
     */
    public static Arguments getScriptWithFormatter(String testName, String resourcePath) throws Exception
    {
        Artifacts a = loadArtifacts(testName, resourcePath);
        FormatterSpec spec = loadFormatterSpec(requireResource(resourcePath, testName, ".formatter"), testName);
        String expected = loadExpectedText(requireResource(resourcePath, testName, ".expected"));
        return arguments(testName, a.script, a.rawMessage, a.decodedMessage,
                         spec.className, spec.rsProps, spec.timezone, expected);
    }

    private static FormatterSpec loadFormatterSpec(URL formatterUrl, String testName) throws IOException
    {
        Properties fileProps = new Properties();
        try (InputStreamReader reader = new InputStreamReader(formatterUrl.openStream()))
        {
            fileProps.load(reader);
        }
        String className = fileProps.getProperty("class");
        if (className == null || className.trim().isEmpty())
        {
            throw new IllegalStateException("'" + testName + ".formatter' must define 'class'");
        }
        TimeZone tz = TimeZone.getTimeZone(fileProps.getProperty("timezone", "UTC"));
        Properties rsProps = new Properties();
        for (String key : fileProps.stringPropertyNames())
        {
            if (!"class".equals(key) && !"timezone".equals(key))
            {
                rsProps.setProperty(key, fileProps.getProperty(key));
            }
        }
        return new FormatterSpec(className, rsProps, tz);
    }

    private static String loadExpectedText(URL expectedUrl) throws IOException, URISyntaxException
    {
        String text = String.join("\n", Files.readAllLines(Paths.get(expectedUrl.toURI())));
        // Files.readAllLines drops the trailing newline; put it back if the file ended with one.
        byte[] raw = Files.readAllBytes(Paths.get(expectedUrl.toURI()));
        if (raw.length > 0 && raw[raw.length - 1] == '\n')
        {
            text = text + "\n";
        }
        return text;
    }

    private static URL requireResource(String resourcePath, String testName, String suffix) throws FileNotFoundException
    {
        String path = "/" + resourcePath + "/";
        URL url = DecodesScript.class.getResource(path + testName + suffix);
        if (url == null)
        {
            throw new FileNotFoundException("Could not find '" + testName + suffix + "' in '" + path + "'");
        }
        return url;
    }

    /**
     * Instantiate and run an OutputFormatter against a DecodedMessage, returning the
     * full text the formatter wrote to its DataConsumer.
     *
     * Uses reflection to invoke OutputFormatter.initFormatter (protected) so tests do
     * not need to live in the decodes.consumer package, and to avoid the
     * DbEnum("OutputFormat") lookup that OutputFormatter.makeOutputFormatter requires.
     */
    public static String formatDecodedMessage(DecodedMessage msg, String formatterClass,
                                              Properties rsProps, TimeZone tz) throws Exception
    {
        OutputFormatter formatter = (OutputFormatter) Class.forName(formatterClass)
                                                           .getDeclaredConstructor()
                                                           .newInstance();
        Method init = OutputFormatter.class.getDeclaredMethod(
            "initFormatter", String.class, TimeZone.class, PresentationGroup.class, Properties.class);
        init.setAccessible(true);
        init.invoke(formatter, formatterClass, tz, null, rsProps);

        StringConsumer consumer = new StringConsumer();
        consumer.setTimeZone(tz);
        try
        {
            formatter.formatMessage(msg, consumer);
        }
        finally
        {
            formatter.shutdown();
        }
        return consumer.getOutput();
    }

    /** Holder for the shared decoding artifacts produced by loadArtifacts. */
    private static final class Artifacts
    {
        final DecodesScript script;
        final RawMessage rawMessage;
        final DecodedMessage decodedMessage;

        Artifacts(DecodesScript script, RawMessage rawMessage, DecodedMessage decodedMessage)
        {
            this.script = script;
            this.rawMessage = rawMessage;
            this.decodedMessage = decodedMessage;
        }
    }

    /** Holder for the parsed contents of a .formatter file. */
    private static final class FormatterSpec
    {
        final String className;
        final Properties rsProps;
        final TimeZone timezone;

        FormatterSpec(String className, Properties rsProps, TimeZone timezone)
        {
            this.className = className;
            this.rsProps = rsProps;
            this.timezone = timezone;
        }
    }

    private static void setRecordingModeAndInterval(ConfigSensor configSensor, String[] parts)
    {
        if (parts.length >=8) 
        {
            configSensor.recordingMode = parts[7].trim().charAt(0);
        }
        if (parts.length >=9)
        {
            String intervalStr = parts[8].trim();
            try 
            {
                int interval = Integer.parseInt(intervalStr);
                configSensor.recordingInterval = interval;
            } 
            catch (NumberFormatException ex) 
            {
                log.atWarn().setCause(ex).log("Cann't parse recording interval '{}' in .sensors file", intervalStr);
            }
        }
    }   

    private static String lookupAlgo(String[] parts)
    {
        if (parts.length >=6) 
        {
            return parts[5].trim(); 
        }
         else
        {
            return Constants.eucvt_none;
        }
    }

    private static double[] lookupCoefficients(String[] parts)
    {
        double[] coefs = new double[6];
        if (parts.length >=7) 
        {
            String[] tokens = parts[6].trim().split(":");
                for (int i = 0; i < Math.min(tokens.length, coefs.length); i++) 
                {
                    coefs[i] = Double.parseDouble(tokens[i].trim());
                }
        }
        return coefs;
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
        List<URL> resources = ClasspathIO.getAllResourcesIn(path);
        Set<String> fixturesWithAssertions = fixtureNamesWithSuffix(resources, ".assertions");
        return resources.stream()
            .filter( u-> u.toString().endsWith(".decodescript"))
            .map(DecodesHelper::testNameFromUrl)
            .filter(fixturesWithAssertions::contains)
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

    public static Stream<Arguments> decodesFormatterTestSets(final String path) throws Exception
    {
        List<URL> resources = ClasspathIO.getAllResourcesIn(path);
        Set<String> fixturesWithExpected = fixtureNamesWithSuffix(resources, ".expected");
        return resources.stream()
            .filter( u-> u.toString().endsWith(".decodescript"))
            .map(DecodesHelper::testNameFromUrl)
            .filter(fixturesWithExpected::contains)
            .map(name -> {
                try
                {
                    return getScriptWithFormatter(name, path);
                }
                catch(Exception ex)
                {
                    throw new RuntimeException("unable to load formatter test information for: " +name+"  , path="+path, ex);
                }
            });
    }

    public static Stream<Arguments> decodesFormatterTestSets() throws Exception
    {
        return decodesFormatterTestSets("decodes/db");
    }

    private static String testNameFromUrl(URL u)
    {
        String fileName = u.toString();
        int lastSlash = fileName.lastIndexOf("/");
        int lastDot = fileName.lastIndexOf('.');
        return fileName.substring(lastSlash + 1, lastDot);
    }

    private static Set<String> fixtureNamesWithSuffix(List<URL> resources, String suffix)
    {
        return resources.stream()
            .filter(u -> u.toString().endsWith(suffix))
            .map(u -> {
                String fileName = u.toString();
                int lastSlash = fileName.lastIndexOf("/");
                return fileName.substring(lastSlash + 1).replace(suffix, "");
            })
            .collect(Collectors.toCollection(HashSet::new));
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
        String info = samplesToString(sensor, samples);
        throw new NoSuchElementException("Expected Sample for sensor " 
                                     + sensor + " at time "
                                     + sampleTime + " doesn't exist\nDecoded "+samples.size()+" samples\n"+info);
    }

    private static String samplesToString(int sensor, ArrayList<DecodedSample> samples)
    {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        for(DecodedSample s: samples)
        {
            TimeSeries ts = s.getTimeSeries();
            if( ts.getSensorId() == sensor)
            {
                for (int i = 0; i <ts.size() ; i++)
                {
                    Date t =ts.timeAt(i);

                    sb.append(sdf.format(t) +" " +ts.formattedSampleAt(i)+"\n");
                    if( i>=30)
                        break;
                }
                break;
            }
        }
        return sb.toString();
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
