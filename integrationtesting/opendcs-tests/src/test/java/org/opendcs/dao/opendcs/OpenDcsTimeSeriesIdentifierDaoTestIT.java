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
import org.opendcs.database.dai.CompDependsNotifyDao;
import org.opendcs.database.dai.SiteDao;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.TimeSeriesDb;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
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
        var notificationDao = db.getDao(CompDependsNotifyDao.class).orElseThrow();
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

            var tsModifiedRecord = notificationDao.getAllNotifyRecords(tx)
                                                  .stream()
                                                  .filter(cdn -> cdn.getKey().equals(tsIdOut.getKey()))
                                                  .filter(cdn -> CpDependsNotify.TS_MODIFIED == cdn.getEventType())
                                                  .findFirst()
                                                  .orElseGet(() -> fail("Not compdepends modification record"))
                                                  ;

            notificationDao.deleteNotifyRecord(tx, tsModifiedRecord);

            assertEquals(tsi, tsOutByTsi);
            assertEquals(tsi, tsOutById);

            var units = dao.getStorageUnitsFor(tx, tsIdOut);
            assertTrue(units.isPresent());

            final String description = "This is a changed description";
            tsIdOut.setDescription(description);
            var tsIdDesc = dao.save(tx, tsIdOut).getDescription();

            assertEquals(description, tsIdDesc);

            var tsModifiedRecord2 = notificationDao.getAllNotifyRecords(tx)
                                                  .stream()
                                                  .filter(cdn -> cdn.getKey().equals(tsIdOut.getKey()))
                                                  .filter(cdn -> CpDependsNotify.TS_MODIFIED == cdn.getEventType())
                                                  .findFirst()
                                                  .orElseGet(() -> fail("Not compdepends modification record"))
                                                  ;
            notificationDao.deleteNotifyRecord(tx, tsModifiedRecord2);


            dao.delete(tx, tsIdOut.getKey());

            assertFalse(dao.getById(tx, tsIdOut.getKey()).isPresent());

            tx.rollback();
        }
    }


    @Test
    void test_pagination() throws Exception
    {
        var siteDao = db.getDao(SiteDao.class).orElseThrow();
        var tsDao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();

        final String testSiteOne = "AAATSTest1";
        final String testSiteTwo = "AAATSTest2";
        final String[] SITES = new String[]{testSiteOne, testSiteTwo};

        try (var tx = db.newTransaction())
        {
            var siteOne = new Site();
            siteOne.addName(Constants.snt_CWMS, testSiteOne);
            var siteTwo = new Site();
            siteTwo.addName(Constants.snt_CWMS, testSiteTwo);
            siteTwo.addName(Constants.snt_NWSHB5, "TSTEST2");

            siteDao.save(tx, siteOne);
            siteDao.save(tx, siteTwo);

            final int COUNT = 50;
            for (int i = 0; i < COUNT; i++)
            {
                for (int j = 0; j < SITES.length; j++)
                {
                    final var tsName = String.format("%s.Precip.Total.1Hour.1Hour.test-%d", SITES[j], i);
                    var tsIdIn = tsDao.makeTsId(tx, tsName);
                    var tsIdOut = tsDao.save(tx, tsIdIn);
                    assertNotNull(tsIdOut, () -> String.format("Could not save %s", tsName));
                }
            }

            var all = tsDao.getAll(tx, -1, -1);
            assertTrue(all.size() >= COUNT * SITES.length);
            var first10 = tsDao.getAll(tx, 10, 0);
            var second10 = tsDao.getAll(tx, 10, 10);

            assertEquals(siteOne.getPreferredName().getNameValue(), first10.getFirst().getSiteName());

            assertEquals(all.subList(0, 10), first10);
            assertEquals(all.subList(10, 20), second10);

            tx.rollback();
        }
    }
}
