package lrgs.multistat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import ilex.cmdline.*;
import ilex.util.Logger;
import ilex.util.FileLogger;
import ilex.util.EnvExpander;

public class MultiStatCmdLineArgs
	extends StdAppSettings
{
	private StringToken configFileArg = new StringToken(
		"f", "config-file", "", TokenOptions.optSwitch, "$DCSTOOL_HOME/multistat.conf");

    public MultiStatCmdLineArgs()
	{
		super(false);
		addToken(configFileArg);
    }

	/**
	  Parses command line arguments.
	*/
	public void parseArgs(String args[])
	{
		try { super.parseArgs(args); }
		catch (IllegalArgumentException ex) 
		{
			System.err.println("Illegal arguments ... program exiting.");
			System.exit(1);
		}
		// Set debug level.
		int dl = getDebugLevel();
		if (dl > 0)
			Logger.instance().setMinLogPriority(
				dl == 1 ? Logger.E_DEBUG1 :
				dl == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Logger.setDateFormat(df);
	}
	
	public String getConfigFileName()
	{
		return configFileArg.getValue();
	}
}
