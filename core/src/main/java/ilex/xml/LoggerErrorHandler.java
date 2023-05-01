/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:37  mjmaloney
*  Javadocs
*
*  Revision 1.2  2004/01/29 16:27:56  mjmaloney
*  XmlHierarchyParser can now set file-name by a delegation Locator.
*
*  Revision 1.1  2001/04/23 12:51:54  mike
*  Added LoggerErrorHandler.
*
*/
package ilex.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ilex.util.Logger;

/**
* Log error messages to the current Logger.instance().
*/
public class LoggerErrorHandler implements ErrorHandler
{
	private boolean _stopOnWarnings;
	private boolean _stopOnErrors;

	/** constructor */
	public LoggerErrorHandler( )
	{
		_stopOnWarnings = true;
		_stopOnErrors = true;
	}

	/**
	* Call with true if you want parsing to stop on a warning.
	* @param tf true/false value
	*/
	public void stopOnWarnings( boolean tf ) { _stopOnWarnings = tf; }

	/**
	* Call with true if you want parsing to stop on an error.
	* @param tf true/false value
	*/
	public void stopOnErrors( boolean tf ) { _stopOnErrors = tf; }

	/**
	* Issue a warning message.
	* @param e the exception
	* @throws SAXException if stop-on-warnings is true.
	*/
	public void warning( SAXParseException e ) throws SAXException
	{
		//String msg = e.getSystemId() + ": " + e.getLineNumber()
		String msg = e.getPublicId() + ": " + e.getLineNumber()
			+ " " + e.getMessage();
		Logger.instance().log(Logger.E_WARNING, msg);
		if (_stopOnWarnings)
			throw new SAXException("SAX Warning: " + msg);
	}

	/**
	* Issue an error message.
	* @param e the exception
	* @throws SAXException if stop-on-errors is true.
	*/
	public void error( SAXParseException e ) throws SAXException
	{
		//String msg = e.getSystemId() + ": " + e.getLineNumber()
		String msg = e.getPublicId() + ": " + e.getLineNumber()
			+ " " + e.getMessage();
		Logger.instance().log(Logger.E_FAILURE, msg);

		if (_stopOnErrors)
			throw new SAXException("SAX Error: " + msg);
	}

	/**
	* Called when fatal error encountered.
	* @param e the exception
	* @throws SAXException rethrows the exception
	*/
	public void fatalError( SAXParseException e ) throws SAXException
	{
		String msg = e.getPublicId() + ": " + e.getLineNumber()
			+ " " + e.getMessage();
		Logger.instance().log(Logger.E_FATAL, msg);
		throw new SAXException("SAX Fatal: " + msg);
	}
}

