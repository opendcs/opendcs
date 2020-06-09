package decodes.xml;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.xml.ElementIgnorer;
import ilex.xml.TaggedLongOwner;
import ilex.xml.TaggedLongSetter;
import ilex.xml.TaggedStringOwner;
import ilex.xml.TaggedStringSetter;
import ilex.xml.XmlHierarchyParser;
import ilex.xml.XmlObjectParser;
import ilex.xml.XmlObjectWriter;
import ilex.xml.XmlOutputStream;

public class PlatformStatusParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedLongOwner
{
	private PlatformStatus platformStatus = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	
	public static final int lastContactTag = 0;
	public static final int lastMessageTag = 1;
	public static final int lastFailureCodesTag = 2;
	public static final int lastErrorTag = 3;
	public static final int lastScheduleEntryIdTag = 4;
	public static final int annotationTag = 5;

	public PlatformStatusParser(PlatformStatus platformStatus)
	{
		this.platformStatus = platformStatus;
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	public void set(int tag, String value) throws SAXException
	{
		switch(tag)
		{
		case lastContactTag:
			platformStatus.setLastContactTime(str2Date(value, tag));
			break;
		case lastMessageTag:
			platformStatus.setLastMessageTime(str2Date(value, tag));
			break;
		case lastFailureCodesTag:
			platformStatus.setLastFailureCodes(value.trim());
			break;
		case lastErrorTag:
			platformStatus.setLastErrorTime(str2Date(value, tag));
			break;
		case annotationTag:
			platformStatus.setAnnotation(value.trim());
			break;
		}
	}
	
	@Override
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case lastScheduleEntryIdTag:
			platformStatus.setLastScheduleEntryStatusId(DbKey.createDbKey(value));
			break;
		}		
	}
	
	private Date str2Date(String s, int tag)
	{
		try { return sdf.parse(s.trim()); }
		catch(Exception ex)
		{
			Logger.instance().warning(myName() + ": Invalid date '" + s + "' tag=" + tag
				+ " -- required format is '" + sdf.toPattern() + "' -- UTC assumed.");
			return null;
		}
	}

	@Override
	public void writeXml(XmlOutputStream xos) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.id_at, platformStatus.getId().toString());
		if (platformStatus.getLastContactTime() != null)
			xos.writeElement(XmlDbTags.LastContact_el, sdf.format(
				platformStatus.getLastContactTime()));
		if (platformStatus.getLastMessageTime() != null)
			xos.writeElement(XmlDbTags.LastMessageTime_el, sdf.format(
				platformStatus.getLastMessageTime()));
		if (platformStatus.getLastFailureCodes() != null)
			xos.writeElement(XmlDbTags.LastFailureCodes_el, platformStatus.getLastFailureCodes());
		if (platformStatus.getLastErrorTime() != null)
			xos.writeElement(XmlDbTags.LastErrorTime_el, sdf.format(
				platformStatus.getLastErrorTime()));
		xos.writeElement(XmlDbTags.LastScheduleEntryStatus_el,
			"" + platformStatus.getLastScheduleEntryStatusId());
		if (platformStatus.getAnnotation() != null)
			xos.writeElement(XmlDbTags.Annotation_el, platformStatus.getAnnotation());
	
		xos.endElement(myName());
	}

	@Override
	public String myName()
	{
		return XmlDbTags.PlatformStatus_el;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	@Override
	public void startElement(XmlHierarchyParser hier, String namespaceURI, String localName,
		String qname, Attributes atts) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.LastContact_el))
			hier.pushObjectParser(new TaggedStringSetter(this, lastContactTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.LastMessageTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this, lastMessageTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.LastFailureCodes_el))
			hier.pushObjectParser(new TaggedStringSetter(this, lastFailureCodesTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.LastErrorTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this, lastErrorTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.LastScheduleEntryStatus_el))
			hier.pushObjectParser(new TaggedLongSetter(this, lastScheduleEntryIdTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.Annotation_el))
			hier.pushObjectParser(new TaggedStringSetter(this, annotationTag));
		else
		{
			Logger.instance().warning(
				"Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
			hier.pushObjectParser(new ElementIgnorer());
		}
	}

	@Override
	public void endElement(XmlHierarchyParser hier, String namespaceURI, String localName,
		String qname) throws SAXException
	{
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
	{
	}
}
