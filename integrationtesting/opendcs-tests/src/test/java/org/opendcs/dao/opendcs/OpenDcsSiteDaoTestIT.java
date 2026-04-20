package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.exceptions.RequiredSiteNameMissingException;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.util.DecodesSettings;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle", "CWMS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml"
})
class OpenDcsSiteDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(SiteDao.class).orElseThrow();


        try (var tx = db.newTransaction())
        {
            var site = new Site();
            var siteName = new SiteName(site, "CWMS", "SimpleTestSite");
            site.addName(siteName);
            var siteName2 = new SiteName(site, "local", "Local Test Name");
            site.addName(siteName2);
            site.country = "US";            
            site.setDescription("A test site");
            site.setActive(true);
            site.nearestCity = "Bob's ville"; // we're specifically testing that ' here
            // modify time set by Dao
            site.setPublicName("A site that exists to test the new DAO");
            site.setElevation(50.0);
            site.setElevationUnits("ft");

            site.setProperty("test_prop_1", "test_value_1");
            site.setProperty("test_prop_2", "test_value_2");
            final var timeSaved = new Date();
            final var siteOut = dao.save(tx, site);

            assertNotNull(siteOut);

            assertEquals("Bob's ville", site.nearestCity);
            assertTrue(siteOut.getLastModifyTime().getTime() >= timeSaved.getTime());

            final var siteByName = dao.getBySiteName(tx, siteName2);
            assertTrue(siteByName.isPresent());

            final var updateSiteIn = siteByName.get();
            updateSiteIn.nearestCity = "Bob's Town"; // The place grew
            final var updateSiteOut = dao.save(tx, updateSiteIn);
            assertEquals("Bob's Town", updateSiteOut.nearestCity);
            assertEquals(updateSiteIn.getDisplayName(), updateSiteOut.getDisplayName());

            
            dao.delete(tx, siteOut.getId());
            var siteByNameAfterDelete = dao.getBySiteName(tx, siteName2);
            assertTrue(siteByNameAfterDelete.isEmpty());

        }

    }
  
    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(SiteDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var sites =  dao.getAll(tx, -1, -1);
            assertFalse(sites.isEmpty());

            final var numSites = 50;
            for (int i = 0; i < numSites; i++)
            {
                var site = new Site();

                var siteName = new SiteName(site, "CWMS", String.format("00AA_TestSite_%02d", i));
                site.addName(siteName);

                if (i % 8 == 0)
                {
                    var siteName2 = new SiteName(site, "local", String.format("Local-Test-Site-%02d", i));
                    site.addName(siteName2);
                }

                if (i % 7 == 0)
                {
                    site.setProperty("test-prop-" + i, "test-val-" + i);
                    site.setProperty("test-prop-" + (i+1), "test-val-" + (i+1));
                }

                dao.save(tx, site);
            }


            final var siteNoPrefName = new Site();
            siteNoPrefName.setDescription("A site that exist to not have a name matching the prefered type");
            var settings = db.getSettings(DecodesSettings.class).orElseThrow();
            var pref = settings.siteNameTypePreference;
            var type = "local";
            if (pref.equals("local"))
            {
                type = "CWMS";
            }
            var name = new SiteName(siteNoPrefName, type, String.format("00AAAA-Test-Site-%02d", numSites));
            siteNoPrefName.addName(name);
            assertThrows(RequiredSiteNameMissingException.class ,() -> dao.save(tx, siteNoPrefName));


            var allSites = dao.getAll(tx, numSites+1, 0);
            assertEquals(numSites+1, allSites.size());

            var first10 = dao.getAll(tx, 10, 0);
            assertEquals(10, first10.size());
            assertEquals("00AAAA-Test-Site-00", first10.getFirst().getName("CWMS").getNameValue());
            assertEquals("00AAAA-Test-Site-09", first10.getLast().getName("CWMS").getNameValue());

            var seventh = first10.get(7);
            assertEquals("test-val-7", seventh.getProperty("test-prop-7"));
            assertEquals("test-val-8", seventh.getProperty("test-prop-8"));

            var eigth = first10.get(8);
            assertTrue(eigth.hasNameValue("Local-Test-Site-08"));

            var second10 = dao.getAll(tx, 10, 10);
            assertEquals(10, second10.size());
            assertEquals("00AAAA-Test-Site-10", second10.getFirst().getName("CWMS").getNameValue());
            assertEquals("00AAAA-Test-Site-19", second10.getLast().getName("CWMS").getNameValue());

            var improperSite = dao.getBySiteName(tx, name);
            assertTrue(improperSite.isPresent());
        }
    }
}
