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

import org.xml.sax.*;
import java.util.Stack;
import java.util.EmptyStackException;

/**
* XmlHierarchyParser parses an XML document by using a hierarchy of object
* parsers.
* <p>
* Each layer of the object hierarchy is handled by an XmlObjectParser,
* whose job it is to map XML tags to internal variables and lower-level
* objects.
* </p>
* <p>
* Typically, parsing an XML file will be handled by one of these objects,
* which contains a Stack of XmlObjectParsers.  At any instant, this stack
* corresponds to the particular point in the XML element hierarchy.that
* parsing is currently being done.  The SAX event methods are delegated
* by this object to the top-most XmlObjectParser on the stack,
* corresponding to the deepest-level element currently being parsed.
* </p>
* <p>
* This object never handles any SAX event methods itself.  If the
* stack is empty, that's considered an error.
* </p>
* <p>
* See the
* <a href="../test/XmlParserTester.html">ilex.test.XmlParserTester</a>
* program for more information.
* </p>
*/
public class XmlHierarchyParser implements ContentHandler
{
	private Locator locator;
	private Stack objectParsers;
	private ErrorHandler errorHandler;
	private String filename;

  /**
	* Constructor.  The caller must ensure that errorHandler is not null.
	* @param errorHandler the error handler
	*/
	public XmlHierarchyParser( ErrorHandler errorHandler )
	{
		objectParsers = new Stack();
		this.errorHandler = errorHandler;
		this.filename = "";
	}

  /**
	* Set the error handler.  The argument eh must not be null.
	* @param eh the error handler
	*/
	public void setErrorHandler( ErrorHandler eh )
	{
		this.errorHandler = eh;
	}

	/**
	* Pushes a new element parser onto the stack.
	* @param parser the new parser
	*/
	public void pushObjectParser( XmlObjectParser parser )
	{
		objectParsers.push(parser);
	}

	/**
	* Pops the top parser off the stack.
	* @return the popped parser.
	* @throws SAXException if stack is empty
	*/
	public XmlObjectParser popObjectParser( ) throws SAXException
	{
		try
		{
			XmlObjectParser p = (XmlObjectParser)objectParsers.pop();
			return p;
		}
		catch(EmptyStackException ex)
		{
			throw new SAXException("Attempt to pop an empty parser stack", ex);
		}
	}

	//==================================================================
	// Methods that override the SAX ContentHandler interface:
	//==================================================================
    /**
	* @param locator
	*/
	public void setDocumentLocator( Locator locator )
	{
		this.locator = new MyLocator(locator, filename);
	}

    /**
	* @throws SAXException
	*/
	public void startDocument( ) throws SAXException
	{
	}


    /**
	* @throws SAXException
	*/
	public void endDocument( ) throws SAXException
	{
	}

    /**
	* @param prefix
	* @param uri
	* @throws SAXException
	*/
	public void startPrefixMapping( String prefix, String uri ) throws SAXException
	{
	}

    /**
	* @param prefix
	* @throws SAXException
	*/
	public void endPrefixMapping( String prefix ) throws SAXException
	{
	}


    /**
	* Called when SAX sees an element start tag.
	* Delegate to the top parser on the stack.
	* @param namespaceURI passed to top parser
	* @param localName passed to top parser
	* @param qName passed to top parser
	* @param atts passed to top parser
	* @throws SAXException if thrown from top parser
	*/
	public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) throws SAXException
	{
		try
		{
			// Delegate this to the Object Parser on the top of the stack.
			XmlObjectParser parser = (XmlObjectParser)objectParsers.peek();
			parser.startElement(this, namespaceURI, localName, qName, atts);
		}
		catch(EmptyStackException ex)
		{
			throw new SAXException("Elements found after top level element ended.", ex);
		}
		catch(SAXException ex)
		{
			// Add location information to application exceptions.
			errorHandler.error(new SAXParseException(ex.getMessage(), locator, ex));
		}
	}

    /**
	* Called when SAX sees an end tag.
	* Delegate this info to the top parser on the stack.
	* @param namespaceURI passed to top parser
	* @param localName passed to top parser
	* @param qName passed to top parser
	* @throws SAXException
	*/
	public void endElement( String namespaceURI, String localName, String qName ) throws SAXException
	{
		try
		{
			// Delegate this to the Object Parser on the top of the stack.
			XmlObjectParser parser = (XmlObjectParser)objectParsers.peek();
			parser.endElement(this, namespaceURI, localName, qName);
		}
		catch(EmptyStackException ex)
		{
			throw new SAXException("End-Element found after top level element ended.", ex);
		}
		catch(SAXException ex)
		{
			// Add location information to application exceptions.
			errorHandler.error(new SAXParseException(ex.getMessage(), locator, ex));
		}
	}

    /**
	* Delegate this info to the top parser on the stack.
	* @param ch the characters
	* @param start the start
	* @param length the length.
	* @throws SAXException if thrown from top parser
	*/
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		try
		{
			// Delegate this to the Object Parser on the top of the stack.
			XmlObjectParser parser = (XmlObjectParser)objectParsers.peek();
			parser.characters(ch, start, length);
		}
		catch(EmptyStackException ex)
		{
			throw new SAXException("Characters found after top level element ended.", ex);
		}
		catch(SAXException ex)
		{
			// Add location information to application exceptions.
			errorHandler.error(new SAXParseException(ex.getMessage(), locator, ex));
		}
	}


    /**
	* Delegate this info to the top parser on the stack.
	* @param ch the whitespace characters
	* @param start the start
	* @param length the length.
	* @throws SAXException if thrown from top parser
	*/
	public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException
	{
		try
		{
			// Delegate this to the Object Parser on the top of the stack.
			XmlObjectParser parser = (XmlObjectParser)objectParsers.peek();
			parser.ignorableWhitespace(ch, start, length);
		}
		catch(EmptyStackException ex)
		{
			throw new SAXException("Whitespace found after top level element ended.", ex);
		}
		catch(SAXException ex)
		{
			// Add location information to application exceptions.
			errorHandler.error(new SAXParseException(ex.getMessage(), locator, ex));
		}
	}


    /**
	* Does nothing.
	* @param target ignored
	* @param data ignored
	*/
	public void processingInstruction( String target, String data ) throws SAXException
	{
	}

    /**
	* Does nothing.
	* @param name ignored.
	*/
	public void skippedEntity( String name ) throws SAXException
	{
	}

	/**
	* Sets the file name
	* @param fn the file name
	*/
	public void setFileName( String fn )
	{
		this.filename = fn;
	}

	class MyLocator implements Locator
	{
		private Locator loc;
		String filename;

		/**
		* @param loc
		* @param fn
		*/
		public MyLocator( Locator loc, String fn )
		{
			this.loc = loc;
			this.filename = fn;
		}

		/**
		* @return
		*/
		public int getColumnNumber( )
		{
			return loc.getColumnNumber();
		}

		/**
		* @return
		*/
		public int getLineNumber( )
		{
			return loc.getLineNumber();
		}
		/**
		* @return
		*/
		public String getPublicId( )
		{
			return filename;
		}
		/**
		* @return
		*/
		public String getSystemId( )
		{
			return loc.getSystemId();
		}
	}


}