package decodes.dbimport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.List;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import lrgs.common.DcpAddress;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.opentsdb.Interval;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.TeeLogger;
import ilex.cmdline.*;
import decodes.sql.DbKey;
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
			dumpDTS("after loadCurrentDatabase");
			if (overwriteDb)
			{
				this.deleteCurrentDatabase();
				this.dumpDTS("after deleteCurrentDatabase");
			}
			initStageDb();
			dumpDTS("after initStageDb");
			readXmlFiles();
			dumpDTS("after readXmlFiles");

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

			mergeStageToTheDb();
			dumpDTS("after mergeStageToTheDb");
			log.trace("After mergeStageToTheDb, there are {} EUs", Database.getDb().engineeringUnitList.size());
			if (!validateOnly)
			{
				normalizeTheDb();
				log.trace("After normalizeTheDb, there are {} EUs", Database.getDb().engineeringUnitList.size());
				writeTheChanges();
				dumpDTS("after writeTheChanges");
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
	Vector<IdDatabaseObject> newObjects;   // Stores new DatabaseObjects to be added.
	ArrayList<IdDatabaseObject> toDelete = new ArrayList<IdDatabaseObject>();
	final List<String> files;
	boolean writePlatformList;
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
		theDb = new decodes.db.Database(false);
		Database.setDb(theDb);

		try
		{

			DecodesSettings settings = DecodesSettings.instance();
			// unfortunately we still need to use the global decodes settings here.
			settings.loadFromProfile(profile);
			log.info("Using Database...");
			if (dbLoc != null)
			{
				log.info("\t'{}'", dbLoc);
				theDbio = DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XML, dbLoc);
			}
			else
			{
				log.info("\t'{}/{}'", settings.editDatabaseTypeCode, settings.editDatabaseLocation);
				theDbio = DatabaseIO.makeDatabaseIO(
					settings.editDatabaseTypeCode, settings.editDatabaseLocation);
			}
		}
		catch (IOException ex)
		{
			throw new DatabaseException("Unable to initialize target database.");
		}

		// Standard Database Initialization for all Apps:
		Site.explicitList = false; // YES Sites automatically added to SiteList
		theDb.setDbIo(theDbio);

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
		throws SAXException, ParserConfigurationException
	{
		log.debug("Initializing the staging database.");
		stageDb = new decodes.db.Database(false);
		Database.setDb(stageDb);
		stageDbio = new XmlDatabaseIO("");
		stageDb.setDbIo(stageDbio);
		topParser = stageDbio.getParser();
		newObjects = new Vector<IdDatabaseObject>();

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
		Database.setDb(theDb);

		if (overwriteDb)
		{
			theDb.engineeringUnitList.clear();
			theDb.unitConverterSet.clear();
			theDb.dataTypeSet.clear();
			if (EnumParser.enumParsed)
			{
				theDb.enumList.clear();
			}
		}

		// An EU is just a bean, not a DatabaseObject. So I can just copy the objects
		// from stage into db-to-write.
		for(Iterator<EngineeringUnit> euit = stageDb.engineeringUnitList.iterator(); euit.hasNext(); )
		{
			theDb.engineeringUnitList.add(euit.next());
		}
		// A UnitConverterDb is an IdDatabaseObject, so it knows which database it
		// belongs to. Therefore, I have to make copies in the db to write.
		for(Iterator<UnitConverterDb> ucdbit = stageDb.unitConverterSet.iteratorDb(); ucdbit.hasNext(); )
		{
			UnitConverterDb stageUC = ucdbit.next();
			UnitConverterDb dbUC = stageUC.copy();
			dbUC.clearId();
			theDb.unitConverterSet.addDbConverter(dbUC);
		}
		// Likewise, a DataType is an IdDatabaseObject, so have to make copies.
		for(Iterator<DataType> dtit = stageDb.dataTypeSet.iterator(); dtit.hasNext(); )
		{
			DataType stageDT = dtit.next();
			// getDataType will create it in the current database ('theDb')
			DataType newDT = DataType.getDataType(stageDT.getStandard(), stageDT.getCode());
			newDT.setDisplayName(stageDT.getDisplayName());
		}

		log.trace("mergeStageToTheDb 1: #EUs={}", theDb.engineeringUnitList.size());
		log.trace("mergeStageToTheDb 3: #stageEUs={}", stageDb.engineeringUnitList.size());

		if (validateOnly)
		{
			log.info("The following messages indicate what WOULD BE modified in the"
				+ " database. No changes will actually been made.");
		}

		if (EnumParser.enumParsed)
		{
			for(Iterator<DbEnum> it = stageDb.enumList.iterator(); it.hasNext(); )
			{
				DbEnum stageOb = it.next();
				DbEnum oldOb = theDb.getDbEnum(stageOb.enumName);
				if (oldOb == null)
				{
					log.info("Adding new Enum '{}'", stageOb.enumName);
					theDb.enumList.addEnum(stageOb);
					for(Iterator<EnumValue> evit = stageOb.iterator(); evit.hasNext(); )
					{
						EnumValue ev = evit.next();
						log.info("    {} - {}", ev.getValue(), ev.getDescription());
					}
				}
				else
				{
					if (!keepOld)
					{
						log.info("Overwriting Enum '{}'", stageOb.enumName);
						for(Iterator<EnumValue> evit = stageOb.iterator();evit.hasNext();)
						{
							EnumValue ev = evit.next();
							oldOb.replaceValue(
								ev.getValue(), ev.getDescription(),
								ev.getExecClassName(), ev.getEditClassName());
						}
					}
					else
					{
						log.info("Keeping old version of Enum '{}'", stageOb.enumName);
					}
				}
			}
		}

		for(Iterator<NetworkList> it = stageDb.networkListList.iterator(); it.hasNext(); )
		{
			NetworkList ob = it.next();
			NetworkList oldOb = theDb.networkListList.find(ob.name);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.name);
				theDb.networkListList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.name);
					theDb.networkListList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.name);
				}
			}
		}

		// If a designator was specified with -G arg, add it to all platforms in the stage
		// db that have no designator.
		if (newDesignator != null)
		{
			for(Platform p : stageDb.platformList.getPlatformVector())
			{
				if ((p.getPlatformDesignator() == null || p.getPlatformDesignator().trim().length() == 0))
				{
				p.setPlatformDesignator(newDesignator);
				}
			}
		}


		// Platform matching is tricky because there are two unique keys.
		// The main key is by matching Site and Platform Designator.
		// The secondary key is by matching a transport medium (TM).
		//
		// There are 5 possible use cases:
		// 1. (site,desig) matches existing platform AND TM matches same platform
		//    ==> New platform replaces existing
		// 2. (site,desig) matches existing platform. TM does not match any platform
		//    ==> New platform replaces existing
		// 3. (site,desig) matches existing platform, but TM matches a different platform.
		//    ==> New platform replaces existing
		//    ==> Remove TM from the different existing platform, and if that platform now has
		//        no remaining TMs, remove it entirely.
		// 4. No match for (site,desig). No match for TM.
		//    ==> Import new platform
		// 5. No match for (site,desig). There is a match for TM.
		//    ==> Import New Platform
		//    ==> Remove TM from the different existing platform, and if that platform now has
		//        no remaining TMs, remove it entirely.
		writePlatformList = false;
		for(Iterator<Platform> it = stageDb.platformList.iterator(); it.hasNext(); )
		{
			Platform newPlat = it.next();
			if (log.isTraceEnabled())
			{
				log.trace("merging platform '{}'", newPlat.getDisplayName());
				for (PlatformSensor ps : newPlat.platformSensors)
				{
					log.trace("   Sensor {}" + ps.sensorNumber + ": actualSite={}",
							  ps.sensorNumber, (ps.site == null ? "null" : ps.site.getPreferredName()));
				}
			}


			if (newPlat.getSite() == null)
			{
				log.warn("Skipping platform with ID={} in the XML input file because "
					   + "it has no associated site.", newPlat.getId());
				continue;
			}

			Platform oldPlatformMatch = null;

			// See if a matching old site exists
			Site oldSite = null;
			for(SiteName newPlatSiteName : newPlat.getSite().getNameArray())
			{
				if ((oldSite = theDb.siteList.getSite(newPlatSiteName)) != null)
				{
					break;
				}
			}
			if (oldSite == null)
			{
				for(SiteName sn : newPlat.getSite().getNameArray())
				{
					if ((oldSite = theDb.siteList.getSite(sn)) != null)
					{
						break;
					}
				}
			}
			log.trace("    site does {} exist in old database.", (oldSite==null ? "not " : ""));

			// Then find an old platform with matching (site,designator)
			if (oldSite != null)
			{
				oldPlatformMatch = theDb.platformList.findPlatform(oldSite, newPlat.getPlatformDesignator());
				log.trace("    Old platform does {} exist with matching site/desig siteId={}"+ ", sitename={}, desig='{}'",
						  (oldPlatformMatch==null?"not":""), oldSite.getId(), oldSite.getPreferredName(), newPlat.getPlatformDesignator()
						 );
			}

			// Try to find existing platform with a matching transport id.
			Platform oldTmMatch = null;
			for(Iterator<TransportMedium> tmit = newPlat.transportMedia.iterator();
				oldTmMatch == null && tmit.hasNext(); )
			{
				TransportMedium tm = tmit.next();
				Date d = newPlat.expiration;
				log.trace("    Looking for match to TM {} with expiration {}",  tm.toString(), d);
				oldTmMatch = theDb.platformList.findPlatform(tm.getMediumType(), tm.getMediumId(), d);
				final Platform logOldTmMatch = oldTmMatch;
				log.atTrace().log(() -> "        - Match was " + (logOldTmMatch==null ? "not found." : ("found with id="+logOldTmMatch.getId())));
				oldPlatformMatch = oldTmMatch;
			}

			if (oldPlatformMatch == null)
			{
				// use cases 4 & 5: This is a NEW platform.
				log.info("Adding New Platform '{}'", newPlat.makeFileName());
				theDb.platformList.add(newPlat);

//				if (oldTmMatch != null)
//				{
//					info("Match for tm '" + oldTmMatch.toString() + "' -- will remove from old platform with id="
//						+ oldTmMatch.getId());
//					// use case 5 No match for (site,desig) but there is a match for TM.
//					// Need to cause the old TMs to be removed from existing platform.
//					for(Iterator<TransportMedium> tmit = newPlat.getTransportMedia(); tmit.hasNext(); )
//					{
//						TransportMedium newTM = tmit.next();
//						TransportMedium oldTM = oldTmMatch.getTransportMedium(newTM.getMediumType());
//						if (oldTM != null && newTM.getMediumId().equals(oldTM.getMediumId()))
//							tmit.remove();
//					}
//					if (oldTmMatch.transportMedia.size() > 0)
//						newObjects.add(oldTmMatch);
//					else if (!DbKey.isNull(oldTmMatch.getId()))
//						toDelete.add(oldTmMatch);
//				}
				newObjects.add(newPlat);
				if (log.isTraceEnabled())
				{
					log.trace("Added new platform '{}' to newObjects List", newPlat.makeFileName());
					for (PlatformSensor ps : newPlat.platformSensors)
					{
						log.trace("   Sensor {}: actualSite={}", ps.sensorNumber,(ps.site == null ? "null" : ps.site.getPreferredName()));
					}
				}
				writePlatformList = true;
			}
			else if (!oldPlatformMatch.equals(newPlat))
			{
				// use cases 1, 2, and 3: There was a match for (site,desig)
				if (!keepOld)
				{
					log.info("Overwriting Platform '{}'", newPlat.makeFileName());

					DbKey oldId = oldPlatformMatch.getId();
					theDb.platformList.removePlatform(oldPlatformMatch);
					newPlat.clearId();
					try
					{
						newPlat.setId(oldId);
					}
					catch(Exception ex)
					{
						log.atError().setCause(ex).log("An exception was thrown in a call that should never throw an exception.");
					}
					log.info("set platform ID to match existing ID={}", oldId);
					theDb.platformList.add(newPlat);


					newObjects.add(newPlat);

					log.debug("Added platform '{}' with id={} and siteid={} to newObjects list.",
							newPlat.makeFileName(), newPlat.getId(),
							(newPlat.getSite()==null?"<nullsite!>":newPlat.getSite().getId()) );
					if (log.isTraceEnabled())
					{
						for(PlatformSensor ps : newPlat.platformSensors)
						{
							log.trace("   sensor {}" + ps.sensorNumber + " actualSite={}",
									  ps.sensorNumber,(ps.site == null ? "null" : ps.site.getPreferredName()));
						}
					}
					writePlatformList = true;
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldPlatformMatch.getObjectType(),newPlat.makeFileName());
				}
			}
		}

		for(Iterator<PresentationGroup> it = stageDb.presentationGroupList.iterator(); it.hasNext(); )
		{
			PresentationGroup ob = it.next();
			PresentationGroup oldOb= theDb.presentationGroupList.find(ob.groupName);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.groupName);
				theDb.presentationGroupList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.groupName);
					theDb.presentationGroupList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.groupName);
				}
			}
		}

		// MJM 6/3/04 - Must do DataSources before Routing Specs so that
		// new DS records will have an ID for the RS to reference.
		for(Iterator<DataSource> it = stageDb.dataSourceList.iterator(); it.hasNext(); )
		{
			DataSource ob = it.next();
			DataSource oldOb= theDb.dataSourceList.get(ob.getName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getName());
				theDb.dataSourceList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getName());
					theDb.dataSourceList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getName());
				}
			}
			else
			{
				log.info("Imported data source '{}' is unchanged from DB version.", ob.getName());
				ob.forceSetId(oldOb.getId());
			}
		}

		RoutingSpecList.silentFind = true;
		for(Iterator<RoutingSpec> it = stageDb.routingSpecList.iterator(); it.hasNext(); )
		{
			RoutingSpec ob = it.next();
			RoutingSpec oldOb= theDb.routingSpecList.find(ob.getName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getName());
				theDb.routingSpecList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getName());
					theDb.routingSpecList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getName());
				}
			}
		}

		for(Iterator<Site> it = stageDb.siteList.iterator(); it.hasNext(); )
		{
			Site ob = it.next();
			if (ob.getPreferredName() == null)
			{
				log.warn("Import file contained a site with no name. Ignoring.");
				continue;
			}
			Site oldOb= theDb.siteList.getSite(ob.getPreferredName());

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.getPreferredName());
				theDb.siteList.addSite(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.getPreferredName());
					theDb.siteList.addSite(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.getPreferredName());
				}
			}
		}

		// Then make sure that all new equivalences are asserted in the new DB.
		// Note: data-type equivalences can never be unasserted by dbimport.
		// The only way to do that is to flush the database and re-import.
		for(Iterator<DataType> it = stageDb.dataTypeSet.iterator(); it.hasNext(); )
		{
			DataType stageOb = it.next();
			DataType theOb = theDb.dataTypeSet.get(stageOb.getStandard(), stageOb.getCode());
			if (theOb == null)
			{
				continue; // shouldn't happen.
			}

			// loop through this dt's equivalences in the stage db.
			for(DataType sdt = stageOb.equivRing; sdt != null && sdt != stageOb;
				sdt = sdt.equivRing)
			{
				// Fetch the copy of this data type that's in the new DB.
				DataType tdt = DataType.getDataType(sdt.getStandard(), sdt.getCode());
				if (!theOb.isEquivalent(tdt))
				{
					log.debug("Asserting equivalence between data types '{}' and '{}'", theOb.toString(), tdt.toString());
					theOb.assertEquivalence(tdt);
				}
			}
		}

		for(PlatformConfig ob : stageDb.platformConfigList.values())
		{
			PlatformConfig oldOb= theDb.platformConfigList.get(ob.configName);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.configName);
				theDb.platformConfigList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.configName);
					theDb.platformConfigList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.configName);
				}
			}
		}

		for(Iterator<EquipmentModel> it = stageDb.equipmentModelList.iterator(); it.hasNext(); )
		{
			EquipmentModel ob = it.next();
			EquipmentModel oldOb= theDb.equipmentModelList.get(ob.name);

			if (oldOb == null)
			{
				log.info("Adding new {} '{}'", ob.getObjectType(), ob.name);
				theDb.equipmentModelList.add(ob);
				newObjects.add(ob);
			}
			else if (!oldOb.equals(ob))
			{
				if (!keepOld)
				{
					log.info("Overwriting {} '{}'", oldOb.getObjectType(), ob.name);
					theDb.equipmentModelList.add(ob);
					newObjects.add(ob);
				}
				else
				{
					log.info("Keeping old version of {} '{}'", oldOb.getObjectType(), ob.name);
				}
			}
		}

//MJM 20140826 I don't think we need to do the following. DataTypes are always
//created via DataType.getDataType method, regardless of where they're parsed
//from (config sensors, dp elements, etc.) So they'll already be in the DataTypeSet
//which was copied above.
		/*
		  ConfigSensors and DataPresentation elements contain references
		  to DataType objects.
		  Change the references to the object in the db-to-write.
		 */
		for(PlatformConfig stagePc : stageDb.platformConfigList.values())
		{
			for(Iterator<ConfigSensor> sit = stagePc.getSensors(); sit.hasNext();)
			{
				ConfigSensor stageSensor = sit.next();
				for(int dtidx = 0; dtidx < stageSensor.getDataTypeVec().size(); dtidx++)
				{
					DataType dt = stageSensor.getDataTypeVec().get(dtidx);
					DataType dbdt = DataType.getDataType(dt.getStandard(), dt.getCode());
					stageSensor.getDataTypeVec().set(dtidx, dbdt);
				}
			}
		}
		for(Iterator<PresentationGroup> pgit = stageDb.presentationGroupList.iterator(); pgit.hasNext();)
		{
			PresentationGroup pg = pgit.next();
			for(Iterator<DataPresentation> dpit = pg.iterator(); dpit.hasNext(); )
			{
				DataPresentation stageDP = dpit.next();
				if (stageDP.getDataType() != null)
				{
					DataType dt = DataType.getDataType(
						stageDP.getDataType().getStandard(), stageDP.getDataType().getCode());
					stageDP.setDataType(dt);
				}
			}
		}

		for(CompAppInfo stageApp : stageDb.loadingAppList)
		{
			CompAppInfo existingApp = null;
			for(CompAppInfo cai : theDb.loadingAppList)
			{
				if (cai.getAppName().equalsIgnoreCase(stageApp.getAppName()))
				{
					existingApp = cai;
					break;
				}
			}
			if (existingApp != null)
			{
				log.info("Overwriting loading app '{}'", stageApp.getAppName());
			}
			else
			{
				log.info("Adding loading app '{}'", stageApp.getAppName());
			}

			newObjects.add(stageApp);
		}

		for(ScheduleEntry stageSE : stageDb.schedEntryList)
		{
			// Don't import manual schedule entries, which are created when someone runs
			// a routing spec with the 'rs' command.
			if (stageSE.getName() == null || stageSE.getName().endsWith("-manual"))
			{
				log.info("Skipping manual schedule entry '{}'", stageSE.getName());
				continue;
			}

			ScheduleEntry existingSE = null;
			for(ScheduleEntry x : theDb.schedEntryList)
			{
				if (x.getName().equals(stageSE.getName()))
				{
					existingSE = x;
					break;
				}
			}
			if (existingSE != null)
			{
				log.info("Overwriting schedule entry '{}'", existingSE.getName());
			}
			else
			{
				log.info("Adding schedule entry '{}'", stageSE.getName());
			}

			newObjects.add(stageSE);
		}

		if (validateOnly)
		{
			log.info(
				"Previous messages indicate what WOULD HAVE BEEN modified in the"
				+ " database. No changes have actually been made.");
		}
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
	private void normalizeTheDb()
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

	private void writeTheChanges()
		throws DatabaseException
	{
		Database.setDb(theDb);

		for(IdDatabaseObject td : toDelete)
		{
			if (td instanceof Platform)
			{
				log.info("Deleting platform {}", ((Platform)td).makeFileName());
				theDb.getDbIo().deletePlatform((Platform)td);
			}
		}

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
			if (isNewObject(pc))
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
	private boolean isNewObject(IdDatabaseObject ob)
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

	private void dumpDTS(String when)
	{
		return;
//		DbEnum dts = theDb.enumList.getEnum(Constants.enum_DataTypeStd);
//		if (dts == null)
//		{
//			info("dumpDTS " + when + " there is no enum " + Constants.enum_DataTypeStd);
//			return;
//		}
//		info("dumpDTS " + when + " enum " + Constants.enum_DataTypeStd + " has the following members:");
//		for(Iterator<EnumValue> evit = dts.iterator(); evit.hasNext(); )
//		{
//			EnumValue ev = evit.next();
//			info("    " + ev.getValue() + " " + ev.getDescription());
//		}
	}
// Debug method:
//	private void showEnums(String when)
//	{
//		System.out.println(when
//			+ ", enumList.size() = " + theDb.enumList.size());
//		for(Iterator eit = theDb.enumList.iterator(); eit.hasNext(); )
//		{
//			decodes.db.Enum en = (decodes.db.Enum)eit.next();
//			System.out.println("\tEnum '"
//				+ en.enumName + "' size=" + en.size());
//		}
//	}
}
