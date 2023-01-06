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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
Unit test for RdbRatingReader
*/
class RdbRatingReaderTest
{
	@ParameterizedTest
	@EmptySource //TODO: get example files
	void testAreaRatingReader(String filename)
	{
		RdbRatingReader rrr = new RdbRatingReader(filename);
		RatingComputation rc = new RatingComputation(rrr);
		assertDoesNotThrow(rc::read);
		rc.dump();
	}
}
