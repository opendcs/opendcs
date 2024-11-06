package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

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

import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.TsdbException;
import opendcs.dai.TimeSeriesDAI;

@DecodesConfigurationRequired({
    "shared/test-sites.xml"})
public class TimeSeriesDaoIT extends AppTestBase
{
    private Logger log = LoggerFactory.getLogger(TimeSeriesDaoIT.class);

    @ConfiguredField
    private TimeSeriesDb tsDb;

    /**
     * This just tests that timeseries can be saved to the database, retrieved, and totally deleted.
     * @param inputFile
     * @throws Exception
     */
    @ParameterizedTest
    @CsvSource({
        "timeseries/${impl}/regular_ts.tsimport"
    })
    @EnableIfTsDb
    public void test_timeseries_operations(String inputFile) throws Exception
    {
        TsImporter importer = new TsImporter(TimeZone.getTimeZone("UTC"), null, (tsIdStr) -> 
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
                FailableResult<TimeSeriesIdentifier,TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
                assertTrue(tsIdSavedResult.isSuccess(), "Time series was not correctly saved.");
                final TimeSeriesIdentifier tsIdSaved = tsIdSavedResult.getSuccess();
                final CTimeSeries result = tsDao.makeTimeSeries(tsIdSaved);
                //result.setUnitsAbbr(tsIn.getUnitsAbbr());
                log.info("Created CTimeseries {}", result);
                tsDao.fillTimeSeries(result,
                                     tsIn.sampleAt(0).getTime(), 
                                     tsIn.sampleAt(tsIn.size()-1).getTime(),
                                      true, true, false);
                log.info("Data loaded.");
                assertEquals(tsIn, result, "Timeseries round trip did not work.");
                tsDao.deleteTimeSeries(tsIn.getTimeSeriesIdentifier());
                final CTimeSeries result2 = tsDao.makeTimeSeries(tsIn.getTimeSeriesIdentifier());
                tsDao.fillTimeSeries(result2,
                                     tsIn.sampleAt(0).getTime(), 
                                     tsIn.sampleAt(tsIn.size()-1).getTime(),
                                      true, true, false);
                assertTrue(result2.size() == 0, "Time series elements were left in the database.");
            }
        }
    }
}
