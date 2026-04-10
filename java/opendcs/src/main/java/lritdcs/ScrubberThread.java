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
package lritdcs;

import java.io.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class ScrubberThread extends LritDcsThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	File directories[];
	File mediumDir, mediumSentDir;
	File lowDir, lowSentDir;
	int scrubHours;

	public ScrubberThread()
	{
		super("ScrubberThread");
		scrubHours = 48;
	}

	public void run()
	{
		log.debug("Starting");
		while(!shutdownFlag)
		{
			try { sleep(60000L); }
			catch(InterruptedException ex) {}

			long now = System.currentTimeMillis();
			for(int i=0; i<directories.length; i++)
			{
				File file[] = directories[i].listFiles();
				for(int j=0; file != null && j<file.length; j++)
				{
					long lmt = file[j].lastModified();
					if ((now - lmt) > (scrubHours * 60L * 60L * 1000L))
					{
						log.debug("Deleting file '{}'", file[j].getPath());
						try { file[j].delete(); }
						catch(Exception ex)
						{
							log.atWarn()
							   .setCause(ex)
							   .log("{}- Unable to delete file '{}'",
							   		Constants.EVT_FILE_DELETE_ERR, file[j].getPath());
						}
					}
				}
			}
		}
	}

	public void init()
		throws InitFailedException
	{
		// Create the directory objects. Make sure they all exist and are
		// directories.
		directories = new File[6];

		int i=0;
		try
		{
			String home = LritDcsConfig.instance().getLritDcsHome();
			directories[0] = new File(home + File.separator + "high");
			directories[1] = new File(home + File.separator + "high.sent");
			directories[2] = new File(home + File.separator + "medium");
			directories[3] = new File(home + File.separator + "medium.sent");
			directories[4] = new File(home + File.separator + "low");
			directories[5] = new File(home + File.separator + "low.sent");
			for(i=0; i<directories.length; i++)
			{
				if (!directories[i].exists())
					directories[i].mkdirs();
			}
		}
		catch(Exception ex)
		{
			throw new InitFailedException("Can't create directory '" + directories[i].getPath() + "'", ex);
		}

		getConfigValues(LritDcsConfig.instance());
		registerForConfigUpdates();
	}

	protected void getConfigValues(LritDcsConfig cfg)
	{
		scrubHours = cfg.getScrubHours();
	}
}