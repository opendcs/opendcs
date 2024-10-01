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
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML files into the object model.
 */
public class EngineeringUnitListParser implements XmlObjectParser, XmlObjectWriter
{
	/**
	Note: Normally we would store a reference to the Java object that 
	we're building, but since EngineeringUnitList is a singleton with 
	only static members, we don't need to do this.
	*/
	public EngineeringUnitListParser( )
	{
		super();
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.EngineeringUnitList_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within EngineeringUnitList");
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
		if (localName.equalsIgnoreCase(XmlDbTags.EngineeringUnit_el))
		{
			String abbr = atts.getValue(XmlDbTags.EngineeringUnit_abbr_at);
			if (abbr== null)
				throw new SAXException(myName() + " without " +
					XmlDbTags.EngineeringUnit_abbr_at + " attribute");

			EngineeringUnit eu = EngineeringUnit.getEngineeringUnit(abbr);
			hier.pushObjectParser(new EngineeringUnitParser(eu));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.UnitConverter_el))
		{
			String from=atts.getValue(XmlDbTags.UnitConverter_fromUnitsAbbr_at);
			String to = atts.getValue(XmlDbTags.UnitConverter_toUnitsAbbr_at);
			if (from == null || to == null)
				throw new SAXException(XmlDbTags.UnitConverter_el +
					" requires attributes " 
					+ XmlDbTags.UnitConverter_fromUnitsAbbr_at
					+ " and " + XmlDbTags.UnitConverter_toUnitsAbbr_at);

			UnitConverterDb uc = new UnitConverterDb(from, to);
			Database.getDb().unitConverterSet.addDbConverter(uc);
			hier.pushObjectParser(new UnitConverterParser(uc));
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
		xos.startElement(myName());

		for(Iterator it = Database.getDb().engineeringUnitList.iterator();
			it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			EngineeringUnitParser p = new EngineeringUnitParser(eu);
			p.writeXml(xos);
		}

		for(Iterator it = Database.getDb().unitConverterSet.iteratorDb();
			it.hasNext(); )
		{
			Object ob = it.next();

			// Don't save derived or executable converters.
			if (ob instanceof UnitConverterDb)
			{
				UnitConverterDb uc = (UnitConverterDb)ob;
				UnitConverterParser p = new UnitConverterParser(uc);
				p.writeXml(xos);
			}
		}

		xos.endElement(myName());
	}
}
