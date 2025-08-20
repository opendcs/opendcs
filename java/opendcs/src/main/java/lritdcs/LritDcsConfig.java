/*
* $Id$
* 
* $Log$
* Revision 1.8  2012/12/12 18:44:24  mmaloney
* Fix UI Thread problem on config requests.
*
* Revision 1.7  2012/12/12 16:01:31  mmaloney
* Several updates for 5.2
*
*/
package lritdcs;

import java.util.*;
import java.io.*;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

public class LritDcsConfig
	extends Observable
{
	/// Global instance returned by instance() method.
	private static LritDcsConfig _instance = null;

	/// Unparsed data read from configuration file. Access with getProperty()
	private File configFile = null;
	private String lritDcsHome;
	private long lastLoadTime;

	//=================================================================
	// The parsed configuration data and the initial default values:
	//=================================================================
	public String dds1HostName;
	public int dds1Port;
	public String dds1UserName;

	public String dds2HostName;
	public int dds2Port;
	public String dds2UserName;

	public String dds3HostName;
	public int dds3Port;
	public String dds3UserName;

	/// Wait this many secs for response, then declare timeout.
	public int ddsTimeOut;

	/// Re-attempt to connect to primary server after this many seconds.
	public int ddsRetryPeriod;

	public int maxBytesLow;
	public int maxMsgsLow;
	public int maxSecondsLow;

	public int maxBytesMedium;
	public int maxMsgsMedium;
	public int maxSecondsMedium;

	public int maxBytesHigh;
	public int maxMsgsHigh;
	public int maxSecondsHigh;

	public int scrubHours;
	public String webBrowser;	
	
	public String dom2AHostName;
	public String dom2ADirLow;
	public String dom2ADirMedium;
	public String dom2ADirHigh;
	public String dom2AUser;

	public String dom2BHostName;
	public String dom2BDirLow;
	public String dom2BDirMedium;
	public String dom2BDirHigh;
	public String dom2BUser;


	public String dom2CHostName;
	public String dom2CDirLow;
	public String dom2CDirMedium;
	public String dom2CDirHigh;
	public String dom2CUser;
	
	public int lqmPort;
	public String lqmIPAddress;
	public int lritUIPort;

	public String UIIPAddresses;
	public int maxManualRetrans;
	public int maxAutoRetrans;
	public int maxTotalFiles;
	public boolean enableLqm;
	public int lqmPendingTimeout;
	public boolean enableLritSchedule;

	public String fileSenderHost;
	public String fileSenderState;
	
//	public boolean ptpEnabled = false;
//	public String ptpDir = "ptp";

	public static boolean exitOnLoadError = true;

	/// Returns the global instance. Reads configuration on first call.
	public static LritDcsConfig instance()
	{
		if (_instance == null)
		{
			_instance = new LritDcsConfig();
			_instance.load();
		}
		return _instance;
	}

	/**
	  Called by the user interface GUI at start-up to construct the 
	  instance but without loading it from a file. Rather, the GUI
	  populates the config from messages it receives over the UI socket.
	*/
	public static void makeInstanceNoLoad()
	{
		if (_instance == null)
			_instance = new LritDcsConfig();
	}

	/// Private constructor, access only via instance()
	private LritDcsConfig()
	{
		lastLoadTime = -1L;
		lritDcsHome = null;;
		restoreDefaults();
	}

	public void restoreDefaults()
	{
		dds1HostName = "cdadata.wcda.noaa.gov";
		dds1Port = 16003;
		dds1UserName = "lrit";

		dds2HostName = "cdabackup.wcda.noaa.gov";
		dds2Port = 16003;
		dds2UserName = "lrit";

		dds3HostName = "lrgseddn1.cr.usgs.gov";
		dds3Port = 16003;
		dds3UserName = "lrit";

		ddsTimeOut = 120;
		ddsRetryPeriod = 600;
		maxBytesLow = 1000000;
		maxMsgsLow = 1000;
		maxSecondsLow = 120;
		maxBytesMedium = 1000000;
		maxMsgsMedium = 1000;
		maxSecondsMedium = 60;
		maxBytesHigh = 16000;
		maxMsgsHigh = 1;
		maxSecondsHigh = 60;
		dom2AHostName = "couch.nesdis.noaa.gov";
		dom2ADirLow = "Low";
		dom2ADirMedium = "Medium";
		dom2ADirHigh = "High";
		dom2AUser = "dcsadmin";
		
		dom2BHostName = "sofa.nesdis.noaa.gov";
		dom2BDirLow = "Low";
		dom2BDirMedium = "Medium";
		dom2BDirHigh = "High";
		dom2BUser = "dcsadmin";
		
		dom2CHostName = "";
		dom2CDirLow = "Low";
		dom2CDirMedium = "Medium";
		dom2CDirHigh = "High";
		dom2CUser = "dcsadmin";
		
		scrubHours = 48;
		webBrowser = "mozilla";
		lqmPort = 17004;
		lqmIPAddress = "";
		lritUIPort = 17005;
		UIIPAddresses = "";
		maxManualRetrans = 40;
		maxAutoRetrans = 40;
		maxTotalFiles = -1;
		enableLqm = true;
		lqmPendingTimeout = 300;
//		rsaPrivateKey = EnvExpander.expand("$HOME/.ssh/id_rsa");
		fileSenderHost="localhost";
		fileSenderState="dormant";
		
//		ptpEnabled = false;
//		ptpDir = "ptp";

		
	}

	public void findConfigFile()
	{
		if (configFile != null)
			return;
		lritDcsHome = System.getProperty("LRITDCS_HOME");
		//lritDcsHome = "C:/LRITDCS";
		//lritDcsHome = System.getenv("LRITDCS_HOME");		
		System.setProperty("user.dir", lritDcsHome);
		Logger.instance().info("CWD set to '" + System.getProperty("user.dir") + "'");
		if (lritDcsHome == null)
		{
			System.err.println(
	"Cannot load configuration. LRITDCS_HOME must be defined in java command.");
			System.exit(1);
		}
		configFile = new File(lritDcsHome + File.separator + "lritdcs.conf");
	}

	/**
	  Reads the configuration from $LRITDCS_HOME/dcstool.conf.
	  Does NOT clear the configuration data. Make an explicit call to clear()
	  before calling this method.
	*/
	public void load()
	{
		lastLoadTime = System.currentTimeMillis();
		findConfigFile();
		Logger.instance().info("Loading configuration from '" + configFile
			+ "'");
		try
		{
			FileInputStream fis = new FileInputStream(configFile);
			Properties configData = new Properties();
			configData.load(fis);
			fis.close();
			PropertiesUtil.loadFromProps(this, configData);
			
//			LritDcsMain theMain = LritDcsMain.instance();
//			if (theMain != null)
//			{
//				LritDcsStatus status = theMain.getStatus();
//				if (status != null)
//				{
//					status.ptpDir = this.ptpDir;
//					status.ptpEnabled = this.ptpEnabled;
//				}
//			}
		}
		catch(IOException ex)
		{
			if (exitOnLoadError)
			{
				Logger.instance().fatal("LRIT:" + Constants.EVT_BAD_CONFIG
					+"- Cannot read LRIT DCS Config File '"+configFile.getPath()
					+ "' (Check LRITDCS_HOME in environment): " + ex);
				System.exit(1);
			}
		}
	}


	public void save()
	{
		findConfigFile();
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(configFile);
			save(fos);
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Cannot save LRIT DCS Config File '"+configFile.getPath()
				+ "' (Check LRITDCS_HOME in environment): " + ex);
		}
		finally
		{
			if (fos != null)
			{
				try { fos.close(); }
				catch(IOException ex) {}
			}
		}
	}

	public  void saveConfig(PrintWriter pw,Properties propConfig)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		StringBuffer sbf = new StringBuffer(propConfig.toString());
		sbf.deleteCharAt(0);
		sbf.deleteCharAt(sbf.length()-1);
		Properties props = PropertiesUtil.string2props(sbf.toString());
		
		try { 
			props.store(baos, "LRIT File-Generator Configuration");
	
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Logger.instance().warning("Cannot write to string!!!");
			return;
		}
		pw.println(baos.toString());
	}
	
	public void saveState(PrintWriter pw,String lritState)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
					
			Properties props = new Properties();
			PropertiesUtil.storeInProps(this, props, null);
			props.put("fileSenderState",lritState );
			props.store(baos, "LRIT File-Generator Configuration");		
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot write to string!!!");
			return;
		}
		pw.println(baos.toString());
	}
	
	public void save(OutputStream os)
		throws IOException
	{
		Properties props = new Properties();		
		PropertiesUtil.storeInProps(this, props, null);
		props.store(os, "LRIT File-Generator Configuration");		
	}

	public void save(PrintWriter pw)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try 
		{
			save(baos);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot write to string!!!");
			return;
		}
		
		StringBuilder sb = new StringBuilder(baos.toString());
		pw.println(sb.toString());
	}

	//=================================================================
	// Low level getters & setters:
	//=================================================================

	public String getDds1HostName()
	{
		return dds1HostName;
	}

	public void setDds1HostName(String hn)
	{
		dds1HostName = hn;
	}

	public int getDds1Port()
	{
		return dds1Port;
	}

	public void setDds1Port(int p)
	{
		dds1Port = p;
	}

	public String getDds1UserName()
	{
		return dds1UserName;
	}

	public void setDds1UserName(String hn)
	{
		dds1UserName = hn;
	}

	public String getDds2HostName()
	{
		return dds2HostName;
	}

	public void setDds2HostName(String hn)
	{
		dds2HostName = hn;
	}

	public int getDds2Port()
	{
		return dds2Port;
	}

	public void setDds2Port(int p)
	{
		dds2Port = p;
	}

	public String getDds2UserName()
	{
		return dds2UserName;
	}

	public void setDds2UserName(String hn)
	{
		dds2UserName = hn;
	}

	public String getDds3HostName()
	{
		return dds3HostName;
	}

	public void setDds3HostName(String hn)
	{
		dds3HostName = hn;
	}

	public int getDds3Port()
	{
		return dds3Port;
	}

	public void setDds3Port(int p)
	{
		dds3Port = p;
	}

	public String getDds3UserName()
	{
		return dds3UserName;
	}

	public void setDds3UserName(String hn)
	{
		dds3UserName = hn;
	}

	public int getDdsTimeOut()
	{
		return ddsTimeOut;
	}

	public void setDdsTimeOut(int hn)
	{
		ddsTimeOut = hn;
	}

	public int getDdsRetryPeriod()
	{
		return ddsRetryPeriod;
	}

	public void setDdsRetryPeriod(int hn)
	{
		ddsRetryPeriod = hn;
	}

	public int getMaxBytesLow()
	{
		return maxBytesLow;
	}

	public void setMaxBytesLow(int v)
	{
		maxBytesLow = v;
	}

	public int getMaxMsgsLow()
	{
		return maxMsgsLow;
	}

	public void setMaxMsgsLow(int v)
	{
		maxMsgsLow = v;
	}

	public int getMaxSecondsLow()
	{
		return maxSecondsLow;
	}

	public void setMaxSecondsLow(int v)
	{
		maxSecondsLow = v;
	}

	public int getMaxBytesMedium()
	{
		return maxBytesMedium;
	}

	public void setMaxBytesMedium(int v)
	{
		maxBytesMedium = v;
	}

	public int getMaxMsgsMedium()
	{
		return maxMsgsMedium;
	}

	public void setMaxMsgsMedium(int v)
	{
		maxMsgsMedium = v;
	}

	public int getMaxSecondsMedium()
	{
		return maxSecondsMedium;
	}

	public void setMaxSecondsMedium(int v)
	{
		maxSecondsMedium = v;
	}

	public int getMaxBytesHigh()
	{
		return maxBytesHigh;
	}

	public void setMaxBytesHigh(int hn)
	{
		maxBytesHigh = hn;
	}

	public int getMaxMsgsHigh()
	{
		return maxMsgsHigh;
	}

	public void setMaxMsgsHigh(int v)
	{
		maxMsgsHigh = v;
	}

	public int getMaxSecondsHigh()
	{
		return maxSecondsHigh;
	}

	public void setMaxSecondsHigh(int v)
	{
		maxSecondsHigh = v;
	}

	

	public int getScrubHours()
	{
		return scrubHours;
	}

	public void setScrubHours(int v)
	{
		scrubHours = v;
	}

	public String getWebBrowser()
	{
		return webBrowser;
	}

	public void setWebBrowser(String hn)
	{
		webBrowser = hn;
	}

	public int getLqmPort()
	{
		return lqmPort;
	}

	public void setLqmPort(int v)
	{
		lqmPort = v;
	}

	public String getLqmIPAddress()
	{
		return lqmIPAddress;
	}

	public void setLqmIPAddress(String v)
	{
		lqmIPAddress = v;
	}

	public int getLritUIPort()
	{
		return lritUIPort;
	}

	public void setLritUIPort(int v)
	{
		lritUIPort = v;
	}

	public String getUIIPAddresses()
	{
		return UIIPAddresses;
	}

	public void setUIIPAddresses(String v)
	{
		UIIPAddresses = v;
	}

	public int getMaxManualRetrans()
	{
		return maxManualRetrans;
	}

	public void setMaxManualRetrans(int v)
	{
		maxManualRetrans = v;
	}

	public int getMaxAutoRetrans()
	{
		return maxAutoRetrans;
	}

	public void setMaxAutoRetrans(int v)
	{
		maxAutoRetrans = v;
	}

	public int getMaxTotalFiles()
	{
		return maxTotalFiles;
	}

	public void setMaxTotalFiles(int v)
	{
		maxTotalFiles = v;
	}

	public boolean getEnableLqm()
	{
		return enableLqm;
	}

	public void setEnableLqm(boolean v)
	{
		enableLqm = v;
	}

	public int getLqmPendingTimeout()
	{
		return lqmPendingTimeout;
	}

	public void setLqmPendingTimeout(int v)
	{
		lqmPendingTimeout = v;
	}

	public long getLastLoadTime()
	{
		return lastLoadTime;
	}

	/**
	 * @return the dom2AHostName
	 */
	public String getDom2AHostName() {
		return dom2AHostName;
	}

	/**
	 * @param dom2AHostName the dom2AHostName to set
	 */
	public void setDom2AHostName(String dom2AHostName) {
		this.dom2AHostName = dom2AHostName;
	}

	/**
	 * @return the dom2ADirLow
	 */
	public String getDom2ADirLow() {
		return dom2ADirLow;
	}

	/**
	 * @param dom2ADirLow the dom2ADirLow to set
	 */
	public void setDom2ADirLow(String dom2ADirLow) {
		this.dom2ADirLow = dom2ADirLow;
	}

	/**
	 * @return the dom2ADirMedium
	 */
	public String getDom2ADirMedium() {
		return dom2ADirMedium;
	}

	/**
	 * @param dom2ADirMedium the dom2ADirMedium to set
	 */
	public void setDom2ADirMedium(String dom2ADirMedium) {
		this.dom2ADirMedium = dom2ADirMedium;
	}

	/**
	 * @return the dom2ADirHigh
	 */
	public String getDom2ADirHigh() {
		return dom2ADirHigh;
	}

	/**
	 * @param dom2ADirHigh the dom2ADirHigh to set
	 */
	public void setDom2ADirHigh(String dom2ADirHigh) {
		this.dom2ADirHigh = dom2ADirHigh;
	}

	/**
	 * @return the dom2AUser
	 */
	public String getDom2AUser() {
		return dom2AUser;
	}

	/**
	 * @param dom2AUser the dom2AUser to set
	 */
	public void setDom2AUser(String dom2AUser) {
		this.dom2AUser = dom2AUser;
	}

	/**
	 * @return the dom2BHostName
	 */
	public String getDom2BHostName() {
		return dom2BHostName;
	}

	/**
	 * @param dom2BHostName the dom2BHostName to set
	 */
	public void setDom2BHostName(String dom2BHostName) {
		this.dom2BHostName = dom2BHostName;
	}

	/**
	 * @return the dom2BDirLow
	 */
	public String getDom2BDirLow() {
		return dom2BDirLow;
	}

	/**
	 * @param dom2BDirLow the dom2BDirLow to set
	 */
	public void setDom2BDirLow(String dom2BDirLow) {
		this.dom2BDirLow = dom2BDirLow;
	}

	/**
	 * @return the dom2BDirMedium
	 */
	public String getDom2BDirMedium() {
		return dom2BDirMedium;
	}

	/**
	 * @param dom2BDirMedium the dom2BDirMedium to set
	 */
	public void setDom2BDirMedium(String dom2BDirMedium) {
		this.dom2BDirMedium = dom2BDirMedium;
	}

	/**
	 * @return the dom2BDirHigh
	 */
	public String getDom2BDirHigh() {
		return dom2BDirHigh;
	}

	/**
	 * @param dom2BDirHigh the dom2BDirHigh to set
	 */
	public void setDom2BDirHigh(String dom2BDirHigh) {
		this.dom2BDirHigh = dom2BDirHigh;
	}

	/**
	 * @return the dom2BUser
	 */
	public String getDom2BUser() {
		return dom2BUser;
	}

	/**
	 * @param dom2BUser the dom2BUser to set
	 */
	public void setDom2BUser(String dom2BUser) {
		this.dom2BUser = dom2BUser;
	}

	/**
	 * @return the dom2CHostName
	 */
	public String getDom2CHostName() {
		return dom2CHostName;
	}

	/**
	 * @param dom2CHostName the dom2CHostName to set
	 */
	public void setDom2CHostName(String dom2CHostName) {
		this.dom2CHostName = dom2CHostName;
	}

	/**
	 * @return the dom2CDirLow
	 */
	public String getDom2CDirLow() {
		return dom2CDirLow;
	}

	/**
	 * @param dom2CDirLow the dom2CDirLow to set
	 */
	public void setDom2CDirLow(String dom2CDirLow) {
		this.dom2CDirLow = dom2CDirLow;
	}

	/**
	 * @return the dom2CDirMedium
	 */
	public String getDom2CDirMedium() {
		return dom2CDirMedium;
	}

	/**
	 * @param dom2CDirMedium the dom2CDirMedium to set
	 */
	public void setDom2CDirMedium(String dom2CDirMedium) {
		this.dom2CDirMedium = dom2CDirMedium;
	}

	/**
	 * @return the dom2CDirHigh
	 */
	public String getDom2CDirHigh() {
		return dom2CDirHigh;
	}

	/**
	 * @param dom2CDirHigh the dom2CDirHigh to set
	 */
	public void setDom2CDirHigh(String dom2CDirHigh) {
		this.dom2CDirHigh = dom2CDirHigh;
	}

	/**
	 * @return the dom2CUser
	 */
	public String getDom2CUser() {
		return dom2CUser;
	}

	/**
	 * @param dom2CUser the dom2CUser to set
	 */
	public void setDom2CUser(String dom2CUser) {
		this.dom2CUser = dom2CUser;
	}

	/**
	  Checks config file for change and reloads if necessary.
	  @returns true if change was loaded, false if not.
	*/
	public boolean checkConfigFile()
	{
		long lmt = configFile.lastModified();
		if (lmt > lastLoadTime)
		{
			//System.out.println("Configuration has changed and is being reloaded.");
			Logger.instance().log(Logger.E_INFORMATION,
				"Configuration has changed and is being reloaded.");
			load();
			return true;
		}
		return false;
	}

	public File getConfigFile()
	{
		return configFile;
	}

	/// Returns DCS Toolkit Home directory.
	public String getLritDcsHome() { return lritDcsHome; }

	/**
	 * @return the fileSenderHost
	 */
	public String getFileSenderHost() {
		return fileSenderHost;
	}

	/**
	 * @param fileSenderHost the fileSenderHost to set
	 */
	public void setFileSenderHost(String fileSenderHost) {
		this.fileSenderHost = fileSenderHost;
	}

	/**
	 * @return the fileSenderState
	 */
	public String getFileSenderState() {
		return fileSenderState;
	}

	/**
	 * @param fileSenderState the fileSenderState to set
	 */
	public void setFileSenderState(String fileSenderState) {
		this.fileSenderState = fileSenderState;
	}

	public String checkNull(String s)
	{
		if (s.equals("null"))
			return null;
		return s;
	}

//	public boolean isPtpEnabled()
//	{
//		return ptpEnabled;
//	}
//
//	public void setPtpEnabled(boolean ptpEnabled)
//	{
//		this.ptpEnabled = ptpEnabled;
//	}
//
//	public String getPtpDir()
//	{
//		return ptpDir;
//	}
//
//	public void setPtpDir(String ptpDir)
//	{
//		this.ptpDir = ptpDir;
//	}
}
