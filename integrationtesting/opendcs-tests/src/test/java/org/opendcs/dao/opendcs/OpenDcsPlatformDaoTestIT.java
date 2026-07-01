package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DecodesConfigDao;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.Platform;
import decodes.db.PlatformSensor;
import decodes.db.SiteName;
import decodes.db.TransportMedium;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"

})
class OpenDcsPlatformDaoTestIT extends AppTestBase
{

    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_can_read_existing_platform() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var platform = dao.getByMediumId(tx, "goes-self-timed", "CE344292")
                              .orElseGet(() -> fail("Could not retrieve platform."));

            assertFalse(platform.transportMedia.isEmpty());
            assertNotNull(platform.getSite());
            assertFalse(platform.getSite().getNameArray().isEmpty());
            assertEquals("I'm here", platform.getProperty("SystemCheck"));


            assertNotNull(platform.getConfig());

            dao.delete(tx, platform.getId());

            var shouldFail = dao.getById(tx, platform.getId());
            assertFalse(shouldFail.isPresent());

            tx.rollback();
        }
    }

    @Test
    void test_create_update_platform() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();
        var configDao = db.getDao(DecodesConfigDao.class).orElseThrow();
        var siteDao = db.getDao(SiteDao.class).orElseThrow();

        Platform platformIn = new Platform();
        platformIn.agency = "OpenDcs Testing";
        platformIn.isProduction = true;
        platformIn.description = "A Test Platform";
        platformIn.setPlatformDesignator("a");
        TransportMedium tm = new TransportMedium(platformIn);
        tm.channelNum = 1;
        tm.assignedTime = 5;
        tm.scriptName = "TEST";
        tm.setMediumType("logger");
        tm.setMediumId("TestPlatform");

        platformIn.transportMedia.add(tm);

        try (var tx = db.newTransaction())
        {
            var site = siteDao.getBySiteName(tx, new SiteName(null, "CWMS", "OKVI4")).orElseThrow();
            platformIn.setSite(site);

            var platformOut = dao.save(tx, platformIn);
            assertFalse(platformOut.transportMedia.isEmpty());
            assertNotNull(platformOut.getSite());
            assertFalse(platformOut.getSite().getNameArray().isEmpty());

            var config = configDao.getByName(tx, "OKVI4").orElseThrow(); // PlatformDao doesn't save the config
            platformOut.setConfig(config);

            var ps1 = new PlatformSensor(platformOut, 1);
            ps1.setProperty("cwmsVersion", "atest");

            platformOut.platformSensors.add(ps1);

            var platformOut2 = dao.save(tx, platformOut);

            assertNotNull(platformOut2.getConfig());

            assertFalse(platformOut2.platformSensors.isEmpty());
            assertEquals("atest", platformOut2.platformSensors.get(0).getProperty("cwmsVersion"));

            dao.delete(tx, platformOut2.getId());

            var shouldFail = dao.getById(tx, platformOut.getId());
            assertFalse(shouldFail.isPresent());
        }
    }

    @Test
    void test_get_all_partial_data() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var platforms = dao.getAll(tx, -1, -1, false);

            assertFalse(platforms.isEmpty());
            assertNull(platforms.get(0).getSite());
        }
    }
}
