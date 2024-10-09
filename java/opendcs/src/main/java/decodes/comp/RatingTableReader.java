/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2006/09/14 18:52:45  mmaloney
*  Abstract table-holder HasLookupTable interface.
*
*  Revision 1.4  2004/08/24 14:31:29  mjmaloney
*  Added javadocs
*
*  Revision 1.3  2004/08/11 21:40:58  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:07  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:55  mjmaloney
*  Created.
*
*/
package decodes.comp;

import decodes.comp.RatingComputation;
import decodes.comp.ComputationParseException;

/**
Abstract interface for objects that read a rating table.
*/
public interface RatingTableReader
{
	/**
	  Reads the rating table into the passed computation
	  @param rc the computation containing the table.
	  @throws ComputationParseException if error reading the table.
	*/
	void readRatingTable( HasLookupTable rc ) 
		throws ComputationParseException;
}
