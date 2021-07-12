package lrgs.multistat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import lritdcs.LritDcsConfig;

/**
* This class is a singleton that holds the configuration for the multi-status
* GUI. We use PropertiesUtil to read/write the values from a Java properties
* file.
*/
public class MultiStatConfig
{
	/** display name of the 1st LRGS */
	public String Lrgs1DisplayName = "";

	/** Hostname of the 1st LRGS */
	public String Lrgs1HostName = null;

	/** Port number for connecting to 1st LRGS */
	public int Lrgs1Port = 16003;

	/** user name for connecting to the 1st LRGS */
	public String Lrgs1UserName = null;

	/** password for connecting to the 1st LRGS */
	public String Lrgs1Password = "N";

	/** display name of 2nd LRGS */
	public String Lrgs2DisplayName = "";

	/** Hostname of the 2nd LRGS */
	public String Lrgs2HostName = null;

	/** Port number for connecting to 2nd LRGS */
	public int Lrgs2Port = 16003;

	/** user name for connecting to the 2nd LRGS */
	public String Lrgs2UserName = null;

	/** password for connecting to the 2nd LRGS */
	public String Lrgs2Password = "N";

	/** display name of the 3rd LRGS */
	public String Lrgs3DisplayName = "";

	/** Hostname of the 3rd LRGS */
	public String Lrgs3HostName = null;

	/** Port number for connecting to 3rd LRGS */
	public int Lrgs3Port = 16003;

	/** user name for connecting to the 3rd LRGS */
	public String Lrgs3UserName = null;

	/** password for connecting to the 3rd LRGS */
	public String Lrgs3Password = "N";

	public String Lrgs4DisplayName = "";
	public String Lrgs4HostName = null;
	public int Lrgs4Port = 16003;
	public String Lrgs4UserName = null;
	public String Lrgs4Password = "N";

	
	
	
//	/** The base URL of the help dir. */
//	public String HelpUrlBase;
//
	/** The sound file to play when an alarm happens. */
	public String soundFile = "alarm.wav";

	/** The base URL in which to find alarm info files. */
	public String alarmInfoBaseUrl;

	/** The local path to the base URL for constructing error files, etc. */
	public String alarmInfoBasePath;

	/** The initial 'Mute' checkbox setting. */	
	public boolean mute = false;

	/** The default operator name. */
	public String operator;

	/** The display name of the LRGS to be used for admin functions. */
	public String adminLrgs = null;

	/** The singleton instance. */
	private static MultiStatConfig _instance = null;

	/** Name of configuration file. */
	private String cfgName;

	/** File object for config file. */
	private File cfgFile;

	/** Last time configuration was loaded from file. */
	private long lastLoadTime;

	/** The public accessor method. */
	public static MultiStatConfig instance()
	{
		if (_instance == null)
			_instance = new MultiStatConfig();
		return _instance;
	}


    private MultiStatConfig()
    {
//		Lrgs1DisplayName = "CDADATA";
//		Lrgs1HostName = "192.168.101.174";
//		Lrgs1Port = 16003;
//		Lrgs1UserName = "wcdas";
//		Lrgs1Password = "Y";
//		Lrgs2DisplayName = "CDABACKUP";
//		Lrgs2HostName = "192.168.101.189";
//		Lrgs2Port = 16003;
//		Lrgs2UserName = "wcdas";
//		Lrgs2Password = "N";
//		Lrgs3DisplayName = "NLRGS1";
//		Lrgs3HostName = "nlrgs1.noaa.gov";
//		Lrgs3Port = 16003;
//		Lrgs3UserName = "wcdas";
//		Lrgs3Password = "N";
//		Lrgs4HostName = "nlrgs2.noaa.gov";
//		Lrgs4Port = 16003;
//		Lrgs4UserName = "wcdas";
//		Lrgs4Password = "N";
		
//		HelpUrlBase = "";
		adminLrgs = "CDADATA";

		StringBuffer sb = new StringBuffer(EnvExpander.expand("$DCSTOOL_HOME"));
		for(int i=0; i<sb.length(); i++)
			if (sb.charAt(i) == '\\')
				sb.setCharAt(i, '/');
		alarmInfoBasePath = sb.toString() + "/doc/";
		alarmInfoBaseUrl = "file://" 
			+ (alarmInfoBasePath.startsWith("/") ? "" : "/")
			+ alarmInfoBasePath;
		Logger.instance().debug1("Alarm files locally stored in '"
			+ alarmInfoBasePath + "' local URL is '" + alarmInfoBaseUrl + "'");

		Properties props = System.getProperties();
		props.setProperty("LRITDCS_HOME", props.getProperty("user.dir"));
		LritDcsConfig.exitOnLoadError = false;		
		lastLoadTime = 1L;		
    }

	public void setConfigFileName(String cfgName)
	{
		this.cfgName = cfgName;
		this.cfgFile = new File(cfgName);
		Logger.instance().info("Config file set to '" + cfgFile.getPath() + "'");
	}

	/** Loads configuration from specified config file. */
	public void loadConfig()
		throws IOException
	{
//		System.out.println("Loading configuration file '" + cfgFile.getPath() + "'");
		clearConfig();
		Logger.instance().info("Loading configuration file '" + cfgFile.getPath() + "'");
		lastLoadTime = System.currentTimeMillis();
		Properties props = new Properties();
		FileInputStream is = new FileInputStream(cfgFile);
		props.load(is);
		is.close();

		PropertiesUtil.loadFromProps(this, props);
		if (!alarmInfoBaseUrl.endsWith("/"))
			alarmInfoBaseUrl = alarmInfoBaseUrl + "/";
		if (!alarmInfoBasePath.endsWith("/"))
			alarmInfoBasePath = alarmInfoBasePath + "/";
	}
	
	private void clearConfig()
	{
		Lrgs1DisplayName = "";
		Lrgs1HostName = null;
		Lrgs1Port = 16003;
		Lrgs1UserName = null;
		Lrgs1Password = "N";
		Lrgs2DisplayName = "";
		Lrgs2HostName = null;
		Lrgs2Port = 16003;
		Lrgs2UserName = null;
		Lrgs2Password = "N";
		Lrgs3DisplayName = "";
		Lrgs3HostName = null;
		Lrgs3Port = 16003;
		Lrgs3UserName = null;
		Lrgs3Password = "N";
		Lrgs4DisplayName = "";
		Lrgs4HostName = null;
		Lrgs4Port = 16003;
		Lrgs4UserName = null;
		Lrgs4Password = "N";

		soundFile= "alarm.wav";

		StringBuffer sb = new StringBuffer(EnvExpander.expand("$DCSTOOL_HOME"));
		for(int i=0; i<sb.length(); i++)
			if (sb.charAt(i) == '\\')
				sb.setCharAt(i, '/');
		alarmInfoBasePath = sb.toString() + "/doc/";
		alarmInfoBaseUrl = "file://" 
			+ (alarmInfoBasePath.startsWith("/") ? "" : "/")
			+ alarmInfoBasePath;

		adminLrgs = null;
	}

	/** @return msec time of last configuration load. */
	public long getLastLoadTime() { return lastLoadTime; }

	/**
	 * Checks to see if config file has changed, and if so, reloads it. 
	 */
	public boolean checkConfig()
	{
		if (cfgFile.lastModified() > lastLoadTime)
		{
			try
			{
				loadConfig();
				return true;
			}
			catch(IOException ex)
			{
				Logger.instance().failure("Cannot load config file '" 
					+ cfgFile.getPath() + "': " + ex);
			}
		}
		return false;
	}
}
