/*
*  $Id$
*/
package decodes.db;

import ilex.util.Logger;

import java.util.ArrayList;
import java.util.Iterator;

import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;

/**
 * The Database class provides a global means of accessing a 'current'
 * database [A concept that we'd like to get away from].
 * It holds public references to all of the other collection classes.
 * Every Database also has a DatabaseIO object, which handles input and
 * output from/to the permanent storage.
 */

public class Database extends DatabaseObject
{
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

	/**
	 * Default constructor.
	 * This creates an empty Database.
	 * If there is no "current database", this also sets the current
	 * database to this.
	 */

	public Database()
	{
		if (getDb() == null) setDb(this);

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
	 * Construct with a database type and a location.
	 * This constructor takes care of creating the DatabaseIO object
	 * and reading in the initial data.
	 */

/*
MJM 20031104 - removed - do not use this constructor!
	  public Database(int type, String location)
		  throws DatabaseException
	  {
		  this();
		  dbio = DatabaseIO.makeDatabaseIO(type, location);
		  read();
	  }
*/

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
	 */

	public static void setDb(Database db) { _theDb = db; }

	/**
	 * Retrieves the 'current' database.
	 * The 'current' database is set by a previous call to setDb().
	 */

	public static Database getDb() { return _theDb; }

	/**
	 * Gets the interface for reading & writing this database.
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
		System.out.println("calling start");
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
}
