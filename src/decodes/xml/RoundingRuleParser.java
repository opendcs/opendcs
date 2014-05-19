/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:49:32  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2003/11/15 20:08:27  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.1  2001/03/18 18:24:36  mike
*  Implemented PerformanceMeasurments objects & parsers.
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for RoundingRule elements.
 */
public class RoundingRuleParser implements XmlObjectParser, XmlObjectWriter, TaggedLongOwner, TaggedDoubleOwner
{
	private RoundingRule roundingRule; // object that we will build.

	private static final int significantDigitsTag = 0;
	private static final int maxDecimalsTag = 1;
	private static final int upperLimitTag = 2;

	/**
	 * @param ob the object in which to store the data.
	 */
	public RoundingRuleParser( RoundingRule ob )
	{
		super();
		roundingRule = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.RoundingRule_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within RoundingRule");
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
		if (localName.equalsIgnoreCase(XmlDbTags.SignificantDigits_el))
			hier.pushObjectParser(new TaggedLongSetter(this, 
				significantDigitsTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.MaxDecimals_el))
			hier.pushObjectParser(new TaggedLongSetter(this, maxDecimalsTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.UpperLimit_el))
			hier.pushObjectParser(new TaggedDoubleSetter(this, upperLimitTag));
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
	 * From TaggedDoubleOwner, called from TaggedDoubleSetter when Double
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param v the double value
	 */
	public void set( int tag, double v )
	{
		switch(tag)
		{
		case upperLimitTag: roundingRule.setUpperLimit(v); break;
		}
	}

	/**
	  Called from TaggedLongSetter.
	  @param tag integer tag defined above
	  @param v long value
	*/
	public void set( int tag, long v )
	{
		switch(tag)
		{
		case significantDigitsTag: 
			roundingRule.sigDigits = (int)v; 
			break;
		case maxDecimalsTag: 
			// Ignore max Decimals in rounding rule -- it is no longer used.
			//roundingRule.maxDecimals = (int)v; 
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
		xos.startElement(myName());

		xos.writeElement(XmlDbTags.SignificantDigits_el, 
			""+roundingRule.sigDigits);

		// Max Decimals no longer used in RR
		//xos.writeElement(XmlDbTags.MaxDecimals_el, 
		//	""+roundingRule.maxDecimals);

		if (roundingRule.getUpperLimit() != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.UpperLimit_el, 
				""+roundingRule.getUpperLimit());

		xos.endElement(myName());
	}
}
