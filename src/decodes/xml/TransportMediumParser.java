/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.17  2004/08/30 14:49:33  mjmaloney
*  Added javadocs
*
*  Revision 1.16  2004/06/03 15:15:10  mjmaloney
*  Fixed import export bugs for SQL database.
*
*  Revision 1.15  2004/04/27 17:15:55  mjmaloney
*  Update to data presentations.
*  Added time zone to transport medium.
*
*  Revision 1.14  2003/11/15 20:08:28  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.13  2003/10/20 20:22:56  mjmaloney
*  Database changes for DECODES 6.0
*
*  Revision 1.12  2002/09/20 02:00:08  mjmaloney
*  SQL Dev
*
*  Revision 1.11  2002/03/31 21:09:42  mike
*  bug fixes
*
*  Revision 1.10  2001/07/04 00:42:27  mike
*  dev
*
*  Revision 1.9  2001/06/24 02:29:36  mike
*  dev
*
*  Revision 1.8  2001/06/20 13:50:04  mike
*  dev
*
*  Revision 1.7  2001/06/16 20:25:55  mike
*  dev
*
*  Revision 1.6  2001/06/05 15:17:30  mike
*  dev
*
*  Revision 1.5  2001/05/04 18:57:33  mike
*  dev
*
*  Revision 1.4  2001/01/04 01:33:30  mike
*  dev
*
*  Revision 1.3  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.2  2000/12/31 23:12:50  mike
*  dev
*
*  Revision 1.1  2000/12/31 15:55:52  mike
*  dev
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for TransportMedium elements.
 */
public class TransportMediumParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedLongOwner
{
	private TransportMedium transportMedium; // object that we will build.

	private static final int channelNumTag = 1;
	private static final int assignedTimeTag = 2;
	private static final int transmitWindowTag = 3;
	private static final int transmitIntervalTag = 4;
	private static final int timeAdjustmentTag = 5;
	private static final int preambleTag = 6;
	private static final int dataOrderTag = 7;
	private static final int timeZoneTag = 8;

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
			catch(IllegalArgumentException e)
			{
				throw new SAXException("Illegal assignedTime: "
					+ e.toString());
			}
			break;
		case transmitWindowTag:
			try
			{
				transportMedium.transmitWindow =
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException e)
			{
				throw new SAXException("Illegal transmitWindow: "
					+ e.toString());
			}
			break;
		case transmitIntervalTag:
			try
			{
				transportMedium.transmitInterval =
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException e)
			{
				throw new SAXException("Illegal transmitInterval: "
					+ e.toString());
			}
			break;
		case preambleTag:
			transportMedium.setPreamble(Character.toUpperCase(str.charAt(0)));
			break;
		case dataOrderTag:
			if (str != null && str.length() > 0 
			 && str.charAt(0) != Constants.dataOrderUndefined)
				Logger.instance().log(Logger.E_DEBUG1,
					"DataOrder='" + str.charAt(0) 
					+ "' element under TransportMedium no longer "
					+ "supported, use value in DecodesScript. -- ignored");
			break;
		case timeZoneTag:
			transportMedium.setTimeZone(str);
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

		xos.endElement(myName());
	}
}
