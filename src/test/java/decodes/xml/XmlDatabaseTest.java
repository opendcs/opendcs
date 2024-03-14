package decodes.xml;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.DecodesScript;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.StreamDecodesScriptReader;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XmlDatabaseTest {

    /**
     * This test checks reading and writing decodes scripts to a new empty xml decodes database
     * @throws Exception
     */
    @Test
    public void testReadingDecodesScripts() throws Exception
    {
        Path xmlDir = Files.createTempDirectory("xml-test-dir-{01}");
        try {
            System.out.println(xmlDir.toString());
            String configName = "config-1";
            DecodesSettings settings = new DecodesSettings();
            settings.editDatabaseTypeCode = DecodesSettings.DB_XML;
            settings.editDatabaseLocation = xmlDir.toString();
            XmlDatabaseIO dbio = (XmlDatabaseIO) DatabaseIO.makeDatabaseIO(settings);

            Database db = new Database();
            db.setDbIo(dbio);
            Platform platform = createPlatform();

            PlatformConfig pc = new PlatformConfig(configName);
            pc.setDatabase(db);
            db.platformConfigList.add(pc);
            platform.setConfig(pc);

            addConfigSensor(pc,1,"GageHeight","SHEF-PE","GH");
            addConfigSensor(pc,2,"WaterTemp","SHEF-PE","WF");

            DecodesScript ds1 =addScript(pc,"Script1","label1: 3(/, F(D,A,10,4), x, F(T,A,8), csv(1, 2, 4, 5, 6, 3) )",true,"Decodes:goes-self-timed");
            addScriptSensor(ds1,1,"feet");

            DecodesScript ds2 = addScript(pc,"Script2","label2: 3(/, F(D,A,10,4), x, F(T,A,8), csv(1, 2, 4, 5, 6, 3) )",false,"Decodes:goes-self-timed");
            addScriptSensor(ds2,2,"degF");

            if( 2 != pc.getNumScripts())
                throw new Exception("Expected 2 scripts (before save).  Found "+pc.getNumScripts());
            db.write();

            // read from disk
            db = new Database();
            dbio = (XmlDatabaseIO) DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XML, xmlDir.toString());
            db.setDbIo(dbio);
            db.read();

            PlatformConfig cfg = db.platformConfigList.get(configName);
            if( 2 != cfg.getNumScripts())
                throw new Exception("Expected 2 scripts (after reading from disk).  Found "+cfg.getNumScripts());
        }
        finally {
            deleteDirectory(xmlDir.toFile());
        }
    }

    private static Platform createPlatform() throws Exception{
        Platform platform = new Platform();
        platform.setId(DbKey.createDbKey(123456));
        platform.isProduction = true;
        platform.agency="CMWS";
        platform.setTimeLastRead();
        Site site = new Site();
        site.latitude="0";
        site.longitude="0";
        site.timeZoneAbbr="EST5EDT";
        site.country = "Australia";
        site.setDescription("Murray River - Australia");
        site.setElevation(9.124);
        site.setProperty("PUBLIC_NAME","TESTSITE1");
       // site.setDatabase(db);
        //SiteName sn = new SiteName(site,"CWMS","name");
        //site.addName(sn);
        platform.setSite(site);
        return platform;
        /*
        <Platform PlatformId="190166">
    <Agency>CWMS</Agency>
    <IsProduction>false</IsProduction>
    <LastModifyTime>01/26/2014 16:19:43</LastModifyTime>
    <Site>
      <Latitude>0</Latitude>
      <Longitude>0</Longitude>
      <Elevation>1.7976931348623157E308</Elevation>
      <ElevationUnits>m</ElevationUnits>
      <Timezone>EST5EDT</Timezone>
      <Country>UNITED STATES</Country>
      <Description>Site used for Regression Testing</Description>
      <SiteName NameType="CWMS">TESTSITE1</SiteName>
      <SiteProperty PropertyName="PUBLIC_NAME">TESTSITE1</SiteProperty>
    </Site>
    <PlatformConfig ConfigName="Dummy-Config">
      <Description>
        Dummy config to allow import platform records in CWMS.
      </Description>
    </PlatformConfig>
  </Platform>
         */
    }
    private static void addConfigSensor(PlatformConfig pc, int sensorNumber, String sensorName, String dataTypeStandard, String dataTypeCode){
        ConfigSensor configSensor = new ConfigSensor(pc, sensorNumber);
        configSensor.sensorName = sensorName;
        configSensor.addDataType(new DataType(dataTypeStandard,dataTypeCode));
        pc.addSensor(configSensor);
    }
    private static void addScriptSensor(DecodesScript decodesScript, int sensorNumber ,String units){

        ScriptSensor sensor = new ScriptSensor(decodesScript, sensorNumber);
        sensor.rawConverter = new UnitConverterDb("raw",units);
        sensor.rawConverter.coefficients= new double[]{1.0, 0.0, Constants.undefinedDouble,Constants.undefinedDouble,Constants.undefinedDouble,Constants.undefinedDouble};
        sensor.rawConverter.algorithm = Constants.eucvt_linear;
        decodesScript.scriptSensors.add(sensor);


    }
    private static DecodesScript addScript(PlatformConfig pc,String scriptName, String scriptContent,boolean ascendingOrder, String scriptType) throws Exception {
        byte[] bytes = scriptContent.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(bytes);
        StreamDecodesScriptReader scriptStream = new StreamDecodesScriptReader(inputStream);
        DecodesScript ds = DecodesScript.from(scriptStream).platformConfig(pc).scriptName(scriptName).build();
        ds.setDataOrder(ascendingOrder ? 'A' : 'D');
        ds.scriptType=scriptType;
        pc.decodesScripts.add(ds);
        return ds;
    }
    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}