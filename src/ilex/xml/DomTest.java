/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:37  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/03/26 20:16:37  mjmaloney
*  Added DomTest
*
*/
package ilex.xml;

import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/**
* This class is a test of DOM. It can be used as a cut&paste model for
* building DOM parsers.
*/
public class DomTest
{
	public DomTest( )
	{
	}

	/**
	* @param uri
	*/
	public void doit( String uri )
	{
		System.out.println("Parsing '" + uri + "'");

		//DOMParser parser = new DOMParser();
		try
		{
			DocumentBuilderFactory factory = 
				DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new File(uri));
			printNode(doc, System.out, "");
		}
		catch(SAXException ex)
		{
			System.err.println("SAXException: " + ex);
			ex.printStackTrace(System.err);
		}
		catch(ParserConfigurationException ex)
		{
			System.err.println("ParserConfigurationException: " + ex);
			ex.printStackTrace(System.err);
		}
		catch(IOException ex)
		{
			System.err.println("IOException: " + ex);
			ex.printStackTrace(System.err);
		}
		catch(Exception ex)
		{
			System.err.println("Exception: " + ex);
			ex.printStackTrace(System.err);
		}
	}

	/**
	* @param node
	* @param out
	* @param indent
	*/
	public void printNode( Node node, PrintStream out, String indent )
	{
		NodeList children;
		String value;

		switch(node.getNodeType())
		{
		case Node.DOCUMENT_NODE:
			out.println(indent + "DOCUMENT START");
			children = node.getChildNodes();
			if (children != null)
				for(int i=0; i<children.getLength(); i++)
					printNode(children.item(i), out, indent + "\t");

		case Node.ELEMENT_NODE:
			out.println(indent + "ELEMENT " + node.getNodeName());
			NamedNodeMap attr = node.getAttributes();
			if (attr != null)
				for(int i=0; i<attr.getLength(); i++)
				{
					Node cur = attr.item(i);
					out.println(indent + "  Attribute '" + cur.getNodeName()
						+ "' = '" + cur.getNodeValue() + "'");
				}

			children = node.getChildNodes();
			if (children != null)
				for(int i=0; i<children.getLength(); i++)
					printNode(children.item(i), out, indent + "\t");

		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
			value =  node.getNodeValue();
			if (value == null)
				break;
			value = value.trim();
			if (value != null && value.length() > 0)
				out.println(indent + "CONTENT: " + value);
			break;

		case Node.PROCESSING_INSTRUCTION_NODE:
			out.println(indent + "PROCESSING_INSTRUCTION: " + node.getNodeName()
				+ " " + node.getNodeValue());
			break;

		case Node.ENTITY_REFERENCE_NODE:
			out.println(indent + "ENTITY_REFERENCE: " + node.getNodeName());
			break;
		
		}
	}

	/**
	* @param args
	*/
	public static void main( String[] args )
	{
		DomTest domTest = new DomTest();
		for(int i=0; i<args.length; i++)
			domTest.doit(args[i]);
	}
}
