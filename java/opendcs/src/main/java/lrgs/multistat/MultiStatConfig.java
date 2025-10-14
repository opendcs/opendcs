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
package lrgs.multistat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import ilex.util.PropertiesUtil;

import lritdcs.LritDcsConfig;

/**
* This class is a singleton that holds the configuration for the multi-status
* GUI. We use PropertiesUtil to read/write the values from a Java properties
* file.
*/
public class MultiStatConfig
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		adminLrgs = "CDADATA";

		StringBuffer sb = new StringBuffer(EnvExpander.expand("$DCSTOOL_HOME"));
		for(int i=0; i<sb.length(); i++)
			if (sb.charAt(i) == '\\')
				sb.setCharAt(i, '/');
		alarmInfoBasePath = sb.toString() + "/doc/";
		alarmInfoBaseUrl = "file://"
			+ (alarmInfoBasePath.startsWith("/") ? "" : "/")
			+ alarmInfoBasePath;
		log.debug("Alarm files locally stored in '{}' local URL is '{}'", alarmInfoBasePath, alarmInfoBaseUrl);

		Properties props = System.getProperties();
		props.setProperty("LRITDCS_HOME", props.getProperty("user.dir"));
		LritDcsConfig.exitOnLoadError = false;
		lastLoadTime = 1L;
    }

	public void setConfigFileName(String cfgName)
	{
		this.cfgName = cfgName;
		this.cfgFile = new File(cfgName);
		log.info("Config file set to '{}'", cfgFile.getPath());
	}

	/** Loads configuration from specified config file. */
	public void loadConfig()
		throws IOException
	{
		clearConfig();
		log.info("Loading configuration file '{}'", cfgFile.getPath());
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
				log.atError().setCause(ex).log("Cannot load config file '{}'", cfgFile.getPath());
			}
		}
		return false;
	}
}