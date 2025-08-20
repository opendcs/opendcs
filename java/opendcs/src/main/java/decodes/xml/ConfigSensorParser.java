/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.StringPair;

import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Site elements.
 */
public class ConfigSensorParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, 
	TaggedDoubleOwner, TaggedLongOwner
{
	private ConfigSensor configSensor; // object that we will build.
	private String propName;     // Tmp storage while waiting for value parse.

	private static final int sensorNameTag = 0;
	private static final int recordingModeTag = 1;
	private static final int recordingIntervalTag = 2;
	private static final int timeOfFirstSampleTag = 3;
	private static final int propertyTag = 4;
	private static final int absoluteMinTag = 5;
	private static final int absoluteMaxTag = 6;
	private static final int usgsStatCodeTag = 7;

	/**
	 * @param cs The ConfigSensor in which to store data
	 */
	public ConfigSensorParser( ConfigSensor cs )
	{
		super();
		configSensor = cs;
		propName = null;
	}

	/**
	 * @return name of top element for this parser.
	 */
	public String myName( ) { return XmlDbTags.ConfigSensor_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within ConfigSensor");
	}

	/**
	 * @param hier the stack of parsers
	 * @param namespaceURI namespaceURI
	 * @param localName name of element
	 * @param qname ignored
	 * @param atts attributes for this element
	 * @throws SAXException on parse error
	 */
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.sensorName_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, sensorNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.recordingMode_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, 
				recordingModeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.recordingInterval_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, 
				recordingIntervalTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.timeOfFirstSample_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, 
				timeOfFirstSampleTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.AbsoluteMin_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, 
				absoluteMinTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.AbsoluteMax_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, 
				absoluteMaxTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
		{
			String st = atts.getValue(XmlDbTags.DataType_standard_at);
			if (st == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_standard_at +" attribute");
			String cd = atts.getValue(XmlDbTags.DataType_code_at);
			if (cd == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_code_at +" attribute");
			DataType dt = DataType.getDataType(st, cd);
			
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm != null)
				dt.setDisplayName(nm);

			configSensor.addDataType(dt);
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.ConfigSensorProperty_el))
		{
			propName = atts.getValue(XmlDbTags.propertyName_at);
			if (propName == null)
				throw new SAXException(XmlDbTags.ConfigSensorProperty_el 
					+ " without " + XmlDbTags.propertyName_at +" attribute");
			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el))
		{
			String nm = atts.getValue(XmlDbTags.name_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.EquipmentModel_el + " without "
					+ XmlDbTags.name_at +" attribute");
			configSensor.equipmentModel = new EquipmentModel(nm);
			hier.pushObjectParser(new EquipmentModelParser(
				configSensor.equipmentModel));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.UsgsStatCode_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,usgsStatCodeTag));
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
	 * Typically this should cause your parser to pop the stack in the
	 * hierarchy. Then do whatever cleanup or finalizing is necessary.
	 * @param hier the stack of parsers
	 * @param namespaceURI ignored
	 * @param localName element that is ending
	 * @param qname ignored
	 */
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
			;	// End of empty DataType element.
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
		case sensorNameTag:
			//if (!TextUtil.containsNoWhitespace(str))
			//	throw new SAXException(XmlDbTags.sensorName_el + 
			//		" must be non-blank and contain no white-space.");
			configSensor.sensorName = str;
			break;
		case recordingModeTag:
			{
				configSensor.recordingMode = Constants.recordingModeFixed;
				if (str != null && str.length() > 0)
				{
					char m = Character.toUpperCase(str.charAt(0));
					if (m == Constants.recordingModeFixed
				 	|| m == Constants.recordingModeVariable
				 	|| m == Constants.recordingModeUndefined)
						configSensor.recordingMode = m;
					else
						throw new SAXException("Illegal recordingMode value");
				}
				break;
			}
		case recordingIntervalTag:
			try
			{
				configSensor.recordingInterval = 
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException e)
			{
				throw new SAXException("Illegal recordingInterval: "
					+ e.toString());
			}
			break;
		case timeOfFirstSampleTag:
			try
			{
				configSensor.timeOfFirstSample = 
					IDateFormat.getSecondOfDay(str);
			}
			catch(IllegalArgumentException e)
			{
				throw new SAXException("Illegal timeOfFirstSample: "
					+ e.toString());
			}
			break;

		case propertyTag:
			if (propName == null)
				throw new SAXException("Property value without name!");
			str = str.trim();
			if (propName.equalsIgnoreCase("StatisticsCode"))
				configSensor.setUsgsStatCode(str.length() > 0 ? str : null);
			else
				configSensor.getProperties().setProperty(propName, str);
			propName = null;
			break;
		case usgsStatCodeTag:
			str = str.trim();
			configSensor.setUsgsStatCode(str.length() > 0 ? str : null);
			break;
		}
	}


	/**
	 * from TaggedDoubleOwner interface
	 * @param tag the integer tag
	 * @param value the value
	 */
	public void set( int tag, double value )
	{
		switch(tag)
		{
		case absoluteMinTag:
			configSensor.absoluteMin = value;
			break;
		case absoluteMaxTag:
			configSensor.absoluteMax = value;
			break;
		}
	}

	/**
	 * from TaggedLongOwner interface.
	 * @param tag the integer tag
	 * @param value the value
	 */
	public void set( int tag, long value )
	{
		switch(tag)
		{
		}
	}

	/**
	 * Writes this enumeration structure to an XML file.
	 * @param xos the output stream
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.sensorNumber_at,
			"" + configSensor.sensorNumber);
		if (configSensor.sensorName != null)
			xos.writeElement(XmlDbTags.sensorName_el, configSensor.sensorName);
		for(Iterator<DataType> it = configSensor.getDataTypes(); it.hasNext(); )
		{
			DataType dt = it.next();
			
			String std = dt.getStandard();
			String cod = dt.getCode();
			String nm = dt.getDisplayName();

			if (nm == null)
				xos.startElement(XmlDbTags.DataType_el, 
					XmlDbTags.DataType_standard_at, std,
					XmlDbTags.DataType_code_at, cod);
			else
			{
				StringPair sp[] = new StringPair[3];
				sp[0] = new StringPair(XmlDbTags.DataType_standard_at, std);
				sp[1] = new StringPair(XmlDbTags.DataType_code_at, cod);
				sp[2] = new StringPair(XmlDbTags.name_at, nm);
				xos.startElement(XmlDbTags.DataType_el, sp);
			}
			xos.endElement(XmlDbTags.DataType_el);
		}
		if (configSensor.recordingMode != Constants.recordingModeUndefined)
			xos.writeElement(XmlDbTags.recordingMode_el, 
				"" + configSensor.recordingMode);
		if (configSensor.recordingInterval != -1)
			xos.writeElement(XmlDbTags.recordingInterval_el, 
				IDateFormat.printSecondOfDay(configSensor.recordingInterval, true));
		if (configSensor.timeOfFirstSample != -1)
			xos.writeElement(XmlDbTags.timeOfFirstSample_el, 
				IDateFormat.printSecondOfDay(configSensor.timeOfFirstSample, true));

		if (configSensor.absoluteMin != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.AbsoluteMin_el, 
				""+configSensor.absoluteMin);

		if (configSensor.absoluteMax != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.AbsoluteMax_el, 
				""+configSensor.absoluteMax);

		boolean wroteStatCode = false;
		String c = configSensor.getUsgsStatCode();
		if (c != null)
		{
			wroteStatCode = true;
			xos.writeElement(XmlDbTags.UsgsStatCode_el, c);
		}

		if (configSensor.equipmentModel != null)
		{
			EquipmentModelParser p = new EquipmentModelParser(
				configSensor.equipmentModel);
			p.writeXml(xos);
		}

		Enumeration e = configSensor.getProperties().propertyNames();
		while(e.hasMoreElements())
		{
			String nm = (String)e.nextElement();
			String v = (String)configSensor.getProperties().getProperty(nm);
			if (nm.equalsIgnoreCase("StatisticsCode"))
			{
				if (!wroteStatCode)
					xos.writeElement(XmlDbTags.UsgsStatCode_el, v);
			}
			else
				xos.writeElement(XmlDbTags.ConfigSensorProperty_el, 
					XmlDbTags.propertyName_at, nm, v);
		}
		xos.endElement(myName());
	}
}
