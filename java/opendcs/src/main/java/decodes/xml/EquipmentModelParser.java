/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2004/08/30 14:49:30  mjmaloney
*  Added javadocs
*
*  Revision 1.8  2003/11/15 20:08:25  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.7  2002/04/06 15:48:19  mike
*  Expand newlines in description fields.
*
*  Revision 1.6  2001/03/23 22:06:36  mike
*  PlatformConfig can now be top-level parser.
*
*  Revision 1.5  2001/03/18 22:23:56  mike
*  Improved output formatting.
*
*  Revision 1.4  2001/01/04 01:33:30  mike
*  dev
*
*  Revision 1.3  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.2  2000/12/31 23:12:50  mike
*  dev
*
*  Revision 1.1  2000/12/31 22:30:47  mike
*  dev
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.AsciiUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for EquipmentModel elements.
 */
public class EquipmentModelParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private EquipmentModel equipmentModel; // object that we will build.
	private String propName;     // Tmp storage while waiting for value parse.

	private static final int companyTag = 0;
	private static final int modelTag = 1;
	private static final int descriptionTag = 2;
	private static final int typeTag = 3;
	private static final int propertyTag = 4;

	/**
	 * @param ob the object in which to store the data.
	 */
	public EquipmentModelParser( EquipmentModel ob )
	{
		super();
		equipmentModel = ob;
		propName = null;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.EquipmentModel_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
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
		if (localName.equalsIgnoreCase(XmlDbTags.equipmentType_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, typeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.company_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, companyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.model_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, modelTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,descriptionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
				throw new SAXException(XmlDbTags.EquipmentProperty_el 
					+ " without " + XmlDbTags.propertyName_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
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
		case companyTag:
			equipmentModel.company = str;
			break;
		case modelTag:
			equipmentModel.model = str;
			break;
		case descriptionTag:
			str = TextUtil.collapseWhitespace(str);
			str = new String(AsciiUtil.ascii2bin(str));
			equipmentModel.description = str;
			break;
		case typeTag:
			equipmentModel.equipmentType = str;
			break;
		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			equipmentModel.properties.setProperty(propName, str);
			propName = null;
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
		xos.startElement(myName(), XmlDbTags.name_at,
			"" + equipmentModel.name);

		if (equipmentModel.equipmentType != null)
			xos.writeElement(XmlDbTags.equipmentType_el, 
				equipmentModel.equipmentType);
		if (equipmentModel.company != null)
			xos.writeElement(XmlDbTags.company_el, equipmentModel.company);
		if (equipmentModel.model != null)
			xos.writeElement(XmlDbTags.model_el, equipmentModel.model);
		if (equipmentModel.description != null)
			xos.writeElement(XmlDbTags.description_el, 
				AsciiUtil.bin2ascii(equipmentModel.description.getBytes()));

		Enumeration e = equipmentModel.properties.propertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)equipmentModel.properties.getProperty(nm);
			
			xos.writeElement(XmlDbTags.EquipmentProperty_el, 
				XmlDbTags.propertyName_at, nm, v);
		}
		xos.endElement(myName());
	}
}
