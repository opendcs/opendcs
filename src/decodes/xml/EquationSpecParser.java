/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2004/08/25 19:31:18  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.6  2002/04/06 15:48:19  mike
*  Expand newlines in description fields.
*
*  Revision 1.5  2001/09/14 21:18:15  mike
*  dev
*
*  Revision 1.4  2001/03/21 21:07:28  mike
*  EquationSpec and PerformanceMeasurements are now top-level elements.
*
*  Revision 1.3  2001/03/18 22:23:56  mike
*  Improved output formatting.
*
*  Revision 1.2  2001/01/20 02:53:48  mike
*  Added EqTable Parser
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
import ilex.util.AsciiUtil;
import ilex.util.IDateFormat;
import decodes.db.*;

// WITHDRAWN
///**
//This class maps the DECODES XML representation for EquationSpec elements.
//*/
//public class EquationSpecParser 
//	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, 
//		TaggedBooleanOwner
//{
//	private EquationSpec equationSpec; // object that we will build.
//
//	private static final int descriptionTag       = 0;
//	private static final int outputNameTag        = 1;
//	private static final int unitsAbbrTag         = 2;
//	private static final int scopeTag             = 3;
//	private static final int applyToTag           = 4;
//	private static final int lastModifyTimeTag    = 5;
//	private static final int isProductionTag      = 6;
//	private static final int expirationTag        = 7;
//
//	public EquationSpecParser(EquationSpec ob)
//	{
//		super();
//		equationSpec = ob;
//	}
//
//	public String myName() { return XmlDbTags.EquationSpec_el; }
//		
//	public void characters(char[] ch, int start, int length)
//		throws SAXException
//	{
//		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
//			throw new SAXException(
//				"No character data expected within EquationSpec");
//	}
//
//	public void startElement(XmlHierarchyParser hier, 
//		String namespaceURI, String localName, String qname, Attributes atts)
//		throws SAXException
//	{
//		if (localName.equalsIgnoreCase(XmlDbTags.description_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, descriptionTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.OutputName_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, outputNameTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
//		{
//			String st = atts.getValue(XmlDbTags.DataType_standard_at);
//			if (st == null)
//				throw new SAXException(XmlDbTags.DataType_el + " without "
//					+ XmlDbTags.DataType_standard_at +" attribute");
//			String cd = atts.getValue(XmlDbTags.DataType_code_at);
//			if (cd == null)
//				throw new SAXException(XmlDbTags.DataType_el + " without "
//					+ XmlDbTags.DataType_code_at +" attribute");
//			equationSpec.dataType = DataType.getDataType(st, cd);
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.UnitsAbbr_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, unitsAbbrTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.Scope_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, scopeTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.ApplyTo_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, applyToTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.lastModifyTime_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, 
//				lastModifyTimeTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
//		{
//			hier.pushObjectParser(new TaggedBooleanSetter(this,isProductionTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.expiration_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, expirationTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.EqStatement_el))
//		{
//			String st = atts.getValue(XmlDbTags.sequenceNum_at);
//			if (st == null)
//				throw new SAXException(XmlDbTags.EqStatement_el + " without "
//					+ XmlDbTags.sequenceNum_at +" attribute");
//			// Note: sequence number in XML starts at 0!!!
//			int num = -1;
//			try { num = Integer.parseInt(st); }
//			catch(NumberFormatException e)
//			{
//				throw new SAXException("Sequence number must be a number");
//			}
//			if (equationSpec.statements.size() <= num)
//			{
//				equationSpec.statements.setSize(num+1);
//				EqStatement eqs = new EqStatement(equationSpec);
//				equationSpec.statements.setElementAt(eqs, num);
//				hier.pushObjectParser(new EqStatementParser(num, eqs));
//			}
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
//		if (localName.equalsIgnoreCase(XmlDbTags.DataType_el))
//			;	// End of empty DataType element.
//		else if (localName.equalsIgnoreCase(myName()))
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
//		case outputNameTag:
//			if (!TextUtil.containsNoWhitespace(str))
//				throw new SAXException(XmlDbTags.OutputName_el + 
//					" must be non-blank and contain no white-space.");
//			equationSpec.outputName = str;
//			break;
//		case descriptionTag:
//			str = TextUtil.collapseWhitespace(str);
//			str = new String(AsciiUtil.ascii2bin(str));
//			equationSpec.description = str;
//			break;
//		case unitsAbbrTag:
//			equationSpec.unitsAbbr = str;
//			break;
//		case scopeTag:
//			equationSpec.scope = str;
//			break;
//		case applyToTag:
//			equationSpec.applyTo = str;
//			break;
//		case expirationTag:
//			try
//			{
//				equationSpec.expiration = Constants.defaultDateFormat.parse(str);
//			}
//			catch(Exception e)
//			{
//				throw new SAXException("Improper date format '" + str 
//					+ "' (should be " + Constants.defaultDateFormat + ")");
//			}
//			break;
//		case lastModifyTimeTag:
//			try
//			{
//				equationSpec.lastModifyTime = 
//					Constants.defaultDateFormat.parse(str);
//			}
//			catch(Exception e)
//			{
//				throw new SAXException("Improper date format '" + str 
//					+ "' (should be " + Constants.defaultDateFormat + ")");
//			}
//			break;
//		}
//	}
//
//	/**
//	  From TaggedBooleanOwner, called from TaggedBooleanSetter when boolean
//	  elements are parsed.
//	*/
//	public void set(int tag, boolean value)
//	{
//		switch(tag)
//		{
//		case isProductionTag:
//			equationSpec.isProduction = value;
//			break;
//		}
//	}
//
//
//	/**
//	  Writes this enumeration structure to an XML file.
//	*/
//	public void writeXml(XmlOutputStream xos)
//		throws IOException
//	{
//		xos.startElement(myName(), XmlDbTags.name_at, equationSpec.name);
//		if (equationSpec.outputName != null)
//			xos.writeElement(XmlDbTags.OutputName_el, equationSpec.outputName);
//		if (equationSpec.description != null)
//			xos.writeElement(XmlDbTags.description_el,
//				AsciiUtil.bin2ascii(equationSpec.description.getBytes()));
//		if (equationSpec.scope != null)
//			xos.writeElement(XmlDbTags.Scope_el,equationSpec.scope);
//		if (equationSpec.applyTo != null)
//			xos.writeElement(XmlDbTags.ApplyTo_el,equationSpec.applyTo);
//		if (equationSpec.dataType != null)
//		{
//			xos.writeElement(XmlDbTags.DataType_el, 
//				XmlDbTags.DataType_standard_at, equationSpec.dataType.standard,
//				XmlDbTags.DataType_code_at, equationSpec.dataType.code, null);
//		}
//		if (equationSpec.unitsAbbr != null)
//			xos.writeElement(XmlDbTags.UnitsAbbr_el,equationSpec.unitsAbbr);
//
//		xos.writeElement(XmlDbTags.isProduction_el,
//			equationSpec.isProduction ? "true" : "false");
//		if (equationSpec.lastModifyTime != null)
//			xos.writeElement(XmlDbTags.lastModifyTime_el, 
//				Constants.defaultDateFormat.format(equationSpec.lastModifyTime));
//		if (equationSpec.expiration != null)
//			xos.writeElement(XmlDbTags.expiration_el, 
//				Constants.defaultDateFormat.format(equationSpec.expiration));
//
//		for(int i = 0; i < equationSpec.statements.size(); i++)
//		{
//			EqStatement st = (EqStatement)equationSpec.statements.elementAt(i);
//			if (st != null)
//			{
//				EqStatementParser p = new EqStatementParser(i, st);
//				p.writeXml(xos);
//			}
//		}
//
//		xos.endElement(myName());
//	}
//}
