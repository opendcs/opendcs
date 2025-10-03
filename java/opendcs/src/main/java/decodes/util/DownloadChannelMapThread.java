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
package decodes.util;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.ServerLock;
import ilex.util.FileServerLock;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
An instance of this class downloads the chans_by_baud file from
https://dcs1.noaa.gov/chans_by_baud.txt
*/
public class DownloadChannelMapThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String purlstr;
	String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	private ChannelMap cmap;

	/**
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	DownloadChannelMapThread(String purlstr, String localfn, ChannelMap cmap)
	{
		this.purlstr = purlstr;
		this.localfn = localfn;
		this.cmap = cmap;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any Channel files more than 5 days old are deleted.
	 */
	public void run()
	{
		ServerLock mylock = null;
		/** Optional server lock ensures only one instance runs at a time. */
		String lockpath = EnvExpander.expand(localfn + ".lock");
		mylock = new FileServerLock(lockpath);
		mylock.setCritical(false);
		if (!mylock.obtainLock())
		{
			log.warn("Cannot download channel map because lock file '{}' " +
					 "is either taken by another process or is unwritable.",
					 lockpath);
			return;
		}
		log.info("Obtained lock file '{}' -- proceeding with download.", lockpath);

		//==========================

		long now = System.currentTimeMillis();
		File localFile = new File(EnvExpander.expand(localfn)
			+ "." + dayFormat.format(new Date(now)));
		log.info("Downloading '{}' to '{}'", purlstr, localFile.getPath());
		try
		{
			URL url = new URL(purlstr);
			try (BufferedInputStream bis = new BufferedInputStream(url.openStream());
				 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(localFile));)
			{
				// Note copyStream will close when done.
				FileUtil.copyStream(bis, bos);
			}

			log.info("CDT Download complete, file size={}", localFile.length());
			File current = new File(EnvExpander.expand(localfn));
			log.info("Copying Channels to '{}'", current.getPath());
			FileUtil.copyFile(localFile, current);
			cmap.load(current);
		}
		catch(MalformedURLException ex)
		{
			log.atWarn().setCause(ex).log("Cannot read Channels from '{}'", purlstr);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot download Channels from '{}'", purlstr);
		}

		for(int i=5; i<60; i++)
		{
			File oldFile = new File(EnvExpander.expand(localfn)
				+ "." + dayFormat.format(new Date(now - i * MS_PER_DAY)));
			if (oldFile.exists())
			{
				log.info("Deleting old Channels '{}'", oldFile.getName());
				oldFile.delete();
			}
		}
		mylock.releaseLock();
	}
}