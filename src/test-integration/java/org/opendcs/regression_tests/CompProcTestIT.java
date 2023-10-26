package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.ConfiguredField;
import org.opendcs.fixtures.EnableIfSql;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.spi.configuration.Configuration;

import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;

public class CompProcTestIT extends AppTestBase
{
    private static final Logger log = Logger.getLogger(CompProcTestIT.class.getName());
    @ConfiguredField
    protected TimeSeriesDb db;

    private static BackgroundTsDbApp<ComputationApp> compProc = null;

    private static final String appName = "compproc_regtest";

    final static String tsids[] = new String[]{"TESTSITE1.Precip-Cum.Inst.1Hour.0.test(TESTSITE1-PC)",
                                "TESTSITE1.Precip-Incr.Total.1Hour.1Hour.test(TESTSITE1-PR-Hr)",
                                "TESTSITE1.Precip-Incr.Total.~1Day.1Day.test(TESTSITE1-PR-Day)",
                                "TESTSITE1.Precip-Incr.Total.~1Month.1Month.test(TESTSITE1-PR-Mon)",
                                "TESTSITE2.Precip-Cum.Inst.1Hour.0.test(TESTSITE2-PC)",
                                "TESTSITE2.Precip-Incr.Total.1Hour.1Hour.test(TESTSITE2-PR-Hr)",
                                "TESTSITE2.Precip-Incr.Total.~1Day.1Day.test(TESTSITE2-PR-Day)",
                                "TESTSITE2.Precip-Incr.Total.~1Month.1Month.test(TESTSITE2-PR-Mon)" };

    @BeforeAll
    public void load_sites() throws Exception
    {
        log.info("Importing shared data.");
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();
        File logFile = new File(config.getUserDir(),"/compproc-setup.log");
        Programs.DbImport(logFile, propertiesFile, environment, exit,
                                getResource("shared/test-sites.xml"),
                                new File(config.getUserDir(),"/schema/cwms/cwms-import.xml").getAbsolutePath(),
                                getResource("shared/presgrp-regtest.xml"));
        Programs.CompImport(logFile, config.getPropertiesFile(), environment, exit, getResource("CompProc/Precip/comps.xml"));
        FileUtils.copyFile(new File(getResource("/CompProc/Precip/datchk.cfg")),new File(config.getUserDir(),"/datchk.cfg"));
        FileUtils.copyFile(new File(getResource("/CompProc/Precip/pathmap.txt")),new File(config.getUserDir(),"/pathmap.txt"));
        FileUtils.copyFile(new File(getResource("/CompProc/Precip/test.datchk")),new File(config.getUserDir(),"/test.datchk"));
        try(LoadingAppDAI loadingDao = db.makeLoadingAppDAO())
        {
            try
            {
                CompAppInfo cai = loadingDao.getComputationApp(appName);
            }
            catch (NoSuchObjectException ex)
            {
                fail("Computation app for testing is not setup.");
            }
        }

        if (compProc == null)
        {
            compProc = BackgroundTsDbApp.forApp(
                ComputationApp.class,appName,propertiesFile,
                            new File(config.getUserDir(),"/compproc-run.log"),environment);
        }
        assertTrue(compProc.isRunning(), "Required Application did not start correctly");
        // TODO: Create system to register timeseries so this doesn't need to be imported twice.
        Programs.ImportTs(new File(config.getUserDir(),"/importTs.log"), propertiesFile,
                          environment, exit, getResource("CompProc/Precip/input.tsimport"));
        Programs.UpdateComputationDependencies(new File(config.getUserDir(),"/update-deps.log"), propertiesFile, environment, exit);
        Programs.ImportTs(new File(config.getUserDir(),"/importTs.log"), propertiesFile,
                          environment, exit, getResource("CompProc/Precip/input.tsimport"));
    }

    @BeforeEach
    public void check_app() throws Exception
    {
        assertTrue(compProc.isRunning(), "Required Application failed to stay running.");
    }


    @AfterAll
    public void stop_compProc() throws Exception
    {
        if (compProc != null)
        {
            compProc.stop();
        }
    }

    @AfterAll
    public void cleanup_timeseries() throws Exception
    {
        Programs.DeleteTs(new File(configuration.getUserDir(),"/deleteTs.log"), 
                          configuration.getPropertiesFile(),
                          environment,
                          exit,
                          "01-Jan-2012/00:00",
                          "03-Jan-2012/00:00",
                          "UTC",
                          tsids);
    }

    @Test
    @EnableIfSql
    public void test_incremental_precip() throws Exception
    {
        

        Configuration config = this.configuration;
        File logDir = config.getUserDir();

        File goldenFile = new File(getResource("CompProc/Precip/output.human-readable"));

        try
        {
            Thread.sleep(15000); // TODO: eliminate wait
        }
        catch (InterruptedException ex)
        {
            /* do nothing */
        }
        String output = Programs.OutputTs(new File(logDir,"/outputTs.log"), config.getPropertiesFile(), 
                                          environment, exit,
                                          "01-Jan-2012/00:00", "03-Jan-2012/00:00", "UTC",
                                          "regtest", tsids);
        String golden = IOUtils.toString(goldenFile.toURI().toURL().openStream(), "UTF8");
        assertEquals(golden,output,"Output Doesn't match expected data.");
    }
}
