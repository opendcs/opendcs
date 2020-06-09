/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/24 23:52:42  mjmaloney
*  Added javadocs.
*
*  Revision 1.1  2001/07/01 18:48:37  mike
*  Moved data source stuff to separate 'datasource' package.
*
*/
package decodes.datasource;

import decodes.util.DecodesException;

/**
This exception is thrown when a data source encounters an unrecoverable
error and should subsequently be closed.
*/
public class DataSourceException extends DecodesException
{
	/**
	  Construct exception with message.
	  @param msg the message
	*/
	public DataSourceException(String msg)
	{
		super(msg);
	}
}

