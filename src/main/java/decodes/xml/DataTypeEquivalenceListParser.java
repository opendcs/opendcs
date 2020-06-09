/*
*  $Id$
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Iterator;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation to the DataTypeEquivalenceList.
 */
public class DataTypeEquivalenceListParser implements XmlObjectParser, XmlObjectWriter
{
	private int state;
	private static final int Top = 0;
	private static final int InDTE = 1;
	private static final int InDT = 2;
	private DataType firstDT;
	public static boolean dtEquivalencesParsed = false;

	/** Default constructor */
	public DataTypeEquivalenceListParser( )
	{
		super();
		state = Top;
		firstDT = null;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.DataTypeEquivalenceList_el; }

	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within DataTypeEquivalenceList");
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
		dtEquivalencesParsed = true;
		if (localName.equalsIgnoreCase(XmlDbTags.DataTypeEquivalence_el))
		{
			if (state != Top)
				throw new SAXException("Nested DataTypeEquivalence illegal!");
			state = InDTE;
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
		{
			if (state != InDTE)
				throw new SAXException(
					"DataType can only appear inside DataTypeEquivalence!");
			state = InDT;

			String st = atts.getValue(XmlDbTags.DataType_standard_at);
			if (st == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_standard_at +" attribute");
			String cd = atts.getValue(XmlDbTags.DataType_code_at);
			if (cd == null)
				throw new SAXException(XmlDbTags.DataType_el + " without "
					+ XmlDbTags.DataType_code_at +" attribute");
			DataType dt = DataType.getDataType(st, cd);

			if (firstDT == null)
				firstDT = dt;
		 	else
				firstDT.assertEquivalence(dt);
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
		if (localName.equalsIgnoreCase(XmlDbTags.DataTypeEquivalence_el))
		{
			if (state != InDTE)
				throw new SAXException("Unexpected end tag for "
					+ XmlDbTags.DataTypeEquivalence_el);
			state = Top;
			firstDT = null;
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
		{
			if (state != InDT)
				throw new SAXException("Unexpected end tag for "
					+ XmlDbTags.DataType_el);
			state = InDTE;
		}
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
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName());

		// Reset all saved flags, we will set them as we save.
		Database.getDb().dataTypeSet.resetSaved();

		for(Iterator it = Database.getDb().dataTypeSet.values().iterator(); 
			it.hasNext(); )
		{
			DataType dt = (DataType)it.next();
			if (!dt.saved
			 && dt.equivRing != null
			 && dt.equivRing != dt)
			{
				xos.startElement(XmlDbTags.DataTypeEquivalence_el);
				xos.writeElement(XmlDbTags.DataType_el,
					XmlDbTags.DataType_standard_at, dt.getStandard(),
					XmlDbTags.DataType_code_at, dt.getCode(), (String)null);
				dt.saved = true;
				for(DataType dti = dt.equivRing; 
					dti != null && dti != dt; dti = dti.equivRing)
				{
					xos.writeElement(XmlDbTags.DataType_el,
						XmlDbTags.DataType_standard_at, dti.getStandard(),
						XmlDbTags.DataType_code_at, dti.getCode(), (String)null);
					dti.saved = true;
				}
				xos.endElement(XmlDbTags.DataTypeEquivalence_el);
			}
		}
		xos.endElement(myName());
	}
}
