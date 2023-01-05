/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/07/17 15:12:44  mmaloney
*  dev
*
*  Revision 1.3  2004/08/24 14:31:29  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2004/08/11 21:40:58  mjmaloney
*  Improved javadocs
*
*  Revision 1.1  2004/06/24 14:29:54  mjmaloney
*  Created.
*
*/
package decodes.comp;

import java.io.File;
import java.util.logging.Logger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Stand-alone test class for LookupTable */
class LookupTableTest
{
	private static final Logger LOGGER = Logger.getLogger(LookupTableTest.class.getName());
	/** 
	  @throws TableBoundsException if lookup error.
	*/
	@ParameterizedTest
	@EmptySource
	//TODO: get example files
	void testLooupTable(String filename, double startingLow, double high) throws TableBoundsException, ComputationParseException
	{
		File f = new File(filename);
		TabRatingReader rr = new TabRatingReader(f.getPath());
		LookupTable.debug = true;
		RatingComputation rc = new RatingComputation(rr);
		rc.setProperty("DepName", "output");
		rc.setApplyShifts(false);
		assertDoesNotThrow(rc::read);
		LookupTable lookupTable = rc.getLookupTable();
		double low = startingLow;
		while(low <= high)
		{
			double fLow = low;
			double out = assertDoesNotThrow(() -> lookupTable.lookup(fLow));
			LOGGER.info(low + ", " + out);
			low += .01;
		}
	}
}
