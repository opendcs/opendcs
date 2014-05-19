/*
*  $Id$
*/
package decodes.dbimport;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.TextUtil;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;

/**
This program retrieves complete, expanded Platform records from the DECODES
database and writes them to standard output in XML format.
*/
public class PlatformExport
{
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
	static StringToken netlistArg = new StringToken("n", "Network-List",
		"", TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static StringToken lrgsnl = new StringToken("f", "LRGS Network-List style",
			"", TokenOptions.optSwitch, "");
	static StringToken siteArg = new StringToken("s", "Site-Name",
		"", TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static BooleanToken allArg = new BooleanToken("a", "All-Platforms", 
		"", TokenOptions.optSwitch, false);
	static StringToken configArg = new StringToken("c", "Config-Name",
		"", TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static StringToken dbLocArg = new StringToken("E", 
		"Explicit Database Location", "", TokenOptions.optSwitch, "");
	static StringToken newerThanFileArg = new StringToken("N", 
		"Newer Than File", "", TokenOptions.optSwitch, "");
	static StringToken platOwnerArg = new StringToken("O", "Platform Owner", "",
		TokenOptions.optSwitch, "");
	static StringToken platNameFileArg = new StringToken("p", "File of Platform Names", "",
		TokenOptions.optSwitch, "");

	static
	{
		cmdLineArgs.addToken(netlistArg);
		cmdLineArgs.addToken(siteArg);
		cmdLineArgs.addToken(allArg);
		cmdLineArgs.addToken(configArg);
		dbLocArg.setType("dirname");
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(newerThanFileArg);
		cmdLineArgs.addToken(platOwnerArg);
		cmdLineArgs.addToken(lrgsnl);
		cmdLineArgs.addToken(platNameFileArg);
	}


	/**
	Usage: [java] decodes.import.PlatformExport options
	<p>
	Options:
	<ul>
	  <li>-n netlist     Export platforms specified by network list.</li>
	  <li>-f lrgs netlist
	  Export platforms specified by an lrgs network list.</li>
	  <li>-s site        Export platform corresponding to specific site.</li>
	  <li>-a             Export all platforms</li>
	  <li>-c configname  Export platforms using a specific configuration.</li>
	  <li>-i             Export from installed database (default=editable)</li>
	</ul>
	@param args command line arguments.
	*/
	public static void main(String args[])
		throws IOException, DecodesException
	{
		Logger.setLogger(new StderrLogger("PlatformExport"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio;

		String dbloc = dbLocArg.getValue();
		if (dbloc.length() > 0)
			dbio = DatabaseIO.makeDatabaseIO(settings.DB_XML, dbloc);
		else
			dbio = DatabaseIO.makeDatabaseIO(
				settings.editDatabaseTypeCode, settings.editDatabaseLocation);
		
		// Standard Database Initialization for all Apps:
		Site.explicitList = false; // YES Sites automatically added to SiteList
		db.setDbIo(dbio);

		// Initialize standard collections:
		db.enumList.read();
		db.dataTypeSet.read();
		db.engineeringUnitList.read();
		db.siteList.read();
		db.equipmentModelList.read();
		db.platformConfigList.read();
		db.platformList.read();
		db.networkListList.read();

		Logger lg = Logger.instance();
		Vector<Platform> platforms;

		long newerThanMsec = 0L;
		String fn = newerThanFileArg.getValue();
		if (fn != null && fn.length() > 0)
		{
			File f = new File(fn);
			if (f.exists())
				newerThanMsec = f.lastModified();	
			else
				Logger.instance().info("File '" + fn 
					+ "' doesn't exist, so accepting all platform dates.");
		}

		if (allArg.getValue())
		{
			platforms = db.platformList.getPlatformVector();
		}
		else
		{	//In here verify if we have the -f argument which means that is
			//a .nl file - Use LRGS object to parse the file.
			//Platforms will be created from the list of DCPs contain within
			//this .nl file
			platforms = new Vector<Platform>();
			if (lrgsnl.getValue() != null && !lrgsnl.getValue().equals(""))
			{
				lrgs.common.NetworkList lnl =
					new lrgs.common.NetworkList(new File(lrgsnl.getValue()));
				if (lnl != null)
				{
					//Copy legacy netlist data into DECODES network list.
					for(Iterator it = lnl.iterator(); it.hasNext(); )
					{
						lrgs.common.NetworkListItem lnli =
							(lrgs.common.NetworkListItem)it.next();
						Platform p = db.platformList.getPlatform(
								Constants.medium_Goes, 
								lnli.addr.toString(), new Date());
						if (p == null)
						{
							p = db.platformList.getPlatform(
									Constants.medium_GoesST, 
									lnli.addr.toString(), new Date());
						}
						if (p == null)
						{
							lg.log(Logger.E_WARNING,
							"No such platform '" + lnli.addr.toString() +
							"' -- skipped.");
							continue;
						}

						if (newerThanMsec > p.lastModifyTime.getTime())
							continue;

						if (!platforms.contains(p))
							platforms.add(p);
					}	
				}
				else
				{
					lg.log(Logger.E_FAILURE,
							"No such network list '" + lrgsnl.getValue() +
							"' -- skipped.");
				}
			}
			else
			{
				for(int i = 0; i < netlistArg.NumberOfValues(); i++)
				{
					String s = netlistArg.getValue(i);
					if (s.length() == 0)
						continue;

					NetworkList nl = db.networkListList.find(s);
					if (nl == null)
					{
						lg.log(Logger.E_FAILURE,
							"No such network list '" + s + "' -- skipped.");
						continue;
					}
		
					for(Iterator it = nl.iterator(); it.hasNext(); )
					{
						NetworkListEntry nle = (NetworkListEntry)it.next();

						Platform p = db.platformList.getPlatform(
						nl.transportMediumType, nle.transportId, new Date());

						if (p == null)
						{
							lg.log(Logger.E_WARNING,
								"No such platform '" + nle.transportId+
								"' -- skipped.");
							continue;
						}

						if (newerThanMsec > p.lastModifyTime.getTime())
							continue;

						if (!platforms.contains(p))
							platforms.add(p);
					}
				}
			}

			for(int i = 0; i < siteArg.NumberOfValues(); i++)
			{
				String s = siteArg.getValue(i);
				if (s.length() == 0)
					continue;

				SiteName sn = new SiteName(null, 
					settings.siteNameTypePreference, s);
				Site site = db.siteList.getSite(sn);

				// find site 's'
				if (site == null)
				{
					lg.log(Logger.E_FAILURE,
						"No such site '" + s + "' -- skipped.");
					continue;
				}
				Vector<Platform> pvec = db.platformList.getPlatforms(site);

				if (pvec.size() == 0)
				{
					lg.log(Logger.E_WARNING,
						"No platform for site '" + s + "' -- skipped.");
					continue;
				}
				for(Platform p : pvec)
				{
					if (p.lastModifyTime != null
					 && newerThanMsec > p.lastModifyTime.getTime())
						continue;

					if (!platforms.contains(p))
						platforms.add(p);
				}
			}

			for(int i = 0; i < configArg.NumberOfValues(); i++)
			{
				String s = configArg.getValue(i);
				if (s.length() == 0)
					continue;

				for(Iterator<Platform> it = db.platformList.iterator(); it.hasNext(); )
				{
					Platform p = it.next();
					if (newerThanMsec > p.lastModifyTime.getTime())
						continue;
					String pcn = p.getConfigName();
					if (pcn != null && s.equalsIgnoreCase(pcn) 
					 && !platforms.contains(p))
						platforms.add(p);
				}
			}
			
			String pnfArg = platNameFileArg.getValue();
			if (pnfArg != null && pnfArg.length() > 0)
			{
				File pnf = new File(pnfArg);
				if (!pnf.canRead())
				{
					System.err.println("Cannot read platform list from '"
						+ pnfArg + "'");
					System.exit(1);
				}
				LineNumberReader lnr = new LineNumberReader(
					new FileReader(pnf));
				String line = null;
				while((line = lnr.readLine()) != null)
				{
					line = line.trim();
					if (line.length() == 0)
						continue;
					int hyphen = line.indexOf('-');
					String siteName = line;
					String designator = null;
					if (hyphen > 0)
					{
						siteName = line.substring(0, hyphen);
						designator = line.substring(hyphen+1);
					}
				  next_platform:
					for(Iterator<Platform> pit = db.platformList.iterator();
						pit.hasNext(); )
					{
						Platform p = pit.next();
						if (p.site == null)
							continue;
						if (!TextUtil.strEqualIgnoreCase(designator, 
							p.getPlatformDesignator()))
							continue;
						for(Iterator<SiteName> sit = p.site.getNames();
							sit.hasNext(); )
						{
							SiteName sn = sit.next();
							if (TextUtil.strEqualIgnoreCase(sn.getNameValue(),
								siteName))
							{
								platforms.add(p);
								continue next_platform;
							}
						}
					}
				}
				lnr.close();
			}
		}

		String newOwner = platOwnerArg.getValue();
		if (newOwner.length() == 0) newOwner = null;

		XmlOutputStream xos = new XmlOutputStream(System.out,
			XmlDbTags.Database_el);
		xos.writeXmlHeader();
		xos.startElement(XmlDbTags.Database_el);
		//Database newDb = new decodes.db.Database();
		for(Iterator<Platform> it = platforms.iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			p.read();   // Read all platform data from the database
			if (newOwner != null)
				p.setAgency(newOwner);
			PlatformParser pp = new PlatformParser(p);
			pp.writeXml(xos);
			
			//newDb.platformList.add(p);
		}
		xos.endElement(XmlDbTags.Database_el);

		// Output platform data as XML.
		//Database.setDb(newDb);
		//TopLevelParser.write(System.out, newDb);
	}
}

