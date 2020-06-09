/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:27  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/03/31 14:28:03  mjmaloney
*  Added new test.
*
*/
package ilex.util;

import java.io.IOException;
import ilex.cmdline.*;


/**
Test class for file logger.
*/
public class FileLoggerTest
{
	static ApplicationSettings settings = new ApplicationSettings();
	static StringToken log_arg = new StringToken("l","log-file","",TokenOptions.optSwitch,"logtest.log");
	static IntegerToken debuglevel_arg = new IntegerToken("d","debug-level","",TokenOptions.optSwitch,0);
	static
	{
		settings.addToken(log_arg);
		settings.addToken(debuglevel_arg);
	}

	/**
	* @param args
	*/
	public static void main( String[] args )
	{
		settings.parseArgs(args);

		String fn = log_arg.getValue();
		if (fn != null && fn.length() > 0)
		{
			String procname = Logger.instance().getProcName();
			try { Logger.setLogger(new FileLogger(procname, fn)); }
			catch(IOException ex)
			{
				System.err.println("Cannot open log file '" + fn + "': " + ex);
				System.exit(1);
			}
		}

		// Set debug level.
		int dl = debuglevel_arg.getValue();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);

		Logger.instance().log(Logger.E_DEBUG3,
			"This is a debug level 3 message");
		Logger.instance().log(Logger.E_DEBUG2,
			"This is a debug level 2 message");
		Logger.instance().log(Logger.E_DEBUG1,
			"This is a debug level 1 message");
		Logger.instance().log(Logger.E_INFORMATION,
			"This is an information message");
		Logger.instance().log(Logger.E_WARNING,
			"This is a warning message");
		Logger.instance().log(Logger.E_FAILURE,
			"This is a failure message");
		Logger.instance().log(Logger.E_FATAL,
			"This is a fatal message");
	}
}
