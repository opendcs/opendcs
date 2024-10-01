/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/24 23:52:46  mjmaloney
*  Added javadocs.
*
*  Revision 1.1  2001/07/24 13:22:40  mike
*  LrgsDataSource development.
*
*/
package decodes.datasource;

/**
  Throws by RawMessage when attempting to decode a message for which
  we don't have either a Platform or TransportMedium record.
*/
public class UnknownPlatformException extends DataSourceException
{
	/**
	  Construct exception with message.
	  @param msg the message
	*/
	public UnknownPlatformException(String msg)
	{
		super(msg);
	}
}

