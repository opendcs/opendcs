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
package decodes.util.hads;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;

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
An instance of this class downloads the USGS Hads from an URL into a
local file and then exits.
*/
public class LoadHadsThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	String purlstr;
	String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;

	/**
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	LoadHadsThread(String purlstr, String localfn)
	{
		this.purlstr = purlstr;
		this.localfn = localfn;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any USGS Hads files more than 5 days old are deleted.
	 */
	public void run()
	{

		long now = System.currentTimeMillis();
		File localFile = new File(EnvExpander.expand(localfn)
			+ dayFormat.format(new Date(now)));
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

			log.info("Download complete, file size={}" , localFile.length());
			File current = new File(EnvExpander.expand(localfn));
			log.info("Copying Hads to '{}'", current.getPath());
			FileUtil.copyFile(localFile, current);
		}
		catch(MalformedURLException ex)
		{
			log.atError().setCause(ex).log("Cannot read HADS from '{}'", purlstr);
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Cannot download HADS from '{}'", purlstr);
		}
		for(int i=5; i<60; i++)
		{
			File oldFile = new File(EnvExpander.expand(localfn)
				+ dayFormat.format(new Date(now - i * MS_PER_DAY)));
			if (oldFile.exists())
			{
				log.info("Deleting old HADS '{}'", oldFile.getName());
				oldFile.delete();
			}
		}
	}
}