/*
*  $Id$
*/
package lrgs.lddc;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import ilex.util.*;
import ilex.cmdline.*;

import lrgs.common.*;
import lrgs.ldds.LddsClient;
import lrgs.ldds.LddsParams;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.CmdGetOutages;
import lrgs.db.Outage;

/**
Interactive test program for the client-side of a DDS connection.
The user can type commands which correspond to primitive DDS requests.
The results are printed to stdout.
*/
public class Client extends CmdLineProcessor
{
	private int port;
	private String host;
	private File directory;
	private LddsClient client;
	private SimpleDateFormat sdf;

	/**
	 * Constructor.
	 * @param is input stream to read commands from.
	 * @param host host to connect to
	 * @param port port # of the DDS server
	 */
	Client(InputStream is, String host, int port)
		throws Exception
	{
		super(is);

		sdf = new SimpleDateFormat(CmdGetOutages.dateSpec);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		client = new LddsClient(host, port);
		client.setDebugStream(System.err);

		addCmd(
			new CmdLine("hello", "[username]  - Login to server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						client.sendHello(tokens.length < 2 ? 
							"anonymous" : tokens[1]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("auth", "[username passwd] - Auth Login to server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						client.sendAuthHello(tokens[1], tokens[2]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("bye", "    - Disconnect from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						client.sendGoodbye();
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("getcrit", 
				"[name] - Get searchcrit from svr, save in [name]")
			{
				public void execute(String[] tokens)
				{
					try
					{
						client.getSearchCrit(
							tokens.length < 2 ? null : tokens[1]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("putcrit", "[name] - Send named searchcrit to Server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						client.sendSearchCrit(
							tokens.length < 2 ? null : tokens[1]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("msg", "- Get DCP Message from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						int timeout = tokens.length < 2 ? 60
							: Integer.parseInt(tokens[1]);
						DcpMsg msg = client.getDcpMsg(timeout);
						if (msg == null)
							System.out.println(
								"No message currently available");
						else
						{
							System.out.println("Message Received: " + 
								msg.getSeqFileName());
							System.out.println(msg.toString());
						}
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("block", "- Get block of DCP Messages from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						int timeout = tokens.length < 2 ? 60
							: Integer.parseInt(tokens[1]);

						DcpMsg msgs[] = client.getDcpMsgBlock(timeout);
						if (msgs == null || msgs.length == 0)
							System.out.println(
								"No message currently available");
						else
						{
							System.out.println("Received " + msgs.length
								+ " messages:");
							for(int i=0; i<msgs.length; i++)
							{
								System.out.println("Message Received: ");
								System.out.println(msgs[i].toString());
							}
						}
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("getnl", 
				"[svrname] [localname]  - Get network list from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						if (tokens.length < 3)
						{
							System.out.println(
								"Usage: getnl <serverfile> <localfile>");
							return;
						}
						File localfile = new File(tokens[2]);
						client.getNetList(tokens[1], localfile);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
		addCmd(
			new CmdLine("putnl", 
				"[localname] [svrname]  - Send named netlist to server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						if (tokens.length < 3)
						{
							System.out.println(
								"Usage: putnl <localfile> <serverfile>");
							return;
						}
						File localfile = new File(tokens[1]);
						client.sendNetList(localfile, tokens[2]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});
//		addCmd(
//			new CmdLine("stop", "       - Use to abort waiting 'msg' request")
//			{
//				public void execute(String[] tokens)
//				{
//					sendStop();
//				}
//			});

		addCmd(
			new CmdLine("status", "- Get status from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						byte[] status = client.getStatus();
						String s = new String(status);
						System.out.println(s);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("events", "- Get events from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						String[] events = client.getEvents();
						if (events.length == 0)
							System.out.println("No new events.");
						else
							for(int i=0; events != null && i<events.length; i++)
								System.out.println(events[i]);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
						ex.printStackTrace();
					}
				}
			});

		final String userUsage = "Usage: user [set|rm|list] [name] [pw] [roles]";
		addCmd(
			new CmdLine("user", userUsage)
			{
				public void execute(String[] tokens)
				{
					if (tokens.length < 2)
					{
						System.out.println(userUsage);
						return;
					}
					String subcmd = tokens[1];
					String txt = null;
					if (subcmd.equalsIgnoreCase("list"))
						txt = "list";
					else if (subcmd.equalsIgnoreCase("rm"))
					{
						if (tokens.length < 3)
						{
							System.out.println(userUsage);
							return;
						}
						txt = "rm " + tokens[2];
					}
					else if (subcmd.equalsIgnoreCase("set"))
					{
						if (tokens.length < 4)
						{
							System.out.println(userUsage);
							return;
						}
						byte[] sk = client.getSessionKey();
						if (sk == null)
						{
							System.out.println(
								"set user requires authenticated connection.");
							return;
						}
						String username = tokens[2];
						try
						{
							PasswordFileEntry pfe = 
								new PasswordFileEntry(username);
							String pw = null;
							if (tokens[3].equals("-"))
								pw = "-";
							else
							{
								pfe.setPassword(tokens[3]);
								String sks = ByteUtil.toHexString(sk);
//System.out.println("Session key: " + sks);
								pw = ByteUtil.toHexString(pfe.getShaPassword());
//System.out.println("authenticator: " + pw);
								DesEncrypter de = new DesEncrypter(sks);
								pw = de.encrypt(pw);
//System.out.println("    encrypted: " + pw);
							}
							txt = "set " + username + " " + pw;
						
							if (tokens.length >= 5)
								txt = txt + " " + tokens[4];
//System.out.println("Msg Text: '" + txt + "'");
						}
						catch(AuthException ex)
						{
							System.out.println(ex);
							ex.printStackTrace();
						}
					}
					try
					{
						LddsMessage msg = 
							new LddsMessage(LddsMessage.IdUser, txt);
						LddsMessage resp = client.serverExec(msg);
						System.out.println("Response:");
						System.out.println(new String(resp.getBytes()));
					}
					catch(Exception ex)
					{
						System.out.println(ex);
						ex.printStackTrace();
					}
				}
			});

		final String getcfgUsage =
			"[lrgs|ddsrecv|drgs] [localfile] - Retrieve config file";
		addCmd(
			new CmdLine("getcfg", getcfgUsage) 
			{
				public void execute(String[] tokens)
				{
					if (tokens.length < 2)
					{
						System.out.println(getcfgUsage);
						return;
					}
					try
					{
						byte[] cfgdata = client.getConfig(tokens[1]);
						if (tokens.length == 2)
						{
							String s = new String(cfgdata);
							System.out.println(s);
						}
						else
						{
							FileOutputStream fos = 
								new FileOutputStream(tokens[2]);
							fos.write(cfgdata);
							fos.close();
						}
					}
					catch(Exception ex)
					{
						System.err.println(ex);
						ex.printStackTrace();
					}
				}
			});

		final String instcfgUsage =
	"instcfg [lrgs|ddsrecv|drgs|netlist:name] localfile - Install config file";
		addCmd(
			new CmdLine("instcfg", instcfgUsage) 
			{
				public void execute(String[] tokens)
				{
					if (tokens.length != 3)
					{
System.out.println("tokens.length=" + tokens.length 
+ ", lasttok='" + tokens[tokens.length-1] + "'");
						System.out.println(instcfgUsage);
						return;
					}
					try
					{
						File f = new File(tokens[2]);
						FileInputStream fis = new FileInputStream(f);
						byte[] cfgdata = new byte[(int)f.length()];
						fis.read(cfgdata);
						fis.close();
						client.installConfig(tokens[1], cfgdata);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("xmlblock", 
				" [file] - Get block of XML Msg Data from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						byte data[] = client.getMsgBlockExtXml(60);
						PrintStream ps = System.out;
						if (tokens.length == 2)
							ps = new PrintStream(
								new FileOutputStream(tokens[1]));
						ps.println(new String(data));
						if (tokens.length == 2)
							ps.close();
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("extblock", "- Get ext block of Messages from server")
			{
				public void execute(String[] tokens)
				{
					try
					{
						int timeout = tokens.length < 2 ? 60
							: Integer.parseInt(tokens[1]);

						DcpMsg msgs[] = client.getMsgBlockExt(timeout);
						if (msgs == null)
							System.out.println("getMsgBlockExt returned null");
						else if (msgs.length == 0)
							System.out.println(
								"Empty extblock returned");
						else
						{
							System.out.println("Received " + msgs.length
								+ " messages:");
							for(int i=0; i<msgs.length; i++)
							{
								System.out.println("Message Received: ");
								System.out.println(msgs[i].toString());
							}
						}
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});

		addCmd(
			new CmdLine("getoutages", 
				" [start [end]] - Get outages from server")
			{
				public void execute(String[] tokens)
				{
					Date start = null;
					Date end = null;
					try
					{
						if (tokens.length >= 2)
							start = sdf.parse(tokens[1]);
						if (tokens.length >= 3)
							end = sdf.parse(tokens[2]);
					}
					catch(ParseException ex)
					{
						System.out.println("Bad date format, try: "
							+ CmdGetOutages.dateSpec);
						return;
					}
					try
					{
						ArrayList<Outage> outages = 
							client.getOutages(start, end);
						System.out.println("" + outages.size() + " returned:");
						for(Outage otg : outages)
							System.out.println("\t" + otg);
					}
					catch(Exception ex)
					{
						System.out.println(ex);
						ex.printStackTrace();
					}
				}
			});

		final String assertOutageUsage =
			" localfile - Install config file";
		addCmd(
			new CmdLine("assertoutages", assertOutageUsage) 
			{
				public void execute(String[] tokens)
				{
					if (tokens.length != 2)
					{
						System.out.println(assertOutageUsage);
						return;
					}
					try
					{
						File f = new File(tokens[1]);
						FileInputStream fis = new FileInputStream(f);
						byte[] data = new byte[(int)f.length()];
						fis.read(data);
						fis.close();
						LddsMessage msg = 
							new LddsMessage(LddsMessage.IdAssertOutages, 
								new String(data));
						LddsMessage resp = client.serverExec(msg);
						System.out.println("Response:");
						System.out.println(new String(resp.getBytes()));
					}
					catch(Exception ex)
					{
						System.out.println(ex);
					}
				}
			});


		addHelpAndQuitCommands();
	}

	// ========================= main ====================================
	private static ApplicationSettings lddc_main = new ApplicationSettings();
	private static IntegerToken lddc_port = new IntegerToken(
		"p", "Port number", "", TokenOptions.optSwitch, LddsParams.DefaultPort);
	private static StringToken lddc_host = new StringToken(
		"h", "Host", "", TokenOptions.optSwitch, "");
	private static IntegerToken lddc_debug = new IntegerToken(
		"d", "debuglevel", "", TokenOptions.optSwitch, 0);
	static
	{
		lddc_main.addToken(lddc_port);
		lddc_main.addToken(lddc_host);
		lddc_main.addToken(lddc_debug);
	}

	/**
	 * This method start the client reading commands from stdin.
	 */
	void go() throws Exception
	{
		client.connect();
		processInput();
	}

	/**
	 * Called prior to exit, disconnects from server.
	 */
	void cleanup()
	{
		client.disconnect();
	}


	/**
	  Main method.
	  Usage: Client -p port -h host
	  @param args command line arguments
	*/
	public static void main(String args[]) 
	{
		Client cli = null;
		try
		{
			lddc_main.parseArgs(args);
			int port = lddc_port.getValue();
			String host = lddc_host.getValue();
			int debug = lddc_debug.getValue();
			if (debug > 0)
				Logger.instance().setMinLogPriority(
					debug == 1 ? Logger.E_DEBUG1 :
					debug == 2 ? Logger.E_DEBUG2 : 3);

			cli = new Client(System.in, host, port);
			cli.go();
		}
		catch(Exception e)
		{
			System.out.println("Exception thrown: " + e);
		}
		finally
		{
			cli.cleanup();
		}
	}
}

