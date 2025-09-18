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
package decodes.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;
import decodes.db.NetworkListSpec;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.db.Site;
import decodes.db.SiteName;


/**
This class handles SQL IO for network lists.
*/
public class NetworkListListIO extends SqlDbObjIo
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	* Transient reference to the PlatformList object for this Database.  
	* This is * used to lookup values for the NetworkListEntry's.
	* This is initialized when it's needed by calling
	* the initDb(Database) method.
	*/
	private PlatformList _platformList;

	/**
	* Transient reference to Database's list of NetworkLists.
	* This is initialized when it's needed by calling
	* the initDb(Database) method.
	*/
	private NetworkListList _networkListList;


	/**
	  Constructor.
	  @param dbio the SqlDatabaseIO to which this IO object belongs
	*/
	public NetworkListListIO(SqlDatabaseIO dbio)
	{
		super(dbio);

		_platformList = null;
		_networkListList = null;
	}

	/**
	* This initializes the links that this object has to things in the
	* Database.  This should be the only way that those objects are
	* set -- that will ensure that they both refer to objects in the
	* same Database.
	* @param db the Database reference
	*/
	private void initDb(Database db)
	{
		_platformList = db.platformList;
		_networkListList = db.networkListList;
	}

	/**
	* Initializes this object's links into the Database from a
	* DatabaseObject.
	*/
	private void initDb(DatabaseObject dbObj)
	{
		 initDb(dbObj.getDatabase());
	}

	/** 
	  Reads the NetworkList and NetworkListEntry tables.  
	  @param nll the list of network lists
	  @param tmType the transport medium type to filter by, or null for all.
	*/
	public void read(NetworkListList nll, String tmType)
		throws SQLException, DatabaseException
	{
		log.debug("Reading NetworkLists...");
		// Initialize our links into the Database
		initDb(nll.getDatabase());

		String q = "SELECT * FROM NetworkList";

		String qtmt = null;
		if (tmType != null)
		{

			if (tmType.equalsIgnoreCase("goes"))
			{
				qtmt = "'goes', 'goes-self-times', 'goes-random'";
			}
			else
			{
				qtmt = "'" + tmType.toLowerCase() + "'";
			}

			q = q + " WHERE lower(transportMediumType) IN (?)";
		}

		try (Connection conn = connection();
			 PreparedStatement pStmt = conn.prepareStatement(q))
		{
			if(tmType != null)
			{
				pStmt.setString(1, qtmt);
			}
			try(ResultSet rs = pStmt.executeQuery())
			{
				if(rs != null)
				{
					while(rs.next())
					{
						putNetworkList(DbKey.createDbKey(rs, 1), rs);
					}
				}
			}

			// For CWMS, by joining with NetworkList, VPD will automatically
			// add the predicate to filter by office id. For other DBs, the
			// join does nothing, but does no harm.
			String nleColumns = "a.networkListId, a.transportId";
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
			{
				nleColumns += ", a.platform_name, a.description";
			}

			q = "SELECT " + nleColumns
					+ " FROM NetworkListEntry a, NetworkList b "
					+ "WHERE a.networkListId = b.id ";

			try (Statement stmt = conn.createStatement();
				 ResultSet rs_nle = stmt.executeQuery(q))
			{
				while (rs_nle.next())
				{
					DbKey id = DbKey.createDbKey(rs_nle, 1);
					String transportId = rs_nle.getString(2);

					NetworkList nl = _networkListList.getById(id);
					if(nl == null)
					{
						log.warn("Orphan network list entry with invalid network list ID {}, ignored.", id);
						continue;
					}
					if (tmType != null)
					{
						if (tmType.equalsIgnoreCase("goes"))
						{
							if (!nl.transportMediumType.equalsIgnoreCase("goes")
									&& !nl.transportMediumType.equalsIgnoreCase("goes-self-times")
									&& !nl.transportMediumType.equalsIgnoreCase("goes-random"))
							{
								continue;
							}
						}
						else if (!nl.transportMediumType.equalsIgnoreCase(tmType))
						{
							continue;
						}
					}
					NetworkListEntry nle = new NetworkListEntry(
							nl, transportId);
					if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
					{
						nle.setPlatformName(rs_nle.getString(3));
						nle.setDescription(rs_nle.getString(4));
					}
					else // Pre version 11 derived name and description from platform & site.
					{
						Platform p = _platformList.findPlatform(
								nl.transportMediumType, transportId, new Date());

						if (p != null)
						{
							//Find the right site name for this network list site
							//name type preference
							Site pSite = p.getSite();
							if (pSite != null)
							{
								SiteName sn = pSite.getName(nl.siteNameTypePref);
								if (sn != null)
								{
									nle.setPlatformName(sn.getNameValue());
								}
								else
								{
									nle.setPlatformName(p.getSiteName(false));
								}
							}
							else
							{
								nle.setPlatformName(p.getSiteName(false));
							}
							nle.setDescription(p.description);
						}
					}
					nl.addEntry(nle);
					nll.add(nl);
				}
			}
		}
	}

	/**
	 Reads the NetworkList and NetworkListEntry tables.
	 @param nll the list of network lists
	 */
	public void read(NetworkListList nll)
			throws SQLException, DatabaseException
	{
		read(nll, null);
	}
	
	/**
	 * Non-cached, stand-alone method to read the list of network list 
	 * specs currently defined in the database.
	 * @return ArrayList of currently defined network list specs.
	 */
	public ArrayList<NetworkListSpec> getNetlistSpecs()
		throws SQLException
	{
		log.debug("Reading NetworkList Specs...");
		// Initialize our links into the Database

		String q = "SELECT id, name, transportMediumType, "
			+ "siteNameTypePreference, lastModifyTime FROM NetworkList";

		ArrayList<NetworkListSpec> ret = new ArrayList<NetworkListSpec>();
		try (Statement stmt = createStatement())
		{
			try (ResultSet rs = stmt.executeQuery(q))
			{
				while(rs.next())
				{
					ret.add(new NetworkListSpec(DbKey.createDbKey(rs, 1), rs.getString(2),
							rs.getString(3), rs.getString(4),
							getTimeStamp(rs, 5, new Date()), 0));
				}
			}
			q = "select networkListId, count(1) as \"numdcps\""
				+ " from NetworkListEntry group by networkListId";
			try (ResultSet rs = stmt.executeQuery(q))
			{
				while(rs.next())
				{
					DbKey id = DbKey.createDbKey(rs, 1);
					for(NetworkListSpec spec : ret)
					{
						if (spec.getId().equals(id))
						{
							spec.setNumEntries(rs.getInt(2));
							break;
						}
					}
				}
			}
			return ret;
		}
	}

	/**
	  Reads a network list from the database into the passed object.
	  The passed object must contain either a valid ID or name.
	  @param nl the network list
	*/
	public void readNetworkList(NetworkList nl)
		throws DatabaseException, SQLException
	{
		initDb(nl);
		DbKey id = nl.getId();
		if (DbKey.isNull(id))
		{
			id = name2id(nl.name);    // will throw if unsuccessful
			nl.setId(id);
		}

		String q = "SELECT * FROM NetworkList WHERE id = " + id;
		log.trace("Executing '{}' to read netlist '{}'", q, nl.name);
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(q);)
		{
			if (!rs.next())
			{
				throw new DatabaseException("No NetworkList found with ID " + id);
			}

			populateNetworkList(nl, rs);
		}
		readNetworkListEntries(nl);
	}

	/**
	  Gets the last-modify time for a network list.
	  @param nl the network list
	  @return the last-modify time for a network list.
	*/
	public Date getLMT(NetworkList nl)
		throws DatabaseException, SQLException
	{
		if ((nl == NetworkList.dummy_all || nl == NetworkList.dummy_production)
		 && _platformList != null)
			return _platformList.getLastModified();

		DbKey id = nl.getId();
		if (id.isNull())
		{
			id = name2id(nl.name);    // will throw if unsuccessful
			nl.setId(id);
		}
		String q = "SELECT lastModifyTime FROM NetworkList WHERE ID = " + id;
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(q);)
		{
			if (!rs.next())
			{
				throw new DatabaseException("No NetworkList found with ID " + id);
			}
			Date ret = getTimeStamp(rs, 1, (Date)null);
			return ret;
		}
	}

	private void readNetworkListEntries(NetworkList nl)
		throws DatabaseException, SQLException
	{
			nl.clear();
			String nle_attributes = "networkListId, transportId";
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
				nle_attributes += ", platform_name, description";

			String q = "SELECT " + nle_attributes
				     + " FROM NetworkListEntry where NetworkListId = "
				     + nl.getId();

		try (Statement stmt = createStatement();
			 ResultSet rs_nle = stmt.executeQuery(q);)
		{
			while (rs_nle.next())
			{
				String transportId = rs_nle.getString(2);

				NetworkListEntry nle = new NetworkListEntry(nl, transportId);
				// DB Version 11 has name and description in each Netlist Entry.
				if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
				{
					nle.setPlatformName(rs_nle.getString(3));
					nle.setDescription(rs_nle.getString(4));
				}
				else
				{
					Platform p = _platformList.getPlatform(nl.transportMediumType,
						transportId);

					if (p != null)
					{
						//Find the right site name for this network list site
						//name type preference
						Site pSite = p.getSite();
						if (pSite != null)
						{	//FIRST - see if it can find a site name for this type
							SiteName sn = pSite.getName(nl.siteNameTypePref);
							if (sn != null)
								nle.setPlatformName(sn.getNameValue());
							else
							{
								nle.setPlatformName(p.getSiteName(false));
							}
						}
						else
						{
							nle.setPlatformName(p.getSiteName(false));
						}

						nle.setDescription(p.description);
					}
				}

				nl.addEntry(nle);
			}
		}
	}

	/**
	* This uses the data in a single row of a ResultSet to populate a
	* NetworkList object.  The ID is used to determine which
	* NetworkList object should get the data.
	* If the NetworkList with that ID is already
	* in memory, then it is used.  Otherwise, a new NetworkList is
	* created.
	* The ResultSet should have already been checked to see that the
	* current row contains valid data.
	* @param id the database ID
	* @param rs  the JDBC result set
	*/
	private NetworkList putNetworkList(DbKey id, ResultSet rs)
		throws DatabaseException, SQLException
	{
		NetworkList nl = _networkListList.getById(id);
		if (nl == null)
		{
			nl = new NetworkList();
			nl.setId(id);
		}
		populateNetworkList(nl, rs);

		return nl;
	}

	private void populateNetworkList(NetworkList nl, ResultSet rs)
		throws DatabaseException, SQLException
	{
		nl.name = rs.getString(2);
		nl.transportMediumType = rs.getString(3);
		nl.siteNameTypePref = rs.getString(4);
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			nl.lastModifyTime = getTimeStamp(rs, 5, (Date)null);
		else
		{
			/* Version 5 db doesn't have LMT. As a kludge, take the
			   current time and truncate back to last half-hour.
			   The routing-spec code will then reload all lists every
			   half hour. Also see kludge in SqlDatabaseIO.checkNetworkLists().
			*/
			long hours = System.currentTimeMillis() / 1800000L;
			nl.lastModifyTime = new Date(hours * 1800000L);
		}

		// add (or re-add) to list (guaranteed not to duplicate).
		_networkListList.add(nl);
	}

	/**
	* This writes a NetworkList to the database.  This will call
	* either insert(nl) or update(nl), depending on whether the
	* argument has a valid SQL database ID number.
	* @param nl the network list
	*/
	public void write(NetworkList nl)
		throws DatabaseException, SQLException
	{
		if (nl.idIsSet()) 
			update(nl);
		else
		{
			DbKey id = name2id(nl.name);
			if (!id.isNull())
			{
				nl.setId(id);
				update(nl);
			}
			else
				insert(nl);
		}
	}

	/**
	* Update an already-existing object in the SQL database.
	* @param nl the network list
	*/
	private void update(NetworkList nl)
		throws DatabaseException, SQLException
	{
		String q =
			"UPDATE NetworkList SET " +
			"Name = " + sqlReqString(nl.name) + ", " +
			"TransportMediumType=" + sqlOptString(nl.transportMediumType)+", " +
			"SiteNameTypePreference=" + sqlOptString(nl.siteNameTypePref);

		nl.lastModifyTime = new Date();
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", lastModifyTime=" + sqlDate(nl.lastModifyTime);

		q = q + " WHERE id = " + nl.getId();

		executeUpdate(q);

		// For the NetworkListEntries, we'll take the easy road, which
		// is to first delete them all from the database, and then to
		// re-insert them.

		deleteEntries(nl);
		insertAllEntries(nl);
	}

	/**
	* Insert a new NetworkList into the SQL database.
	* @param nl the network list
	*/
	private void insert(NetworkList nl)
		throws DatabaseException, SQLException
	{
		//System.out.println("	  NetworkListListIO.insert(nl)");

		DbKey id = getKey("NetworkList");
		nl.setId(id);

		String networkListAttrs = 
			"id, name, transportmediumtype, sitenametypepreference";
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			networkListAttrs += ", lastmodifytime";
			
		String q =
			"INSERT INTO NetworkList(" + networkListAttrs + ") VALUES (" +
			id + ", " +
			sqlReqString(nl.name) + ", " + 
			sqlOptString(nl.transportMediumType) + ", " +
			sqlOptString(nl.siteNameTypePref);

		nl.lastModifyTime = new Date();
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", " + sqlDate(nl.lastModifyTime);

		q += ")";
		executeUpdate(q);

		insertAllEntries(nl);
	}

	/**
	* Inserts all the NetworkListEntry's associated with a NetworkList.
	* @param nl the network list
	*/
	private void insertAllEntries(NetworkList nl)
		throws DatabaseException, SQLException
	{
		for(Iterator<NetworkListEntry> it = nl.iterator(); it.hasNext(); )
		{
			NetworkListEntry nle = it.next();
			insertNLE(nl.getId(), nle);
		}
	}

	/**
	* Insert a NetworkListEntry into the database.
	* @param nl_id the ID of the network list
	* @param nle the network list entry
	*/
	private void insertNLE(DbKey nl_id, NetworkListEntry nle)
		throws DatabaseException, SQLException
	{
		String nle_attributes = "networkListId, transportId";
		if (_dbio.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
			nle_attributes += ", platform_name, description";
		String q = "INSERT INTO NetworkListEntry(" + nle_attributes + ") VALUES("
			+ nl_id + ", "
			+ sqlReqString(nle.transportId);
		if (_dbio.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
		{
			String desc = nle.getDescription();
			String platname = nle.getPlatformName();
			if (_dbio.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_12
			 && platname != null && platname.length() > 24)
				platname = platname.substring(0, 24);
				
			if (desc != null && desc.length() > 80)
				desc = desc.substring(0, 80);
			q = q + ", " + sqlOptString(platname)
				+ ", " + sqlOptString(desc);
		} 
		q += ")";
		executeUpdate(q);
	}

	/**
	* Delete a NetworkList object from the database, using its ID number.
	* @param nl the network list
	*/
	public void delete(NetworkList nl)
		throws DatabaseException, SQLException
	{
		deleteEntries(nl);
		String q = "DELETE FROM NetworkList WHERE ID = " + nl.getId();
		executeUpdate(q);
	}

	/**
	* Deletes all NetworkListEntry's belonging to a NetworkList.
	* @param nl the network list
	*/
	private void deleteEntries(NetworkList nl)
		throws DatabaseException, SQLException
	{
		String q = "DELETE FROM NetworkListEntry " +
					 "WHERE NetworkListId = " + nl.getId();
		tryUpdate(q);
	}

	private DbKey name2id(String name)
		throws DatabaseException, SQLException
	{
		try (Statement stmt = createStatement();
		     ResultSet rs = stmt.executeQuery("SELECT id FROM NetworkList where name = " + sqlReqString(name));)
		{
			DbKey ret = Constants.undefinedId;
			if (rs.next())
			{
				ret = DbKey.createDbKey(rs, 1);
			}
			return ret;
		}
	}
}

