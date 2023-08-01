/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/09/02 13:09:03  mjmaloney
*  javadoc
*
*  Revision 1.3  2003/04/22 23:59:32  mjmaloney
*  1st working DRGS implementation.
*
*  Revision 1.2  2003/03/27 21:17:43  mjmaloney
*  drgs dev
*
*  Revision 1.1  2003/03/27 16:58:50  mjmaloney
*  drgs development.
*
*/
package lrgs.drgs;

import java.io.IOException;
import java.util.Properties;

import ilex.cmdline.*;
import ilex.gui.GuiApp;
import ilex.cmdline.StdAppSettings;
import ilex.util.Logger;
import ilex.util.FileLogger;
import ilex.util.ShellExpander;

/**
  This object parses command line arguments to the DRGS daemon.
*/
public class DrgsCmdLineArgs
	extends StdAppSettings
{
	private StringToken log_arg;
	private StringToken cfg_arg;
	private StringToken lock_arg;
	private StringToken procname_arg;

	/**
	  Constructor: Adds the DRGS-specific options to the StdAppSettings
	  base clase. See parseArgs method for usage.
	*/
    public DrgsCmdLineArgs()
	{
		super(false);

		// -n <procname>
		procname_arg = new StringToken(
			"n", "process-name", "", TokenOptions.optSwitch, "DrgsInput");
		addToken(procname_arg);

		// -l <logfile>
		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, "");
		addToken(log_arg);

		// -f <cfgfile>
		cfg_arg = new StringToken(
			"f", "cfg-file", "", TokenOptions.optSwitch, "drgsconf.xml");
		addToken(cfg_arg);

		// -k <lockfile>
		String lockfile = ShellExpander.expand("~/drgs.lock");
		lock_arg = new StringToken(
			"k", "lock-file", "", TokenOptions.optSwitch, lockfile);
		addToken(lock_arg);
    }

	/** @return the log file name */
	public String getLogFile()
	{
		String s = log_arg.getValue();
		if (s == null || s.length() == 0)
			return null;
		return s;
	}

	/** @return the config file name */
	public String getCfgFile()
	{
		return cfg_arg.getValue();
	}

	/** @return the lock file name */
	public String getLockFile()
	{
		return lock_arg.getValue();
	}

	/** @return the process name */
	public String getProcessName()
	{
		return procname_arg.getValue();
	}

	/**
	  Parses the command line arguments.
	  The super implementation does the parsing. This method then extracts
	  and validates DRGS-specific arguments.
	  <ul>
	    <li>-n procname    (Process name to use in log and LRGS connection)</li>
		<li>-l logfile     (DRGS Log file to write to)</li>
		<li>-f cfgfile     (DRGS configuration file to read)</li>
		<li>-k lockfile    (Lock file ensures only one instance)</li>
	  </ul>
	*/
	public void parseArgs(String args[])
	{
		super.parseArgs(args);

		// If log-file specified, open it.
		String fn = getLogFile();
		if (fn != null && fn.length() > 0)
		{
			String procname = Logger.instance().getProcName();
			try { Logger.setLogger(new FileLogger(procname, fn, getDebugLevel())); }
			catch(IOException e)
			{
				System.err.println("Cannot open log file '" + fn + "': " + e);
				System.exit(1); // TODO: remove this system.exit
			}
		}

	}
}
