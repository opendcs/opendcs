/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*  $Id: DownLinkXio.java,v 1.1 2023/05/15 18:33:56 mmaloney Exp $
*
*  $Log: DownLinkXio.java,v $
*  Revision 1.1  2023/05/15 18:33:56  mmaloney
*  First check-in of lrgsclient package, derived from OpenDCS lrgs.ldds classes but simplified for API.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
*  Revision 1.2  2010/01/07 21:48:09  shweta
*  Enhancements for multiple DDS Receive  group.
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/06/28 17:37:02  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.3  2004/09/02 13:09:05  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/06/08 19:31:36  mjmaloney
*  Final cosmetic mods
*
*  Revision 1.1  2004/05/04 18:03:56  mjmaloney
*  Moved from statusgui package to here.
*
*/
package org.opendcs.odcsapi.lrgsclient;

import java.util.Date;

import org.opendcs.odcsapi.beans.ApiLrgsHourlyQuality;
import org.opendcs.odcsapi.beans.ApiLrgsDownlinkStatus;
import org.opendcs.odcsapi.util.ApiTextUtil;
import org.opendcs.odcsapi.xml.ElementIgnorer;
import org.opendcs.odcsapi.xml.TaggedLongOwner;
import org.opendcs.odcsapi.xml.TaggedLongSetter;
import org.opendcs.odcsapi.xml.TaggedStringOwner;
import org.opendcs.odcsapi.xml.TaggedStringSetter;
import org.opendcs.odcsapi.xml.XmlHierarchyParser;
import org.opendcs.odcsapi.xml.XmlObjectParser;
import org.opendcs.odcsapi.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class DownLinkXio
	implements XmlObjectParser, TaggedLongOwner, TaggedStringOwner
{
	private LrgsStatusXio parent = null;
	
	/** The downlink we're parsing. */
	private ApiLrgsDownlinkStatus dl = null;

	private static final int tTag     = 0;
	private static final int scTag    = 1;
	private static final int sTag     = 2;
	private static final int lmrtTag  = 3;
	private static final int lsnTag   = 4;
	private static final int berTag   = 5;
	private static final int groupTag = 6;

	
	/**
	  Construct parser.
	  @param parent the LrgsStatusSnapshotExt to populate from XML data
	  @param dl the DownLink to populate
	*/
	public DownLinkXio(ApiLrgsDownlinkStatus dl, LrgsStatusXio parent)
	{
		super();
		this.dl = dl;
		this.parent = parent;
	}

	/** @return XML element name. */
	public String myName() { return StatusXmlTags.DownLink; }

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
				if (hour < 0 || hour > 23)
				{
					parent.warning("Invalid hour in DownLink.Quality " + hour + ": ignored.");
					hier.pushObjectParser(new ElementIgnorer());
					return;
				}
				
				ApiLrgsHourlyQuality qm = dl.getHourlyQuality()[hour];
				hier.pushObjectParser(new QualityMeasurementXio(qm, parent));
			}
			catch(NumberFormatException ex)
			{
				parent.warning("Invalid hour in Quality record, hour='"
					+ hours + "' -- must be 0...23 -- Quality ignored.");
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
		else
		{
		
			parent.warning("Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
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
			dl.setType(DownlinkCodes.type2str((int)value));
			break;
		case scTag:
			if (scTag != DownlinkCodes.DL_STRSTAT)
				dl.setStatus(DownlinkCodes.statcode2str((int)value));
			break;
		case lmrtTag:
			dl.setLastMsgRecvTime(new Date(value * 1000L));
			break;
		case lsnTag:
			dl.setLastSeqNum(value);
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
			dl.setStatus(value);
			break;
		case groupTag:
			dl.setGroup(value);
			break;
		}
	}
}
