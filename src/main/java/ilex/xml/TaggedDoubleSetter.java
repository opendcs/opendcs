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
*  Revision 1.4  2004/08/30 14:50:39  mjmaloney
*  Javadocs
*
*  Revision 1.3  2004/04/26 20:02:26  mjmaloney
*  Dev.
*
*  Revision 1.2  2000/12/22 03:52:30  mike
*  *** empty log message ***
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package ilex.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import ilex.util.Logger;
import ilex.xml.XmlObjectParser;
import ilex.xml.TaggedDoubleOwner;
import ilex.xml.XmlHierarchyParser;

/**
* TaggedDoubleSetter is used to set simple long or integer elements contained in
* an object.
*/
public class TaggedDoubleSetter implements XmlObjectParser
{
	StringBuffer sb;
	TaggedDoubleOwner owner;
	int tag;
	
	/**
	* Constructor
	* @param owner the owner to call when content is parsed.
	* @param tag the tag to return to the owner.
	*/
	public TaggedDoubleSetter( TaggedDoubleOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
	}

	/**
	* Accumulate characters until end element seen.
	* @param ch the characters
	* @param start the start
	* @param length the length
	*/
	public void characters( char[] ch, int start, int length )
	{
		sb.append(ch, start, length);
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		Logger.instance().log(Logger.E_WARNING,
			"Received subordinate element '" + localName 
			+ "' when expecting simple number -- ignored.");
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		try 
		{
			owner.set(tag, Double.parseDouble(sb.toString().trim()));
		}
		catch(NumberFormatException ex)
		{
			throw new SAXException("Floating point number expected");
		}
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}
