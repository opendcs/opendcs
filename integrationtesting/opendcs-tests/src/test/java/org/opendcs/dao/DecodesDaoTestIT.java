package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

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
    void test_basic_operations() throws Exception
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
}
