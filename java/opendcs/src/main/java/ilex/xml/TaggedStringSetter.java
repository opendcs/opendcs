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
package ilex.xml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
* TaggedStringSetter is used to set simple string elements contained in
* an object.
*/
public class TaggedStringSetter implements XmlObjectParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	StringBuffer sb;
	TaggedStringOwner owner;
	int tag;
	boolean trailingSpace;

	/**
	* Constructor
	* @param owner the owner to call when content is parsed.
	* @param tag the tag to return to the owner.
	*/
	public TaggedStringSetter( TaggedStringOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
		trailingSpace = false;
	}

	/**
	* Accumulate characters until end element seen.
	* @param ch the characters
	* @param start the start
	* @param length the length
	*/
	public void characters( char[] ch, int start, int length )
	{
		int end = start + length;
		int firstNonSpace = start;
		for(; firstNonSpace < end; firstNonSpace++)
			if (!Character.isWhitespace(ch[firstNonSpace]))
				break;
		if (firstNonSpace == end)  // String is all whitespace
		{
			trailingSpace = true;
			return;
		}

		// There must be at least one non-space character.
		int lastNonSpace = end-1;
		for(; lastNonSpace >= firstNonSpace; lastNonSpace--)
			if (!Character.isWhitespace(ch[lastNonSpace]))
				break;

		if (sb.length() > 0
		 && (trailingSpace || firstNonSpace > start))
			sb.append(' ');
		sb.append(ch, firstNonSpace, lastNonSpace-firstNonSpace+1);
		trailingSpace = lastNonSpace < end-1;
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		log.warn("Received subordinate element '{}' when expecting simple string -- ignored.", localName);
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		owner.set(tag, sb.toString());
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}