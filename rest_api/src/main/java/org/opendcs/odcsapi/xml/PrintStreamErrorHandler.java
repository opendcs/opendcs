/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*  $Id: PrintStreamErrorHandler.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/xml/PrintStreamErrorHandler.java,v $
*
*  $State: Exp $
*
*  $Log: PrintStreamErrorHandler.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
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
package org.opendcs.odcsapi.xml;

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

