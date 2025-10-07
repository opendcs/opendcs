/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package ilex.xml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
* Log error messages to the current Logger.instance().
*/
public class LoggerErrorHandler implements ErrorHandler
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		String msg = e.getPublicId() + ": " + e.getLineNumber()
			+ " " + e.getMessage();
		log.warn(msg);
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
		log.error(msg);

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
		log.error(msg);
		throw new SAXException("SAX Fatal: " + msg);
	}
}