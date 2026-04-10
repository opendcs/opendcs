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
package lrgs.statusxml;

import java.io.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.*;

import ilex.util.TextUtil;
import ilex.xml.*;

/**
Top-level parser for LRGS Status Snapshot XML.
@author Michael Maloney, Ilex Engineering, Inc.
*/
public class TopLevelXio implements XmlObjectParser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		ErrorHandler eh = new LoggerErrorHandler(log);
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
		byte ba[] = new byte[(int)f.length()];
		try (FileInputStream fis = new FileInputStream(f))
		{
			fis.read(ba);
		}
		TopLevelXio tlx = new TopLevelXio();
		LrgsStatusSnapshotExt lsse = tlx.parse(ba, 0, ba.length, "file:"+f.getPath());
	}
}