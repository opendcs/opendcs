/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/09/27 20:21:55  mmaloney
*  dev
*
*  Revision 1.5  2004/08/30 14:49:32  mjmaloney
*  Added javadocs
*
*  Revision 1.4  2003/11/15 20:08:27  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.3  2001/12/02 13:57:21  mike
*  dev
*
*  Revision 1.2  2001/03/18 18:24:36  mike
*  Implemented PerformanceMeasurments objects & parsers.
*
*  Revision 1.1  2001/03/17 01:00:27  mike
*  dev
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Enumeration;
import decodes.db.*;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for PresentationGroup elements.
 */
public class PresentationGroupParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, TaggedBooleanOwner
{
	private PresentationGroup presentationGroup; // object that we will build.

	private static final int inheritsFromTag = 0;
	private static final int isProductionTag = 1;
	private static final int lastModifyTimeTag = 2;

	/**
	 * @param ob the object in which to store the data.
	 */
	public PresentationGroupParser( PresentationGroup ob )
	{
		super();
		this.presentationGroup = ob;
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.PresentationGroup_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within PresentationGroup");
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
		if (localName.equalsIgnoreCase(XmlDbTags.InheritsFrom_el))
			hier.pushObjectParser(
				new TaggedStringSetter(this, inheritsFromTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
			hier.pushObjectParser(
				new TaggedBooleanSetter(this, isProductionTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
			hier.pushObjectParser(new TaggedStringSetter(this,
				lastModifyTimeTag));
		else if (localName.equalsIgnoreCase(XmlDbTags.DataPresentation_el))
		{
			DataPresentation dp = new DataPresentation(presentationGroup);
			presentationGroup.addDataPresentation(dp);
			hier.pushObjectParser(new DataPresentationParser(dp));
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
		if (localName.equalsIgnoreCase(myName()))
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
		case inheritsFromTag:
			presentationGroup.inheritsFrom = str;
			break;
		case lastModifyTimeTag:
/*
MJM 20031023 - Don't use LMT contained in XML. It may not agree with the
File.lastModified() calls used elsewhere.
			try
			{
				presentationGroup.lastModifyTime
					= Constants.defaultDateFormat.parse(str);
			}
			catch(Exception e)
			{
				throw new SAXException("Improper date format '" + str
					+ "' (should be " + Constants.defaultDateFormat + ")");
			}
*/
			break;
		}
	}

	/**
	 * From TaggedBooleanOwner, called from TaggedBooleanSetter when string
	 * elements are parsed.
	 * @param tag the integer tag defined above
	 * @param value the boolean value
	 */
	public void set( int tag, boolean value )
	{
		switch(tag)
		{
		case isProductionTag:
			presentationGroup.isProduction = value;
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
		xos.startElement(myName(), XmlDbTags.name_at, 
			presentationGroup.groupName);

		if (presentationGroup.inheritsFrom != null)
		xos.writeElement(XmlDbTags.InheritsFrom_el,
			presentationGroup.inheritsFrom);
		xos.writeElement(XmlDbTags.isProduction_el,
			presentationGroup.isProduction ? "true" : "false");
/*
MJM 20031023 - Don't save LMT in file.
		if (presentationGroup.lastModifyTime != null)
			xos.writeElement(XmlDbTags.lastModifyTime_el,
				Constants.defaultDateFormat.format(
					presentationGroup.lastModifyTime));
*/

		for(int i = 0; i < presentationGroup.dataPresentations.size(); i++)
		{
			DataPresentation dp = (DataPresentation)
				presentationGroup.dataPresentations.elementAt(i);
			DataPresentationParser p = new DataPresentationParser(dp);
			p.writeXml(xos);
		}

		xos.endElement(myName());
	}
}
