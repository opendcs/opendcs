package decodes.xml;

import java.io.IOException;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import decodes.db.Constants;
import decodes.db.ScheduleEntry;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.ElementIgnorer;
import ilex.xml.TaggedBooleanOwner;
import ilex.xml.TaggedBooleanSetter;
import ilex.xml.TaggedStringOwner;
import ilex.xml.TaggedStringSetter;
import ilex.xml.XmlHierarchyParser;
import ilex.xml.XmlObjectParser;
import ilex.xml.XmlObjectWriter;
import ilex.xml.XmlOutputStream;

public class ScheduleEntryParser implements XmlObjectParser, XmlObjectWriter,
	TaggedBooleanOwner, TaggedStringOwner
{
	private ScheduleEntry schedEntry = null;
	
	public static final int appNameTag = 0;
	public static final int rsNameTag = 1;
	public static final int startTimeTag = 2;
	public static final int runIntervalTag = 3;
	public static final int enabledTag = 4;
	public static final int lastModifiedTag = 5;
	public static final int timeZoneTag = 6;
	
	/** Switch to set last-modify-time whenever we write.
	 * If this parser is being used to write a stand-alone ScheduleEntry
	 * file in an XML database, then we DO want to do this.
	 * Otherwise, if this is being used to export a list of ScheduleEntries,
	 * then we do NOT want to do this.
	 */
	private boolean setLMTonWrite = true;

	/**
	 * Construct an XML parser/writer for ScheduleEntry
	 * @param schedEntry the entry to read/write
	 * @param setLMTonWrite true means set Last Modify Time when writing.
	 */
	public ScheduleEntryParser(ScheduleEntry schedEntry, boolean setLMTonWrite)
	{
		this.schedEntry = schedEntry;
		this.setLMTonWrite = setLMTonWrite;
	}

	@Override
	public void set(int tag, String value) throws SAXException
	{
		switch(tag)
		{
		case appNameTag:
			schedEntry.setLoadingAppName(value.trim());
			break;
		case rsNameTag:
			schedEntry.setRoutingSpecName(value.trim());
			break;
		case startTimeTag:
			try
			{
				schedEntry.setStartTime(Constants.defaultDateFormat.parse(value.trim()));
			}
			catch(Exception e)
			{
				throw new SAXException("Improper start date/time format '" + value
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
			break;
		case lastModifiedTag:
			try
			{
				schedEntry.setLastModified(Constants.defaultDateFormat.parse(value.trim()));
			}
			catch(Exception e)
			{
				throw new SAXException("Improper last modified date/time format '" + value
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
			break;
		case runIntervalTag:
			schedEntry.setRunInterval(value.trim());
			break;
		case timeZoneTag:
			schedEntry.setTimezone(value.trim());
			break;
		}
	}

	@Override
	public void set(int tag, boolean value)
	{
		schedEntry.setEnabled(value);
	}

	@Override
	public void writeXml(XmlOutputStream xos) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.name_at,
			"" + schedEntry.getName());

		if (schedEntry.getLoadingAppName() != null)
			xos.writeElement(XmlDbTags.LoadingAppName_el, schedEntry.getLoadingAppName());
		if (schedEntry.getRoutingSpecName() != null)
			xos.writeElement(XmlDbTags.RoutingSpecName_el, schedEntry.getRoutingSpecName());
		if (schedEntry.getStartTime() != null)
			xos.writeElement(XmlDbTags.StartTime_el, 
				Constants.defaultDateFormat.format(schedEntry.getStartTime()));
		if (schedEntry.getRunInterval() != null)
			xos.writeElement(XmlDbTags.RunInterval_el, schedEntry.getRunInterval());
		xos.writeElement(XmlDbTags.Enabled_el, ""+schedEntry.isEnabled());
		if (schedEntry.getTimezone() != null)
			xos.writeElement(XmlDbTags.TimeZone_el, schedEntry.getTimezone());
		if (setLMTonWrite)
			schedEntry.setLastModified(new Date());
		xos.writeElement(XmlDbTags.lastModifyTime_el, 
			Constants.defaultDateFormat.format(schedEntry.getLastModified()));
	
		xos.endElement(myName());
	}

	@Override
	public String myName()
	{
		return XmlDbTags.ScheduleEntry_el;
	}

	@Override
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	@Override
	public void startElement(XmlHierarchyParser hier, String namespaceURI,
		String localName, String qname, Attributes atts) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.LoadingAppName_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, appNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RoutingSpecName_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, rsNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.StartTime_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, startTimeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RunInterval_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, runIntervalTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Enabled_el))
		{
			hier.pushObjectParser(new TaggedBooleanSetter(this,enabledTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, lastModifiedTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TimeZone_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, timeZoneTag));
		}
		else
		{
			Logger.instance().log(Logger.E_WARNING,
				"Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
			hier.pushObjectParser(new ElementIgnorer());
		}
	}

	@Override
	public void endElement(XmlHierarchyParser hier, String namespaceURI,
		String localName, String qname) throws SAXException
	{
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
		throws SAXException
	{
	}

}
