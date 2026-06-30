package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.File;
import java.sql.Connection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.TsdbAppRequired;
import org.opendcs.fixtures.assertions.Waiting;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.spi.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.CpCompDependsUpdater;
import decodes.tsdb.DataCollection;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

/**
 * CompProcTestIT tests importing time-series data.
 */
@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/presgrp-regtest.xml"
    })
@ComputationConfigurationRequired("shared/loading-apps.xml")
class CompProcTestIT extends AppTestBase
{
    private static final Logger log = LoggerFactory.getLogger(CompProcTestIT.class);

    @ConfiguredField
    protected TimeSeriesDb db;

    /* there is insufficient test data for the day and month calculations? */
    final static String tsids[] = new String[]{"TESTSITE1.Precip-Cum.Inst.1Hour.0.test(TESTSITE1-PC)",
                                "TESTSITE1.Precip-Incr.Total.1Hour.1Hour.test(TESTSITE1-PR-Hr)",
                                //"TESTSITE1.Precip-Incr.Total.~1Day.1Day.test(TESTSITE1-PR-Day)",
                                //"TESTSITE1.Precip-Incr.Total.~1Month.1Month.test(TESTSITE1-PR-Mon)",
                                "TESTSITE2.Precip-Cum.Inst.1Hour.0.test(TESTSITE2-PC)",
                                "TESTSITE2.Precip-Incr.Total.1Hour.1Hour.test(TESTSITE2-PR-Hr)"};
                                //"TESTSITE2.Precip-Incr.Total.~1Day.1Day.test(TESTSITE2-PR-Day)",
                                //"TESTSITE2.Precip-Incr.Total.~1Month.1Month.test(TESTSITE2-PR-Mon)" };

    @BeforeAll
    void load_sites() throws Exception
    {
        if (!configuration.isTsdb())
        {
            return;
        }
        log.info("Importing shared data.");
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();
        FileUtils.copyFile(new File(getResource(config, "/CompProc/Precip/datchk.cfg")), new File(config.getUserDir(),"/datchk.cfg"));
        FileUtils.copyFile(new File(getResource(config, "/CompProc/Precip/pathmap.txt")), new File(config.getUserDir(),"/pathmap.txt"));
        FileUtils.copyFile(new File(getResource(config, "/CompProc/Precip/test.datchk")), new File(config.getUserDir(),"/test.datchk"));

        // TODO: Create system to register timeseries so this doesn't need to be imported twice.
        Programs.ImportTs(new File(config.getUserDir(),"/importTs.log"), propertiesFile,
                          environment, exit, getResource(config,"CompProc/Precip/input.tsimport"));
        Programs.UpdateComputationDependencies(new File(config.getUserDir(),"/update-deps.log"), propertiesFile, environment, exit);
        Programs.ImportTs(new File(config.getUserDir(),"/importTs.log"), propertiesFile,
                          environment, exit, getResource(config, "CompProc/Precip/input.tsimport"));
    }

    @AfterAll
    void cleanup_timeseries() throws Exception
    {
        if (!configuration.isTsdb())
        {
            return;
        }
        Programs.DeleteTs(new File(configuration.getUserDir(),"/deleteTs.log"),
                          configuration.getPropertiesFile(),
                          environment,
                          exit,
                          "01-Jan-2012/00:00",
                          "03-Jan-2012/00:00",
                          "UTC",
                          tsids);
    }

    /* NOTE: this should be a chain of 2 comps, the datchk one isn't getting executed,
     * may be because current test environment is Postgres not CWMS. However, purpose
     * was for even getting a single compproc test going thus I'm going to accept that
     * for now and improve in a later PR.
     * Current work is from test_012 in the provided discussion at
     * https://github.com/opendcs/opendcs/discussions/348
     */
    @Test
    @TsdbAppRequired(app = ComputationApp.class, appName="compproc_regtest")
    @ComputationConfigurationRequired({"shared/loading-apps.xml", "CompProc/Precip/comps.xml"})
    void test_incremental_precip() throws Exception
    {
        final Configuration config = this.configuration;
        final File logDir = config.getUserDir();

        final File goldenFile = new File(getResource(config, "CompProc/Precip/output.human-readable"));

        BackgroundTsDbApp.waitForApp(CpCompDependsUpdater.class,
                                            "compdepends_compproc",
                                            configuration.getPropertiesFile(),
                                            new File(configuration.getUserDir(),"compproctest-deps-update.log"),
                                            environment, 60, TimeUnit.SECONDS, "-O");
        Programs.ImportTs(
            new File(config.getUserDir(),"/importTs.log"),
            config.getPropertiesFile(),
            environment, exit,
            getResource(config, "CompProc/Precip/input.tsimport"));
        try
        {
            final String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
            Waiting.assertResultWithinTimeFrame((t) ->
            {
                try
                {
                    final String output = Programs.OutputTs(new File(logDir,"/outputTs.log"), config.getPropertiesFile(),
                                            environment, exit,
                                            "01-Jan-2012/00:00", "03-Jan-2012/00:00", "UTC",
                                            "regtest", tsids);
                    if (log.isTraceEnabled())
                    {
                        log.trace("Current Output TimeSeries: {}{}", System.lineSeparator(), output);
                    }
                    return golden.equals(output);
                }
                catch (Throwable ex)
                {
                    log.error("Test application had an unexpected error", ex);
                    return false;
                }
            }, 3, TimeUnit.MINUTES, 15, TimeUnit.SECONDS
            ,"Calculated results were not found within the expected time frame.");
        }
        catch(InterruptedException ex)
        {
            /* do nothing */
        }
    }

    @Test
    @TsdbAppRequired(app = ComputationApp.class, appName="compproc_regtest")
    @ComputationConfigurationRequired({"shared/loading-apps.xml", "CompProc/Precip/comps.xml"})
    @Disabled("The tasklist table is not mapped to DAO that make this easy to run cross implementation. Remove the sourceid not null constraint on cp_comp_tasklist"
            + " and you can run it with OpenDCS Postgres implementation. Future work will enable this test.")
    void test_bad_recs_cleared() throws Exception
    {
        /**
         * 1. insert into tasklist records with 0 computations enabled.
         * 2. wait for compproc to remove them.
         * 
         * success condition, tasklist entries are removed.
         */
        try(TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
            LoadingAppDAI laDAI = db.makeLoadingAppDAO();
            DaoBase dao = new DaoBase(db,"test");
            Connection c = db.getConnection();)
        {
            KeyGenerator keyGen = db.getKeyGenerator();
            DbKey appKey = laDAI.lookupAppId("compproc_regtest");
            TimeSeriesIdentifier tsIdInput = db.makeTsId("TESTSITE1.Stage.Inst.15Minutes.0.raw-nocomps");
            DbKey inputKey = tsDAI.createTimeSeries(tsIdInput);

            dao.doModify("insert into cp_comp_tasklist(record_num, loading_application_id, ts_id, num_value, date_time_loaded, sample_time, flags) "
                       + "values (?,?,?,?,?,?,?)",keyGen.getKey("cp_comp_tasklist", c), appKey, inputKey, 1.0,new Date(), new Date(),0);

            DataCollection data = tsDAI.getNewData(appKey, 20000, 0); // arbitrary number, previously the default.
            assertTrue(data.isEmpty());
            assertEquals(0,
                         dao.getSingleResultOr("select count(ts_id) from cp_comp_tasklist where ts_id=?",
                                             rs -> rs.getInt(1), -1,
                                             inputKey),
                         "Bad records were left in the tasklist.");
        }
    }

    /**
     * This is an attempt to test an issue with a data gap seen at LRL in USACE's cloud environment.
     * Cause is not known at time of initial setup. Here we attempt to partially recreate conditions.
     * @throws Exception
     */
    @Test
    @TsdbAppRequired(app = ComputationApp.class, appName="compproc_regtest")
    @DecodesConfigurationRequired("CompProc/ScalerAdder/sites.xml")
    @ComputationConfigurationRequired({"shared/loading-apps.xml", "CompProc/ScalerAdder/comps.xml"})
    void test_scaleradder_three_inputs() throws Exception
    {

        final String outputTsIds[] = new String[]
        {
            "Barren.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Barren)",
            "Green.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Green)",
            "Nolin.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Nolin)",
            "Rough.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Rough)",
            "CJBRown.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(CJBrown)",
            "CaesarCreek.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(CaesarCreek)",
            "WHHarsa.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(WHHarsa)",
            "CarrCreek.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(CarrCreek)",
            "Buckhorn.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Buckhorn)",
            "CaveRun.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(CaveRun)",
            "CMHarden.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(CMHarden)",
            "Monroe.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Monroe)",
            "Patoka.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Patoka)",
            "Brookville.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Brookville)",
            "Mississinewa.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Mississinewa)",
            "Salamonie.Flow-Outflow.Inst.1Hour.0.lrldlb-comp(Salamonie)"
        };

        final Configuration config = this.configuration;
        final File logDir = config.getUserDir();

        final File goldenFile = new File(getResource(config, "CompProc/ScalerAdder/output.human-readable"));

        Programs.ImportTs(
            new File(config.getUserDir(),"/importTs.log"),
            config.getPropertiesFile(),
            environment, exit,
            getResource(config, "CompProc/ScalerAdder/input.tsimport"));

        BackgroundTsDbApp.waitForApp(CpCompDependsUpdater.class,
                                     "compdepends_compproc",
                                     configuration.getPropertiesFile(),
                                     new File(configuration.getUserDir(),"compproctest-adder-update.log"),
                                     environment, 60, TimeUnit.SECONDS, "-O");

        // First import is to create time series so compdepends can be updated to use them.
        // This import is to actually trigger the computation.
        Programs.ImportTs(
            new File(config.getUserDir(),"/importTs.log"),
            config.getPropertiesFile(),
            environment, exit,
            getResource(config, "CompProc/ScalerAdder/input.tsimport"));

        try
        {
            final String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
            Waiting.assertResultWithinTimeFrame((t) ->
            {
                try
                {
                    final String output = Programs.OutputTs(new File(logDir,"/outputTs.log"), config.getPropertiesFile(),
                                            environment, exit,
                                            "07-June-2026/00:00", "08-Jun-2026/00:00", "UTC",
                                            "regtest", outputTsIds);
                    if (log.isTraceEnabled())
                    {
                        log.trace("Current Output TimeSeries: {}{}", System.lineSeparator(), output);
                    }
                    return golden.equals(output);
                }
                catch (Throwable ex)
                {
                    log.error("Test application had an unexpected error", ex);
                    return false;
                }
            }, 3, TimeUnit.MINUTES, 5, TimeUnit.SECONDS
            ,"Calculated results were not found within the expected time frame.");
        }
        catch(InterruptedException ex)
        {
            /* do nothing */
        }
    }
}
