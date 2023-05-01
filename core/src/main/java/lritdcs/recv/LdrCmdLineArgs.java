/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/03/02 22:21:36  mjmaloney
*  Created.
*
*/
package lritdcs.recv;

import java.io.IOException;
import java.io.File;

import ilex.cmdline.*;
import ilex.util.FileLogger;
import ilex.util.Logger;
import ilex.util.StderrLogger;

public class LdrCmdLineArgs extends ApplicationSettings
{
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private StringToken configFile_arg;
	private BooleanToken noFileHeaders_arg;
	private StringToken lockFile_arg;
	private String progname;

	public LdrCmdLineArgs()
	{
		super();
		this.progname = "ldr";

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		String logname = "ldr.log";
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, logname);
		addToken(log_arg);

		configFile_arg = new StringToken(
			"f", "config-file", "", TokenOptions.optSwitch, "ldr.conf");
		addToken(configFile_arg);

		noFileHeaders_arg = new BooleanToken(
			"h", "(TEST ONLY) Don't Use LRIT File Headers", "", 
			TokenOptions.optSwitch, false);
		addToken(noFileHeaders_arg);

		lockFile_arg = new StringToken(
			"k", "lock-file-name", "", TokenOptions.optSwitch, "ldr.lock");
		addToken(lockFile_arg);
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
			Logger.setLogger(new FileLogger(progname, fn)); 
		}
		catch(IOException e)
		{
			System.err.println("Cannot open log file '" + fn + "': " + e);
			System.exit(1);
		}

		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);

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

	public String getLockFile()
	{
		return lockFile_arg.getValue();
	}
}
