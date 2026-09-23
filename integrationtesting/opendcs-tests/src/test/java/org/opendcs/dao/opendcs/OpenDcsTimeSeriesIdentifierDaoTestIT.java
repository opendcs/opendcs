package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import decodes.cwms.CwmsTsId;
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
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var tsIdIn = dao.makeTsId(tx, "TESTSITE1.Precip.Total.1Hour.1Hour.test");
            var tsi = dao.save(tx, tsIdIn);
            assertFalse(DbKey.isNull(tsi.getKey()));

            var cwmsTsId = assertInstanceOf(CwmsTsId.class, tsi);
            assertNotEquals(-1, cwmsTsId.getStorageTable());
            var tsIdOutResult = dao.findBy(tx, tsIdIn.getUniqueString());

            assertTrue(tsIdOutResult.isSuccess());
            var tsIdOut = tsIdOutResult.success().orElseGet(() -> fail("time series not retrieved."));
            assertNotNull(tsIdOut);
            assertNotNull(tsIdOut.getDataType());
            assertFalse(DbKey.isNull(tsIdOut.getSite().getId()));

            var tsOutByTsi = dao.getByTimeSeriesIdentifier(tx, tsIdIn)
                                .orElseGet(() -> fail("TS ID not found."));
            var tsOutById = dao.getById(tx, tsi.getKey())
                               .orElseGet(() -> fail("TS ID not found."));

            assertEquals(tsi, tsOutByTsi);
            assertEquals(tsi, tsOutById);


            dao.delete(tx, tsIdOut.getKey());

            assertFalse(dao.getById(tx, tsIdOut.getKey()).isPresent());

            tx.rollback();
        }
    }


    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            final int COUNT = 50;
            for (int i = 0; i < COUNT; i++)
            {
                final var tsName = String.format("TESTSITE1.Precip.Total.1Hour.1Hour.test-%d", i);
                var tsIdIn = dao.makeTsId(tx, tsName);
                var tsIdOut = dao.save(tx, tsIdIn);
                assertNotNull(tsIdOut, () -> String.format("Could not save %s", tsName));
            }


            var all = dao.getAll(tx, -1, -1);
            assertTrue(all.size() >= 50);
            var first10 = dao.getAll(tx, 10, 0);
            var second10 = dao.getAll(tx, 10, 10);

            assertEquals(all.subList(0, 10), first10);
            assertEquals(all.subList(10, 20), second10);

            tx.rollback();
        }
    }
}
