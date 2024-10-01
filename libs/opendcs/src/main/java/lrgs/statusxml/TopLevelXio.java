/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/07/28 20:22:14  mjmaloney
*  LRGS Monitor backward compatibility with LRGS 4.0.
*
*  Revision 1.5  2005/06/30 15:15:29  mjmaloney
*  Java Archive Development.
*
*  Revision 1.4  2004/09/02 13:09:06  mjmaloney
*  javadoc
*
*  Revision 1.3  2004/06/03 15:34:16  mjmaloney
*  LRIT release prep
*
*  Revision 1.2  2004/05/25 20:16:28  mjmaloney
*  dev
*
*  Revision 1.1  2004/05/04 18:03:59  mjmaloney
*  Moved from statusgui package to here.
*
*/
package lrgs.statusxml;

import java.io.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;

import ilex.xml.*;
import ilex.util.*;
import lrgs.apistatus.LrgsStatusSnapshot;

/**
Top-level parser for LRGS Status Snapshot XML.
@author Michael Maloney, Ilex Engineering, Inc.
*/
public class TopLevelXio
	implements XmlObjectParser
{
	/** SAX parser object */
	private XMLReader parser;

	/** Manages hierarchy of parsers */
	private XmlHierarchyParser xhp;

	/** Save original input file name for use in log messages. */
	String inputName;

	/** The object currently being parsed. */
	LrgsStatusSnapshotExt lsse;

	/** default constructor */
	public TopLevelXio()
	    throws ParserConfigurationException, SAXException
	{
		super();

        SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		parser = sp.getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);

		ErrorHandler eh = new PrintStreamErrorHandler(System.out);
		xhp = new XmlHierarchyParser(eh);
		setErrorHandler(eh);
		parser.setContentHandler(xhp);
	}

	/** Sets the SAX error handler. */
	public void setErrorHandler(ErrorHandler eh)
	{
		parser.setErrorHandler(eh);
		xhp.setErrorHandler(eh);
	}

	/**
	  Parses the passed buffer into an LRGS Snapshot
	  @param data a byte array containing the complete XML data.
	  @param offset offset into data where XML starts
	  @param len length of XML data
	  @param inputName for log messages
	  @return LrgsStatusSnapshotExt parsed from data
	*/
	public synchronized LrgsStatusSnapshotExt
		parse(byte[] data, int offset, int len, String inputName)
		throws IOException, SAXException
	{
		this.inputName = inputName;

		xhp.setFileName(inputName);
		lsse = new LrgsStatusSnapshotExt();
		initializeSnapshot(lsse);
		ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, len);
		lsse.majorVersion = 4;
		lsse.minorVersion = 0;
		parse(bais);
		return lsse;
	}

	/** Initialize snapshot before parsing. */
	public void initializeSnapshot(LrgsStatusSnapshotExt lsse)
	{
	}

	/**
	  Starts the parsing using the passed InputStream.
	  @param is the InputStream to parse
	*/
	public void parse(InputStream is)
		throws IOException, SAXException
	{
		xhp.pushObjectParser(this);
		parser.parse(new InputSource(is));
		inputName = "";
	}

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException("No character data expected in TopLevelElement");
	}

	/**
	  Called when sub-element seen.
	  The only element expected at the top level is "LrgsStatusSnapshot".
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the new element
	  @param qname ignored
	  @param atts attributes from the new element
	*/
	public void startElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname, Attributes atts)
		throws SAXException
	{
		if (localName.equalsIgnoreCase(StatusXmlTags.LrgsStatusSnapshot))
		{
			lsse.hostname = atts.getValue(StatusXmlTags.hostname);
			hier.pushObjectParser(new LrgsStatusSnapshotXio(lsse));
		}
		else
		{
			String msg = 
				"Unexpected tag '" + localName + "' at top level of file.";
			warning(msg);
			throw new SAXException(msg);
		}
	}

	/**
	  Signals the end of the current element.
	  @param hier the parser stack
	  @param namespaceURI ignored
	  @param localName name of the element
	  @param qname ignored
	*/
	public void endElement(XmlHierarchyParser hier,
		String namespaceURI, String localName, String qname)
		throws SAXException
	{
		hier.popObjectParser();
	}

	/** Does nothing. */
    public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	}

	/**
	  Logs a warning message with the input name prefix.
	  @param msg the message
	*/
	public void warning(String msg)
	{
		Logger.instance().log(Logger.E_WARNING, inputName + ": " + msg);
	}

	/**
	  Logs a failure message with the input name prefix.
	  @param msg the message
	*/
	public void failure(String msg)
	{
		Logger.instance().log(Logger.E_FAILURE, inputName + ": " + msg);
	}

	/**
	Test main: Parses all files given on command line & prints them
	to System.out.
	By comparing the input and output, you can verify that both the parse
	and print functioned properly.
	  @param args command line arguments
	*/
	public static void main(String args[])
		throws IOException, SAXException, ParserConfigurationException
	{
		File f = new File(args[0]);
		FileInputStream fis = new FileInputStream(f);
		byte ba[] = new byte[(int)f.length()];
		fis.read(ba);
		fis.close();
//System.out.println("Loaded file of " + ba.length + " bytes:");
//System.out.println(new String(ba));
		
		TopLevelXio tlx = new TopLevelXio();
		LrgsStatusSnapshotExt lsse = tlx.parse(ba, 0, ba.length, "file:"+f.getPath());
	}
}
