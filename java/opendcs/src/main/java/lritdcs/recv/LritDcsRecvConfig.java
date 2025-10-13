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
package lritdcs.recv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;


/**
Holds the LRIT DCS File Receiver Configuration.
*/
public class LritDcsRecvConfig
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	 * One of the header-type constants defined in LritDcsDirMonitor.
	 */
	public char fileHeaderType;

	/**
	 * Directory to monitor for incoming LRIT files.
	 */
	public String fileInputDir;

	/**
	 * Directory to place files in after processing.
	 */
	public String fileDoneDir;

	/** Directory to store the hourly message files in. */
	public String msgFileDir;

	/** Prefix for DCS files, if set, all others will be ignored. */
	public String filePrefix;

	/** suffix for DCS files, if set, all others will be ignored. */
	public String fileSuffix;


	// Last time configuration was loaded from disk file.
	private long lastLoadTime;

	// the one and only instance.
	private static LritDcsRecvConfig _instance = null;

	// My properties file
	private File propFile;

	public static LritDcsRecvConfig instance()
	{
		if (_instance == null)
			_instance = new LritDcsRecvConfig();
		return _instance;
	}

	private LritDcsRecvConfig()
	{
		fileHeaderType = LritDcsDirMonitor.HEADER_TYPE_DOM6;
		fileInputDir = "dcs-files";
		fileDoneDir = "dcs-done";
		msgFileDir = "hourly-files";
		filePrefix = null;
		fileSuffix = null;
		lastLoadTime = 0L;
		propFile = null;
	}

	public void setPropFile(String propFileName)
	{
		propFile = new File(propFileName);
	}

	public synchronized void load()
		throws IOException
	{
		if (propFile == null)
			return;
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(propFile);
		props.load(fis);
		fis.close();
		PropertiesUtil.loadFromProps(this, props);

		File f = new File(fileInputDir);
		if (!f.isDirectory())
			if (!f.mkdirs())
			{
				log.warn("Directory '{}' does not exist and cannot be made.", f.getPath());
			}
		f = new File(fileDoneDir);
		if (!f.isDirectory())
			if (!f.mkdirs())
			{
				log.warn("Directory '{}' does not exist and cannot be made.", f.getPath());
			}
		f = new File(msgFileDir);
		if (!f.isDirectory())
			if (!f.mkdirs())
			{
				log.warn("Directory '{}' does not exist and cannot be made.", f.getPath());
			}
		lastLoadTime = System.currentTimeMillis();
	}

	public void checkConfig()
		throws IOException
	{
		if (propFile.exists() && propFile.lastModified() > lastLoadTime)
			load();
	}

	public long getLastLoadTime()
	{
		return lastLoadTime;
	}
}
