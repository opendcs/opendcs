/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/25 19:31:17  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.5  2001/09/14 21:18:15  mike
*  dev
*
*  Revision 1.4  2001/06/12 14:14:29  mike
*  dev
*
*  Revision 1.3  2001/03/18 22:23:56  mike
*  Improved output formatting.
*
*  Revision 1.2  2001/01/22 02:23:20  mike
*  dev
*
*  Revision 1.1  2001/01/20 02:53:48  mike
*  Added EqTable Parser
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
//This class maps the DECODES XML representation for EqTable elements.
//*/
//public class EqTableParser 
//	implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, 
//		TaggedBooleanOwner, TaggedDoubleOwner
//{
//	private EqTable eqTable; // object that we will build.
//	private String propName; // Tmp storage while waiting for value parse.
//
//	private static final int descriptionTag          = 0;
//	private static final int lookupAlgorithmTag      = 1;
//	private static final int inputNameTag            = 2;
//	private static final int applyInputLowerBoundTag = 3;
//	private static final int inputLowerBoundTag      = 4;
//	private static final int applyInputUpperBoundTag = 5;
//	private static final int inputUpperBoundTag      = 6;
//	private static final int outputNameTag           = 7;
//	private static final int isProductionTag         = 8;
//	private static final int expirationTag           = 9;
//	private static final int propertyTag             = 10;
//
//	public EqTableParser(EqTable ob)
//	{
//		super();
//		eqTable = ob;
//	}
//
//	public String myName() { return XmlDbTags.EqTable_el; }
//		
//	public void characters(char[] ch, int start, int length)
//		throws SAXException
//	{
//		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
//			throw new SAXException(
//				"No character data expected within EqTable");
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
//		else if (localName.equalsIgnoreCase(XmlDbTags.lookupAlgorithm_el))
//		{
//			hier.pushObjectParser(
//				new TaggedStringSetter(this, lookupAlgorithmTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.inputName_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, inputNameTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.applyInputLowerBound_el))
//		{
//			hier.pushObjectParser(
//				new TaggedBooleanSetter(this, applyInputLowerBoundTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.inputLowerBound_el))
//		{
//			hier.pushObjectParser(
//				new TaggedDoubleSetter(this, inputLowerBoundTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.applyInputUpperBound_el))
//		{
//			hier.pushObjectParser(
//				new TaggedBooleanSetter(this, applyInputUpperBoundTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.inputUpperBound_el))
//		{
//			hier.pushObjectParser(
//				new TaggedDoubleSetter(this, inputUpperBoundTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.OutputName_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, outputNameTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.isProduction_el))
//		{
//			hier.pushObjectParser(
//				new TaggedBooleanSetter(this,isProductionTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.expiration_el))
//		{
//			hier.pushObjectParser(new TaggedStringSetter(this, expirationTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.EqTableProperty_el))
//		{
//			propName = atts.getValue(XmlDbTags.propertyName_at);
//			if (propName == null)
//				throw new SAXException(XmlDbTags.EqTableProperty_el 
//					+ " without " + XmlDbTags.propertyName_at +" attribute");
//			hier.pushObjectParser(new TaggedStringSetter(this, propertyTag));
//		}
//		else if (localName.equalsIgnoreCase(XmlDbTags.EqTablePoint_el))
//		{
//			String xs = atts.getValue(XmlDbTags.x_at);
//			String ys = atts.getValue(XmlDbTags.y_at);
//			if (xs == null || ys == null)
//				throw new SAXException(XmlDbTags.EqTablePoint_el + 
//					" requires both " + XmlDbTags.x_at + " and "
//					+ XmlDbTags.y_at);
//
//			double x, y;
//			try 
//			{
//				x = Double.parseDouble(xs);
//				y = Double.parseDouble(ys);
//			}
//			catch(NumberFormatException e)
//			{
//				throw new SAXException(
//					"Point coordinates must be floating point numbers");
//			}
//			eqTable.addPoint(x, y);
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
//		if (localName.equalsIgnoreCase(XmlDbTags.EqTablePoint_el))
//			;	// End of empty Point element.
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
//		case descriptionTag:
//			eqTable.description = TextUtil.collapseWhitespace(str);
//			break;
//		case lookupAlgorithmTag:
//			eqTable.lookupAlgorithm = str;
//			break;
//		case inputNameTag:
//			eqTable.inputName = str;
//			break;
//		case outputNameTag:
//			eqTable.outputName = str;
//			break;
//		case propertyTag:
//			if (propName == null)
//				throw new SAXException("Property value without name!");
//			eqTable.properties.setProperty(propName, str);
//			propName = null;
//			break;
//		case expirationTag:
//			try
//			{
//				eqTable.expiration = Constants.defaultDateFormat.parse(str);
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
//		case applyInputLowerBoundTag:
//			eqTable.applyInputLowerBound = value;
//			break;
//		case applyInputUpperBoundTag:
//			eqTable.applyInputUpperBound = value;
//			break;
//		case isProductionTag:
//			eqTable.isProduction = value;
//			break;
//		}
//	}
//
//	/**
//	  From TaggedDoubleOwner, called from TaggedDoubleSetter when double 
//	  elements are parsed.
//	*/
//	public void set(int tag, double value)
//	{
//		switch(tag)
//		{
//		case inputLowerBoundTag:
//			eqTable.inputLowerBound = value;
//			break;
//		case inputUpperBoundTag:
//			eqTable.inputUpperBound = value;
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
//		xos.startElement(myName(), XmlDbTags.name_at, eqTable.name);
//		if (eqTable.description != null)
//			xos.writeElement(XmlDbTags.description_el,eqTable.description);
//		if (eqTable.lookupAlgorithm != null)
//			xos.writeElement(XmlDbTags.lookupAlgorithm_el,
//				eqTable.lookupAlgorithm);
//		if (eqTable.inputName != null)
//			xos.writeElement(XmlDbTags.inputName_el,eqTable.inputName);
//		if (eqTable.applyInputLowerBound 
//		 && eqTable.inputLowerBound != Constants.undefinedDouble)
//		{
//			xos.writeElement(XmlDbTags.applyInputLowerBound_el,"true");
//			xos.writeElement(XmlDbTags.inputLowerBound_el,""+
//				eqTable.inputLowerBound);
//		}
//		if (eqTable.applyInputUpperBound 
//		 && eqTable.inputUpperBound != Constants.undefinedDouble)
//		{
//			xos.writeElement(XmlDbTags.applyInputUpperBound_el,"true");
//			xos.writeElement(XmlDbTags.inputUpperBound_el,""+
//				eqTable.inputUpperBound);
//		}
//		if (eqTable.outputName != null)
//			xos.writeElement(XmlDbTags.OutputName_el,eqTable.outputName);
//		xos.writeElement(XmlDbTags.isProduction_el,
//			eqTable.isProduction ? "true" : "false");
//		if (eqTable.expiration != null)
//			xos.writeElement(XmlDbTags.expiration_el, 
//				Constants.defaultDateFormat.format(eqTable.expiration));
//
//		for(int i = 0; i < eqTable.pointVec.size(); i++)
//		{
//			Point p = (Point)eqTable.pointVec.elementAt(i);
//			xos.writeElement(XmlDbTags.EqTablePoint_el, 
//				XmlDbTags.x_at, ""+p.x, XmlDbTags.y_at, ""+p.y, null);
//		}
//
//		Enumeration e = eqTable.properties.propertyNames();
//		while(e.hasMoreElements())
//		{
//			String nm = (String)e.nextElement();
//			String v = (String)eqTable.properties.getProperty(nm);
//			xos.startElement(XmlDbTags.EqTableProperty_el, 
//				XmlDbTags.propertyName_at, nm);
//			xos.writePCDATA(v);
//			xos.endElement(XmlDbTags.EqTableProperty_el);
//		}
//
//		xos.endElement(myName());
//	}
//}
