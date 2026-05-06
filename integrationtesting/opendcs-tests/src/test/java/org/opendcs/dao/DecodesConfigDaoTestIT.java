package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;

@EnableIfTsDb
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"
})
class DecodesConfigDaoTestIT extends AppTestBase
{

    private static final String[] SENSORS = new String[]{"Stage", "Elev", "Flow"};
    private static final String[] UNTIS = new String[]{"in", "ft", "cfs"};

    @ConfiguredField
    OpenDcsDatabase db;

    // this random sources needs to be repeatable, hence static seed.
    private static final Random RANDOM = new Random(30); // NOSONAR


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

        var dataTypeDao = db.getDao(DataTypeDao.class)
                            .orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var pcIn = new PlatformConfig("TestConfig-1");

            var sensor1 = new ConfigSensor(pcIn, 1);
            sensor1.sensorName = "Stage";
            sensor1.recordingInterval = 0;
            sensor1.recordingMode = Constants.recordingModeFixed;
            sensor1.setProperty("cwmsInterval", "15Minutes");
            sensor1.addDataType(dataTypeDao.lookup(tx, "CWMS", "Stage").orElseThrow());
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

    @Test
    void test_pagination() throws Exception
    {
        var decodesConfigDao = db.getDao(DecodesConfigDao.class)
                                 .orElseThrow();

        var dataTypeDao = db.getDao(DataTypeDao.class)
                            .orElseThrow();

        try (var tx = db.newTransaction())
        {
            var all = decodesConfigDao.getAll(tx, -1, -1);
            assertFalse(all.isEmpty());

            var onlyOne = decodesConfigDao.getAll(tx, 1, 1);
            assertEquals(1, onlyOne.size());
            assertEquals("OKVI4", onlyOne.getFirst().getName());
            assertEquals("ST", onlyOne.getFirst().decodesScripts.getFirst().scriptName);

            final var numConfigs = 30;

            final var savedConfigs = new ArrayList<PlatformConfig>();

            for (var i = 0; i < numConfigs; i++)
            {
                final var name = String.format("0000-Config-%02d", i);

                var pc = createConfig(name, RANDOM.nextInt(3), RANDOM.nextInt(2), RANDOM.nextInt(3), (i % 7) == 0, tx, dataTypeDao);
                var pcOut = decodesConfigDao.save(tx, pc);
                savedConfigs.add(pcOut);
            }

            final var allSaved = decodesConfigDao.getAll(tx, numConfigs, 0);
            assertEquals(numConfigs, allSaved.size());


            final var first10 = decodesConfigDao.getAll(tx, 10, 0);
            assertEquals(savedConfigs.getFirst().configName, first10.getFirst().configName);
            assertEquals(savedConfigs.get(9).configName, first10.getLast().configName);

            final var second10 = decodesConfigDao.getAll(tx, 10, 10);
            assertEquals(savedConfigs.get(10).configName, second10.getFirst().configName);
            assertEquals(savedConfigs.get(19).configName, second10.getLast().configName);

            tx.rollback();
        }
    }


    private PlatformConfig createConfig(String name, int numScripts, int numStatements, int numSensors, boolean sensorProperties, DataTransaction tx, DataTypeDao dataTypeDao) throws Exception
    {
        

        var pc = new PlatformConfig(name);

        for (var i = 0; i < numSensors; i++)
        {
            var sensor = new ConfigSensor(pc, i + 1);
            sensor.sensorName = SENSORS[RANDOM.nextInt(SENSORS.length)];
            sensor.recordingInterval = 900;
            sensor.recordingMode = 'F';
            sensor.timeOfFirstSample = 0;
            sensor.addDataType(dataTypeDao.lookup(tx, "CWMS", sensor.sensorName)
                                          .orElseGet(() -> fail("DataType for " + sensor.sensorName + " doesn't exist.")));
            if (sensorProperties)
            {
                sensor.setProperty("CwmsInterval", "15Minutes");
            }
        }

        for (var i = 0; i < numScripts; i++)
        {
            var script = DecodesScript.empty()
                                      .platformConfig(pc)
                                      .scriptName(String.format("S-%d", i))
                                      .withDataOrder('A')
                                      .build();
            for (var j = 0; j < numStatements; j++)
            {
                var fs = new FormatStatement(script, j + 1);
                fs.label = "STMT" + j;
                fs.format = String.format("/%dx", j);
                script.getFormatStatements().add(fs);
            }

            for (var k = 0; k < numSensors; k++)
            {
                var scriptSensor = new ScriptSensor(script, k + 1);

                final var sensorName = pc.getSensor(k + 1);
                int idx = 0;
                for (var sensorIdx = 0; sensorIdx < SENSORS.length; sensorIdx++)
                {
                    if (SENSORS[sensorIdx].equals(sensorName))
                    {
                        idx = sensorIdx;
                        break;
                    }
                }
                var toUnits = UNTIS[idx];

                scriptSensor.rawConverter = new UnitConverterDb("raw", toUnits);
                scriptSensor.rawConverter.algorithm = Constants.eucvt_none;

                script.addScriptSensor(scriptSensor);
            }



            pc.addScript(script);
        }

        return pc;
    }
}
