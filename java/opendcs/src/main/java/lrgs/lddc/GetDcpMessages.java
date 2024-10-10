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

import decodes.util.ResourceFactory;
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
public class GetDcpMessages extends Thread
{
	private String user;
	private String crit;
	private LddsClient lddsClient;
	private boolean verbose, newline, unix;
	private String before, after;
	private String passwd;
	private int maxData;

	protected int total;
	protected long lastmsgtime;
	protected int timeout;
	protected boolean singleMode;
	protected boolean extendedMode;
	protected boolean specialFormat1;
	protected ArrayList<String> netlistNames;
	protected int waitMsec;
	protected boolean escapeNonPrinting = false;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/DDD HH-mm-ss.SSS");
	DecimalFormat df = new DecimalFormat("##");
	DecimalFormat df4 = new DecimalFormat("####");
	/**
	 * Constructor.
	 * @param host host name to connect to
	 * @param port port number
	 * @param user user name
	 * @param crit name of search criteria file
	 * @param verbose if true, print additional info about each message
	 * @param beforeAscii String to print before each message
	 * @param afterAscii String to print after each message
	 * @param newline if true, print newline after each message
	 */
	public GetDcpMessages(String host, int port, String user, String crit, 
		boolean verbose, String beforeAscii, String afterAscii, boolean newline,
		boolean unix)
		throws Exception
	{
		this.user = user;
		this.crit = crit;
		this.verbose = verbose;
		this.newline = newline;
		this.unix = unix;
		before = new String(AsciiUtil.ascii2bin(beforeAscii));
		after = new String(AsciiUtil.ascii2bin(afterAscii));

		lddsClient = new LddsClient(host, port);
		if (verbose)
			lddsClient.setDebugStream(System.err);
		total = 0;
		timeout = 120;
		this.passwd = null;
		singleMode = false;
		netlistNames = new ArrayList<String>();
		waitMsec = 0;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		sdf.setTimeZone(tz);
	}

	/**
	 * Sets an optional password to use in connection.
	 * @param pw the password
	 */
	public void setPassword(String pw)
	{
		this.passwd = pw;
	}

	/**
	 * Sets the single-mode flag, causing messages to be requested in the
	 * old fashion, one-at-a-time.
	 * param @tf the true/fals flag value.
	 */
	public void setSingleMode(boolean tf) { singleMode = tf; }

	/**
	 * Sets a timeout for the connection.
	 * param t number of seconds.
	 */
	public void setTimeout(int t) { timeout = t; }

	/**
	 * Adds a network list to be sent to the server. 
	 */
	public void addNetworkList(String netlistName)
	{
		if (netlistName != null && netlistName.trim().length() > 0)
			netlistNames.add(netlistName);
	}

	/** Thread run method. */
	public void run()
	{
		try
		{
			ResourceFactory.instance().initDbResources();

			// Connect & login as specified user.
			lddsClient.connect();
			if (passwd != null)
				lddsClient.sendAuthHello(user, passwd);
			else
				lddsClient.sendHello(user);
	
			// MsgBlock retrieval only supported in protoversion 4 and higher
			if (lddsClient.getServerProtoVersion() < 4)
				singleMode = true;

			for(String nm : netlistNames)
			{
				File f = new File(nm);
				lddsClient.sendNetList(f, f.getName());
			}

			// Send search crit to get messages since last
			if (crit != null)
				lddsClient.sendSearchCrit(crit);
		}
		catch(Exception e)
		{
			Logger.instance().log(Logger.E_FATAL, "Cannot initialize: " + e);
			//e.printStackTrace(System.err);
			return;
		}

		// Continue to receive messages until I get one past start.
		boolean done = false;
		boolean goodbye = true;
		long endTime = System.currentTimeMillis() + (timeout*1000L);

		lddsClient.enableMultiMessageMode(!singleMode);
		if (extendedMode)
			lddsClient.enableExtMessageMode(true);

		while( !done )
		{
			try
			{
				DcpMsg msg = lddsClient.getDcpMsg(timeout);
				if (msg != null)
				{
					outputMessage(msg);
					total = total + msg.getMsgLength();
					if (total > maxData && maxData > 0)
					{
						done = true;
						System.out.println("Max data limit reached (" + maxData + ")");
					}
					endTime = System.currentTimeMillis() + (timeout*1000L);
					if (waitMsec > 0)
					{
						try { Thread.sleep((long)waitMsec); }
						catch(InterruptedException ex) {}
					}
				}
				else
				{
					throw new ProtocolError(
						"Timeout waiting for response from server.");
				}
			}
			catch(ServerError se)
			{
				if (se.Derrno == LrgsErrorCode.DMSGTIMEOUT)
				{
					if (System.currentTimeMillis() > endTime)
					{
						String s = "No message received in " + timeout 
							+ " seconds, exiting.";
						System.err.println(s);
						Logger.instance().log(Logger.E_FATAL, s);
						done = true;
					}
					else
					{
						Logger.instance().log(Logger.E_DEBUG1,
							"Server caught up to present, pausing...");
						try { Thread.sleep(1000L); }
						catch(InterruptedException ie) {}
						continue;
					}
				}
				if (se.Derrno == LrgsErrorCode.DUNTIL
				 || se.Derrno == LrgsErrorCode.DUNTILDRS)
					System.err.println(
						"Until time reached. Normal termination");
				else
					System.err.println(se.toString());
				done = true;
			}
			catch(Exception e)
			{
				Logger.instance().log(Logger.E_FATAL, e.toString());
				e.printStackTrace(System.err);
				goodbye = false;
				done = true;
			}
		}

		// all done...
		try
		{
			if (goodbye)
				lddsClient.sendGoodbye();
			lddsClient.disconnect();
		}
		catch(Exception e){}
		System.exit(0);
	}

	/**
	 * Outputs a single message to stdout.
	 * @param msg the DCP message
	 */
	protected void outputMessage(DcpMsg msg)
	{
		if ( specialFormat1 ) 
		{
			String header=sdf.format(msg.getLocalReceiveTime())+":"+
				sdf.format(msg.getCarrierStart())+":"+
				sdf.format(msg.getCarrierStop())+":"+
				msg.getBaud()+":"+
				msg.getDataSourceId()+":"+
				sdf.format(msg.getDomsatTime());
			String hl = df.format(header.length());
			System.out.print(hl+header);
			System.out.print(msg.toString());
			System.out.println("");
		}
		else 
		{
			if (before != null)
			{
				System.out.print(before);
				if (unix) System.out.println("");
			}
			if (extendedMode && verbose)
			{
				System.out.println("localReceiveTime=" 
					+ msg.getLocalReceiveTime());
				System.out.println("Flag=0x" + Integer.toHexString(msg.flagbits) 
					+ ",   sequenceNum=" + msg.getSequenceNum() 
					+ ",   filterCode=" + (int)msg.mergeFilterCode);
				System.out.println("baud=" + msg.getBaud() 
					+ ",   carrierStart=" + msg.getCarrierStart()
					+ ",   carrierStop=" + msg.getCarrierStop());
				System.out.println("domsatTime=" + msg.getDomsatTime()
					+ ",   dataSourceId=" + msg.getDataSourceId());
			}
			if (escapeNonPrinting)
				System.out.print(AsciiUtil.bin2ascii(msg.getData()));
			else
				System.out.print(msg.toString());
			if (unix) System.out.println("");
			if (after != null)
			{
				System.out.print(after);
				if (unix) System.out.println("");
			}
			if (newline)
				System.out.println("");
		}
		System.out.flush();
	}
	
	// ========================= main ====================================
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
	static BooleanToken extendedArg = new BooleanToken(
		"E", "extended", "", TokenOptions.optSwitch,false);
	static BooleanToken specialFormat1Arg = new BooleanToken(
		"F1", "extended", "", TokenOptions.optSwitch,false);
	static StringToken netlistArg = new StringToken(
		"N", "Network Lists to Send", "", 
		TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static IntegerToken waitArg = new IntegerToken(
		"w", "Wait Msec after each msg", "", TokenOptions.optSwitch, 0);
	static BooleanToken escapeArg = new BooleanToken(
		"e", "Escape non-printing ASCII chars", "", TokenOptions.optSwitch,false);
	static BooleanToken unixArg = new BooleanToken(
		"x", "unix newline", "", TokenOptions.optSwitch,false);
	static IntegerToken maxDataArg = new IntegerToken(
		"m", "Max characters to output", "", TokenOptions.optSwitch, 0);

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
		settings.addToken(extendedArg);
		settings.addToken(specialFormat1Arg);
		settings.addToken(netlistArg);
		settings.addToken(waitArg);
		settings.addToken(escapeArg);
		settings.addToken(unixArg);
		settings.addToken(maxDataArg);
	}

	/**
	  Main method.
	  <pre>
	  Usage: GetDcpMessages -p port -h host -u user -f searchcrit -v 
		-b before -a after
		... where:
			-p port defaults to 16003.
			-h host defaults to localhost.
			-u user is required.
			-f searchcrit specifies a search criteria file to be downloaded
			   before starting the transfer.
			-N (netlist)	sends NetworkList (netlist).
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
			-E            Enabled extended retrieval mode to V6 server.
			-w msec       Wait msecs after outputting each msg (def=0).
			-e            Escape non-printing ascii characters
			-x            Inserts CR/LF properly on SunOS systems.
			-m amount     Sets the max number of characters to output.
	  </pre>
	  @param args command line arguments.
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

			String crit = searchcritArg.getValue();
			if (crit != null && crit.length() == 0)
				crit = null;

			GetDcpMessages gdm = new GetDcpMessages(
				hostArg.getValue(), portArg.getValue(), userArg.getValue(),
				crit, verboseArg.getValue(), beforeArg.getValue(), 
				afterArg.getValue(), newlineArg.getValue(), unixArg.getValue());
			gdm.timeout = timeoutArg.getValue();
			gdm.setSingleMode(singleArg.getValue());
			gdm.extendedMode = extendedArg.getValue();
			gdm.specialFormat1 = specialFormat1Arg.getValue();
			gdm.waitMsec = waitArg.getValue();
			gdm.maxData = maxDataArg.getValue();

			String passwd = passwordArg.getValue();
			if (passwd != null && passwd.length() > 0)
				gdm.setPassword(passwd);

			for(int i=0; i<netlistArg.NumberOfValues(); i++)
				gdm.addNetworkList(netlistArg.getValue(i));

			gdm.escapeNonPrinting = escapeArg.getValue();

			gdm.start();
		}
		catch(Exception e)
		{
			System.out.println("Exception while attempting to start gdm: " 
				+ e);
		}
	}
}

