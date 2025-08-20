/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2009/08/24 13:33:20  shweta
*  Configuration variables added for backup LRIT.
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/05/27 13:15:01  mjmaloney
*  DR fixes.
*
*  Revision 1.2  2004/05/19 14:03:44  mjmaloney
*  dev.
*
*  Revision 1.1  2004/05/18 01:01:57  mjmaloney
*  Created.
*
*/
/*
*  Orig Author: David Johnson, ILEX Engineering, Inc.
*/

package lqm;

import java.util.*;
import java.io.*;

import ilex.util.Logger;

public class LqmConfiguration
{
	/// Configuration file to load
	File cfgFile;

	/// Delete files older than this many days.
	int maxFileAge;

	/// Scan this directory for incoming DCS files.
	File dcsInputDir;

	/// Move files to this directory after processing them.
	File dcsDoneDir;

	/// Host name or IP Address of LRIT server to connect to.
	String lritHostName;

	/// TCP Port Number to use for LRIT connection
	int lritPortNum;

	/// Last time configuration was loaded.
	private long lastLoadMillis;

	private static LqmConfiguration _instance = null;
	
	 String lritHostNameAlt;
	
	 int lritPortNumAlt;
	
	public static LqmConfiguration instance()
	{
		if(_instance == null)
			_instance = new LqmConfiguration();
		return(_instance);
	}

	/// Called from instance method. Initialize config to default values.
	private LqmConfiguration()
	{
		cfgFile = new File("lqm.conf");
		maxFileAge = 7;
		dcsInputDir = new File("dcsfiles");
		dcsDoneDir = new File("dcsdone");
		lritHostName = "drot.wcda.noaa.gov";
		lritPortNum = 17004;
		lritHostNameAlt = "drot.wcda.noaa.gov";
		lritPortNumAlt = 17004;
		lastLoadMillis = 0L;

	}	

	/// Loads configuration from specified config file.
	public void loadConfig()
		throws IOException
	{
		Properties Prop1 = new Properties();
		System.out.println(cfgFile);
		InputStream is = new FileInputStream(cfgFile);
		Prop1.load(new FileInputStream(cfgFile));
		is.close();
		
		for (Enumeration e = Prop1.propertyNames(); e.hasMoreElements(); ) 
		{
    		String key = (String) e.nextElement();

			if (key.equalsIgnoreCase("maxFileAge"))
			{
				String val = Prop1.getProperty(key);
				try { maxFileAge = Integer.parseInt(val.trim()); }
				catch(NumberFormatException ex)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Improper maxFileAge value '" + val + "'");
					maxFileAge = 7;
				}
			}
			else if (key.equalsIgnoreCase("dcsInputDir"))
			{
				String val = Prop1.getProperty(key);
				dcsInputDir = new File(val);
				if (dcsInputDir.isDirectory() == false)
					if (dcsInputDir.mkdirs() == false)
					{
						Logger.instance().log(Logger.E_FAILURE,
							"Cannot create input directory '" + val +"'");
					}
			}
			else if (key.equalsIgnoreCase("dcsDoneDir"))
			{
				String val = Prop1.getProperty(key);
				dcsDoneDir = new File(val);
				if (dcsDoneDir.isDirectory() == false)
					if (dcsDoneDir.mkdirs() == false)
					{
						Logger.instance().log(Logger.E_FAILURE,
							"Cannot create done directory '" + val +"'");
					}
			}
			else if (key.equalsIgnoreCase("lritHostName"))
			{
				lritHostName = Prop1.getProperty(key);
			}	
			else if (key.equalsIgnoreCase("lritHostNameAlt"))
			{
				lritHostNameAlt = Prop1.getProperty(key);
			}	
			else if (key.equalsIgnoreCase("lritPortNum"))
			{
				String val = Prop1.getProperty(key);
				try { lritPortNum = Integer.parseInt(val.trim()); }
				catch(NumberFormatException ex)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Improper lritPortNum value '" + val + "'");
					lritPortNum = 17004;
				}
			}
			
			else if (key.equalsIgnoreCase("lritPortNumAlt"))
			{
				String val = Prop1.getProperty(key);
				try { lritPortNumAlt = Integer.parseInt(val.trim()); }
				catch(NumberFormatException ex)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Improper lritPortNumAlt value '" + val + "'");
					lritPortNumAlt = 17004;
				}
			}
		}

		lastLoadMillis = System.currentTimeMillis();
	}

	/// Sets name of configuration file
	public void setConfigFileName(String nm)
	{
		cfgFile = new File(nm);
	}

	/// Checks configuration & reloads it if it has changed.
	public void checkConfig()
	{
		if (cfgFile.lastModified() > lastLoadMillis)
		{
			try { loadConfig(); }
			catch(IOException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
				  "Cannot load config file '" + cfgFile.getPath()
				  + "': " + ex);
				return;
			}
		}
	}

	/// Returns time that config was last loaded
	public long getLastLoadTime() { return lastLoadMillis; }

	// Test main: load config and print values to stdout.
	public static void main(String args[])
		throws IOException
	{
		LqmConfiguration cfg = LqmConfiguration.instance();
		if (args.length > 0)
			cfg.setConfigFileName(args[0]);
		cfg.loadConfig();
/*
		System.out.println("maxFileAge=" + cfg.maxFileAge);
		System.out.println("lastLoadMillis=" + cfg.lastLoadMillis);
		System.out.println("lritHostName=" + cfg.lritHostName);
		System.out.println("lritPortNum=" + cfg.lritPortNum);
		System.out.println("dcsInputDir=" + cfg.dcsInputDir.getPath());
		System.out.println("dcsDoneDir=" + cfg.dcsDoneDir.getPath());*/
	}
}
