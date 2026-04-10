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
* TaggedBooleanSetter is used to set simple boolean elements contained in
* an object.
*/
public class TaggedBooleanSetter implements XmlObjectParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	StringBuffer sb;
	TaggedBooleanOwner owner;
	int tag;

	/**
	* Constructor
	* @param owner call when content parsed
	* @param tag the tag to pass
	*/
	public TaggedBooleanSetter( TaggedBooleanOwner owner, int tag )
	{
		sb = new StringBuffer();
		this.owner = owner;
		this.tag = tag;
	}

	/**
	* Character content is concatenated.
	*/
	public void characters( char[] ch, int start, int length )
	{
		String s = new String(ch,start,length);
		sb.append(s.trim());
	}

	/**
	Not expecting subordinate elements! Issue warning and push Ignorer.
	*/
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		log.warn("Received subordinate element '{}' when expecting simple boolean -- ignored.", localName);
		hier.pushObjectParser(new ElementIgnorer());
	}


	/**
	* Parse collected characters and call the owner with the tag and value.
	*/
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		String s = sb.toString();
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"))
			owner.set(tag, true);
		else
			owner.set(tag, false);
		hier.popObjectParser();
	}

	/**
	* Does nothing.
	* @param ch
	* @param start
	* @param length
	*/
	public void ignorableWhitespace( char[] ch, int start, int length )
	{
	}
}