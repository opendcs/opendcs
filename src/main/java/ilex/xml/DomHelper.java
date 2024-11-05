/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2017/03/24 11:58:46  mmaloney
*  Added getLongIntContent()
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/09/05 12:55:05  mjmaloney
*  Handle null & untrimmed strings in getIntegerContent
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2007/10/23 20:36:18  mmaloney
*  commented out info lines
*
*  Revision 1.6  2007/09/29 21:58:41  mmaloney
*  dev
*
*  Revision 1.5  2007/02/16 22:23:49  mmaloney
*  Added provisions for default values.
*
*  Revision 1.4  2007/01/30 18:40:34  mmaloney
*  Added method to support outage functions.
*
*  Revision 1.3  2006/06/02 20:05:01  mmaloney
*  Added DomHelper write methods.
*
*  Revision 1.2  2004/08/30 14:50:37  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/03/27 13:52:01  mjmaloney
*  Created DomHelper
*
*/
package ilex.xml;

import org.xml.sax.SAXException;
import ilex.util.ErrorException;
import ilex.util.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
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
import java.io.PrintStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

/**
* This class has static utility methods useful for building DOM parsers.
*/
public abstract class DomHelper
{
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
//		Logger.instance().log(Logger.E_INFORMATION,
//			moduleName + ": Parsing '" + filename + "'");

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
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": SAXException parsing '" + filename + "': "+ex);
			throw new ErrorException(ex.toString());
		}
		catch(ParserConfigurationException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": ParserConfigurationException parsing '" 
				+ filename + "': " + ex);
			ex.printStackTrace(System.err);
			throw new ErrorException(ex.toString());
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": IOException parsing '" + filename + "': " + ex);
			throw new ErrorException(ex.toString());
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
		Logger.instance().debug1(moduleName + " Parsing stream.");

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
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": SAXException parsing input stream: "+ex);
			throw new ErrorException(ex.toString());
		}
		catch(ParserConfigurationException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": ParserConfigurationException parsing " 
				+ "input stream: " + ex);
			ex.printStackTrace(System.err);
			throw new ErrorException(ex.toString());
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				moduleName + ": IOException parsing input stream: " + ex);
			throw new ErrorException(ex.toString());
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
			Logger.instance().log(Logger.E_WARNING,
				mod + ": Excpected integer for '" + node.getNodeName()
				+ "', using default of " + defaultValue);
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
			Logger.instance().log(Logger.E_WARNING,
				mod + ": Excpected double for '" + node.getNodeName()
				+ "', using default of " + defaultValue);
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
			Logger.instance().log(Logger.E_WARNING,
				mod + ": Excpected integer for '" + node.getNodeName()
				+ "', using default of " + defaultValue);
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
			Logger.instance().log(Logger.E_WARNING,
				mod + ": Excpected boolean for '" + node.getNodeName()
				+ "', using default of " + defVal);
			return defVal;
		}
		s = s.toLowerCase();
		char c = s.charAt(0);
		if (c == 't' || c == 'y' || s.equals("on"))
			return true;
		if (c == 'f' || c == 'n' || s.equals("off"))
			return false;
		Logger.instance().log(Logger.E_WARNING,
			mod + ": Unrecognized boolean value '" + s + "' for '"
			 + node.getNodeName() + "', using default of " + defVal);
		return defVal;
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
			Logger.instance().warning(
				mod + ": Excpected date for '" + node.getNodeName() + "'");
			return null;
		}
		try { return df.parse(s); }
		catch(ParseException ex)
		{
			Logger.instance().warning(
				mod + ": cannot parse date '" + s + "' for '"
			 	+ node.getNodeName() + "': " + ex);
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
//System.out.println("Searching for attr '" + name + "'");
		NamedNodeMap nnm = elem.getAttributes();
		if (nnm == null)
			return null;
		int n = nnm.getLength();
		for(int i=0; i<n; i++)
		{
			Node child = nnm.item(i);
//System.out.println("attr type=" + child.getNodeType() + ", name=" + child.getNodeName());
			if (child.getNodeName().equalsIgnoreCase(name))
//{
//System.out.println("found match, returning '" + child.getTextContent() + "'");
				return child.getTextContent();
//}
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
//System.out.println("findNode looking for type=" + type + ", name=" + name);
		NodeList children = parent.getChildNodes();
		for(int i=0; children != null && i<children.getLength(); i++)
		{
			Node child = children.item(i);
//System.out.println("child type=" + child.getNodeType() + ", name=" + child.getNodeName());
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
			String msg = "Cannot make new DOM Document: " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new ErrorException(msg);
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
		FileOutputStream fos = null;
		try
		{
System.err.println("writeDocument opening...");
			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer trans = tranFactory.newTransformer();
			Source src = new DOMSource(doc);
			fos = new FileOutputStream(filename);
//			Result dest = new StreamResult(fos);
			Result dest = new StreamResult(System.out);

//			trans.setOutputProperty(OutputKeys.STANDALONE, "yes");
//			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("indent", "yes");
//			trans.setOutputProperty("indent-amount", "2");
			trans.setOutputProperty(
				"{ http://xml.apache.org/xslt }indent-amount", "2");

System.err.println("writeDocument trasnforming...");
			trans.transform(src, dest);
System.err.println("writeDocument done.");
		}
		catch(TransformerConfigurationException ex)
		{
			String msg = "Cannot make new transformer: " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new ErrorException(msg);
		}
		catch(IOException ex)
		{
			String msg = "Cannot open '" + filename + "' for writing: " + ex;
			throw new ErrorException(msg);
		}
		catch(TransformerException ex)
		{
			String msg = "Error in transformer: " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new ErrorException(msg);
		}
		finally
		{
			if (fos != null)
				try { fos.close(); } catch(Exception fex) {}
		}
	}
}
