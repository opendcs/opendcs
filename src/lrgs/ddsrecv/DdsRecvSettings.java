/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.9  2013/09/26 13:38:09  mmaloney
*  Refactor to allow the same netlist to be used in multiple groups.
*
*/
package lrgs.ddsrecv;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import lrgs.common.BadConfigException;
import lrgs.common.NetworkList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
Singleton class holding settings for the DrgsInput application.
*/
public class DdsRecvSettings
{
	public ArrayList<DdsRecvConnectCfg> connectCfgs = new ArrayList<DdsRecvConnectCfg>();
	private static DdsRecvSettings _instance = null;
	private String cfgFileName = null;
	private static final String module = "DdsRecvSettings";
	public int timeout = 90;

	public static boolean readNetworkLists = true;
	boolean decodesAll = false;
	boolean decodesProduction = false;
	private long lastConfigRead = 0L;
	
	private boolean isReloaded = false;
	
	private ArrayList<NetlistGroupAssoc> netlistGroupAssociations = new ArrayList<NetlistGroupAssoc>();

	/** Default constructor. */
	private DdsRecvSettings()
	{
	}

	/** @return singleton instance. */
	public static DdsRecvSettings instance()
	{
		if (_instance == null)
			_instance = new DdsRecvSettings();
		return _instance;
	}

	/**
	  Pass the name of the XML file containing the DDS Input Process
	  configuration. Sets internal variables according to that configuration.
	  @param filename the XML configuration file name
	*/
	public synchronized void setFromFile(String filename)
		throws BadConfigException
	{
		cfgFileName = filename;
		Logger.instance().log(Logger.E_INFORMATION,
			module + ": Parsing '" + filename + "'");

		Document doc;
		try
		{
			doc = DomHelper.readFile(module, filename);
		}
		catch(ilex.util.ErrorException ex)
		{
			throw new BadConfigException(ex.toString());
		}

		setFromDoc(doc, cfgFileName);
	}

	public void setFromDoc(Document doc, String cfgFileName)
		throws BadConfigException
	{
		resetToDefaults();
		lastConfigRead = System.currentTimeMillis();
		
		Node ddsrecvElement = doc.getDocumentElement();
		if (!ddsrecvElement.getNodeName().equalsIgnoreCase("ddsrecvconf"))
		{
			String s = module 
				+ ": Wrong type of configuration file -- Cannot initialize. "
				+ "Root element is not 'ddsrecvconf'.";
			Logger.instance().warning(
				DdsRecv.module + ":" + DdsRecv.EVT_BAD_CONFIG + "- " + s);
			throw new BadConfigException(s);
		}

		NodeList children = ddsrecvElement.getChildNodes();
		if (children != null)
		{
			for(int i=0; i<children.getLength(); i++)
			{
				Node node = children.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					if (node.getNodeName().equalsIgnoreCase("connection"))
					{
						addConnectionElement(node);
					}
					else if (node.getNodeName().equalsIgnoreCase("networkList"))
					{
						String name = DomHelper.getTextContent(node);
						Element elem = (Element)node;						
						String groupName = DomHelper.findAttr(elem, "group");
						if (groupName == null || groupName.trim().length() == 0)
							groupName = NetlistGroupAssoc.DEFAULT_GROUP;
						netlistGroupAssociations.add(
							new NetlistGroupAssoc(name, groupName));
					}
					else if (node.getNodeName().equalsIgnoreCase("timeout"))
					{
						timeout = DomHelper.getIntegerContent(node, timeout, "ddsrecv");
					}
					else
						Logger.instance().warning(
							"Unrecognized configuration element '"
							+ node.getNodeName() + " in " + cfgFileName
							+ " (ignored)");
				}
			}
		}

		if (!readNetworkLists)
			return;
		
		// Resolve each named network list and populate networkLists Vector.
		decodes.db.Database decDb = decodes.db.Database.getDb();
		if (decDb != null)
		{
			try 
			{
				decDb.networkListList.read(); 
				decodes.db.NetworkList.legacyNetlistDir = 
					EnvExpander.expand("$LRGSHOME/tmp");
			}
			catch(decodes.db.DatabaseException ex)
			{
				Logger.instance().warning(
					module + ": Error in DECODES database interface: " + ex);
				decDb = null;
			}
		}

		decodesAll = decodesProduction = false;
		for(NetlistGroupAssoc nga : netlistGroupAssociations)
		{
			Logger.instance().info(module + " Evaluating netlist '" +
				nga.getNetlistName() + "' in group '" + nga.getGroupName() + "'");
			if (nga.getNetlistName().equalsIgnoreCase("<all>")
			 && decodes.db.Database.getDb() != null)
			{
				decodesAll = true;
				continue;
			}
			if (nga.getNetlistName().equalsIgnoreCase("<production>")
			 && decodes.db.Database.getDb() != null)
			{
				decodesProduction = true;
				continue;
			}
			Logger.instance().info(module + " Looking for netlist '" +
				nga.getNetlistName() + "'");
			// find the list -- either in DECODES database or netlist dir.
			if (decDb != null)
			{
				try
				{
					decodes.db.NetworkList decNl = 
						decDb.networkListList.find(nga.getNetlistName());
					if (decNl != null)
					{
						Logger.instance().info(module 
							+ " Reading DECODES network list '" + decNl.name
					 		+ "'");
						decNl.read();
						decNl.prepareForExec();
						//Code added to check for null on legacyNetworkList
						if (decNl.legacyNetworkList != null)
						{
							decNl.legacyNetworkList.setHandle(decNl);
							nga.setNetworkList(decNl.legacyNetworkList);
						}
						continue;
					}
				}
				catch(decodes.db.DatabaseException ex)
				{
					Logger.instance().warning(
						module + ": Can't read network list '" + nga.getNetlistName()
						+ "' from DECODES database: " + ex);
				}
			}
			String path = EnvExpander.expand("$LRGSHOME/netlist/" + nga.getNetlistName());
			File nlfile = new File(path);
			if (!nlfile.canRead())
			{
				path = path + ".nl";
				nlfile = new File(path);
				if (!nlfile.canRead())
				{
					Logger.instance().warning(
						module + ": Network list '" + nga.getNetlistName()
						+ "' not found in DECODES database or $LRGSHOME/netlist"
						+ " -- ignored.");
					continue;
				}
			}
			try
			{
				Logger.instance().info(module 
					+ " Reading Legacy network list '" + nlfile.getName()
			 		+ "'");
				NetworkList nl = new NetworkList(nlfile);
				nga.setNetworkList(nl);
			}
			catch(IOException ex)
			{
				Logger.instance().warning(
					module + ": Error reading legeacy network list '" 
						+ path + "': " + ex);
				continue;
			}
		}
	}

	private void addConnectionElement(Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
		int num = -1;
		String ns = DomHelper.findAttr(elem, "number");
		if (ns == null)
		{
			Logger.instance().warning(module + 
				" Invalid connection element without 'number' attribute.");
			return;
		}
		ns = ns.trim();
			
		try { num = Integer.parseInt(ns); }
		catch(NumberFormatException ex)
		{
			Logger.instance().warning(module + 
				" invalid connection element in '" + cfgFileName 
				+ "' - bad or missing 'number' attribute -- skipped");
			return;
		}

		String host = DomHelper.findAttr(elem, "host");
		if (host == null)
		{
			Logger.instance().warning(module + 
				" Invalid connection element without 'host' attribute.");
			return;
		}
		host = host.trim();
		if (host.length() == 0)
		{
			Logger.instance().warning(module +
				" invalid connection element in '" + cfgFileName 
				+ "' - missing required 'host' attribute -- skipped");
			return;
		}
		DdsRecvConnectCfg cfg = new DdsRecvConnectCfg(num, host);
		
		// Content elements will contain the optional settings.
		NodeList children = node.getChildNodes();
		if (children != null)
			for(int i=0; i<children.getLength(); i++)
			{
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE)
				{
					if (child.getNodeName().equalsIgnoreCase("name"))
						cfg.name = DomHelper.getTextContent(child);
					else if (child.getNodeName().equalsIgnoreCase("port"))
						cfg.port = DomHelper.getIntegerContent(child,
							16003, module);
					else if (child.getNodeName().equalsIgnoreCase("enabled"))
						cfg.enabled = DomHelper.getBooleanContent(child,
							true, module);
					else if (child.getNodeName().equalsIgnoreCase("username"))
						cfg.username = DomHelper.getTextContent(child);
					else if (
						child.getNodeName().equalsIgnoreCase("authenticate"))
						cfg.authenticate = DomHelper.getBooleanContent(child,
							false, module);
					else if (
						child.getNodeName().equalsIgnoreCase("hasDomsatSeqNums"))
						cfg.hasDomsatSeqNums = DomHelper.getBooleanContent(child,
							false, module);
					else if (
						child.getNodeName().equalsIgnoreCase("acceptARMs"))
						cfg.acceptARMs = DomHelper.getBooleanContent(child,
							false, module);
					else if (child.getNodeName().equalsIgnoreCase("group"))
						cfg.group = DomHelper.getTextContent(child);
					else
						Logger.instance().warning(module +
							" Unrecognized parameter '" + child.getNodeName()
							+ " in connection " + num + " -- ignored.");
				}
			}

		DdsRecvConnectCfg old = getConnectCfg(num);
		if (old != null)
		{
			Logger.instance().debug1(module + 
				" Removing old connection " + num + ": " + old);
			connectCfgs.remove(old);
		}
		Logger.instance().debug1(module + 
			" Adding connection " + num + ": " + cfg);
		connectCfgs.add(cfg);
	}

	/**
	  @return the connect config with the specified number,
	  or null if no match.
	*/
	public DdsRecvConnectCfg getConnectCfg(int connectNum)
	{
		for(Iterator<DdsRecvConnectCfg> it = connectCfgs.iterator(); it.hasNext(); )
		{
			DdsRecvConnectCfg cfg = it.next();
			if (cfg.connectNum == connectNum)
				return cfg;
		}
		return null;
	}

	/**
	  @return iterator for cycling through the connect configurations.
	*/
	public Iterator<DdsRecvConnectCfg> getConnectConfigs()
	{
		return connectCfgs.iterator();
	}

	/**
	  Resets application to initial blank state.
	*/
	public void resetToDefaults()
	{
		connectCfgs.clear();
		netlistGroupAssociations.clear();
		cfgFileName = "$LRGSHOME/ddsrecv.conf";
	}

	/**
	  Debug method to print settings to output.
	  @param out usually System.out
	*/
	public void print(PrintStream out)
	{
		out.println("DDS Recv Settings (" + cfgFileName + "):");
		for(NetlistGroupAssoc nga : netlistGroupAssociations)
			out.println("NetworkList: " + nga.getNetlistName() + " group="
				+ nga.getGroupName());
		for(Iterator<DdsRecvConnectCfg> it = getConnectConfigs(); it.hasNext(); )
		{
			DdsRecvConnectCfg cfg = (DdsRecvConnectCfg)it.next();
			out.println(cfg.toString());
		}
	}

	/**
	 * Checks to see if any network lists have changed.
	 * @return true if any lists were changed, false otherwise.
	 */
	public boolean networkListsHaveChanged()
	{
		if ((decodesAll || decodesProduction)
		 && decodes.db.Database.getDb().getDbIo().getPlatformListLMT().getTime()
		 	> lastConfigRead)
			return true;
		
		for(NetlistGroupAssoc nga : netlistGroupAssociations)
		{
			decodes.db.NetworkList decNl =
				(decodes.db.NetworkList)nga.getNetworkList().getHandle();
			if (decNl != null)
			{
				try
				{
					Date lastModTime = 
						decNl.getDatabase().getDbIo().getNetworkListLMT(decNl);
					if (lastModTime == null)
					{
						Logger.instance().info(module + 
							" DECODES Network list '" + nga.getNetworkList().makeFileName()
							+ "' has been deleted.");
						return true;
					}
					long curCopyLMT = decNl.lastModifyTime.getTime();
					if (lastModTime.getTime() > curCopyLMT)
					{
						Logger.instance().info(module + 
							" DECODES Network list '" + nga.getNetworkList().makeFileName()
							+ "' has changed, forcing connection reconfig.");
						return true;
					}
				}
				catch(decodes.db.DatabaseException ex)
				{
					Logger.instance().info(module + 
						" DECODES Network list '" + nga.getNetworkList().makeFileName()
						+ "' can no longer be read.");
					return true;
				}
			}
			else
			{
				Date lastReadTime = nga.getNetworkList().getLastReadTime();
				File file = nga.getNetworkList().getFile();
				if (!file.canRead())
				{
					Logger.instance().info(module + 
						" Network list '" + nga.getNetworkList().makeFileName()
						+ "' has been removed, forcing connection reconfig.");
					return true;
				}
				if (file.lastModified() > lastReadTime.getTime())
				{
					Logger.instance().info(module + 
						" Network list file '" + nga.getNetworkList().makeFileName()
						+ "' has changed, forcing connection reconfig.");
					return true;
				}
			}
		}
		return false;
	}

	public void storeToXml(OutputStream os)
		throws IOException
	{
		XmlOutputStream xos = new XmlOutputStream(os, "ddsrecvconf");
		xos.startElement("ddsrecvconf");
		xos.writeElement("timeout", "" + timeout);
		
		for(NetlistGroupAssoc nga : netlistGroupAssociations)
			xos.writeElement("networkList", "group", nga.getGroupName(), 
				nga.getNetlistName());

		for(DdsRecvConnectCfg drcc : connectCfgs)
		{
			xos.startElement("connection",
				"number", "" + drcc.connectNum,
				"host", drcc.host);
			xos.writeElement("name", drcc.name);
			xos.writeElement("port", ""+drcc.port);
			xos.writeElement("enabled", ""+drcc.enabled);
			xos.writeElement("username", drcc.username);
			xos.writeElement("authenticate", "" + drcc.authenticate);
			xos.writeElement("hasDomsatSeqNums", "" + drcc.hasDomsatSeqNums);
			xos.writeElement("acceptARMs", "" + drcc.acceptARMs);
			xos.writeElement("group", "" + drcc.group);
			xos.endElement("connection");
		}
		
		xos.endElement("ddsrecvconf");
	}

	/**
	 * @return the isReloaded
	 */
	public boolean isReloaded() {
		return isReloaded;
	}

	/**
	 * @param isReloaded the isReloaded to set
	 */
	public void setReloaded(boolean isReloaded) {
		this.isReloaded = isReloaded;
	}

	public ArrayList<NetlistGroupAssoc> getNetlistGroupAssociations()
	{
		return netlistGroupAssociations;
	}

	public void addNetlistAssoc(String netlistName, String group)
	{
		for(NetlistGroupAssoc nga : netlistGroupAssociations)
			if (netlistName.equals(nga.getNetlistName())
			 && group.equals(nga.getGroupName()))
				return;
		netlistGroupAssociations.add(
			new NetlistGroupAssoc(netlistName, group));
	}
}
