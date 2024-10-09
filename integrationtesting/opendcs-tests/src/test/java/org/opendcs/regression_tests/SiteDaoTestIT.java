package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.SiteDAI;

public class SiteDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb tsdb;

    @Test
    @EnableIfSql
    public void test_insertion_and_selection() throws Exception
    {
        try(SiteDAI dao = tsdb.makeSiteDAO();)
        {
            Site s = new Site();
            s.addName(new SiteName(s,"CWMS","TestSite"));
            s.setActive(true);
            s.setDescription("A test site");
            dao.writeSite(s);

            Site ret = dao.getSiteBySiteName(new SiteName(null,"CWMS","TestSite"));
            assertNotNull(ret,"Could not round-trip site");
            assertEquals("TestSite",ret.getName("CWMS").getNameValue(),"SiteName mappings are incorrect.");

            dao.deleteSite(ret.getKey());
            assertThrows(NoSuchObjectException.class, ()-> dao.getSiteById(ret.getId()), "Site was not deleted;");
        }
    }

    /**
     * TODO: need to determine a way to properly distiguish a required sitename.
     * E.G. for CWMS the CWMS location table is used and can't be removed.
     * @throws Exception
     */
    @Test
    @EnableIfSql
    public void test_updating_record() throws Exception
    {
        try(SiteDAI dao = tsdb.makeSiteDAO();)
        {
            Site s = new Site();
            s.addName(new SiteName(s,"CWMS","TestSite"));
            s.addName(new SiteName(s, "local", "TestSite-local name"));
            s.setActive(true);
            s.setDescription("A test site");
            dao.writeSite(s);

            Site ret = dao.getSiteBySiteName(new SiteName(null,"CWMS","TestSite"));
            assertNotNull(ret,"Could not round-trip site");
            assertEquals("TestSite",ret.getName("CWMS").getNameValue(),"SiteName mappings are incorrect.");

            s.removeName("local");
            s.addName(new SiteName(s,"HDB","915"));
            dao.writeSite(s);

            Site ret2 = dao.getSiteById(ret.getId());
            assertNotNull(ret2, "Could not get Site again from database");
            assertEquals("915",ret2.getName("HDB").getNameValue(), "new SiteName was not set.");
        }
    }

    @Test
    @EnableIfSql
    public void test_fill_cache() throws Exception
    {
        try(SiteDAI dao = tsdb.makeSiteDAO();)
        {
            long lastFill = dao.getLastCacheFillMsec();
            dao.fillCache();
            long afterFill = dao.getLastCacheFillMsec();
            assertTrue( afterFill > lastFill, "Cache does not appear to have been filled by this call.");
        }
    }
}