/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2004/05/19 14:03:43  mjmaloney
*  dev.
*
*/
package lqm;

import java.io.IOException;
import java.io.File;

import ilex.cmdline.*;
import ilex.util.FileLogger;
import ilex.util.Logger;
import ilex.util.StderrLogger;

public class LqmCmdLineArgs extends ApplicationSettings
{
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private StringToken configFile_arg;
	private BooleanToken noFileHeaders_arg;
	private String progname;

	public LqmCmdLineArgs()
	{
		super();
		this.progname = "lqm";

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		String logname = "lqm.log";
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, logname);
		addToken(log_arg);

		configFile_arg = new StringToken(
			"f", "config-file", "", TokenOptions.optSwitch, "lqm.conf");
		addToken(configFile_arg);

		noFileHeaders_arg = new BooleanToken(
			"h", "(TEST ONLY) Don't Use LRIT File Headers", "", 
			TokenOptions.optSwitch, false);
		addToken(noFileHeaders_arg);
	}

    /**
     * Returns the numeric debug-level specified on the command line, or
     * 0 if none was specified.
     */
	public int getDebugLevel()
	{
		return debuglevel_arg.getValue();
	}

	public String getLogFile()
	{
		return log_arg.getValue();
	}

	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		// If log-file specified, open it.
		String fn = getLogFile();
		try 
		{
			Logger.setLogger(new FileLogger(progname, fn, getDebugLevel())); 
		}
		catch(IOException e)
		{
			System.err.println("Cannot open log file '" + fn + "': " + e);
			System.exit(1); // TODO: remove this System.exit
		}
		
		Logger.instance().log(Logger.E_INFORMATION, "Process '"
			+ progname + "' Starting.....");
	}

	public String getConfigFile()
	{
		return configFile_arg.getValue();
	}

	public boolean useFileHeaders()
	{
		return !noFileHeaders_arg.getValue();
	}
}
