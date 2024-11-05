/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/09/24 13:59:01  mjmaloney
*  network DCPs
*
*/
package lrgs.statusxml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.xml.*;
import ilex.util.*;
import lrgs.networkdcp.NetworkDcpStatus;


/**
This class maps the DECODES XML representation for Process elements.
*/
public class NetworkDcpStatusXio
	implements XmlObjectParser, TaggedLongOwner, TaggedStringOwner
{
	/// Top of the parser hierarchy
	private NetworkDcpStatus nds;

	private static final int dnTag   = 0;
	private static final int pmTag   = 1;
	private static final int lpaTag   = 2;
	private static final int lcTag   = 3;
	private static final int ngpTag = 4;
	private static final int nfpTag = 5;
	private static final int nmTag = 6;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

	/**
	  Construct parser.
	  @param ap the AttachedProcess to populate from XML data
	*/
	public NetworkDcpStatusXio(NetworkDcpStatus nds)
	{
		super();
		this.nds = nds;
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.networkDcp; }

	/** No content characters expected -- only sub-elements. */
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		if (!TextUtil.isAllWhitespace(new String(ch, start, length)))
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
		if (localName.equalsIgnoreCase(StatusXmlTags.displayName))
			hier.pushObjectParser(new TaggedStringSetter(this, dnTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.pollingMinutes))
			hier.pushObjectParser(new TaggedLongSetter(this, pmTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.lastPollAttempt))
			hier.pushObjectParser(new TaggedStringSetter(this, lpaTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.lastContact))
			hier.pushObjectParser(new TaggedStringSetter(this, lcTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.numGoodPolls))
			hier.pushObjectParser(new TaggedLongSetter(this, ngpTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.numFailedPolls))
			hier.pushObjectParser(new TaggedLongSetter(this, nfpTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.numMessages))
			hier.pushObjectParser(new TaggedLongSetter(this, nmTag));
		else
		{
			Logger.instance().log(Logger.E_WARNING,
				"Invalid element '" + localName + "' under " + myName()
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
	  From TaggedLongOwner, called from TaggedLongSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, long value)
	{
		switch(tag)
		{
		case pmTag:
			nds.setPollingMinutes((int)value);
			break;
		case ngpTag:
			nds.setNumGoodPolls(value);
			break;
		case nfpTag:
			nds.setNumFailedPolls(value);
			break;
		case nmTag:
			nds.setNumMessages(value);
			break;
		}
	}

	/**
	  From TaggedStringOwner, called from TaggedStringSetter when string
	  elements are parsed.
	  @param tag numeric tag defined above
	  @param value the value
	*/
	public void set(int tag, String value)
	{
		switch(tag)
		{
		case dnTag:
			nds.setDisplayName(value);
			break;
		case lpaTag:
			try { nds.setLastPollAttempt(sdf.parse(value)); }
            catch (ParseException ex)
            {
            	Logger.instance().warning(
            		"Bad last-poll-attempt date format '" + value + "': "
            		+ ex + " -- ignored.");
            }
			break;
		case lcTag:
			try { nds.setLastContact(sdf.parse(value)); }
			catch (ParseException ex)
			{
            	Logger.instance().warning(
            		"Bad last-contact date format '" + value + "': "
            		+ ex + " -- ignored.");
			}
			break;
		}
	}
}
