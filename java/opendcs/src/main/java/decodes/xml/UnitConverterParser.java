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
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for UnitConverter elements.
 */
public class UnitConverterParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedDoubleOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private UnitConverterDb dbConverter; // object that we will build.

	private static final int algorithmTag = 0;
	private static final int aTag = 1;
	private static final int bTag = 2;
	private static final int cTag = 3;
	private static final int dTag = 4;
	private static final int eTag = 5;
	private static final int fTag = 6;

	// Used by dbimport to detect if any unit converters were parsed.
	public static boolean unitConvertersParsed = false;

	/**
	 * @param ob the object in which to store the data.
	 */
	public UnitConverterParser( UnitConverterDb ob )
	{
		super();
		dbConverter = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.UnitConverter_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within UnitConverter");
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
		unitConvertersParsed = true;
		if (localName.equalsIgnoreCase(XmlDbTags.algorithm_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, algorithmTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.a_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, aTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.b_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, bTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.c_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, cTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.d_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, dTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.e_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, eTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.f_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, fTag));
		}
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
	 * Called after start of new element for this parser is detected.
	 * @throws SAXException on parse error
	 */
	public void set( int tag, String str ) throws SAXException
	{
		switch(tag)
		{
		case algorithmTag:
			dbConverter.algorithm = str;
			break;
		}
	}

	/**
	 * From TaggedDoubleOwner, called from TaggedDoubleSetter when Double
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param v the double value
	 */
	public void set( int tag, double v )
	{
		switch(tag)
		{
		case aTag: dbConverter.coefficients[0] = v; break;
		case bTag: dbConverter.coefficients[1] = v; break;
		case cTag: dbConverter.coefficients[2] = v; break;
		case dTag: dbConverter.coefficients[3] = v; break;
		case eTag: dbConverter.coefficients[4] = v; break;
		case fTag: dbConverter.coefficients[5] = v; break;
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		if (dbConverter.fromAbbr == null
		 || dbConverter.toAbbr == null)
			return;
		xos.startElement(myName(),
			XmlDbTags.UnitConverter_fromUnitsAbbr_at, dbConverter.fromAbbr,
			XmlDbTags.UnitConverter_toUnitsAbbr_at, dbConverter.toAbbr);

		if (dbConverter.algorithm != null)
			xos.writeElement(XmlDbTags.algorithm_el, dbConverter.algorithm);

		if (dbConverter.coefficients[0] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.a_el, ""+dbConverter.coefficients[0]);
		if (dbConverter.coefficients[1] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.b_el, ""+dbConverter.coefficients[1]);
		if (dbConverter.coefficients[2] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.c_el, ""+dbConverter.coefficients[2]);
		if (dbConverter.coefficients[3] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.d_el, ""+dbConverter.coefficients[3]);
		if (dbConverter.coefficients[4] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.e_el, ""+dbConverter.coefficients[4]);
		if (dbConverter.coefficients[5] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.f_el, ""+dbConverter.coefficients[5]);

		xos.endElement(myName());
	}
}
