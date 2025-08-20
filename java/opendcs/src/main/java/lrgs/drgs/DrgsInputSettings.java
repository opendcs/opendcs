/*
*  $Id$
*/
package lrgs.drgs;

import java.util.Vector;
import java.util.Iterator;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import ilex.util.Logger;
import ilex.util.ByteUtil;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;
import lrgs.common.BadConfigException;

/**
Singleton class holding settings for the DrgsInput application.
*/
public class DrgsInputSettings
{
	private int debugLevel;
	public Vector<DrgsConnectCfg> connections;
	private static DrgsInputSettings _instance;
	private String cfgFileName;
	private static String module = "DrgsInputSettings";

	/** True if we are to validate incoming DCP messages from the DRGS. */
	public boolean enableValidate;

	/** URL from which to download the PDT. */
	public String pdtUrl;

	/** URL from which to download the CDT. */
	public String cdtUrl;

	/** Flag indicating we should re-read the PDT from the specified URL. */
	public boolean readPdtFlag;
	
	/** Flag indicating we should save pollingPeriod (default=false), will
	 * be set to true for the Network DCP configurations.
	 */
	public boolean savePollingPeriod = false;
	
	/** Timeout period for this connection, default = 20 sec. */
	public long timeoutMsec = 20000L;

	/** Default constructor. */
	public DrgsInputSettings()
	{
		debugLevel = 0;
		connections = new Vector<DrgsConnectCfg>();
		enableValidate = false;
		pdtUrl = "https://dcs1.noaa.gov/pdts_compressed.txt";
	}

	/**
	 * Returns the singleton instance for use with the DRGS interface.
	 * Note that the Network DCPs will use a separate instance. 
	 * @return singleton instance. 
	 * */
	public static DrgsInputSettings instance()
	{
		if (_instance == null)
			_instance = new DrgsInputSettings();
		return _instance;
	}

	/**
	  Pass the name of the XML file containing the DRGS Input Process
	  configuration. Sets internal variables according to that configuration.
	  @param filename the XML configuration file name
	*/
	public void setFromFile(String filename)
		throws BadConfigException
	{
		resetToDefaults();

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

		Node drgsElement = doc.getDocumentElement();
		if (!drgsElement.getNodeName().equalsIgnoreCase("drgsconf"))
		{
			String s = module 
				+ ": Wrong type of configuration file -- Cannot initialize. "
				+ "Root element is not 'drgsconf'.";
			Logger.instance().log(Logger.E_WARNING, s);
			throw new BadConfigException(s);
		}

		NodeList children = drgsElement.getChildNodes();
		if (children != null)
		{
			for(int i=0; i<children.getLength(); i++)
			{
				Node node = children.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE)
				{
					if (node.getNodeName().equalsIgnoreCase("debug"))
						setDebugLevel(
							DomHelper.getIntegerContent(node,0,module));
					else if (node.getNodeName().equalsIgnoreCase("connection"))
					{
						addConnectionElement(node);
					}
					else if (node.getNodeName().equalsIgnoreCase("validate"))
					{
						pdtUrl = 
							"https://dcs1.noaa.gov/pdts_compressed.txt";
						cdtUrl =
							"https://dcs1.noaa.gov/chans_by_baud.txt";

						Element elem = (Element)node;
						String s = elem.getAttribute("enable").trim();
						boolean enable = true;
						if (s != null)
							enable = TextUtil.str2boolean(s);

						s = elem.getAttribute("pdturl").trim();
						if (s != null && s.length() > 0)
							pdtUrl = s;

						s = elem.getAttribute("cdturl").trim();
						if (s != null && s.length() > 0)
							cdtUrl = s;

						if (!enableValidate && enable)
							readPdtFlag = true;
						else if (enableValidate && !enable)
							readPdtFlag = false;

						enableValidate = enable;
						Logger.instance().info("Validation is "
							+ (enableValidate ? "ENABLED" : "NOT-ENABLED")
							+ ", pdturl='" + pdtUrl + "'");
					}
				}
			}
		}
	}

	private void addConnectionElement(Node node)
	{
		// Get number and host attributes.
		Element elem = (Element)node;
		int num = -1;
		String ns = elem.getAttribute("number").trim();
		try { num = Integer.parseInt(ns); }
		catch(NumberFormatException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"DrgsInputSettings invalid connection element in '"
				+ cfgFileName 
				+ "' - bad or missing 'number' attribute -- skipped");
			return;
		}
		String host = elem.getAttribute("host").trim();
		if (host.length() == 0)
		{
			Logger.instance().log(Logger.E_WARNING,
				"DrgsInputSettings invalid connection element in '"
				+ cfgFileName 
				+ "' - missing required 'host' attribute -- skipped");
			return;
		}
		DrgsConnectCfg cfg = new DrgsConnectCfg(num, host);
		
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
					else if (child.getNodeName().equalsIgnoreCase("msgport"))
						cfg.msgPort = DomHelper.getIntegerContent(child,
							17010, module);
					else if (child.getNodeName().equalsIgnoreCase("enabled"))
						cfg.msgEnabled = DomHelper.getBooleanContent(child,
							true, module);
					else if (child.getNodeName().equalsIgnoreCase("evtport"))
						cfg.evtPort = DomHelper.getIntegerContent(child,
							17011, module);
					else if (child.getNodeName().equalsIgnoreCase("evtenabled"))
						cfg.evtEnabled = DomHelper.getBooleanContent(child,
							false, module);
					else if(child.getNodeName().equalsIgnoreCase("startpattern"))
					{
						String s = DomHelper.getTextContent(child);
						cfg.startPattern = ByteUtil.fromHexString(s);
					}
					else if (child.getNodeName().equalsIgnoreCase("cfgfile"))
						cfg.cfgFile = DomHelper.getTextContent(child);
					else if (child.getNodeName().equalsIgnoreCase("sourcecode"))
					{
						String sc = DomHelper.getTextContent(child);
						if (sc != null && sc.length() >= 2)
						{
							cfg.drgsSourceCode[0] = (byte)sc.charAt(0);
							cfg.drgsSourceCode[1] = (byte)sc.charAt(1);
						}
					}
					else if (child.getNodeName().equalsIgnoreCase(
						"pollingPeriod"))
						cfg.pollingPeriod = DomHelper.getIntegerContent(child,
							0, module);
					else						
						Logger.instance().warning("Unrecognized element '"
							+ child.getNodeName() + "' ignored DRGS Config.");
				}
			}

		DrgsConnectCfg old = getConnectCfg(num);
		if (old != null)
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Removing old connection " + num + ": " + old);
			connections.remove(old);
		}
		Logger.instance().log(Logger.E_DEBUG1,
			"Adding drgs config " + num + ": " + cfg);
		connections.add(cfg);
	}

	/** @return debug level */
	public int getDebugLevel() { return debugLevel; }

	/** Sets the debug level.
	  @param dbl should be 0 (no debug), 1, 2, or 3 (most verbose)
	*/
	public void setDebugLevel(int dbl) 
	{
//		debugLevel = dbl; 
//		Logger.instance().setMinLogPriority(
//				dbl <= 0 ? Logger.E_INFORMATION :
//				dbl == 1 ? Logger.E_DEBUG1 :
//				dbl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);
	}

	/**
	  @return the connect config with the specified number,
	  or null if no match.
	*/
	public DrgsConnectCfg getConnectCfg(int connectNum)
	{
		for(Iterator<DrgsConnectCfg> it = connections.iterator(); it.hasNext(); )
		{
			DrgsConnectCfg cfg = it.next();
			if (cfg.connectNum == connectNum)
				return cfg;
		}
		return null;
	}

	/**
	  @return iterator for cycling through the connect configurations.
	*/
	public Iterator<DrgsConnectCfg> getConnectConfigs()
	{
		return connections.iterator();
	}

	/**
	  Resets application to initial blank state.
	*/
	public void resetToDefaults()
	{
		connections.clear();
		cfgFileName = "none";
	}

	/**
	  Debug method to print settings to output.
	  @param out usually System.out
	*/
	public void print(PrintStream out)
	{
		out.println("DRGS Settings (" + cfgFileName + "):");
		out.println("debugLevel=" + debugLevel);
		for(Iterator it = getConnectConfigs(); it.hasNext(); )
		{
			DrgsConnectCfg cfg = (DrgsConnectCfg)it.next();
			out.println(cfg.toString());
		}
	}

	public void storeToXml(OutputStream os)
		throws IOException
	{
		XmlOutputStream xos = new XmlOutputStream(os, "drgsconf");
		xos.startElement("drgsconf");
		
		if (!savePollingPeriod)
		{
			int n = 1;
			if (pdtUrl != null) n++;
			if (cdtUrl != null) n++;
			StringPair[] atts = new StringPair[n];
			atts[0] = new StringPair("enable", "" + enableValidate);
			n=1;
			if (pdtUrl != null)
				atts[n++] = new StringPair("pdturl", pdtUrl);
			if (cdtUrl != null)
				atts[n++] = new StringPair("cdturl", cdtUrl);
			xos.writeElement("validate", atts, null);
		}
		
		for(DrgsConnectCfg dcc : connections)
		{
			xos.startElement("connection",
				"number", "" + dcc.connectNum,
				"host", dcc.host);
			xos.writeElement("name", dcc.name);
			xos.writeElement("enabled", ""+dcc.msgEnabled);
			xos.writeElement("msgport", ""+dcc.msgPort);
			xos.writeElement("evtport", ""+dcc.evtPort);
			xos.writeElement("evtenabled", ""+dcc.evtEnabled);
			xos.writeElement("startpattern", 
				ByteUtil.toHexString(dcc.startPattern));
			if (dcc.drgsSourceCode != null)
				xos.writeElement("sourcecode", new String(dcc.drgsSourceCode));
			if (dcc.cfgFile != null)
				xos.writeElement("cfgfile", dcc.cfgFile);
			if (savePollingPeriod)
				xos.writeElement("pollingPeriod", ""+dcc.pollingPeriod);
			xos.endElement("connection");
		}

		xos.endElement("drgsconf");
	}

	public void setModule(String module) { this.module = module; }
}

