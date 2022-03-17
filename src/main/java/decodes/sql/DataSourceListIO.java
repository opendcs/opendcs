/*
* $Id$
*
* $Log$
* Revision 1.3  2016/07/20 15:41:39  mmaloney
* Simplify & refactoring.
*
* Revision 1.2  2015/03/19 15:23:14  mmaloney
* punch list
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.4  2013/03/21 18:27:39  mmaloney
* DbKey Implementation
*
* Revision 1.3  2010/12/08 13:40:49  mmaloney
* Specify Columns in INSERT statements.
*
* Revision 1.2  2008/10/04 14:50:18  mjmaloney
* DCP Monitor Improvements
*
* Revision 1.1  2008/04/04 18:21:04  cvs
* Added legacy code to repository
*
* Revision 1.15  2007/12/11 01:05:18  mmaloney
* javadoc cleanup
*
* Revision 1.14  2007/08/30 21:04:45  mmaloney
* dev
*
* Revision 1.13  2007/08/29 18:21:28  mmaloney
* added a check for null
*
* Revision 1.12  2007/04/25 13:57:31  ddschwit
* Changed SELECT * to SELECT columns
*
* Revision 1.11  2006/05/22 14:05:40  mmaloney
* dev
*
* Revision 1.10  2004/09/02 12:15:25  mjmaloney
* javadoc
*
* Revision 1.9  2003/11/15 20:28:34  mjmaloney
* Mods to transparently support either V5 or V6 database.
*
* Revision 1.8  2002/10/25 19:49:27  mjmaloney
* Removed extraneous debug messages.
*
* Revision 1.7  2002/10/11 01:27:01  mjmaloney
* Added SocketStreamDataSource and NoaaportPMParser stuff.
*
* Revision 1.6  2002/10/06 14:23:58  mjmaloney
* SQL Development.
*
* Revision 1.5  2002/10/04 13:32:11  mjmaloney
* SQL dev.
*
* Revision 1.4  2002/09/19 12:18:05  mjmaloney
* SQL Updates
*
* Revision 1.3  2002/08/29 05:48:49  chris
* Added RCS keyword headers.
*/

package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

import ilex.util.Logger;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.DataSource;
import decodes.db.DataSourceList;
import decodes.db.Constants;

/**
* This class handles reading and writing the DataSourceList from/to
* the SQL database.
*/
public class DataSourceListIO extends SqlDbObjIo
{
	/**
	* Transient storage for the DataSourceList that's being read or written.
	*/
	DataSourceList _dsList;

	/** Constructor. */
	public DataSourceListIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}

	//---------------------------------------------------------------------
	/**
	* Reads in the DataSourceList from the SQL database.
	*
	* Note:  because DataSource objects are referenced by other objects, it's
	* likely, when reading in a new SQL database from scratch, that some of
	* the DataSources will have already been instantiated in memory by the
	* time this is called.
	* <p>
	* Assume that the objects
	* in memory are correct.  Any pre-existing objects that were created
	* while reading in the entire SQL database, as the result of some
	* other object's referencing them, will be correct.
	*
	* @param dsList the DataSourceList to read
	*/
	public void read(DataSourceList dsList)
		throws SQLException, DatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG2, "Reading DataSources");

		_dsList = dsList;

		HashMap<DbKey, DataSource> id2ds = new HashMap<DbKey, DataSource>();
		Statement stmt = createStatement();
		try
		{
			ResultSet rs = stmt.executeQuery("SELECT id, name, dataSourceType, dataSourceArg "
				+ "FROM DataSource");

			while (rs != null && rs.next())
			{
				DataSource ds = rs2ds(rs);
				id2ds.put(ds.getKey(), ds);
			}

			rs = stmt.executeQuery("SELECT groupId, sequenceNum, memberId FROM DataSourceGroupMember");

			while (rs != null && rs.next())
			{
				DbKey groupId = DbKey.createDbKey(rs, 1);
				int seqNum = rs.getInt(2);
				DbKey memberId = DbKey.createDbKey(rs, 3);

				DataSource group = id2ds.get(groupId);
				if (group == null)
				{
					Logger.instance().warning(
						"Parent groupId=" + groupId + " does not refer to a valid data source group!");
					continue;
				}
				DataSource member = id2ds.get(memberId);
				if (member == null)
				{
					Logger.instance().warning(
						"Data source child groupId=" + memberId + " does not refer to a valid group!");
					continue;
				}
				group.addGroupMember(seqNum, member);
			}
		}
		finally
		{
			stmt.close();
		}
		for(DataSource ds : id2ds.values())
			dsList.add(ds);
	}

	/**
	* Get a DataSource object by its ID number.
	* This first looks in the list in memory, and if not found there, this
	* attempts to go to the database to find it.
	* This is necessary to ensure that the DataSourceList is from
	* the same Database as the DataSource.
	* @param dbObj used, if necessary, to retrieve the
	* DataSourceList object from the Database object.
	* @param id the surrogate key of the DataSource
	*
	* @throws  DatabaseException if no matching DataSource is found.
	*/
	public DataSource getDS(DatabaseObject dbObj, DbKey id)
		throws DatabaseException
	{
		//Logger.instance().log(Logger.E_DEBUG3, "DataSourceListIO.get ID=" + id);
		_dsList = dbObj.getDatabase().dataSourceList;
		DataSource ret = _dsList.getById(id);
		if (ret != null)
			return ret;
		
		return readDS(id);
	}
	
	public DataSource readDS(DbKey id)
		throws DatabaseException
	{
		Statement stmt = null;
		try
		{
			stmt = createStatement();
			
			String q = "SELECT id, name, dataSourceType, dataSourceArg " +
					   "FROM DataSource WHERE id = " + id;
			
			ResultSet rs = stmt.executeQuery(q);

			// There should be only one row in the result set
			if (rs == null || !rs.next())
				throw new DatabaseException(
					"No DataSource found with id " + id);

			DataSource ret = rs2ds(rs);
			_dsList.add(ret);
			resolveGroupMembers(ret);
			return ret;
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (stmt != null)
				try { stmt.close(); } catch(SQLException ex) {}
		}
	}

	private DataSource rs2ds(ResultSet rs)
		throws DatabaseException, SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		String name = rs.getString(2);
		String type = rs.getString(3);
		String arg = rs.getString(4);

//System.out.println("Creating new data source name=" + name + ", type=" + type);
		DataSource ds = new DataSource(name, type);
		ds.setId(id);
		ds.setDataSourceArg(arg);

		return ds;
	}

	/**
	* This resolves the group members, for those DataSource objects that
	* are groups.
	* @param ds the DataSource
	* @param id the database ID
	*/
	private void resolveGroupMembers(DataSource ds)
		throws DatabaseException, SQLException
	{
		if (!ds.isGroupType()) return;
		
		Statement stmt = null;
		try
		{
			stmt = createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT groupId, sequenceNum, memberId " +
				"FROM DataSourceGroupMember WHERE GroupId = " + ds.getKey()
				+ " order by sequenceNum");

			while (rs != null && rs.next())
			{
				int seqNum = rs.getInt(2);
				DbKey memberId = DbKey.createDbKey(rs, 3);
				DataSource member = readDS(memberId);
				ds.addGroupMember(seqNum, member);
			}
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
		finally 
		{
			if (stmt != null) try { stmt.close(); } catch(SQLException ex) {}
		}
	}


	/**
	* Write a DataSource out to the Database
	* @param ds the DataSource
	*/
	public void write(DataSource ds)
		throws DatabaseException, SQLException
	{

		if (ds.idIsSet())
		{
			update(ds);
		}
		else
		{
			DbKey id = name2id(ds.getName());
			if (id != null && !id.isNull())
			{
				ds.setId(id);
				update(ds);
			}
			else
				insert(ds);
		}
	}

	/**
	* Update a pre-existing DataSource into the database.
	* @param ds the DataSource
	*/
	private void update(DataSource ds)
		throws DatabaseException, SQLException
	{
		Logger.instance().debug3("Updating data source '" + ds.getName() + "' id=" + ds.getId());

		String q = "UPDATE DataSource SET " +
			"Name = " + sqlReqString(ds.getName()) + ", " +
			"dataSourceType = " +
			sqlReqString(ds.dataSourceType) + ", " +
			"dataSourceArg = " + escapeString(ds.getDataSourceArg()) + " " +
		   "WHERE ID = " + ds.getId();
//					   sqlReqEnumValId(_dstLookup, ds.dataSourceType) + ", " +
		executeUpdate(q);

		// Now do the group members.  Take the easy path, and just
		// delete them all and then re-insert them.
		deleteGroupMembers(ds);
		insertGroupMembers(ds);
	}

	/**
	* Insert a new DataSource into the database.
	* @param ds the DataSource
	*/
	private void insert(DataSource ds)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("DataSource");
		ds.setId(id);
		
		Logger.instance().debug3("Inserting data source '" + ds.getName() + "' new id=" + ds.getId());

		String q = "INSERT INTO DataSource(id, name, datasourcetype, datasourcearg) "
			+ "VALUES (" +
			id + ", " +
			sqlReqString(ds.getName()) + ", " +
			sqlReqString(ds.dataSourceType) + ", " +
			escapeString(ds.getDataSourceArg()) +
			 ")";
		executeUpdate(q);

		insertGroupMembers(ds);
	}

	/**
	* This inserts records into the DataSourceGroupMember table, one for
	* each member of a group-type DataSource.
	* @param ds the DataSource
	*/
	private void insertGroupMembers(DataSource ds)
		throws DatabaseException, SQLException
	{
		Vector<DataSource> members = ds.groupMembers;

		Logger.instance().debug3("Data Source '" + ds.getName()
			+ "' with ID=" + ds.getId() + " will have the following members:");
		int seq=0;
		for (int i = 0; i < members.size(); ++i) 
		{
			DataSource m = members.get(i);
			if (m == null)
				continue;
			
			Logger.instance().debug3("    Member '" + m.getName()
				+ "' with ID=" + m.getId());
			
			String q = "INSERT INTO DataSourceGroupMember VALUES (" +
						 ds.getId() + ", " +
						 seq + ", " + 
						 m.getId() + 
					   ")";
			seq++;
			executeUpdate(q);
		}
	}

	/**
	* Deletes a DataSource from the database.  The argument must have
	* a valid ID number.
	* @param ds the DataSource
	*/
	public void delete(DataSource ds)
		throws DatabaseException, SQLException
	{
		DbKey id = ds.getId();

		// First, for referential integrity, delete all the records, if any,
		// in DataSourceGroupMember for which this DataSource is the member.

		String q = "DELETE FROM DataSourceGroupMember " +
				   "WHERE MemberId = " + id;
		tryUpdate(q);

		q = "DELETE FROM DataSourceGroupMember " +
			   "WHERE GroupId = " + id;
		tryUpdate(q);

		q = "DELETE FROM DataSource WHERE ID = " + id;
		executeUpdate(q);

		deleteGroupMembers(ds);
	}

	/**
	* Deletes all the records in the DataSourceGroupMember for this
	* DataSource.
	* @param ds the DataSource
	*/
	private void deleteGroupMembers(DataSource ds)
		throws DatabaseException, SQLException
	{
		String q = "DELETE FROM DataSourceGroupMember " +
				   "WHERE GroupId = " + ds.getId();
		tryUpdate(q);
	}


	private DbKey name2id(String name)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM DataSource where name = "
			+ sqlReqString(name));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}

}

