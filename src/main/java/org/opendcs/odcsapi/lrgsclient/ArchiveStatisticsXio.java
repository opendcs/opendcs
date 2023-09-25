/*
*  $Id: ArchiveStatisticsXio.java,v 1.1 2023/05/15 18:33:56 mmaloney Exp $
*
*  $Log: ArchiveStatisticsXio.java,v $
*  Revision 1.1  2023/05/15 18:33:56  mmaloney
*  First check-in of lrgsclient package, derived from OpenDCS lrgs.ldds classes but simplified for API.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/09/02 13:09:05  mjmaloney
*  javadoc
*
*  Revision 1.1  2004/05/04 18:03:55  mjmaloney
*  Moved from statusgui package to here.
*
*/
package org.opendcs.odcsapi.lrgsclient;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.opendcs.odcsapi.beans.ApiLrgsStatus;
import org.opendcs.odcsapi.util.ApiTextUtil;
import org.opendcs.odcsapi.xml.ElementIgnorer;
import org.opendcs.odcsapi.xml.TaggedLongOwner;
import org.opendcs.odcsapi.xml.TaggedLongSetter;
import org.opendcs.odcsapi.xml.XmlHierarchyParser;
import org.opendcs.odcsapi.xml.XmlObjectParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
This class maps the DECODES XML representation for ArchiveStatistics elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class ArchiveStatisticsXio
	implements XmlObjectParser, TaggedLongOwner
{
	/// Top of the parser hierarchy
	private ApiLrgsStatus lrgsStatus;

	private static final int doTag   = 0;
	private static final int dnTag   = 1;
	private static final int dwTag   = 2;
	private static final int ooTag   = 3;
	private static final int omtTag  = 4;
	private static final int lsnTag  = 5;
	private static final int mmTag   = 6;
	private static final int mbTag   = 7;
	private static final int dsTag   = 8;

	/**
	  Construct parser.
	  @param lrgsStatus the LrgsStatusSnapshotExt to populate from XML data
	*/
	public ArchiveStatisticsXio(ApiLrgsStatus lrgsStatus)
	{
		super();
		this.lrgsStatus = lrgsStatus;
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.ArchiveStatistics; }

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!ApiTextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	/**
	  Called when sub-element seen under Archive Statistics.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the new element
	  @param qname ignored
	  @param atts attributes from the new element
	*/
	public void startElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname, Attributes atts)
		throws SAXException
	{
		if (localName.equalsIgnoreCase(StatusXmlTags.dirOldest))
			hier.pushObjectParser(new TaggedLongSetter(this, doTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.dirNext))
			hier.pushObjectParser(new TaggedLongSetter(this, dnTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.dirWrap))
			hier.pushObjectParser(new TaggedLongSetter(this, dwTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.oldestOffset))
			hier.pushObjectParser(new TaggedLongSetter(this, ooTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.oldestMsgTime))
			hier.pushObjectParser(new TaggedLongSetter(this, omtTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.lastSeqNum))
			hier.pushObjectParser(new TaggedLongSetter(this, lsnTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.maxMessages))
			hier.pushObjectParser(new TaggedLongSetter(this, mmTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.maxBytes))
			hier.pushObjectParser(new TaggedLongSetter(this, mbTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.dirSize))
			hier.pushObjectParser(new TaggedLongSetter(this, dsTag));
		else
			hier.pushObjectParser(new ElementIgnorer());
	}

	/**
	  Signals the end of the current element.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the element
	  @param qname ignored
	*/
	public void endElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname)
		throws SAXException
	{
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	/** Does nothing. */
    public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	}

	/**
	  From TaggedLongOwner, called when long value elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case doTag:
			lrgsStatus.setArcDirOldest(value);
			break;
		case dnTag:
			lrgsStatus.setArcDirNext(value);
			break;
		case dwTag:
			lrgsStatus.setArcDirWrap(value);
			break;
		case dsTag:
			lrgsStatus.setArcDirSize(value);
			break;
		case ooTag:
			lrgsStatus.setArcOldestOffset(value);
			break;
		case omtTag:
			lrgsStatus.setArcOldestMsgTime(new Date(value*1000L));
			break;
		case lsnTag:
			lrgsStatus.setArcLastSeqNum(value);
			break;
		case mmTag:
		case mbTag:
			// maxMessages and maxBytes no longer used.
			break;
		}
	}
}
