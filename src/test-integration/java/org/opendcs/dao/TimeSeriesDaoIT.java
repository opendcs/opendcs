package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.InputStream;
import java.util.Collection;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.helpers.TestResources;

import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsImporter;
import opendcs.dai.TimeSeriesDAI;

@DecodesConfigurationRequired({
    "shared/test-sites.xml"})
public class TimeSeriesDaoIT extends AppTestBase
{
    private Logger log = Logger.getLogger(TimeSeriesDaoIT.class.getName());

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
    public void test_event_logger(String inputFile) throws Exception
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
                tsDao.saveTimeSeries(tsIn);
                final CTimeSeries result = tsDao.makeTimeSeries(tsIn.getTimeSeriesIdentifier());
                tsDao.fillTimeSeries(result,
                                     tsIn.sampleAt(0).getTime(), 
                                     tsIn.sampleAt(tsIn.size()-1).getTime(),
                                      true, true, false);
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
