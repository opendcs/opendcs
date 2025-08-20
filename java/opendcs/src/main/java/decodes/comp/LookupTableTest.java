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

/** Stand-alone test class for LookupTable */
public class LookupTableTest
{
	/** 
	  Do the test 
	  @param args command line args.
	  @throws TableBoundsException if lookup error.
	*/
	public static void main(String args[])
		throws Exception
	{
		File f = new File(args[0]);
		TabRatingReader rr = new TabRatingReader(f.getPath());
		LookupTable.debug = true;
		LookupTable lt = new LookupTable();
		RatingComputation rc = new RatingComputation(rr);
		rc.setProperty("DepName", "output");
		rc.setApplyShifts(false);
		rc.read();
		double low = Double.parseDouble(args[1]);
		double high = Double.parseDouble(args[2]);
		while(low <= high)
		{
			double out = rc.getLookupTable().lookup(low);
			System.out.println("" + low + ", " + out);
			low += .01;
		}
	}
}
