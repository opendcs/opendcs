/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2004/08/30 14:49:33  mjmaloney
*  Added javadocs
*
*  Revision 1.7  2004/06/03 15:15:10  mjmaloney
*  Fixed import export bugs for SQL database.
*
*  Revision 1.6  2003/11/15 20:08:27  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.5  2002/05/19 00:22:19  mjmaloney
*  Deprecated decodes.db.TimeZone and decodes.db.TimeZoneList.
*  These are now replaced by the java.util.TimeZone class.
*
*  Revision 1.4  2001/12/02 13:57:21  mike
*  dev
*
*  Revision 1.3  2001/11/15 01:55:04  mike
*  Implemented RoutingSpec Edit Screens
*
*  Revision 1.2  2001/03/16 22:21:07  mike
*  Added NetworkLists & corresponding parsers.
*
*  Revision 1.1  2001/03/16 19:53:18  mike
*  Implemented XML parsers for routing specs
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Routing Spec elements.
 */
public class RoutingSpecParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedBooleanOwner
{
	private RoutingSpec routingSpec; // object that we will build.
	private String propName;         // Tmp storage while wating for value.

	private static final int enableEquationsTag = 0;
	private static final int usePerformanceMeasurmentsTag = 1;
	private static final int outputFormatTag = 2;
	private static final int outputTimeZoneAbbrTag = 3;
	private static final int presentationGroupNameTag = 4;
	private static final int sinceTimeTag = 5;
	private static final int untilTimeTag = 6;
	private static final int consumerTypeTag = 7;
	private static final int consumerArgTag = 8;
	private static final int isProductionTag = 9;
	private static final int propertyTag = 10;
	private static final int lastModifyTimeTag = 11;

	/**
	 * @param ob the object in which to store the data.
	 */
	public RoutingSpecParser( RoutingSpec ob )
	{
		super();
		this.routingSpec = ob;
		propName = null;
	}

	@Override
	public String myName( ) { return XmlDbTags.RoutingSpec_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within RoutingSpec");
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
		if (localName.equalsIgnoreCase(XmlDbTags.EnableEquations_el))
			hier.pushObjectParser(
				new TaggedBooleanSetter(this, enableEquationsTag));
		else if (localName.equalsIgnoreCase(
			XmlDbTags.UsePerformanceMeasurements_el))
			hier.pushObjectParser(
				new TaggedBooleanSetter(this, usePerformanceMeasurmentsTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.OutputFormat_el))
			hier.pushObjectParser(
				new TaggedStringSetter(this, outputFormatTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.OutputTimeZone_el))
			hier.pushObjectParser(
				new TaggedStringSetter(this, outputTimeZoneAbbrTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.PresentationGroupName_el))
			hier.pushObjectParser(
				new TaggedStringSetter(this, presentationGroupNameTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.SinceTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this, sinceTimeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.UntilTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this, untilTimeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.ConsumerType_el))
			hier.pushObjectParser(
				new TaggedStringSetter(this, consumerTypeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.ConsumerArg_el))
			hier.pushObjectParser(new TaggedStringSetter(this, consumerArgTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
			hier.pushObjectParser(
				new TaggedBooleanSetter(this, isProductionTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this,
				lastModifyTimeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.DataSource_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			String tp = atts.getValue(XmlDbTags.type_at);
			if (nm == null || tp == null)
				throw new SAXException(XmlDbTags.DataSource_el + " without "
					+ XmlDbTags.name_at +" or " + XmlDbTags.type_at
					+ " attribute");
			DataSourceList dsl = routingSpec.getDatabase().dataSourceList;
			routingSpec.dataSource = dsl.get(nm);
			if (routingSpec.dataSource == null)
			{
				routingSpec.dataSource = new DataSource(nm, tp);
				dsl.add(routingSpec.dataSource);
			}
			hier.pushObjectParser(new DataSourceParser(routingSpec.dataSource));
		}
		else if (localName.equalsIgnoreCase(
			XmlDbTags.RoutingSpecNetworkList_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.RoutingSpecNetworkList_el +
					" without " + XmlDbTags.name_at + " attribute");
			routingSpec.addNetworkListName(nm);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RoutingSpecProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
				throw new SAXException(XmlDbTags.RoutingSpecProperty_el
					+ " without " + XmlDbTags.propertyName_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
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
		if (localName.equalsIgnoreCase(XmlDbTags.RoutingSpecNetworkList_el))
			; // End of empty element.
		else if (localName.equalsIgnoreCase(myName()))
			hier.popObjectParser();
		else
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
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
		case outputFormatTag:
			routingSpec.outputFormat = str;
			break;
		case outputTimeZoneAbbrTag:
			routingSpec.outputTimeZoneAbbr = str;
			break;
		case presentationGroupNameTag:
			routingSpec.presentationGroupName = str;
			break;
		case sinceTimeTag:
			routingSpec.sinceTime = str;
			break;
		case untilTimeTag:
			routingSpec.untilTime = str;
			break;
		case consumerTypeTag:
			routingSpec.consumerType = str;
			break;
		case consumerArgTag:
			routingSpec.consumerArg = str;
			break;
		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			routingSpec.getProperties().setProperty(propName, str);
			propName = null;
			break;
		case lastModifyTimeTag:
/*
MJM 20031023 - Don't use the LMT contained in XML file. It may not agree
with the File.lastModified() calls used elsewhere.
			try
			{
				routingSpec.lastModifyTime = 
					Constants.defaultDateFormat.parse(str);
			}
			catch(Exception e)
			{
				throw new SAXException("Improper date format '" + str
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
*/
			break;
		}
	}

	/**
	 * From TaggedBooleanOwner, called from TaggedBooleanSetter when string
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param value the boolean value
	 */
	public void set( int tag, boolean value )
	{
		switch(tag)
		{
		case enableEquationsTag:
			routingSpec.enableEquations = value;
			break;
		case usePerformanceMeasurmentsTag:
			routingSpec.usePerformanceMeasurements = value;
			break;
		case isProductionTag:
			routingSpec.isProduction = value;
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
		xos.startElement(myName(), XmlDbTags.name_at, routingSpec.getName());

		xos.writeElement(XmlDbTags.isProduction_el,
			routingSpec.isProduction ? "true" : "false");

		if (routingSpec.dataSource != null)
		{
			DataSourceParser p = new DataSourceParser(routingSpec.dataSource);
			p.writeXml(xos);
		}

		xos.writeElement(XmlDbTags.EnableEquations_el,
			""+routingSpec.enableEquations);
		xos.writeElement(XmlDbTags.UsePerformanceMeasurements_el,
			""+routingSpec.usePerformanceMeasurements);
/*
MJM 20031023 - Don't save LMT in XML.
		if (routingSpec.lastModifyTime != null)
			xos.writeElement(XmlDbTags.lastModifyTime_el,
				Constants.defaultDateFormat.format(routingSpec.lastModifyTime));
*/
		if (routingSpec.outputFormat != null)
			xos.writeElement(XmlDbTags.OutputFormat_el,
				routingSpec.outputFormat);
		if (routingSpec.outputTimeZoneAbbr != null)
			xos.writeElement(XmlDbTags.OutputTimeZone_el,
				routingSpec.outputTimeZoneAbbr);
		if (routingSpec.presentationGroupName != null)
			xos.writeElement(XmlDbTags.PresentationGroupName_el,
				routingSpec.presentationGroupName);

		xos.writeElement(XmlDbTags.ConsumerType_el,
			routingSpec.consumerType == null ? "file" : routingSpec.consumerType);
		if (routingSpec.consumerArg != null)
			xos.writeElement(XmlDbTags.ConsumerArg_el,
				routingSpec.consumerArg);

		if (routingSpec.sinceTime != null)
			xos.writeElement(XmlDbTags.SinceTime_el, routingSpec.sinceTime);
		if (routingSpec.untilTime != null)
			xos.writeElement(XmlDbTags.UntilTime_el, routingSpec.untilTime);

		// If source was an XML file, network list names will be filled in
		if (routingSpec.networkListNames.size() > 0)
			for(int i = 0; i < routingSpec.networkListNames.size(); i++)
			{
				String nm = (String)routingSpec.networkListNames.elementAt(i);
				xos.writeElement(XmlDbTags.RoutingSpecNetworkList_el,
					XmlDbTags.name_at, nm, null);
			}
		// Else if source a SQL database, network lists will be filled in.
		else if (routingSpec.networkLists.size() > 0)
			for(int i = 0; i < routingSpec.networkLists.size(); i++)
			{
				NetworkList nl =
					(NetworkList)routingSpec.networkLists.elementAt(i);
				xos.writeElement(XmlDbTags.RoutingSpecNetworkList_el,
					XmlDbTags.name_at, nl.name, null);
			}

		Enumeration e = routingSpec.getProperties().propertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)routingSpec.getProperties().getProperty(nm);
			xos.startElement(XmlDbTags.RoutingSpecProperty_el,
				XmlDbTags.propertyName_at, nm);
			xos.writePCDATA(v);
			xos.endElement(XmlDbTags.RoutingSpecProperty_el);
		}

		xos.endElement(myName());
	}
}
