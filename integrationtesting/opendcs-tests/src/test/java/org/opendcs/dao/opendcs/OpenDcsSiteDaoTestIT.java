package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.Site;
import decodes.db.SiteName;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
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
            site.nearestCity = "Bob's ville"; // we're specifically testing that '  here
            site.setLastModifyTime(new Date());
            site.setPublicName("A site that exists to test the new DAO");
            site.setProperty("test_prop_1", "test_value_1");
            site.setProperty("test_prop_2", "test_value_2");

            final var siteOut = dao.save(tx, site);

            assertNotNull(siteOut);

            assertEquals("Bob's ville", site.nearestCity);
            assertEquals(site.getLastModifyTime(), siteOut.getLastModifyTime());

            final var siteByName = dao.getBySiteName(tx, siteName2);
            assertTrue(siteByName.isPresent());

            
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
        }
       
    }
}
