/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2009/10/09 18:12:01  mjmaloney
*  default to last known state
*
*  Revision 1.2  2009/08/14 14:05:51  shweta
*  Added mandatory parameter ' file sender state'  which needs to be provided to start LRIT.
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/12/30 19:40:59  mmaloney
*  dev
*
*  Revision 1.3  2004/05/24 13:55:05  mjmaloney
*  dev
*
*  Revision 1.2  2004/04/29 16:11:07  mjmaloney
*  Implemented new header fields.
*
*  Revision 1.1  2003/08/06 23:29:24  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.IOException;
import java.io.File;
import java.util.TimeZone;

import ilex.cmdline.*;
import ilex.util.FileLogger;
import ilex.util.Logger;
import ilex.util.StderrLogger;

public class DcsCmdLineArgs extends ApplicationSettings
{
    // Public strings set by command line options:
	private IntegerToken debuglevel_arg;
	private StringToken log_arg;
	private String progname;
	private StringToken state_arg;

	public DcsCmdLineArgs(String progname)
	{
		super();
		this.progname = progname;

		debuglevel_arg = new IntegerToken("d", "debug-level", "",
			TokenOptions.optSwitch, 0);
		addToken(debuglevel_arg);

		String logname = LritDcsConfig.instance().getLritDcsHome()
			+ File.separator + "lritdcs.log";

		log_arg = new StringToken(
			"l", "log-file", "", TokenOptions.optSwitch, logname);

		addToken(log_arg);
		
		state_arg = new StringToken(
				"s", "LRIT-State", "", TokenOptions.optSwitch, "last");
		addToken(state_arg);
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

		//checks if the LRIT is started in active or dormant state
		String lritState = getLritState();		
		LritDcsMain.instance().setLritStartState(lritState);
		
		
		// If log-file specified, open it.
		String fn = getLogFile();
		try 
		{
			Logger.setLogger(new FileLogger(progname, fn, getDebugLevel())); 
		}
		catch(IOException e)
		{
			System.err.println("Cannot open log file '" + fn + "': " + e);
			System.exit(1);
		}		

		Logger.instance().setTimeZone(TimeZone.getTimeZone("UTC"));
		Logger.instance().log(Logger.E_INFORMATION, "LRIT Process '"
			+ progname + "' Starting.....");
		
		
	}
	
	public String  getLritState() {
		
		return state_arg.getValue();
	}

}
