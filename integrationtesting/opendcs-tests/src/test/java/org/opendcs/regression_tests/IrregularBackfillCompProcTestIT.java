package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.annotations.TsdbAppRequired;
import org.opendcs.fixtures.assertions.Waiting;
import org.opendcs.fixtures.helpers.Programs;

import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import ilex.var.TimedVariable;
import opendcs.dai.TimeSeriesDAI;

@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({
    "shared/loading-apps.xml",
    "CompProc/IrregularBackfill/comps.xml"
})
public class IrregularBackfillCompProcTestIT extends AppTestBase
{
    private static final String OUTPUT_TSID =
        "TESTSITE1.Flow.Inst.15Minutes.0.irb-queue-output";

    @ConfiguredField
    private TimeSeriesDb db;

    @Test
    @EnableIfTsDb({"CWMS-Oracle"})
    @TsdbAppRequired(app = ComputationApp.class, appName = "compproc_regtest")
    public void historical_gate_value_triggers_backfill() throws Exception
    {
        File logDir = configuration.getUserDir();
        File propertiesFile = configuration.getPropertiesFile();
        String contextResource = getResource(
            configuration, "CompProc/IrregularBackfill/context.tsimport");

        Programs.ImportTs(
            new File(logDir, "irregular-backfill-context.log"),
            propertiesFile, environment, exit,
            contextResource);

        Programs.UpdateComputationDependencies(
            new File(logDir, "irregular-backfill-dependencies.log"),
            propertiesFile, environment, exit);

        // Replay values now that their time series have computation dependencies.
        Programs.ImportTs(
            new File(logDir, "irregular-backfill-trigger.log"),
            propertiesFile, environment, exit,
            contextResource);

        Waiting.assertResultWithinTimeFrame(
            ignored -> hasExpectedOutputs(11.0, 12.0, 13.0),
            2, TimeUnit.MINUTES,
            5, TimeUnit.SECONDS,
            "Initial context data was not processed before the correction.");

        Programs.DeleteTs(
            new File(logDir, "irregular-backfill-delete-output.log"),
            propertiesFile, environment, exit,
            "01-Jan-2024/00:00", "01-Jan-2024/01:00", "UTC",
            OUTPUT_TSID);

        Programs.ImportTs(
            new File(logDir, "irregular-backfill-correction.log"),
            propertiesFile, environment, exit,
            getResource(configuration, "CompProc/IrregularBackfill/correction.tsimport"));

        Waiting.assertResultWithinTimeFrame(
            ignored -> hasExpectedOutputs(21.0, 22.0, 23.0),
            2, TimeUnit.MINUTES,
            5, TimeUnit.SECONDS,
            "Historical gate correction did not trigger the expected backfill.");
    }

    private boolean hasExpectedOutputs(double first, double second, double third)
    {
        try (TimeSeriesDAI timeSeriesDAO = db.makeTimeSeriesDAO())
        {
            TimeSeriesIdentifier tsid =
                timeSeriesDAO.getTimeSeriesIdentifier(OUTPUT_TSID);
            CTimeSeries output = db.makeTimeSeries(tsid);
            output.setUnitsAbbr("cfs");
            timeSeriesDAO.fillTimeSeries(
                output,
                Date.from(Instant.parse("2024-01-01T00:15:00Z")),
                Date.from(Instant.parse("2024-01-01T00:45:00Z")),
                true, true, false);

            return hasValue(output, "2024-01-01T00:15:00Z", first)
                && hasValue(output, "2024-01-01T00:30:00Z", second)
                && hasValue(output, "2024-01-01T00:45:00Z", third);
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private boolean hasValue(CTimeSeries timeSeries, String timestamp, double expected)
        throws Exception
    {
        TimedVariable value =
            timeSeries.findWithin(Date.from(Instant.parse(timestamp)), 0);
        return value != null && Math.abs(value.getDoubleValue() - expected) < 0.0001;
    }
}
