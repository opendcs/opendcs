/**
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
 * Copyright 2012 Sutron Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.dbimport;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

/**
 * Special file-import for NOS (National Ocean Service)
 *
 * Usage: nosPlatformImport [options] <filename> [<filename> ...]
 *
 * Accept text files with lines like this:
 *     # NOS-Site-Number  Format  GOES-ID  Self-Timed-Channel
 *     1234567          NOS6MIN  CE632102  49
 *
 * Do the following for each line:
 * - Find matching config name that matches the "Format" column (NOS6MIN or NOSHOURLY).
 *   If no matching config, fail with an error message.
 * - Find matching site with NOS site name type. If none exists, create one.
 * - Find matching platform for the NOS site #. If none exists, create one.
 * - In the platform record, associate with the given site, and config record.
 * - Also in the platform, create or replace the goes-self-timed transport medium
 *   using the GOES ID and channel given.
 * - If ANOTHER platform has a matching GOES ID and channel, disable it by removing the TM.
 * - Write the new/modified platform record.
 *
 * @author mmaloney
 */
public class NosPlatformImport implements Runnable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
	private StringToken agencyArg = new StringToken("A", "default agency code",
		"", TokenOptions.optSwitch, "nos");
	private StringToken dbLocArg = new StringToken("E",
		"Explicit Database Location", "", TokenOptions.optSwitch, "");
	private StringToken fileArgs = new StringToken("", "Input Files", "",
		TokenOptions.optArgument|TokenOptions.optMultiple
		|TokenOptions.optRequired, "");
	private int numPlatformsWritten = 0;


	public NosPlatformImport()
	{
		cmdLineArgs.addToken(agencyArg);
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(fileArgs);
	}

	public static void main(String args[])
	{
		NosPlatformImport npi = new NosPlatformImport();
		npi.parseArgs(args);
		npi.run();
	}

	private void parseArgs(String args[])
	{
		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);
	}

	@Override
	public void run()
	{
		if (!loadDatabase())
			return;

		numPlatformsWritten = 0;
		for(int idx = 0; idx < fileArgs.NumberOfValues(); idx++)
			processFile(fileArgs.getValue(idx));
		if (numPlatformsWritten > 0)
			try
			{
				Database.getDb().platformList.write();
			}
			catch (DatabaseException ex)
			{
				log.atError().setCause(ex).log("Cannot write platform list file.");
			}
	}

	/**
	 * Initialize the DECODES database. Preload all configs, sites, & platforms.
	 */
	private boolean loadDatabase()
	{
		try
		{
			DecodesInterface.initDecodes(null);
			DecodesInterface.initializeForEditing();
			return true;
		}
		catch (DecodesException ex)
		{
			log.atError().setCause(ex).log("Cannot initialize DECODES Database.");
			return false;
		}
	}

	/**
	 * Process the named file.
	 * @param filename the file to process
	 */
	private void processFile(String filename)
	{
		try(FileReader fr = new FileReader(filename);
			LineNumberReader lnr = new LineNumberReader(fr);)
		{
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;

				StringTokenizer st = new StringTokenizer(line);
				if (st.countTokens() != 4)
				{
					log.warn("{}({}) Incorrect number of tokens ({}) -- line skipped.",
							 filename, lnr.getLineNumber(), st.countTokens());
					continue;
				}
				String siteNum = st.nextToken();
				String format = st.nextToken();
				String goesId = st.nextToken();
				String chans = st.nextToken();
				try
				{
					int chan = Integer.parseInt(chans);
					processLine(siteNum, format, goesId, chan,
						filename, lnr.getLineNumber());
				}
				catch(NumberFormatException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{}({}) Non-numeric channel ({}) -- line skipped.",
					   		filename, lnr.getLineNumber(), chans);
						continue;
				}
			}
		}
		catch (IOException ex)
		{
			log.atError().setCause(ex).log("Cannot open '{}'", filename);
		}
	}

	private void processLine(String siteNum, String format, String goesId, int chan, String filename, int linenum)
	{
		Database ddb = Database.getDb();

		// Find matching config name that matches the "Format" column (NOS6MIN or NOSHOURLY).
		PlatformConfig config = ddb.platformConfigList.get(format);

		// If no matching config, fail with an error message.
		if (config == null)
		{
			log.warn("{}({}) No matching config ({}) -- line skipped.", filename, linenum, format);
			return;
		}

		// Find matching site with NOS site name type. If none exists, create one.
		Site site = ddb.siteList.getSite(Constants.snt_NOS, siteNum);
		if (site == null)
		{
			site = new Site();
			site.addName(new SiteName(site, Constants.snt_NOS, siteNum));
			ddb.siteList.addSite(site);
			try
			{
				site.write();
			}
			catch (DatabaseException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("{}({}) cannot create site ({}) -- line skipped.", filename, linenum, siteNum);
				return;
			}
		}

		// Find matching platform for the NOS site #. If none exists, create one.
		Platform platform = ddb.platformList.findPlatform(site, null);
		Platform oldTmPlatform = ddb.platformList.findPlatform(
				Constants.medium_GoesST, goesId, null);
		boolean newPlatform = false;
		if (platform == null)
		{
			log.debug("{}({}) Creating new platform for {} - {}", filename, linenum, siteNum, goesId);
			platform = new Platform();
			platform.agency = agencyArg.getValue();
			newPlatform = true;
		}
		else
		{
			log.debug("{}({}) Found existing platform for {}", filename, linenum, siteNum);
		}

		// In the platform record, associate with the given site, and config record.
		platform.setSite(site);
		log.debug("{}({}) setting config to {}", filename, linenum, config.getName());
		platform.setConfig(config);
		platform.setConfigName(config.getName());

		// Also in the platform, create or replace the goes-self-timed transport medium
		// using the GOES ID and channel given.
		TransportMedium tm = platform.getTransportMedium(Constants.medium_GoesST);
		if (tm == null)
		{
			log.debug("{}({}) Creating new GOES ST transport medium ", filename, linenum);
			tm = new TransportMedium(platform, Constants.medium_GoesST,
				goesId);
			platform.transportMedia.add(tm);
		}
		else
		{
			log.debug("{}({}) Found existing GOES ST transport medium", filename, linenum);
		}

		tm.scriptName = "st";
		tm.channelNum = chan;
		tm.setMediumId(goesId);

		// If ANOTHER platform has a matching GOES ID and channel, disable it by removing the TM.
		if (oldTmPlatform != null && oldTmPlatform != platform)
		{
			log.info("{}({}) Disabling old platform with matching ID {} for site {}",
					 filename, linenum, goesId, siteNum);
			tm = oldTmPlatform.getTransportMedium(Constants.medium_GoesST);
			oldTmPlatform.transportMedia.remove(tm);
			try
			{
				oldTmPlatform.write();
			}
			catch (DatabaseException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("{}({}) cannot write old Platform ({})", filename, linenum, goesId);
			}
		}

		// Write the new/modified platform record.
		try
		{
			if (newPlatform)
				ddb.platformList.add(platform);
			platform.write();
			numPlatformsWritten++;
		}
		catch (DatabaseException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("{}({}) cannot write platform ({}", filename, linenum, goesId);
		}
	}
}
