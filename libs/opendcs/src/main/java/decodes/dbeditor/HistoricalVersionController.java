/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/09/20 14:18:47  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/05/03 02:14:39  mike
*  dev
*
*  Revision 1.2  2001/05/02 15:17:59  mike
*  Implementing platform edit screen....
*
*/
package decodes.dbeditor;

import java.util.Date;
import decodes.db.DatabaseException;

/**
The HistoricalVersionDialog must have a parent that implements this interface.
This is over-kill. It was originally thought that several types of objects
might need historical versions. However, only Platforms currently implement
this capability.
*/
public interface HistoricalVersionController
{
	/** 
	  Makes the historical version with the specified expiration date.
	  @param expiration the date.
	*/
	public void makeHistoricalVersion(Date expiration)
		throws DatabaseException;

	/** @return entity name */
	public String entityName();
}
