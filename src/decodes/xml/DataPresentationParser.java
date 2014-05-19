/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2006/05/11 13:32:35  mmaloney
*  DataTypes are now immutable! Modified all references. Modified SQL IO code.
*
*  Revision 1.3  2004/08/30 14:49:28  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2003/11/15 20:08:22  mjmaloney
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

import java.text.NumberFormat;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for DataPresentation elements.
 */
public class DataPresentationParser 
	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, 
	TaggedLongOwner, TaggedDoubleOwner
{
	private DataPresentation dataPresentation; // object that we will build.

	private static final int equipmentModelNameTag = 0;
	private static final int unitsAbbrTag = 1;
	private static final int maxDecimalsTag = 2;
	private static final int minValueTag = 3;
	private static final int maxValueTag = 4;

	/**
	 * @param ob the object in which to store the data.
	 */
	public DataPresentationParser( DataPresentation ob )
	{
		super();
		this.dataPresentation = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.DataPresentation_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within DataPresentation");
	}

	/**
	 * @param hier the stack of parsers
	 * @param namespaceURI namespaceURI
	 * @param localName name of element
	 * @param qname ignored
	 * @param atts attributes for this element
	 * @throws SAXException on parse error
	 */
	public void startElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname, Attributes atts ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
		{
			String st = atts.getValue(XmlDbTags.DataType_standard_at);
			if (st == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_standard_at +" attribute");
			String cd = atts.getValue(XmlDbTags.DataType_code_at);
			if (cd == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_code_at +" attribute");
			dataPresentation.setDataType(DataType.getDataType(st, cd));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.UnitsAbbr_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, unitsAbbrTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.EquipmentModelName_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, 
				equipmentModelNameTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.MaxDecimals_el))
		{
			hier.pushObjectParser(new TaggedLongSetter(this, 
				maxDecimalsTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.MinValue_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, minValueTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.MaxValue_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, maxValueTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.RoundingRule_el))
		{
			// silently ignore
			hier.pushObjectParser(new ElementIgnorer());
//			RoundingRule rr = new RoundingRule(dataPresentation);
//			dataPresentation.addRoundingRule(rr);
//			hier.pushObjectParser(new RoundingRuleParser(rr));
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
	 * Typically this should cause your parser to pop the stack in the
	 * hierarchy. Then do whatever cleanup or finalizing is necessary.
	 * @param hier the stack of parsers
	 * @param namespaceURI ignored
	 * @param localName element that is ending
	 * @param qname ignored
	 */
	public void endElement( XmlHierarchyParser hier, String namespaceURI, String localName, String qname ) throws SAXException
	{
		if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
			;	// End of empty DataType element.
		else if (localName.equalsIgnoreCase(myName()))
			hier.popObjectParser();
		else
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
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
		case unitsAbbrTag:
			dataPresentation.setUnitsAbbr(str);
			break;
//		case equipmentModelNameTag:
//			dataPresentation.setEquipmentModelName(str);
//			break;
		}
	}

	/**
	 * From TaggedLongOwner, called from TaggedLongSetter when long-int
	 * elements are parsed.
	 * @param tag integer tag
	 * @param val value
	 */
	public void set( int tag, long val )
	{
		switch(tag)
		{
		case maxDecimalsTag:
			dataPresentation.setMaxDecimals((int)val);
			break;
		}
	}

	/**
	 * Writes this enumeration structure to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName());

		if (dataPresentation.getDataType() != null)
		{
			xos.startElement(XmlDbTags.DataType_el, 
				XmlDbTags.DataType_standard_at, 
					dataPresentation.getDataType().getStandard(),
				XmlDbTags.DataType_code_at, dataPresentation.getDataType().getCode());
			xos.endElement(XmlDbTags.DataType_el);
		}

		if (dataPresentation.getUnitsAbbr() != null)
			xos.writeElement(XmlDbTags.UnitsAbbr_el,dataPresentation.getUnitsAbbr());

//		if (dataPresentation.getEquipmentModelName() != null)
//			xos.writeElement(XmlDbTags.EquipmentModelName_el,
//				dataPresentation.getEquipmentModelName());

		if (dataPresentation.getMaxDecimals() != Integer.MAX_VALUE)
			xos.writeElement(XmlDbTags.MaxDecimals_el,
				"" + dataPresentation.getMaxDecimals());

//		for(int i = 0; i < dataPresentation.roundingRules.size(); i++)
//		{
//			RoundingRule rr = (RoundingRule)
//				dataPresentation.roundingRules.elementAt(i);
//			RoundingRuleParser p = new RoundingRuleParser(rr);
//			p.writeXml(xos);
//		}
		NumberFormat numFmt = NumberFormat.getNumberInstance();
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(5);
		
		if (dataPresentation.getMinValue() != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.MinValue_el, 
				numFmt.format(dataPresentation.getMinValue()));
		if (dataPresentation.getMaxValue() != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.MaxValue_el, 
				numFmt.format(dataPresentation.getMaxValue()));

		xos.endElement(myName());
	}

	@Override
	public void set(int tag, double value)
	{
		switch(tag)
		{
		case minValueTag:
			dataPresentation.setMinValue(value);
			break;
		case maxValueTag:
			dataPresentation.setMaxValue(value);
			break;
		}
	}
}
