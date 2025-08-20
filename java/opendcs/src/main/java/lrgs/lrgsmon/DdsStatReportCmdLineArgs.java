/*
*
*/
package lrgs.lrgsmon;

import ilex.cmdline.StdAppSettings;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;

/**
 * This class reads all the command line arguments used
 * by the DdsStatReport program.
 *
 */
public class DdsStatReportCmdLineArgs
	extends StdAppSettings
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

		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);
	}
}
