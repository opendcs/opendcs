/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2010/12/23 18:23:56  mmaloney
*  udpated for groups
*
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2006/01/11 19:04:09  mmaloney
*  dev
*
*  Revision 1.8  2004/08/30 14:49:29  mjmaloney
*  Added javadocs
*
*  Revision 1.7  2003/11/15 20:08:23  mjmaloney
*  Updates for new structures in DECODES Database Version 6.
*  Parsers now ignore unrecognized elements with a warning. They used to
*  abort. The new behavior allows easier future enhancements.
*
*  Revision 1.6  2003/10/20 20:22:55  mjmaloney
*  Database changes for DECODES 6.0
*
*  Revision 1.5  2001/06/16 20:25:55  mike
*  dev
*
*  Revision 1.4  2001/03/18 22:23:56  mike
*  Improved output formatting.
*
*  Revision 1.3  2001/01/03 02:54:59  mike
*  dev
*
*  Revision 1.2  2000/12/31 23:12:50  mike
*  dev
*
*  Revision 1.1  2000/12/31 22:30:47  mike
*  dev
*
*/
package decodes.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Vector;

import decodes.db.*;
import decodes.db.DecodesScript.DecodesScriptBuilder;
import ilex.util.TextUtil;
import ilex.util.Logger;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for DecodesScript elements.
 */
public class DecodesScriptParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, DecodesScriptReader
{
	private DecodesScript decodesScript = null;
	private FormatStatement tmpFmt = null;  // Tmp for building label & statement
	private ArrayList<FormatStatement> statements = new ArrayList<>();
	private int currentStatementPosition = 0;
	private ArrayList<ScriptSensor> sensors = new ArrayList<>();
	private String scriptType = null;
	private char dataOrder = Constants.dataOrderUndefined;

	private static final int scriptTypeTag = 0;
	private static final int formatStatementTag = 1;
	private static final int dataOrderTag = 2;

	/**
	 * @param script the object in which to store the data.
	 * 
	 */
	public DecodesScriptParser(DecodesScript script)
	{
		this.decodesScript = script;
	}

	/**
	 * Constructor.
	 * 
	 */
	public DecodesScriptParser( )
	{
		super();
	}

	/**
	 * @return name of element parsed by this parser
	 */
	public String myName( ) { return XmlDbTags.DecodesScript_el; }
		
	/**
	 * @param ch Characters from file
	 * @param start start of characters
	 * @param length length of characters
	 */
	public void characters( char[] ch, int start, int length ) throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within DecodesScript");
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
		if (localName.equalsIgnoreCase(XmlDbTags.scriptType_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, scriptTypeTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.FormatStatement_el))
		{
			String nm = atts.getValue(XmlDbTags.label_at);
			if (nm == null)
				throw new SAXException(XmlDbTags.FormatStatement_el + 
					" without " + XmlDbTags.label_at +" attribute");
			/*tmpFmt = new FormatStatement(decodesScript, 
				decodesScript.getFormatStatements().size());*/
			tmpFmt = new FormatStatement(null, statements.size());
			tmpFmt.label = nm;

			hier.pushObjectParser(new TaggedStringSetter(this, 
				formatStatementTag));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.ScriptSensor_el))
		{
			String ns = atts.getValue(XmlDbTags.sensorNumber_at);
			int snum = -1;
			try
			{
				snum = Integer.parseInt(ns);
			}
			catch(NumberFormatException e)
			{
				throw new SAXException("Sensor number must be an integer");
			}
			ScriptSensor ob = new ScriptSensor(null, snum);
			sensors.add(ob);
			hier.pushObjectParser(new ScriptSensorParser(ob));
		}
		else if (localName.equalsIgnoreCase(XmlDbTags.dataOrder_el))
		{
			hier.pushObjectParser(new TaggedStringSetter(this, dataOrderTag));
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
			case scriptTypeTag:
			{
				scriptType = str;
				break;
			}
			case formatStatementTag:
			{
				tmpFmt.format = TextUtil.collapseWhitespace(str);
				statements.add(tmpFmt);
				break;
			}
			case dataOrderTag:
				{
					char m = Constants.dataOrderUndefined;
					if (str.length() > 0)
					{
						m = Character.toUpperCase(str.charAt(0));
						if (m != Constants.dataOrderAscending
						&& m != Constants.dataOrderDescending
						&& m != Constants.dataOrderUndefined)
						{
							TopLevelParser.instance().parseWarning(
								"Invalid dataOrder '" + m + "'");
							m = Constants.dataOrderUndefined;
						}
					}
					dataOrder = m;
					break;
				}
			}
	}

	/**
	 * Retrieve the parsed data order.
	 * @return
	 */
	public char getDataOrder() {
		return dataOrder;
	}

	/**
	 * Retrieve the parsed script type.
	 * @return
	 */
	public String getType() {
		return scriptType;
	}

	/**
	 * Retrieve the Parsed sensor information.
	 */
	public ArrayList<ScriptSensor> getSensors()
	{
		return sensors;
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(myName(), XmlDbTags.DecodesScript_scriptName_at,
			decodesScript.scriptName);

		if (decodesScript.scriptType != null)
			xos.writeElement(XmlDbTags.scriptType_el,decodesScript.scriptType);

		if (decodesScript.getDataOrder() != Constants.dataOrderUndefined)
			xos.writeElement(XmlDbTags.dataOrder_el,
				"" + decodesScript.getDataOrder());
		Vector<FormatStatement> formatStatements = decodesScript.getFormatStatements();
		for(int i = 0; i < formatStatements.size(); i++)
		{
			FormatStatement fs = formatStatements.elementAt(i);
			xos.writeElement(XmlDbTags.FormatStatement_el, 
				XmlDbTags.label_at, fs.label, fs.format);
		}

		for(int i = 0; i < decodesScript.scriptSensors.size(); i++)
		{
			ScriptSensor ss = 
				(ScriptSensor)decodesScript.scriptSensors.elementAt(i);
			ScriptSensorParser p = new ScriptSensorParser(ss);
			p.writeXml(xos);
		}
		xos.endElement(myName());
	}

	@Override
	public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException {
		// TODO Auto-generated method stub
		if( currentStatementPosition < statements.size() ) {
			int cur = currentStatementPosition;
			currentStatementPosition++;
			FormatStatement curStmt = statements.get(cur);
			FormatStatement newStmt = new FormatStatement(script, cur);
			newStmt.format = curStmt.format;
			newStmt.label = curStmt.label;
			return Optional.of(newStmt);
		} else {
			return Optional.empty();
		}
		
	}
}
