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
import lrgs.apistatus.AttachedProcess;


/**
This class maps the DECODES XML representation for Process elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class ProcessXio implements XmlObjectParser, TaggedLongOwner, TaggedStringOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/// Top of the parser hierarchy
	private AttachedProcess ap;

	private static final int nTag   = 0;
	private static final int tTag   = 1;
	private static final int uTag   = 2;
	private static final int sTag   = 3;
	private static final int lsnTag = 4;
	private static final int lptTag = 5;
	private static final int lmtTag = 6;
	private static final int scTag  = 7;
	private static final int vTag   = 8;

	/**
	  Construct parser.
	  @param ap the AttachedProcess to populate from XML data
	*/
	public ProcessXio(AttachedProcess ap)
	{
		super();
		this.ap = ap;
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.Process; }

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
		if (localName.equalsIgnoreCase(StatusXmlTags.name))
			hier.pushObjectParser(new TaggedStringSetter(this, nTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.type))
			hier.pushObjectParser(new TaggedStringSetter(this, tTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.user))
			hier.pushObjectParser(new TaggedStringSetter(this, uTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.status))
			hier.pushObjectParser(new TaggedStringSetter(this, sTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.LastSeqNum))
			hier.pushObjectParser(new TaggedLongSetter(this, lsnTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.LastPollTime))
			hier.pushObjectParser(new TaggedLongSetter(this, lptTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.LastMsgTime))
			hier.pushObjectParser(new TaggedLongSetter(this, lmtTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.staleCount))
			hier.pushObjectParser(new TaggedLongSetter(this, scTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.ddsVersion))
			hier.pushObjectParser(new TaggedStringSetter(this, vTag));
		else
		{
			log.warn("Invalid element '{}' under {} -- skipped.", localName, myName());
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
	  From TaggedLongOwner, called from TaggedLongSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case lsnTag:
			ap.lastSeqNum = (int)value;
			break;
		case lptTag:
			ap.lastPollTime = (int)value;
			break;
		case lmtTag:
			ap.lastMsgTime = (int)value;
			break;
		case scTag:
			ap.stale_count = (short)value;
			break;
		}
	}

	/**
	  From TaggedStringOwner, called from TaggedStringSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, String value)
	{
		switch(tag)
		{
		case nTag:
			ap.setName(value);
			break;
		case tTag:
			ap.type = value;
			break;
		case uTag:
			ap.user = value;
			break;
		case sTag:
			ap.status = value;
			break;
		case vTag:
			ap.ddsVersion = value;
			break;
		}
	}
}