package lritdcs.lrit2damsnt;

import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import lritdcs.recv.LritDcsDirMonitor;

public class Lrit2DamsNtConfig
{
	/**
	 * One of the header-type constants defined in LritDcsDirMonitor.
	 */
	public char fileHeaderType = LritDcsDirMonitor.HEADER_TYPE_DOM6;
	
	/**
	 * Directory to monitor for incoming LRIT files.
	 * No default -- required config parameter.
	 */
	public String fileInputDir = null;
	
	/**
	 * Optional directory to place files in after processing.
	 * If not specified or blank, then files are discarded after processing.
	 */
	public String fileDoneDir = null;
	
	/** Prefix for DCS files, if set, all others will be ignored. */
	public String filePrefix = null;

	/** suffix for DCS files, if set, all others will be ignored. default=".dcs" */
	public String fileSuffix = ".dcs";

	/** Port to listen on for DAMS-NT message connections (required) */
	public int msgListenPort = -1;
	
	/** Port to listen on for DAMS-NT event connections (no event port if undefined) */
	public int evtListenPort = -1;
	
	/** 
	 * For multi-headed systems, optionally specify an interface to listen on.
	 * Default is to listen on any connection. Specify hostname or IP address.
	 */
	public String listenHost = null;
	
	/** Hex string containing 4-byte DAMS-NT start pattern (default = SM\r\n) */
	public String damsNtStartPattern = "534D0D0A";
	
	/** Do not process files older than this # seconds (default = 900 = 15 min) */
	public int fileAgeMaxSeconds = 600;
	
	/** Set to true to process only files for which CRC check is good. */
	public boolean goodFilesOnly = false;
	
	// Last time configuration was loaded from disk file.
	private long lastLoadTime = 0L;

	// the one and only instance.
	private static Lrit2DamsNtConfig _instance = null;

	// My properties file
	private File propFile;
	
	public static Lrit2DamsNtConfig instance()
	{
		if (_instance == null)
			_instance = new Lrit2DamsNtConfig();
		return _instance;
	}

	private Lrit2DamsNtConfig()
	{
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
				System.out.println("Directory '" + f.getPath() 
					+ "' does not exist and cannot be made.");
		if (fileDoneDir != null)
		{
			f = new File(fileDoneDir);
			if (!f.isDirectory())
				if (!f.mkdirs())
					System.out.println("Directory '" + f.getPath() 
						+ "' does not exist and cannot be made.");
		}
		lastLoadTime = System.currentTimeMillis();
	}

	public void checkConfig()
		throws IOException
	{
		if (!propFile.canRead())
			throw new IOException("Config file '" + propFile.getPath() + "' does not exist or is unreadable.");
		if (propFile.lastModified() > lastLoadTime)
			load();
	}

	public long getLastLoadTime()
	{
		return lastLoadTime;
	}
}
