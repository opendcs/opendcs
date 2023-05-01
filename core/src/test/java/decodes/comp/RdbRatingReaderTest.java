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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 Unit test for RdbRatingReader
 */
class RdbRatingReaderTest
{
	private static final Logger LOGGER = Logger.getLogger(RdbRatingReaderTest.class.getName());

	@ParameterizedTest
	@CsvSource({"decodes/comp/BMD.rdb", "decodes/comp/MUGT1-WFkStonesR-MurfreesboroTN.rdb"})
	void testAreaRatingReader(String filename) throws TableBoundsException
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
}
