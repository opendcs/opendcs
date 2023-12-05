package org.opendcs.odcsapi.lrgsclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.opendcs.odcsapi.beans.ApiLrgsDownlinkStatus;
import org.opendcs.odcsapi.beans.ApiLrgsProcStatus;
import org.opendcs.odcsapi.beans.ApiLrgsStatus;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiTextUtil;
import org.opendcs.odcsapi.xml.ElementIgnorer;
import org.opendcs.odcsapi.xml.PrintStreamErrorHandler;
import org.opendcs.odcsapi.xml.TaggedBooleanOwner;
import org.opendcs.odcsapi.xml.TaggedBooleanSetter;
import org.opendcs.odcsapi.xml.TaggedLongOwner;
import org.opendcs.odcsapi.xml.TaggedLongSetter;
import org.opendcs.odcsapi.xml.TaggedStringOwner;
import org.opendcs.odcsapi.xml.TaggedStringSetter;
import org.opendcs.odcsapi.xml.XmlHierarchyParser;
import org.opendcs.odcsapi.xml.XmlObjectParser;
import org.opendcs.odcsapi.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
This class maps the DECODES XML representation for LrgsStatusSnapshot elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class LrgsStatusXio
	implements XmlObjectParser, TaggedBooleanOwner, TaggedLongOwner,
	TaggedStringOwner
{
	public static final String module = "LrgsStatusXio";
	
	/// Top of the parser hierarchy
	private ApiLrgsStatus lrgsStatus;

	private static final int isUsableTag      = 0;
	private static final int systemTimeTag      = 1;
	private static final int maxClientsTag      = 2;
	private static final int currentNumClientsTag     = 3;
	private static final int maxDownlinksTag      = 4;
	private static final int systemStatusTag      = 5;
	private static final int majorVersionTag     = 6;
	private static final int minorVersionTag     = 7;
	private static final int ddTag      = 8;
	private static final int fullVersionTag      = 9;

	private boolean inNetworkDcpList = false;
	
	/** Manages hierarchy of parsers */
	private XmlHierarchyParser xhp = null;
	/** SAX parser object */
	private XMLReader parser = null;

	
	/**
	  Construct parser.
	  @param lrgsStatus the LrgsStatusSnapshotExt to populate from XML data
	*/
	public LrgsStatusXio()
		throws ParserConfigurationException, SAXException
	{
		super();
		
        SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		parser = sp.getXMLReader();
		parser.setFeature("http://xml.org/sax/features/namespaces", true);

		ErrorHandler eh = new PrintStreamErrorHandler(System.out);
		xhp = new XmlHierarchyParser(eh);
		parser.setErrorHandler(eh);
		xhp.setErrorHandler(eh);
		parser.setContentHandler(xhp);
	}

	public ApiLrgsStatus parse(byte[] data, int offset, int len, String inputName)
		throws IOException, SAXException
	{
		lrgsStatus = new ApiLrgsStatus();
		xhp.setFileName(inputName);
		ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, len);
			
		xhp.pushObjectParser(this);
		parser.parse(new InputSource(bais));

		return lrgsStatus;
	}

	
	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.LrgsStatusSnapshot; }

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!ApiTextUtil.isAllWhitespace(new String(ch, start, length)))
			throw new SAXException(
				"No character data expected within " + myName());
	}

	/**
	  Called when sub-element seen under Archive Statistics.
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
			String hn = XmlUtils.getAttrIgnoreCase(atts, "hostname");
			if (hn != null)
				lrgsStatus.setHostname(hn);
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.isUsable))
			hier.pushObjectParser(new TaggedBooleanSetter(this, isUsableTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.systemStatus))
			hier.pushObjectParser(new TaggedStringSetter(this, systemStatusTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.SystemTime))
			hier.pushObjectParser(new TaggedLongSetter(this, systemTimeTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.MaxClients))
			hier.pushObjectParser(new TaggedLongSetter(this, maxClientsTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.CurrentNumClients))
			hier.pushObjectParser(new TaggedLongSetter(this, currentNumClientsTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.MaxDownlinks))
			hier.pushObjectParser(new TaggedLongSetter(this, maxDownlinksTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.majorVersion))
			hier.pushObjectParser(new TaggedLongSetter(this, majorVersionTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.minorVersion))
			hier.pushObjectParser(new TaggedLongSetter(this, minorVersionTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.fullVersion))
			hier.pushObjectParser(new TaggedStringSetter(this, fullVersionTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.domsatDropped))
			hier.pushObjectParser(new TaggedStringSetter(this, ddTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.ArchiveStatistics))
		{
			hier.pushObjectParser(new ArchiveStatisticsXio(lrgsStatus));
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.Process))
		{
			String slots = XmlUtils.getAttrIgnoreCase(atts, "slot");
			String pids = XmlUtils.getAttrIgnoreCase(atts, "pid");
			ApiLrgsProcStatus procStat = new ApiLrgsProcStatus();
			try
			{
				int slot = Integer.parseInt(slots);
				int pid = Integer.parseInt(pids);
				procStat.setSlot(slot);
				procStat.setPid(pid);
				lrgsStatus.getProcStatus().add(procStat);
				hier.pushObjectParser(new ProcessXio(procStat, this));
			}
			catch(NumberFormatException ex)
			{
				warning(
					"Invalid slot or pid in Process record, slot='"
					+ slots + "', pid='" + pids 
					+ "' -- must be numbers -- Process ignored.");
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.DownLink))
		{
			String slots = XmlUtils.getAttrIgnoreCase(atts, "slot");
			String name = XmlUtils.getAttrIgnoreCase(atts, "name");
			ApiLrgsDownlinkStatus dlStat = new ApiLrgsDownlinkStatus();
			try
			{
				dlStat.setSlot(Integer.parseInt(slots));
				dlStat.setName(name);
				lrgsStatus.getDownlinkStatus().add(dlStat);
				hier.pushObjectParser(new DownLinkXio(dlStat, this));
			}
			catch(NumberFormatException ex)
			{
				warning("Invalid slot in DownLink record, slot='"
					+ slots + "' -- must be number -- DownLink ignored.");
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.Quality))
		{
			String hours = XmlUtils.getAttrIgnoreCase(atts, "hour");
			try
			{
				int hour = Integer.parseInt(hours);
				if (hour < 0 || hour > 23)
				{
					warning("Invalid hour '" + hour + "' in (archive) Quality measurement. Ignored.");
					hier.pushObjectParser(new ElementIgnorer());
				}
				hier.pushObjectParser(
					new QualityMeasurementXio(lrgsStatus.getHourlyArchiveQuality()[hour], this));
			}
			catch(NumberFormatException ex)
			{
				warning( 
					"Invalid hour in (archive) Quality record, hour='"
					+ hours + "' -- must be 0...23 -- Quality ignored.");
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
//		else if (localName.equalsIgnoreCase(StatusXmlTags.networkDcpList))
//		{
//			inNetworkDcpList = true;
//			// Stay in this parser so we get the networkDcp tags
//		}
//		else if (localName.equalsIgnoreCase(StatusXmlTags.networkDcp))
//		{
//			if (lrgsStatus.networkDcpStatusList == null)
//				lrgsStatus.networkDcpStatusList = new NetworkDcpStatusList();
//			String portstr = XmlUtils.getAttrIgnoreCase(atts, "port");
//			int port = 0;
//			try { port = Integer.parseInt(portstr.trim()); }
//			catch(Exception ex) {}
//			String host = XmlUtils.getAttrIgnoreCase(atts, "host");
//			NetworkDcpStatus nds = 
//				lrgsStatus.networkDcpStatusList.getStatus(host, port);
//			hier.pushObjectParser(new NetworkDcpStatusXio(nds));
//		}
		else
		{
			debug("Invalid element '" + localName + "' under " + myName()
				+ " -- skipped.");
			hier.pushObjectParser(new ElementIgnorer());
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
		if (inNetworkDcpList)
		{
			inNetworkDcpList = false;
			return;
		}
		if (!localName.equalsIgnoreCase(myName()))
			throw new SAXException(
				"Parse stack corrupted: got end tag for " + localName
				+ ", expected " + myName());
		hier.popObjectParser();
	}

	/** Does nothing. */
    public void ignorableWhitespace (char ch[], int start, int length)
		throws SAXException
	{
	}

	/**
	 * From TaggedStringOwner, called when string elements are parsed.
	 * @param tag numeric tag defined above.
	 * @param value the string value parse
	 */
	public void set(int tag, String value)
	{
		switch(tag)
		{
		case systemStatusTag:
			lrgsStatus.setSystemStatus(value);
			break;
		case fullVersionTag:
			lrgsStatus.setLrgsVersion(value);
		}
	}

	/**
	  From TaggedLongOwner, called from TaggedLongSetter when long
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case systemTimeTag:
			lrgsStatus.setSystemTime(new Date(value));
			break;
		case maxClientsTag:
			lrgsStatus.setMaxClients((int)value);
			break;
		case currentNumClientsTag:
			lrgsStatus.setCurrentNumClients((int)value);
			break;
		}
	}

	/**
	  From TaggedBooleanOwner, called from TaggedBooleanSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, boolean value)
	{
		switch(tag)
		{
		case isUsableTag:
			lrgsStatus.setIsUsable(value);
			break;
		}
	}

	public void warning(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).warning(module + " " + msg);
	}

	public void info(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).info(module + " " + msg);
	}

	public void debug(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).fine(module + " " + msg);
	}

}
