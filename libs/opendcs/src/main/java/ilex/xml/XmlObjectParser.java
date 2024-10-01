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
*  Revision 1.2  2004/08/30 14:50:41  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package ilex.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import ilex.xml.XmlHierarchyParser;

/**
* This interface works with XmlHierarchyParser.
* The Hierarchy parser keeps a stack of XmlObjectParser objects. It delegates
* the parsing to the parser on the top of the stack.
*/
public abstract interface XmlObjectParser
{
	/**
	* This is a pass-through from the SAX Content Handler.
	* Your parser should save/convert these characters as appropriate.
	* @param ch the characters
	* @param start the start
	* @param length the length
	* @throws SAXException
	*/
	void characters( char[] ch, int start, int length ) throws SAXException;

	/**
	* Start a new element within the hierarchy.
	* The first argument is the hierarchy. You can use it to push lower-level
	* object parsers. Subsequent arguments are from the SAX Content Handler.
	*/
	void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException;

	/**
	* Signals the end of the current element.
	* Typically this should cause your parser to pop the stack in the
	* hierarchy. Then do whatever cleanup or finalizing is necessary.
	* @param hier
	* @param namespaceURI
	* @param localName
	* @param qname
	* @throws SAXException
	*/
	void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException;

	/**
	* Allows an object to keep track of whitespace, if needed.
	* @param ch
	* @param start
	* @param length
	* @throws SAXException
	*/
	void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException;

}
