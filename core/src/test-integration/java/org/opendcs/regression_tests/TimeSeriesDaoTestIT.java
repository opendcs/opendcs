package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.EnableIfTsDb;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.TimeSeriesDAI;

public class TimeSeriesDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;


    @Test
    @EnableIfTsDb
    public void test_timeseries_store_and_retrieval()
    {
        try (TimeSeriesDAI dao = db.makeTimeSeriesDAO();)
        {
        }
        assertNotNull(db, "Timeseries Database was not provided to ");
    }
    
}
