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
package ilex.xml;

import org.xml.sax.SAXException;
import ilex.util.ErrorException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
* This class has static utility methods useful for building DOM parsers.
*/
public abstract class DomHelper
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static DocumentBuilderFactory factory = null;
	public static SimpleDateFormat dateFormat =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	/**
	* Reads a file into a DOM Document.
	* Pass your program module name (for log messages).
	* Pass file name.
	* @param moduleName the module name
	* @param filename the file name
	* @return @throws ErrorException
	*/
	public static Document readFile( String moduleName, String filename )
		throws ErrorException
	{
		try
		{
			if (factory == null)
				factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File(filename));
			return doc;
		}
		catch(SAXException ex)
		{

			throw new ErrorException("SAXException parsing " + filename, ex);
		}
		catch(ParserConfigurationException ex)
		{
			throw new ErrorException("ParserConfigurationException parsing '" + filename + "'", ex);
		}
		catch(IOException ex)
		{
			throw new ErrorException("IOException parsing '" + filename + "'", ex);
		}
	}

	/**
	* Reads an input stream into a DOM Document.
	* Pass your program module name (for log messages).
	* Pass file name.
	* @param moduleName the module name
	* @param filename the file name
	* @return @throws ErrorException
	*/
	public static Document readStream(String moduleName, InputStream strm)
		throws ErrorException
	{
		log.debug("Parsing stream.");

		try
		{
			if (factory == null)
				factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(strm);
			return doc;
		}
		catch(SAXException ex)
		{
			throw new ErrorException("Error parsing input stream.", ex);
		}
		catch(ParserConfigurationException ex)
		{
			throw new ErrorException("ParserConfigurationException parsing input stream", ex);
		}
		catch(IOException ex)
		{
			throw new ErrorException("IOException parsing input stream.", ex);
		}
	}

	/**
	* Concatenates the TEXT and CDATA content contained in this node,
	* trims, it, and returns it as a single string.
	* If no content found, the empty string will be returned.
	* @param node the Node
	* @return concatenated string
	*/
	public static String getTextContent( Node node )
	{
		StringBuilder sb = new StringBuilder();
		NodeList children = node.getChildNodes();
		if (children != null)
			for(int i=0; i<children.getLength(); i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() == Node.TEXT_NODE
				 || child.getNodeType() == Node.CDATA_SECTION_NODE)
				{
					if (sb.length() > 0)
						sb.append(' ');
					String v = child.getNodeValue();
					if (v != null)
						sb.append(v.trim());
				}
			}
		return sb.toString();
	}

	/**
	 * Returns full content of this node as a string, including any
	 * children nodes, recursively. Use this is the content of the node
	 * may contain other XML elements that you want to represent as a string.
	 */
	public static String getFullContent(Node node)
	{
		StringBuilder sb = new StringBuilder();
		NodeList children = node.getChildNodes();
		if (children != null)
			for(int i=0; i<children.getLength(); i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() == Node.TEXT_NODE
				 || child.getNodeType() == Node.CDATA_SECTION_NODE)
				{
					if (sb.length() > 0)
						sb.append(' ');
					String v = child.getNodeValue();
					if (v != null)
						sb.append(v.trim());
				}
				else if (child.getNodeType() == Node.ELEMENT_NODE)
				{
					sb.append("<" + child.getNodeName());
					NamedNodeMap nnm = child.getAttributes();
					if (nnm != null && nnm.getLength() > 0)
					{
						int n = nnm.getLength();
						for(int j=0; j<n; j++)
						{
							Node attr = nnm.item(j);
							sb.append(" " + attr.getNodeName()
								+ "=\"" + attr.getTextContent() + "\"");
						}
					}
					sb.append(">");
					sb.append(getFullContent(child));
					sb.append("</" + child.getNodeName() + ">");
				}
			}
		return sb.toString();
	}


	/**
	* Gets the content of this node and converts to an integer.
	* If not a number, prints log message and returns default value.
	* @param node the Node
	* @param defaultValue default value used if parse error
	* @param mod module name
	* @return integer content
	*/
	public static int getIntegerContent( Node node, int defaultValue, String mod )
	{
		String s = getTextContent(node);
		if (s == null)
			return defaultValue;
		try { return Integer.parseInt(s.trim()); }
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Expected integer for '{}', using default of '{}", node.getNodeName(), defaultValue);
			return defaultValue;
		}
	}

	public static double getDoubleContent(Node node, double defaultValue, String mod)
	{
		String s = getTextContent(node);
		if (s == null)
			return defaultValue;
		try { return Double.parseDouble(s.trim()); }
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Expected double for '{}', using default of {}", node.getNodeName(), defaultValue);
			return defaultValue;
		}

	}

	/**
	* Gets the content of this node and converts to an integer.
	* If not a number, prints log message and returns default value.
	* @param node the Node
	* @param defaultValue default value used if parse error
	* @param mod module name
	* @return integer content
	*/
	public static long getLongIntContent( Node node, long defaultValue, String mod )
	{
		String s = getTextContent(node);
		if (s == null)
			return defaultValue;
		try { return Long.parseLong(s.trim()); }
		catch(NumberFormatException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Expected integer for '{}', using default of {}", node.getNodeName(), defaultValue);
			return defaultValue;
		}
	}



	/**
	* Gets the content of this node and converts to a boolean.
	* Strings starting with y, Y, t, T, or have a value "on" will return true.
	* Strings starting with n, N, f, F, or have a value "off" will return false.
	* Otherwise, WARNING messages are placed in the log and the default value
	* is returned.
	* @param node the Node
	* @param defVal default value used if parse error
	* @param mod module name
	* @return boolean content
	*/
	public static boolean getBooleanContent( Node node, boolean defVal, String mod )
	{
		String s = getTextContent(node);
		if (s.length() == 0)
		{
			log.warn("Expected boolean for '{}', using default of {}", node.getNodeName(), defVal);
			return defVal;
		}
		s = s.toLowerCase();
		char c = s.charAt(0);
		if (c == 't' || c == 'y' || s.equals("on"))
			return true;
		if (c == 'f' || c == 'n' || s.equals("off"))
			return false;
		log.warn("Unrecognized boolean value '{}' for '{}', using default of {}", s, node.getNodeName(), defVal);
		return defVal;
	}

	public static <T extends Enum<T>> T getEnum(Node node, Class<T> enumType)
	{
		String s = getTextContent(node);
		return Enum.valueOf(enumType, s);
	}

	/**
	* Gets the content of this node and converts to a date.
	* <p>
	* Uses the static 'dateFormat' object to do the conversion. Default
	* date/time format is "yyyy-MM-dd HH:mm:ss z". You may set dateFormat
	* to a different object if a different format is needed.
	*
	* @param node the Node
	* @param mod module name
	* @return date value or null if conversion is not successful.
	*/
	public static Date getDateContent(Node node, String mod)
	{
		return getDateContent(node, mod, dateFormat);
	}

	public static Date getDateContent(Node node, String mod, DateFormat df)
	{
		String s = getTextContent(node);
		if (s.length() == 0)
		{
			log.warn("Expected date for '{}'", node.getNodeName());
			return null;
		}
		try { return df.parse(s); }
		catch(ParseException ex)
		{
			log.atWarn().setCause(ex).log("cannot parse date '{}' for '{}'", s, node.getNodeName());
			return null;
		}
	}

	/**
	 * Returns an attribute by a case-INsensitive attriibute name.
	 * @param attrname the attribute name
	 * @param elem the Element.
	 * @return the attribute value or null if not found.
	 */
	public static String findAttr(Element elem, String name)
	{
		NamedNodeMap nnm = elem.getAttributes();
		if (nnm == null)
			return null;
		int n = nnm.getLength();
		for(int i=0; i<n; i++)
		{
			Node child = nnm.item(i);
			if (child.getNodeName().equalsIgnoreCase(name))
			{
				return child.getTextContent();
			}
		}
		return null;
	}

	/**
	 * Returns the first matching Node by a case-INsensitive node-name search.
	 * @param parent the Element.
	 * @param name the node name to search for.
	 * @param type the node type (one of the constants defined in Node).
	 * @return the Node or null if not found.
	 */
	public static Node findNode(Element parent, String name, short type)
	{
		NodeList children = parent.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() == type
			 && name.equalsIgnoreCase(child.getNodeName()))
				return child;
		}
		return null;
	}


	/**
	 * @return a new, empty document.
	 */
	public static Document makeNewDocument()
		throws ErrorException
	{
		try
		{
			if (factory == null)
				factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			return doc;
		}
		catch(ParserConfigurationException ex)
		{
			throw new ErrorException("Cannot make new DOM Document.", ex);
		}
	}

	/**
	 * Writes a DOM Document to a file.
	 * @param doc the document
	 * @param filename the file name
	 */
	public static void writeDocument(Document doc, String filename)
		throws ErrorException
	{
		try (FileOutputStream fos = new FileOutputStream(filename))
		{
			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer trans = tranFactory.newTransformer();
			Source src = new DOMSource(doc);
			Result dest = new StreamResult(System.out);
			trans.setOutputProperty("indent", "yes");
			trans.setOutputProperty("{ http://xml.apache.org/xslt }indent-amount", "2");
			trans.transform(src, dest);
		}
		catch(TransformerConfigurationException ex)
		{
			throw new ErrorException("Cannot make new transformer.", ex);
		}
		catch(IOException ex)
		{
			throw new ErrorException("Cannot open '" + filename + "' for writing.", ex);
		}
		catch(TransformerException ex)
		{
			throw new ErrorException("Error in transformer.", ex);
		}
	}
}
