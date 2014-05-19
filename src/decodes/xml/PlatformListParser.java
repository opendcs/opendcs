/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.4  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Date;
import java.util.Iterator;
import decodes.db.*;
import decodes.sql.DbKey;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * Parses the platform list cross reference file. Populates PlatformList
 * with partial PlatformObjects.
 */
public class PlatformListParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner,
		TaggedBooleanOwner
{
  /**
	 * This is the object being parsed or written
	 */
	private PlatformList plist;
	
	private Date fileLMT;


	Platform platform; // partial Platform object being populated
	private SiteName workingName;
	private static final int siteNameTag = 1;
	private static final int expirationTag = 2;
	private static final int agencyTag = 3;
	private static final int descriptionTag = 4;
	private static final int configNameTag = 5;
	private static final int platformDesignatorTag = 6;
	private static final int isProductionTag = 7;
	private static final int lastModifyTag = 8;

	/**
	 * @param pl the object in which to store the data.
	 */
	public PlatformListParser( PlatformList pl )
	{
		super();
		platform = null;
		plist = pl;
		fileLMT = new Date();
	}
	
	public void setFileLMT(Date lmt) { fileLMT = lmt; }

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.PlatformList_el; }

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
	public void startElement(XmlHierarchyParser hier, String namespaceURI, 
		String localName, String qname, Attributes atts ) 
		throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.PlatformXref_el))
		{
			if (platform != null)
				throw new SAXException(XmlDbTags.PlatformXref_el
					+ " not allowed inside " + XmlDbTags.PlatformXref_el);

			String str = atts.getValue(XmlDbTags.PlatformId_at);
			if (str == null)
				throw new SAXException("PlatformXref without " +
					XmlDbTags.PlatformId_at + " attribute");
			DbKey platformId;
			try { platformId = DbKey.createDbKey(Long.parseLong(str)); }
			catch (NumberFormatException e)
			{
				throw new SAXException("PlatformXref " +
					XmlDbTags.PlatformId_at + " must be a number");
			}

			// MJM 20041027 Check to see if this platform already exists.
			// That way, I can reread the list periodically to pick up any
			// newly created platforms.
			platform = plist.getById(platformId);
			if (platform != null)
			{
				platform = null;
				Logger.instance().log(Logger.E_DEBUG3,
					"Ignoring previously read platform ID=" + platformId);
				hier.pushObjectParser(new ElementIgnorer());
			}
			else
			{
				platform = new Platform(platformId);
				plist.add(platform);
			}
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TransportXref_el))
		{
			if (platform == null)
				throw new SAXException(XmlDbTags.TransportXref_el
					+ " must be inside " + XmlDbTags.PlatformXref_el);

			String ts = atts.getValue(XmlDbTags.TransportMedium_mediumType_at);
			if (ts  == null)
				throw new SAXException(XmlDbTags.TransportMedium_mediumType_at
					+ " is a required attribute for "
					+ XmlDbTags.TransportXref_el);

			String id = atts.getValue(XmlDbTags.TransportMedium_mediumId_at);
			if (id  == null)
				throw new SAXException(XmlDbTags.TransportMedium_mediumId_at
					+ " is a required attribute for "
					+ XmlDbTags.TransportXref_el);

			TransportMedium tm = new TransportMedium(platform, ts, id);
			platform.transportMedia.add(tm);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.SiteName_el))
		{
			String typ = atts.getValue(XmlDbTags.SiteName_nameType_at);
			if (typ == null)
				throw new SAXException(XmlDbTags.SiteName_el + " without "
					+ XmlDbTags.SiteName_nameType_at +" attribute");
			workingName = new SiteName(null, typ);
			hier.pushObjectParser(new TaggedStringSetter(this, siteNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.expiration_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, expirationTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.agency_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, agencyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
		}
		else if (localName.equalsIgnoreCase(
			XmlDbTags.PlatformConfig_configName_at))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, configNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.PlatformDesignator_el))
		{
			hier.pushObjectParser(
				new TaggedStringSetter(this, platformDesignatorTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
		{
			hier.pushObjectParser(
				new TaggedBooleanSetter(this, isProductionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, lastModifyTag));
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
		if (localName.equalsIgnoreCase(XmlDbTags.PlatformXref_el))
		{
			if (platform != null)
			{
				if (platform.lastModifyTime == null)
					platform.lastModifyTime = fileLMT;
			}
			platform = null;
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TransportXref_el))
			; // End of empty TransportXref element
		else if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		else
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
		case siteNameTag:
			if (workingName == null)
				throw new SAXException("Unexpected site name!");
			workingName.setNameValue(str);
			platform.site = plist.getDatabase().siteList.getSite(workingName);
			workingName = null;
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
		case agencyTag:
			platform.agency = str;
			break;
		case descriptionTag:
			platform.description = TextUtil.collapseWhitespace(str);
			break;
		case configNameTag:
			platform.setConfigName(str);
			break;
		case platformDesignatorTag:
			platform.setPlatformDesignator(str);
			break;
		case lastModifyTag:
			try
			{
				platform.lastModifyTime = Constants.defaultDateFormat.parse(str);
			}
			catch(Exception e)
			{
				throw new SAXException("Improper last-modify date/time format '" + str
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
			break;
		}
	}

	public void set( int tag, boolean tf)
	{
		switch(tag)
		{
		case isProductionTag:
			platform.isProduction = tf;
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
		xos.startElement(XmlDbTags.PlatformList_el);

		for(Iterator<Platform> it = plist.iterator();
			it.hasNext(); )
		{
			Platform plat = it.next();
			DbKey id = plat.getId();
			if (id == Constants.undefinedId)
				continue;

			xos.startElement(XmlDbTags.PlatformXref_el,
				XmlDbTags.PlatformId_at, ""+plat.getId());

			for(Iterator<TransportMedium> tt = plat.transportMedia.iterator();
				tt.hasNext(); )
			{
				TransportMedium tm = tt.next();
				xos.writeElement(XmlDbTags.TransportXref_el,
					XmlDbTags.TransportMedium_mediumType_at, tm.getMediumType(),
					XmlDbTags.TransportMedium_mediumId_at, tm.getMediumId(), null);
			}

			if (plat.site != null)
			{
				SiteName sn = plat.site.getPreferredName();
				if (sn != null)
					xos.writeElement(XmlDbTags.SiteName_el,
						XmlDbTags.SiteName_nameType_at, sn.getNameType(),
						sn.getNameValue());
			}

			if (plat.expiration != null)
				xos.writeElement(XmlDbTags.expiration_el,
					Constants.defaultDateFormat.format(plat.expiration));

			if (plat.agency != null)
				xos.writeElement(XmlDbTags.agency_el, plat.agency);

			if (plat.description != null)
				xos.writeElement(XmlDbTags.description_el,plat.description);

			String nm = plat.getConfigName();
			if (nm != null)
				xos.writeElement(XmlDbTags.PlatformConfig_configName_at, nm);

			String d = plat.getPlatformDesignator();
			if (d != null && d.length() > 0)
				xos.writeElement(XmlDbTags.PlatformDesignator_el, d);

			xos.writeElement(XmlDbTags.isProduction_el, "" + plat.isProduction);

			if (plat.lastModifyTime != null)
				xos.writeElement(XmlDbTags.lastModifyTime_el,
					Constants.defaultDateFormat.format(plat.lastModifyTime));

			xos.endElement(XmlDbTags.PlatformXref_el);
		}
		xos.endElement(XmlDbTags.PlatformList_el);
	}
}
