/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 14:50:38  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/04/13 15:46:46  mike
*  Return string buffer, not string.
*
*  Revision 1.2  2001/04/11 22:44:20  mike
*  Added StringBufferErrorHandler.
*
*  Revision 1.1  2001/04/09 20:46:49  mike
*  Created StringBufferErrorHandler
*
*/
package ilex.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.PrintStream;

/**
* Stores formatted error messages in a string buffer. This is useful
* for later display in a GUI dialog.
*/
public class StringBufferErrorHandler implements ErrorHandler
{
	private StringBuffer sb;
	private String linesep;
	private int numErrors;
	private boolean _stopOnWarnings;
	private boolean _stopOnErrors;

	public StringBufferErrorHandler( )
	{
		sb = new StringBuffer();
		linesep = System.getProperty("line.separator", "\n");
		clear();
		_stopOnWarnings = true;
		_stopOnErrors = true;
	 }

	
	/**
	* @param tf true if you want warnings to halt the parser.
	*/
	public void stopOnWarnings( boolean tf ) { _stopOnWarnings = tf; }

	/**
	* @param tf true if you want errors to halt the parser.
	*/
	public void stopOnErrors( boolean tf ) { _stopOnErrors = tf; }

	/** Clears the StringBuffer. */
	public void clear( )
	{
		sb.setLength(0);
		numErrors = 0;
	}

	/**
	* @return StrinbBuffer.
	*/
	public StringBuffer getErrors( )
	{
		return sb;
	}

	/**
	* @return number of errors
	*/
	public int getNumErrors( )
	{
		return numErrors;
	}

	/**
	* Issue a warning message.
	* @param e the exception
	* @throws SAXException
	*/
	public void warning( SAXParseException e ) throws SAXException
	{
		sb.append("**WARNING** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage() + linesep);
		numErrors++;
		if (_stopOnWarnings)
			throw new SAXException("Warning encountered");
	}

	/**
	* Issue an error message.
	* @param e the exception
	* @throws SAXException
	*/
	public void error( SAXParseException e ) throws SAXException
	{
		sb.append("**ERROR** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage() + linesep);
		numErrors++;
		if (_stopOnErrors)
			throw new SAXException("Error encountered");
	}

	/**
	* Issue a fatal message.
	* @param e the exception
	* @throws SAXException
	*/
	public void fatalError( SAXParseException e ) throws SAXException
	{
		sb.append("**FATAL ERROR** " + e.getSystemId() + ": " + 
			e.getLineNumber() + " " + e.getMessage() + linesep);
		numErrors++;
		throw new SAXException("Fatal error encountered");
	}

	/**
	* Adds a non-SAX related exception to the buffer.
	* @param msg the message
	* @param e the exception
	*/
	public void addException( String msg, Exception e )
	{
		sb.append(msg + ": " + e.toString() + linesep);
	}
}

