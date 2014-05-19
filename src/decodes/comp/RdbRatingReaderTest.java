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

/**
Unit test for RdbRatingReader
*/
public class RdbRatingReaderTest
{
	/** Do the test
	* @param args one arg containing the file name
	*/
	public static void main(String args[])
		throws Exception
	{
		RdbRatingReader rrr = new RdbRatingReader(args[0]);
		RatingComputation rc = new RatingComputation(rrr);
		rc.read();
		rc.dump();
	}
}
