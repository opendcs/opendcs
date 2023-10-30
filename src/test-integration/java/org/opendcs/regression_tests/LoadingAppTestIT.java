package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;

import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dao.LoadingAppDao;



public class LoadingAppTestIT extends AppTestBase
{
    @ConfiguredField
    protected TimeSeriesDb db;

    @Test
    @EnableIfSql
    public void test_utilityClassIsPresent() throws Exception
    {
        try(LoadingAppDao dao = new LoadingAppDao(db);)
        {
            CompAppInfo appInfo = dao.getComputationApp("utility");
            assertNotNull(appInfo, "Utility loading application was not present in the database.");
            assertEquals("utility",appInfo.getAppName(), "Returned application info is incorrect.");
        }
    }
}
