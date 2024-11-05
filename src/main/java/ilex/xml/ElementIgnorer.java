/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:37  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/11/11 02:44:50  mjmaloney
*  Added ElementIgnorer.
*
*/
package ilex.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import ilex.xml.XmlObjectParser;
import ilex.xml.XmlHierarchyParser;

/**
* ElementIgnorer is used to skip over blocks of unrecognized XML.
* In order to facilitate extensibility, the parsers are coded to skip
* blocks of XML that they don't recognize.
* Pushing this object onto the stack will skip the current element and
* any sub-elements, until the current element end-tag is seen.
*/
public class ElementIgnorer implements XmlObjectParser
{
	int stackDepth;
	
	/** Constructor */
	public ElementIgnorer( )
	{
		stackDepth = 1; // Assume startElement for this element already seen.
	}

	/**
	* Does nothing.
	* @param ch ignored
	* @param start ignored
	* @param length ignored
	*/
	public void characters( char[] ch, int start, int length )
	{
	}

	/**
	* Does nothing.
	* @param hier ignored
	* @param namespaceURI ignored
	* @param localName ignored
	* @param qname ignored
	* @param atts ignored
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		// A sub-element has been started.
		stackDepth++;
	}


	/**
	* Pops this parser off the stack
	* @param hier ignored
	* @param namespaceURI ignored
	* @param localName ignored
	* @param qname ignored
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		if (--stackDepth == 0)
			hier.popObjectParser();
	}

    /**
	* Does nothing.
	* @param ch ignored.
	* @param start ignored.
	* @param length ignored.
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}
