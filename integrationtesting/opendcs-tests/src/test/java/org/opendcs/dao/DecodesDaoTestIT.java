package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptReader;
import decodes.db.FormatStatement;
import decodes.db.PlatformConfig;

@EnableIfTsDb
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"
})
class DecodesDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;



    @Test
    void test_existing_config() throws Exception
    {
        var decodesConfigDao = db.getDao(DecodesConfigDao.class)
                                 .orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var config = decodesConfigDao.getByName(tx, "OKVI4")
                                               .orElseGet(() -> fail("Config not retrieved."));

            assertEquals("OKVI4", config.configName);
            assertFalse(config.decodesScripts.isEmpty());
            final var script = config.getScript("ST");
            assertNotNull(script);
            assertEquals("Decodes", script.scriptType);
            assertEquals(config, script.platformConfig);
            assertEquals(1, script.getFormatStatements().size());
            assertEquals("4x,4(f(s,b,3,1),f(s,b,3,2)),24x,f(s,b,1,3)", script.getFormatStatement("st").format);

            var scriptSensor1 = script.getScriptSensor(1);
            assertNotNull(scriptSensor1);
            assertNotNull(scriptSensor1.rawConverter);

            assertEquals("raw", scriptSensor1.rawConverter.fromAbbr);
            assertEquals("ft", scriptSensor1.rawConverter.toAbbr);
            assertEquals("usgs-standard", scriptSensor1.rawConverter.algorithm);
            assertArrayEquals(new double[]{0.01, 0, 1.0, 0.0, 0.0, 0.0}, scriptSensor1.rawConverter.coefficients);
        }
    }


    @Test
    void test_basic_operations() throws Exception
    {
        var decodesConfigDao = db.getDao(DecodesConfigDao.class)
                                 .orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var pcIn = new PlatformConfig("TestConfig-1");

            var sensor1 = new ConfigSensor(pcIn, 1);
            sensor1.sensorName = "Stage";
            sensor1.recordingInterval = 0;
            sensor1.recordingMode = Constants.recordingModeFixed;
            sensor1.setProperty("cwmsInterval", "15Minutes");
            sensor1.addDataType(DataType.getDataType("CWMS", "Stage"));
            pcIn.addSensor(sensor1);
            final var script = DecodesScript.empty()
                                            .platformConfig(pcIn)
                                            .addDefaultSensors()
                                            .scriptName("ST")
                                            .build();
            var fs = new FormatStatement(script, 1);
            fs.label = "ST";
            fs.format = "/,1f(s,a,5D' ', 1";
            script.getFormatStatements().add(fs);

            pcIn.description = "A simple test configuration.";
            final var pcOut = decodesConfigDao.save(tx, pcIn);
            assertEquals("TestConfig-1", pcOut.getName());


            decodesConfigDao.delete(tx, pcOut.getId());

            final var pcNotExist = decodesConfigDao.getByName(tx, "TestConfig-1");
            assertTrue(pcNotExist.isEmpty());
        }
    }
}
