/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/24 23:52:43  mjmaloney
*  Added javadocs.
*
*  Revision 1.1  2001/07/01 18:48:37  mike
*  Moved data source stuff to separate 'datasource' package.
*
*/
package decodes.datasource;

/**
Thrown when a data source times out. That is, it fails to return a RawMessage
within a specified length of time.
*/
public class DataSourceTimeoutException extends DataSourceException
{
	/**
	  Construct with message.
	  @param msg the message
	*/
	public DataSourceTimeoutException(String msg)
	{
		super(msg);
	}
}

