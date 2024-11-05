/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/24 23:52:44  mjmaloney
*  Added javadocs.
*
*  Revision 1.1  2001/08/24 19:31:41  mike
*  Moved PMParser stuff to datasource package.
*  Added reference in RawMessage to performance measurements.
*  Created FileDataSource.
*
*/
package decodes.datasource;

/**
This exception indicates a run-time error in parsing the message header
to obtain required performance measurements. Failure to parse performance 
measurements that are not required should NOT throw an exception but should
fail silently.
*/
public class HeaderParseException extends DataSourceException
{
	/**
	Construct exception with message.
	@param msg the message
	*/
	public HeaderParseException(String msg)
	{
		super(msg);
	}
}

