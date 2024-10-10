/**
 * @(#) LritDcsDirMonitor.java
 */

package lritdcs.recv;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import ilex.util.DirectoryMonitorThread;
import ilex.util.Logger;
import ilex.util.FileUtil;

import lrgs.common.DcpMsg;

import lritdcs.LritDcsFileReader;
import lritdcs.BadMessageException;

public class LritDcsDirMonitor extends DirectoryMonitorThread
	implements FilenameFilter
{
	private DcpMsgProcessor msgProcessor;

	/**
	 * Code indicating the header type to expect. See symbolic codes.
	 * More may be added to support LRIT receivers from different vendors.
	 */
	private char headerType;
	
	/**
	 * Pre-transmission header only. Use this when testing by copying files
	 * directly from LRITDCS Sender into the receive directory.
	 */
	public static final char HEADER_TYPE_PREXMIT = 'N';
	
	/**
	 * Domain-6 header. Use this when this app is running on the open-source
	 * DOMAIN-6 LRIT receive system.
	 */
	public static final char HEADER_TYPE_DOM6 = '6';

	/** Last time this object retrieved its configuration. */
	long lastConfigGet;

	private LritDcsRecvConfig conf;
	private String prefix;
	private String suffix;
	private File doneDir;
	
	public LritDcsDirMonitor( DcpMsgProcessor proc)
	{
		super();
		msgProcessor = proc;
		prefix = null;
		suffix = null;

		conf = LritDcsRecvConfig.instance();

		setSleepEveryCycle(true);
		setSleepInterval(1000L);
		setFilenameFilter(this);
		configure();
	}
	
	/**
	 * Called from Dir Mon Thread when a new file is seen.
	 * Validate the file, then pick it apart into messages.
	 * For each message, call the processor.
	 */
	public void processFile( File file )
	{
		Logger.instance().debug1(
			"Monitor found file '" + file.getPath() + "'");

		LritDcsFileReader ldfr = new LritDcsFileReader(file.getPath(), 
			headerType == HEADER_TYPE_DOM6);
	
		try{ ldfr.load(); }
		catch(Exception ex)
		{
			Logger.instance().warning(
				"Bad LRIT-DCS file '" + file.getName() + "': " + ex);
			if (System.currentTimeMillis() - file.lastModified() > 60000L)
			{
				File toFile = new File(doneDir, file.getName());
				try 
				{
					FileUtil.moveFile(file, toFile);
				}
				catch(IOException ex2)
				{
					Logger.instance().warning("Cannot move '" + file.getPath()
						+ "' to '" + toFile.getPath() + "': " + ex2);
				}
			}
			return;
		}

		if (!ldfr.checkLength())
		{
			Logger.instance().info(
				"File " + file + " failed checkLength, not completed");
			if (System.currentTimeMillis() - file.lastModified() < 60000L)
			{
				return;
			}
		}
		else if (!ldfr.checkCRC())
		{
			Logger.instance().warning("CRC failed on " + file.getPath());
		}

		DcpMsg msg;
		try
		{
			while( (msg = ldfr.getNextMsg()) != null)
			{
				Logger.instance().debug3("got message for '" + 
					msg.getDcpAddress() + "'");
				msgProcessor.processMsg(msg);
			}
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error reading file '" + file.getPath() + "': " + ex);
		}

		File toFile = new File(doneDir, file.getName());
		try 
		{
			Logger.instance().debug3("Moving file '" + file.getName() + "'");
			FileUtil.moveFile(file, toFile);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot move '" + file.getPath()
				+ "' to '" + toFile.getPath() + "': " + ex);
		}
	}
	
	public void finishedScan( )
	{
		Logger.instance().debug3("finishedScan");
		if (lastConfigGet < conf.getLastLoadTime())
			configure();
	}


	/**
	 * Check to se if config has been updated, retrieve my variables if so.
	 */
	public void configure()
	{	
		Logger.instance().info("LritDcsDirMonitor Getting configuration");

		emptyDirectories();
		synchronized(conf)
		{
			headerType = conf.fileHeaderType;
			addDirectory(new File(conf.fileInputDir));
			prefix = conf.filePrefix;
			suffix = conf.fileSuffix;
			doneDir = new File(conf.fileDoneDir);
		}
		lastConfigGet = System.currentTimeMillis();

		Logger.instance().info(
			"LritDcsDirMonitor input dir='" + conf.fileInputDir + "'");
		Logger.instance().info(
			"LritDcsDirMonitor file prefix='" + prefix + "'");
		Logger.instance().info(
			"LritDcsDirMonitor file suffix='" + suffix + "'");
		Logger.instance().info(
			"LritDcsDirMonitor file doneDir='" + doneDir + "'");
	}

	/**
	 * Overeloaded from FilenameFilter, return true if this file is to be
	 * processed, according to the prefix & suffix supplied in the config.
	 * @param dir the directory containing the file (ignored)
	 * @param name the file name.
	 */
	public boolean accept(File dir, String name)
	{
		return (prefix == null || name.startsWith(prefix))
		    && (suffix == null || name.endsWith(suffix));
	}

	@Override
	protected void cleanup()
	{
	}
}
