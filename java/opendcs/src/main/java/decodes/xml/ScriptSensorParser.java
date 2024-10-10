/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/30 14:49:33  mjmaloney
*  Added javadocs
*
*  Revision 1.5  2003/11/15 20:08:27  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.4  2001/01/13 01:50:28  mike
*  dev
*
*  Revision 1.3  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.2  2000/12/31 23:12:50  mike
*  dev
*
*  Revision 1.1  2000/12/31 22:30:47  mike
*  dev
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
 * This class maps the DECODES XML representation for ScriptSensor elements.
 */
public class ScriptSensorParser implements XmlObjectParser, XmlObjectWriter
{
	private ScriptSensor scriptSensor; // object that we will build.

	/**
	 * @param ss the object in which to store the data.
	 */
	public ScriptSensorParser( ScriptSensor ss )
	{
		super();
		scriptSensor = ss;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.ScriptSensor_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within ScriptSensor");
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
		if (localName.equalsIgnoreCase(XmlDbTags.UnitConverter_el))
		{
			String nm = atts.getValue(XmlDbTags.UnitConverter_toUnitsAbbr_at);
			scriptSensor.rawConverter = new UnitConverterDb("raw", nm);
			hier.pushObjectParser(new UnitConverterParser(
				scriptSensor.rawConverter));
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
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.sensorNumber_at, 
			"" + scriptSensor.sensorNumber);
		if (scriptSensor.rawConverter != null)
		{
			UnitConverterParser p = new UnitConverterParser(
				scriptSensor.rawConverter);
			p.writeXml(xos);
		}
		xos.endElement(myName());
	}
}
