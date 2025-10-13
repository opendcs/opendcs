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
import lrgs.apistatus.DownLink;
import lrgs.apistatus.QualityMeasurement;


/**
This class maps the DECODES XML representation for DownLink elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class DownLinkXio implements XmlObjectParser, TaggedLongOwner, TaggedStringOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The downlink we're parsing. */
	private DownLink dl;

	/** Link back to parent object. */
	private LrgsStatusSnapshotExt lsse;

	/** Every downlink has a unique 'slot' */
	private int slot;

	private static final int tTag     = 0;
	private static final int scTag    = 1;
	private static final int sTag     = 2;
	private static final int lmrtTag  = 3;
	private static final int lsnTag   = 4;
	private static final int berTag   = 5;
	private static final int groupTag = 6;


	/**
	  Construct parser.
	  @param lsse the LrgsStatusSnapshotExt to populate from XML data
	  @param dl the DownLink to populate
	*/
	public DownLinkXio(LrgsStatusSnapshotExt lsse, DownLink dl, int slot)
	{
		super();
		this.lsse = lsse;
		this.dl = dl;
		this.slot = slot;
	}

	/** @return XML element name. */
	public String myName() { return StatusXmlTags.DownLink; }

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
		if (localName.equalsIgnoreCase(StatusXmlTags.type))
			hier.pushObjectParser(new TaggedLongSetter(this, tTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.StatusCode))
			hier.pushObjectParser(new TaggedLongSetter(this, scTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.status))
			hier.pushObjectParser(new TaggedStringSetter(this, sTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.LastMsgRecvTime))
			hier.pushObjectParser(new TaggedLongSetter(this, lmrtTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.LastSeqNum))
			hier.pushObjectParser(new TaggedLongSetter(this, lsnTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.BER))
			hier.pushObjectParser(new TaggedLongSetter(this, berTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.group))
			hier.pushObjectParser(new TaggedStringSetter(this, groupTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.Quality))
		{
			String hours = XmlUtils.getAttrIgnoreCase(atts, "hour");
			try
			{
				int hour = Integer.parseInt(hours);
				QualityMeasurement qm = new QualityMeasurement(false, 0, 0, 0);
				lsse.addDownlinkQualityMeasurement(slot, qm, hour);
				hier.pushObjectParser(new QualityMeasurementXio(qm));
			}
			catch(NumberFormatException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid hour in Quality record, hour='{}' -- must be 0...23 -- Quality ignored.",
				   		hours);
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
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
	  From TaggedLongOwner, called from TaggedLongSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case tTag:
			dl.type = (short)value;
			break;
		case scTag:
			dl.statusCode = (short)value;
			break;
		case lmrtTag:
			dl.lastMsgRecvTime = (int)value;
			break;
		case lsnTag:
			dl.lastSeqNum = (int)value;
			dl.hasSeqNum = true;
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
		case sTag:
			dl.statusString = value;
			break;
		case berTag:
			dl.BER = value;
			dl.hasBER = true;
			break;
		case groupTag:
			dl.group = value;
			break;
		}
	}
}
