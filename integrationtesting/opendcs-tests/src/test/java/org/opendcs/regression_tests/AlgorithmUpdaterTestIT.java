package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.opendcs.algorithms.update.AlgorithmUpdater;
import org.opendcs.database.AbstractJdbiOpenDcsDatabaseWrapper;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.cwms.algo.ResEvapAlgo;
import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;

/** Exercises the annotation driven update against the installed OpenDCS schema and DAOs. */
@DecodesConfigurationRequired({"shared/test-sites.xml"})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
@EnableIfTsDb({"OpenDCS-Postgres"})
final class AlgorithmUpdaterTestIT extends AppTestBase
{
    @ConfiguredField
    private TimeSeriesDb tsDb;

    @ConfiguredField
    private OpenDcsDatabase database;

    @Test
    void upgradesAnOldAlgorithmAndComputationWithoutLosingTheirMappings() throws Exception
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String algorithmName = "ResEvapUpgrade" + suffix;
        String computationName = "ResEvapUpgradeComp" + suffix;
        var dataSource = ((AbstractJdbiOpenDcsDatabaseWrapper) database).getDataSource();

        DbCompAlgorithm oldAlgorithm = new DbCompAlgorithm(DbKey.NullKey,
                algorithmName, ResEvapAlgo.class.getName(), "Keep this algorithm comment");
        oldAlgorithm.addParm(new DbAlgoParm("hourlyEvap", "o"));
        oldAlgorithm.addParm(new DbAlgoParm("dailyEvap", "o"));
        oldAlgorithm.setProperty("rating", "Keep this custom value");

        TimeSeriesIdentifier hourly = tsDb.makeEmptyTsId();
        hourly.setUniqueString("TESTSITE1.Stage.Inst.1Hour.0.ResEvapUpgrade" + suffix);
        TimeSeriesIdentifier daily = tsDb.makeEmptyTsId();
        daily.setUniqueString("TESTSITE1.Flow.Inst.1Day.0.ResEvapUpgrade" + suffix);

        DbKey computationId = DbKey.NullKey;
        DbKey hourlyId = DbKey.NullKey;
        DbKey dailyId = DbKey.NullKey;
        try
        {
            try (AlgorithmDAI algorithms = tsDb.makeAlgorithmDAO();
                 TimeSeriesDAI series = tsDb.makeTimeSeriesDAO();
                 ComputationDAI computations = tsDb.makeComputationDAO())
            {
                algorithms.writeAlgorithm(oldAlgorithm);
                hourlyId = series.createTimeSeries(hourly);
                dailyId = series.createTimeSeries(daily);

                DbComputation oldComputation = new DbComputation(DbKey.NullKey, computationName);
                oldComputation.setAlgorithmName(algorithmName);
                oldComputation.setApplicationName("compproc_regtest");
                oldComputation.addParm(new DbCompParm("hourlyEvap", hourlyId,
                        hourly.getInterval(), hourly.getTableSelector(), 3600));
                oldComputation.addParm(new DbCompParm("dailyEvap", dailyId,
                        daily.getInterval(), daily.getTableSelector(), 0));
                oldComputation.setProperty("hourlyEvap_MISSING", "IGNORE");
                computations.writeComputation(oldComputation);
                computationId = oldComputation.getId();
            }

            long algorithmId = oldAlgorithm.getId().getValue();
            AlgorithmUpdater.synchronize(dataSource, true, Set.of(algorithmId));
            try (ComputationDAI computations = tsDb.makeComputationDAO())
            {
                assertNotNull(computations.getComputationById(computationId).getParm("hourlyEvap"),
                        "Preview must leave the old role in the database");
            }

            var first = AlgorithmUpdater.synchronize(dataSource, false, Set.of(algorithmId));
            assertEquals(2, first.get(0).renamedParameters());

            try (AlgorithmDAI algorithms = tsDb.makeAlgorithmDAO();
                 ComputationDAI computations = tsDb.makeComputationDAO())
            {
                DbCompAlgorithm updatedAlgorithm = algorithms.getAlgorithmById(oldAlgorithm.getId());
                DbComputation updatedComputation = computations.getComputationById(computationId);

                assertEquals("Keep this algorithm comment", updatedAlgorithm.getComment());
                assertEquals("Keep this custom value", updatedAlgorithm.getProperty("rating"));
                assertNotNull(updatedAlgorithm.getParm("hourlyEvapRate"));
                assertNotNull(updatedAlgorithm.getParm("dailyEvapDepth"));
                assertNotNull(updatedAlgorithm.getParm("hourlyEvapDepth"));
                assertNull(updatedAlgorithm.getParm("hourlyEvap"));

                DbCompParm hourlyUpdated = updatedComputation.getParm("hourlyEvapRate");
                DbCompParm dailyUpdated = updatedComputation.getParm("dailyEvapDepth");
                assertNotNull(hourlyUpdated);
                assertNotNull(dailyUpdated);
                assertEquals(hourlyId, hourlyUpdated.getSiteDataTypeId());
                assertEquals(dailyId, dailyUpdated.getSiteDataTypeId());
                assertEquals(3600, hourlyUpdated.getDeltaT());
                assertEquals("IGNORE", updatedComputation.getProperty("hourlyEvapRate_MISSING"));
                assertNull(updatedComputation.getParm("hourlyEvap"));
                assertNull(updatedComputation.getParm("hourlyEvapDepth"),
                        "The new output has no time series until a user assigns one");
            }

            var second = AlgorithmUpdater.synchronize(dataSource, false, Set.of(algorithmId));
            assertEquals(0, second.get(0).renamedParameters());
        }
        finally
        {
            try (ComputationDAI computations = tsDb.makeComputationDAO();
                 AlgorithmDAI algorithms = tsDb.makeAlgorithmDAO();
                 TimeSeriesDAI series = tsDb.makeTimeSeriesDAO())
            {
                if (!DbKey.isNull(computationId)) computations.deleteComputation(computationId);
                if (!DbKey.isNull(oldAlgorithm.getId())) algorithms.deleteAlgorithm(oldAlgorithm.getId());
                if (!DbKey.isNull(hourlyId)) series.deleteTimeSeries(hourly);
                if (!DbKey.isNull(dailyId)) series.deleteTimeSeries(daily);
            }
        }
    }
}
