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
 * This class maps the DECODES XML representation for EngineeringUnit elements.
 */
public class EngineeringUnitParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private EngineeringUnit eu; // object that we will build.

	private static final int nameTag = 0;
	private static final int familyTag = 1;
	private static final int measuresTag = 2;

	// Used by dbimport to detect if any new units were parsed.
	public static boolean engineeringUnitsParsed = false;
	
	/**
	 * @param ob the object in which to store the data.
	 */
	public EngineeringUnitParser( EngineeringUnit ob )
	{
		super();
		eu = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.EngineeringUnit_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within EngineeringUnit");
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
		engineeringUnitsParsed = true;
		if (localName.equalsIgnoreCase(XmlDbTags.name_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, nameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Family_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, familyTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.Measures_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, measuresTag));
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
		case nameTag:
			eu.setName(str);
			break;
		case familyTag:
			eu.family = str;
			break;
		case measuresTag:
			eu.measures = str;
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
		xos.startElement(myName(), XmlDbTags.EngineeringUnit_abbr_at, eu.abbr);

		if (eu.getName() != null)
			xos.writeElement(XmlDbTags.name_el, eu.getName());
		if (eu.family != null)
			xos.writeElement(XmlDbTags.Family_el, eu.family);
		if (eu.measures != null)
			xos.writeElement(XmlDbTags.Measures_el, eu.measures);

		xos.endElement(myName());
	}
}