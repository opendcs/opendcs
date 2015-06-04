/*
 * $Id$
 *
 * Open Source Software
 *
 * $Log$
 * Revision 1.4  2014/08/29 18:22:50  mmaloney
 * 6.1 Schema Mods
 *
 * Revision 1.3  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.2  2014/06/27 20:18:19  mmaloney
 * New columns in Network List Entry table for DB version 11.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.7  2013/04/23 13:45:28  mmaloney
 * Add join to NetworkList so that CWMS adds office ID predicate.
 *
 * Revision 1.6  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.5  2010/12/08 13:40:49  mmaloney
 * Specify Columns in INSERT statements.
 *
 * Revision 1.4  2010/11/28 21:05:25  mmaloney
 * Refactoring for CCP Time-Series Groups
 *
 * Revision 1.3  2008/09/29 00:22:08  mjmaloney
 * Network List Maintenance GUI Improvements
 *
 * Revision 1.2  2008/09/28 19:22:44  mjmaloney
 * Added <all> and <production> network lists
 *
 * Revision 1.1  2008/04/04 18:21:04  cvs
 * Added legacy code to repository
 *
 * Revision 1.16  2008/03/13 16:48:06  mmaloney
 * Fixed bug in multiple add-site to netlist.
 *
 * Revision 1.15  2008/01/21 15:21:40  mmaloney
 * modified files
 *
 * Revision 1.13  2007/07/17 15:12:44  mmaloney
 * dev
 *
 * Revision 1.12  2007/04/25 13:18:11  ddschwit
 * Changed SELECT * to SELECT columns
 *
 * Revision 1.11  2004/09/02 12:15:28  mjmaloney
 * javadoc
 *
 * Revision 1.10  2003/11/17 14:53:49  mjmaloney
 * dev.
 *
 * Revision 1.9  2003/11/15 20:28:36  mjmaloney
 * Mods to transparently support either V5 or V6 database.
 *
 * Revision 1.8  2002/10/19 23:13:59  mjmaloney
 * Added SQL method to read a single network list.
 *
 * Revision 1.7  2002/10/06 14:23:58  mjmaloney
 * SQL Development.
 *
 * Revision 1.6  2002/10/04 13:32:12  mjmaloney
 * SQL dev.
 *
 * Revision 1.5  2002/09/20 12:59:07  mjmaloney
 * SQL Dev.
 *
 * Revision 1.4  2002/08/29 05:48:50  chris
 * Added RCS keyword headers.
 *
 *
 */

package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;

import ilex.util.Logger;

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
	public void initDb(Database db)
	{
		_platformList = db.platformList;
		_networkListList = db.networkListList;
	}

	/**
	* Initializes this object's links into the Database from a
	* DatabaseObject.
	*/
	public void initDb(DatabaseObject dbObj)
	{
		 initDb(dbObj.getDatabase());
	}

	/** 
	  Reads the NetworkList and NetworkListEntry tables.  
	  @param nll the list of network lists
	*/
	public void read(NetworkListList nll)
		throws SQLException, DatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG1,"Reading NetworkLists...");
		// Initialize our links into the Database
		initDb(nll.getDatabase());

		String q = "SELECT * FROM NetworkList";
		
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery( q );

		if (rs != null) 
		{
			while (rs.next()) 
			{
				putNetworkList(DbKey.createDbKey(rs, 1), rs);
			}
		}

		// For CWMS, by joining with NetworkList, VPD will automatically
		// add the predicate to filter by office id. For other DBs, the
		// join does nothing, but does no harm.
		String nleColumns = "a.networkListId, a.transportId";
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
			nleColumns += ", a.platform_name, a.description";
		
		q = "SELECT " + nleColumns
		  + " FROM NetworkListEntry a, NetworkList b "
		  + "WHERE a.networkListId = b.id ";
		
		ResultSet rs_nle = stmt.executeQuery( q );

		while (rs_nle != null && rs_nle.next()) 
		{
			DbKey id = DbKey.createDbKey(rs_nle, 1);
			String transportId = rs_nle.getString(2);

			NetworkList nl = _networkListList.getById(id);
			if (nl == null)
			{
				Logger.instance().log(Logger.E_WARNING,
				  "Orphan network list entry with invalid network list ID "
					+ id + ", ignored.");
				continue;
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
							nle.setPlatformName(sn.getNameValue());
						else
							nle.setPlatformName(p.getSiteName(false));
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
	
	/**
	 * Non-cached, stand-alone method to read the list of network list 
	 * specs currently defined in the database.
	 * @return ArrayList of currently defined network list specs.
	 */
	public ArrayList<NetworkListSpec> getNetlistSpecs()
		throws SQLException
	{
		Logger.instance().debug1("Reading NetworkList Specs...");
		// Initialize our links into the Database

		String q = "SELECT id, name, transportMediumType, "
			+ "siteNameTypePreference, lastModifyTime FROM NetworkList";
		
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery( q );
		ArrayList<NetworkListSpec> ret = new ArrayList<NetworkListSpec>();
		while(rs != null && rs.next())
			ret.add(
				new NetworkListSpec(DbKey.createDbKey(rs, 1), rs.getString(2),
					rs.getString(3), rs.getString(4), 
					getTimeStamp(rs, 5, new Date()), 0));
		
		q = "select networkListId, count(1) as \"numdcps\""
			+ " from NetworkListEntry group by networkListId";
		rs = stmt.executeQuery( q );
		while(rs != null && rs.next())
		{
			DbKey id = DbKey.createDbKey(rs, 1);
			for(NetworkListSpec spec : ret)
				if (spec.getId().equals(id))
				{
					spec.setNumEntries(rs.getInt(2));
					break;
				}
		}
		return ret;
	}


	/**
	* Retrieve a NetworkList by ID number.
	* If the desired NetworkList is not in memory, this attempts to read it
	* from the Database.
	* @param dbObj exists to allow this method to get the correct Database.
	* @param id the database ID
	* @throws DatabaseException  if no NetworkList corresponding to the
	* indicated id number is found.
	*/
	public NetworkList getNetworkList(DatabaseObject dbObj, DbKey id)
		throws DatabaseException, SQLException
	{
		initDb(dbObj);
		NetworkList nl = _networkListList.getById(id);
		if (nl != null)
			return nl;

		return readNetworkList(id);
	}

	/**
	* This reads one NetworkList from the database, including all its
	* ancillary data (NetworkListEntry (and others?)).
	* If a NetworkList with the
	* desired ID number is already in memory, this re-reads its data.
	* This returns a reference to the NetworkList.
	* @param id the database ID
	* @throws DatabaseException if no object with the indicated ID exists
	* in the database.
	*/
	public NetworkList readNetworkList(DbKey id)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		
		String q = "SELECT * FROM NetworkList WHERE id = " + id;
		
		ResultSet rs = stmt.executeQuery( q );

		if (rs == null || !rs.next())
			throw new DatabaseException(
				"No NetworkList found with ID " + id);

		NetworkList nList = putNetworkList(id, rs);
		stmt.close();

		readNetworkListEntries(nList);

		return nList;
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
		if (id.isNull())
		{
			id = name2id(nl.name);    // will throw if unsuccessfull
			nl.setId(id);
		}

		Statement stmt = createStatement();
		
		String q = "SELECT * FROM NetworkList WHERE id = " + id;

		Logger.instance().log(Logger.E_DEBUG1,
			"Executing '" + q + "' to read netlist '" + nl.name + "'");
		ResultSet rs = stmt.executeQuery(q);

		if (rs == null || !rs.next())
			throw new DatabaseException(
				"No NetworkList found with ID " + id);

		populateNetworkList(nl, rs);

		stmt.close();

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
			id = name2id(nl.name);    // will throw if unsuccessfull
			nl.setId(id);
		}

		Statement stmt = createStatement();
		String q = "SELECT lastModifyTime FROM NetworkList WHERE ID = " + id;
		ResultSet rs = stmt.executeQuery(q);
		if (rs == null || !rs.next())
			throw new DatabaseException(
				"No NetworkList found with ID " + id);
		Date ret = getTimeStamp(rs, 1, (Date)null);
		stmt.close();
		return ret;
	}

	private void readNetworkListEntries(NetworkList nl)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		String nle_attributes = "networkListId, transportId";
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
			nle_attributes += ", platform_name, description";
		
		String q = "SELECT " + nle_attributes
			     + " FROM NetworkListEntry where NetworkListId = "
			     + nl.getId();

		ResultSet rs_nle = stmt.executeQuery(q);

		while (rs_nle != null && rs_nle.next())
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
		stmt.close();
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
	public NetworkList putNetworkList(DbKey id, ResultSet rs)
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
	public void update(NetworkList nl)
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
	public void insert(NetworkList nl)
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
	public void insertAllEntries(NetworkList nl)
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
	public void deleteEntries(NetworkList nl)
		throws DatabaseException, SQLException
	{
		String q = "DELETE FROM NetworkListEntry " +
					 "WHERE NetworkListId = " + nl.getId();
		tryUpdate(q);
	}

	private DbKey name2id(String name)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM NetworkList where name = "
			+ sqlReqString(name));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}
}

