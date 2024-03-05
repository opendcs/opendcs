/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.1  2008/04/04 18:20:59  cvs
 *  Added legacy code to repository
 *
 *  Revision 1.2  2004/08/11 21:40:59  mjmaloney
 *  Improved javadocs
 *
 *  Revision 1.1  2004/06/24 14:29:57  mjmaloney
 *  Created.
 *
 */
package decodes.comp;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import decodes.tsdb.TsImport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
		RdbRatingReader.class.getClassLoader();
		try (InputStream is = ClassLoader.getSystemResourceAsStream(ratingFile))
		{
			assertNotNull(is, "Could not find resource " + ratingFile);
			RdbRatingReader ratingReader = new RdbRatingReader(is);
			RatingComputation rc = new RatingComputation(ratingReader);
			rc.read();
			double result = rc.getLookupTable().lookup(0.0);
			assertNotEquals(0.0, result); // TODO: use the actual input and output data as other tables will be different.
		}
	}
}
