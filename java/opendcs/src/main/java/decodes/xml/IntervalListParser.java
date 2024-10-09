/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import decodes.db.IntervalList;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import java.util.Calendar;

import ilex.xml.*;
import opendcs.opentsdb.Interval;

/**
 * This class maps the DECODES XML files into the object model.
 */
public class IntervalListParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
{
	private IntervalList intervals = null;
	private Interval workingInterval = null;

	/**
	  Note: Normally we would store a reference to the Java object that
	  we're building, but since EnumList is a singleton with only static
	  members, we don't need to do this.
	  @param db The database to read & write.
	*/
	public IntervalListParser(IntervalList intervals)
	{
		super();
		this.intervals = intervals;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.IntervalList_el; }

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
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName,
		String qname, Attributes atts )
			throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.Interval_el))
		{
			String intvName = atts.getValue(XmlDbTags.name_at);
			if (intvName == null)
				throw new SAXException(XmlDbTags.Interval_el + " without " +
					XmlDbTags.name_at + " attribute");

			workingInterval = new Interval(intvName);
			hier.pushObjectParser(new TaggedStringSetter(this, 1));
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
		xos.startElement(XmlDbTags.IntervalList_el);

		for(Interval intv : intervals.getList())
			xos.writeElement(XmlDbTags.Interval_el, XmlDbTags.name_at, intv.getName(),
				IntervalCodes.getCalConstName(intv.getCalConstant())
				+ "*" + intv.getCalMultiplier());
		xos.endElement(XmlDbTags.IntervalList_el);
	}

	@Override
	public void set(int tag, String value) throws SAXException
	{
		if (workingInterval == null)
			throw new SAXException("Unexpected set '" + value + "' in IntervalList.");
		IntervalIncrement ii = IntervalCodes.getIntervalCalIncr(value);
		if (ii == null)
			Logger.instance().warning("Invalid content in Interval '" + workingInterval.getName() + "': '"
				+ value + "' -- ignored.");
		else
		{
			workingInterval.setCalConstant(ii.getCalConstant());
			workingInterval.setCalMultiplier(ii.getCount());
			intervals.add(workingInterval);
			workingInterval = null;
		}
	}
}
