/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.AsciiUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Platform elements.
 */
public class PlatformParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, 
		TaggedBooleanOwner
{
	private Platform platform; // object that we will build.
	private String propName;     // Tmp storage while waiting for value parse.

	private static final int descriptionTag = 0;
	private static final int agencyTag = 1;
	private static final int isProductionTag = 2;
	private static final int expirationTag = 3;
	private static final int lastModifyTimeTag = 4;
	private static final int operationalProfileTag = 5;
	private static final int propertyTag = 6;
	private static final int platformDesignatorTag = 7;

	/**
	 * @param platform the object in which to store the data.
	 */
	public PlatformParser( Platform platform )
	{
		super();
		this.platform = platform;
		propName = null;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.Platform_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within Platform");
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
		else if (localName.equalsIgnoreCase(XmlDbTags.agency_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, agencyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
		{
			hier.pushObjectParser(new TaggedBooleanSetter(this,isProductionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.expiration_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, expirationTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				lastModifyTimeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.OperationalProfile_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				operationalProfileTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Site_el))
		{
			platform.setSite(new Site(platform));
			hier.pushObjectParser(new SiteParser(platform.getSite()));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformConfig_el))
		{
			String nm = atts.getValue(XmlDbTags.PlatformConfig_configName_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.PlatformConfig_el + " without "
					+ XmlDbTags.PlatformConfig_configName_at +" attribute");

			platform.setConfigName(nm); // soft link

			PlatformConfig pc = new PlatformConfig(nm);

			// The editor keeps a 'soft' link to the platform config. It
			// doesn't want to read platform config records from the platform
			// file.
			if (!Platform.configSoftLink)
				platform.setConfig(pc);
			// otherwise, pc will be discarded after parsing.

			hier.pushObjectParser(new PlatformConfigParser(pc));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TransportMedium_el))
		{
			String typ = atts.getValue(XmlDbTags.TransportMedium_mediumType_at);
			String id = atts.getValue(XmlDbTags.TransportMedium_mediumId_at);
			if (typ == null || id == null)
				throw new SAXException("Missing required attribute: "
					+ XmlDbTags.TransportMedium_mediumType_at + " or "
					+ XmlDbTags.TransportMedium_mediumId_at);

			// Does this platform have a TM with same type but different ID?
			TransportMedium tm = platform.getTransportMedium(typ);
			if (tm != null && !id.equalsIgnoreCase(tm.getMediumId()))
			{
				tm.setMediumId(id);
			}
			else if (tm == null)
			{
				tm = new TransportMedium(platform, typ, id);
				platform.transportMedia.add(tm);
			}
			hier.pushObjectParser(new TransportMediumParser(tm));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformSensor_el))
		{
			String ns = atts.getValue(XmlDbTags.sensorNumber_at);
			if (ns == null)
				throw new SAXException(
					"Missing required sensorNumber attribute");
			int snum = -1;
			try
			{
				snum = Integer.parseInt(ns);
			}
			catch(NumberFormatException e)
			{
				throw new SAXException(
					"Sensor number must be an integer ("+ns+")");
			}
			PlatformSensor ps = new PlatformSensor(platform, snum);
			platform.addPlatformSensor(ps);
			hier.pushObjectParser(new PlatformSensorParser(ps));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
				throw new SAXException(XmlDbTags.PlatformProperty_el 
					+ " without " + XmlDbTags.propertyName_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformDesignator_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				platformDesignatorTag));
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
		case descriptionTag:
			str = TextUtil.collapseWhitespace(str);
			str = new String(AsciiUtil.ascii2bin(str));
			platform.description = str;
			break;
		case agencyTag:
			platform.agency = str;
			break;
		case expirationTag:
			try
			{
				platform.expiration = Constants.defaultDateFormat.parse(str);
			}
			catch(Exception e)
			{
				throw new SAXException("Improper date format '" + str
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
			break;
		case lastModifyTimeTag:
			try
			{
				platform.lastModifyTime = Constants.defaultDateFormat.parse(str);
//Logger.instance().debug3("PlatformParser set LMT to " + platform.lastModifyTime);
			}
			catch(Exception e)
			{
				throw new SAXException("Improper date format '" + str
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
			break;
		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			platform.setProperty(propName, str);
			propName = null;
			break;
		case operationalProfileTag:
			break;
		case platformDesignatorTag:
			platform.setPlatformDesignator(str);
			break;
		}
	}

	/**
	 * From TaggedBooleanOwner, called from TaggedBooleanSetter when string
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param value the boolean value
	 */
	public void set( int tag, boolean value )
	{
		switch(tag)
		{
		case isProductionTag:
			platform.isProduction = value;
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
		if (!platform.idIsSet())
			xos.startElement(myName());
		else
			xos.startElement(myName(), XmlDbTags.PlatformId_at,
				"" + platform.getId());

		if (platform.description != null)
			xos.writeElement(XmlDbTags.description_el,
				AsciiUtil.bin2ascii(platform.description.getBytes()));

		String s = platform.getPlatformDesignator();
		if (s != null)
			xos.writeElement(XmlDbTags.PlatformDesignator_el, s);

		if (platform.agency != null)
			xos.writeElement(XmlDbTags.agency_el, platform.agency);
		xos.writeElement(XmlDbTags.isProduction_el,
			platform.isProduction ? "true" : "false");
		if (platform.lastModifyTime != null)
			xos.writeElement(XmlDbTags.lastModifyTime_el,
				Constants.defaultDateFormat.format(platform.lastModifyTime));
		if (platform.expiration != null)
			xos.writeElement(XmlDbTags.expiration_el,
				Constants.defaultDateFormat.format(platform.expiration));
		if (platform.getSite() != null)
		{
			SiteParser p = new SiteParser(platform.getSite());
			p.writeXml(xos);
		}
		PlatformConfig pc = platform.getConfig();
		if (pc != null)
		{
			PlatformConfigParser p = new PlatformConfigParser(pc);
			p.writeXml(xos);
		}
		for(int i = 0; i < platform.transportMedia.size(); i++)
		{
			TransportMedium tm =
				(TransportMedium)platform.transportMedia.elementAt(i);
			TransportMediumParser p = new TransportMediumParser(tm);
			p.writeXml(xos);
		}
		for (Iterator it = platform.getPlatformSensors(); it.hasNext(); )
		{
			PlatformSensor ps = (PlatformSensor)it.next();
			PlatformSensorParser p = new PlatformSensorParser(ps);
			p.writeXml(xos);
		}
		Enumeration e = platform.getPropertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)platform.getProperty(nm);
			
			xos.writeElement(XmlDbTags.PlatformProperty_el, 
				XmlDbTags.propertyName_at, nm, v);
		}
		xos.endElement(myName());
	}
}
