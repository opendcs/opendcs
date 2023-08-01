/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.lrgsmain;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.SequenceFileLogger;
import ilex.util.QueueLogger;
import ilex.util.Logger;
import ilex.util.TeeLogger;

public class LrgsCmdLineArgs extends ApplicationSettings
{
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private StringToken configFile_arg;
	private IntegerToken maxLogSize_arg;
	private IntegerToken numOldLogs_arg;
	private final StringToken lockArg = new StringToken("k", "lock-file",
	"",TokenOptions.optSwitch, "$LRGSHOME/lrgs.lock");
	public final BooleanToken windowsSvcArg = new BooleanToken("w",
	"Run as Windows Service", "", TokenOptions.optSwitch, false);
	private final BooleanToken foreground = new BooleanToken("F",
		"Run in forground. Captures SigTerm Directly","",
		TokenOptions.optSwitch,false);

	public static final String progname = "lrgs";

	QueueLogger qLogger;
	SequenceFileLogger fLogger;

	public LrgsCmdLineArgs()
	{
		super();

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, 
			EnvExpander.expand("$LRGSHOME/lrgslog"));
		addToken(log_arg);

		configFile_arg = new StringToken(
			"f", "config-file", "", TokenOptions.optSwitch, 
			"$LRGSHOME/lrgs.conf");
		addToken(configFile_arg);

		maxLogSize_arg = new IntegerToken("S", "MaxLogSize", "",
			TokenOptions.optSwitch, 20000000);
		addToken(maxLogSize_arg);

		numOldLogs_arg = new IntegerToken("N", "NumOldLogs", "",
			TokenOptions.optSwitch, 5);
		addToken(numOldLogs_arg);
		addToken(lockArg);
		addToken(windowsSvcArg);
		addToken(foreground);

		qLogger = null;
		fLogger = null;


	}

    /**
     * @return the numeric debug-level specified on the command line, or
     * E_INFORMATION not specified as d1,2, or 3
     */
	public int getDebugLevel()
	{
		return Logger.debugLevelFromArgValue(debuglevel_arg.getValue());
	}

	public String getLogFile()
	{
		return log_arg.getValue();
	}

	/**
	  Parses command line arguments, sets internal variables, & sets up logging.
	  We will use a TeeLogger to fork log messages to a file and to an internal
	  QueueLogger. The Queue will be used by DDS clients who want to retrieve
	  log messages.
	  @param args command line arguments from main().
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		try 
		{
			qLogger = new QueueLogger(progname,Logger.E_INFORMATION);

			fLogger = new SequenceFileLogger(
						progname,
						getLogFile(),
						getDebugLevel(),
						maxLogSize_arg.getValue(),
						numOldLogs_arg.getValue()			
			);

			TeeLogger tLogger = new TeeLogger(progname, fLogger, qLogger);
			Logger.setLogger(tLogger); 
	
			SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			Logger.setDateFormat(df);
		}
		catch(IOException ex)
		{
			System.err.println("Cannot open log file '" + getLogFile() 
				+ "': " + ex);
			System.exit(1);
		}

		Logger.instance().log(Logger.E_INFORMATION, "========== Process '"
			+ progname + "' Starting ==========");
	}

	public String getConfigFile()
	{
		return configFile_arg.getValue();
	}
	
	public String getLockFile()
	{
		return lockArg.getValue();
	}

	public boolean runInForGround() {
		return this.foreground.getValue();
	}
}
