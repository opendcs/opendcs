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

import ilex.cmdline.StdAppSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

/**
 * This class reads all the command line arguments used
 * by the DdsStatReport program.
 *
 */
public class DdsStatReportCmdLineArgs extends StdAppSettings
{
	// LRGS User Utilization Reports cmd line arguments
	private StringToken startTime_arg;
	private StringToken endTime_arg;
	private StringToken hourlyFileName_arg;
	private StringToken htmlFilesDirectory_arg;
	private StringToken lrgsConfigFilePath_arg;
	private StringToken lrgsName_arg;

	/** Constructor. Initialize all command line arguments variables */
    public DdsStatReportCmdLineArgs()
	{
		super(false);

		// Construct LRGS User Utilization Reports arguments
		// & call addToken for each one.
		startTime_arg = new StringToken(
			"s", "start_time Format: yyyyMMdd[HH]", "",
			TokenOptions.optMultiple, "Today's date at 00:00");
		addToken(startTime_arg);
		endTime_arg = new StringToken(
				"e", "end_time Format: yyyyMMdd[HH]", "",
				TokenOptions.optMultiple, "Today's date at 23:59");
		addToken(endTime_arg);
		hourlyFileName_arg = new StringToken(
				"o", "Output hourly file name (omit .html)",
				"", TokenOptions.optRequired, "");
		addToken(hourlyFileName_arg);
		htmlFilesDirectory_arg = new StringToken(
				"D", "Output Directory for html files", "",
				TokenOptions.optSwitch, "$LRGSHOME/reports");
		addToken(htmlFilesDirectory_arg);
		lrgsConfigFilePath_arg = new StringToken(
				"f", "Lrgs DB conf file name with path", "",
				TokenOptions.optSwitch, "$LRGSHOME/lrgs.conf");
		addToken(lrgsConfigFilePath_arg);
		lrgsName_arg = new StringToken(
				"n", "The LRGS name", "",
				TokenOptions.optRequired, "");
		addToken(lrgsName_arg);
    }

    /** @return startTime argument for LRGS usage report HTML files. */
	public String getStartTime()
	{
		return startTime_arg.getValue();
	}

    /** @return endTime argument for LRGS usage report HTML files. */
	public String getEndTime()
	{
		return endTime_arg.getValue();
	}

    /** @return file name argument for LRGS usage report HTML files. */
	public String getHourlyFileName()
	{
		return hourlyFileName_arg.getValue();
	}

	/** @return file directory argument for LRGS usage report HTML files. */
	public String getHtmlFilesDirectory()
	{
		return htmlFilesDirectory_arg.getValue();
	}

	/** @return file path to the Lrgs.conf file. */
	public String getLrgsConfigFilePath()
	{
		return lrgsConfigFilePath_arg.getValue();
	}

	/** @return name of lrgs in used. */
	public String getLrgsName()
	{
		return lrgsName_arg.getValue();
	}

	/**
	  Parses command line arguments.
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);
	}
}