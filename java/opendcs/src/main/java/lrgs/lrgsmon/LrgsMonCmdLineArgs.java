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
package lrgs.lrgsmon;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.DecodesSettings;

import ilex.cmdline.*;
import ilex.util.EnvExpander;

public class LrgsMonCmdLineArgs extends StdAppSettings
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		// MJM 20210213 new location for prop file
		String propFile = super.getPropertiesFile(); // -P cmd line arg
		FileInputStream fis = null;
		if (propFile != null)
		{
			try { fis = new FileInputStream(propFile); }
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot find specified DECODES properties file '{}'", propFile);
				fis = null;
			}
		}
		if (fis == null)
		{
			propFile = EnvExpander.expand("$DCSTOOL_USERDIR/user.properties");
			try { fis = new FileInputStream(propFile); }
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot find user DECODES properties file '{}'", propFile);
				fis = null;
			}
		}
		if (fis == null)
		{
			propFile = EnvExpander.expand("$DCSTOOL_HOME/decodes.properties");
			try { fis = new FileInputStream(propFile); }
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot find default DECODES properties file '{}'", propFile);
				fis = null;
			}
		}
		if (fis != null)
		{
			log.info("Loading DECODES properties from '{}'", propFile);
			DecodesSettings settings = DecodesSettings.instance();
			if (!settings.isLoaded())
			{
				Properties props = new Properties();
				try
				{
					props.load(fis);
				}
				catch(IOException ex)
				{
					log.atWarn().setCause(ex).log("LrgsMonCmdLineArgs:parseArgs Cannot open DECODES Properties File '{}'", propFile);
				}
				finally
				{
					try { fis.close(); } catch(Exception ex) {}
				}
				settings.loadFromProperties(props);
			}
		}

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
