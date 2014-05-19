/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/12/29 15:30:40  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.13  2006/02/27 16:15:01  mmaloney
*  dev
*
*  Revision 1.12  2005/03/15 16:52:03  mjmaloney
*  Rename Enum to DbEnum for Java 5 compatibility
*
*  Revision 1.11  2005/03/15 16:11:30  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.10  2004/08/30 14:49:30  mjmaloney
*  Added javadocs
*
*  Revision 1.9  2003/11/15 20:08:24  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.8  2003/10/20 20:22:55  mjmaloney
*  Database changes for DECODES 6.0
*
*  Revision 1.7  2002/03/31 21:09:42  mike
*  bug fixes
*
*  Revision 1.6  2001/01/04 01:33:30  mike
*  dev
*
*  Revision 1.5  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.4  2000/12/31 23:12:50  mike
*  dev
*
*  Revision 1.3  2000/12/31 15:55:51  mike
*  dev
*
*  Revision 1.2  2000/12/29 02:50:03  mike
*  dev
*
*  Revision 1.1  2000/12/28 14:01:19  mike
*  First working version.
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Collection;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Enum elements into
 * Enum objects.
 */
public class EnumParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private decodes.db.DbEnum dbenum; // Enum object that we will build.
	private static final int defaultValueTag = 1;

	/** This flag is used by importer to detect whether enums were modified. */
	public static boolean enumParsed = false;

	/**
	 * @param dbenum the object in which to store the data.
	 */
	public EnumParser( decodes.db.DbEnum dbenum )
	{
		super();
		this.dbenum = dbenum;
		enumParsed = true;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.Enum_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected within Enum");
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
		if (localName.equalsIgnoreCase(XmlDbTags.EnumValue_el))
		{
			String key = atts.getValue(XmlDbTags.EnumValue_value_at);
			if (key == null)
				throw new SAXException( "Enum without " 
					+ XmlDbTags.EnumValue_value_at +" attribute");
			EnumValue v = null;
			try
			{
				v = dbenum.addValue(key, null, null, null);
//Logger.instance().debug3("Added to enum '" + dbenum.enumName + "' value '" 
//+ v.value + "' new size=" + dbenum.size());
			}
			catch(ValueAlreadyDefinedException ex)
			{
				throw new SAXException(ex.toString());
			}
			hier.pushObjectParser(new EnumValueParser(v));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EnumDefaultValue_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,defaultValueTag));
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
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
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
		case defaultValueTag:
			dbenum.setDefault(str);
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
		xos.startElement( myName(),  XmlDbTags.name_at, dbenum.enumName);
		
		String def = dbenum.getDefault();
		if (def != null && def.trim().length() > 0)
			xos.writeElement(XmlDbTags.EnumDefaultValue_el, def);

		Collection col = dbenum.values();
		for(Iterator it = col.iterator(); it.hasNext(); )
		{
			EnumValue ev = (EnumValue)it.next();
			EnumValueParser p = new EnumValueParser(ev);
			p.writeXml(xos);
		}
		xos.endElement(myName());
	}
}
