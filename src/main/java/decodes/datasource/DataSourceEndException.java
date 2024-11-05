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
*  Revision 1.1  2001/09/14 21:17:37  mike
*  dev
*
*/
package decodes.datasource;

/**
Special exception to indicate that the data source has reached its logical
end. This could be an LRGS reaching the specified 'until' time, or a file
reaching EOF.
*/
public class DataSourceEndException extends DataSourceException
{
	/**
	  Construct exception with specified message.
	  @param msg the message.
	*/
	public DataSourceEndException(String msg)
	{
		super(msg);
	}
}

