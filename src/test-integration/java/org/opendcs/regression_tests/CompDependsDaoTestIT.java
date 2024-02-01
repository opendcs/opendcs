package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfDaoSupported;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;

import decodes.cwms.CwmsTsId;
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
@ComputationConfigurationRequired({"shared/loading-apps.xml","${DCSTOOL_HOME}/imports/comp-standard/algorithms.xml"})
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
            /**
             * While this shouldn't be required, during testing it appears that something
             * causes the DbIo instance to become null. The good news is that I'm pretty sure
             * it's an artifact of just how many times Database:setDb gets called during a test run
             * and not an issue with general operation. Further investigate is required.
             */
            //Database.setDb(decodesDb);
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

            DbComputation comp = new DbComputation(DbKey.NullKey, "stage_flow");
            DbCompParm input = new DbCompParm("indep", inputKey, tsIdInput.getInterval(), tsIdInput.getTableSelector(), 0);
            DbCompParm output = new DbCompParm("dep", outputKey, tsIdOutput.getInterval(), tsIdOutput.getTableSelector(), 0);
            comp.addParm(input);
            comp.addParm(output);
            comp.setAlgorithmName("TabRating");
            comp.setApplicationName("compproc_regtest");
            comp.setEnabled(true);

            compDAI.writeComputation(comp);

            try(BackgroundTsDbApp<?> app =
                    BackgroundTsDbApp.forApp(CpCompDependsUpdater.class,
                                            "compdepends",
                                            configuration.getPropertiesFile(),
                                            new File(configuration.getUserDir(),"cdn-test.log"),
                                            environment);)
            {
                assertTrue(
                    BackgroundTsDbApp.waitForResult(
                        timeMs -> !cd.getResults("select * from cp_comp_depends", rs -> rs.getLong(1))
                                     .isEmpty(),
                        2, TimeUnit.MINUTES,
                        5, TimeUnit.SECONDS)
                );
            }
        }
    }
}
