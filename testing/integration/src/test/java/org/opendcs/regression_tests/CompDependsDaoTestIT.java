package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfDaoSupported;
import org.opendcs.fixtures.assertions.Waiting;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;

import decodes.db.Database;
import decodes.sql.DbKey;
import decodes.tsdb.CpCompDependsUpdater;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.CompDependsNotifyDAO;

@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "${DCSTOOL_HOME}/schema/cwms/cwms-import.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
public class CompDependsDaoTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb db;

    @ConfiguredField
    private Database decodesDb;

    @Test
    @EnableIfDaoSupported(CompDependsDAO.class)
    public void test_compdepends_operations() throws Exception
    {
        try(CompDependsDAO cd = (CompDependsDAO)db.makeCompDependsDAO();
            CompDependsNotifyDAO cdnDao = (CompDependsNotifyDAO)db.makeCompDependsNotifyDAO();
            TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
            ComputationDAI compDAI = db.makeComputationDAO();)
        {
            cd.clearScratchpad();
            cd.doModify("delete from cp_depends_notify", new Object[0]);
            cd.doModify("delete from cp_comp_depends", new Object[0]);
            assertTrue(cd.getResults("select * from cp_comp_depends",
                                     rs -> rs.getLong(1)).isEmpty());
            TimeSeriesIdentifier tsIdInput = db.makeEmptyTsId();
            TimeSeriesIdentifier tsIdOutput = db.makeEmptyTsId();
            tsIdInput.setUniqueString("TESTSITE1.Stage.Inst.15Minutes.0.raw");
            tsIdOutput.setUniqueString("TESTSITE1.Flow.Inst.15Minutes.0.raw");
            DbKey inputKey = tsDAI.createTimeSeries(tsIdInput);
            DbKey outputKey = tsDAI.createTimeSeries(tsIdOutput);

            DbComputation comp = new DbComputation(DbKey.NullKey, "stage_flow_tmp");
            DbCompParm input = new DbCompParm("indep", inputKey, tsIdInput.getInterval(), tsIdInput.getTableSelector(), 0);
            DbCompParm output = new DbCompParm("dep", outputKey, tsIdOutput.getInterval(), tsIdOutput.getTableSelector(), 0);
            comp.addParm(input);
            comp.addParm(output);
            comp.setAlgorithmName("TabRating");
            comp.setApplicationName("compproc_regtest");
            comp.setEnabled(true);

            compDAI.writeComputation(comp);
            final DbComputation compInDb = compDAI.getComputationByName("stage_flow_tmp");
            try(BackgroundTsDbApp<?> app =
                    BackgroundTsDbApp.forApp(CpCompDependsUpdater.class,
                                            "compdepends",
                                            configuration.getPropertiesFile(),
                                            new File(configuration.getUserDir(),"cdn-test.log"),
                                            environment);)
            {
                assertTrue(app.isRunning(), "App did not start correctly.");
                Waiting.assertResultWithinTimeFrame(
                        timeMs -> !cd.getResults("select * from cp_comp_depends where computation_id=?",
                                                 rs -> rs.getLong(1),
                                                 compInDb.getKey()
                                                )
                                     .isEmpty(),
                        2, TimeUnit.MINUTES,
                        5, TimeUnit.SECONDS,
                        "Expected values were not found in the comp depends table."
                );
            }
        }
    }
}
