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
package lrgs.statusxml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.util.TextUtil;
import ilex.xml.*;
import lrgs.apistatus.QualityMeasurement;


/**
This class maps the DECODES XML representation for QualityMeasurement elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class QualityMeasurementXio implements XmlObjectParser, TaggedLongOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/// Top of the parser hierarchy
	private QualityMeasurement qm;

	private static final int ngTag    = 0;
	private static final int ndTag    = 1;
	private static final int nrTag    = 2;

	/**
	  Construct parser.
	  @param qm the QualityMeasurement to populate from XML data
	*/
	public QualityMeasurementXio(QualityMeasurement qm)
	{
		super();
		this.qm = qm;
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.Quality; }

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	/**
	  Called when sub-element seen under Archive Statistics.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the new element
	  @param qname ignored
	  @param atts attributes from the new element
	*/
	public void startElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname, Attributes atts)
		throws SAXException
	{
		if (localName.equalsIgnoreCase(StatusXmlTags.numGood))
			hier.pushObjectParser(new TaggedLongSetter(this, ngTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.numDropped))
			hier.pushObjectParser(new TaggedLongSetter(this, ndTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.numRecovered))
			hier.pushObjectParser(new TaggedLongSetter(this, nrTag));
		else
		{
			log.warn("Invalid element '{}' under {} -- skipped.", localName, myName());
			hier.pushObjectParser(new ElementIgnorer());
		}
	}

	/**
	  Signals the end of the current element.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the element
	  @param qname ignored
	*/
	public void endElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname)
		throws SAXException
	{
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	/** Does nothing. */
    public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	}

	/**
	  From TaggedLongOwner, called from TaggedLongSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case ngTag:
			qm.numGood = (int)value;
			qm.containsData = true;
			break;
		case ndTag:
			qm.numDropped = (int)value;
			qm.containsData = true;
			break;
		case nrTag:
			qm.numRecovered = (int)value;
			qm.containsData = true;
			break;
		}
	}
}