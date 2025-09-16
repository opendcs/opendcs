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
package decodes.routing;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Platform;
import decodes.db.RoutingSpec;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.NoSuchObjectException;

/**
 * Maintain an archive of Raw Messages for a routing spec.
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public class RawArchive
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private RoutingSpec routingSpec = null;
	private String archivePath = null;
	private String module = "RawArchive";
	private String startDelim = null;
	private String endDelim = null;
	private IntervalIncrement maxAge = null; 
	private String lastArchivePath = null;
	private PurgeOldFilesThread purgeThread = null;

	public RawArchive(RoutingSpec routingSpec)
	{
		this.routingSpec = routingSpec;
		archivePath = routingSpec.getProperty("RawArchivePath");
		startDelim = routingSpec.getProperty("RawArchiveStartDelim");
		endDelim = routingSpec.getProperty("RawArchiveEndDelim");
		String ma = routingSpec.getProperty("RawArchiveMaxAge");
		if (ma != null)
		{
			try
			{
				maxAge = IntervalIncrement.parseMult(ma)[0];
				purgeThread = new PurgeOldFilesThread(this);
				purgeThread.start();
			}
			catch (NoSuchObjectException ex)
			{
					log.atWarn()
				   .setCause(ex)
				   .log("Unrecognized RawArchiveMaxAge property '{}' -- NO ARCHIVE LIMIT WILL BE ENFORCED.", ma);
			}
		}

	}
	
	public void shutdown()
	{
		if (purgeThread != null)
			purgeThread.shutdown = true;
	}

	/**
	 * Evaluate the archiveFileName within the context of the routing spec
	 * and the message. Open it (or create it). Append the raw message.
	 * Then close it.
	 * 
	 * @param rawMsg The raw message to archive
	 */
	public void archive(RawMessage rawMsg)
	{
		// First build a complete set of props.
		Properties props = new Properties();
		PropertiesUtil.copyProps(props, System.getProperties());
		PropertiesUtil.copyProps(props, routingSpec.getProperties());
		
		// Copy in platform properties.
		Platform p;
		try
		{
			p = rawMsg.getPlatform();
			if (p != null)
			{
				PropertiesUtil.copyProps(props, p.getProperties());
				props.setProperty("STATION", p.getSiteName(false));
				props.setProperty("MEDIUMID", rawMsg.getTransportMedium().getMediumId());
				if (p.getPlatformDesignator() != null && p.getPlatformDesignator().length() > 0)
					props.setProperty("DESIGNATOR", p.getPlatformDesignator());
			}
		}
		catch (UnknownPlatformException ex) 
		{ 
			log.atTrace().setCause(ex).log("Unable to find a platform while archiving.");
		}
		
		// Copy in all performance measurement properties.
		for(Iterator<String> pmNameIt = rawMsg.getPMNames(); pmNameIt.hasNext();)
		{
			String name = pmNameIt.next();
			String value = rawMsg.getPM(name).getStringValue();
			props.setProperty(name, value);
		}
		
		// Now evaluate the archive name against the aggregate properties.
		String evalName = EnvExpander.expand(archivePath, props);
		log.trace("Writing to archive file '{}'", evalName);
		
		try
		{
			FileOutputStream out = new FileOutputStream(evalName, true);
			if (startDelim != null)
				out.write(AsciiUtil.ascii2bin(EnvExpander.expand(startDelim, props)));
			out.write(rawMsg.getData());
			if (endDelim != null)
				out.write(AsciiUtil.ascii2bin(EnvExpander.expand(endDelim, props)));
			out.close();
			lastArchivePath = evalName;
		}
		catch (IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot write message to '{}'", evalName);
		}
	}
	
	void doPurge()
	{
		String parentDir = null;
		if (lastArchivePath != null)
		{
			File f = new File(lastArchivePath);
			if (f.exists())
				parentDir = f.getParent();
		}
		// If we have not yet written to an archive, try to get it from the
		// (possibly abstract) archiveFileName.
		if (parentDir == null)
		{
			int idx = archivePath.lastIndexOf('/');
			if (idx == -1)
				idx = archivePath.lastIndexOf('\\');
			if (idx > 0)
			{
				File f = new File(EnvExpander.expand(
					archivePath.substring(0, idx)));
				if (f.isDirectory())
					parentDir = f.getPath();
			}
		}
		if (parentDir == null)
		{
			log.info("Cannot purge archive dir. Parent unknown.");
			return;
		}
		log.info("purging old archive files for routing spec{} from directory '",
				 routingSpec.getName(), parentDir);
		File parent = new File(parentDir);
		if (!parent.isDirectory())
		{
			log.info("Cannot purge archive dir. Parent '{}' is not a directory.", parentDir);
			return;
		}
		File archiveFiles[] = parent.listFiles();
		if (archiveFiles == null || archiveFiles.length == 0)
		{
			log.trace("no files to purge.");
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.add(maxAge.getCalConstant(), -maxAge.getCount());
		long cutoff = cal.getTime().getTime();
		for(int idx = 0; idx < archiveFiles.length; idx++)
			if (archiveFiles[idx].lastModified() < cutoff)
			{
				log.info("deleting old archive '{}'", archiveFiles[idx].getPath());
				archiveFiles[idx].delete();
			}
	}
}


class PurgeOldFilesThread extends Thread
{
	long lastPurge = 0L;
	static final long purgeInterval = 3600000L;
	boolean shutdown = false;
	RawArchive rarc = null;
	
	PurgeOldFilesThread(RawArchive rarc)
	{
		this.rarc = rarc;
	}
	
	@Override
	public void run()
	{
		while(!shutdown)
		{
			try { sleep(10000L); }
			catch(InterruptedException ex) {}
			if (System.currentTimeMillis() - lastPurge > purgeInterval)
			{
				rarc.doPurge();
				lastPurge = System.currentTimeMillis();
			}
		}
	}
}