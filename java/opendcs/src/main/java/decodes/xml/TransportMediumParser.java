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
package decodes.xml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for TransportMedium elements.
 */
public class TransportMediumParser implements XmlObjectParser, XmlObjectWriter,
											  TaggedStringOwner, TaggedLongOwner, TaggedBooleanOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private TransportMedium transportMedium; // object that we will build.

	private static final int channelNumTag = 1;
	private static final int assignedTimeTag = 2;
	private static final int transmitWindowTag = 3;
	private static final int transmitIntervalTag = 4;
	private static final int timeAdjustmentTag = 5;
	private static final int preambleTag = 6;
	private static final int dataOrderTag = 7;
	private static final int timeZoneTag = 8;

	// Added in DatabaseVersion 11
	private static final int loggerTypeTag = 9;
	private static final int baudTag = 10;
	private static final int stopBitsTag = 11;
	private static final int parityTag = 12;
	private static final int dataBitsTag = 13;
	private static final int doLoginTag = 14;
	private static final int usernameTag = 15;
	private static final int passwordTag = 16;

	/**
	 * @param ob the object in which to store the data.
	 */
	public TransportMediumParser( TransportMedium ob )
	{
		super();
		transportMedium = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.TransportMedium_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within TransportMedium");
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
		if (localName.equalsIgnoreCase(XmlDbTags.channelNum_el))
		{
			hier.pushObjectParser(new TaggedLongSetter(this, channelNumTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.assignedTime_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				assignedTimeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.transmitWindow_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				transmitWindowTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.transmitInterval_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				transmitIntervalTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.timeAdjustment_el))
		{
			hier.pushObjectParser(new TaggedLongSetter(this,
				timeAdjustmentTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.EquipmentModel_el + " without "
					+ XmlDbTags.name_at +" attribute");
			transportMedium.equipmentModel = new EquipmentModel(nm);
			hier.pushObjectParser(new EquipmentModelParser(
				transportMedium.equipmentModel));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DecodesScript_el))
		{
			// Note: this is just a name reference to a script that must
			// exist in the PlatformConfig object.
			String nm = atts.getValue(XmlDbTags.DecodesScript_scriptName_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.DecodesScript_el + " without "
					+ XmlDbTags.DecodesScript_scriptName_at +" attribute");
			transportMedium.scriptName = nm;
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.preamble_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				preambleTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.dataOrder_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				dataOrderTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.TimeZone_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,
				timeZoneTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.loggerType_el))
			hier.pushObjectParser(new TaggedStringSetter(this, loggerTypeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.baud_el))
			hier.pushObjectParser(new TaggedLongSetter(this, baudTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.stopBits_el))
			hier.pushObjectParser(new TaggedLongSetter(this, stopBitsTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.parity_el))
			hier.pushObjectParser(new TaggedStringSetter(this, parityTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.dataBits_el))
			hier.pushObjectParser(new TaggedLongSetter(this, dataBitsTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.doLogin_el))
			hier.pushObjectParser(new TaggedBooleanSetter(this, doLoginTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.username_el))
			hier.pushObjectParser(new TaggedStringSetter(this, usernameTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.password_el))
			hier.pushObjectParser(new TaggedStringSetter(this, passwordTag));
		else
		{
			log.warn("Invalid element '{}' under {} -- skipped.", localName, myName());
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
		if (localName.equalsIgnoreCase(XmlDbTags.DecodesScript_el))
			return;
		else if (!localName.equalsIgnoreCase(myName()))
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
		case assignedTimeTag:
			try
			{
				transportMedium.assignedTime =
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException ex)
			{
				throw new SAXException("Illegal assignedTime: '" + str + "'", ex);
			}
			break;
		case transmitWindowTag:
			try
			{
				transportMedium.transmitWindow =
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException ex)
			{
				throw new SAXException("Illegal transmitWindow: '" + str + "'", ex);
			}
			break;
		case transmitIntervalTag:
			try
			{
				transportMedium.transmitInterval =
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException ex)
			{
				throw new SAXException("Illegal transmitInterval: '" + str + "'", ex);
			}
			break;
		case preambleTag:
			transportMedium.setPreamble(Character.toUpperCase(str.charAt(0)));
			break;
		case dataOrderTag:
			if (str != null && str.length() > 0 && str.charAt(0) != Constants.dataOrderUndefined)
			 {
				log.debug("DataOrder='{}' element under TransportMedium no " +
						  "longer supported, use value in DecodesScript. -- ignored",
						  str.charAt(0));
			 }
			break;
		case timeZoneTag:
			transportMedium.setTimeZone(str);
			break;
		case loggerTypeTag:
			transportMedium.setLoggerType(str);
			break;
		case parityTag:
			if (str.length() > 0)
				transportMedium.setParity(str.charAt(0));
			break;
		case usernameTag:
			transportMedium.setUsername(str);
			break;
		case passwordTag:
			transportMedium.setPassword(str);
			break;
		}
	}

	/**
	  Called from TaggedLongSetter.
	  @param tag integer tag defined above
	  @param v long value
	*/
	public void set( int tag, long v )
	{
		switch(tag)
		{
		case channelNumTag:
			transportMedium.channelNum = (int)v;
			break;
		case timeAdjustmentTag:
			transportMedium.setTimeAdjustment((int)v);
			break;
		case baudTag:
			transportMedium.setBaud((int)v);
			break;
		case stopBitsTag:
			transportMedium.setStopBits((int)v);
			break;
		case dataBitsTag:
			transportMedium.setDataBits((int)v);
			break;
		}
	}

	@Override
	public void set(int tag, boolean value)
	{
		switch(tag)
		{
		case doLoginTag:
			transportMedium.setDoLogin(value);
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(),
			XmlDbTags.TransportMedium_mediumType_at,
				transportMedium.getMediumType(),
			XmlDbTags.TransportMedium_mediumId_at,transportMedium.getMediumId());

		if (transportMedium.scriptName != null)
			xos.writeElement(XmlDbTags.DecodesScript_el,
				XmlDbTags.DecodesScript_scriptName_at,
				transportMedium.scriptName, null);

		if (transportMedium.equipmentModel != null)
		{
			EquipmentModelParser p = new EquipmentModelParser(
				transportMedium.equipmentModel);
			p.writeXml(xos);
		}
		if (transportMedium.channelNum != -1)
			xos.writeElement(XmlDbTags.channelNum_el,
				"" + transportMedium.channelNum);

		if (transportMedium.assignedTime != -1)
			xos.writeElement(XmlDbTags.assignedTime_el,
				IDateFormat.printSecondOfDay(transportMedium.assignedTime, true));
		if (transportMedium.transmitWindow != -1)
			xos.writeElement(XmlDbTags.transmitWindow_el,
				IDateFormat.printSecondOfDay(transportMedium.transmitWindow, true));
		if (transportMedium.transmitInterval != -1)
			xos.writeElement(XmlDbTags.transmitInterval_el,
				IDateFormat.printSecondOfDay(transportMedium.transmitInterval, true));

		int ta = transportMedium.getTimeAdjustment();
		if (ta != 0)
			xos.writeElement(XmlDbTags.timeAdjustment_el, "" + ta);

		char c = transportMedium.getPreamble();
		if (c != Constants.preambleUndefined)
			xos.writeElement(XmlDbTags.preamble_el, "" + c);

		String tz = transportMedium.getTimeZone();
		if (tz != null)
			xos.writeElement(XmlDbTags.TimeZone_el, tz);

		if (transportMedium.getLoggerType() != null
		 && transportMedium.getLoggerType().length() > 0)
			xos.writeElement(XmlDbTags.loggerType_el, transportMedium.getLoggerType());
		if (transportMedium.getBaud() != 0)
			xos.writeElement(XmlDbTags.baud_el, ""+transportMedium.getBaud());
		if (transportMedium.getStopBits() != 0)
			xos.writeElement(XmlDbTags.stopBits_el, ""+transportMedium.getStopBits());
		if (transportMedium.getParity() != 'N')
			xos.writeElement(XmlDbTags.parity_el, ""+transportMedium.getParity());
		if (transportMedium.getDataBits() != 0)
			xos.writeElement(XmlDbTags.dataBits_el, ""+transportMedium.getDataBits());
		if (transportMedium.isDoLogin() && transportMedium.getUsername() != null
		 && transportMedium.getUsername().length() > 0)
		{
			xos.writeElement(XmlDbTags.doLogin_el, ""+transportMedium.isDoLogin());
			xos.writeElement(XmlDbTags.username_el, transportMedium.getUsername());
			if (transportMedium.getPassword() != null
			 && transportMedium.getPassword().length() > 0)
				xos.writeElement(XmlDbTags.password_el, transportMedium.getPassword());
		}

		xos.endElement(myName());
	}

}