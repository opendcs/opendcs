/*
*  $Id$
*/
package decodes.dbimport;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.cmdline.*;

import decodes.tsdb.DbIoException;
import decodes.util.*;
import decodes.db.*;
import decodes.xml.XmlDatabaseIO;
import decodes.xml.TopLevelParser;

/**
Exports the entire database to an XML file.
Writes output to stdout.
*/
public class DbExport
{
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");

	static
	{
	}


	/**
	Usage: [java] decodes.import.DbExport <options>
	Options:
	<ul>
	  <li>-i         Export from 'installed' database (default is edit db)</li>
	  <li>-l logfile Specify log file. Default="./util.log"</li>
	</ul>
	@param args command line arguments
	*/
	public static void main(String args[])
		throws IOException, DecodesException
	{
		Logger.setLogger(new StderrLogger("DbExport"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);
		Logger.instance().log(Logger.E_INFORMATION,
			"DbExport Starting (" + DecodesVersion.startupTag()
			+ ") =====================");

		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio;

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
		db.dataSourceList.read();
		db.presentationGroupList.read();
		db.routingSpecList.read();
		
		LoadingAppDAI loadingAppDAO = dbio.makeLoadingAppDAO();
		try
		{
			db.loadingAppList.addAll(loadingAppDAO.listComputationApps(false));
		}
		catch(DbIoException ex)
		{
			Logger.instance().warning("Cannot list loading apps: " + ex);
			db.loadingAppList.clear();
		}
		finally
		{
			loadingAppDAO.close();
		}
		
		ScheduleEntryDAI scheduleEntryDAO = dbio.makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
			Logger.instance().debug1("Cannot export schedule entries. Not supported on this database.");
		else
		{
			try
			{
				db.schedEntryList.addAll(scheduleEntryDAO.listScheduleEntries(null));
			}
			catch(DbIoException ex)
			{
				Logger.instance().warning("Cannot list schedule entries: " + ex);
				db.schedEntryList.clear();
			}
			finally
			{
				scheduleEntryDAO.close();
			}
		}

		Logger lg = Logger.instance();
		Vector<Platform> platforms = db.platformList.getPlatformVector();

		// Completely read all platform data from the database
		for(Platform p : platforms)
		{
			try { p.read(); }
			catch(Exception ex)
			{
				String msg = "Error reading platform '"
					+ p.makeFileName() + "': " + ex;
				System.err.println(msg);
			}
		}
		
		// Likewise, completely read all presentation groups
		Vector<PresentationGroup> pgs = db.presentationGroupList.getVector();
		for(PresentationGroup pg : pgs)
		{
			try { pg.read(); }
			catch(Exception ex)
			{
				String msg = "Error reading presentation group '"
					+ pg.getDisplayName() + "': " + ex;
				System.err.println(msg);
			}
		}

		TopLevelParser.write(System.out, db);
	}
}

