package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesDb;

@EnableIfTsDb({"OpenDCS-Postgres"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml"
})
class OpenDcsTimeSeriesIdentifierDaoTestIT extends AppTestBase
{

    @ConfiguredField
    OpenDcsDatabase db;

    @ConfiguredField
    TimeSeriesDb tsDb; // until save is implemented

    @Test
    void test_retrieve_time_series_identifier() throws Exception
    {
        var tsIdIn = tsDb.makeTsId("TESTSITE1.Precip.Total.1Hour.1Hour.test");
        tsIdIn.setStorageUnits("mm");
        tsIdIn.setDescription("Simple test identifier.");

        try (var dao = tsDb.makeTimeSeriesDAO())
        {
            var id = dao.createTimeSeries(tsIdIn);
            assertFalse(DbKey.isNull(id));
        }

        var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var tsIdOutResult = dao.findBy(tx, tsIdIn.getUniqueString());

            assertTrue(tsIdOutResult.isSuccess());
            var tsIdOut = tsIdOutResult.getSuccess().orElseGet(() -> fail("time series not retrieved."));
            assertNotNull(tsIdOut);
            assertNotNull(tsIdOut.getDataType());
            assertFalse(DbKey.isNull(tsIdOut.getSite().getId()));
        }
    }
}
