/*
*  $Id$
*/
package decodes.tsdb.xml;

import decodes.tsdb.TsdbException;

/**
Thrown when an XML IO error occurs when reading or writing an
XML file.
*/
public class DbXmlException extends TsdbException
{
	/**
	 * Constructor.
	 * @param msg explanatory message.
	 */
	public DbXmlException(String msg)
	{
		super(msg);
	}

	/**
	 * DbXmlException with additional cause.
	 * @param msg
	 * @param cause
	 */
	public DbXmlException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}
