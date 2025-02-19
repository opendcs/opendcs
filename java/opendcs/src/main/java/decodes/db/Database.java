/*
*  $Id$
*/
package decodes.db;

import ilex.util.Logger;
import opendcs.util.functional.ThrowingRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;

import org.opendcs.utils.logging.Timer;
import org.slf4j.LoggerFactory;

import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;

/**
 * The Database class provides a global means of accessing a 'current'
 * database [A concept that we'd like to get away from].
 * It holds public references to all of the other collection classes.
 * Every Database also has a DatabaseIO object, which handles input and
 * output from/to the permanent storage.
 */

public class Database extends DatabaseObject
{
	private static org.slf4j.Logger log = LoggerFactory.getLogger(Database.class);
	private static Database _theDb = null;  // Static 'current' instance

	// Collection classes that represent this database:

	/** Holds the EngineeringUnit objects from the database. */
	public EngineeringUnitList   engineeringUnitList;

	/** Holds all the Enum and EnumValue objects from the database. */
	public EnumList			  enumList;

	public NetworkListList	   networkListList;

//	public PMConfigList		  pMConfigList;

	public PlatformList		  platformList;

	public PresentationGroupList presentationGroupList;

	public RoutingSpecList	   routingSpecList;

	public SiteList			  siteList;

	public DataTypeSet		   dataTypeSet;

	public UnitConverterSet	  unitConverterSet;

	public PlatformConfigList	platformConfigList;

	public EquipmentModelList	equipmentModelList;

	public DataSourceList		dataSourceList;

	public ArrayList<CompAppInfo> loadingAppList = new ArrayList<CompAppInfo>();

	public ArrayList<ScheduleEntry> schedEntryList = new ArrayList<ScheduleEntry>();

	public ArrayList<PlatformStatus> platformStatusList = new ArrayList<PlatformStatus>();

	/** The interface for reading and writing this database. */

	private DatabaseIO dbio;

	private boolean decodesInitialized = false;
	private boolean initializedForDecoding = false;
	private boolean initializedForEditing = false;
	private boolean siteListRead = false;

	/**
	 * Default constructor.
	 * This creates an empty Database.
	 * If there is no "current database", this also sets the current
	 * database to this.
	 */
	public Database()
	{
		this(false);
	}

	/**
	 * Used in areas were an completely independent instance of the Decodes Database should be provided.
	 *
	 * @param independent whether to bypass the entire singleton concept for this instance.
	 */
	public Database(boolean independent)
	{
		super(null); // prevent DatabaseObject from calling the static Database.getDb() method.
		if (!independent && Database.getDb() == null)
		{
			Database.setDb(this);
		}

		engineeringUnitList = new EngineeringUnitList();
		engineeringUnitList.setDatabase(this);
		enumList = new EnumList();
		enumList.setDatabase(this);

		networkListList = new NetworkListList();
		networkListList.setDatabase(this);
		platformList = new PlatformList();
		platformList.setDatabase(this);
		presentationGroupList = new PresentationGroupList();
		presentationGroupList.setDatabase(this);
		routingSpecList = new RoutingSpecList();
		routingSpecList.setDatabase(this);
		siteList = new SiteList();
		siteList.setDatabase(this);
		dataTypeSet = new DataTypeSet();
		dataTypeSet.setDatabase(this);
		unitConverterSet = new UnitConverterSet();
		unitConverterSet.setDatabase(this);
		platformConfigList = new PlatformConfigList();
		platformConfigList.setDatabase(this);
		equipmentModelList = new EquipmentModelList();
		equipmentModelList.setDatabase(this);
		dataSourceList = new DataSourceList();
		dataSourceList.setDatabase(this);

		this.setDatabase(this);
	}

	/**
	 * The Database is itself a DatabaseObject; its type is "Database".
	 */

  	public String getObjectType() { return "Database"; }

	/**
	 * Sets the 'current' database to the passed value. Subsequent calls
	 * to the static getDb() method will return this value.
	 * <p>
	 * The first call to the Database constructor will call this method
	 * implicitly.
	 * @param db The new database
	 * @deprecated since 7.0.12 like getDb can be used in old code that is being updated; avoid using it
	 * in anything new.
	 */
	@Deprecated
	public static void setDb(Database db) { _theDb = db; }

	/**
	 * Retrieves the 'current' database.
	 * The 'current' database is set by a previous call to setDb().
	 * @returns a global instance of the database
	 * @deprecated since 7.0.9 existing code can use this, but anything totally new that requires the decodes database
	 * *MUST* take the Database in its constructor. If upgrading/updating things at least consider trying to move
	 * to constructor injections; even if it means just moving this call up a level.
	 */
	@Deprecated
	public static Database getDb() { return _theDb; }

	/**
	 * Gets the interface for reading &amp; writing this database.
	 */

	public DatabaseIO getDbIo() { return dbio; }

	/**
	 * Sets the interface to be used for reading and writing this database.
	 */

	public void setDbIo(DatabaseIO dbio) { this.dbio = dbio; }

	@Override
	public void prepareForExec() {}

	@Override
	public boolean isPrepared() { return false;}

	/**
	 * Reads the entire database into memory.
	 * The DBIO must be set before calling this method.
	 */

	public void read()
		throws DatabaseException
	{
		enumList.read();
		dataTypeSet.read();
		engineeringUnitList.read();
		siteList.read();
		platformList.read();
		platformConfigList.read();
		equipmentModelList.read();
		// equationSpecList.read();
		routingSpecList.read();
		dataSourceList.read();
		networkListList.read();
		presentationGroupList.read();
		// eqTableList.read();
		// pMConfigList.read();

		for(Iterator it = platformList.iterator(); it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			p.read();
		}
	}

	/**
	 * Writes the database back out to permanent storage.
	 * The DBIO must be set before calling this method.
	 */

	public void write()
		throws DatabaseException
	{
		enumList.write();
		dataTypeSet.write();
		engineeringUnitList.write();
		siteList.write();
		platformConfigList.write();
		equipmentModelList.write();
		// equationSpecList.write();
		routingSpecList.write();
		dataSourceList.write();
		networkListList.write();
		presentationGroupList.write();
		// eqTableList.write();
		// pMConfigList.write();

		for(Iterator it = platformList.iterator(); it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			p.write();
		}
		platformList.write();
	}

	/**
	 * Reads an enumeration from the DB if necessary and adds it to the cache.
	 * @param enumName Name of enumeration
	 * @return the DbEnum object.
	 */
	public DbEnum getDbEnum(String enumName)
	{
		if (enumList == null)
			return null;
		DbEnum ret = enumList.getEnum(enumName);
		if (ret != null || enumList.haveReadAllEnums())
			return ret;

		try
		{
			if (!(dbio instanceof SqlDatabaseIO))
			{
				enumList.read();
				return enumList.getEnum(enumName);
			}
			SqlDatabaseIO sdbio = (SqlDatabaseIO)dbio;
			ret = sdbio.readEnum(enumName);
			if (ret != null)
				enumList.addEnum(ret);
			return ret;
		}
		catch(DatabaseException ex)
		{
			String msg = "Cannot read enum: " + enumName + ": " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return null;
		}
	}

	public PresentationGroupList getPresentationGroupList()
	{
		if (!presentationGroupList.wasRead())
		{
			try { presentationGroupList.read(); }
			catch(DatabaseException ex)
			{
				String msg = "Cannot read presentation group list: " + ex;
				System.err.println(msg);
				ex.printStackTrace(System.err);
				Logger.instance().failure(msg);
			}
		}
		return presentationGroupList;
	}

	private void timedLoad(String dataSet, ThrowingRunnable<DecodesException> task) throws DecodesException
	{
		log.info("Loading {}.", dataSet);
		final Duration duration = Timer.elapsedTime(task);
		log.info("Loading {} to {} seconds", dataSet, duration.toMillis()/1000.);
	}

	public void init(DecodesSettings settings ) throws DecodesException
	{
		initMinimal(settings);
		timedLoad("Enums", () -> this.enumList.read());
		timedLoad("DataTypes", () -> this.dataTypeSet.read());
		timedLoad("DataSources", () -> this.dataSourceList.read());
		this.decodesInitialized = true;
	}

	public void initMinimal(DecodesSettings settings) throws DecodesException
	{
		if (decodesInitialized)
		{
			return;
		}
		log.info("Init DECODES DB: ");
		timedLoad("Engineering Units", () -> this.engineeringUnitList.read());
	}

	public void initializeForEditing() throws DecodesException
	{
		if (!decodesInitialized)
		{
			throw new DecodesException("Must initialize DECODES before calling "
				+ "initializeForEditing()");
		}
		if (initializedForEditing)
		{
			return;
		}
		log.info("Loading Sites.");

		readSiteList(true);
		final Duration siteReadTime = Timer.elapsedTime(() -> this.siteList.read());
		log.info("Loading Sites Took {} seconds", siteReadTime.toMillis()/1000.0);

		log.info("Loading Equipment.");
		final Duration equipmentReadTime = Timer.elapsedTime(() -> this.equipmentModelList.read());
		log.info("Loading equipment took {} seconds", equipmentReadTime.toMillis()/1000.0);

		log.info("Loading Configs.");
		final Duration platformConfigListTime = Timer.elapsedTime(() -> this.platformConfigList.read());
		log.info("Loading Configs took {} seconds", platformConfigListTime.toMillis()/1000.0);

		initializeForDecoding();

		platformConfigList.countPlatformsUsing();

		log.info("Loading Routing Specs.");
		final Duration routingReadTime = Timer.elapsedTime(() -> this.routingSpecList.read());
		log.info("Loading Routing Specs took {} seconds", routingReadTime.toMillis()/1000.0);
		initializedForEditing = true;
	}

	public void initializeForDecoding() throws DecodesException
	{
		if (!decodesInitialized)
		{
			throw new DecodesException("Must initialize DECODES before calling "
				+ "initializeForDecoding()");
		}
		if (initializedForDecoding)
		{
			return;
		}
		readSiteList(false);
		timedLoad("PlatformConfigList", () -> this.platformConfigList.read());
		timedLoad("Platforms", () -> this.platformList.read());
		timedLoad("Presentation Groups", () -> this.presentationGroupList.read());
		timedLoad("Network lists", () -> this.networkListList.read());
		initializedForDecoding = true;
	}

	private void readSiteList(boolean explicit) throws DecodesException
	{
		final boolean orig = Site.explicitList;
		try
		{
			Site.explicitList = explicit;
			if(!siteListRead)
			{
				timedLoad("Sites", () -> siteList.read());
				siteListRead = true;
			}
		}
		finally
		{
			Site.explicitList = orig;
		}
	}

}
