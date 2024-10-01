/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import java.util.Iterator;

import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.util.StringPair;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Site elements.
 */
public class SiteParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedDoubleOwner
{
	private Site site; // object that we will build.
	private SiteName workingName;
	private String propName = null;

	private static final int latitudeTag = 0;
	private static final int longitudeTag = 1;
	private static final int timezoneTag = 2;
	private static final int countryTag = 3;
	private static final int stateTag = 4;
	private static final int nearestCityTag = 5;
	private static final int regionTag = 6;
	private static final int siteNameTag = 7;
	private static final int elevationTag = 8;
	private static final int elevationUnitsTag = 9;
	private static final int descriptionTag = 10;
	private static final int propertyTag = 11;

	/**
	 * @param site the object in which to store the data.
	 */
	public SiteParser( Site site )
	{
		super();
		this.site = site;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.Site_el; }

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
		if (localName.equalsIgnoreCase(XmlDbTags.latitude_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, latitudeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.longitude_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, longitudeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.elevation_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, elevationTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.elevationUnits_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, 
				elevationUnitsTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.timezone_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, timezoneTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.country_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, countryTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.state_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, stateTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.nearestCity_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, nearestCityTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.region_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, regionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.SiteName_el))
		{
			String typ = atts.getValue(XmlDbTags.SiteName_nameType_at);
			if (typ == null)
				throw new SAXException(XmlDbTags.SiteName_el + " without "
					+ XmlDbTags.SiteName_nameType_at +" attribute");
			workingName = new SiteName(site, typ);
			String x = atts.getValue(XmlDbTags.SiteName_usgsDbno_at);
			if (x != null)
				workingName.setUsgsDbno(x);
			x = atts.getValue(XmlDbTags.SiteName_agencyCode_at);
			if (x != null)
				workingName.setAgencyCode(x);
			hier.pushObjectParser(new TaggedStringSetter(this, siteNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.SiteProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
                throw new SAXException(XmlDbTags.SiteProperty_el
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
		case latitudeTag:
			site.latitude = str;
			break;
		case longitudeTag:
			site.longitude = str;
			break;
		case elevationUnitsTag:
			site.setElevationUnits(str);
			break;
		case timezoneTag:
			site.timeZoneAbbr = str;
			break;
		case countryTag:
			site.country = str;
			break;
		case stateTag:
			site.state = str;
			break;
		case nearestCityTag:
			site.nearestCity = str;
			break;
		case regionTag:
			site.region = str;
			break;
		case siteNameTag:
			if (workingName == null)
				throw new SAXException("Multiple site name assignments!");
			workingName.setNameValue(str);
			site.addName(workingName);
			// Already added to collection, delete the tmp working reference.
			workingName = null;
			break;
		case descriptionTag:
			site.setDescription(str);
			break;
		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			if (propName.equalsIgnoreCase("PUBLIC_NAME"))
				site.setPublicName(str);
			else
				site.setProperty(propName, str);
			propName = null;
			break;
		}
	}

	/**
	 * From TaggedDoubleOwner, called from TaggedDoubleSetter when Double
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param value the double value
	 */
	public void set( int tag, double value )
	{
		switch(tag)
		{
		case elevationTag:
			site.setElevation(value);
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
		xos.startElement(myName());
		if (site.latitude != null && site.latitude.length() > 0)
			xos.writeElement(XmlDbTags.latitude_el, site.latitude);
		if (site.longitude != null && site.longitude.length() > 0)
			xos.writeElement(XmlDbTags.longitude_el, site.longitude);
		double elev = site.getElevation();
		xos.writeElement(XmlDbTags.elevation_el, "" + elev);
		xos.writeElement(XmlDbTags.elevationUnits_el, site.getElevationUnits());
		if (site.timeZoneAbbr != null && site.timeZoneAbbr.length() > 0)
			xos.writeElement(XmlDbTags.timezone_el, site.timeZoneAbbr);
		if (site.country != null && site.country.length() > 0)
			xos.writeElement(XmlDbTags.country_el, site.country);
		if (site.state != null && site.state.length() > 0)
			xos.writeElement(XmlDbTags.state_el, site.state);
		if (site.nearestCity != null && site.nearestCity.length() > 0)
			xos.writeElement(XmlDbTags.nearestCity_el, site.nearestCity);
		if (site.region != null && site.region.length() > 0)
			xos.writeElement(XmlDbTags.region_el, site.region);
		String description = site.getDescription();
		if (description != null && description.length() > 0)
			xos.writeElement(XmlDbTags.description_el, description);

		for(Iterator it = site.getNames(); it.hasNext(); )
		{
			SiteName nm = (SiteName)it.next();
			if (nm.getNameType().equalsIgnoreCase("usgs"))
			{
				int sz = 1;
				String agency = nm.getAgencyCode();
				String dbno = nm.getUsgsDbno();
				if (agency != null) sz++;
				if (dbno != null) sz++;
				StringPair sp[] = new StringPair[sz];
				sp[0] = new StringPair(XmlDbTags.SiteName_nameType_at,
					nm.getNameType());
				int n = 1;
				if (agency != null)
				{
					sp[n] = new StringPair(XmlDbTags.SiteName_agencyCode_at,
						agency);
					n++;
				}
				if (dbno != null)
				{
					sp[n] = new StringPair(XmlDbTags.SiteName_usgsDbno_at,
						dbno);
				}
				xos.writeElement(XmlDbTags.SiteName_el, sp, nm.getNameValue());
			}
			else
				xos.writeElement(XmlDbTags.SiteName_el,
					XmlDbTags.SiteName_nameType_at, nm.getNameType(), 
					nm.getNameValue());
		}
		
		Enumeration e = site.getPropertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)site.getProperty(nm);
			
			xos.writeElement(XmlDbTags.SiteProperty_el, 
				XmlDbTags.propertyName_at, nm, v);
		}
		if (site.getPublicName() != null)
			xos.writeElement(XmlDbTags.SiteProperty_el, 
				XmlDbTags.propertyName_at, "PUBLIC_NAME", site.getPublicName());
		xos.endElement(myName());
	}
}
