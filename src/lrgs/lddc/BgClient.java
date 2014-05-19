/*
*  $Id$
*
*/
package lrgs.lddc;

import java.io.*;
import java.util.Date;
import ilex.util.*;
import ilex.cmdline.*;
import lrgs.common.*;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsParams;
import lrgs.ldds.ServerError;

/**
This is a benchmarking test client program that opens a specified number
of threads to a single DDS server and requests a real-time stream of messages.
It was used to test the bandwidth of DAPS-II and LRGS DDS servers.
*/
public class BgClient extends Thread
{
	private int port;
	private String host;
	private String user;
	private String crit;
	private LddsClient client;
	private boolean quiet;
	private int delay;
	private int clientnum;
	private int total;
	private long lastmsgtime;
	private boolean running;
	private long start;

	/**
	 * Constructor for a single client thread object.
	 * @param host the host to connect to
	 * @param port the port to connect on
	 * @param user the user name to connect as
	 * @param crit name of searchcrit file to use
	 * @param quite verbose/quite flag
	 * @delay # seconds to delay before restarting
	 * @clientnum Unique ID assigned to this thread.
	 */
	BgClient(String host, int port, String user, String crit, boolean quiet,
		int delay, int clientnum)
		throws Exception
	{
		this.user = user;
		this.crit = crit;

		client = new LddsClient(host, port);
		if (!quiet)
			client.setDebugStream(System.out);
		this.quiet = quiet;
		this.delay = delay;
		this.clientnum = clientnum;
		total = 0;
	}

	/**
	 * Thread run method.
	 */
	public void run()
	{
		running = true;
		start = System.currentTimeMillis();
		while(running)
		{
			try
			{
				// Connect & login as specified user.
				client.connect();
				client.sendHello(user);
	
				// Send search crit to get messages since last
				if (crit != null && crit.length() > 0)
					client.sendSearchCrit(crit);
	
				// Grab current time.
	
				// Continue to receive messages until I get one past start.
				long now = System.currentTimeMillis();
				System.out.println(
					"Client " + clientnum + " Connected at " + now);
				long msgtime = 0;
				DcpMsg msg;
				try
				{
					do
					{
						msg = client.getDcpMsg(5);
						Date d = msg.getDapsTime();
						lastmsgtime = d.getTime();
	
						if (msg == null)
							System.out.println("No message currently available");
						else if (!quiet)
						{
							System.out.println("------- (now="+now+", this="+lastmsgtime+")");
							System.out.println("Message Received: " + msg.getSeqFileName());
							System.out.println(msg.toString());
						}
						else if (++total % 100 == 0)
							showSummary();
					} while (running && msg != null && lastmsgtime < now);
				}
				catch(ServerError se)
				{
					if (se.Derrno == LrgsErrorCode.DUNTIL)
						System.out.println("UNTIL Reached - normal termination.");
					else
						System.out.println(se);
				}
	
				showSummary();
	
				// Logoff and disconnect
				client.sendGoodbye();
				client.disconnect();
			}
			catch(Exception e)
			{
				System.out.println("Client " + clientnum + ": " + e);
				e.printStackTrace();
			}
	
			// Sleep for 1 minutes
			try { Thread.sleep(delay*1000); }
			catch (InterruptedException ie) {}
		}
	}

	/**
	 * Prints a message to stdout showing current statistics for this thread.
	 */
	void showSummary()
	{
		System.out.println("Client " + clientnum + ": " + total 
			+ " messages. lastmsgtime=" + lastmsgtime);
	}

	/** Cleanup and disconnect this thread. */
	void cleanup()
	{
		client.disconnect();
	}

	/**
	 * Stops this thread and prints a message to standard output with final
	 * statistics.
	 */
	void stopAndPrintStats()
	{
		running = false;
		long now = System.currentTimeMillis();
		int elapsed = (int)((now - start)/1000L);

		System.out.println("Client " + clientnum + " Elapsed sec: " + elapsed +
			", total=" + total + 
			", rate=" + ((float)total/(float)elapsed) + " msg/sec");
		try { Thread.sleep(50L); }
		catch(Exception ex) {}
	}


	// ========================= main ====================================
	/**
	*/
	private static ApplicationSettings lddc_main = new ApplicationSettings();
	private static IntegerToken lddc_port = new IntegerToken(
		"p", "Port number", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	private static StringToken lddc_host = new StringToken(
		"h", "Host", "", TokenOptions.optSwitch, "");
	private static StringToken lddc_user = new StringToken(
		"u", "User", "", TokenOptions.optSwitch, "");
	private static StringToken lddc_crit = new StringToken(
		"f", "SearchCritFile", "", TokenOptions.optSwitch, "");
	private static IntegerToken lddc_nclients = new IntegerToken(
		"n", "# Clients", "", TokenOptions.optSwitch, 1);
	private static BooleanToken lddc_quiet = new BooleanToken(
		"q", "quiet-mode", "", TokenOptions.optSwitch,false);
	private static IntegerToken lddc_wait = new IntegerToken(
		"w", "Wait-seconds", "", TokenOptions.optSwitch, 60);
	private static IntegerToken lddc_time = new IntegerToken(
		"t", "Run-seconds", "", TokenOptions.optSwitch, 60);

	static
	{
		lddc_main.addToken(lddc_port);
		lddc_main.addToken(lddc_host);
		lddc_main.addToken(lddc_user);
		lddc_main.addToken(lddc_crit);
		lddc_main.addToken(lddc_nclients);
		lddc_main.addToken(lddc_quiet);
		lddc_main.addToken(lddc_wait);
		lddc_main.addToken(lddc_time);
	}


	/**
	 * Main method. 
       <pre>
	  Usage: BgClient -p port -h host -u user -f searchcritfile
		-q -n nthreads -w wait-interval -t #seconds
		... where:
			-q (quiet mode) do not echo messages to screen.
			-n nthreads = number of threads to start (default 1)
			-w wait = # seconds to wait between each connect (default 60)
			          Wait kicks in on timeout or when UNTIL exception received.
			-t #seconds = Total number of seconds to run. Then exit and print
			              statistics.
	   </pre>
	 * @param args command line arguments
	 */
	public static void main(String args[]) 
	{
		BgClient cli[] = null;
		int nclients = 1;
		try
		{
			lddc_main.parseArgs(args);
			int port = lddc_port.getValue();
			String host = lddc_host.getValue();
			String user = lddc_user.getValue();
			String crit = lddc_crit.getValue();
			if (crit.length() == 0)
				crit = null;
			nclients = lddc_nclients.getValue();
			boolean quiet = lddc_quiet.getValue();
			int wait = lddc_wait.getValue();
			int nsecs = lddc_time.getValue();

			cli = new BgClient[nclients];
			for(int i = 0; i < nclients; i++)
			{
				System.out.println("Starting client " + i);
				cli[i] = new BgClient(host, port, user, crit, quiet, wait, i);
				cli[i].start();
			}

			long stop = System.currentTimeMillis() + ((long)nsecs * 1000L);
			while(System.currentTimeMillis() < stop)
			{
				try { Thread.sleep(1000); }
				catch (InterruptedException ie) {}
			}

			for(int i = 0; i < nclients; i++)
			{
				cli[i].stopAndPrintStats();
			}
			System.exit(0);
		}
		catch(Exception e)
		{
			System.out.println("Exception thrown: " + e);
		}
		finally
		{
			if (cli != null)
				for(int i = 0; i < nclients; i++)
					cli[i].cleanup();
		}
	}
}

