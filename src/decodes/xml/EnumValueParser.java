/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
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
*  Revision 1.7  2002/04/06 15:48:19  mike
*  Expand newlines in description fields.
*
*  Revision 1.6  2001/03/18 22:23:56  mike
*  Improved output formatting.
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
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.AsciiUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for EnumValue elements into
 * EnumValue objects.
 */
public class EnumValueParser implements XmlObjectParser, TaggedStringOwner, XmlObjectWriter, TaggedLongOwner
{
	private EnumValue enumValue; // object that we will build.

	private static final int descriptionTag = 0;
	private static final int execClassTag = 1;
	private static final int editClassTag = 2;
	private static final int sortNumberTag = 3;

	/**
	 * @param enumValue the object in which to store the data.
	 */
	public EnumValueParser( EnumValue enumValue )
	{
		super();
		this.enumValue = enumValue;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.EnumValue_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected");
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
		if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.execClass_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, execClassTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.editClass_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, editClassTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.sortNumber_el))
		{
			hier.pushObjectParser(new TaggedLongSetter(this, sortNumberTag));
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
	public void set( int tag, String str )
	{
		switch(tag)
		{
		case descriptionTag:
			str = TextUtil.collapseWhitespace(str);
			str = new String(AsciiUtil.ascii2bin(str));
			enumValue.setDescription(str);
			break;
		case execClassTag:
			enumValue.setExecClassName(str);
			break;
		case editClassTag:
			enumValue.setEditClassName(str);
			break;
		}
	}

	/**
	  Called from TaggedLongSetter.
	  @param tag integer tag defined above
	  @param value long value
	*/
	public void set( int tag, long value )
	{
		switch(tag)
		{
		case sortNumberTag:
			enumValue.setSortNumber((int)value);
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
		xos.startElement(myName(), 
			XmlDbTags.EnumValue_value_at, enumValue.getValue());
		if (enumValue.getDescription() != null)
			xos.writeElement(XmlDbTags.description_el, 
				AsciiUtil.bin2ascii(enumValue.getDescription().getBytes()));
		if (enumValue.getExecClassName() != null)
			xos.writeElement(XmlDbTags.execClass_el, enumValue.getExecClassName());
		if (enumValue.getEditClassName() != null)
			xos.writeElement(XmlDbTags.editClass_el, enumValue.getEditClassName());
		if (enumValue.getSortNumber() != Integer.MAX_VALUE)
			xos.writeElement(XmlDbTags.sortNumber_el, ""+enumValue.getSortNumber());
		xos.endElement(myName());
	}
}
