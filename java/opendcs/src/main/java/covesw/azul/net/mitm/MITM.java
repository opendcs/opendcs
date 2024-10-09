package covesw.azul.net.mitm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import ilex.cmdline.ApplicationSettings;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.IntegerToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.net.BasicClient;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

public class MITM
extends BasicServer
{
	private static ApplicationSettings settings = new ApplicationSettings();
	private static StringToken connectHostArg = new StringToken(
		"c", "host or IP to connect to", "", TokenOptions.optSwitch, "");
	private static IntegerToken connectPortArg = new IntegerToken("p", "port to connect to", "",
		TokenOptions.optSwitch, 0);
	private static IntegerToken listenPortArg = new IntegerToken("s", "server listening port", "",
		TokenOptions.optSwitch, 18000);
	private static BooleanToken printHexArg = new BooleanToken("h", "print hex after ascii",
		"", TokenOptions.optSwitch, false);
	private static StringToken sessionLogArg = new StringToken(
		"f", "root name of session log", "", TokenOptions.optSwitch, "mitm-");
	
	private int sequenceNum = 0;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
	
	public static void main(String args[])
		throws Exception
	{
		settings.addToken(connectHostArg);
		settings.addToken(connectPortArg);
		settings.addToken(listenPortArg);
		settings.addToken(printHexArg);
		settings.addToken(sessionLogArg);
		settings.parseArgs(args);
		MITM mitm = new MITM(listenPortArg.getValue());
		mitm.listen();
	}

	
	public MITM(int port) throws IllegalArgumentException, IOException
	{
		super(port);
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket initSock) throws IOException
	{
		final String fname = sessionLogArg.getValue() + sdf.format(new Date()) + (sequenceNum++);
		final MITMLogger mitmLogger = new MITMLogger(fname, printHexArg.getValue());
		
		// Use Basic Client to open a socket to the designated remote server.
		final BasicClient dest = 
			new BasicClient(connectHostArg.getValue(), connectPortArg.getValue());
		System.out.println("Connecting to " + connectHostArg.getValue() 
			+ ":" + connectPortArg.getValue());
		dest.connect();
		
		// Start thread that connects initSock.in to destSock.out with prefix ">>"
		final MITMConnector idCon = new MITMConnector(">>",
			new BufferedInputStream(initSock.getInputStream()),
			new BufferedOutputStream(dest.getOutputStream()), mitmLogger);
		
		// Start a thread that connects destSock.in to initSock.out with prefix "<<"
		final MITMConnector diCon = new MITMConnector("<<",
			new BufferedInputStream(dest.getInputStream()),
			new BufferedOutputStream(initSock.getOutputStream()), mitmLogger);
		
		idCon.mate = diCon;
		diCon.mate = idCon;
		idCon.start();
		diCon.start();
		
		// Start a BasicSvrThread to monitor the two connectors.
		BasicSvrThread ret = 
			new BasicSvrThread(this, initSock)
			{
				@Override
				protected void serviceClient()
				{
					if (!idCon.isAlive() || !diCon.isAlive())
					{
						disconnect();
						dest.disconnect();
						mitmLogger.close();
						System.out.println("Connection with log '" + fname + "' has terminated.");
					}
					else // do nothing. The connector threads do the work.
						try { sleep(100L); } catch(InterruptedException ex) {}
				}
			};
		return ret;
	}
	
	

}
