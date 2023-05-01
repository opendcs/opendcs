/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/09/02 13:09:06  mjmaloney
*  javadoc
*
*  Revision 1.1  2004/05/04 18:03:58  mjmaloney
*  Moved from statusgui package to here.
*
*/
package lrgs.statusxml;

import java.io.IOException;
import java.util.Iterator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.xml.*;
import ilex.util.*;
import lrgs.apistatus.QualityMeasurement;


/**
This class maps the DECODES XML representation for QualityMeasurement elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class QualityMeasurementXio
	implements XmlObjectParser, TaggedLongOwner
{
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
			Logger.instance().log(Logger.E_WARNING,
				"Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
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
