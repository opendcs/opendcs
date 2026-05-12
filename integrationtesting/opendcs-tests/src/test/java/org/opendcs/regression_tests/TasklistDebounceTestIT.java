package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.DataCollection;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

/**
 * Verifies the tasklistDebounceSeconds setting added to keep getNewData()
 * from returning entries that were just inserted by the same compproc cycle.
 * See DecodesSettings.tasklistDebounceSeconds and the getNewData() implementations
 * in OpenTimeSeriesDAO, CwmsTimeSeriesDAO, HdbTimeSeriesDAO.
 */
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
@EnableIfTsDb({"OpenDCS-Postgres"})
final class TasklistDebounceTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;

    private static final String APP_NAME = "compproc_regtest";

    @Test
    void debounce_zero_returns_fresh_row() throws Exception
    {
        DecodesSettings.instance().tasklistDebounceSeconds = 0;
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey tsKey = createInputTs("TESTSITE1.Stage.Inst.15Minutes.0.debounce-default", tsDAI);
            DbKey sourceKey = ensureDataSource(dao, appKey);
            insertTasklistRow(dao, appKey, tsKey, sourceKey);

            DataCollection data = tsDAI.getNewData(appKey, 20000);
            assertTrue(rowPresent(data, tsKey),
                    "With debounce=0, freshly inserted tasklist row must be returned.");
        }
    }

    @Test
    void debounce_excludes_fresh_row_then_includes_after_age() throws Exception
    {
        DecodesSettings.instance().tasklistDebounceSeconds = 2;
        try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
             LoadingAppDAI laDAI = db.makeLoadingAppDAO();
             DaoBase dao = new DaoBase(db, "test"))
        {
            DbKey appKey = laDAI.lookupAppId(APP_NAME);
            DbKey tsKey = createInputTs("TESTSITE1.Stage.Inst.15Minutes.0.debounce-fresh", tsDAI);
            DbKey sourceKey = ensureDataSource(dao, appKey);
            insertTasklistRow(dao, appKey, tsKey, sourceKey);

            DataCollection immediate = tsDAI.getNewData(appKey, 20000);
            assertEquals(false, rowPresent(immediate, tsKey),
                    "With debounce=2s, freshly inserted row must be excluded.");

            Thread.sleep(3000L);

            DataCollection aged = tsDAI.getNewData(appKey, 20000);
            assertTrue(rowPresent(aged, tsKey),
                    "After 3s, the row should age past the 2s debounce window.");
        }
    }

    private DbKey createInputTs(String tsidStr, TimeSeriesDAI tsDAI) throws Exception
    {
        TimeSeriesIdentifier tsId = db.makeTsId(tsidStr);
        return tsDAI.createTimeSeries(tsId);
    }

    private void insertTasklistRow(DaoBase dao, DbKey appKey, DbKey tsKey, DbKey sourceKey)
            throws Exception
    {
        try (Connection c = db.getConnection())
        {
            KeyGenerator keyGen = db.getKeyGenerator();
            dao.doModify(
                "insert into cp_comp_tasklist(record_num, loading_application_id, "
                + "ts_id, num_value, date_time_loaded, sample_time, flags, source_id) "
                + "values (?,?,?,?,?,?,?,?)",
                keyGen.getKey("cp_comp_tasklist", c), appKey, tsKey, 1.0,
                new Date(), new Date(), 0, sourceKey);
        }
    }

    /**
     * cp_comp_tasklist.source_id has a NOT NULL FK to TSDB_DATA_SOURCE. Reuse an
     * existing row for this loading app if one is present (compproc may have
     * inserted one already), otherwise create one.
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

    private boolean rowPresent(DataCollection dc, DbKey tsKey)
    {
        return dc.getAllTimeSeries().stream()
                .anyMatch(ts -> ts.getTimeSeriesIdentifier() != null
                        && tsKey.equals(ts.getTimeSeriesIdentifier().getKey()));
    }
}
