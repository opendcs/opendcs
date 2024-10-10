/*
*  $Id$
*/
package lrgs.lddc;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;

import ilex.util.*;
import ilex.cmdline.*;
import lrgs.common.*;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsParams;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.ServerError;

/**
This was originally written as a test client program but it has since found
use as a command-line utility for pulling data from a DDS server.
Command line arguments are used to specify the server paramters, search
criteria, and various formatting options.
*/
public class GetEvents extends Thread
{
	private String user;
	private LddsClient lddsClient;

	/**
	 * Constructor.
	 * @param host host name to connect to
	 * @param port port number
	 * @param user user name
	 */
	public GetEvents(String host, int port, String user)
		throws Exception
	{
		this.user = user;
		lddsClient = new LddsClient(host, port);
	}

	/** Thread run method. */
	public void run()
	{
		try
		{
			// Connect & login as specified user.
			lddsClient.connect();
			lddsClient.sendHello(user);
		}
		catch(Exception e)
		{
			Logger.instance().log(Logger.E_FATAL, "Cannot initialize: " + e);
			//e.printStackTrace(System.err);
			return;
		}

		// Continue to receive events
		while( true )
		{
			try
			{
				String[] events = lddsClient.getEvents();
				if (events.length == 0)
				{
					try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
				}
				for(int i=0; events != null && i<events.length; i++)
					System.out.println(events[i]);
			}
			catch(Exception e)
			{
				Logger.instance().log(Logger.E_FATAL, e.toString());
				e.printStackTrace(System.err);
			}
		}
	}

	
	// ========================= main ====================================
	static ApplicationSettings settings = new ApplicationSettings();
	static StringToken hostArg = new StringToken(
		"h", "Host", "", TokenOptions.optSwitch, "localhost");
	static IntegerToken portArg= new IntegerToken(
		"p", "Port number", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	static StringToken userArg = new StringToken(
		"u", "User", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static IntegerToken debugArg = new IntegerToken(
		"d", "debug level", "", TokenOptions.optSwitch, 0);
	static StringToken logArg = new StringToken(
		"l", "log-file name", "", TokenOptions.optSwitch, "");

	static
	{
		settings.addToken(hostArg);
		settings.addToken(portArg);
		settings.addToken(userArg );
		settings.addToken(debugArg);
		settings.addToken(logArg);
	}

	/**
	  Main method.
	*/
	public static void main(String args[]) 
	{
		try
		{
			settings.parseArgs(args);
			
			String lf = logArg.getValue();
			if (lf != null && lf.length() > 0)
				Logger.setLogger(new FileLogger("GetDcpMessages", lf));
			int dba = debugArg.getValue();
			if (dba > 0)
				Logger.instance().setMinLogPriority(
					dba == 1 ? Logger.E_DEBUG1 :
					dba == 2 ? Logger.E_DEBUG2 : Logger.E_DEBUG3);

			GetEvents gdm = new GetEvents(
				hostArg.getValue(), portArg.getValue(), userArg.getValue());
			
			gdm.start();
		}
		catch(Exception e)
		{
			System.out.println("Exception while attempting to start gdm: " 
				+ e);
		}
	}
}

