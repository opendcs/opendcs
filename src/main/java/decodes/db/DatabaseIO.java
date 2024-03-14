/*
*  $Id$
*/
package decodes.db;

import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.spi.authentication.AuthSource;
import org.xml.sax.SAXException;


import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.SqlDatabaseIO;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;
import decodes.xml.XmlDatabaseIO;
import ilex.util.AuthException;

/**
This is the base class for both XmlDatabaseIO and SqlDatabaseIO.
It defines the interface for the (heavy) objects that implement all
of the IO methods for reading/writing the DECODES database.
*/
public abstract class DatabaseIO
{
	protected final javax.sql.DataSource dataSource;

	public DatabaseIO(javax.sql.DataSource dataSource) throws DatabaseException
	{
		this.dataSource = dataSource;
	}
	/**
	  Creates a concrete IO class as specified by type and location
	  arguments.
	  @param type The database type.
	  @param location Interpration varies depending on type.
	  @return the DatabaseIO object
	  @throws DatabaseException if type unrecognized or location is invalid.
	*/
	public static final DatabaseIO makeDatabaseIO(int type, String location) throws DatabaseException
	{
		return null;
	}

	public static final DatabaseIO makeDatabaseIO(DecodesSettings settings) throws DatabaseException
	{
		return makeDatabaseIO(settings, settings.editDatabaseLocation);
	}

	public static final DatabaseIO makeDatabaseIO(DecodesSettings settings, String locOverride) throws DatabaseException
	{
		ResourceFactory.instance().initDbResources();
		
		try
		{
			final String location = locOverride.startsWith("jdbc:xml:") ? locOverride : "jdbc:xml:" + locOverride;
			final int type = settings.editDatabaseTypeCode;

			AuthSource auth = AuthSourceService.getFromString(settings.DbAuthFile);
			Properties credentials = auth.getCredentials();
			SimpleDataSource dataSource = new SimpleDataSource(location, credentials);

			switch (type)
			{
				case DecodesSettings.DB_XML:  		return new XmlDatabaseIO(dataSource);
				case DecodesSettings.DB_SQL:  		return new SqlDatabaseIO(dataSource);
				case DecodesSettings.DB_CWMS:     	return new decodes.cwms.CwmsSqlDatabaseIO(dataSource);
				case DecodesSettings.DB_OPENTSDB:	return new opendcs.opentsdb.OpenTsdbSqlDbIO(dataSource);
				case DecodesSettings.DB_HDB:		return new decodes.hdb.HdbSqlDatabaseIO(dataSource);
				default: throw new DatabaseException("No database defined (fix properties file)");
			}
		}
		catch (AuthException ex)
		{
			throw new DatabaseException("Unable to authenticate against the database.");
		}
		
	}

	//========== Identification methods ==========================

	/**
	  The identification methods are used primarily for constructing
	  error and dialog messages. DECODES modules should avoid doing
	  different processing based on the database type.
	  @return a string identifying the database type
	 */
	public abstract String getDatabaseType();   // e.g. "XML" or "SQL"

	/**
	  Returns a string identifier that specifies a particular database.
	  For example, an URL or directory name for an XML directory tree.
	  @return String representation of the database name
	*/
	public abstract String getDatabaseName();   // Depends on interface.


	//========== Finalization Methods ==========================

	/** Closes the IO interface and releases any internal resources. */
	public abstract void close( );

	//========== Collection Retrieval Methods ==================

	// These methods return collections of objects that may be only
	// partially populated. They are used to retrieve lists for populating
	// GUI menus.

	/**
	Populates the list of PlatformConfig objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param pcl the list to populate
	*/
	public abstract void readConfigList(PlatformConfigList pcl)
		throws DatabaseException;

	/**
	Populates the list of DataSource objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param dsl the list to populate
	*/
	public abstract void readDataSourceList(DataSourceList dsl)
		throws DatabaseException;

	/**
	Reads the set of known data-type objects in this database.
	Objects in this collection are complete.
	  @param dts the list to populate
	*/
	public abstract void readDataTypeSet(DataTypeSet dts)
		throws DatabaseException;

	/**
	  Reads a single data-type object given its numeric key.
	  @return data type or null if not found
	*/
	public DataType readDataType(DbKey id)
		throws DatabaseException
	{
		return null;
	}

	/**
	  Writes the data type set to the database.
	  @param dts the data type set
	*/
	public abstract void writeDataTypeSet( DataTypeSet dts )
		throws DatabaseException;

	/**
	Reads the list of EngineeringUnit objects defined in this database.
	Objects in this collection are complete.
	  @param eul the list to populate
	*/
	public abstract void readEngineeringUnitList(EngineeringUnitList eul)
		throws DatabaseException;

	/**
	Writes the entire collection of engineering units to the database.
	  @param eul the list to write
	*/
	public abstract void writeEngineeringUnitList(EngineeringUnitList eul)
		throws DatabaseException;

	/**
	Populates the set of known enumeration objects in this database.
	  @param el the list to populate
	*/
	public abstract void readEnumList(EnumList el)
		throws DatabaseException;

	/**
	Writes the EU list to the database.
	  @param el the list to write.
	*/
	public abstract void writeEnumList(EnumList el)
		throws DatabaseException;

	/**
	Populates the list of EquipmentModel objects defined in this database.
	Objects in this collection are complete.
	  @param eml the list to populate
	*/
	public abstract void readEquipmentModelList(EquipmentModelList eml)
		throws DatabaseException;

	/**
	Populates the list of NetworkList objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param nll the list to populate
	*/
	public abstract void readNetworkListList(NetworkListList nll)
		throws DatabaseException;

	/**
	Populates the list of Platform objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param pl the list to populate
	*/
	public abstract void readPlatformList(PlatformList pl)
		throws DatabaseException;

	/**
	Populates the list of PresentationGroup objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param pgl the list to populate
	*/
	public abstract void readPresentationGroupList(PresentationGroupList pgl)
		throws DatabaseException;

	/**
	Populates the list of RoutingSpec objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param rsl the list to populate
	*/
	public abstract void readRoutingSpecList(RoutingSpecList rsl)
		throws DatabaseException;

	/**
	Populates the list of Site objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param sl the list to populate
	*/
	public abstract void readSiteList(SiteList sl)
		throws DatabaseException;

	/**
	Writes the entire set of Sites back out to the database
	  @param sl the list to write
	*/
	public void writeSiteList(SiteList sl)
		throws DatabaseException
	{
		for (Iterator i = sl.iterator(); i.hasNext(); )
		{
			Site s = (Site) i.next();
			writeSite(s);
		}
	}

	/**
	Populates the list of UnitConverter objects defined in this database.
	Objects in this list may be only partially populated (key values
	and primary display attributes only).
	  @param ucs the list to populate
	*/
	public abstract void readUnitConverterSet(UnitConverterSet ucs)
		throws DatabaseException;


	//=============== Object-level Read/Write Functions ============

	/**
	Reads site information. The passed Site object may only be partially
	populated (e.g. from a site list containing names only).
	*/
	public abstract void readSite( Site site )
		throws DatabaseException;

	/**
	Writes site information back to the database.
	*/
	public abstract void writeSite( Site site )
	throws DatabaseException;

	/**
	Deletes a site from the database.
	*/
	public abstract void deleteSite( Site site )
		throws DatabaseException;

	/**
	* Reads a complete platform from the database.
	* This uses this object's platformId member to
	* uniquely identify the record in the database.
	* <p>
	*   The resulting platform object will be populated with links to sites,
	*   platform configs, platform sensors, and transport media.
	* </p>
	  @param p the platform to read
	*/
	public abstract void readPlatform( Platform p )
		throws DatabaseException;

	/**
	* Writes a complete platform back to the database.
	* This uses this object's platformId member to
	* uniquely identify the record in the database.
	  @param p the platform to write
	*/
	public abstract void writePlatform( Platform p )
		throws DatabaseException;

	/**
	* Deletes a platform from to the database, including its transport
	* media. It's configuration is not deleted.
	  @param p the platform to delete
	*/
	public abstract void deletePlatform( Platform p )
		throws DatabaseException;

	/**
	  @return Date object representing the last modify time for this 
	  platform in the database, or null if the platform no longer exists 
	  in the database.
	  @param p the platform
	*/
	public abstract Date getPlatformLMT(Platform p)
		throws DatabaseException;

	/**
	* Reads a platform config object from the database. The passed object
	* must have either configId (for SQL) or configName (for XML) defined.
	* <p>
	*   The resulting PlatformConfig will be complete with links to
	*   config sensors, decodes scripts (and subordinate script data), and
	*   equipment model.
	* </p>
	  @param pc the PlatformConfig to read
	*/
	public abstract void readConfig( PlatformConfig pc )
		throws DatabaseException;

	/**
	* Writes a platform configuration back to the database.
	  @param cfg the PlatformConfig to write
	*/
	public abstract void writeConfig( PlatformConfig cfg )
		throws DatabaseException;

	/**
	* Deletes a platform configuration from the database.
	  @param cfg the PlatformConfig to delete
	*/
	public abstract void deleteConfig( PlatformConfig cfg )
		throws DatabaseException;

	/**
	* Read (or re-read) a single EquipmentModel from the database.
	* This uses the EquipmentModel's name (not it's ID number) to
	* uniquely identify the record in the database.
	  @param em the EquipmentModel to read
	*/
	public abstract void readEquipmentModel( EquipmentModel em )
		throws DatabaseException;

	/**
	* Write an EquipmentModel to the database.
	* This uses the EquipmentModel's name (not it's ID number) to
	* uniquely identify the record in the database.
	  @param em the EquipmentModel to write
	*/
	public abstract void writeEquipmentModel( EquipmentModel em )
		throws DatabaseException;

	/**
	  Deletes a record from the database.
	  @param em the EquipmentModel to delete
	*/
	public abstract void deleteEquipmentModel( EquipmentModel em )
		throws DatabaseException;

	/**
	  Reads the presentation group completely into memory. Prior to this
	  call only key values may have been retrieved.
	  @param pg the PresentationGroup to read
	*/
	public abstract void readPresentationGroup( PresentationGroup pg )
		throws DatabaseException;

	/**
	  Writes the presentation group back to the database, overwriting
	  any previous instance therein.
	  @param pg the PresentationGroup to write
	*/
	public abstract void writePresentationGroup( PresentationGroup pg )
		throws DatabaseException;

	/**
	  Deletes the presentation group from the database.
	  @param pg the PresentationGroup to delete
	*/
	public abstract void deletePresentationGroup( PresentationGroup pg )
		throws DatabaseException;

	/**
	  @return Date object representing the last modify time for this 
	  presentation group in the database, or null if the presentation 
	  group no longer exists in the database.
	*/
	public abstract Date getPresentationGroupLMT(PresentationGroup pg)
		throws DatabaseException;

	/**
	  Reads a routing spec completely into memory.
	  Prior to this call perhaps only the key values were retrieved.
	  @param rs the RoutingSpec to read.
	*/
	public abstract void readRoutingSpec( RoutingSpec rs )
		throws DatabaseException;

	/**
	 * Returns the most recent data that the platform list was modified, this
	 * will be the time of the most-recent platform mod.
	 * @return the most recent data that the platform list was modified.
	 */
	public abstract Date getPlatformListLMT();

	/**
	  Writes a routing spec to the database.
	  @param rs the RoutingSpec to write
	*/
	public abstract void writeRoutingSpec( RoutingSpec rs )
		throws DatabaseException;

	/**
	  Deletes a routing spec from the database.
	  @param rs the RoutingSpec to delete
	*/
	public abstract void deleteRoutingSpec( RoutingSpec rs )
		throws DatabaseException;

	/**
	  Returns Date object representing the last modify time for this 
	  routing spec in the database, or null if the routing spec no 
	  longer exists in the database.
	*/
	public abstract Date getRoutingSpecLMT(RoutingSpec rs)
		throws DatabaseException;

	/**
	  Reads a DataSource from the database.
	  @param ds the DataSource to read
	*/
	public abstract void readDataSource( DataSource ds )
		throws DatabaseException;

	/**
	  Writes a DataSource from the database.
	  @param ds the DataSource to write
	*/
	public abstract void writeDataSource( DataSource ds )
		throws DatabaseException;

	/**
	  Deletes a DataSource from the database.
	  @param ds the DataSource to delete
	*/
	public abstract void deleteDataSource( DataSource ds )
		throws DatabaseException;

	/**
	* Reads (or re-reads) a NetworkList from the database.  This uses
	* the object's name member (not its ID) to uniquely identify the
	* record in the database.
	  @param nl the NetworkList to read
	*/
	public abstract void readNetworkList( NetworkList nl )
		throws DatabaseException;

	/**
	* Writes a NetworkList to the database.  This uses
	* the object's name member (not its ID) to uniquely identify the
	* record in the database.
	  @param nl the NetworkList to write 
	*/
	public abstract void writeNetworkList( NetworkList nl )
		throws DatabaseException;

	/**
	  Deletes a network list from the database. Does not modify the site
	  or platform records referenced by the list entries.
	  @param nl the NetworkList to delete
	*/
	public abstract void deleteNetworkList( NetworkList nl )
		throws DatabaseException;

	/**
	  @return Date object representing the last modify time for this 
	  network list in the database or null if the network list no longer 
	  exists in the database.
	*/
	public abstract Date getNetworkListLMT(NetworkList nl)
		throws DatabaseException;
	
	/**
	 * Non-cached, stand-alone method to read the list of network list 
	 * specs currently defined in the database.
	 * @return ArrayList of currently defined network list specs.
	 */
	public abstract ArrayList<NetworkListSpec> getNetlistSpecs()
		throws DatabaseException;

	/**
	  Writes the PlatformList to the database.
	  @param pl the list to write
	*/
	public abstract void writePlatformList(PlatformList pl)
		throws DatabaseException;
	public abstract boolean commitAfterSelectStatus();
	public abstract void setCommitAfterSelect(boolean status);

	public abstract PlatformConfig newPlatformConfig(PlatformConfig pc, 
		String model, String owner)
		throws DatabaseException;
	
	/** @return platform ID if match is found, null if not */
	public abstract DbKey lookupPlatformId(String mediumType, String mediumId,
		Date timeStamp)
		throws DatabaseException;
	
	/** 
	 * Find a platform ID by site name, and optionally, designator.
	 * @return matching platform ID or null if no match found.
	 */
	public abstract DbKey lookupCurrentPlatformId(SiteName sn, String designator,
		boolean useDesignator)
		throws DatabaseException;

	public abstract Site getSiteBySiteName(SiteName sn)
		throws DatabaseException;
	
	/**
	 * Factory method to make a DAO for loading applications
	 * @return the DAO
	 */
	public abstract LoadingAppDAI makeLoadingAppDAO();

	/** Factory method to make a DAO for schedule entries */
	public abstract ScheduleEntryDAI makeScheduleEntryDAO();
	
	/** Factory method to make a DAO for platform statuses */
	public abstract PlatformStatusDAI makePlatformStatusDAO();
	/**
	  @return DECODES database version may be used by some IO code to access
	  version-specific features and columns.
	*/
	public int getDecodesDatabaseVersion()
	{
		// This method is overloaded by Sql and other DB IO classes to read
		// the version number out of the database.
		return DecodesDatabaseVersion.DECODES_DB_5;
	}

	public boolean isNwis() { return false; }
}
