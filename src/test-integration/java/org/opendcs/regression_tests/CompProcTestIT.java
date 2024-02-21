package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.TsdbAppRequired;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;
import org.opendcs.fixtures.helpers.Programs;
import org.opendcs.spi.configuration.Configuration;

import decodes.tsdb.ComputationApp;
import decodes.tsdb.CpCompDependsUpdater;
import decodes.tsdb.TimeSeriesDb;

/**
 * CompProcTestIT tests importing time-series data.
 */
@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "${DCSTOOL_HOME}/schema/cwms/cwms-import.xml",
        "shared/presgrp-regtest.xml"
    })
@ComputationConfigurationRequired("shared/loading-apps.xml")
public class CompProcTestIT extends AppTestBase
{
    private static final Logger log = Logger.getLogger(CompProcTestIT.class.getName());
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
    public void load_sites() throws Exception
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
    public void cleanup_timeseries() throws Exception
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
    public void test_incremental_precip() throws Exception
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
            BackgroundTsDbApp.waitForResult((t) ->
            {
                final String output = Programs.OutputTs(new File(logDir,"/outputTs.log"), config.getPropertiesFile(),
                                          environment, exit,
                                          "01-Jan-2012/00:00", "03-Jan-2012/00:00", "UTC",
                                          "regtest", tsids);
                return golden.equals(output);
            }, 1, TimeUnit.MINUTES, 15, TimeUnit.SECONDS);
        }
        catch(InterruptedException ex)
        {
            /* do nothing */
        }
    }
}
