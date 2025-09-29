/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.xml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Vector;

import decodes.db.*;
import ilex.util.TextUtil;
import java.io.IOException;
import ilex.xml.*;

/**
 * This class maps the DECODES XML representation for DecodesScript elements.
 */
public class DecodesScriptParser implements XmlObjectParser, XmlObjectWriter, TaggedStringOwner, DecodesScriptReader
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			catch(NumberFormatException ex)
			{
				throw new SAXException("Sensor number must be an integer", ex);
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
			log.warn("Invalid element '{}' under {} -- skipped.", localName, myName());
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
							log.warn("Invalid dataOrder '{}'", m);
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
	public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException
	{
		if (currentStatementPosition < statements.size())
		{
			int cur = currentStatementPosition;
			currentStatementPosition++;
			FormatStatement curStmt = statements.get(cur);
			FormatStatement newStmt = new FormatStatement(script, cur);
			newStmt.format = curStmt.format;
			newStmt.label = curStmt.label;
			return Optional.of(newStmt);
		}
		else
		{
			return Optional.empty();
		}

	}
}
