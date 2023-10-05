package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.ConfiguredField;
import org.opendcs.fixtures.EnableIfSql;

import decodes.db.Site;
import decodes.db.SiteName;
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
        }
    }
}