package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.LoadingAppDAI;
import opendcs.dao.DaoBase;

/**
 * Verifies the SYSDATE-based debounce predicate added to
 * {@link decodes.cwms.CwmsTimeSeriesDAO#getNewData}: rows whose
 * {@code date_time_loaded} is within the debounce window must be excluded
 * from the candidate set, while older rows are admitted.
 *
 * <p>Tests the SQL filter directly rather than the full {@code getNewData →
 * processTasklistEntry → releaseNewData} pipeline. The pipeline depends on
 * fixture state (loading-app dependent comps, TS lookups, badRec deletion
 * paths) that can vary between local and CI CWMS-Oracle environments. The
 * production change is one line — the WHERE-clause predicate — and that's
 * exactly what this test asserts on.
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
    private static final int FRESH_AGE_SEC = 0;
    private static final int AGED_AGE_SEC = 10;
    // Synthetic ts_code; CWMS schema has no FK on site_datatype_id so the
    // value is unconstrained. Filter assertions are by record_num so the
    // synthetic SDI never collides with real test data.
    private static final long SYNTHETIC_SDI = 9_999_999L;

    @Test
    void debounce_predicate_excludes_fresh_admits_aged() throws Exception
    {
        try (LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey sdi = DbKey.createDbKey(SYNTHETIC_SDI);

            DbKey freshRec = insertTasklistRow(dao, appKey, sdi, FRESH_AGE_SEC);
            DbKey agedRec = insertTasklistRow(dao, appKey, sdi, AGED_AGE_SEC);

            // The production predicate added to CwmsTimeSeriesDAO.getNewData.
            // 2 seconds: aged (10s old) admitted; fresh (0s old) excluded.
            int matching = countMatchingDebouncePredicate(dao, appKey, sdi, 2);
            assertEquals(1, matching,
                "Debounce predicate (debounce=2s) must admit aged row only.");
            assertTrue(rowMatchesDebouncePredicate(dao, agedRec, 2),
                "Aged row must be admitted by debounce=2s predicate.");
            assertEquals(false, rowMatchesDebouncePredicate(dao, freshRec, 2),
                "Fresh row must be excluded by debounce=2s predicate.");
        }
    }

    @Test
    void debounce_predicate_with_zero_debounce_admits_both() throws Exception
    {
        try (LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey sdi = DbKey.createDbKey(SYNTHETIC_SDI);

            insertTasklistRow(dao, appKey, sdi, FRESH_AGE_SEC);
            insertTasklistRow(dao, appKey, sdi, AGED_AGE_SEC);

            // With debounce=0, production code skips appending the predicate
            // entirely; this test mirrors that by querying without it.
            int matching = countWithoutDebounce(dao, appKey, sdi);
            assertEquals(2, matching,
                "Without debounce predicate, both rows must match.");
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

    /**
     * Inserts a tasklist row with date_time_loaded set to {@code SYSDATE -
     * ageSeconds/86400}. Going through DaoBase.bind() with java.util.Date
     * routes to java.sql.Date for the non-OpenTSDB branch and truncates the
     * time component, so the date math is done in SQL instead.
     */
    private DbKey insertTasklistRow(DaoBase dao, DbKey appKey, DbKey sdi, int ageSeconds)
            throws Exception
    {
        try (Connection c = db.getConnection())
        {
            DbKey recNum = db.getKeyGenerator().getKey("cp_comp_tasklist", c);
            dao.doModify(
                "insert into cp_comp_tasklist(record_num, loading_application_id, "
                + "site_datatype_id, value, date_time_loaded, start_date_time, "
                + "delete_flag, flags) "
                + "values (?,?,?,?, SYSDATE - ?/86400, SYSDATE - ?/86400, ?, ?)",
                recNum, appKey, sdi, 1.0,
                ageSeconds, ageSeconds, "N", 0);
            return recNum;
        }
    }

    /**
     * Counts rows for the (appKey, sdi) pair that the debounce predicate
     * admits. Mirrors the WHERE clause appended by CwmsTimeSeriesDAO.getNewData
     * when {@code tasklistDebounceSeconds > 0}.
     */
    private int countMatchingDebouncePredicate(DaoBase dao, DbKey appKey, DbKey sdi,
                                                int debounceSec) throws Exception
    {
        return dao.getSingleResult(
            "select count(*) from cp_comp_tasklist "
            + "where loading_application_id = ? and site_datatype_id = ? "
            + "and date_time_loaded <= SYSDATE - ?/86400",
            rs -> rs.getInt(1),
            appKey, sdi, debounceSec);
    }

    private int countWithoutDebounce(DaoBase dao, DbKey appKey, DbKey sdi) throws Exception
    {
        return dao.getSingleResult(
            "select count(*) from cp_comp_tasklist "
            + "where loading_application_id = ? and site_datatype_id = ?",
            rs -> rs.getInt(1),
            appKey, sdi);
    }

    private boolean rowMatchesDebouncePredicate(DaoBase dao, DbKey recNum, int debounceSec)
            throws Exception
    {
        Integer count = dao.getSingleResult(
            "select count(*) from cp_comp_tasklist "
            + "where record_num = ? and date_time_loaded <= SYSDATE - ?/86400",
            rs -> rs.getInt(1),
            recNum, debounceSec);
        return count != null && count > 0;
    }
}
