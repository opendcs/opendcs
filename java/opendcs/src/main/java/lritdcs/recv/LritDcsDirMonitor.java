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
package lritdcs.recv;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.DirectoryMonitorThread;
import ilex.util.FileUtil;

import lrgs.common.DcpMsg;

import lritdcs.LritDcsFileReader;

public class LritDcsDirMonitor extends DirectoryMonitorThread implements FilenameFilter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.debug("Monitor found file '{}'", file.getPath());

		LritDcsFileReader ldfr = new LritDcsFileReader(file.getPath(),
			headerType == HEADER_TYPE_DOM6);

		try{ ldfr.load(); }
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Bad LRIT-DCS file '{}'", file.getName());
			if (System.currentTimeMillis() - file.lastModified() > 60000L)
			{
				File toFile = new File(doneDir, file.getName());
				try
				{
					FileUtil.moveFile(file, toFile);
				}
				catch(IOException ex2)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Cannot move '{}' to '{}'", file.getPath(), toFile.getPath());
				}
			}
			return;
		}

		if (!ldfr.checkLength())
		{
			log.info("File {} failed checkLength, not completed", file);
			if (System.currentTimeMillis() - file.lastModified() < 60000L)
			{
				return;
			}
		}
		else if (!ldfr.checkCRC())
		{
			log.warn("CRC failed on {}", file.getPath());
		}

		DcpMsg msg;
		try
		{
			while( (msg = ldfr.getNextMsg()) != null)
			{
				log.trace("got message for '{}'", msg.getDcpAddress());
				msgProcessor.processMsg(msg);
			}
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Error reading file '{}'", file.getPath());
		}

		File toFile = new File(doneDir, file.getName());
		try
		{
			log.trace("Moving file '{}'", file.getName());
			FileUtil.moveFile(file, toFile);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot move '{}' to '{}'", file.getPath(), toFile.getPath());
		}
	}

	public void finishedScan( )
	{
		log.trace("finishedScan");
		if (lastConfigGet < conf.getLastLoadTime())
			configure();
	}


	/**
	 * Check to se if config has been updated, retrieve my variables if so.
	 */
	public void configure()
	{
		log.info("LritDcsDirMonitor Getting configuration");

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

		log.info("LritDcsDirMonitor input dir='{}'", conf.fileInputDir);
		log.info("LritDcsDirMonitor file prefix='{}'", prefix);
		log.info("LritDcsDirMonitor file suffix='{}'", suffix);
		log.info("LritDcsDirMonitor file doneDir='{}'", doneDir);
	}

	/**
	 * Overloaded from FilenameFilter, return true if this file is to be
	 * processed, according to the prefix and suffix supplied in the config.
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