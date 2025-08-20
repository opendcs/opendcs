/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2016/03/24 19:20:16  mmaloney
*  Added hideHostNames property.
*
*  Revision 1.2  2016/02/29 22:26:43  mmaloney
*  Encapsulate 'name'.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2010/09/21 01:33:20  mmaloney
*  Don't include downlinks like LRIT and DDS if they are not used in this LRGS.
*
*  Revision 1.4  2010/01/07 21:47:37  shweta
*  Enhancements for multiple DDS Receive  group.
*
*  Revision 1.3  2008/09/24 13:59:01  mjmaloney
*  network DCPs
*
*  Revision 1.2  2008/05/05 15:03:08  cvs
*  Algorithm Editor Updates
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2008/01/14 14:57:35  mmaloney
*  dev
*
*  Revision 1.10  2007/08/08 17:46:40  mmaloney
*  *** empty log message ***
*
*  Revision 1.9  2006/12/04 17:35:44  mmaloney
*  dev
*
*  Revision 1.8  2005/10/20 18:01:06  mmaloney
*  event nums
*
*  Revision 1.7  2005/10/11 17:55:19  mmaloney
*  dev
*
*  Revision 1.6  2005/07/28 20:22:14  mjmaloney
*  LRGS Monitor backward compatibility with LRGS 4.0.
*
*  Revision 1.5  2005/06/30 15:15:29  mjmaloney
*  Java Archive Development.
*
*  Revision 1.4  2005/06/28 17:37:02  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.3  2004/09/02 13:09:05  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/06/08 19:31:36  mjmaloney
*  Final cosmetic mods
*
*  Revision 1.1  2004/05/04 18:03:57  mjmaloney
*  Moved from statusgui package to here.
*
*/
package lrgs.statusxml;

import java.io.IOException;
import java.util.StringTokenizer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ilex.xml.*;
import ilex.util.*;

import lrgs.apistatus.*;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.networkdcp.NetworkDcpStatus;
import lrgs.networkdcp.NetworkDcpStatusList;

/**
This class maps the DECODES XML representation for LrgsStatusSnapshot elements.

@author Michael Maloney, Ilex Engineering, Inc.
*/
public class LrgsStatusSnapshotXio
	implements XmlObjectParser, TaggedBooleanOwner, TaggedLongOwner,
	TaggedStringOwner, XmlObjectWriter
{
	/// Top of the parser hierarchy
	private LrgsStatusSnapshotExt lsse;

	private static final int iuTag      = 0;
	private static final int stTag      = 1;
	private static final int mcTag      = 2;
	private static final int cncTag     = 3;
	private static final int mdTag      = 4;
	private static final int ssTag      = 5;
	private static final int mjvTag     = 6;
	private static final int mivTag     = 7;
	private static final int ddTag      = 8;
	private static final int fvTag      = 9;

	private boolean inNetworkDcpList = false;
	private boolean hideHostNames = false;
	
	/**
	  Construct parser.
	  @param lsse the LrgsStatusSnapshotExt to populate from XML data
	*/
	public LrgsStatusSnapshotXio(LrgsStatusSnapshotExt lsse)
	{
		super();
		this.lsse = lsse;
	}

	/** @return XML tag for this element */
	public String myName() { return StatusXmlTags.LrgsStatusSnapshot; }

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
		if (localName.equalsIgnoreCase(StatusXmlTags.isUsable))
			hier.pushObjectParser(new TaggedBooleanSetter(this, iuTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.systemStatus))
			hier.pushObjectParser(new TaggedStringSetter(this, ssTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.SystemTime))
			hier.pushObjectParser(new TaggedLongSetter(this, stTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.MaxClients))
			hier.pushObjectParser(new TaggedLongSetter(this, mcTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.CurrentNumClients))
			hier.pushObjectParser(new TaggedLongSetter(this, cncTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.MaxDownlinks))
			hier.pushObjectParser(new TaggedLongSetter(this, mdTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.majorVersion))
			hier.pushObjectParser(new TaggedLongSetter(this, mjvTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.minorVersion))
			hier.pushObjectParser(new TaggedLongSetter(this, mivTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.fullVersion))
			hier.pushObjectParser(new TaggedStringSetter(this, fvTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.domsatDropped))
			hier.pushObjectParser(new TaggedStringSetter(this, ddTag));
		else if (localName.equalsIgnoreCase(StatusXmlTags.ArchiveStatistics))
		{
			hier.pushObjectParser(new ArchiveStatisticsXio(lsse));
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.Process))
		{
			String slots = XmlUtils.getAttrIgnoreCase(atts, "slot");
			String pids = XmlUtils.getAttrIgnoreCase(atts, "pid");
			try
			{
				int slot = Integer.parseInt(slots);
				int pid = Integer.parseInt(pids);
				AttachedProcess ap = 
					new AttachedProcess(pid, "", "", "", -1, 0, 0, "", (short)0);
				lsse.addProcess(ap, slot);
				hier.pushObjectParser(new ProcessXio(ap));
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_WARNING, 
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
			try
			{
				int slot = Integer.parseInt(slots);
				DownLink dl = 
					new DownLink(name, (short)0, false, false, (short)0, 0, -1, "", "");
				lsse.addDownLink(dl, slot);
				hier.pushObjectParser(new DownLinkXio(lsse, dl, slot));
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_WARNING, 
					"Invalid slot in DownLink record, slot='"
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
				QualityMeasurement qm = new QualityMeasurement(false, 0, 0, 0);
				lsse.addQualityMeasurement(qm, hour);
				hier.pushObjectParser(new QualityMeasurementXio(qm));
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_WARNING, 
					"Invalid hour in Quality record, hour='"
					+ hours + "' -- must be 0...23 -- Quality ignored.");
				hier.pushObjectParser(new ElementIgnorer());
			}
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.networkDcpList))
		{
			inNetworkDcpList = true;
			// Stay in this parser so we get the networkDcp tags
		}
		else if (localName.equalsIgnoreCase(StatusXmlTags.networkDcp))
		{
			if (lsse.networkDcpStatusList == null)
				lsse.networkDcpStatusList = new NetworkDcpStatusList();
			String portstr = XmlUtils.getAttrIgnoreCase(atts, "port");
			int port = 0;
			try { port = Integer.parseInt(portstr.trim()); }
			catch(Exception ex) {}
			String host = XmlUtils.getAttrIgnoreCase(atts, "host");
			NetworkDcpStatus nds = 
				lsse.networkDcpStatusList.getStatus(host, port);
			hier.pushObjectParser(new NetworkDcpStatusXio(nds));
		}
		else
		{
			Logger.instance().debug1(
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
		case ssTag:
			lsse.systemStatus = value;
			break;
		case ddTag:
			parseDomsatDropped(value);
			break;
		case fvTag:
			lsse.fullVersion = value;
		}
	}

	private void parseDomsatDropped(String dd)
	{
		StringTokenizer st = new StringTokenizer(dd);
		for(int i=0; i<24 && st.hasMoreTokens(); i++)
		{
			String t = st.nextToken();
			try { lsse.domsatDropped[i] = Integer.parseInt(t); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(
					"Invalid string '" + t + "' in domsatDropped.");
			}
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
		case stTag:
			int t = (int)(value / 1000L);
			lsse.lss.lrgsTime = t;
			lsse.lss.currentHour = (short)((t % (24*3600)) / 3600);
			break;
		case mcTag:
			lsse.setMaxClients((int)value);
			break;
		case cncTag:
			lsse.currentNumClients = (int)value;
			break;
		case mdTag:
			lsse.setMaxDownlinks((int)value);
			break;
		case mjvTag:
			lsse.majorVersion = (int)value;
			break;
		case mivTag:
			lsse.minorVersion = (int)value;
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
		case iuTag:
			lsse.isUsable = value;
			break;
		}
	}

	/**
	 * Writes this object's data, along with subordinates, to an XML file.
	 * @param xos the output stream object
	 * @throws IOException on IO error
	 */
	public void writeXml( XmlOutputStream xos ) throws IOException
	{
		xos.startElement(StatusXmlTags.LrgsStatusSnapshot, 
			StatusXmlTags.hostname, lsse.hostname);
		xos.writeElement(StatusXmlTags.systemStatus, "" + lsse.systemStatus);
		xos.writeElement(StatusXmlTags.isUsable, "" + lsse.isUsable);
		xos.writeElement(StatusXmlTags.SystemTime,""+(lsse.lss.lrgsTime*1000L));
		xos.writeElement(StatusXmlTags.MaxClients, ""+lsse.maxClients);
		xos.writeElement(StatusXmlTags.CurrentNumClients, 
			"" + lsse.currentNumClients);
		xos.writeElement(StatusXmlTags.majorVersion,""+lsse.majorVersion);
		xos.writeElement(StatusXmlTags.minorVersion,""+lsse.minorVersion);
		if (lsse.fullVersion != null)
			xos.writeElement(StatusXmlTags.fullVersion,""+lsse.fullVersion);

		// Archive Statistics 
		xos.startElement(StatusXmlTags.ArchiveStatistics);
		xos.writeElement(StatusXmlTags.dirOldest, 
			""+lsse.lss.arcStats.dirOldest);
		xos.writeElement(StatusXmlTags.dirNext, ""+lsse.lss.arcStats.dirNext);
		xos.writeElement(StatusXmlTags.dirWrap, ""+lsse.lss.arcStats.dirWrap);
		xos.writeElement(StatusXmlTags.dirSize, ""+lsse.lss.arcStats.dirSize);
		xos.writeElement(StatusXmlTags.oldestOffset, 
			""+lsse.lss.arcStats.oldestOffset);
		xos.writeElement(StatusXmlTags.oldestMsgTime, 
			""+lsse.lss.arcStats.oldestMsgTime);
		xos.writeElement(StatusXmlTags.lastSeqNum, 
			""+lsse.lss.arcStats.lastSeqNum);
		xos.writeElement(StatusXmlTags.maxMessages, 
			""+lsse.lss.arcStats.maxMessages);
		xos.writeElement(StatusXmlTags.maxBytes, ""+lsse.lss.arcStats.maxBytes);
		xos.endElement(StatusXmlTags.ArchiveStatistics);

		for(int i=0; i<lsse.maxClients; i++)
		{
			AttachedProcess ap = lsse.lss.attProcs[i];
			if (ap.pid > 0)
			{
				xos.startElement(StatusXmlTags.Process, 
					"slot", ""+i, "pid", ""+ap.pid);
				String hostname = ap.getName();
				if (hideHostNames)
					hostname = "-";
				xos.writeElement(StatusXmlTags.name, hostname);
				xos.writeElement(StatusXmlTags.type, ap.type);
				xos.writeElement(StatusXmlTags.user, ap.user);
				xos.writeElement(StatusXmlTags.status, ap.status);
				xos.writeElement(StatusXmlTags.LastSeqNum, 
					"" + ap.lastSeqNum);
				xos.writeElement(StatusXmlTags.LastPollTime, 
					"" + ap.lastPollTime);
				xos.writeElement(StatusXmlTags.LastMsgTime, 
					"" + ap.lastMsgTime);
				xos.writeElement(StatusXmlTags.staleCount, 
					"" + ap.stale_count);
				if (ap.ddsVersion != null)
					xos.writeElement(StatusXmlTags.ddsVersion, ap.ddsVersion);
				xos.endElement(StatusXmlTags.Process);
			}
		}

		xos.writeElement(StatusXmlTags.MaxDownlinks, ""+lsse.maxDownlinks);
		for(int slot=0; slot<lsse.maxDownlinks; slot++)
		{
			DownLink dnl = lsse.lss.downLinks[slot];

			if (dnl.name != null && dnl.name.length() != 0)
			{
				if (dnl.name.startsWith("LRIT")
				 && !LrgsConfig.instance().enableLritRecv)
					continue;
				else if (dnl.name.startsWith("DDS")
				 && !LrgsConfig.instance().enableDdsRecv)
					continue;
				
				xos.startElement(StatusXmlTags.DownLink, "slot", ""+slot, 
					"name", dnl.name);
				xos.writeElement(StatusXmlTags.type, ""+dnl.type);
				xos.writeElement(StatusXmlTags.StatusCode, ""+dnl.statusCode);
				xos.writeElement(StatusXmlTags.status, dnl.statusString);
				xos.writeElement(StatusXmlTags.LastMsgRecvTime, 
					""+dnl.lastMsgRecvTime);				
				xos.writeElement(StatusXmlTags.group, dnl.group);
				
				if (dnl.hasSeqNum)
					xos.writeElement(StatusXmlTags.LastSeqNum, 
						""+dnl.lastSeqNum);
				if (dnl.hasBER)
					xos.writeElement(StatusXmlTags.BER, dnl.BER);

				if (lsse.downlinkQMs[slot] != null)
				{
					for(int h = 0; h < 24; h++)
					{
						QualityMeasurement qm = 
							lsse.downlinkQMs[slot].dl_qual[h];
						if (qm.containsData)
						{
							xos.startElement(StatusXmlTags.Quality, 
								"hour", ""+h);
							xos.writeElement(StatusXmlTags.numGood, 
								""+qm.numGood);
							if (qm.numDropped > 0)
								xos.writeElement(StatusXmlTags.numDropped, 
									""+qm.numDropped);
							if (qm.numRecovered > 0)
								xos.writeElement(StatusXmlTags.numRecovered, 
									""+qm.numRecovered);
							xos.endElement(StatusXmlTags.Quality);
						}
					}
				}
				xos.endElement(StatusXmlTags.DownLink);
			}
		}

		for(int h = 0; h < 24; h++)
		{
			QualityMeasurement qm = lsse.lss.qualMeas[h];
			if (qm != null && qm.containsData)
			{
				xos.startElement(StatusXmlTags.Quality, "hour", ""+h);
				xos.writeElement(StatusXmlTags.numGood, ""+qm.numGood);
				xos.writeElement(StatusXmlTags.numDropped, ""+qm.numDropped);
				xos.writeElement(StatusXmlTags.numRecovered, ""+qm.numRecovered);
				xos.endElement(StatusXmlTags.Quality);
			}
		}

		StringBuffer sb = new StringBuffer();
		for(int h=0; h<24; h++)
			sb.append(" " + lsse.domsatDropped[h]);
		xos.writeElement(StatusXmlTags.domsatDropped, sb.toString());
		
		if (lsse.networkDcpStatusList != null)
			lsse.networkDcpStatusList.saveToXml(xos);

		xos.endElement(StatusXmlTags.LrgsStatusSnapshot);
	}

	public void setHideHostNames(boolean hideHostNames)
	{
		this.hideHostNames = hideHostNames;
	}
}
