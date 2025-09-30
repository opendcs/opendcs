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

import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;

public class LqmMain
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	LqmDirectoryMonitor myMonitor;
	SenderThread senderThread;

	LqmMain()
	{
	}

	public void run()
	{
		log.info("Program Starting");
		LqmConfiguration cfg = LqmConfiguration.instance();

		String cfgName = cmdLineArgs.getConfigFile();
		cfg.setConfigFileName(cfgName);
		try { cfg.loadConfig();}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Cannot read config file '{}'", cfgName);
			System.exit(1);
		}

		myMonitor = new LqmDirectoryMonitor(cmdLineArgs.useFileHeaders());
		senderThread = new SenderThread();
		myMonitor.setSenderThread(senderThread);

		senderThread.start();
		myMonitor.start();

		// Main Loop
		while(true)
		{
			try { Thread.sleep(10000); }
			catch(InterruptedException ex) {}
			cfg.checkConfig();
			checkDoneFileAges();
			senderThread.sendStatus("OK");
		}
	}


	private void checkDoneFileAges()
	{
		LqmConfiguration cfg = LqmConfiguration.instance();
		long allowableFileAge = System.currentTimeMillis()
			- (cfg.maxFileAge * 86400000L);

		try
		{
			File path = cfg.dcsDoneDir;
			File pathFiles[] = path.listFiles();
			int pathLength = pathFiles.length;
			for(int a = 0; a < pathLength; a++)
			{
				if(pathFiles[a].lastModified() < allowableFileAge)
				{
					log.info("Deleting Old File: {}", pathFiles[a].getName());
					pathFiles[a].delete();
				}
			}
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Exception listing directory '{}' -- will not delete any old files.",
			   		cfg.dcsDoneDir.getPath());
		}
	}

	private static LqmCmdLineArgs cmdLineArgs = new LqmCmdLineArgs();

	public static void main(String args[])
		throws IOException
	{
		cmdLineArgs.parseArgs(args);
		LqmMain lm = new LqmMain();
		lm.run();
	}
}
