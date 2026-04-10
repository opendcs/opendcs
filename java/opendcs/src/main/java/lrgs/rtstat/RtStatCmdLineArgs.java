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
package lrgs.rtstat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.DecodesSettings;

import ilex.cmdline.*;
import lrgs.db.LrgsConstants;
import ilex.util.EnvExpander;

public class RtStatCmdLineArgs extends StdAppSettings
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	// Add DECODES-specific setting declarations here...
	IntegerToken scan_arg;
	private StringToken iconFileArg;
	private StringToken headerFileArg;
	private StringToken hostArg;
	private StringToken userArg;
	private StringToken lrgsMonUrlArg;
	private StringToken logFileArg;
	private StringToken passwordArg;
	private IntegerToken portArg;

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
		logFileArg = new StringToken( "l", "logfile name",
			"", TokenOptions.optSwitch, "");
		addToken(logFileArg);
		passwordArg = new StringToken("pw", "Password for initial connection",
			"", TokenOptions.optSwitch, "");
		addToken(passwordArg);
		portArg = new IntegerToken("p", "port for the remote LRGS",
			"", TokenOptions.optSwitch, LrgsConstants.DEFAULT_LRGS_PORT);
		addToken(portArg);
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
				try (FileInputStream fis = new FileInputStream(propFile))
				{
					props.load(fis);
				}
				catch(IOException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("RtStatCmdLineArgs:parseArgs Cannot open DECODES Properties File '{}'", propFile);
				}
				settings.loadFromProperties(props);
			}
			//End new code
		}
		catch (IllegalArgumentException ex)
		{
			log.atError().setCause(ex).log("Illegal arguments ... program exiting.");
			System.exit(1);
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

	public String getPassword()
	{
		final String x = passwordArg.getValue();
		return x.length() > 0 ? x : null;
	}

	public int getPort()
	{
		return portArg.getValue();
	}

	public String getLrgsMonUrl()
	{
		return lrgsMonUrlArg.getValue();
	}
}