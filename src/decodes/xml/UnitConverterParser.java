/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2011/08/01 00:58:22  mmaloney
*  Don't XML write if from or to is null.
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2007/12/11 01:05:22  mmaloney
*  javadoc cleanup
*
*  Revision 1.7  2004/08/30 14:49:34  mjmaloney
*  Added javadocs
*
*  Revision 1.6  2003/11/15 20:08:28  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.5  2001/08/12 17:36:57  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.4  2001/01/13 17:22:48  mike
*  Added parsers for EngineeringUnits
*
*  Revision 1.3  2001/01/13 01:50:28  mike
*  dev
*
*  Revision 1.2  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.1  2000/12/31 23:13:03  mike
*  created
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
 * This class maps the DECODES XML representation for UnitConverter elements.
 */
public class UnitConverterParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedDoubleOwner
{
	private UnitConverterDb dbConverter; // object that we will build.

	private static final int algorithmTag = 0;
	private static final int aTag = 1;
	private static final int bTag = 2;
	private static final int cTag = 3;
	private static final int dTag = 4;
	private static final int eTag = 5;
	private static final int fTag = 6;

	/**
	 * @param ob the object in which to store the data.
	 */
	public UnitConverterParser( UnitConverterDb ob )
	{
		super();
		dbConverter = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.UnitConverter_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within UnitConverter");
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
		if (localName.equalsIgnoreCase(XmlDbTags.algorithm_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, algorithmTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.a_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, aTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.b_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, bTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.c_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, cTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.d_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, dTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.e_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, eTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.f_el))
		{
			hier.pushObjectParser(new TaggedDoubleSetter(this, fTag));
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
	 * Called after start of new element for this parser is detected.
	 * @throws SAXException on parse error
	 */
	public void set( int tag, String str ) throws SAXException
	{
		switch(tag)
		{
		case algorithmTag:
			dbConverter.algorithm = str;
			break;
		}
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
		case aTag: dbConverter.coefficients[0] = v; break;
		case bTag: dbConverter.coefficients[1] = v; break;
		case cTag: dbConverter.coefficients[2] = v; break;
		case dTag: dbConverter.coefficients[3] = v; break;
		case eTag: dbConverter.coefficients[4] = v; break;
		case fTag: dbConverter.coefficients[5] = v; break;
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		if (dbConverter.fromAbbr == null
		 || dbConverter.toAbbr == null)
			return;
		xos.startElement(myName(), 
			XmlDbTags.UnitConverter_fromUnitsAbbr_at, dbConverter.fromAbbr,
			XmlDbTags.UnitConverter_toUnitsAbbr_at, dbConverter.toAbbr);

		if (dbConverter.algorithm != null)
			xos.writeElement(XmlDbTags.algorithm_el, dbConverter.algorithm);

		if (dbConverter.coefficients[0] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.a_el, ""+dbConverter.coefficients[0]);
		if (dbConverter.coefficients[1] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.b_el, ""+dbConverter.coefficients[1]);
		if (dbConverter.coefficients[2] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.c_el, ""+dbConverter.coefficients[2]);
		if (dbConverter.coefficients[3] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.d_el, ""+dbConverter.coefficients[3]);
		if (dbConverter.coefficients[4] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.e_el, ""+dbConverter.coefficients[4]);
		if (dbConverter.coefficients[5] != Constants.undefinedDouble)
			xos.writeElement(XmlDbTags.f_el, ""+dbConverter.coefficients[5]);

		xos.endElement(myName());
	}
}
