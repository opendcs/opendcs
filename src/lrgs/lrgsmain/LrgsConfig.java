/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.lrgsmain;


import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

import lrgs.ldds.PasswordChecker;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

/**
This class holds all of the configuration variables for the archive module.
*/
public class LrgsConfig
	implements PropertiesOwner
{
	/** True if noaaport interface is enabled. */
	public boolean noaaportEnabled;
	
	/** Port number to either listen on or connect to */
	public int noaaportPort;
	
	/** Either "unisys", "marta", or "PDI" */
	public String noaaportReceiverType = "marta";
	
	/** Needed if receiver is "unisys" */
	public String noaaportHostname = "";
	
	/** If supplied, capture noaaport data to this file with date/time extension */
	public String noaaportCaptureFile = "";
	
	/** Private instance reference */
	private static LrgsConfig _instance = null;

	/** File object for config file. */
	private File cfgFile;

	/** Last time configuration was loaded from file. */
	private long lastLoadTime;

	// Configuration variables settable from properties are public

	/** Directory containing all archive files */
	public String archiveDir;

	/** Number of day files to save in archive */
	public int numDayFiles;

	/** True if LRIT Input Interface is to be enabled. */
	public boolean enableLritRecv;

	/** for dams-nt LRIT connection, this is the host or IP address to connect to */
	public String lritHostName = null;
	
	/** For dams-nt LRIT connection, this is the port to connect to */
	public int lritPort = 17010;

	/** Hex string representing the DRGS Start Pattern */
	public String lritDamsNtStartPattern = "534D0D0A";
	
	/** One of the HEADER_TYPE constants in LritFileMonitor */
	public char lritHeaderType;
	
	/** The 2-char msg source code to use for messages received from LRIT DAMS-NT */
	public String lritSrcCode = "LR";

	/** True if DDS Input Interface is to be enabled. */
	public boolean enableDdsRecv;

	/** Name of DDS Receive Config File. */
	public String ddsRecvConfig;

	/** True if DRGS Input Interface is to be enabled. */
	public boolean enableDrgsRecv;

	/** Name of DRGS Receive Config File. */
	public String drgsRecvConfig;

	/** Port number for DDS Server to listen on. */
	public int ddsListenPort;

	/** If multi-NIC server and DDS only listens on one NIC, set bind addr. */
	public String ddsBindAddr;

	/** Maximum number of DDS clients to accept at any one time (0=no limit). */
	public int ddsMaxClients;

	/** True if DDS requires password authentication for message retreival. */
	public boolean ddsRequireAuth;

	/** True to enable administrative functions via DDS. */
	public boolean ddsAllowAdmin;

	/** DDS Usage Log Filename. */
	public String ddsUsageLog;

	/** DDS Network List Directory for mapping global names. */
	public String ddsNetlistDir;

	/** DDS Directory containing user sandbox directories. */
	public String ddsUserRootDir;

	/** DDS Directory containing user sandbox directories. */
	public String ddsUserRootDirLocal;

	/** Maximum number of downlink interfaces. */
	public int maxDownlinks;

	/** Assert Timeout if this many seconds goes by with no messages. */
	public int timeoutSeconds;

	/** File in which to place periodic HTML status snapshot. */
	public String htmlStatusFile;

	/** Number of seconds between HTML status snapshots. */
	public int htmlStatusSeconds;

	/** Merge preference 1: Highest priority. */
	public String mergePref1;
	
	/** Merge preference 2. */
	public String mergePref2;
	
	/** Merge preference 3. */
	public String mergePref3;
	
	/** Merge preference 4: Lowest priority. */
	public String mergePref4;
	
	/** True if DOMSAT Input Interface is to be even loaded. */
	public boolean loadDomsat;

	/** True if DOMSAT Input Interface is to be enabled. */
	public boolean enableDomsatRecv;

	/** Number of seconds after which DOMSAT declares a timeout. */
	public int domsatTimeout;

	/** Interface class to use for DOMSAT hardware. */
	public String domsatClass;

	/** NESDIS-ONLY: Enables output of DQM messages to COM1 serial port. */
	public boolean enableDapsDqm;

	/** NESDIS-ONLY: serial port for sending messages to DAPS. */
	public String dqmSerialPort;

	/** For bad DRGS's, option to ignore No-EOT-Termination condition. */
	public boolean ignoreDrgsNoEotTermination;

	/** Maximum size of a single lrgslog file */
	public int maxLogSize;

	/** Number of old log files to keep */
	public int numOldLogs;

	/** URL for accessing the LRGS database. */
	public String dbUrl;

	/** SimpleDateString format for writing to database. */
	public String sqlWriteDateFormat;

	/** SimpleDateString format for reading database. */
	public String sqlReadDateFormat;

	/** TimeZone for dates read from or written to the database */
	public String sqlTimeZone;

	/** Full package/class name for JDBC Driver */
	public String JdbcDriverClass;
	
	/** Name of class for key generation. */
	public String keyGeneratorClass;

	/** True if ddsrecv is to to recover discrete outages, else rt stream */
	public boolean recoverOutages;
	
	/** True if the network DCPs interface is enabled. */
	public boolean networkDcpEnable = false;
	
	/** URL for downloading PDT - defaults to NESDIS DCS location */
	public String pdtUrl = "https://dcs1.noaa.gov/pdts_compressed.txt";
	
	/** True if this LRGS should do PDT validation for interfaces that don't 
	 * already provide it.
	 */
	public boolean doPdtValidation = false;
	
	/** URL for downloading channel table from NOAA */
	public String channelMapUrl = 
		"https://dcs1.noaa.gov/chans_by_baud.txt";
	
	/** Tells merge filter to prefer good messages from designated input */
	public boolean archivePreferredGood = false;
	
	/** If true, then only locally-assigned DDS accounts can do admin. */
	public boolean localAdminOnly = false;
	
	/** True to enable Iridium interface */
	public boolean iridiumEnabled = false;
	
	/** Port number for incoming Iridium SBD messages */
	public int iridiumPort = 10800;
	
	/** If specified, iridium raw-data will be captured here. */
	public String iridiumCaptureFile = null;
	
	/** Domsat Protocol Converter Host Name */
	public String dpcHost = null;
	
	/** Domsat Protocol Converter Port Number */
	public int dpcPort = 9000;
	
	/** Accept Abnormal Response Messages from DOMSAT (default=true) */
	public boolean acceptDomsatARMs = true; 
	
	/** Store XMIT Records (i.e. the DCP Monitor Function) */
	public boolean storeXmitRecords = false;
	
	/** Meteosat LRIT can be VCS (default) or IBL*/
	public boolean iblLrit = false;
	
	/** Number of seconds after which to assert an LRIT timeout. */
	public int lritTimeout = 120;
	
	/** Enables ingest of EDL files via hot directory */
	public boolean edlIngestEnable = false;
	
	/** Specifies directory for incoming EDL files to be ingested into LRGS archive */
	public String edlIngestDirectory = "$LRGSHOME/edl-incoming";
	
	/** If true, then subdirectories of edlIngestDirectory are recursively searched */
	public boolean edlIngestRecursive = false;
	
	/** If set, then only files with a specific suffix will be processed */
	public String edlFilenameSuffix = null;
	
	/** If set, then edl files will be moved here after processing */
	public String edlDoneDirectory = null;
	
	/** Max age of an LRIT message in seconds. Messages older than this are discarded. */
	public int lritMaxMsgAgeSec = 7200;
	
	/** Set to positive integer to enable minimum hourly checking on LRIT */
	public int lritMinHourly = 0;
	
	/** Set to positive integer to enable minimum hourly checking on DDS Recv */
	public int ddsMinHourly = 0;
	
	/** Set to positive integer to enable minimum hourly checking on EDL */
	public int edlMinHourly = 0;

	/** Set to positive integer to enable minimum hourly checking on DRGS */
	public int drgsMinHourly = 0;

	public static final boolean def_noaaportEnabled = false;
	public static final int def_noaaportPort = 18000;
	public static final String def_archiveDir = ".";
	public static final int def_numDayFiles = 31;
	public static final boolean def_enableLritRecv = false;
	public static final char def_lritHeaderType = '6';
	public static final boolean def_enableDdsRecv = false;
	public static final boolean def_enableDrgsRecv = false;
	public static final int def_ddsListenPort = 16003;
	public static final String def_ddsBindAddr = null;
	public static final int def_ddsMaxClients = 150;
	public static final boolean def_ddsRequireAuth = false;
	public static final boolean def_ddsAllowAdmin = false;
	public static final String def_ddsUsageLog = "$LRGSHOME/dds-log";
	public static final String def_ddsNetlistDir = "$LRGSHOME/netlist";
	public static final String def_ddsUserRootDir = "$LRGSHOME/users";
	public static final String def_ddsUserRootDirLocal = "$LRGSHOME/users.local";
	public static final int def_maxDownlinks = 32;
	public static final int def_timeoutSeconds = 90;
	public static final String def_ddsRecvConfig = "$LRGSHOME/ddsrecv.conf";
	public static final String def_drgsRecvConfig = "$LRGSHOME/drgsrecv.conf";
	public static final String def_htmlStatusFile = "$LRGSHOME/lrgsstatus.html";
	public static final int def_htmlStatusSeconds = 10;
	public static final String def_mergePref1 = null;
	public static final String def_mergePref2 = null;
	public static final String def_mergePref3 = null;
	public static final String def_mergePref4 = null;
	public static final boolean def_enableDomsatRecv = false;
	public static final int def_domsatTimeout = 60;
	public static final boolean def_loadDomsat = true;
	public static final boolean def_enableDapsDqm = false;
	public static final String def_dqmSerialPort = "COM1";
	public static final String def_domsatClass = "lrgs.domsatrecv.DomsatSangoma";
	public static final boolean def_ignoreDrgsNoEotTermination = false;
	public static final int def_maxLogSize = 20000000; // 20 meg.
	public static final String def_dbUrl = null;
	public static final String def_sqlWriteDateFormat = "''yyyy-MM-dd HH:mm:ss''";
	public static final String def_sqlReadDateFormat = "yyyy-MM-dd HH:mm:ss";
	public static final String def_sqlTimeZone = "UTC";
	public static final String def_keyGeneratorClass = "decodes.sql.SequenceKeyGenerator";
	public static final String def_JdbcDriverClass = "org.postgresql.Driver";
	public static final boolean def_recoverOutages = false;
	public static final boolean def_localAdminOnly = false;
	public static final String def_dpcHost = "";
	public static final int def_dpcPort = 9000;
	public static final boolean def_enableDcpInterface = false;
	public static final boolean def_storeXmitRecords = false;
	public static final String def_dcpInterfaceXmlConfig = null;
	
	/**
	 * This contains the miscellaneous properties not represented on a custom
	 * GUI panel in rtstat.
	 */
	private PropertySpec miscPropSpecs[] = 
	{
		new PropertySpec("loadDecodes", PropertySpec.BOOLEAN,
			"Set to true to have DECODES database loaded at LRGS startup. Used in some"
			+ " circumstances to resolve network lists for DDS Receive"),
		new PropertySpec("damsNtTimeout", PropertySpec.INT,
			"If no data is received on a DAMS-NT socket in this many seconds, then issue a timeout"
			+ " warning and reconnect."),
		new PropertySpec("passwordCheckerClass", PropertySpec.STRING,
			"Name of class that does password checking. If not set, then all passwords accepted."),
		new PropertySpec("localIpMask", PropertySpec.STRING,
			"(e.g. 192.168.0.0/24) Local IP addresses will not be displayed on rtstat page"),
		new PropertySpec("hideHostNames", PropertySpec.BOOLEAN,
			"(default=false) Set to true to hide host names on the rtstat display."),
		new PropertySpec("restrictEventsToAdmin", PropertySpec.BOOLEAN,
			"(default=false) Set to true to disallow events to non-adminstrators "
			+ "on the rtstat display."),
			
		new PropertySpec("writeDacqEvents", PropertySpec.BOOLEAN,
			"(default=false) if loadDecodes is also true, this means to create DACQ_EVENT "
			+ "entries in the database for INFO and higher-priority events."),
		
//		new PropertySpec("storeXmitRecords", PropertySpec.BOOLEAN,
//			"Set to true to store XMIT Records (i.e. the DCP Monitor Function) (NOT IMPLEMENTED)")
	};
	
	/** 
	 * This needs to be public so that PropertiesUtil.loadFromProps will add
	 * properties with names that don't match public attributes to it.
	 */
	public Properties otherProps = new Properties();
	
	private PasswordChecker passwordChecker = null;

	/** If true, then Authenticated DDS connections must be done with SHA-256. */
	public boolean reqStrongEncryption = false;

	public static LrgsConfig instance()
	{
		if (_instance == null)
			_instance = new LrgsConfig();
		return _instance;
	}

	public LrgsConfig()
	{
		lastLoadTime = 0L;
		archiveDir = def_archiveDir;
		numDayFiles = def_numDayFiles;
		enableLritRecv = def_enableLritRecv;
		lritHeaderType = def_lritHeaderType;
		enableDdsRecv = def_enableDdsRecv;
		enableDrgsRecv = def_enableDrgsRecv;
		ddsListenPort = def_ddsListenPort;
		ddsBindAddr = def_ddsBindAddr;
		ddsMaxClients = def_ddsMaxClients;
		ddsRequireAuth = def_ddsRequireAuth;
		ddsAllowAdmin = def_ddsAllowAdmin;
		ddsUsageLog = def_ddsUsageLog;
		ddsNetlistDir = def_ddsNetlistDir;
		ddsUserRootDir = def_ddsUserRootDir;
		ddsUserRootDirLocal = def_ddsUserRootDirLocal;
		maxDownlinks = def_maxDownlinks;
		timeoutSeconds = def_timeoutSeconds;
		ddsRecvConfig = def_ddsRecvConfig;
		drgsRecvConfig = def_drgsRecvConfig;
		htmlStatusFile = def_htmlStatusFile;
		htmlStatusSeconds = def_htmlStatusSeconds;
		mergePref1 = def_mergePref1;
		mergePref2 = def_mergePref2;
		mergePref3 = def_mergePref3;
		mergePref4 = def_mergePref4;
		enableDomsatRecv = def_enableDomsatRecv;
		domsatTimeout = def_domsatTimeout;
		loadDomsat = def_loadDomsat;
		enableDapsDqm = def_enableDapsDqm;
		dqmSerialPort = def_dqmSerialPort;
		domsatClass = def_domsatClass;
		ignoreDrgsNoEotTermination = def_ignoreDrgsNoEotTermination;
		maxLogSize = def_maxLogSize;
		dbUrl = def_dbUrl;
		sqlWriteDateFormat = def_sqlWriteDateFormat;
		sqlReadDateFormat = def_sqlReadDateFormat;
		sqlTimeZone = def_sqlTimeZone;
		keyGeneratorClass = def_keyGeneratorClass;
		JdbcDriverClass = def_JdbcDriverClass;
		recoverOutages = def_recoverOutages;
		localAdminOnly = def_localAdminOnly;
		reqStrongEncryption = false;
	}

	public void setConfigFileName(String cfgName)
	{
		this.cfgFile = new File(cfgName);
	}

	/** Loads configuration from specified config file. */
	public void loadConfig()
		throws IOException
	{
		Logger.instance().warning(
			LrgsMain.module + ":" + LrgsMain.EVT_CONFIG_CHANGE
			+ " Loading configuration file '" + cfgFile.getPath() + "'");
		lastLoadTime = System.currentTimeMillis();
		Properties props = new Properties();
		FileInputStream is = new FileInputStream(cfgFile);
		props.load(is);
		is.close();

		PropertiesUtil.loadFromProps(this, props);
	}

	/** @return msec time of last configuration load. */
	public long getLastLoadTime() { return lastLoadTime; }

	/**
	 * Checks to see if config file has changed, and if so, reloads it. 
	 */
	public void checkConfig()
	{
		if (cfgFile.lastModified() > lastLoadTime)
		{
			try { loadConfig(); }
			catch(IOException ex)
			{
				Logger.instance().failure("Cannot load config file '" 
					+ cfgFile.getPath() + "': " + ex);
			}
		}
	}

	/** @return the configuration File object. */
	public File getCfgFile() { return cfgFile; }

	/**
	 * Retrieves a 'miscellaneous' property. 
	 * @param name property name
	 * @return property value or null if undefined.
	 */
	public String getMiscProp(String name)
	{
		return PropertiesUtil.getIgnoreCase(otherProps, name);
	}

	public void setMiscProp(String name, String value)
	{
		otherProps.setProperty(name, value);
	}
	
	/**
	 * Retrieves a 'miscellaneous' boolean property. 
	 * If not defined, returns the specified default value.
	 * @param name property name
	 * @defaultV the default value
	 * @return property value as a boolean or 'defaultV' if undefined.
	 */
	public boolean getMiscBooleanProperty(String name, boolean defaultV)
	{
		String pv = getMiscProp(name);
		if (pv == null)
			return defaultV;
		return TextUtil.str2boolean(pv);
	}

	public int getMiscIntProperty(String name, int defaultV)
	{
		String pv = getMiscProp(name);
		if (pv == null)
			return defaultV;
		try { return Integer.parseInt(pv); }
		catch(Exception ex)
		{
			Logger.instance().warning("Bad format for config value '" + name
				+ "': expected integer");
			return defaultV;
		}
	}

	/**
	 * @return true if GR3110 interface is enabled.
	 */
	public boolean getEnableGR3110Recv()
	{
		return getMiscBooleanProperty("gr3110.enable", false);
	}

	public int getGR3110Timeout()
	{
		return getMiscIntProperty("gr3110.timeout", 3600);
	}

	public String getGR3110SerialPort()
	{
		return getMiscProp("gr3110.serialPort");
	}

	public String getGR3110CaptureFile()
	{
		return getMiscProp("gr3110.captureFile");
	}

	public String getGR3110SrcCode()
	{
		return getMiscProp("gr3110.srcCode");
	}

	public String getGR3110SatelliteCode()
	{
		return getMiscProp("gr3110.satelliteCode");
	}

	public boolean getGR3110ParityStrip()
	{
		return getMiscBooleanProperty("gr3110.parityStrip", true);
	}

	/**
	 * The 'noTimeout' property says to never go 'unusable' because of a
	 * timeout. It is for and LRGS that should continue to function even
	 * when it is offline.
	 */
	public boolean getNoTimeout()
	{
		return getMiscBooleanProperty("noTimeout", false);
	}
	
	public boolean getLoadDecodes()
	{
		return getMiscBooleanProperty("loadDecodes", false);
	}
	
	public int getDamsNtTimeout()
	{
		return this.getMiscIntProperty("damsNtTimeout", 20);
	}

	public String getSangomaIfName()
	{
		return getMiscProp("SangomaIfName");
	}
	
	/** @return URL for downloading PDT - defaults to NESDIS DCS location */
	public String getPdtUrl() { return pdtUrl; }
	
	/** True if this LRGS should do PDT validation for interfaces that don't 
	 * already provide it.
	 */
	public boolean getDoPdtValidation() { return doPdtValidation; }
	
	/** @return URL for downloading channel map - default = nesdis ftp site */
	public String getChannelMapUrl() { return channelMapUrl; }
	
	public Properties getOtherProps()
	{
		return otherProps;
	}
	

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return miscPropSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}
	
	public PasswordChecker getPasswordChecker()
	{
		return passwordChecker;
	}

	public void setPasswordChecker(PasswordChecker passwordChecker)
	{
		this.passwordChecker = passwordChecker;
	}
}
