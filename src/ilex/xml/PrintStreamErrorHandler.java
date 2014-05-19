/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:38  mjmaloney
*  Javadocs
*
*  Revision 1.2  2001/04/23 12:51:54  mike
*  Added LoggerErrorHandler.
*
*  Revision 1.1  2001/01/03 02:56:01  mike
*  created
*
*
*/
package ilex.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import java.io.PrintStream;

/**
* Simple class that prints formatted error messages to a print stream.
*/
public class PrintStreamErrorHandler implements ErrorHandler
{
	private PrintStream ps;

	/**
	* Constructor
	* @param ps the print stream
	*/
	public PrintStreamErrorHandler( PrintStream ps )
	{
		this.ps = ps;
	}

	/**
	* Issue a warning message.
	* @param e the exception
	* @throws SAXException
	*/
	public void warning( SAXParseException e ) throws SAXException
	{
		ps.println("**WARNING** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage());
		throw new SAXException("Warning encountered");
	}

	/**
	* Issue an error message.
	* @param e the exception
	* @throws SAXException
	*/
	public void error( SAXParseException e ) throws SAXException
	{
		ps.println("**ERROR** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage());
		throw new SAXException("Error encountered");
	}

	/**
	* Issue a fatal message.
	* @param e the exception
	* @throws SAXException
	*/
	public void fatalError( SAXParseException e ) throws SAXException
	{
		ps.println("**FATAL ERROR** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage());
		System.out.println("**FATAL ERROR** " + e.getSystemId() + ": " + e.getLineNumber()
			+ " " + e.getMessage());
		throw new SAXException("Fatal error encountered");
	}
}

