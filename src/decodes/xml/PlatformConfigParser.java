/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Collection;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.AsciiUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for PlatformConfig elements.
 */
public class PlatformConfigParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private PlatformConfig platformConfig; // object that we will build.

	private static final int descriptionTag = 0;

	/**
	 * @param platformConfig the object in which to store the data.
	 */
	public PlatformConfigParser( PlatformConfig platformConfig )
	{
		super();
		this.platformConfig = platformConfig;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.PlatformConfig_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within PlatformConfig");
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
		if (localName.equalsIgnoreCase(XmlDbTags.description_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.EquipmentModel_el + " without "
					+ XmlDbTags.name_at +" attribute");
			EquipmentModel eqm = new EquipmentModel(nm);
			platformConfig.equipmentModel = eqm;
			platformConfig.getDatabase().equipmentModelList.add(eqm);
			hier.pushObjectParser(new EquipmentModelParser(eqm));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.ConfigSensor_el))
		{
			String ns = atts.getValue(XmlDbTags.sensorNumber_at);
			int snum = -1;
			try { snum = Integer.parseInt(ns); }
			catch(NumberFormatException e)
			{
				throw new SAXException("Sensor number must be an integer");
			}
			ConfigSensor cs = new ConfigSensor(platformConfig, snum);
			platformConfig.addSensor(cs);
			hier.pushObjectParser(new ConfigSensorParser(cs));
		}
		// Installed Platform DCPML files will link from TransportMedium to
		// DecodesScript. However, files created for archive will link from
		// PlatformConfig to DecodesScript. Hence we parse here:
		else if (localName.equalsIgnoreCase(XmlDbTags.DecodesScript_el))
		{
			String nm = atts.getValue(XmlDbTags.DecodesScript_scriptName_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.DecodesScript_el + " without "
					+ XmlDbTags.DecodesScript_scriptName_at +" attribute");

            DecodesScript ds = new DecodesScript(platformConfig, nm);

			platformConfig.addScript(ds);

			hier.pushObjectParser(new DecodesScriptParser(ds));
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
		if (!localName.equalsIgnoreCase(myName()))
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
		case descriptionTag:
			str = TextUtil.collapseWhitespace(str);
			str = new String(AsciiUtil.ascii2bin(str));
			platformConfig.description = str;
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
		xos.startElement(myName(), XmlDbTags.PlatformConfig_configName_at,
			platformConfig.configName);
		if (platformConfig.description != null)
			xos.writeElement(XmlDbTags.description_el,
				AsciiUtil.bin2ascii(platformConfig.description.getBytes()));
		if (platformConfig.equipmentModel != null)
		{
			EquipmentModelParser p = new EquipmentModelParser(
				platformConfig.equipmentModel);
			p.writeXml(xos);
		}
		for(Iterator it = platformConfig.getSensors(); it.hasNext(); )
		{
			ConfigSensor cs = (ConfigSensor)it.next();
			ConfigSensorParser p = new ConfigSensorParser(cs);
			p.writeXml(xos);
		}

		for(Iterator it = platformConfig.getScripts(); it.hasNext(); )
		{
			DecodesScriptParser p =
				new DecodesScriptParser((DecodesScript)it.next());
			p.writeXml(xos);
		}

		xos.endElement(myName());
	}
}
