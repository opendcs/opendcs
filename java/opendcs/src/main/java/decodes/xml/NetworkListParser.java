/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:49:31  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2003/11/15 20:08:25  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.1  2001/03/16 22:21:07  mike
*  Added NetworkLists & corresponding parsers.
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for NetworkList elements.
 */
public class NetworkListParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private NetworkList networkList; // object that we will build.

	private static final int transportMediumTypeTag = 0;
	private static final int siteNameTypePrefTag = 1;

	/**
	 * @param ob the object in which to store the data.
	 */
	public NetworkListParser( NetworkList ob )
	{
		super();
		networkList = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.NetworkList_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within NetworkList");
	}

	/**
	 * Called after start of new element for this parser is detected.
	 * @param hier the stack of parsers
	 * @param namespaceURI namespaceURI
	 * @param localName name of element
	 * @param qname ignored
	 * @param atts attributes for this element
	 * @throws SAXException on parse error
	 */
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.TransportMediumType_el))
		{
			hier.pushObjectParser(
				new TaggedStringSetter(this, transportMediumTypeTag));
		}
		else if(localName.equalsIgnoreCase(XmlDbTags.SiteNameTypePreference_el))
		{
			hier.pushObjectParser(
				new TaggedStringSetter(this, siteNameTypePrefTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.NetworkListEntry_el))
		{
			String id = atts.getValue(XmlDbTags.TransportId_at);
			if (id == null)
				throw new SAXException(XmlDbTags.NetworkListEntry_el
					+ " without " + XmlDbTags.TransportId_at +" attribute");
			NetworkListEntry entry = new NetworkListEntry(networkList, id);
			networkList.addEntry(entry);
			hier.pushObjectParser(new NetworkListEntryParser(entry));
		}
		else
		{
			Logger.instance().log(Logger.E_WARNING,
				"Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
			hier.pushObjectParser(new ElementIgnorer());
		}
	}

	/**
	 * Signals the end of the current element.
	 * Causes parser to pop the stack in the hierarchy. 
	 * @param hier the stack of parsers
	 * @param namespaceURI ignored
	 * @param localName element that is ending
	 * @param qname ignored
	 */
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		if (localName.equalsIgnoreCase(myName()))
			hier.popObjectParser();
		else
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
	}

	/**
	 * Allows an object to keep track of whitespace, if needed.
	 * @param ch the whitespace
	 * @param start the start of the whitespace
	 * @param length the length of the whitespace
	 */
	public void ignorableWhitespace( char[] ch, int start, int length ) throws SAXException
	{
	}

	/**
	 * From TaggedStringOwner, called from TaggedStringSetter when string
	 * elements are parsed.
	 * @param tag the tag defined above
	 * @param str the string content of the element
	 * @throws SAXException if context or parse error
	 */
	public void set( int tag, String str ) throws SAXException
	{
		switch(tag)
		{
		case transportMediumTypeTag:
			networkList.transportMediumType = str;
			break;
		case siteNameTypePrefTag:
			networkList.siteNameTypePref = str;
			break;
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.name_at, networkList.name);

		if (networkList.transportMediumType != null)
			xos.writeElement(XmlDbTags.TransportMediumType_el,
				networkList.transportMediumType);
		if (networkList.siteNameTypePref != null)
			xos.writeElement(XmlDbTags.SiteNameTypePreference_el,
				networkList.siteNameTypePref);

		Iterator it = networkList.networkListEntries.values().iterator();
		while(it.hasNext())
		{
			NetworkListEntry nle = (NetworkListEntry)it.next();
			NetworkListEntryParser p = new NetworkListEntryParser(nle);
			p.writeXml(xos);
		}

		xos.endElement(myName());
	}
}
