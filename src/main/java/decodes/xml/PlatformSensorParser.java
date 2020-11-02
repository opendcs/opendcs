/*
*  $Id$
*/
package decodes.xml;

import java.io.IOException;
import java.util.Enumeration;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.xml.*;

import decodes.db.*;

/**
 * This class maps the DECODES XML representation for Site elements.
 */
public class PlatformSensorParser implements XmlObjectParser, XmlObjectWriter, 
	TaggedStringOwner, TaggedLongOwner
{
	private PlatformSensor platformSensor; // object that we will build.
	private String propName; // Tmp storage while waiting for value parse.

	private static final int propertyTag = 0;
	private static final int usgsDdnoTag = 1;

	/**
	 * @param ps the object in which to store the data.
	 */
	public PlatformSensorParser( PlatformSensor ps )
	{
		super();
		platformSensor = ps;
		propName = null;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.PlatformSensor_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within PlatformSensor");
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
		if (localName.equalsIgnoreCase(XmlDbTags.Site_el))
		{
			platformSensor.site = new Site();
			hier.pushObjectParser(new SiteParser(platformSensor.site));
		}
		else if(localName.equalsIgnoreCase(XmlDbTags.PlatformSensorProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
				throw new SAXException(XmlDbTags.PlatformSensorProperty_el 
					+ " without " + XmlDbTags.propertyName_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.UsgsDdno_el))
		{
			hier.pushObjectParser(new TaggedLongSetter(this, usgsDdnoTag));
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
		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			else if (propName.equalsIgnoreCase("DDNO"))
			{
				try 
				{
					platformSensor.setUsgsDdno(Integer.parseInt(str));
					propName = null;
					return;
				}
				catch(NumberFormatException ex) { /* fall through */ }
			}
			platformSensor.getProperties().setProperty(propName, str);
			propName = null;
			break;
		}
	}

	/**
	 * From TaggedLongOwner, sets a tagged field with a long integer.
	 * @param tag the tag defined above
	 * @param v the long-integer content of the element
	 */
	public void set( int tag, long v) 
	{
		switch(tag)
		{
		case usgsDdnoTag:
			platformSensor.setUsgsDdno((int)v);
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.sensorNumber_at, 
			"" + platformSensor.sensorNumber);
		if (platformSensor.site != null)
		{
			SiteParser p = new SiteParser(platformSensor.site);
			p.writeXml(xos);
		}

		int ddno = platformSensor.getUsgsDdno();
		if (ddno <= 0)
		{
			String s = PropertiesUtil.rmIgnoreCase(
				platformSensor.getProperties(), "DDNO");
			if (s != null)
			{
				try { ddno = Integer.parseInt(s.trim()); }
				catch(NumberFormatException ex)
				{
					ddno = 0;
					platformSensor.getProperties().setProperty("DDNO", s);
				}
			}
		}
		if (ddno > 0)
			xos.writeElement(XmlDbTags.UsgsDdno_el, "" + ddno);

		Enumeration e = platformSensor.getProperties().propertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)platformSensor.getProperties().getProperty(nm);
			xos.writeElement(XmlDbTags.PlatformSensorProperty_el, 
				XmlDbTags.propertyName_at, nm, v);
		}
		xos.endElement(myName());
	}
}
