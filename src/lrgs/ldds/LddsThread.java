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
package lrgs.ldds;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;

import ilex.net.*;
import ilex.util.Logger;
import ilex.util.QueueLogger;

import lrgs.common.*;
import lrgs.db.DdsConnectionStats;
import lrgs.ddsserver.DdsServer;
import lrgs.ddsserver.MessageArchiveRetriever;


/**
Each connected client will have an LddsThread object.
This is an abstract base class that is missing the methods that create
the interfaces to the archive, retriever, and dcp-name-mapper. 
<p>
For the legacy Linux LRGS, these interfaces use JNI to communicate with
C modules via shared memory and semaphores. For the Java-Only-Archive,
these interfaces are 100% pure Java.
*/
public abstract class LddsThread extends BasicSvrThread
{
	/** The input stream for building protocol messages. */
	protected LddsInputStream ins;

	/** The socket output stream. */
	private OutputStream outs;

	/** Unique number of this connection within this server-run. */
	protected int uniqueID;

	/** Used to map names to dcp addresses. */
	protected DcpNameMapper nameMapper;

	/** The client user: */
	public LddsUser user;

	/** Search criteria being used on this connection. */
	public SearchCriteria crit;

	/** Native interface to retrieve messages. */
	protected DcpMsgSource msgacc;

	/** Interface used to retrieve messages from the local system. */
	protected DcpMsgRetriever msgretriever;

	/** For building get-block responses */
	private byte blockBuffer[];

	/** The number of messages served since last call to retrieveNumServed */
	private int numServed;

	/** The last time a request was received on this connection. */
	private Date lastActivity;

	/** The Queue Logger used for serving out log messages. */
	protected QueueLogger qlog;

	/** My log index. */
	public int logIndex;

	/** Object to provide LRGS status. */
	private LrgsStatusProvider statusProvider;

	/** Last sequence number retrieved, for status reporting. */
	public int lastSeqNum;

	/** Unix time_t of last message retrieved. */
	public int lastMsgTime;

	/** Factory used to convert incoming client messages into commands. */
	protected static CmdFactory cmdFactory = null;

	/** Store various statistics about this connection. */
	public DdsConnectionStats myStats;

	public StatLogger statLogger;

	ArrayList<DcpMsg> seqNumMsgBuf = null;
	int seqNumMsgBufIdx = 0;

	private OutageXmlParser outageXmlParser = null;
	
	private String hostname = "(unknown host)";

	/**
	  Constructor.
	  @param parent the server object
	  @param socket the socket to the client
	  @param id unique integer ID for this client.
	*/
	public LddsThread(BasicServer parent, Socket socket, int id)
		throws IOException
	{
		super(parent, socket);

		ins = new LddsInputStream(socket.getInputStream());
		outs = socket.getOutputStream();
		uniqueID = id;

		user = null;   // Will be created when client sends 'Hello'
		crit = null;   // Will be created on get/put criteria messages
		msgacc = null; // Will be created on HELLO message
		blockBuffer = new byte[99999];
		qlog = null;

		lastActivity = new Date();
		Logger.instance().debug1(DdsServer.module +
			" New client: " + getClientName() + " id=" + id);

		setName("ddsclient-" + id);

		statusProvider = null;
		lastSeqNum = 0;
		lastMsgTime = 0;
		hostname = socket.getInetAddress().toString();
		Logger.instance().debug1(DdsServer.module +
			" set hostname initially to '" + hostname + "'");
			
		myStats = new DdsConnectionStats();
		myStats.setConnectionId(id);
		myStats.setStartTime(new Date());
		myStats.setFromIpAddr(hostname);
		myStats.setSuccessCode(DdsConnectionStats.SC_CONNECTED);
		
		int pri = this.getPriority();
		this.setPriority(pri - 1);
Logger.instance().debug1(DdsServer.module 
+ " enqueing LddsThread to GetHostnameThread priority=" + getPriority());
		GetHostnameThread.instance().enqueue(this);
Logger.instance().debug1(DdsServer.module 
+ " enqueue done.");
	}

	/**
	 * Sets the command factory, used to convert client messages into commands.
	 * @param factory the factory\
	 */
	public static void setCmdFactory(CmdFactory factory)
	{
		cmdFactory = factory;
	}

	/** 
	  Sets the Queue Logger allowing clients to retrieve log messages. 
	  @param qlog the logger
	*/
	public void setQueueLogger(QueueLogger qlog)
	{
		this.qlog = qlog;
		logIndex = qlog.getNextIdx();
	}
	
	/**
	  Called when the hello message is received.
	  @param user the LddsUser object containing name and context
	*/
	public void attachLrgs(LddsUser user) 
		throws ArchiveUnavailableException, DdsInternalException
	{
		if (msgacc != null)
		{
			msgacc.detachSource();
			msgacc = null;
		}
		this.user = user;
		if (user.isAuthenticated)
		{
			myStats.setSuccessCode(myStats.SC_AUTHENTICATED);
			statLogger.incrNumAuth();
		}
		else
		{
			myStats.setSuccessCode(myStats.SC_UNAUTHENTICATED);
			statLogger.incrNumUnAuth();
		}
		myStats.setUserName(user.name);

		// The name mapper maps DCP names in a search crit to addresses
		if (nameMapper == null)
			nameMapper = makeDcpNameMapper();

		// the DcpMsgSource provides the actual interface to the archive.
		msgacc = makeDcpMsgSource();
		msgacc.setClientName(hostname+"-"+uniqueID);
		msgacc.attachSource();
		msgacc.setProcInfo("DDS-CLI", user.name);

		// Set up the msgacc interface to save the last index after
		// each read. This is necessary so that the search criteria
		// "LRGS_SINCE: last" will work.
		msgacc.setSaveLast(user.directory.getPath()
			+File.separator+"lrgslastindex", 
			DcpMsgSource.SaveLastOnGetIndex);

		// The message retriever handles search criteria evaluation.
		msgretriever = makeDcpMsgRetriever();
		msgretriever.setDcpMsgSource(msgacc);
		msgretriever.setUsername(user.name);
		
		msgretriever.setUserSandbox(user.directory);
		msgretriever.setDcpNameMapper(nameMapper);
		((MessageArchiveRetriever)msgretriever).setProtocolVersion(
			user.getClientDdsVersion());
		try { msgretriever.init(); }
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new DdsInternalException("msgretriever.init(): " + ex);
		}

		msgacc.setStatus("Running");
		Logger.instance().debug1(DdsServer.module +
			" Accepted connection #" + getUniqueID() + " from " 
			+ getClientName() + "");
		lastActivity = new Date();
		statLogger.logStat(this);
	}

	/**
	  Template method to create the DcpMsgSource object.
	*/
	protected abstract DcpMsgSource makeDcpMsgSource()
		throws ArchiveUnavailableException;

	/**
	  Template method to create the DcpMsgRetriever object.
	*/
	protected abstract DcpMsgRetriever makeDcpMsgRetriever()
		throws DdsInternalException;

	/**
	  Template method to create the name mapper.
	  This version in the base class uses the native shared memory interface.
	*/
	protected abstract DcpNameMapper makeDcpNameMapper();

	/**
	  Disconnects from this client.
	*/
	public void disconnect()
	{
		Logger.instance().debug1(DdsServer.module + " Connection #"
			+ getUniqueID() + " " + getClientName() + " disconnecting");
		myStats.setEndTime(new Date());
		if (myStats.getSuccessCode() == myStats.SC_CONNECTED)
			myStats.setSuccessCode(myStats.SC_HANGUP);

		try
		{
			outs.close();
			ins.close();
		}
		catch(IOException ioe) {}

		// This will cause thread to terminate & rm this from the list.
		super.disconnect();

		// Log the stats now that we are no longer in the list.
		statLogger.logStat(this);

		if (msgacc != null)
		{
			msgacc.detachSource();
			msgacc = null;
		}
		user = null;
		crit = null;
		msgretriever = null;
		Logger.instance().debug1(DdsServer.module + " Connection #"
			+ getUniqueID() + " disconnection complete.");
	}

	/**
	  Called continually from BasicSvrThread:
		- Block waiting for complete request from client
		- Execute request when one is received.
	*/
	protected void serviceClient()
	{
		LddsCommand cmd;
		LddsMessage msg = null;
		long serviceStart = System.currentTimeMillis();
		try
		{
			msg = ins.getMessage();
			if (cmdFactory == null)
				cmdFactory = new CmdFactory();
			cmd = cmdFactory.makeCommand(msg);
		}
		catch(IOException ex)
		{
			Logger.instance().debug1(DdsServer.module 
				+ " IO Error on connection to " 
				+ getClientName() + " (Disconnecting): " + ex.toString());
			disconnect();
			return;
		}
		catch(ProtocolError ex)
		{
			Logger.instance().info(DdsServer.module 
				+ " Protocol error on connection to " + getClientName()
				+ "(" + ex.getMessage() 
				+ ") -- Hanging up. "
				+ "Ask this user if they are using DDS-compliant software.");
			disconnect();
			return;
		}
		catch(Exception ex)
		{
			String emsg = "Unexpected Error on connection to " + getClientName()
				+ " (Disconnecting): " + ex;
			System.err.println(emsg);
			ex.printStackTrace(System.err);
			warning(emsg);
			disconnect();

			return;
		}

		lastActivity = new Date();

		if (cmd != null)
		{
			try 
			{
				int n = cmd.execute(this);
				if (n > 0)
					myStats.addMsgsReceived(n);
				numServed += n;
			}
			catch(ArchiveException aex)
			{
				String rs = "?" + aex.getErrorCode() + ",0," + aex.getMessage();
				if (!(aex instanceof UntilReachedException))
					Logger.instance().debug3(DdsServer.module
						+ " ArchiveException on "
						+ getClientName() + " Response='" + rs + "' : " + aex);
				LddsMessage resp = new LddsMessage(cmd.getCommandCode(), rs);
				try { send(resp); }
				catch(IOException ioex)
				{
					Logger.instance().warning(DdsServer.module
						+ "Cannot return response to "
						+ getClientName() + ": " + ioex);
					aex.setHangup(true);
				}
				if (aex.getHangup())
					disconnect();
			}
			catch(IOException ex)
			{
				long elapsed = System.currentTimeMillis() - serviceStart;
				String emsg = DdsServer.module 
					+ " Client hangup on connection with user '"
					+ getClientName() + "', elapsed msec=" + elapsed
					+ ": " + ex.toString();
				Logger.instance().warning(emsg);
				disconnect();
			}
			catch(Exception ex)
			{
				long elapsed = System.currentTimeMillis() - serviceStart;
				String emsg = DdsServer.module +
					" Unexpected Exception on connection with user '"
					+ getClientName() + "', elapsed msec=" + elapsed
					+ ": " + ex.toString();
				System.err.println(emsg);
				ex.printStackTrace(System.err);
				Logger.instance().warning(emsg);
				disconnect();
			}
		}
		else // (cmd == null)
		{
			String resptext = "?" + LrgsErrorCode.DBADKEYWORD + 
				",0,Unrecognized request ID '" + msg.MsgId + "'";
			Logger.instance().warning(DdsServer.module +
				" client " + getClientName() + " unrecognized request ID "
				+ msg.MsgId + " resp='" + resptext + "' -- will hangup.");
			LddsMessage resp = new LddsMessage(LddsMessage.IdHello, resptext);
			try { send(resp); }
			catch(IOException ex)
			{
				Logger.instance().warning(DdsServer.module + " "
					+ getClientName() + ": " + ex);
			}
			disconnect();
		}
	}

	/**
	  Send a message to the client.
	  @param msg the message to send
	*/
	public void send(LddsMessage msg) 
		throws IOException
	{
		byte msgbytes[] = msg.getBytes();
		try
		{
			outs.write(msgbytes);
			outs.flush();
		}
		catch(Exception ex)
		{
			throw new IOException("Error sending data: " + ex);
		}
	}

	/** @return the name of the remote user. */
	public String getUserName()
	{
		return user != null ? user.name : "(unknown)";
	}

	/** @return the DDS Version number of the client connected to this server. */
	public int getClientDdsVersion()
	{
		return user != null ? user.getClientDdsVersionNum() : DdsVersion.DdsVersionNum;
	}

	public void setHostName(String hostname)
	{
		this.hostname = hostname;
	}
	
	/** @return the hostname for remote user. */
	public String getHostName()
	{
		return hostname;
	}
	
	/** @return a string of the form username@hostname */
	public String getClientName()
	{
		StringBuilder ret = 
			new StringBuilder(user != null ? user.name : "(unknown)");
		ret.append("@" + hostname);
		ret.append("(id=" + uniqueID + ")");
		return ret.toString();
	}

	/** @return internal block buffer */
	public byte[] getBlockBuffer() { return blockBuffer; }

	/** @return number of DCP messages served to this client connection */
	public synchronized int retrieveNumServed()
	{
		int ret = numServed;
		numServed = 0;
		return ret;
	}

	/** return time of last activity on this connection */
	public Date getLastActivity()
	{
		return lastActivity;
	}

	/** return unique ID assigned to this connection */
	public int getUniqueID()
	{
		return uniqueID;
	}

	/** @return the status provider */
	public LrgsStatusProvider getStatusProvider()
	{
		return statusProvider;
	}

	/**
	 * Sets the status provider.
	 * @param sp the status provider.
	 */
	public void setStatusProvider(LrgsStatusProvider sp)
	{
		statusProvider = sp;
	}

	/**
	 * @return true if authentication is required by this server.
	 */
	public abstract boolean isAuthRequired();

	/**
	 * @return true if same user is allowed multiple connections.
	*/
	public abstract boolean isSameUserMultAttachOK();

//	/**
//	 * @return the root directory for user sandbox directories.
//	 */
//	public abstract String getDdsUserRootDir();

	/**
	 * Convenience method to issue a warning log message on behalf of this
	 * client connection.
	 */
	public void warning(String msg)
	{
		Logger.instance().warning("DDS Client " + getClientName()
			+ " " + msg);
	}

	public OutageXmlParser getOutageXmlParser()
	{
		if (outageXmlParser == null)
			outageXmlParser = new OutageXmlParser();
		return outageXmlParser;
	}
	
	public boolean isLocal()
	{
		if (this.user == null)
			return false;
		return user.isLocal();
	}

}
