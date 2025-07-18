/**
 * Copyright 2025 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import lrgs.common.DcpAddress;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.opentsdb.Interval;

import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.SimpleDataSource;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.TeeLogger;
import ilex.cmdline.*;
import decodes.sql.PlatformListIO;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.util.*;
import decodes.db.*;
import decodes.launcher.Profile;
import decodes.xml.DataTypeEquivalenceListParser;
import decodes.xml.ElementFilter;
import decodes.xml.EngineeringUnitParser;
import decodes.xml.UnitConverterParser;
import decodes.xml.XmlDatabaseIO;
import decodes.xml.XmlDbTags;
import decodes.xml.TopLevelParser;
import decodes.xml.EnumParser;

/**
This class is a main program to import XML files into your editable
DECODES database.
*/
public class DbImport
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(DbImport.class);

	/**
	This program imports XML files into the database.
	Usage: [java] decodes.import.DbImport options files...
	<p>
	Options:
	<ul>
	  <li>-v    Validate only: show conflicts but do not import anything.</li>
      <li>-o    Keep old records when there is a conflict.
                (default is to overwrite old records with new).</li>
	  <li>-a    AutoInstall option. Records are flagged as production and
	            installed to the production database after import.</li>
	</ul>
	*/
	public static void main(String args[])
		throws IOException, DecodesException,
		       SAXException, ParserConfigurationException
	{
		BooleanToken validateOnlyArg = new BooleanToken("v",
			"Validate Only", "", TokenOptions.optSwitch, false);
		BooleanToken keepOldArg = new BooleanToken("o",
			"Keep old records on conflict", "", TokenOptions.optSwitch, false);
		StringToken agencyArg = new StringToken("A", "default agency code",
			"", TokenOptions.optSwitch, "");
		StringToken platOwnerArg = new StringToken("O", "Platform Owner", "",
			TokenOptions.optSwitch, "");
		StringToken platDesigArg = new StringToken("G",
			"Platform Designator (for platforms with no designator in XML file)", "",
			TokenOptions.optSwitch, "");
		StringToken dbLocArg = new StringToken("E",
			"Explicit Database Location", "", TokenOptions.optSwitch, "");
		StringToken fileArgs = new StringToken("", "XmlFiles", "",
			TokenOptions.optArgument|TokenOptions.optMultiple
			|TokenOptions.optRequired, "");
		StringToken pdtFilePath = new StringToken("t",
				"pdt file (full file system path, " +
				"fill descriptions if empty)", "",
				TokenOptions.optSwitch,
				"");
		BooleanToken noConfigs = new BooleanToken("C",
			"Link platforms to existing configs, ignore configs in XML file", "",
			TokenOptions.optSwitch, false);
		BooleanToken overwriteDb = new BooleanToken("W",
			"Overwrite contents of current database with imported image", "",
			TokenOptions.optSwitch, false);
		BooleanToken overwriteDbConfirm = new BooleanToken("y",
			"Confirm overwrite (otherwise program will ask for confirmation.)", "",
			TokenOptions.optSwitch, false);
		BooleanToken reflistArg = new BooleanToken("r",
			"Deprecated write-reflist arg. (Reflists are always written.)", "",
			TokenOptions.optSwitch, false);
		BooleanToken allowHistoricalArg = new BooleanToken("H",
			"Allow import of historical versions. (default=ignore.)", "",
			TokenOptions.optSwitch, false);
		BooleanToken platformRelatedOnlyArg = new BooleanToken("p",
			"Import ONLY platform-related elements. (default=import all.)", "",
			TokenOptions.optSwitch, false);
			Logger.setLogger(new StderrLogger("DbImport"));
		CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");

		cmdLineArgs.addToken(validateOnlyArg);
		cmdLineArgs.addToken(keepOldArg);
		cmdLineArgs.addToken(agencyArg);
		cmdLineArgs.addToken(platOwnerArg);
		cmdLineArgs.addToken(platDesigArg);
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(pdtFilePath);
		cmdLineArgs.addToken(fileArgs);
		cmdLineArgs.addToken(noConfigs);
		cmdLineArgs.addToken(allowHistoricalArg);
		cmdLineArgs.addToken(overwriteDb);
		cmdLineArgs.addToken(overwriteDbConfirm);
		cmdLineArgs.addToken(reflistArg);
		cmdLineArgs.addToken(platformRelatedOnlyArg);
		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);
		// Send WARNING & higher messages to stderr, file logger set by args.
		Logger fileLogger = Logger.instance();
		String procname = fileLogger.getProcName();
		Logger stderrLogger = new StderrLogger(procname);
		stderrLogger.setMinLogPriority(Logger.E_WARNING);
		stderrLogger.setUseDateTime(false);
		Logger teeLogger = new TeeLogger(procname, fileLogger, stderrLogger);
		Logger.setLogger(teeLogger);

		log.info("DbImport Starting ({}) ======================", DecodesVersion.startupTag());

		XmlDatabaseIO.writeDependents = false;

		if (noConfigs.getValue())
		{
			Platform.configSoftLink = true;
		}

		if(overwriteDb.getValue())
		{
			if (!overwriteDbConfirm.getValue())
			{
				System.out.print("Are you sure you want to replace "
					+ (platformRelatedOnlyArg.getValue() ? "ALL Platform Related Records"
					   : "ALL DECODES records")
					+ " from the database ' (y/n)?");
				String answer = System.console().readLine();
				if (!answer.toLowerCase().equals("y"))
					System.exit(1);
			}
		}
		List<String> files = new ArrayList<>();
		for(int i = 0; i < fileArgs.NumberOfValues(); i++)
		{
			String f = fileArgs.getValue(i);
			if (f != null && !f.isEmpty())
			{
				files.add(f);
			}

		}

		final String dbLoc = dbLocArg.getValue().length() == 0 ? null : dbLocArg.getValue();
		final String pdtFile = pdtFilePath.getValue().length() == 0 ? null : pdtFilePath.getValue();
		final String newDesignatorArg = platDesigArg.getValue().length() == 0 ? null : platDesigArg.getValue();
		final String defAgency = agencyArg.getValue().length() == 0 ? null : agencyArg.getValue();

		final String newOwner = platOwnerArg.getValue().length() == 0 ? null : platOwnerArg.getValue();

		DbImport dbImport = new DbImport(cmdLineArgs.getProfile(), dbLoc, validateOnlyArg.getValue(),
										 keepOldArg.getValue(), platformRelatedOnlyArg.getValue(), overwriteDb.getValue(),
										 allowHistoricalArg.getValue(), pdtFile, newDesignatorArg, defAgency,
										 newOwner, files);
		dbImport.importDatabase();
	}

	public void importDatabase() throws DatabaseException
	{
		try
		{
			loadCurrentDatabase();
			log.trace("after loadCurrentDatabase");
			theDb.printSizesToLog("--- Current Database ---");
			if (overwriteDb)
			{
				this.deleteCurrentDatabase();
				log.trace("after deleteCurrentDatabase");
			}
			initStageDb();
			log.trace("after initStageDb");
			
			readXmlFiles();
			log.trace("after readXmlFiles");
			stageDb.printSizesToLog("--- Staging Database (in memory xml) ---");
			if (log.isTraceEnabled())
			{
				log.trace("After readXmlFiles, there are {} platforms to import:", this.stageDb.platformList.size());
				for(Iterator<Platform> pit = this.stageDb.platformList.iterator(); pit.hasNext(); )
				{
					Platform p = pit.next();
					log.trace("Platform '{}'", p.makeFileName());
					for (PlatformSensor ps : p.platformSensors)
					{
						log.trace("   Sensor {}: actualSite={}",
								ps.sensorNumber, (ps.site == null ? "null" : ps.site.getPreferredName()));
					}
				}
			}

			DbMerge merger = new DbMerge.Builder(theDb, stageDb)
			.overwriteDb(overwriteDb)
			.validateOnly(validateOnly)
			.keepOld(keepOld)
			.newDesignator(newDesignator)
			.build();
		
			merger.merge();

			theDb.printSizesToLog("after mergeStageToTheDb");
			if (!validateOnly)
			{
				normalizeTheDb(merger.getImmutableNewObjects());
				theDb.printSizesToLog("after normalizeTheDb");
				writeTheChanges(merger.getWritePlatformList(),merger.getImmutableNewObjects());
				theDb.printSizesToLog("after writeTheChanges");
			}

		}
		catch (ParserConfigurationException | SAXException ex)
		{
			throw new DatabaseException("Unable to parse or process input XML data.", ex);
		}
		catch (IOException ex)
		{
			throw new DatabaseException("Unable to read or write required data", ex);
		}
	}

	Database theDb;
	DatabaseIO theDbio;
	boolean validateOnly;
	boolean keepOld;
	boolean platformRelatedOnly;
	boolean overwriteDb;
	boolean allowHistorical;
	final String newDesignator;
	final String newPlatformOwner;
	final String defaultAgency;
	Database stageDb;
	XmlDatabaseIO stageDbio;  // For reading the input files.
	TopLevelParser topParser; // Top level XML parser.
	final List<String> files;
	private Pdt pdt = null;

	public DbImport(Profile profile, String dbLoc, boolean validateOnly, boolean keepOld, boolean overwriteDb,
		     boolean platformRelatedOnly, boolean allowHistorical, String pdtFilePath, String newDesignator,
			 String defaultAgency, String newPlatformOwner, List<String> files) throws DatabaseException
	{

		this.files = files;
		this.platformRelatedOnly = platformRelatedOnly;
		this.overwriteDb = overwriteDb;
		this.validateOnly = validateOnly;
		this.keepOld = keepOld;
		this.allowHistorical = allowHistorical;
		this.newDesignator = newDesignator;
		this.defaultAgency = defaultAgency;
		this.newPlatformOwner = newPlatformOwner;
		// Construct the database and the interface specified by properties.
		try
		{
			DecodesSettings settings = DecodesSettings.instance();
			// unfortunately we still need to use the global decodes settings here.
			settings.loadFromProfile(profile);
			OpenDcsDatabase databases = DatabaseService.getDatabaseFor(null, settings);				
			theDb = databases.getLegacyDatabase(Database.class).get();
			Database.setDb(theDb);
			theDbio = theDb.getDbIo();
		}
		catch (IOException ex)
		{
			throw new DatabaseException("Unable to initialize target database.");
		}

		// Standard Database Initialization for all Apps:
		Site.explicitList = false; // YES Sites automatically added to SiteList

		if (pdtFilePath != null)
		{
			initializePdt(pdtFilePath);
		}

		if (overwriteDb)
		{
			if (validateOnly)
			{
				throw new IllegalStateException("Overwrite Flag -W is inconsistent with validate-only -v flag.");
			}
			if (keepOld)
			{
				throw new IllegalStateException("Overwrite Flag -W is inconsistent with keep-old -o flag.");
			}

		}

		PlatformListIO.isDbImport = true;
	}

	/**
	 * Reads the entire CURRENT editable database into memory in order to prepare for a merge.
	 */
	private void loadCurrentDatabase()
		throws DatabaseException
	{
		// Read all collections:
		theDb.enumList.read();
		theDb.dataTypeSet.read();
		theDb.engineeringUnitList.read();
		theDb.siteList.read();
		theDb.equipmentModelList.read();
		theDb.platformConfigList.read();
		theDb.platformList.read();
		theDb.networkListList.read();
		theDb.dataSourceList.read();
		theDb.routingSpecList.read();
		LoadingAppDAI loadingAppDAO = theDbio.makeLoadingAppDAO();
		try
		{
			theDb.loadingAppList.addAll(loadingAppDAO.listComputationApps(false));
		}
		catch (DbIoException ex)
		{
			log.warn("Cannot list loading apps.", ex);
		}
		finally
		{
			loadingAppDAO.close();
		}
		ScheduleEntryDAI scheduleEntryDAO = theDbio.makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
		{
			log.debug("Cannot import schedule entries. Not supported on this database.");
		}
		else
		{
			try
			{
				theDb.schedEntryList.addAll(scheduleEntryDAO.listScheduleEntries(null));
			}
			catch(DbIoException ex)
			{
				log.warn("Cannot list schedule entries.", ex);
				theDb.schedEntryList.clear();
			}
			finally
			{
				scheduleEntryDAO.close();
			}
		}
	}

	/**
	 * Called when the Overwrite CURRENT database option is used.
	 */
	public void deleteCurrentDatabase()
		throws DatabaseException
	{
		log.info("Since the OVERWRITE option was given, deleting current database.");
		if (!platformRelatedOnly)
		{
			log.info("ONLY deleting platform-related entities.");
			ScheduleEntryDAI scheduleEntryDAO = theDbio.makeScheduleEntryDAO();
			if (scheduleEntryDAO != null)
			{
				try
				{
					for(ScheduleEntry se : theDb.schedEntryList)
						scheduleEntryDAO.deleteScheduleEntry(se);
				}
				catch (DbIoException ex)
				{
					log.warn("Cannot delete schedule entry.", ex);
				}
				finally
				{
					scheduleEntryDAO.close();
				}
				theDb.schedEntryList.clear();
			}
			for(Iterator<RoutingSpec> rsit = theDb.routingSpecList.iterator(); rsit.hasNext(); )
			{
				RoutingSpec rs = rsit.next();
				theDbio.deleteRoutingSpec(rs);
			}
			theDb.routingSpecList.clear();

			for(Iterator<DataSource> dsit = theDb.dataSourceList.iterator(); dsit.hasNext(); )
			{
				DataSource ds = dsit.next();
				theDbio.deleteDataSource(ds);
			}
			theDb.dataSourceList.clear();

			// NOTE: Never delete loading apps -- too many dependencies outside of DECODES.
			// (block of code for removing loading apps removed 10/26/2022)
		}

		for(NetworkList nl : theDb.networkListList.getList())
		{
			theDbio.deleteNetworkList(nl);
		}
		theDb.networkListList.clear();

		for(Platform p : theDb.platformList.getPlatformVector())
		{
			theDbio.deletePlatform(p);
		}
		theDb.platformList.clear();

		// NOTE: Never delete SITE records. Too many dependencies outside of DECODES.
		// (block of code for removing sites removed 10/26/2022)

		for(PlatformConfig pc : theDb.platformConfigList.values())
		{
			theDbio.deleteConfig(pc);
		}
		theDb.platformConfigList.clear();

		for(EquipmentModel em : theDb.equipmentModelList.values())
		{
			theDbio.deleteEquipmentModel(em);
		}
		theDb.equipmentModelList.clear();

		// Note: EU list and conversions are always assumed to be complete.
		// Therefore just empty the Java collections. When new ones are read
		// they will automatically delete all the old ones.
		if (!platformRelatedOnly)
		{
			theDb.unitConverterSet.clear();
			theDb.engineeringUnitList.clear();

			// Likewise DataTypes are read/written as a set. Just clear the
			// Java collection and only the new ones will survive.
			theDb.dataTypeSet.clear();

			// Likewise again for enumerations.
			theDb.enumList.clear();
		}
	}

	/**
	 * Initialize the staging database by copying 'setup' elements from the CURRENT
	 * database. XML files will subsequently be read into the staging database.
	 */
	private void initStageDb()
		throws SAXException, ParserConfigurationException, DatabaseException
	{
		log.debug("Initializing the staging database.");
		stageDb = new decodes.db.Database(false);
		Database.setDb(stageDb);
		javax.sql.DataSource ds = new SimpleDataSource("jdbc:xml:", "", "");
		stageDbio = new XmlDatabaseIO(ds, null);
		stageDb.setDbIo(stageDbio);
		topParser = stageDbio.getParser();

		if (!overwriteDb)
		{
			// Engineering Units, data types, and unit conversions are automatically
			// merged when the XML files are read. Simply point the stage sets to
			// the CURRENT database sets.
			stageDb.engineeringUnitList = theDb.engineeringUnitList;
			stageDb.dataTypeSet = theDb.dataTypeSet;
			stageDb.unitConverterSet = theDb.unitConverterSet;

			log.info("Copying existing enumerations into staging db.");
			// Copy the dataType, enums, & other 'setup' info into stageDb.
			for(Iterator<DbEnum> it = theDb.enumList.iterator(); it.hasNext(); )
			{
				DbEnum en = it.next();
				DbEnum stageEnum = new decodes.db.DbEnum(en.enumName);
				stageEnum.forceSetId(en.getId());
				stageEnum.setDefault(en.getDefault());
				for(Iterator<EnumValue> vit = en.iterator(); vit.hasNext(); )
				{
					EnumValue ev = vit.next();
					EnumValue stageEv = stageEnum.replaceValue(ev.getValue(),
						ev.getDescription(), ev.getExecClassName(), ev.getEditClassName());
					stageEv.setSortNumber(ev.getSortNumber());
				}
			}
		}
		// BUT ... if I am overwriting, I want to keep the EU list, Data Type list
		// and Unit Converter lists completely separate to avoid merge.
		else
		{
			log.trace("EU and DT lists will remain separate.");
		}

	}

	/** Read the XML files into the staging database. */
	private void readXmlFiles()
		throws IOException, DatabaseException
	{
		//enumsModified = false;
		EnumParser.enumParsed = false;
		EngineeringUnitParser.engineeringUnitsParsed = false;
		UnitConverterParser.unitConvertersParsed = false;
		DataTypeEquivalenceListParser.dtEquivalencesParsed = false;

		// Read all the files into a new 'staging' database.
		for(String s: files)
		{
			log.info("Processing '{}'", s);
			DatabaseObject ob = null;

			// If -p argument is used set a filter to skip non-platform-related elements.
			if (platformRelatedOnly)
			{
				topParser.setElementFilter(
					new ElementFilter()
						{
							@Override
							public boolean acceptElement(String elementName)
							{
								return elementName.equalsIgnoreCase(XmlDbTags.Platform_el)
									|| elementName.equalsIgnoreCase(XmlDbTags.NetworkList_el)
									|| elementName.equalsIgnoreCase(XmlDbTags.PlatformConfig_el)
									|| elementName.equalsIgnoreCase(XmlDbTags.EquipmentModel_el)
									|| elementName.equalsIgnoreCase(XmlDbTags.Site_el);
							}
						});
			}

			try
			{
				ob = topParser.parse(new File(s));
			}
			catch(org.xml.sax.SAXException ex)
			{
				throw new IOException("Unable to process " + s, ex);
			}

			// Some file entity types must be explicitly added to the database.
			// Some are implicitely added during the XML read.
			if (ob instanceof Platform)
			{
				Platform p = (Platform)ob;
				// Ignore historical versions unless the -H arg was given
				if (p.expiration == null || allowHistorical)
				{
					stageDb.platformList.add((Platform)ob);
				}
			}
			else if (ob instanceof Site)
			{
				stageDb.siteList.addSite((Site)ob);
			}
			else if (ob instanceof RoutingSpec)
			{
				stageDb.routingSpecList.add((RoutingSpec)ob);
			}
			else if (ob instanceof NetworkList)
			{
				stageDb.networkListList.add((NetworkList)ob);
			}
			else if (ob instanceof PresentationGroup)
			{
				stageDb.presentationGroupList.add((PresentationGroup)ob);
			}
			else if (ob instanceof ScheduleEntry)
			{
				stageDb.schedEntryList.add((ScheduleEntry)ob);
			}
			else if (ob instanceof CompAppInfo)
			{
				stageDb.loadingAppList.add((CompAppInfo)ob);
			}
			else if (ob instanceof PlatformList)
			{
				log.error("Cannot import PlatformList files! '{}'", s);
				throw new DatabaseException("Cannot import PlatformList XML files!");
			}
		}

		/*
		  XML Platforms file may have contained PlatformConfig, Site, and
		  EquipementModel objects. Copy them into the stage-db collections.
		*/
		for(Iterator<Platform> it = stageDb.platformList.iterator(); it.hasNext(); )
		{
			Platform plat = it.next();

			// The PlatformID needs to be cleared so it won't conflict
			// with an ID in the real editable database.
			plat.clearId();

			PlatformConfig pc = plat.getConfig();
			if (pc != null)
			{
				stageDb.platformConfigList.add(pc);
				if (pc.equipmentModel != null)
				{
					stageDb.equipmentModelList.add(pc.equipmentModel);
				}
			}

			if (plat.getSite() != null)
			{
				try
				{
					SiteName sn = plat.getSite().getPreferredName();
					Site oldSite = stageDb.siteList.getSite(sn);
					if (oldSite != null)
					{
						stageDb.siteList.removeSite(oldSite);
					}
					stageDb.siteList.addSite(plat.getSite());
				}
				catch (Exception ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Platform {} has an invalid site configuration. Platform will be imported without a site", plat.getDcpAddress());
					plat.setSite(null);
				}
			}
		}

		// Set presentation group parent objects so that when we write to SQL,
		// it can write the parent first so that it has an ID for reference.
		for(PresentationGroup pg : stageDb.presentationGroupList.getVector())
		{
			if (pg.inheritsFrom != null && pg.inheritsFrom.trim().length() > 0)
			{
				for (PresentationGroup pg2 : stageDb.presentationGroupList.getVector())
				{
					if (pg != pg2 && pg.inheritsFrom.equalsIgnoreCase(pg2.groupName))
					{
						pg.parent = pg2;
						break;
					}
				}
			}
		}
	}

	/**
	  Resolve any conflicts between existing database and new entries.
	  Move stage entries into theDb.
	*/
	private void mergeStageToTheDb()
	{
		
	}

	/**
	  Normalizes the links in the database.
	  Reading XML files will result in duplicate subordinate objects in
	  the parent object. Example, two configs based on the same equipmentModel
	  'SU8200'. Each config will have a separate copy of the EM. However only
	  one copy will have been saved in the database's equipementModelList.
	  This method goes through the parent objects, and resets the subordinates
	  to the copy in the database collections.
	  This is necessary for SQL, because only the ones in the collections will
	  be saved, and will have valid SQL integer IDs.
	*/
	private void normalizeTheDb(List<IdDatabaseObject> newObjects)
		throws DatabaseException
	{
		// Point to the new merged, database */
		Database.setDb(theDb);

		/*
		  EquipModels must have unique names. Normalize the references for
		  objects that can hold EMs (PlatformConfig, ConfigSensor, and
		  TransportMedium).
		*/
		for(PlatformConfig pc : theDb.platformConfigList.values())
		{
			if (pc.equipmentModel != null)
			{
				pc.equipmentModel =
					theDb.equipmentModelList.get(pc.equipmentModel.name);
			}

			for(Iterator<ConfigSensor> sit = pc.getSensors(); sit.hasNext(); )
			{
				ConfigSensor cs = sit.next();
				if (cs.equipmentModel != null)
					cs.equipmentModel =
						theDb.equipmentModelList.get(cs.equipmentModel.name);
			}
		}

		for(Iterator<Platform> it = theDb.platformList.iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			for(Iterator<TransportMedium> tmit = p.transportMedia.iterator(); tmit.hasNext(); )
			{
				TransportMedium tm = tmit.next();
				if (tm.equipmentModel != null)
				{
					tm.equipmentModel =
						theDb.equipmentModelList.get(tm.equipmentModel.name);
				}
			}
		}

		/*
		  Platforms also need to normalize the references to Configs and Sites.
		*/
		for(Iterator<Platform> it = theDb.platformList.iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			PlatformConfig pc = p.getConfig();
			if (pc != null)
			{
				pc = theDb.platformConfigList.get(pc.configName);
				p.setConfig(pc);
			}
		}

		// Set default agency & DBNO on any new sites.
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject dob = it.next();
			if (dob instanceof Site)
			{
				SiteName sn = ((Site)dob).getName("usgs");
				if (sn != null)
				{
					if (sn.getAgencyCode() == null)
					{
						sn.setAgencyCode(defaultAgency);
					}
				}
			}
			// Presentation Group parent references need to be normalized
			else if (dob instanceof PresentationGroup)
			{
				PresentationGroup pg = (PresentationGroup)dob;
				if (pg.parent != null)
				{
					PresentationGroup theDbParent =
						theDb.presentationGroupList.find(pg.parent.groupName);
					if (theDbParent != null)
					{
						pg.parent = theDbParent;
					}
					else
					{
						pg.parent = null;
					}
				}
			}
		}


		/*
		  RoutingSpec needs to normalize references to datasource & netlists.
		*/


		// Group data sources may have links to non-imported data sources.
		// Scenario: import a group with member X. But member X already exists
		// in my db.

	}

	private void writeTheChanges(boolean writePlatformList, List<IdDatabaseObject> newObjects) 
		throws DatabaseException
	{
		Database.setDb(theDb);


		// All the new Objects must now have the real database.
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject dob = it.next();
			dob.setDatabase(theDb);
		}

		// MJM 20040904
		// As we add sites and platforms, check to make sure that the
		// SiteName types are in our enumeration. If not, add them.
		decodes.db.DbEnum siteNameTypeEnum = theDb.getDbEnum(Constants.enum_SiteName);
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			Site st = null;
			if (ob instanceof Site)
			{
				st = (Site)ob;
			}
			else if (ob instanceof Platform)
			{
				st = ((Platform)ob).getSite();
			}

			if (st != null)
			{
				for(Iterator<SiteName> snit = st.getNames(); snit.hasNext(); )
				{
					SiteName sn = snit.next();
					String nameType = sn.getNameType();
					if (siteNameTypeEnum.findEnumValue(nameType) == null)
					{
						siteNameTypeEnum.replaceValue(
						 	nameType, nameType, null, null);
					}
				}
			}
		}

		// Only write the ENUM List if ENUMS were actually seen in the imported xml files.
		if (EnumParser.enumParsed)
		{
			theDb.enumList.write();
		}
		// Only write EUs if EUs or converters were seen in the imported xml
		if (EngineeringUnitParser.engineeringUnitsParsed || UnitConverterParser.unitConvertersParsed)
		{
			theDb.engineeringUnitList.write(); // This will also write converters.
		}
		// Configs, Pres Groups, & Computations all write their own data types.
		// Writing the dataTypeSet is really only the equivalences.
		if (DataTypeEquivalenceListParser.dtEquivalencesParsed)
		{
			theDb.dataTypeSet.write();
		}

		/**
		  For SQL, have to write objects from bottom up in the hierarchy.
		  That guarantees that IDs of subordinate objects are assigned before
		  the parent objects are written. The parent then links to the
		  subordinate via its new ID.
		*/

		// First EquipmentModels...
		log.info("Writing modified EquipmentModels");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof EquipmentModel)
			{
				try
				{
					ob.write();
				}
				catch (DatabaseException ex)
				{
					EquipmentModel eq = (EquipmentModel)ob;
					log.atError()
					   .setCause(ex)
					   .log("Could not import equipment model {}", eq.name);
				}
			}
		}

		// Then PlatformConfigs
		//for(Iterator it = newObjects.iterator(); it.hasNext(); )

		log.info("Writing modified configs");
		Vector<PlatformConfig> modifiedCfgs = new Vector<PlatformConfig>();
		for(PlatformConfig pc : theDb.platformConfigList.values())
		{
			if (isNewObject(newObjects, pc))
			{
				modifiedCfgs.add(pc);
			}
		}
		for(Iterator<PlatformConfig> it = modifiedCfgs.iterator(); it.hasNext(); )
		{
			PlatformConfig pc = it.next();
			try
			{
				pc.write();
			}
			catch (DatabaseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Could not import configuration {}",pc.configName);
			}

		}

		log.debug("Before writing sites...");
		if (log.isDebugEnabled())
		{
			for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
			{
				IdDatabaseObject ob = it.next();
				if (ob instanceof Platform)
				{
					log.debug("Platform in list: {}", ((Platform)ob).makeFileName());
				}
			}
		}

		// Then Sites
		log.info("Writing modified Sites");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof Site)
			{
				try
				{
					ob.write();
				}
				catch (DatabaseException ex)
				{
					Site s = (Site)ob;
					log.atError()
					   .setCause(ex)
					   .log("Could not import site {}",s.getPreferredName());
				}
			}
		}

		if (log.isDebugEnabled())
		{
			log.debug("After writing sites...");
			for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
			{
				IdDatabaseObject ob = it.next();
				if (ob instanceof Platform)
				{
					log.debug("Platform in list: {}", ((Platform)ob).makeFileName());
				}
			}
		}

		// Then Platforms
		log.info("Writing modified Platforms");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();

			if (ob instanceof Platform)
			{
				Platform p = (Platform)ob;
				if (p.transportMedia.size() == 0 || p.getConfig() == null)
				{
					log.info("NOT writing platform {} {}",
							 p.makeFileName(),
							 (p.transportMedia.size() == 0
							    ? " it has no TMs"
								: " it has no config."));
					continue;
				}

				log.debug("Will try to write platform '{}' with id={}", p.makeFileName(), p.getId());
				if (newPlatformOwner != null)
				{
					p.setAgency(newPlatformOwner);
				}
				if ((p.getPlatformDesignator() == null || p.getPlatformDesignator().trim().length() == 0)
				 	&& newDesignator != null)
				{
					p.setPlatformDesignator(newDesignator);
				}

				//Code added Oct 17, 2007 for DCP Mon Enhancement Prob. #2
				//To set the Platform Description - if platform description
				//is empty - get it from Site record - if site description
				//is empty - get it from pdt
				String desc = setPlatformDescription(p);
				if (desc != null && desc.trim().length() > 0)
				{
					p.setDescription(desc);
				}
				//End code added Oct 17, 2007
				try
				{
					p.write();
				}
				catch (DatabaseException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Could not import platform '{}' with id={}", p.makeFileName(), p.getId());
				}
			}
		}

		// Data Sources must be written before RoutingSpec
		// First Simple Data Sources:
		log.info("Writing modified simple DataSources");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof DataSource && !((DataSource)ob).isGroupType())
			{
				try
				{
					ob.write();
				}
				catch (DatabaseException ex)
				{
					DataSource ds = (DataSource)ob;
					log.atError()
					   .setCause(ex)
					   .log("Could not import data source '{}'", ds.getName());
				}
			}
		}

		log.info("Writing modified group DataSources");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof DataSource && ((DataSource)ob).isGroupType())
			{
				try
				{
					ob.write();
				}
				catch (DatabaseException ex)
				{
					DataSource ds = (DataSource)ob;
					log.atError()
					   .setCause(ex)
					   .log("Could not import group data source '{}'", ds.getName());
				}
			}
		}

		log.info("Writing modified RoutingSpecs");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof RoutingSpec)
			{
				try
				{
					ob.write();
				}
				catch (DatabaseException ex)
				{
					RoutingSpec rs = (RoutingSpec)ob;
					log.atError()
					   .setCause(ex)
					   .log("Could not import group routing spec '{}'", rs.getName());
				}
			}
		}

		log.info("Writing Processes.");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (ob instanceof CompAppInfo)
			{
				try
				{
					log.trace("Writing '{}'", ob.getObjectType());
					ob.write();
				}
				catch (NullPointerException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Error writing '{}'", ob.getObjectType());
				}
				catch (DatabaseException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Could not import other modified objs");
				}
			}
		}

		// Then anything else...
		log.info("Writing other modified objects.");
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			IdDatabaseObject ob = it.next();
			if (!(ob instanceof EquipmentModel
			      || ob instanceof PlatformConfig
				  || ob instanceof Site
				  || ob instanceof Platform
				  || ob instanceof DataSource
				  || ob instanceof RoutingSpec
				  || ob instanceof CompAppInfo))
			{
				if (ob != null)
				{
					try
					{
						log.debug("Writing '{}'", ob.getObjectType());
						ob.write();
					}
					catch (NullPointerException ex)
					{
						log.atError()
					   	   .setCause(ex)
					       .log("Error writing '{}'", ob.getObjectType());
					}
					catch (DatabaseException ex)
					{
						log.atError()
					   	   .setCause(ex)
					   	   .log("Could not import other modified objs");
					}
				}
			}
		}


		if (writePlatformList)
		{
			log.info("Writing PlatformList.");
			theDb.platformList.write();
		}

		IntervalList editList = IntervalList.editInstance();
		if (editList.getList().size() > 0)
		{
			log.info("Writing interval list with {} elements.", editList.getList().size());
			// This will be true if we ingested an interval list.
			if (theDbio instanceof SqlDatabaseIO)
			{
				IntervalDAI intervalDAO = ((SqlDatabaseIO)theDbio).makeIntervalDAO();
				if (intervalDAO != null)
				{
					try
					{
						for(Interval intv : editList.getList())
						{
							intervalDAO.writeInterval(intv);
						}
					}
					catch(DbIoException ex)
					{
						log.warn("Error writing interval.", ex);
					}
					finally
					{
						intervalDAO.close();
					}
				}
			}
		}
	}

	/**
	 * Verify if the platform description is empty. If it is empty it
	 * will set it from the Site record. If the Site record description
	 * is empty it will set the description from the PDT. The pdt file
	 * is an argument supplied at start up of the program.
	 *
	 * @param p Platform record
	 * @return platform description
	 */
	private String setPlatformDescription(Platform p)
	{
		String newDesc = null;
		if (p != null)
		{
			String desc = p.description;
			if (desc == null || desc.equals(""))
			{
				Site pSite = p.getSite();
				if (pSite != null)
				{	//Get description from Site record
					if (pSite.getDescription() != null
						&& pSite.getDescription().trim().length() > 0)
					{
						desc = pSite.getDescription();
					}
				}
				if ((desc == null || desc.equals("")))
				{
					//Get description from pdt
					String dcpAddress = null;
					TransportMedium tm =
						p.getTransportMedium(Constants.medium_Goes);
					if (tm == null)
					{
						tm = p.getTransportMedium(
											Constants.medium_GoesST);
					}
					if (tm == null)
					{
						tm = p.getTransportMedium(
											Constants.medium_GoesRD);
					}
					if (tm != null)
					{
						dcpAddress = tm.getMediumId();
					}
					if (dcpAddress != null)
					{
						desc = findPdtDescription(new DcpAddress(dcpAddress));
					}
				}
			}
			//Set platform description to be returned
			newDesc = desc;
		}
		return newDesc;
	}

	/**
	 * Parse out the pdt file if one was supplied as an argument
	 *
	 * @param pdtFilePath
	 */
	private void initializePdt(String pdtFilePath)
	{
		pdt = Pdt.instance();
		File pdtFile = new File(pdtFilePath);
		pdt.load(pdtFile);
	}

	/**
	 * This method uses the PDT file to get a description for the
	 * imported Platform record.
	 *
	 * @param dcpAddress
	 * @return description
	 *
	 */
	private String findPdtDescription(DcpAddress dcpAddress)
	{
		String desc = "";
		if (pdt != null)
		{
			PdtEntry pdtEntry = pdt.find(dcpAddress);
			if (pdtEntry != null)
			{
				desc = pdtEntry.description;
			}
		}
		return desc;
	}

	/** @return true if the passed object is in the newObjects vector. */
	private static boolean isNewObject(List<IdDatabaseObject> newObjects,IdDatabaseObject ob)
	{
		for(Iterator<IdDatabaseObject> it = newObjects.iterator(); it.hasNext(); )
		{
			if (ob == it.next())
			{
				return true;
			}
		}
		return false;
	}

}
