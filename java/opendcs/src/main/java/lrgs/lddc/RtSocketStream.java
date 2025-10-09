/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.lddc;

import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;
import ilex.net.*;
import ilex.util.AsciiUtil;
import ilex.util.FileServerLock;
import ilex.util.ServerLock;
import ilex.util.ServerLockable;
import lrgs.common.*;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsParams;
import lrgs.ldds.ProtocolError;
import lrgs.ldds.ServerError;

public class RtSocketStream extends Thread implements ServerLockable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String user;
	private String crit;
	private LddsClient client;
	private boolean verbose, newline;
	private byte[] before;
	private byte[] after;
	private String passwd;

	private int total;
	private long lastmsgtime;
	int timeout;
	boolean singleMode;
	static final byte nlbytes[] = { (byte)'\n' };
	SearchCriteria mysc;
	private ServerLock myServerLock;
	private String lockFileName = null;

	RtSocketStreamServer mySvr;
	private boolean done = false;

	RtSocketStream(String host, int port, String user, String crit,
		boolean verbose, String beforeAscii, String afterAscii,
		boolean newline, int svrPort, String lockFileName)
		throws Exception
	{
		this.user = user;
		this.crit = crit;
		this.verbose = verbose;
		this.newline = newline;
		if (beforeAscii == null)
			before = null;
		else
			before = AsciiUtil.ascii2bin(beforeAscii);

		if (afterAscii == null)
			after = null;
		else
			after = AsciiUtil.ascii2bin(afterAscii);

		client = new LddsClient(host, port);
		if (verbose)
			client.setDebugStream(System.err);
		total = 0;
		timeout = 120;
		this.passwd = null;
		singleMode = false;

		mySvr = new RtSocketStreamServer(svrPort);
		mysc = null;
		if (lockFileName != null && lockFileName.length() > 0)
		{
			this.lockFileName = lockFileName;
		}
	}

	public void setPassword(String pw)
	{
		this.passwd = pw;
	}

	public void run()
	{
		if (lockFileName != null)
		{
			myServerLock = new FileServerLock(lockFileName);
			if (!myServerLock.obtainLock(this))
			{
				log.error("Lock file '{}' already taken. Another instance of this process already running?",
						  lockFileName);
				return;
			}
		}

		// If I successfully connect, update searchcrit to since last
		if (connect())
		{
			if (crit == null)
				mysc = new SearchCriteria();
			else
			{
				try { mysc = new SearchCriteria(new File(crit)); }
				catch(Exception ex)
				{
					log.atError().setCause(ex).log("Cannot make searchcrit from file '{}'", crit);
					mysc = new SearchCriteria();
				}
			}
			mysc.setLrgsSince("last");
		}

		Thread listenThread =
			new Thread()
			{
				public void run()
				{
					try { mySvr.listen(); }
					catch(IOException ex)
					{
						log.atError().setCause(ex).log("Cannot listen at port {}", mySvr.getPort());
						System.exit(1);
					}
				}
			};
		listenThread.start();

		// Continue to receive messages until I get one past start.
		done = false;
		boolean goodbye = true;
		long endTime = System.currentTimeMillis() + (timeout*1000L);
		while( !done )
		{
			try
			{
				if (!client.isConnected())
				{
					Thread.sleep(10000L); // pause 10 sec.
					connect();
				}
				else if (singleMode)
				{
					DcpMsg msg = client.getDcpMsg(timeout);
					if (msg != null)
					{
						outputMessage(msg);
						endTime = System.currentTimeMillis() + (timeout*1000L);
					}
					else
					{
						throw new ProtocolError(
							"Timeout waiting for response from server.");
					}
				}
				else // get multiple message blocks
				{
					DcpMsg msgs[] = client.getDcpMsgBlock(timeout);
					if (msgs != null)
					{
						for(int i=0; i<msgs.length; i++)
							outputMessage(msgs[i]);
						endTime = System.currentTimeMillis() + (timeout*1000L);
					}
					else
					{
						throw new ProtocolError(
							"Timeout waiting for block response from server.");
					}
				}
			}
			catch(ServerError se)
			{
				if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT)
				{
					if (System.currentTimeMillis() > endTime)
					{
						log.atError().setCause(se).log("No message received in {} seconds, exiting.", timeout);
						done = true;
					}
					else
					{
						log.atDebug().setCause(se).log("Server caught up to present, pausing...");
						try { Thread.sleep(1000L); }
						catch(InterruptedException ie) {}
						continue;
					}
				}
				if (se.Derrno == LrgsErrorCode.DUNTIL
				 || se.Derrno == LrgsErrorCode.DUNTILDRS)
					log.info("Until time reached. Normal termination");
				else
					log.atError().setCause(se).log("Abnormal Server error.");
				done = true;
			}
			catch(ProtocolError ex)
			{
				log.atError().setCause(ex).log("Protocol Error (will reconnect to DDS).");
				client.disconnect();
			}
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Unexpected error.");
				goodbye = false;
				done = true;
			}
		}

		// all done...
		try
		{
			if (goodbye)
				client.sendGoodbye();
			client.disconnect();
			if (mySvr != null)
				mySvr.shutdown();
			sleep(2000L);
		}
		catch(Exception e){}
		System.exit(0);
	}

	private void outputMessage(DcpMsg msg)
	{
		if (before != null)
			mySvr.writeToClients(before);
		mySvr.writeToClients(msg.getData());
		if (after != null)
			mySvr.writeToClients(after);
		if (newline)
			mySvr.writeToClients(nlbytes);
	}

	private boolean connect()
	{
		try
		{
			// Connect & login as specified user.
			client.connect();
			if (passwd != null)
				client.sendAuthHello(user, passwd);
			else
				client.sendHello(user);

			// MsgBlock retrieval only supported in protoversion 4 and higher
			if (client.getServerProtoVersion() < 4)
				singleMode = true;

			// Send search crit
			if (mysc != null)
				client.sendSearchCrit(mysc);
			else if (crit != null)
				client.sendSearchCrit(crit);

			return true;
		}
		catch(ServerError | ProtocolError | IOException ex)
		{
			log.atError().setCause(ex).log("Cannot connect to '{}'", client.getName());
			return false;
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Cannot initialize: exiting");
			System.exit(1);
		}
		return true;
	}

	// ========================= main ====================================
	/**
	  Usage: RtSocketStream options
		Options:
			-p port defaults to 16003.
			-h host defaults to localhost.
			-u user is required.
			-f searchcrit specifies a search criteria file to be downloaded
			   before starting the transfer.
			-v (verbose)  causes various status messages to be printed.
			-n (newline)  causes newline to be output after each message.
			-b before     string to be printed before each message.
			-a after      string to be printed after each message.
			-t timeout    # seconds to wait for response from server
			-d 0123       set debug level to 0, 1, 2, or 3
			-l logfile    Set name of debug log file (default=stderr)
			-P password   Set password and do authenticated connection.
			-s            (single mode) only get one message at a time,
			              even if server can support multiple.
			-N svrPort    Port number for incoming socket stream clients
			-k svrLock	  File to use as server lock
	*/
	static ApplicationSettings settings = new ApplicationSettings();
	static StringToken hostArg = new StringToken(
		"h", "Host", "", TokenOptions.optSwitch, "localhost");
	static IntegerToken portArg= new IntegerToken(
		"p", "Port number", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	static StringToken userArg = new StringToken(
		"u", "User", "", TokenOptions.optSwitch | TokenOptions.optRequired, "");
	static StringToken searchcritArg = new StringToken(
		"f", "SearchCritFile", "", TokenOptions.optSwitch, "");
	static BooleanToken newlineArg = new BooleanToken(
		"n", "newline", "", TokenOptions.optSwitch,false);
	static BooleanToken verboseArg = new BooleanToken(
		"v", "verbose-mode", "", TokenOptions.optSwitch,false);
	static StringToken beforeArg = new StringToken(
		"b", "Before-String", "", TokenOptions.optSwitch, "");
	static StringToken afterArg = new StringToken(
		"a", "After-String", "", TokenOptions.optSwitch, "");
	static IntegerToken timeoutArg = new IntegerToken(
		"t", "timeout seconds", "", TokenOptions.optSwitch, 120);
	static IntegerToken debugArg = new IntegerToken(
		"d", "debug level", "", TokenOptions.optSwitch, 0);
	static StringToken logArg = new StringToken(
		"l", "log-file name", "", TokenOptions.optSwitch, "");
	static StringToken passwordArg = new StringToken(
		"P", "Password for auth connect", "", TokenOptions.optSwitch, "");
	static BooleanToken singleArg = new BooleanToken(
		"s", "single", "", TokenOptions.optSwitch,false);

	static IntegerToken serverPortArg= new IntegerToken(
		"N", "Server Port Number", "", TokenOptions.optSwitch, 10000);
	static StringToken lockFileArg = new StringToken("k",
		"Optional Lock File", "", TokenOptions.optSwitch, "");

	static
	{
		settings.addToken(hostArg);
		settings.addToken(portArg);
		settings.addToken(userArg );
		settings.addToken(searchcritArg );
		settings.addToken(verboseArg );
		settings.addToken(beforeArg );
		settings.addToken(afterArg );
		settings.addToken(newlineArg );
		settings.addToken(timeoutArg );
		settings.addToken(debugArg);
		settings.addToken(logArg);
		settings.addToken(passwordArg);
		settings.addToken(singleArg);
		settings.addToken(serverPortArg);
		settings.addToken(lockFileArg);
	}

	public static void main(String args[])
	{
		try
		{
			settings.parseArgs(args);


			String crit = searchcritArg.getValue();
			if (crit != null && crit.length() == 0)
				crit = null;

			RtSocketStream client = new RtSocketStream(
				hostArg.getValue(), portArg.getValue(), userArg.getValue(),
				crit, verboseArg.getValue(), beforeArg.getValue(),
				afterArg.getValue(), newlineArg.getValue(),
				serverPortArg.getValue(), lockFileArg.getValue());
			client.timeout = timeoutArg.getValue();
			client.singleMode = singleArg.getValue();

			String passwd = passwordArg.getValue();
			if (passwd != null && passwd.length() > 0)
				client.setPassword(passwd);

			client.start();
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Exception while attempting to start client.");
		}
	}

	public void lockFileRemoved()
	{
		if (done)
			return;
		log.info("Exiting -- Lock File Removed.");
		done = true;
	}
}

class RtSocketStreamServer extends BasicServer
{
	public RtSocketStreamServer(int port)
		throws IllegalArgumentException, IOException
	{
		super(port);
	}

	/// Writes the data to all current clients.
	public void writeToClients(byte[] data)
	{
		// Make a local copy of the clients in case exception thrown in loop
		Vector v = new Vector(mySvrThreads);
		for(Iterator it = v.iterator(); it.hasNext(); )
		{
			RtSocketStreamSvrThread thr = (RtSocketStreamSvrThread)it.next();
			try { thr.write(data); }
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Error writing to thread at '{}'", thr.getClientName());
				thr.disconnect();
			}
		}
	}
	protected BasicSvrThread newSvrThread(Socket sock)
		throws IOException
	{
		BasicSvrThread ret = new RtSocketStreamSvrThread(this, sock);
		log.debug("New RtSocketStream client at host {}", ret.getClientName());
		return ret;
	}

	public int getPort() { return portNum; }
}

class RtSocketStreamSvrThread extends BasicSvrThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private OutputStream os;

	public RtSocketStreamSvrThread(BasicServer parent, Socket socket)
	{
		super(parent, socket);
		try { os = socket.getOutputStream(); }
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Cannot get output stream for new client socket (will hangup).");
			os = null;
		}
	}

	public void write(byte[] data)
	{
		try { os.write(data); }
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("IOException writing to client socket.");
			disconnect();
		}
	}

	public void run()
	{
		if (os == null)
		{
			disconnect();
			return;
		}
		try
		{
			while(connected)
				serviceClient();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable to service client request.");
			disconnect();

		}
	}

	public void serviceClient()
	{
		try{ sleep(1000L); }
		catch(InterruptedException e) {}
	}

	public void disconnect()
	{
		log.debug("Client at host '{}' disconnecting", getClientName());
		super.disconnect();
	}
}
