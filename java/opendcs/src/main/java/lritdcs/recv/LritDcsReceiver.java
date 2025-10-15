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


import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import ilex.util.FileServerLock;
import ilex.util.EnvExpander;
import ilex.util.ServerLock;

public class LritDcsReceiver
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private LritDcsRecvConfig config;

	private LritDcsDirMonitor myMonitor;

	private static LdrCmdLineArgs cmdLineArgs = new LdrCmdLineArgs();

	private ServerLock mylock;

	private LritDcsReceiver()
	{
	}

	public void run()
	{
		log.info("Program Starting");

		// Check lock file.
		String lockpath = EnvExpander.expand(cmdLineArgs.getLockFile());
		mylock = new FileServerLock(lockpath);
        if (mylock.obtainLock() == false)
        {
            log.error("NOT started: lock file busy");
            System.exit(1);
        }

        mylock.releaseOnExit();
        Runtime.getRuntime().addShutdownHook(
            new Thread()
            {
                public void run()
                {
                    log.info("DCP LRIT Receiver exiting {}",
							(mylock.wasShutdownViaLock() ? "(lock file removed)": ""));
                }
            });

		LritDcsRecvConfig cfg = LritDcsRecvConfig.instance();
		String cfgName = cmdLineArgs.getConfigFile();
		cfg.setPropFile(cfgName);
		try { cfg.load(); }
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Cannot read config file '{}'", cfgName);
			System.exit(1);
		}

		MsgArch arch = new MsgArch();
		arch.init();

		myMonitor = new LritDcsDirMonitor(arch);
		myMonitor.start();

		// Main Loop
		while(true)
		{
			try
			{
				Thread.sleep(60000);
				cfg.checkConfig();
				scrubArchive();
				scrubDoneFiles();
			}
			catch(InterruptedException ex) {}
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("Config file error.");
			}
		}
	}

	private void scrubArchive()
	{
		File arcDir = new File(LritDcsRecvConfig.instance().msgFileDir);
		File files[] = arcDir.listFiles();
		int pfxLen = MsgPerArch.filePrefix.length();
		int dateLen = MsgPerArch.dateSpec.length();
		int neededLen = pfxLen + dateLen + MsgPerArch.fileSuffix.length();
		for(int i=0; files != null && i < files.length; i++)
		{
			String nm = files[i].getName();
			if (nm.length() != neededLen)
			{
				log.warn("Scrubber - skipping file with bad name length '{}'", nm);
				continue;
			}
			try
			{
				Date d = MsgPerArch.fnf.parse(
					nm.substring(pfxLen, pfxLen+dateLen));
				if (System.currentTimeMillis() - d.getTime() >
					(1000L * 60L * 60L * 24L))
				{
					log.info("Scrubber deleting '{}'", nm);
					if (!files[i].delete())
					{
						log.warn("Scrubber could not delete '{}'", nm);
					}
				}
			}
			catch(ParseException ex)
			{
				log.atWarn().setCause(ex).log("Scrubber - skipping file with bad date component '{}'", nm);
			}
		}

	}

	private void scrubDoneFiles()
	{
		File doneDir = new File(LritDcsRecvConfig.instance().fileDoneDir);
		if (!doneDir.isDirectory())
			return;
		long now = System.currentTimeMillis();
		File list[] = doneDir.listFiles();
		for(int i=0; list != null && i < list.length; i++)
		{
			if (now - list[i].lastModified() > (1000L*60L*60L*24L))
			{
				try
				{
					if (!list[i].delete())
					{
						log.warn("Cannot delete '{}'", list[i].getPath());
					}
				}
				catch(Exception ex)
				{
					log.atWarn().setCause(ex).log("Cannot delete '{}'", list[i].getPath());
				}
			}
		}
	}


	public static void main( String[] args )
		throws IOException
	{
		cmdLineArgs.parseArgs(args);

		LritDcsReceiver main = new LritDcsReceiver();
		main.run();
	}
}
