/**
 * Open source software by Cove Software, LLC
 */
package decodes.dbimport;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.util.StderrLogger;
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
public class NosPlatformImport
	implements Runnable
{
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
		Logger.setLogger(new StderrLogger("DbImport"));
		
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
				Logger.instance().failure("Cannot write platform list file: "
					+ ex);
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
			String msg = "Cannot initialize DECODES Database: " + ex;
			Logger.instance().fatal(msg);
			System.err.println(msg);
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Process the named file.
	 * @param filename the file to process
	 */
	private void processFile(String filename)
	{
		try
		{
			LineNumberReader lnr = new LineNumberReader(
				new FileReader(filename));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;
				
				StringTokenizer st = new StringTokenizer(line);
				if (st.countTokens() != 4)
				{
					Logger.instance().warning(filename + "(" + lnr.getLineNumber()
						+ ") Incorrect number of tokens (" + st.countTokens() + 
						") -- line skipped.");
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
					Logger.instance().warning(filename + "(" + lnr.getLineNumber()
							+ ") Non-numeric channel (" + chans + 
							") -- line skipped.");
						continue;
				}
			}
			lnr.close();
		} 
		catch (IOException ex)
		{
			Logger.instance().failure("Cannot open '" + filename + "': " + ex);
		}
	}
	
	private void processLine(String siteNum, String format, String goesId, int chan, 
		String filename, int linenum)
	{
		Database ddb = Database.getDb();
		
		// Find matching config name that matches the "Format" column (NOS6MIN or NOSHOURLY).
		PlatformConfig config = ddb.platformConfigList.get(format);
		
		// If no matching config, fail with an error message.
		if (config == null)
		{
			Logger.instance().warning(filename + "(" + linenum
				+ ") No matching config (" + format + 
				") -- line skipped.");
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
				Logger.instance().warning(filename + "(" + linenum
					+ ") cannot create site (" + siteNum + 
					"): " + ex + " -- line skipped.");
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
			Logger.instance().debug1(filename + "(" + linenum
					+ ") Creating new platform for " + siteNum 
					+ " - " + goesId);
			platform = new Platform();
			platform.agency = agencyArg.getValue();
			newPlatform = true;
		}
		else
			Logger.instance().debug1(filename + "(" + linenum
				+ ") Found existing platform for " + siteNum);
			
		// In the platform record, associate with the given site, and config record.
		platform.setSite(site);
		Logger.instance().debug1(filename + "(" + linenum
				+ ") setting config to " + config.getName());
		platform.setConfig(config);
		platform.setConfigName(config.getName());
		
		// Also in the platform, create or replace the goes-self-timed transport medium
		// using the GOES ID and channel given.
		TransportMedium tm = platform.getTransportMedium(Constants.medium_GoesST);
		if (tm == null)
		{
			Logger.instance().debug1(filename + "(" + linenum
					+ ") Creating new GOES ST transport medium ");
			tm = new TransportMedium(platform, Constants.medium_GoesST,
				goesId);
			platform.transportMedia.add(tm);
		}
		else
			Logger.instance().debug1(filename + "(" + linenum
					+ ") Found existing GOES ST transport medium");

		tm.scriptName = "st";
		tm.channelNum = chan;
		tm.setMediumId(goesId);
		
		// If ANOTHER platform has a matching GOES ID and channel, disable it by removing the TM.
		if (oldTmPlatform != null && oldTmPlatform != platform)
		{
			Logger.instance().info(filename + "(" + linenum
				+ ") Disabling old platform with matching ID " + goesId
				+ " for site " + siteNum);
			tm = oldTmPlatform.getTransportMedium(Constants.medium_GoesST);
			oldTmPlatform.transportMedia.remove(tm);
			try
			{
				oldTmPlatform.write();
			}
			catch (DatabaseException ex)
			{
				Logger.instance().warning(filename + "(" + linenum
					+ ") cannot write old Platform (" + goesId + 
					"): " + ex);
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
			Logger.instance().warning(filename + "(" + linenum
				+ ") cannot write platform (" + goesId + 
				"): " + ex);
		}
	}
	
}
