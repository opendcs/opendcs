/*
* $Id$
*/
package lrgs.rtstat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import decodes.util.DecodesSettings;

import ilex.cmdline.*;
import ilex.util.Logger;
import ilex.util.FileLogger;
import ilex.util.EnvExpander;

public class RtStatCmdLineArgs
	extends StdAppSettings
{
	// Add DECODES-specific setting declarations here...
	IntegerToken scan_arg;
	private StringToken iconFileArg;
	private StringToken headerFileArg;
	private StringToken hostArg;
	private StringToken userArg;
	private StringToken lrgsMonUrlArg;

    public RtStatCmdLineArgs()
	{
		super(false);

		scan_arg = new IntegerToken("s", "scan period (seconds)", "",
            TokenOptions.optSwitch, 2);
		addToken(scan_arg);
		iconFileArg = new StringToken(
			"i", "Icon for Web Report", "", TokenOptions.optSwitch, 
			"file://$DECODES_INSTALL_DIR/icons/satdish.jpg");
		hostArg = new StringToken( "h", "host name for initial connection", 
			"", TokenOptions.optSwitch, "");
		addToken(hostArg);
		userArg = new StringToken( "u", "user name for initial connection", 
			"", TokenOptions.optSwitch, "");
		addToken(userArg);
		headerFileArg = new StringToken("H", "Header for HTML screen", 
			"", TokenOptions.optSwitch, "");
		addToken(headerFileArg);
		lrgsMonUrlArg = new StringToken( "M", 
			"URL pointing to LRGS Monitor Web Application", 
			"", TokenOptions.optSwitch, "");
		addToken(lrgsMonUrlArg);
    }

	/**
	  Parses command line arguments.
	*/
	public void parseArgs(String args[])
	{
		try 
		{ 
			super.parseArgs(args); 
			//New Code
			//Load the decodes.properties
			String propFile;
			propFile = super.getPropertiesFile();
			if (propFile == null || propFile.length() == 0)
			{
				// Get DECODES_INSTALL_DIR from system properties & look there.
				String installDir = System.getProperty("DECODES_INSTALL_DIR");
				if (installDir != null)
				{
					propFile = 
						installDir + File.separator + "decodes.properties";
				}
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
					"RtStatCmdLineArgs:parseArgs " +
					"Cannot open DECODES Properties File '"+propFile+"': "+e);
				}
				settings.loadFromProperties(props);
			}
			//End new code
		}
		catch (IllegalArgumentException ex) 
		{
			System.err.println("Illegal arguments ... program exiting.");
			System.exit(1);
		}
		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
		{
			int dv = 
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3;
			// Debug info only goes to file, never to clients.
			Logger.instance().setMinLogPriority(dv);
		}
	}

	/** @return scan period in seconds. */
	public int getScanPeriod()
	{
		int r = scan_arg.getValue();
		return r > 0 ? r : 2;
	}

	/** @return name of image file to be referenced from report. */
	public String getIconFile()
	{
		return EnvExpander.expand(iconFileArg.getValue());
	}

	/** @return name of header file to be used in detail report. */
	public String getHeaderFile()
	{
		String x = headerFileArg.getValue();
		return x.length() > 0 ? x : null;
	}

	/** @return the host name arg supplied on command line, or null if none. */
	public String getHostName()
	{
		String x = hostArg.getValue();
		return x.length() > 0 ? x : null;
	}
	
	public String getUserName()
	{
		String x = userArg.getValue();
		return x.length() > 0 ? x : null;
	}

	public String getLrgsMonUrl()
	{
		return lrgsMonUrlArg.getValue();
	}
}
