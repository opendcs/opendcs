package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import org.jdbi.v3.core.Handle;
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
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsImporter;
import decodes.tsdb.VarFlags;
import ilex.var.TimedVariable;
import opendcs.opentsdb.OpenTimeSeriesDAO;
import opendcs.opentsdb.StorageTableSpec;

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
    void updating_description_preserves_storage_table() throws Exception
    {
        var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
        try (var timeSeriesDao = tsDb.makeTimeSeriesDAO())
        {
            TsImporter.importTimeSeriesFile(tsDb,
                getResource(configuration, "Comps/ToIrregular/Test1/timeseries/inputs/test1-inst-input.tsimport"),
                null, TimeZone.getTimeZone("UTC"), Constants.snt_CWMS);

            var original = timeSeriesDao.getTimeSeriesIdentifier("TESTSITE1.Precip.Inst.1Hour.0.input");
            try (var tx = db.newTransaction())
            {
                try
                {
                    var tsId = (CwmsTsId)dao.getById(tx, original.getKey()).orElseThrow();
                    var before = timeSeriesDao.makeTimeSeries(tsId);
                    timeSeriesDao.fillTimeSeries(before, null, null);
                    assertEquals(14, before.size());

                    tsId.setDescription("Updated description");
                    var updated = (CwmsTsId)dao.save(tx, tsId);
                    var after = timeSeriesDao.makeTimeSeries(updated);
                    after.setTimeSeriesIdentifier(updated);
                    timeSeriesDao.fillTimeSeries(after, null, null);

                    assertEquals("Updated description", updated.getDescription());
                    assertAll(
                        () -> assertEquals(tsId.getStorageTable(), updated.getStorageTable(),
                            "Changing a description must not move the series to another storage table"),
                        () -> org.opendcs.fixtures.assertions.TimeSeries.assertEquals(before, after,
                            "Changing a description must preserve access to the stored values"));
                }
                finally
                {
                    tx.rollback();
                }
            }
            finally
            {
                timeSeriesDao.deleteTimeSeries(original);
            }
        }
    }

    @Test
    void allocating_series_preserves_existing_annual_load() throws Exception
    {
        var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
        try (var tx = db.newTransaction();
             var storageDao = (OpenTimeSeriesDAO)tsDb.makeTimeSeriesDAO())
        {
            storageDao.setManualConnection(tx.connection(Handle.class).orElseThrow().getConnection());
            int tableCount = storageDao.getTableSpecs('N').size();
            assertTrue(tableCount > 0);
            for (int i = 0; i < tableCount; i++)
            {
                dao.save(tx, dao.makeTsId(tx, "TESTSITE1.Precip.Total.1Hour.1Hour.loadseed" + i));
            }
            int before = storageDao.getTableSpecs('N').stream()
                                   .mapToInt(StorageTableSpec::getEstAnnualValues).sum();

            var input = (CwmsTsId)dao.makeTsId(tx, "TESTSITE1.Precip.Total.1Hour.1Hour.loadtest");
            dao.save(tx, input);
            int after = storageDao.getTableSpecs('N').stream()
                                  .mapToInt(StorageTableSpec::getEstAnnualValues).sum();

            assertEquals(before + OpenTimeSeriesDAO.interval2estAnnualValues(input.getIntervalOb()), after,
                         "Allocation must add to the existing annual load");
            tx.rollback();
        }
    }

    /** Verifies an empty read does not allocate storage and stale reader metadata can find the first write. */
    @Test
    void stale_reader_and_writer_preserve_storage_assignment() throws Exception
    {
        try (var reader = tsDb.makeTimeSeriesDAO();
             var writer = tsDb.makeTimeSeriesDAO())
        {
            var id = (CwmsTsId)tsDb.makeTsId("TESTSITE1.Precip.Total.1Hour.1Hour.storage-regression");
            id.setStorageUnits("in");
            reader.createTimeSeries(id); // TS_SPEC: table = -1.
            try
            {
                var readerId = id.copyNoKey(); // Reader also has -1.
                readerId.setKey(id.getKey());
                var output = new CTimeSeries(readerId);
                output.setUnitsAbbr("in");
                assertEquals(0, reader.fillTimeSeries(output, null, null)); // Still unassigned.
                var dao = db.getDao(TimeSeriesIdentifierDao.class).orElseThrow();
                try (var tx = db.newTransaction())
                {
                    assertEquals(-1, ((CwmsTsId)dao.getById(tx, id.getKey()).orElseThrow()).getStorageTable(),
                        "Reading an empty series must not allocate storage");
                }

                var input = new CTimeSeries(id);
                input.setUnitsAbbr("in");
                input.addSample(new TimedVariable(Date.from(Instant.parse("2012-01-01T00:00:00Z")),
                    12.5, VarFlags.TO_WRITE));
                writer.saveTimeSeries(input); // Assigns TS_NUM_####; reader still has -1.
                assertEquals(1, reader.fillTimeSeries(output, null, null)); // Reader finds the assigned table.
                assertEquals(12.5, output.sampleAt(0).getDoubleValue());
            }
            finally
            {
                reader.deleteTimeSeries(id);
            }
        }
    }

    @Test
    void fetching_next_notification() throws Exception
    {
        var notificationDao = db.getDao(CompDependsNotifyDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            for (var record : notificationDao.getAllNotifyRecords(tx))
            {
                notificationDao.deleteNotifyRecord(tx, record);
            }
            assertTrue(notificationDao.getNextRecord(tx).isEmpty());

            var record = new CpDependsNotify();
            record.setKey(DbKey.createDbKey(123));
            record.setEventType(CpDependsNotify.TS_MODIFIED);
            notificationDao.saveRecord(tx, record);

            var next = notificationDao.getNextRecord(tx).orElseThrow();
            assertEquals(record.getKey(), next.getKey());
            assertEquals(record.getEventType(), next.getEventType());
            notificationDao.deleteNotifyRecord(tx, next);
            assertTrue(notificationDao.getNextRecord(tx).isEmpty());
            tx.rollback();
        }
    }

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
            assertTrue(notificationDao.getAllNotifyRecords(tx).stream()
                           .noneMatch(record -> record.getRecordNum() == tsModifiedRecord.getRecordNum()),
                       "Deleted notification must no longer be present");

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
        final String[] sites = new String[]{testSiteOne, testSiteTwo};

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
                for (int j = 0; j < sites.length; j++)
                {
                    final var tsName = String.format("%s.Precip.Total.1Hour.1Hour.test-%d", sites[j], i);
                    var tsIdIn = tsDao.makeTsId(tx, tsName);
                    var tsIdOut = tsDao.save(tx, tsIdIn);
                    assertNotNull(tsIdOut, () -> String.format("Could not save %s", tsName));
                }
            }

            var all = tsDao.getAll(tx, -1, -1);
            assertTrue(all.size() >= COUNT * sites.length);
            var first10 = tsDao.getAll(tx, 10, 0);
            var second10 = tsDao.getAll(tx, 10, 10);

            assertEquals(siteOne.getPreferredName().getNameValue(), first10.getFirst().getSiteName());

            assertEquals(all.subList(0, 10), first10);
            assertEquals(all.subList(10, 20), second10);

            tx.rollback();
        }
    }
}
