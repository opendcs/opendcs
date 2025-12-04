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
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML files into the object model.
 */
public class EnumListParser implements XmlObjectParser, XmlObjectWriter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	final EnumList enums;
	DbEnum currDbEnum;

	/**
	  Note: Normally we would store a reference to the Java object that
	  we're building, but since EnumList is a singleton with only static
	  members, we don't need to do this.

	  NOTE2: as we move towards new daos we're to first pretend the above isn't true... 
	         add then make it so.
	  @param enums The enums to read and write.
	*/
	public EnumListParser(EnumList enums)
	{
		super();
		this.enums = enums;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.EnumList_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected within EnumList");
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
		if (localName.equalsIgnoreCase(XmlDbTags.Enum_el))
		{
			String enumName = atts.getValue(XmlDbTags.name_at);
			if (enumName == null)
				throw new SAXException("Enum without " +
					XmlDbTags.name_at + " attribute");
			
			currDbEnum = new decodes.db.DbEnum(enumName);
			enums.addEnum(currDbEnum);
			hier.pushObjectParser(new EnumParser(currDbEnum));
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
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(XmlDbTags.EnumList_el);

		
		for(Iterator<DbEnum> it = enums.iterator(); it.hasNext(); )
		{
			decodes.db.DbEnum dbenum = it.next();
			EnumParser p = new EnumParser(dbenum);
			p.writeXml(xos);
		}
		xos.endElement(XmlDbTags.EnumList_el);
	}
}