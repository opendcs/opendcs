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
package lrgs.ddsserver;

import java.net.*;
import java.io.*;
import java.util.Iterator;

import ilex.util.*;
import ilex.net.*;
import lrgs.apistatus.AttachedProcess;
import lrgs.common.ArchiveUnavailableException;
import lrgs.archive.MsgArchive;
import lrgs.common.NetlistDcpNameMapper;
import lrgs.ldds.GetHostnameThread;
import lrgs.ldds.LddsLoggerThread;
import lrgs.ldds.LddsParams;
import lrgs.ldds.LddsThread;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.JavaLrgsStatusProvider;


/**
Main class for the LRGS DDS server that uses the Java-Only-Archive.
*/
public class DdsServer extends BasicServer 
	implements Runnable
{
	public static final String module = "DdsSvr";

	/** Event number meaning server disabled */
	public static final int EVT_SVR_DISABLED = 1;

	/** Event number meaning can't make client */
	public static final int EVT_INTERNAL_CLIENT = 2;

	/** Event number meaning we already have max clients. */
	public static final int EVT_MAX_CLIENTS = 3;

	/** Event number meaning error on listening socket. */
	public static final int EVT_LISTEN = 4;

	/** Control switch used by main to enable/disable the server. */
	private boolean enabled;

	/** Logs statistics for each client-thread each minute: */
	public static LddsLoggerThread statLoggerThread;

	/** Internal shutdown flag */
	boolean shutdownFlag;

	private String status;

	/** Passed from parent, allows clients to retrieve event messages from Q */
	private QueueLogger qlog;

	/** The archive object from which to serve dcp messages. */
	public MsgArchive msgArchive;

	/** Global mapper for DCP names. */
	NetlistDcpNameMapper globalMapper;

	/** Provides status to clients. */
	public JavaLrgsStatusProvider statusProvider;

	/** Server uses a FileCounter to assign unique ID to each connection. */
	public static final String counterName = "$LRGSHOME/dds-counter";

	private Counter conIdCounter;

	/**
	  Constructor.
	  @param port the listening port.
	  @param bindaddr indicates the NIC to listen on if there are more than one.
	  @param archive the MsgArchive object to serve data from.
	  @param qlog the QueueLogger to serve event messages from.
	  @throws IOException on invalid port number or socket already bound.
	*/
	public DdsServer(int port, InetAddress bindaddr, MsgArchive msgArchive,
		QueueLogger qlog, JavaLrgsStatusProvider statusProvider)
		throws IOException
	{
		super(port, bindaddr);
		enabled = false;
		shutdownFlag = false;
		this.msgArchive = msgArchive;
		this.qlog = qlog;
		globalMapper = new NetlistDcpNameMapper(
			new File(EnvExpander.expand(LrgsConfig.instance().ddsNetlistDir)),
			null);
		this.statusProvider = statusProvider;
		conIdCounter = null;
		setModuleName(module);
	}

	/**
	  Used by main as a switch to coordinate archive and DDS.
	  When disabled, hangup on all clients and don't accept new ones.
	  @param tf true if enabling the DDS server.
	*/
	public void setEnabled(boolean tf) 
	{
		if (tf != enabled)
		{
			if (enabled)
			{
				killAllSvrThreads();
			}
			enabled = tf;
		}
	}

	/** Initializes the application. */
	public void init()
		throws ArchiveUnavailableException
	{
		status = "R";
		BackgroundStuff bs = new BackgroundStuff(this);
		bs.start();
		statLoggerThread = new LddsLoggerThread(this, 
			LrgsConfig.instance().ddsUsageLog);
		statLoggerThread.start();
		Logger.instance().debug1("Starting DDS Listening thread.");
		GetHostnameThread.instance().start();
		Thread listenThread = new Thread(this);
		listenThread.start();
		try { conIdCounter = new FileCounter(EnvExpander.expand(counterName));}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot create File-conIdCounter: " + ex);
			conIdCounter = new SimpleCounter(1);
		}
	}

	/** Updates the status in the LRGS client slot. */
	public void updateStatus()
	{
		switch(status.length())
		{
		case 0:
		case 10:
			    status = "R"; break;
		case 1: status = "Ru"; break;
		case 2: status = "Run"; break;
		case 3: status = "Runn"; break;
		case 4: status = "Runni"; break;
		case 5: status = "Runnin"; break;
		case 6: status = "Running"; break;
		case 7: status = "Running-"; break;
		case 8: status = "Running--"; break;
		case 9: status = "Running---"; break;
		}
	}

	/** Shuts the application down. */
	public void shutdown()
	{
		Logger.instance().info(module + " shutting down.");
		shutdownFlag = true;
		statLoggerThread.shutdown();
		setEnabled(false);
		super.shutdown();
	}

	/**
	  Overloaded from BasicServer, this constructs a new LddsThread
	  to handle this client connection.
	  @param sock the client connection socket
	*/
	protected BasicSvrThread newSvrThread(Socket sock) 
		throws IOException
	{
		try
		{
//			sock.setTcpNoDelay(true);
//			sock.setSoTimeout(LddsParams.ServerHangupSeconds * 1000);

// Experimenting with this.
// Setting linger to 0 will tell the server to immediatly drop connections on
// close without the normal ack/nak tcp mechanism.
//			sock.setSoLinger(true, 0);

			Logger.instance().debug1(module + " New DDS client. "
				+ " KeepAlive=" + sock.getKeepAlive()
				+ " SoLinger=" + sock.getSoLinger()
				+ " SoTimeout=" + sock.getSoTimeout()
				+ " TcpNoDelay=" + sock.getTcpNoDelay()
				+ " ReuseAddress=" + sock.getReuseAddress());
		}
		catch(Exception ex)
		{
			Logger.instance().warning(module 
				+ " Exception setting or printing socket options: " + ex);
		}

		if (!enabled)
		{
			Logger.instance().warning(module + ":" + EVT_SVR_DISABLED
				+ "- Cannot accept new client, Server disabled.");
			sock.close();
			return null;
		}

		int numcli = getNumSvrThreads();
		int maxcli = LrgsConfig.instance().ddsMaxClients;
		if (maxcli > 0 && numcli >= maxcli)
		{
			Logger.instance().warning(module + ":" + EVT_MAX_CLIENTS
				+ " Cannot accept new client, already have max of "
				+ maxcli + " connected.");
			sock.close();
			return null;
		}

		AttachedProcess ap = statusProvider.getFreeClientSlot();
		if (ap == null)
		{
			Logger.instance().warning(module + ":" + EVT_MAX_CLIENTS
				+ " Cannot get free client data structure, already have max of "
				+ maxcli + " connected.");
			sock.close();
		}
		else
			Logger.instance().debug1(module + ":" + (-EVT_MAX_CLIENTS)
				+ " New client accepted");
 

		try 
		{
			int id = conIdCounter.getNextValue();
			//Work around for when file is bad
			if (id == -1)
			{
				conIdCounter.setNextValue(1);
				Logger.instance().warning(module + ":" + 
						"Re-setting "+
						EnvExpander.expand(counterName) +
						" to 1.");
			}
			id = conIdCounter.getNextValue();
			//End work around
			LddsThread ret = new JLddsThread(this, sock, id, msgArchive,
				globalMapper, ap);
			ret.statLogger = statLoggerThread;
			ret.setQueueLogger(qlog);
			ret.setStatusProvider(statusProvider);
			return ret;
		}
		catch(IOException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			String msg = "- Unexpected exception creating new client thread: " 
				+ ex;
			Logger.instance().failure(module + " " + EVT_INTERNAL_CLIENT
				+ msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return null;
		}
	}

	/** From java.lang.Runnable interface. */
	public void run()
	{
		try
		{
			Logger.instance().info(module
				+ " ServerSocket.getSoTimeout=" + listeningSocket.getSoTimeout());
			Logger.instance().info(module
				+ " ServerSocket.getReceiveBufferSize=" 
					+ listeningSocket.getReceiveBufferSize());
			Logger.instance().info(module
				+ " ServerSocket.getReuseAddress=" 
				+ listeningSocket.getReuseAddress());
		}
		catch(Exception ex)
		{
			Logger.instance().warning(module
				+ " Error getting or setting server socket params: " + ex);
		}
		try { listen(); }
		catch(IOException ex)
		{
			Logger.instance().failure(module + ":" + EVT_LISTEN
				+ "- Error on listening socket: " + ex);
		}
	}

	protected void listenTimeout()
	{
		Logger.instance().info(module + " listen timeout");
	}

//	/** 
//	  Log status for an LddsThread object.
//	  @param lt the thread object
//	*/
//	public void logStat(LddsThread lt)
//	{
//		statLoggerThread.logStat(lt);
//	}
}


/**
  Check configuration to see if server has been disabled.
*/
class BackgroundStuff extends Thread
{
	DdsServer svr;

	BackgroundStuff(DdsServer svr)
	{
		this.svr = svr;
	}

	public void run()
	{
		long lastNetlistCheck = System.currentTimeMillis();

		try { sleep((long)2000); }
		catch (InterruptedException ie) {}
		while(svr.shutdownFlag == false)
		{
			svr.updateStatus();

			// Hangup on clients who have gone catatonic.
			long now = System.currentTimeMillis();
			LddsThread badClient = null;

			synchronized(svr)
			{
				for(@SuppressWarnings("rawtypes")
				Iterator it = svr.getSvrThreads(); it.hasNext(); )
				{
					LddsThread lt = (LddsThread)it.next();
					if ((now - lt.getLastActivity().getTime()) 
						> (LddsParams.ServerHangupSeconds*1000L))
					{
						badClient = lt;
						break;
						// Can't disconnect inside this loop because it will
						// cause a modification to the vector we're iterating!
					}
				}
			}
			if (badClient != null) // Found one to hang up on?
			{
				Logger.instance().debug1(DdsServer.module +
					" Hanging up on client '" + badClient.getClientName()
					+ "' due to inactivity for more than "
					+ LddsParams.ServerHangupSeconds + " seconds.");
				badClient.disconnect();
				badClient = null;
			}

			// Check global network lists for change once per minute.
			if (now - lastNetlistCheck > 60000L)
			{
				lastNetlistCheck = now;
				svr.globalMapper.check();
			}

			try { sleep((long)2000); }
			catch (InterruptedException ie) {}
		}
	}
}

