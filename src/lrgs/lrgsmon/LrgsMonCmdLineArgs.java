/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2008/02/10 20:17:36  mmaloney
*  dev
*
*  Revision 1.2  2008/01/31 21:24:19  cvs
*  files modified for internationalization
*
*  Revision 1.8  2007/10/26 17:05:01  mmaloney
*  added new argument that allows the user to change the header of the status summary page
*
*  Revision 1.7  2007/10/26 13:54:59  mmaloney
*  dev
*
*  Revision 1.6  2007/02/19 23:01:47  mmaloney
*  Summary Status Implementation
*
*  Revision 1.5  2005/08/09 18:20:02  mjmaloney
*  dev
*
*  Revision 1.4  2005/08/07 19:28:59  mjmaloney
*  Improvements to detailed report.
*
*  Revision 1.3  2004/06/08 19:31:35  mjmaloney
*  Final cosmetic mods
*
*  Revision 1.2  2004/06/08 18:03:34  mjmaloney
*  Added arguments to set image icon and lock file name.
*
*  Revision 1.1  2004/06/01 15:26:35  mjmaloney
*  Created.
*
*/
package lrgs.lrgsmon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import decodes.util.DecodesSettings;

import ilex.cmdline.*;
import ilex.util.Logger;
import ilex.util.FileLogger;
import ilex.util.EnvExpander;

public class LrgsMonCmdLineArgs
	extends StdAppSettings
{
	// Add DECODES-specific setting declarations here...
	private StringToken log_arg;
	private IntegerToken scan_arg;
	private String propFile;
	private StringToken outputDir_arg;
	private StringToken lrgsListFile_arg;
	private StringToken lockFileArg;
	private StringToken iconFileArg;
	private StringToken headerFileArg;
	private StringToken summaryHeaderFileArg;

    public LrgsMonCmdLineArgs()
	{
		super(false);

		// Construct DECODES-specific setting & call addToken for each one.
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, "lrgsmon.log");
		addToken(log_arg);
		scan_arg = new IntegerToken("s", "scan period (seconds)", "",
            TokenOptions.optSwitch, 30);
		addToken(scan_arg);
		outputDir_arg = new StringToken(
			"o", "Output Directory", "", TokenOptions.optSwitch, ".");
		addToken(outputDir_arg);
		lrgsListFile_arg = new StringToken(
			"f", "LRGS List File", "", TokenOptions.optSwitch, "lrgsmon.conf");
		addToken(lrgsListFile_arg);
		lockFileArg = new StringToken(
			"k", "lrgsmon lock file", "", TokenOptions.optSwitch, "lrgsmon.lock");
		addToken(lockFileArg);
		iconFileArg = new StringToken(
			"i", "Icon for Web Report", "", TokenOptions.optSwitch, "satdish.jpg");
		addToken(iconFileArg);

		headerFileArg = new StringToken(
			"h", "Header for detailed web report", "", TokenOptions.optSwitch, 
			null);
		addToken(headerFileArg);

		summaryHeaderFileArg = new StringToken(
			"r", "Header for summary web report", "", TokenOptions.optSwitch, 
			null);
		addToken(summaryHeaderFileArg);
    }

	/// Returns log file specified on command line, or default.
	public String getLogFile()
	{
		String s = log_arg.getValue();
		if (s == null || s.length() == 0)
			return null;
		return s;
	}

	/**
	  Parses command line arguments.
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);
		//New Code
		//Load the decodes.properties - this is used ONLY for 
		//internationalization
		String propFile;
		propFile = super.getPropertiesFile();
		// Get DECODES_INSTALL_DIR from system properties & look there.
		String installDir = System.getProperty("DECODES_INSTALL_DIR");
		if (installDir != null)
		{
			propFile = 
				installDir + File.separator + "decodes.properties";
		}
		DecodesSettings settings = DecodesSettings.instance();
		if (!settings.isLoaded())
		{
			Properties props = new Properties();
			try
			{
				FileInputStream fis = new FileInputStream(propFile);
				props.load(fis);
				fis.close();
			}
			catch(IOException e)
			{
				Logger.instance().log(Logger.E_WARNING,
				"LrgsMonCmdLineArgs:parseArgs " +
				"Cannot open DECODES Properties File '"+propFile+"': "+e);
			}
			settings.loadFromProperties(props);
		}
		//End new code
		
		// If log-file specified, open it.
		String fn = getLogFile();
		if (fn != null && fn.length() > 0)
		{
			String procname = Logger.instance().getProcName();
			try { Logger.setLogger(new FileLogger(procname, fn)); }
			catch(IOException e)
			{
				System.err.println("Cannot open log file '" + fn + "': " + e);
				System.exit(1);
			}
		}

		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);
	}

	/** @return scan period in seconds. */
	public int getScanPeriod()
	{
		int r = scan_arg.getValue();
		return r > 0 ? r : 30;
	}

	/** @return output directory for HTML files. */
	public String getOutputDir()
	{
		return outputDir_arg.getValue();
	}

	/** @return name of LRGS List file specified on cmd line, or default. */
	public String getLrgsListFile()
	{
		return lrgsListFile_arg.getValue();
	}

	/** @return name of lock file */
	public String getLockFile()
	{
		return lockFileArg.getValue();
	}

	/** @return name of image file to be referenced from report. */
	public String getIconFile()
	{
		return iconFileArg.getValue();
	}

	/** @return name of header file to be used in detail report. */
	public String getHeaderFile()
	{
		return headerFileArg.getValue();
	}

	/** @return name of header file to be used in summary report. */
	public String getSummaryHeaderFile()
	{
		return summaryHeaderFileArg.getValue();
	}
}
