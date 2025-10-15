/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.statusxml;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.util.TextUtil;
import ilex.xml.*;

/**
This class maps the DECODES XML representation for ArchiveStatistics elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class ArchiveStatisticsXio implements XmlObjectParser, TaggedLongOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/// Top of the parser hierarchy
	private LrgsStatusSnapshotExt lsse;

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
	  @param lsse the LrgsStatusSnapshotExt to populate from XML data
	*/
	public ArchiveStatisticsXio(LrgsStatusSnapshotExt lsse)
	{
		super();
		this.lsse = lsse;
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.ArchiveStatistics; }

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
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
		{
			log.warn("Invalid element '{}' under {} -- skipped", localName, myName());
			hier.pushObjectParser(new ElementIgnorer());
		}
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
			lsse.lss.arcStats.dirOldest = (int)value;
			break;
		case dnTag:
			lsse.lss.arcStats.dirNext = (int)value;
			break;
		case dwTag:
			lsse.lss.arcStats.dirWrap = (short)value;
			break;
		case dsTag:
			lsse.lss.arcStats.dirSize = (int)value;
			break;
		case ooTag:
			lsse.lss.arcStats.oldestOffset = (int)value;
			break;
		case omtTag:
			lsse.lss.arcStats.oldestMsgTime = (int)value;
			break;
		case lsnTag:
			lsse.lss.arcStats.lastSeqNum = (int)value;
			break;
		case mmTag:
			lsse.lss.arcStats.maxMessages = (int)value;
			break;
		case mbTag:
			lsse.lss.arcStats.maxBytes = (int)value;
			break;
		}
	}
}
