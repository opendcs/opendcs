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
* TaggedDoubleSetter is used to set simple long or integer elements contained in
* an object.
*/
public class TaggedDoubleSetter implements XmlObjectParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	StringBuffer sb;
	TaggedDoubleOwner owner;
	int tag;

	/**
	* Constructor
	* @param owner the owner to call when content is parsed.
	* @param tag the tag to return to the owner.
	*/
	public TaggedDoubleSetter( TaggedDoubleOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
	}

	/**
	* Accumulate characters until end element seen.
	* @param ch the characters
	* @param start the start
	* @param length the length
	*/
	public void characters( char[] ch, int start, int length )
	{
		sb.append(ch, start, length);
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		log.warn("Received subordinate element '{}' when expecting simple number -- ignored.", localName);
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		try
		{
			owner.set(tag, Double.parseDouble(sb.toString().trim()));
		}
		catch(NumberFormatException ex)
		{
			throw new SAXException("Floating point number expected", ex);
		}
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}