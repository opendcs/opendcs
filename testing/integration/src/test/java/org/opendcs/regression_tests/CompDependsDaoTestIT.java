package org.opendcs.regression_tests;

<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
import static org.junit.jupiter.api.Assertions.assertFalse;
=======
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
import org.junit.jupiter.api.AfterAll;
=======
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfDaoSupported;
<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;

import decodes.cwms.CwmsTsId;
=======
import org.opendcs.fixtures.assertions.Waiting;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;

>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
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
<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
@ComputationConfigurationRequired({"shared/loading-apps.xml","${DCSTOOL_HOME}/imports/comp-standard/algorithms.xml"})
=======
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
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
<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
            /**
             * While this shouldn't be required, during testing it appears that something
             * causes the DbIo instance to become null. The good news is that I'm pretty sure
             * it's an artifact of just how many times Database:setDb gets called during a test run
             * and not an issue with general operation. Further investigate is required.
             */
            //Database.setDb(decodesDb);
=======
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
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

<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
            DbComputation comp = new DbComputation(DbKey.NullKey, "stage_flow");
=======
            DbComputation comp = new DbComputation(DbKey.NullKey, "stage_flow_tmp");
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
            DbCompParm input = new DbCompParm("indep", inputKey, tsIdInput.getInterval(), tsIdInput.getTableSelector(), 0);
            DbCompParm output = new DbCompParm("dep", outputKey, tsIdOutput.getInterval(), tsIdOutput.getTableSelector(), 0);
            comp.addParm(input);
            comp.addParm(output);
            comp.setAlgorithmName("TabRating");
            comp.setApplicationName("compproc_regtest");
            comp.setEnabled(true);

            compDAI.writeComputation(comp);
<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java

=======
            final DbComputation compInDb = compDAI.getComputationByName("stage_flow_tmp");
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
            try(BackgroundTsDbApp<?> app =
                    BackgroundTsDbApp.forApp(CpCompDependsUpdater.class,
                                            "compdepends",
                                            configuration.getPropertiesFile(),
                                            new File(configuration.getUserDir(),"cdn-test.log"),
                                            environment);)
            {
<<<<<<< HEAD:testing/integration/src/test/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
                assertTrue(
                    BackgroundTsDbApp.waitForResult(
                        timeMs -> !cd.getResults("select * from cp_comp_depends", rs -> rs.getLong(1))
                                     .isEmpty(),
                        2, TimeUnit.MINUTES,
                        5, TimeUnit.SECONDS)
=======
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
>>>>>>> master:src/test-integration/java/org/opendcs/regression_tests/CompDependsDaoTestIT.java
                );
            }
        }
    }
}
