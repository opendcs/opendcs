package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.DataCollection;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

/**
 * CWMS-Oracle counterpart of {@link TasklistDebounceTestIT}. Exercises the
 * SYSDATE-based debounce predicate in {@link decodes.cwms.CwmsTimeSeriesDAO}.
 * Uses the CWMS cp_comp_tasklist schema (site_datatype_id, Oracle DATE
 * columns, no source_id NOT NULL constraint). Inserts a fresh row and an aged
 * row referring to a synthetic site_datatype_id, runs getNewData +
 * releaseNewData, and asserts which specific record_nums survived.
 */
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
@EnableIfTsDb({"CWMS-Oracle"})
final class CwmsTasklistDebounceTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;

    private static final String APP_NAME = "compproc_regtest";
    private static final long FRESH_AGE_MS = 100L;
    private static final long AGED_AGE_MS = 10_000L;
    // Synthetic ts_code used as the site_datatype_id. No FK constraint, so the
    // row is treated as a non-existent TSID (NoSuchObjectException →
    // bad-rec) by processTasklistEntry — exactly what the assertions expect.
    private static final long SYNTHETIC_SDI = 9_999_999L;

    @Test
    void debounce_excludes_fresh_row_keeps_aged_row_processed() throws Exception
    {
        DecodesSettings.instance().tasklistDebounceSeconds = 2;
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey sdi = DbKey.createDbKey(SYNTHETIC_SDI);

            long now = System.currentTimeMillis();
            DbKey freshRec = insertTasklistRow(dao, appKey, sdi, new Date(now - FRESH_AGE_MS));
            DbKey agedRec = insertTasklistRow(dao, appKey, sdi, new Date(now - AGED_AGE_MS));

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
        DecodesSettings.instance().tasklistDebounceSeconds = 0;
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey sdi = DbKey.createDbKey(SYNTHETIC_SDI);

            long now = System.currentTimeMillis();
            DbKey freshRec = insertTasklistRow(dao, appKey, sdi, new Date(now - FRESH_AGE_MS));
            DbKey agedRec = insertTasklistRow(dao, appKey, sdi, new Date(now - AGED_AGE_MS));

            consumeNewData(tsDAI, appKey);

            assertFalse(rowExists(dao, freshRec),
                "With debounce=0, fresh row should have been processed and removed.");
            assertFalse(rowExists(dao, agedRec),
                "Aged row should have been processed and removed.");
        }
    }

    @AfterEach
    void cleanupSyntheticRows() throws Exception
    {
        try (DaoBase dao = new DaoBase(db, "cleanup"))
        {
            dao.doModify(
                "delete from cp_comp_tasklist where site_datatype_id = ?",
                DbKey.createDbKey(SYNTHETIC_SDI));
        }
    }

    private void consumeNewData(TimeSeriesDAI tsDAI, DbKey appKey) throws Exception
    {
        DataCollection dc = tsDAI.getNewData(appKey, 20000);
        db.releaseNewData(dc, tsDAI, 250);
    }

    private DbKey insertTasklistRow(DaoBase dao, DbKey appKey, DbKey sdi, Date dateTimeLoaded)
            throws Exception
    {
        try (Connection c = db.getConnection())
        {
            DbKey recNum = db.getKeyGenerator().getKey("cp_comp_tasklist", c);
            dao.doModify(
                "insert into cp_comp_tasklist(record_num, loading_application_id, "
                + "site_datatype_id, value, date_time_loaded, start_date_time, "
                + "delete_flag, flags) "
                + "values (?,?,?,?,?,?,?,?)",
                recNum, appKey, sdi, 1.0,
                dateTimeLoaded, dateTimeLoaded, "N", 0);
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
}
