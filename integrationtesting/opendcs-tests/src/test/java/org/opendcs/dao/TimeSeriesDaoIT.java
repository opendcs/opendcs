package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;

import java.io.InputStream;
import java.util.Collection;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.utils.FailableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.TsdbException;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

@DecodesConfigurationRequired({
    "shared/test-sites.xml"})
class TimeSeriesDaoIT extends AppTestBase
{
    private final Logger log = LoggerFactory.getLogger(TimeSeriesDaoIT.class);

    @ConfiguredField
    private TimeSeriesDb tsDb;

    /**
     * This just tests that timeseries can be saved to the database, retrieved, and totally deleted.
     * @param inputFile The input file containing the timeseries data to import.
     * @throws Exception If there is an error importing the timeseries data.
     */
    @ParameterizedTest
    @CsvSource({
        "timeseries/${impl}/regular_ts.tsimport"
    })
    @EnableIfTsDb
    void test_timeseries_operations(String inputFile) throws Exception
    {
        TsImporter importer = new TsImporter(TimeZone.getTimeZone("UTC"), null, tsIdStr ->
        {
            try
            {
                return tsDb.makeTsId(tsIdStr);
            }
            catch (BadTimeSeriesException ex)
            {
                throw new DbIoException("Unable to create TimeSeriesIdentifier from: " + tsIdStr, ex);
            }
        });
        try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
            InputStream inputStream = TestResources.getResourceAsStream(configuration, inputFile))
        {
            Collection<CTimeSeries> allTs = importer.readTimeSeriesFile(inputStream);
            for (CTimeSeries tsIn: allTs)
            {
                log.info("Saving {}", tsIn.getTimeSeriesIdentifier());
                tsDao.saveTimeSeries(tsIn);
                // Retrieve the TimeSeriesIdentifier that shouldn't been saved to the database.
                // This will also fill in required metadata so that the retrieval operations are handled correctly.
                FailableResult<TimeSeriesIdentifier, TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
                assertTrue(tsIdSavedResult.isSuccess(), "Time series was not correctly saved.");
                final TimeSeriesIdentifier tsIdSaved = tsIdSavedResult.getSuccess();
                final CTimeSeries result = tsDao.makeTimeSeries(tsIdSaved);
                result.setUnitsAbbr(tsIn.getUnitsAbbr());
                log.info("Created CTimeseries {}", result);
                tsDao.fillTimeSeries(result,
                        tsIn.sampleAt(0).getTime(),
                        tsIn.sampleAt(tsIn.size() - 1).getTime(),
                        true, true, false);
                log.info("Data loaded.");
                assertEquals(tsIn, result, "Timeseries round trip did not work.");
                tsDao.deleteTimeSeries(tsIn.getTimeSeriesIdentifier()); // This will also delete the timeseries identifier.
                final CTimeSeries result2 = tsDao.makeTimeSeries(tsIn.getTimeSeriesIdentifier());
                assertThrows(BadTimeSeriesException.class, () -> tsDao.fillTimeSeries(result2,
                         tsIn.sampleAt(0).getTime(),
                         tsIn.sampleAt(tsIn.size()-1).getTime(),
                          true, true, false));
            }
        }
    }

    @Test
    @EnableIfTsDb({"CWMS-Oracle", "OpenDCS-Postgres", "OpenDCS-Oracle"})
    void test_timeseries_validation() throws Exception {
        try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
            SiteDAI siteDao = tsDb.makeSiteDAO())
        {
            final TimeSeriesIdentifier tsId = tsDb.makeTsId("Paria R at Lees Ferry.Flow.Inst.15Minutes.0.Rev-SPL-USGS");
            siteDao.writeSite(tsId.getSite());
            final DbKey tsIdKey = tsDao.createTimeSeries(tsId);
            final TimeSeriesIdentifier tsIdOut = tsDao.findTimeSeriesIdentifier(tsIdKey).getSuccess();
            assertEquals(tsId, tsIdOut, "saved timeseries identifier does not match retrieved result.");
        }
    }
}
