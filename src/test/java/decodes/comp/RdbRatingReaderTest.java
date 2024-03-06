package decodes.comp;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import decodes.cwms.CwmsTsId;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TsImporter;
import ilex.var.TimedVariable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 Unit test for RdbRatingReader
 */
class RdbRatingReaderTest
{
    private static final Logger LOGGER = Logger.getLogger(RdbRatingReaderTest.class.getName());

    @ParameterizedTest
    @CsvSource({"decodes/comp/BMD.rdb", "decodes/comp/MUGT1-WFkStonesR-MurfreesboroTN.rdb"})
    void test_area_rating_reader(String filename) throws Exception
    {
        Path path = Paths.get("src", "test", "resources").resolve(filename).toAbsolutePath();
        RdbRatingReader rrr = new RdbRatingReader(path.toString());
        RatingComputation rc = new RatingComputation(rrr);
        assertDoesNotThrow(rc::read);
        LookupTable lookupTable = rc.getLookupTable();
        assertDoesNotThrow(() -> lookupTable.lookup(5));
        if(LOGGER.isLoggable(Level.FINE))
        {
            rc.dump();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "decodes/comp/ratings/APLO/rating.rdb,decodes/comp/ratings/APLO/input.tsimport,decodes/comp/ratings/APLO/output.tsimport"
    })
    void test_rdb_reader(String ratingFile, String inputFile, String outputFile) throws Exception
    {
        TsImporter importer = new TsImporter(TimeZone.getTimeZone("UTC"),"CWMS", tsIdStr ->
        {
            CwmsTsId tsId = new CwmsTsId();
            tsId.setUniqueString(tsIdStr);
            return tsId;
        });
        RdbRatingReader.class.getClassLoader();
        try (InputStream rating = ClassLoader.getSystemResourceAsStream(ratingFile);
             InputStream input = ClassLoader.getSystemResourceAsStream(inputFile);
             InputStream output = ClassLoader.getSystemResourceAsStream(outputFile);
            )
        {
            assertNotNull(rating, "Could not find rating resource " + ratingFile);
            assertNotNull(input, "Could not find input resource " + inputFile);
            assertNotNull(output, "Could not find output resource "+ outputFile);
            /* it is only expected that there will be one timeseries per each input or output file */
            final CTimeSeries inputTs = importer.readTimeSeriesFile(input).iterator().next();
            final CTimeSeries outputTs = importer.readTimeSeriesFile(output).iterator().next();
            final RdbRatingReader ratingReader = new RdbRatingReader(rating);
            RatingComputation rc = new RatingComputation(ratingReader);
            rc.read();
            LookupTable lt = rc.getLookupTable();
            final CTimeSeries resultTs = new CTimeSeries(outputTs.getTimeSeriesIdentifier());
            for (int i = 0; i < inputTs.size(); i++)
            {
                final TimedVariable tv =  inputTs.sampleAt(i);
                if (tv != null)
                {
                    double rated = lt.lookup(tv.getDoubleValue());
                    final TimedVariable ratedTv = new TimedVariable(tv);
                    ratedTv.setValue(rated);
                    resultTs.addSample(ratedTv);
                }
            }
            assertEquals(outputTs.size(), resultTs.size(), "results TS is missing a value.");
            for(int i = 0; i < outputTs.size(); i++)
            {
                final TimedVariable expected = outputTs.sampleAt(i);
                final TimedVariable result = outputTs.sampleAt(i);
                assertEquals(expected.getTime(), result.getTime(), "Sample dates have been shifted from expectation.");
                assertEquals(expected.getDoubleValue(), result.getDoubleValue(), "Result Sample does not match expected value.");
            }
        }
    }
}
