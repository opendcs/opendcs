/*
*  $Id: TaggedStringSetter.java,v 1.1 2023/05/15 18:36:26 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/xml/TaggedStringSetter.java,v $
*
*  $State: Exp $
*
*  $Log: TaggedStringSetter.java,v $
*  Revision 1.1  2023/05/15 18:36:26  mmaloney
*  Added this odcsapi.xml package for parsing DDS message blocks and LRGS status.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:40  mjmaloney
*  Javadocs
*
*  Revision 1.4  2004/04/26 20:02:27  mjmaloney
*  Dev.
*
*  Revision 1.3  2000/12/29 02:50:07  mike
*  dev
*
*  Revision 1.2  2000/12/22 03:52:30  mike
*  *** empty log message ***
*
*  Revision 1.1  2000/12/21 21:30:11  mike
*  Created
*
*
*/
package org.opendcs.odcsapi.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
* TaggedStringSetter is used to set simple string elements contained in
* an object.
*/
public class TaggedStringSetter implements XmlObjectParser
{
	StringBuffer sb;
	TaggedStringOwner owner;
	int tag;
	boolean trailingSpace;
	
	/**
	* Constructor
	* @param owner the owner to call when content is parsed.
	* @param tag the tag to return to the owner.
	*/
	public TaggedStringSetter( TaggedStringOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
		trailingSpace = false;
	}

	/**
	* Accumulate characters until end element seen.
	* @param ch the characters
	* @param start the start
	* @param length the length
	*/
	public void characters( char[] ch, int start, int length )
	{
		int end = start + length;
		int firstNonSpace = start;
		for(; firstNonSpace < end; firstNonSpace++)
			if (!Character.isWhitespace(ch[firstNonSpace]))
				break;
		if (firstNonSpace == end)  // String is all whitespace
		{
			trailingSpace = true;
			return;
		}

		// There must be at least one non-space character.
		int lastNonSpace = end-1;
		for(; lastNonSpace >= firstNonSpace; lastNonSpace--)
			if (!Character.isWhitespace(ch[lastNonSpace]))
				break;

		if (sb.length() > 0 
		 && (trailingSpace || firstNonSpace > start))
			sb.append(' ');
		sb.append(ch, firstNonSpace, lastNonSpace-firstNonSpace+1);
		trailingSpace = lastNonSpace < end-1;
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		owner.set(tag, sb.toString());
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}
