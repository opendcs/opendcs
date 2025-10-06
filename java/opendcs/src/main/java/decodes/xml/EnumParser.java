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
import java.util.Collection;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for Enum elements into
 * Enum objects.
 */
public class EnumParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private decodes.db.DbEnum dbenum; // Enum object that we will build.
	private static final int defaultValueTag = 1;

	/** This flag is used by importer to detect whether enums were modified. */
	public static boolean enumParsed = false;

	/**
	 * @param dbenum the object in which to store the data.
	 */
	public EnumParser( decodes.db.DbEnum dbenum )
	{
		super();
		this.dbenum = dbenum;
		enumParsed = true;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.Enum_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected within Enum");
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
		if (localName.equalsIgnoreCase(XmlDbTags.EnumValue_el))
		{
			String key = atts.getValue(XmlDbTags.EnumValue_value_at);
			if (key == null)
				throw new SAXException( "Enum without "
					+ XmlDbTags.EnumValue_value_at +" attribute");
			EnumValue v = null;
			try
			{
				v = dbenum.addValue(key, null, null, null);
			}
			catch(ValueAlreadyDefinedException ex)
			{
				throw new SAXException("Enum '" + key + "' already defined.", ex);
			}
			hier.pushObjectParser(new EnumValueParser(v));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EnumDefaultValue_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this,defaultValueTag));
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
		case defaultValueTag:
			dbenum.setDefault(str);
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
		xos.startElement( myName(),  XmlDbTags.name_at, dbenum.enumName);

		String def = dbenum.getDefault();
		if (def != null && def.trim().length() > 0)
			xos.writeElement(XmlDbTags.EnumDefaultValue_el, def);

		Collection col = dbenum.values();
		for(Iterator it = col.iterator(); it.hasNext(); )
		{
			EnumValue ev = (EnumValue)it.next();
			EnumValueParser p = new EnumValueParser(ev);
			p.writeXml(xos);
		}
		xos.endElement(myName());
	}
}