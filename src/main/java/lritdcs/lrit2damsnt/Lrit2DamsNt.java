package lritdcs.lrit2damsnt;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lritdcs.HritDcsFileReader;
import lritdcs.HritException;
import lritdcs.LritDcsFileReader;
import lritdcs.recv.LritDcsDirMonitor;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.ByteUtil;
import ilex.util.DirectoryMonitorThread;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.ServerLock;
import ilex.util.ServerLockable;
import decodes.tsdb.CompEventSvr;
import decodes.util.CmdLineArgs;

/**
 * Main class for LRIT to DAMS NT Tool
 * @author mmaloney
 *
 */
public class Lrit2DamsNt
	extends DirectoryMonitorThread
	implements ServerLockable, FilenameFilter
{
	public static final String module = "Lrit2DamsNt";
	// Static command line arguments and initialization for main method.
	protected CmdLineArgs cmdLineArgs = new CmdLineArgs();
	private Lrit2DamsNtConfig config = Lrit2DamsNtConfig.instance();
	private StringToken cfgFileArg;
	private StringToken lockFileArg;
	private ServerLock mylock;
	private long lastMsgTime = System.currentTimeMillis();
	private DamsNtMsgSvr damsNtMsgSvr = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyDDDHHmmssSSS");
	private CompEventSvr compEventSvr = null;
	
	public Lrit2DamsNt()
	{
		cmdLineArgs.setDefaultLogFile("lrit2damsnt.log");
		cfgFileArg = new StringToken("c", "config-file",
			"", TokenOptions.optSwitch, "$DCSTOOL_HOME/lrit2damsnt.conf");
		lockFileArg = new StringToken("k", "lock file",
			"",TokenOptions.optSwitch, "$DCSTOOL_HOME/lrit2damsnt.lock");
		cmdLineArgs.addToken(cfgFileArg);
		cmdLineArgs.addToken(lockFileArg);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Called after constructor and before starting the thread.
	 */
	public void init(String args[])
	{
		// Process the arguments
		cmdLineArgs.parseArgs(args);
		config.setPropFile(EnvExpander.expand(cfgFileArg.getValue()));
		
		// Read config file for first time.
		checkConfig();
		
		// Get the lock file, exit if busy.
		String lockpath = EnvExpander.expand(lockFileArg.getValue());
		mylock = new ServerLock(lockpath);
		if (mylock.obtainLock(this) == false)
		{
			fatal("Cannot start: lock file busy");
			System.exit(0);
		}
		mylock.releaseOnExit();

		// Establish the queue logger.
		if (config.evtListenPort > 0)
		{
			try 
			{
				compEventSvr = new CompEventSvr(config.evtListenPort);
				compEventSvr.startup();
			}
			catch(IOException ex)
			{
				failure(
					"Cannot create Event server on port " + config.evtListenPort
					+ ": " + ex
					+ " -- no events available to external clients.");
			}
		}
		
		// Create & Start the Msg Server
		try
		{
			InetAddress ia = 
				(config.listenHost != null && config.listenHost.length() > 0) ?
				InetAddress.getByName(config.listenHost) : null;
			damsNtMsgSvr = new DamsNtMsgSvr(config.msgListenPort, ia, this);
			Thread listenThread = 
				new Thread()
				{
					public void run()
					{
						try { damsNtMsgSvr.listen(); }
						catch(IOException ex)
						{
							failure("Listen failed: " + ex);
						}
					}
				};
			listenThread.start();
		}
		catch(UnknownHostException ex)
		{
			fatal("Invalid config listenHost '" + config.listenHost + "': " + ex);
			System.exit(1);
		}
		catch (IOException ex)
		{
			fatal("Cannot listen on port " + config.msgListenPort
				+ "(listenHost=" + config.listenHost + "): " + ex);
			System.exit(1);
		}
		
		// Configure the base class Dir Monitor
		addDirectory(new File(EnvExpander.expand(config.fileInputDir)));
		setFilenameFilter(this);
	}

	public static void main(String args[])
	{
		Lrit2DamsNt lrit2DamsNt = new Lrit2DamsNt();
		lrit2DamsNt.init(args);
		lrit2DamsNt.start();
	}

	@Override
	protected void processFile(File file)
	{
		debug("processFile(" + file.getPath() + ")");
		
		long ageMsec = System.currentTimeMillis() - file.lastModified();
		if (ageMsec/1000L > config.fileAgeMaxSeconds)
		{
			info("Ignoring " + file.getPath() + " because age is " 
				+ (ageMsec/1000L) + " seconds.");
			doneFile(file);
			return;
		}
		
		HritDcsFileReader ldfr = new HritDcsFileReader(file.getPath(), 
			config.fileHeaderType == LritDcsDirMonitor.HEADER_TYPE_DOM6);
	
		try
		{
			ldfr.load();
			ldfr.checkHeader();
		}
		catch(HritException ex)
		{
			warning("Error reading HRIT file '" + file.getName() + "': " + ex);
			return;
		}
		catch(Exception ex)
		{
			if (ageMsec > 60000L)
			{
				doneFile(file);
			}
			// Otherwise, maybe the header isn't yet complete.
			// Just return if it's less than a minute old.
			return;
		}


		DcpMsg msg;
		try
		{
			while( (msg = ldfr.getNextMsg()) != null)
			{
				debug("got message for '" + msg.getDcpAddress() + "'");
				processMsg(msg);
			}
		}
		catch(Exception ex)
		{
			warning("Error reading file '" + file.getPath() + "': " + ex);
			if (Logger.instance().getLogOutput() != null)
			{
				ex.printStackTrace(Logger.instance().getLogOutput());
			}
		}
		
		doneFile(file);
	}
	
	private void doneFile(File file)
	{
		if (config.fileDoneDir != null && config.fileDoneDir.length() > 0)
		{
			String toDir = EnvExpander.expand(config.fileDoneDir);
			try
			{
				FileUtil.moveFile(file, new File(toDir, file.getName()));
				return;
			}
			catch(IOException ex)
			{
				warning("Cannot move file '" + file.getPath()
					+ "' to directory '" + toDir + "': " + ex + " -- will delete.");
			}
		}
		if (!file.delete())
			warning("Cannot delete file '" + file.getPath() + "'");
	}
	
	private void processMsg(DcpMsg msg)
	{
		Date cstart = msg.getCarrierStart();
		Date cstop = msg.getCarrierStop();

		// Format message with DAMS-NT header
		byte msgData[] = msg.getData();
		byte damsNtData[] = 
			new byte[
			    msgData.length - 37      // subtract GOES header len
		      + 55                       // Add DAMS-NT Header Len
		      + 2                        // Terminating CRLF after msg data
		      + 14 + 1 + 14 + 2          // carrier-start sp carrier-drop CRLF
		      ];
		
		// Start Pattern
		byte[] startPattern = ByteUtil.fromHexString(config.damsNtStartPattern);
		for(int i=0; i<4; i++)
			damsNtData[i] = startPattern[i];
		
		// Slot Num. For LRIT we will use channel num.
		for(int i=0; i<3; i++)
			damsNtData[4+i] = msgData[DcpMsg.IDX_GOESCHANNEL+i];
		
		// Channel Num and Spacecraft
		for(int i=0; i<3; i++)
			damsNtData[7+i] = msgData[DcpMsg.IDX_GOESCHANNEL+i];
		damsNtData[10] = msgData[DcpMsg.IDX_GOES_SC];

		// Baud rate
		int baudcode = msg.getFlagbits() & DcpMsgFlag.BAUD_MASK;
		int b = baudcode == DcpMsgFlag.BAUD_100 ? 100 :
			    baudcode == DcpMsgFlag.BAUD_1200 ? 1200 : 300;
		damsNtData[11] = (byte)('0' + b/1000);
		damsNtData[12] = (byte)('0' + (b%1000)/100);
		damsNtData[13] = (byte)('0' + (b%100)/10);
		damsNtData[14] = (byte)('0' + (b%10));

		// Start time from GOES header
		for(int i=0; i<11; i++)
			damsNtData[15+i] = msgData[DcpMsg.IDX_YEAR + i];
		
		// Signal Strength
		for(int i=0; i<2; i++)
			damsNtData[26+i] = msgData[DcpMsg.IDX_SIGSTRENGTH + i];
		
		// Freq Offset
		for(int i=0; i<2; i++)
			damsNtData[28+i] = msgData[DcpMsg.IDX_FREQOFFSET + i];
	
		damsNtData[30] = msgData[DcpMsg.IDX_MODINDEX];
		damsNtData[31] = msgData[DcpMsg.IDX_DATAQUALITY];
		
		// For error flag, first digit is 1 if we have extended
		// message times. Second digit is 0=good, 1=parity err, 2=binary msg, 
		// or 4=binary msg with detected bit errors
		damsNtData[32] = 
			(cstart != null && cstop != null) ? (byte)'1' : (byte)'0';
		if (msgData[DcpMsg.IDX_FAILCODE] == (byte)'?')
			damsNtData[33] = (byte)'1';
		else if ((msg.getFlagbits() & DcpMsgFlag.BINARY_MSG) != 0)
			damsNtData[33] = (msg.getFlagbits() & DcpMsgFlag.HAS_BINARY_ERRORS) != 0
				? (byte)'2' : (byte)'0';
		else // ASCII Good Message
			damsNtData[33] = (byte)'0';
		
		// Original (uncorrected) address
		DcpAddress addr = msg.getOrigAddress();
		if (addr == null)
			addr = msg.getDcpAddress();
		String strAddr = addr.toString();
		for(int i=0; i<8 && i<strAddr.length(); i++)
			damsNtData[34+i] = (byte)strAddr.charAt(i);
		
		// Corrected actual address
		addr = msg.getDcpAddress();
		strAddr = addr.toString();
		for(int i=0; i<8 && i<strAddr.length(); i++)
			damsNtData[42+i] = (byte)strAddr.charAt(i);
	
		// Message length as 5 digits
		for(int i=0; i<5; i++)
			damsNtData[50+i] = msgData[DcpMsg.IDX_DATALENGTH+i];
		
		// Message data
		for(int i=0; i<msgData.length-37; i++)
			damsNtData[55 + i] = msgData[37+i];
		
		int suffix = 55 + msgData.length-37;
		damsNtData[suffix++] = (byte)'\r';
		damsNtData[suffix++] = (byte)'\n';
		
		// Extended message times
		if (cstart != null && cstop != null)
		{
			String ds = sdf.format(cstart);
			for(int i=0; i<14; i++)
				damsNtData[suffix++] = (byte)ds.charAt(i);
			damsNtData[suffix++] = (byte)' ';
			ds = sdf.format(cstop);
			for(int i=0; i<14; i++)
				damsNtData[suffix++] = (byte)ds.charAt(i);
			damsNtData[suffix++] = (byte)'\r';
			damsNtData[suffix++] = (byte)'\n';
		}
		
		// Forward to all currently connected clients.
		damsNtMsgSvr.distribute(damsNtData);
	}

	@Override
	protected void finishedScan()
	{
		checkConfig();
		if (System.currentTimeMillis() - lastMsgTime > 10000L)
		{
			damsNtMsgSvr.sendNone();
			lastMsgTime = System.currentTimeMillis();
		}
	}
	
	private void checkConfig()
	{
		try { config.checkConfig(); }
		catch (IOException ex)
		{
			fatal("Cannot read config file: " + ex);
			shutdown();
		}
	}

	@Override
	protected void cleanup()
	{
		info("Shutting down.");
		
		// Shutdown the msg and evt servers and hangup on all clients.
		damsNtMsgSvr.shutdown();
		
		if (compEventSvr != null)
			compEventSvr.shutdown();
		
		// Remove lock file if one is present. We are probably exiting
		// because the lock file was removed, so no error if it doesn't exist.
		mylock.deleteLockFile();
	}
	
	public void fatal(String msg)
	{
		Logger.instance().fatal(module + " " + msg + " -- will shutdown.");
	}
	public void failure(String msg)
	{
		Logger.instance().failure(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.instance().warning(module + " " + msg);
	}
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	public void debug(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}

	@Override
	public void lockFileRemoved()
	{
		fatal("Lock file removed.");
		shutdown();
	}

	@Override
	public boolean accept(File dir, String name)
	{
		if (config.filePrefix != null && config.filePrefix.length() > 0
		 && !name.startsWith(config.filePrefix))
		{
			debug("Will ignore '" + name 
				+ "' because it does not have required prefix '"
				+ config.filePrefix + "'");
			return false;
		}
		if (config.fileSuffix != null && config.fileSuffix.length() > 0
		 && !name.endsWith(config.fileSuffix))
		{
			debug("Will ignore '" + name 
				+ "' because it does not have required suffix '"
				+ config.filePrefix + "'");
			return false;
		}

		return true;
	}

}
