/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.3  2008/11/20 18:49:45  mjmaloney
*  merge from usgs mods
*
*  Revision 1.2  2008/09/05 13:14:14  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2003/09/18 14:22:50  mjmaloney
*  Created.
*
*
*/
package lrgs.lddc;

import java.io.*;

import ilex.util.*;
import ilex.net.*;
import ilex.cmdline.*;
import lrgs.common.*;
import lrgs.ldds.LddsParams;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.ServerError;

public class UsbrDrotFeed extends GetDcpMessages
{
	private String drotFeedSvr;
	private int drotFeedPort;
	private BasicClient drotFeed;
	public static final int pauseTime = 10000; // 10 sec.
	private boolean firstConnect = true;

	UsbrDrotFeed(String ddsHost, int ddsPort, String ddsUser, String crit, 
		String usbrHost, int usbrPort)
		throws Exception
	{
		// no before/after strings, no verbose, no newline.
		super(ddsHost, ddsPort, ddsUser, crit, false, "", "", false, false);
		timeout = 3600;
		setSingleMode(true);
		Logger.instance().log(Logger.E_INFORMATION,
			"Constructing client to " + usbrHost + ":" + usbrPort);
		drotFeed = new BasicClient(usbrHost, usbrPort);
		String passwd = passwordArg.getValue();
		if (passwd != null && passwd.length() > 0)
			setPassword(passwd);
	}

	/**
	  The program should never exit. If the super.run() method exits,
	  it means that the DDS session has failed. We just pause here &
	  retry later.
	*/
	public void run()
	{
		while(true)
		{
			super.run();
			Logger.instance().log(Logger.E_WARNING,
				"DDS session failed, pause before retry...");
			try { sleep(pauseTime); }
			catch(InterruptedException ex) {}
		}
	}

	protected void waitForAllClear()
	{
		while(true)
		{
			checkConnection();
			try
			{
				InputStream is = drotFeed.getInputStream();
				int c;
				while((c = is.read()) != 0);
				return;
			}
			catch(IOException ex)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Failed to get all-clear from "
					+ "server at " + drotFeed.getHost() + ":" 
					+ drotFeed.getPort());
				drotFeed.disconnect();
			}
		}
	}

	protected void checkConnection()
	{
		while(!drotFeed.isConnected())
		{
			if (!firstConnect)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Pausing before attempt to reconnect to USBR "
					+ "server at " + drotFeed.getHost() + ":" 
					+ drotFeed.getPort());
				try { sleep(10000L); }
				catch(InterruptedException  ex) {}
			}
			firstConnect = false;
System.out.println("Trying connect()");
			try { drotFeed.connect(); }
			catch(IOException ex)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Error connecting to server at " 
					+ drotFeed.getHost() + ":" 
					+ drotFeed.getPort() + ": " + ex);
			}
		}
	}

	protected void outputMessage(DcpMsg msg)
	{
Logger.instance().log(Logger.E_DEBUG1, "UsbrDrotFeed.outputMessage");
		waitForAllClear();
				
		try 
		{
			drotFeed.sendData(msg.getData());
			drotFeed.getOutputStream().flush();
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
					"Error on server at " + drotFeed.getHost() + ":" 
				+ drotFeed.getPort() + ": " + ex);
			drotFeed.disconnect();
		}
	}

	// ========================= main ====================================
	/**
	  Usage: UsbrDrotFeed -p port -h host -u user -f searchcrit 

		... where:
			-p port defaults to 16003.
			-h host defaults to localhost.
			-u user is required.
			-f searchcrit specifies a search criteria file to be downloaded
			   before starting the transfer.
			-t timeout    # seconds to wait for response from server
			-d 0123       set debug level to 0, 1, 2, or 3
			-l logfile    Set name of debug log file (default=stderr)
			-H USBRHost  Host to receive the data.
			-P USBRPoret Port number of USBR service
	*/
	static ApplicationSettings usbrSettings = new ApplicationSettings();
	static StringToken ddsHostArg = new StringToken(
		"h", "DDSHost", "", TokenOptions.optSwitch, "localhost");
	static IntegerToken ddsPortArg= new IntegerToken(
		"p", "DDSPort", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	static StringToken ddsUserArg = new StringToken(
		"u", "DDSUser", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static StringToken searchcritArg = new StringToken(
		"f", "SearchCritFile", "", TokenOptions.optSwitch, "");
	static IntegerToken timeoutArg = new IntegerToken(
		"t", "timeout seconds", "", TokenOptions.optSwitch, 3600);
	static IntegerToken debugArg = new IntegerToken(
		"d", "debug level", "", TokenOptions.optSwitch, 0);
	static StringToken logArg = new StringToken(
		"l", "log-file name", "", TokenOptions.optSwitch, "");
	static StringToken usbrHostArg = new StringToken(
		"H", "USBR Host", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static IntegerToken usbrPortArg = new IntegerToken(
		"P", "USBR Port", "", TokenOptions.optSwitch | TokenOptions.optRequired, 0);

	static
	{
		usbrSettings.addToken(ddsHostArg);
		usbrSettings.addToken(ddsPortArg);
		usbrSettings.addToken(ddsUserArg );
		usbrSettings.addToken(searchcritArg );
		usbrSettings.addToken(timeoutArg );
		usbrSettings.addToken(debugArg);
		usbrSettings.addToken(logArg);
		usbrSettings.addToken(usbrHostArg);
		usbrSettings.addToken(usbrPortArg);
		passwordArg = new StringToken(
			"w", "Password for auth connect", "", TokenOptions.optSwitch, "");
		usbrSettings.addToken(passwordArg); //Inherited from GetDcpMessages
	}

	public static void main(String args[]) 
	{
		try
		{
			usbrSettings.parseArgs(args);
			
			String lf = logArg.getValue();
			if (lf != null && lf.length() > 0)
			{
				Logger.setLogger(new FileLogger("drotfeed.err", lf, debugArg.getValue()));
			}

			String crit = searchcritArg.getValue();
			if (crit != null && crit.length() == 0)
				crit = null;

			UsbrDrotFeed me = new UsbrDrotFeed(ddsHostArg.getValue(),
				ddsPortArg.getValue(), ddsUserArg.getValue(), crit, 
				usbrHostArg.getValue(), usbrPortArg.getValue());

			me.setTimeout(timeoutArg.getValue());
			me.setSingleMode(true);

			me.start();
		}
		catch(Exception e)
		{
			System.out.println("Exception while attempting to start client: " 
				+ e);
		}
	}
}
