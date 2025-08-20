/*
*  $Id$
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
*  Revision 1.2  2004/04/26 20:02:26  mjmaloney
*  Dev.
*
*  Revision 1.1  2000/12/31 15:56:56  mike
*  dev
*
*/
package ilex.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import ilex.util.Logger;
import ilex.xml.XmlObjectParser;
import ilex.xml.TaggedBooleanOwner;
import ilex.xml.XmlHierarchyParser;

/**
* TaggedBooleanSetter is used to set simple boolean elements contained in
* an object.
*/
public class TaggedBooleanSetter implements XmlObjectParser
{
	StringBuffer sb;
	TaggedBooleanOwner owner;
	int tag;
	
	/**
	* Constructor
	* @param owner call when content parsed
	* @param tag the tag to pass
	*/
	public TaggedBooleanSetter( TaggedBooleanOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
	}

	/**
	* Character content is concatenated.
	*/
	public void characters( char[] ch, int start, int length )
	{
		String s = new String(ch,start,length);
		sb.append(s.trim());
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		Logger.instance().log(Logger.E_WARNING,
			"Received subordinate element '" + localName 
			+ "' when expecting simple boolean -- ignored.");
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		String s = sb.toString();
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"))
			owner.set(tag, true);
		else
			owner.set(tag, false);
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	* @param ch
	* @param start
	* @param length
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}
