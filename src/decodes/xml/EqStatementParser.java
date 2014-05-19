/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/25 19:31:17  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.1  2001/01/10 14:58:33  mike
*  Added EquationSpec classes & parsers.
*
*/
package decodes.xml;

import java.io.IOException;
import java.util.Enumeration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.xml.*;
import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import decodes.db.*;

// WITHDRAWN
///**
//This class maps the DECODES XML representation for EqStatement elements.
//*/
//public class EqStatementParser 
//	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner
//{
//	private int sequenceNum;
//	private EqStatement eqStatement; // object that we will build.
//
//	private static final int varNameTag           = 0;
//	private static final int expressionTag        = 1;
//
//	public EqStatementParser(int sequenceNum, EqStatement ob)
//	{
//		super();
//		this.sequenceNum = sequenceNum;
//		this.eqStatement = ob;
//	}
//
//	public String myName() { return XmlDbTags.EqStatement_el; }
//		
//	public void characters(char[] ch, int start, int length)
//		throws SAXException
//	{
//		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
//			throw new SAXException(
//				"No character data expected within EqStatement");
//	}
//
//	public void startElement(XmlHierarchyParser hier, 
//		String namespaceURI, String localName, String qname, Attributes atts)
//		throws SAXException
//	{
//		if (localName.equalsIgnoreCase(XmlDbTags.VarName_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, varNameTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.Expression_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, expressionTag));
//		}
//		else
//			throw new SAXException("Invalid element '" + localName
//				+ "' under " + myName());
//	}
//
//	/**
//	  Signals the end of the current element. 
//	  Typically this should cause your parser to pop the stack in the 
//	  hierarchy. Then do whatever cleanup or finalizing is necessary.
//	*/
//	public void endElement(XmlHierarchyParser hier, 
//		String namespaceURI, String localName, String qname)
//		throws SAXException
//	{
//		if (localName.equalsIgnoreCase(myName()))
//			hier.popObjectParser();
//		else
//			throw new SAXException(
//				"Parse stack corrupted: got end tag for " + localName
//				+ ", expected " + myName());
//	}
//
//	/**
//	  Allows an object to keep track of whitespace, if needed.
//	*/
//    public void ignorableWhitespace (char ch[], int start, int length)
//		throws SAXException
//	{
//	}
//
//	/**
//	  From TaggedStringOwner, called from TaggedStringSetter when string
//	  elements are parsed.
//	*/
//	public void set(int tag, String str)
//		throws SAXException
//	{
//		switch(tag)
//		{
//		case varNameTag:
//			if (!TextUtil.containsNoWhitespace(str))
//				throw new SAXException(XmlDbTags.VarName_el + 
//					" must be non-blank and contain no white-space.");
//			eqStatement.varName = str;
//			break;
//		case expressionTag:
//			eqStatement.expression = str;
//			break;
//		}
//	}
//
//	/**
//	  Writes this enumeration structure to an XML file.
//	*/
//	public void writeXml(XmlOutputStream xos)
//		throws IOException
//	{
//		xos.startElement(myName(), XmlDbTags.sequenceNum_at, ""+sequenceNum);
//		if (eqStatement.varName != null)
//			xos.writeElement(XmlDbTags.VarName_el, eqStatement.varName);
//		if (eqStatement.expression != null)
//			xos.writeElement(XmlDbTags.Expression_el,eqStatement.expression);
//		xos.endElement(myName());
//	}
//}
