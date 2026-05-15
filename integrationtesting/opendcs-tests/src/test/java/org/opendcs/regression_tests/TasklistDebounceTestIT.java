package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.DataCollection;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

/**
 * Verifies tasklistDebounceSeconds filters fresh cp_comp_tasklist rows out of
 * getNewData() while still returning aged ones. The test inserts two rows
 * (one with date_time_loaded = now-100ms, one with now-10s), runs getNewData
 * + releaseNewData, then asserts which specific record_nums survive. Aged
 * rows are always consumed; fresh rows survive iff debounce held them back.
 */
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
final class TasklistDebounceTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;

    private static final String APP_NAME = "compproc_regtest";
    private static final long FRESH_AGE_MS = 100L;
    private static final long AGED_AGE_MS = 10_000L;

    @Test
    void debounce_excludes_fresh_row_keeps_aged_row_processed() throws Exception
    {
        db.setTasklistDebounceSeconds(2);
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey tsKey = createInputTs("TESTSITE1.Stage.Inst.15Minutes.0.debounce-window", tsDAI);
            DbKey sourceKey = ensureDataSource(dao, appKey);

            long now = System.currentTimeMillis();
            DbKey freshRec = insertTasklistRow(dao, appKey, tsKey, sourceKey, now - FRESH_AGE_MS);
            DbKey agedRec = insertTasklistRow(dao, appKey, tsKey, sourceKey, now - AGED_AGE_MS);

            consumeNewData(tsDAI, appKey);

            assertTrue(rowExists(dao, freshRec),
                "Fresh row should remain in cp_comp_tasklist (debounce held it back).");
            assertFalse(rowExists(dao, agedRec),
                "Aged row should have been processed and removed by getNewData/releaseNewData.");
        }
    }

    @Test
    void debounce_zero_processes_both_rows() throws Exception
    {
        db.setTasklistDebounceSeconds(0);
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey tsKey = createInputTs("TESTSITE1.Stage.Inst.15Minutes.0.debounce-off", tsDAI);
            DbKey sourceKey = ensureDataSource(dao, appKey);

            long now = System.currentTimeMillis();
            DbKey freshRec = insertTasklistRow(dao, appKey, tsKey, sourceKey, now - FRESH_AGE_MS);
            DbKey agedRec = insertTasklistRow(dao, appKey, tsKey, sourceKey, now - AGED_AGE_MS);

            consumeNewData(tsDAI, appKey);

            assertFalse(rowExists(dao, freshRec),
                "With debounce=0, fresh row should have been processed and removed.");
            assertFalse(rowExists(dao, agedRec),
                "Aged row should have been processed and removed.");
        }
    }

    /**
     * Run getNewData followed by releaseNewData so that both bad-recs (deleted
     * inline by getNewData) and validly-processed rows (deleted by releaseNewData)
     * are cleared from cp_comp_tasklist. Test assertions then only depend on
     * whether a row was visible to getNewData at all, not on which deletion path
     * it took.
     */
    private void consumeNewData(TimeSeriesDAI tsDAI, DbKey appKey) throws Exception
    {
        DataCollection dc = tsDAI.getNewData(appKey, 20000);
        db.releaseNewData(dc, tsDAI, 250);
    }

    private DbKey createInputTs(String tsidStr, TimeSeriesDAI tsDAI) throws Exception
    {
        TimeSeriesIdentifier tsId = db.makeTsId(tsidStr);
        return tsDAI.createTimeSeries(tsId);
    }

    private DbKey insertTasklistRow(DaoBase dao, DbKey appKey, DbKey tsKey, DbKey sourceKey,
                                    long dateTimeLoadedMs)
            throws Exception
    {
        try (Connection c = db.getConnection())
        {
            DbKey recNum = db.getKeyGenerator().getKey("cp_comp_tasklist", c);
            dao.doModify(
                "insert into cp_comp_tasklist(record_num, loading_application_id, "
                + "ts_id, num_value, date_time_loaded, sample_time, flags, source_id) "
                + "values (?,?,?,?,?,?,?,?)",
                recNum, appKey, tsKey, 1.0,
                dateTimeLoadedMs, dateTimeLoadedMs, 0, sourceKey);
            return recNum;
        }
    }

    private boolean rowExists(DaoBase dao, DbKey recNum) throws Exception
    {
        Integer count = dao.getSingleResult(
            "select count(*) from cp_comp_tasklist where record_num = ?",
            rs -> rs.getInt(1),
            recNum);
        return count != null && count > 0;
    }

    /**
     * cp_comp_tasklist.source_id has a NOT NULL FK to TSDB_DATA_SOURCE. Reuse an
     * existing row for this loading app if one is present, otherwise create one.
     */
    private DbKey ensureDataSource(DaoBase dao, DbKey appKey) throws Exception
    {
        DbKey existing = dao.getSingleResultOr(
            "select source_id from tsdb_data_source where loading_application_id = ? "
            + "and module is null",
            rs -> DbKey.createDbKey(rs, 1),
            null,
            appKey);
        if (existing != null && !DbKey.isNull(existing))
            return existing;

        try (Connection c = db.getConnection())
        {
            DbKey sourceKey = db.getKeyGenerator().getKey("tsdb_data_source", c);
            dao.doModify(
                "insert into tsdb_data_source(source_id, loading_application_id, module) "
                + "values (?,?,null)",
                sourceKey, appKey);
            return sourceKey;
        }
    }
}
