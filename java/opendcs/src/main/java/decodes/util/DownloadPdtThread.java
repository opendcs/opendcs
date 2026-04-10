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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.ServerLock;
import ilex.util.FileServerLock;

/**
An instance of this class downloads the PDT from an URL into a local file,
and then exits.
*/
class DownloadPdtThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String urlstr;
	private String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	private Pdt existingPdt;

	/**
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	DownloadPdtThread(String urlstr, String localfn, Pdt existingPdt)
	{
		this.urlstr = urlstr;
		this.localfn = localfn;
		this.existingPdt = existingPdt;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any PDT files more than 5 days old are deleted.
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
			log.warn("Cannot download PDT because lock file '{}' is " +
					 "either taken by another process or is unwritable.", lockpath);
			return;
		}
		log.info("Obtained lock file '{}' -- proceeding with download.", lockpath);

		long now = System.currentTimeMillis();
		File datedFile = new File(EnvExpander.expand(localfn)
			+ "." + dayFormat.format(new Date(now)));
		log.info("Downloading '{}' to '{}'", urlstr, datedFile.getPath());
		try
		{
			URL url = new URL(urlstr);
			try (BufferedInputStream bis = new BufferedInputStream(url.openStream());
				 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(datedFile));)
			{
				// Note copyStream will close when done.
				FileUtil.copyStream(bis, bos);
			}

			log.info("PDT downloaded to '{}', file size={}", datedFile.getPath(), datedFile.length());
			File existingFile = new File(EnvExpander.expand(localfn));
			log.info("Merging PDT to '{}'", existingFile.getPath());
			merge2ExistingPdt(datedFile, existingFile);
		}
		catch(MalformedURLException ex)
		{
			log.atWarn().setCause(ex).log("Cannot read PDT from '{}'", urlstr);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot download PDT from '{}'", urlstr);
		}

		for(int i=5; i<60; i++)
		{
			File oldFile = new File(EnvExpander.expand(localfn)
				+ "." + dayFormat.format(new Date(now - i * MS_PER_DAY)));
			if (oldFile.exists())
			{
				log.info("Deleting old PDT '{}'", oldFile.getName());
				oldFile.delete();
			}
		}
		mylock.releaseLock();

		if (existingPdt.getPdtLoadListener() != null)
		{
			existingPdt.getPdtLoadListener().pdtLoaded();
			existingPdt.setPdtLoadListener(null);
		}
	}

	/**
	 * Merge the newly-downloaded file into the one currently being used by
	 * applications.
	 * @param newDownloadFile Newly downloaded PDT file
	 * @param existingFile PDT currently being used by applications
	 */
	public void merge2ExistingPdt(File newDownloadFile, File existingFile)
	{
		Pdt newDownloadPdt = new Pdt();

		newDownloadPdt.load(newDownloadFile);
		existingPdt.load(existingFile);

		int numMods = 0;
		for(PdtEntry newPdtEntry : newDownloadPdt.getEntries())
		{
			PdtEntry existingEntry = existingPdt.find(newPdtEntry.dcpAddress);
			if (existingEntry == null
			 || newPdtEntry.lastmodified.after(existingEntry.lastmodified))
			{
				numMods++;
				existingPdt.put(newPdtEntry);
			}
		}

		if (numMods > 0)
		{
			existingPdt.save(existingFile);
			log.info("DownloadPdtThread: saved new pdt to '{}' with {} modifications.",
					 existingFile.getPath(), numMods);
		}
		else
		{
			log.info("DownloadPdtThread: No PDT changes. Final PDT has {} entries.", Pdt.instance().size());
		}
	}

}