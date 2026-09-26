package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle", "CWMS-Oracle"})
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
            assertNotNull(platforms.get(0).getSite());
            assertTrue(platforms.get(0).platformSensors.isEmpty());
        }
    }

    /**
     * The site names joined onto each platform used to come back in whatever order the
     * database happened to produce. Site#getPreferredName() - and the web UI - fall back to
     * the <em>first</em> name when the preferred type isn't defined, so an unordered join made
     * the displayed platform name flip between name types from one page load to the next
     * (issue #2052). Verify the order is stable and preference-first instead.
     */
    @Test
    void test_site_names_ordered_consistently() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var first = namesOfPlatformsWithSites(dao.getAll(tx, -1, -1, false));
            var second = namesOfPlatformsWithSites(dao.getAll(tx, -1, -1, false));
            assertFalse(first.isEmpty(), "No platform with a site was returned.");
            assertEquals(first, second, "Site name order changed between identical queries.");

            var preferred = DecodesSettings.instance().siteNameTypePreference;
            first.forEach((platformId, nameTypes) ->
                    assertTrue(!nameTypes.contains(preferred) || preferred.equalsIgnoreCase(nameTypes.get(0)),
                            () -> String.format("Platform %s has a %s name but it wasn't first: %s",
                                    platformId, preferred, nameTypes)));
        }
    }

    /** Platform id -> the site's name types, in the order the query returned them. */
    private static Map<DbKey, List<String>> namesOfPlatformsWithSites(List<Platform> platforms)
    {
        Map<DbKey, List<String>> byPlatform = new LinkedHashMap<>();
        for (Platform p : platforms)
        {
            if (p.getSite() == null)
            {
                continue;
            }
            List<String> nameTypes = new ArrayList<>();
            for (Iterator<SiteName> it = p.getSite().getNames(); it.hasNext(); )
            {
                nameTypes.add(it.next().getNameType());
            }
            if (!nameTypes.isEmpty())
            {
                byPlatform.put(p.getId(), nameTypes);
            }
        }
        return byPlatform;
    }
}
