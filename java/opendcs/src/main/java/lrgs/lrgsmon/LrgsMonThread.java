/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/09/05 13:17:23  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/02/15 01:22:34  mmaloney
*  Added ?arg feature to support new real-time status GUI.
*
*  Revision 1.3  2004/06/04 17:32:16  mjmaloney
*  Completed LRGSmon application.
*
*  Revision 1.2  2004/06/02 21:31:51  mjmaloney
*  dev
*
*  Revision 1.1  2004/06/01 15:26:36  mjmaloney
*  Created.
*
*/
package lrgs.lrgsmon;

import java.io.*;

import org.xml.sax.SAXException;

import ilex.util.Logger;
import lrgs.ldds.DdsVersion;
import lrgs.ldds.LddsClient;
import lrgs.ldds.ServerError;
import lrgs.ldds.ProtocolError;
import lrgs.statusxml.*;

/**
This thread handles a connection to a single DDS server.
*/
public class LrgsMonThread
	extends ThreadBase
{
	/** controlling main object */
	private LrgsMonitor parent;

	/** Host name to connect to. */
	private String host;

	/** User number to sign on as */
	private String user;

	/** If non-null, use authenticated connection with this password */
	private String passwd;

	/** External hostname to include as argument in URL. */
	private String extHost;

	/** Connection to the DDS server */
	private LddsClient lddsClient;

	/** Last time attempt was made to poll status. */
	private long lastStatusPoll = 0L;

	/** Last time connection attempt to remote server was made. */
	private long lastConnectAttempt;

	/** XML parser to parse incoming status messages from the DDS servers. */
	lrgs.statusxml.TopLevelXio statusXio;

	public LrgsMonThread(LrgsMonitor lm, String host, int port, 
		String user, String passwd, String extHost)
	{
		super("LrgsMon(" + host + ")");
		parent = lm;
		this.host = host;
		this.user = user;
		this.passwd = passwd;
		this.extHost = extHost;
		lddsClient = new LddsClient(host, port);
		try { statusXio = new lrgs.statusxml.TopLevelXio(); }
		catch(Exception ex)
		{
			String msg = "Internal Error: " + ex;
			fatal(msg);
			System.err.println(msg);
			System.exit(1);
		}
	}

	public void run()
	{
		shutdownFlag = false;

		while(!shutdownFlag)
		{
			try {sleep(1000L); }
			catch(InterruptedException ex) {}

			// If not connected, retry once per minute.
			long now = System.currentTimeMillis();
			if (!lddsClient.isConnected())
			{
				if (now - lastConnectAttempt > 60000L)
				{
					lastConnectAttempt = now;
					tryConnect();
				}
			}
			else if (now - lastStatusPoll >= (parent.getScanSeconds()*1000L))
			{
				lastStatusPoll = now;
				pollStatus();
			}
		}
		if (lddsClient.isConnected())
			lddsClient.disconnect();
	}

	/// Attempt connection & authentication
	private void tryConnect()
	{
		try
		{
			lddsClient.connect();
		}
		catch(Exception ex)
		{
			warning("Cannot connect to " + lddsClient.getName() + ": " + ex);
			return;
		}

		info("Connected to " + lddsClient.getName() + ", my client protocol version="
			+ DdsVersion.DdsVersionNum);

		try
		{
			if (passwd == null)
				lddsClient.sendHello(user);
			else
				lddsClient.sendAuthHello(user, passwd);
		}
		catch(ServerError ex)
		{
			warning("ServerError logging in to " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
			return;
		}
		catch(ProtocolError ex)
		{
			warning("Unexpected response to login from " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
			return;
		}
		catch(IOException ex)
		{
			warning("IOException logging in to " + lddsClient.getName() 
				+ ": " + ex);
			lddsClient.disconnect();
			return;
		}

		if (lddsClient.getServerProtoVersion() < 6)
		{
			warning("DDS Server at " + lddsClient.getName()
				+ " is DDS protocol version " + lddsClient.getServerProtoVersion()
				+ ". This application requires version 6 or later!");
			warning("Please upgrade the LRGS at " + lddsClient.getHost() + ".");
			lastStatusPoll = System.currentTimeMillis() + 60 * 60000L;
			lddsClient.disconnect();
			return;
		}
	}

	/**
	  Polls this LRGS for status, constructs status structure, and passes
	  it to the parent for inclusion in the HTML output.
	*/
	private void pollStatus()
	{
		debug1("Polling " + host);
		try
		{
			byte[] statmsg = lddsClient.getStatus();
			if (Logger.instance().getMinLogPriority() <= Logger.E_DEBUG2)
			{
				try
				{
					FileOutputStream fos = new FileOutputStream("LastStatusSnap.xml");
					fos.write(statmsg);
					fos.close();
				}
				catch(IOException ex){}
			}
			LrgsStatusSnapshotExt status = 
				statusXio.parse(statmsg, 0, statmsg.length, host);
			parent.updateStatus(host, status, extHost);
		}
		catch(ServerError ex)
		{
			warning("ServerError on getStatus from " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
		}
		catch(ProtocolError ex)
		{
			warning("Unexpected response to getStatus " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
		}
		catch(IOException ex)
		{
			warning("IOException on getStatus from " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
		}
		catch(SAXException ex)
		{
			warning("Error parsing status from " + lddsClient.getName()
				+ ": " + ex);
			lddsClient.disconnect();
		}
	}
}
