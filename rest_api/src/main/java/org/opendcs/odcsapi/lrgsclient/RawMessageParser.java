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

package org.opendcs.odcsapi.lrgsclient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.ApiRawMessageBlock;
import org.opendcs.odcsapi.util.ApiTextUtil;
import org.opendcs.odcsapi.xml.ElementIgnorer;
import org.opendcs.odcsapi.xml.TaggedDoubleOwner;
import org.opendcs.odcsapi.xml.TaggedDoubleSetter;
import org.opendcs.odcsapi.xml.TaggedLongOwner;
import org.opendcs.odcsapi.xml.TaggedLongSetter;
import org.opendcs.odcsapi.xml.TaggedStringOwner;
import org.opendcs.odcsapi.xml.TaggedStringSetter;
import org.opendcs.odcsapi.xml.XmlHierarchyParser;
import org.opendcs.odcsapi.xml.XmlObjectParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class RawMessageParser
	implements XmlObjectParser, TaggedLongOwner, TaggedStringOwner,
	TaggedDoubleOwner
{
	private ApiRawMessageBlock armb = null;
	private ApiRawMessage arm = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss.SSS");
	private RawMessageBlockParser parent = null;
	
	private static final int DOMSAT_SEQ_TAG = 1;
	private static final int LOCAL_RECV_TAG = 2;
	private static final int CARRIER_START_TAG = 3;
	private static final int CARRIER_STOP_TAG = 4;
	private static final int BAUD_TAG = 5;
	private static final int GOOD_PHASE_TAG = 6;
	private static final int FREQ_OFF_TAG = 7;
	private static final int SIG_STR_TAG = 8;
	private static final int PHASE_NOISE_TAG = 9;
	private static final int XMIT_TIME_TAG = 10;
	private static final int MOMSM_TAG = 11;
	private static final int MTMSM_TAG = 12;
	private static final int CDR_REF_TAG = 13;
	private static final int SESSION_STAT_TAG = 14;
	private static final int BINARY_MSG_TAG = 15;
	
	
	public RawMessageParser(ApiRawMessageBlock armb, ApiRawMessage arm, RawMessageBlockParser parent)
	{
		this.armb = armb;
		this.arm = arm;
		this.parent = parent;
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException
	{
		if (!ApiTextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within 'DcpMsg' - only sub elements.");
	}

	@Override
	public void startElement(XmlHierarchyParser hier, String namespaceURI, String localName, String qname,
			Attributes atts) throws SAXException
	{
		if (localName.equalsIgnoreCase("DomsatSeq"))
			hier.pushObjectParser(new TaggedLongSetter(this, DOMSAT_SEQ_TAG));
		else if (localName.equalsIgnoreCase("LocalRecvTime"))
				hier.pushObjectParser(new TaggedStringSetter(this, LOCAL_RECV_TAG));
		else if (localName.equalsIgnoreCase("CarrierStart"))
			hier.pushObjectParser(new TaggedStringSetter(this, CARRIER_START_TAG));
		else if (localName.equalsIgnoreCase("CarrierStop"))
			hier.pushObjectParser(new TaggedStringSetter(this, CARRIER_STOP_TAG));
		else if (localName.equalsIgnoreCase("Baud"))
			hier.pushObjectParser(new TaggedLongSetter(this, BAUD_TAG));
		else if (localName.equalsIgnoreCase("GoodPhasePct"))
			hier.pushObjectParser(new TaggedDoubleSetter(this, GOOD_PHASE_TAG));
		else if (localName.equalsIgnoreCase("FreqOffset"))
			hier.pushObjectParser(new TaggedDoubleSetter(this, FREQ_OFF_TAG));
		else if (localName.equalsIgnoreCase("SignalStrength"))
			hier.pushObjectParser(new TaggedDoubleSetter(this, SIG_STR_TAG));
		else if (localName.equalsIgnoreCase("PhaseNoise"))
			hier.pushObjectParser(new TaggedDoubleSetter(this, PHASE_NOISE_TAG));
		else if (localName.equalsIgnoreCase("XmitTime"))
			hier.pushObjectParser(new TaggedStringSetter(this, XMIT_TIME_TAG));
		else if (localName.equalsIgnoreCase("MOMSM"))
			hier.pushObjectParser(new TaggedLongSetter(this, MOMSM_TAG));
		else if (localName.equalsIgnoreCase("MTMSM"))
			hier.pushObjectParser(new TaggedLongSetter(this, MTMSM_TAG));
		else if (localName.equalsIgnoreCase("CDR_Reference"))
			hier.pushObjectParser(new TaggedLongSetter(this, CDR_REF_TAG));
		else if (localName.equalsIgnoreCase("SessionStatus"))
			hier.pushObjectParser(new TaggedLongSetter(this, SESSION_STAT_TAG));
		else if (localName.equalsIgnoreCase("BinaryMsg"))
			hier.pushObjectParser(new TaggedStringSetter(this, BINARY_MSG_TAG));
		else if (localName.equalsIgnoreCase("DomsatTime"))
			hier.pushObjectParser(new ElementIgnorer());
		else
		{
			parent.warning("Unrecognized tag '" + localName + "' within DcpMsg. Skipped.");
			hier.pushObjectParser(new ElementIgnorer());
		}
	}

	@Override
	public void endElement(XmlHierarchyParser hier, String namespaceURI, String localName, String qname)
			throws SAXException
	{
		if (localName.equalsIgnoreCase("DcpMsg"))
		{
			armb.getMessages().add(arm);
			hier.popObjectParser();
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
	{
	}

	@Override
	public void set(int tag, String value) throws SAXException
	{
		switch(tag)
		{
		case LOCAL_RECV_TAG:
			try { arm.setLocalRecvTime(sdf.parse(value)); }
			catch(ParseException ex)
			{
				parent.warning("Invalid date format in LocalRecvTime '" + value + "': " + ex);
			}
			break;
		case CARRIER_START_TAG:
			try { arm.setCarrierStart(sdf.parse(value)); }
			catch(ParseException ex)
			{
				parent.warning("Invalid date format in CarrierStart '" + value + "': " + ex);
			}
			break;
		case CARRIER_STOP_TAG:
			try { arm.setCarrierStop(sdf.parse(value)); }
			catch(ParseException ex)
			{
				parent.warning("Invalid date format in CarrierStop '" + value + "': " + ex);
			}
			break;
		case XMIT_TIME_TAG:
			try { arm.setXmitTime(sdf.parse(value)); }
			catch(ParseException ex)
			{
				parent.warning("Invalid date format in XmitTime '" + value + "': " + ex);
			}
			break;
		case BINARY_MSG_TAG:
			arm.setBase64(value.trim());
			break;
		}
		
	}

	@Override
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case DOMSAT_SEQ_TAG: arm.setSequenceNum((int)value); break;
		case BAUD_TAG: arm.setBaud((int)value); break;
		case MOMSM_TAG: arm.setMomsn((int)value); break;
		case MTMSM_TAG: arm.setMtmsn((int)value); break;
		case CDR_REF_TAG: arm.setCdrReference(value); break;
		case SESSION_STAT_TAG: arm.setSequenceNum((int)value); break;
		}
	}

	@Override
	public void set(int tag, double value)
	{
		switch(tag)
		{
		case GOOD_PHASE_TAG: arm.setGoodPhasePct(value); break;
		case FREQ_OFF_TAG: arm.setFreqOffset(value); break;
		case SIG_STR_TAG: arm.setSignalStrength(value); break;
		case PHASE_NOISE_TAG: arm.setPhaseNoise(value); break;
		}
		
	}


}
