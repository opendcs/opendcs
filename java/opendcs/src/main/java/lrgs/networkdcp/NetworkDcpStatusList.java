/**
 * 
 */
package lrgs.networkdcp;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;


import ilex.xml.XmlOutputStream;
import ilex.util.Logger;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.statusxml.StatusXmlTags;
import ilex.xml.DomHelper;


/**
 * @author mjmaloney
 *
 */
public class NetworkDcpStatusList
{
	private ArrayList<NetworkDcpStatus> statusList = 
		new ArrayList<NetworkDcpStatus>();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	public NetworkDcpStatusList()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	public ArrayList<NetworkDcpStatus> getStatusList()
	{
		return statusList;
	}
	
	public NetworkDcpStatus getStatus(String host, int port)
	{
		for(NetworkDcpStatus nds : statusList)
			if (host.equalsIgnoreCase(nds.getHost())
			 && port == nds.getPort())
				return nds;
		NetworkDcpStatus nds = new NetworkDcpStatus(host, port);
		statusList.add(nds);
//Logger.instance().info("Created new NetworkDcpStatus for host=" + host
//+ ", port=" + port);
		return nds;
	}
	
	public void remove(String host, int port)
	{
		for(int i=0; i<statusList.size(); i++)
		{
			NetworkDcpStatus nds = statusList.get(i);
			if (host.equalsIgnoreCase(nds.getHost())
			 && port == nds.getPort())
			{
				statusList.remove(i);
				return;
			}
		}
	}
	
	public synchronized void pollAttempt(DrgsConnectCfg cfg, 
		boolean success, int numMessages)
	{
		NetworkDcpStatus nds = getStatus(cfg.host, cfg.msgPort);
		nds.setPollingMinutes(cfg.pollingPeriod);
		nds.setDisplayName(cfg.name);
		nds.pollAttempt(success, numMessages);
	}
	
	/** Called at init time to populate list from XML file. */
	public synchronized void add(NetworkDcpStatus nds)
	{
		statusList.add(nds);
	}
	
	/** 
	 * Called periodically to checkpoint a copy of the list to an XML file.
	 * Passing the XmlOutputStream allows this method to be used to send to
	 * a network as part of a status message, or to save to a file for
	 * check-pointed status.  
	 */
	public synchronized void saveToXml(XmlOutputStream xos)
	{
		// Delete statuses that have never been polled, or in last 30 days.
		for(Iterator<NetworkDcpStatus> it = statusList.iterator(); 
			it.hasNext(); )
		{
			NetworkDcpStatus nds = it.next();
			Date d = nds.getLastPollAttempt();
			if (nds.getPollingMinutes() > 0
			 && d != null
			 && System.currentTimeMillis() - d.getTime() > 30*24*3600*1000L)
				it.remove();
		}
		try
		{
			xos.startElement(StatusXmlTags.networkDcpList);
			for(NetworkDcpStatus nds : statusList)
			{
				xos.startElement(StatusXmlTags.networkDcp,
					StatusXmlTags.host, nds.getHost(), 
					StatusXmlTags.port, ""+nds.getPort());
				xos.writeElement(StatusXmlTags.displayName,
					nds.getDisplayName());
				xos.writeElement(StatusXmlTags.pollingMinutes,
					""+nds.getPollingMinutes());
				if (nds.getLastPollAttempt() != null)
					xos.writeElement(StatusXmlTags.lastPollAttempt,
						sdf.format(nds.getLastPollAttempt()));
				if (nds.getLastContact() != null)
					xos.writeElement(StatusXmlTags.lastContact,
						sdf.format(nds.getLastContact()));
				
				xos.writeElement(StatusXmlTags.numGoodPolls,
					""+nds.getNumGoodPolls());
				xos.writeElement(StatusXmlTags.numFailedPolls,
					""+nds.getNumFailedPolls());
				xos.writeElement(StatusXmlTags.numMessages,
					""+nds.getNumMessages());
				xos.endElement(StatusXmlTags.networkDcp);
			}
			xos.endElement(StatusXmlTags.networkDcpList);
		}
		catch(IOException iox)
		{
			Logger.instance().warning("Cannot save NetworkDcpList to XML: "
				+ iox);
		}
	}
	
	public synchronized void initFromXml(Element networkDcpListElem)
	{
		NodeList networkDcpNodeList = networkDcpListElem.getChildNodes();
		if (networkDcpNodeList != null)
			for(int i=0; i<networkDcpNodeList.getLength(); i++)
			{
				Node netDcpNode = networkDcpNodeList.item(i);

				if (netDcpNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element elem = (Element)netDcpNode;
					String host = elem.getAttribute("host");
					int port = 0;
					try { port = Integer.parseInt(elem.getAttribute("port")); }
					catch(NumberFormatException ex)
					{
						Logger.instance().warning(
							"Reading NetworkDcp, bad port attribute for host '"
							+ host + "' -- skipped.");
						continue;
					}
					setNetworkDcpStatus(host, port, elem);
				}
			}
	}
	
	private void setNetworkDcpStatus(String host, int port, Element elem)
	{
		NetworkDcpStatus nds = getStatus(host, port);
		NodeList netDcpAttrs = elem.getChildNodes();
		for(int j=0; j<netDcpAttrs.getLength(); j++)
		{
			Node x = netDcpAttrs.item(j);

			if (x.getNodeName().equalsIgnoreCase("DisplayName"))
				nds.setDisplayName(DomHelper.getTextContent(x));
			else if (x.getNodeName().equalsIgnoreCase("PollingMinutes"))
				nds.setPollingMinutes(
					DomHelper.getIntegerContent(x, 0, "ReadNetDcp"));
			else if (x.getNodeName().equalsIgnoreCase("LastPollAttempt"))
			{
				String dt = DomHelper.getTextContent(x);
				try
				{
					Date d = sdf.parse(dt);
					nds.setLastPollAttempt(d);
				}
				catch(ParseException ex)
				{
					Logger.instance().warning(
						"LastPollAttempt bad date format '" + dt + "'");
				}
			}
			else if (x.getNodeName().equalsIgnoreCase("LastContact"))
			{
				String dt = DomHelper.getTextContent(x);
				try
				{
					Date d = sdf.parse(dt);
					nds.setLastContact(d);
				}
				catch(ParseException ex)
				{
					Logger.instance().warning(
						"LastContact bad date format '" + dt + "'");
				}
			}
			else if (x.getNodeName().equalsIgnoreCase("NumGoodPolls"))
				nds.setNumGoodPolls(
					DomHelper.getIntegerContent(x, 0, "ReadNetDcp"));
			else if (x.getNodeName().equalsIgnoreCase("NumFailedPolls"))
				nds.setNumFailedPolls(
					DomHelper.getIntegerContent(x, 0, "ReadNetDcp"));
			else if (x.getNodeName().equalsIgnoreCase("NumMessages"))
				nds.setNumMessages(
					DomHelper.getIntegerContent(x, 0,"ReadNetDcp"));
		}
	}
}
