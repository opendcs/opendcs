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
package lqm;

import java.io.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.DirectoryMonitorThread;
import lritdcs.*;

/**
This class extends the ILEX DirectoryMonitorThread utility to monitor
for incoming LRIT files in the specified directory.
When a complete incoming file is detected, it is processed.
*/
public class LqmDirectoryMonitor extends DirectoryMonitorThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/// We send messages to senderThread for notifying the LRIT sender.
	SenderThread senderThread = null;

	/// Last time this object retrieved its configuration.
	long lastConfigGet;

	/// Use LRIT File Headers -- turned off by cmd line arg for test mode.
	boolean useFileHeaders;

	LqmDirectoryMonitor(boolean useFileHeaders)
	{
		super();
		this.useFileHeaders = useFileHeaders;
		setSleepEveryCycle(true);
		setSleepInterval(1000L);
		configure();
		setFilenameFilter(
			new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.startsWith("DCS")
						&& name.endsWith(".lrit");
				}
			});
		lastConfigGet = 0;
	}

	/// Called from LQM main after constructing the SenderThread.
	public void setSenderThread(SenderThread st)
	{
		senderThread = st;
	}

	/// Called from base class when a new file is detected.
	protected void processFile(File file)
	{
		log.info("Monitor found file '{}'", file.getPath());

		LritDcsFileReader ldfr = new LritDcsFileReader(file.getPath(),
			useFileHeaders);

		try{ ldfr.load(); }
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Can't load '{}'", file);
			return;
		}
		catch(BadMessageException ex)
		{
			log.atError().setCause(ex).log("Bad LRIT header in file '{}'", file.getName());
			if (System.currentTimeMillis() - file.lastModified() > 60000L)
			{
				file.renameTo(
					new File(LqmConfiguration.instance().dcsDoneDir,
						file.getName()));
			}
			return;
		}

		boolean check = true;
		String nm = ldfr.origFileName();
		if (!ldfr.checkLength())
		{
			log.info("File '{}' failed checkLength, not completed", file);
			check = false;
			if (System.currentTimeMillis() - file.lastModified() < 60000L)
			{
				return;
			}
		}
		else if (!ldfr.checkCRC())
		{
			log.info("CRC failed on {}", file.getPath());
			check = false;
		}

		// Tell sender thread to send notification.
		senderThread.sendResult(nm, check);

		if (!file.renameTo(
			new File(LqmConfiguration.instance().dcsDoneDir, file.getName())))
		{
			log.warn("Could not move '{}' to DONE directory. Attempting to delete.", file.getPath());
			file.delete();
		}
	}

	/// Will be called after each dir scan, about once per second.
	public void finishedScan()
	{
		if (lastConfigGet < LqmConfiguration.instance().getLastLoadTime())
			configure();
	}

	/// Check to se if config has been updated, retrieve my variables if so.
	public void configure()
	{
		log.info("LqmDirectoryMonitor Getting configuration");
		emptyDirectories();
		addDirectory(LqmConfiguration.instance().dcsInputDir);
		log.info("LqmDirectoryMonitor input dir='{}'", LqmConfiguration.instance().dcsInputDir.getPath());
		lastConfigGet = System.currentTimeMillis();
	}

	@Override
	protected void cleanup()
	{
	}
}