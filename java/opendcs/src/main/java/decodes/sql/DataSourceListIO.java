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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.ValueNotFoundException;
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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.trace("Reading DataSources");

		_dsList = dsList;

		HashMap<DbKey, DataSource> id2ds = new HashMap<DbKey, DataSource>();

		try (Statement stmt = createStatement())
		{
			try (ResultSet rs = stmt.executeQuery("SELECT id, name, dataSourceType, dataSourceArg "
				+ "FROM DataSource"))
			{
				while (rs.next())
				{
					DataSource ds = rs2ds(rs);
					id2ds.put(ds.getKey(), ds);
				}
			}


			try (ResultSet rs = stmt.executeQuery("SELECT groupId, sequenceNum, memberId FROM DataSourceGroupMember"))
			{
				while (rs.next())
				{
					DbKey groupId = DbKey.createDbKey(rs, 1);
					int seqNum = rs.getInt(2);
					DbKey memberId = DbKey.createDbKey(rs, 3);

					DataSource group = id2ds.get(groupId);
					if (group == null)
					{
						log.warn("Parent groupId={} does not refer to a valid data source group!", groupId);
						continue;
					}
					DataSource member = id2ds.get(memberId);
					if (member == null)
					{
						log.warn("Data source child groupId={} does not refer to a valid group!", memberId);
						continue;
					}
					group.addGroupMember(seqNum, member);
				}
			}
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
		_dsList = dbObj.getDatabase().dataSourceList;
		DataSource ret = _dsList.getById(id);
		if (ret != null)
			return ret;
		
		return readDS(id);
	}
	
	public DataSource readDS(DbKey id)
		throws DatabaseException
	{
		String q = "SELECT id, name, dataSourceType, dataSourceArg " +
					   "FROM DataSource WHERE id = " + id;
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(q);)
		{
			// There should be only one row in the result set
			if (!rs.next())
				throw new ValueNotFoundException(
					"No DataSource found with id " + id);

			DataSource ret = rs2ds(rs);
			if (_dsList != null)
				_dsList.add(ret);
			resolveGroupMembers(ret);
			return ret;
		}
		catch (SQLException ex)
		{
			throw new DatabaseException("Unable to read DataSource", ex);
		}
	}

	private DataSource rs2ds(ResultSet rs)
		throws DatabaseException, SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		String name = rs.getString(2);
		String type = rs.getString(3);
		String arg = rs.getString(4);

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


		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(
				"SELECT groupId, sequenceNum, memberId " +
				"FROM DataSourceGroupMember WHERE GroupId = " + ds.getKey()
				+ " order by sequenceNum");)
		{
			while (rs.next())
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
		log.trace("Updating data source '{}' id={}", ds.getName(), ds.getId());

		String q = "UPDATE DataSource SET " +
			"Name = " + sqlReqString(ds.getName()) + ", " +
			"dataSourceType = " +
			sqlReqString(ds.dataSourceType) + ", " +
			"dataSourceArg = " + escapeString(ds.getDataSourceArg()) + " " +
		   "WHERE ID = " + ds.getId();
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
		
		log.trace("Inserting data source '{}' new id={}", ds.getName(), ds.getId());

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

		log.trace("Data Source '{}' with ID={} will have the following members:", ds.getName(), ds.getId());
		int seq=0;
		for (int i = 0; i < members.size(); ++i) 
		{
			DataSource m = members.get(i);
			if (m == null)
				continue;
			
			log.trace("    Member '{}' with ID={}", m.getName(), m.getId());
			
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
		DbKey ret = Constants.undefinedId;
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(
				"SELECT id FROM DataSource where name = "
				+ sqlReqString(name));)
		{
			if (rs.next())
			{
				ret = DbKey.createDbKey(rs, 1);
			}
		}
		return ret;
	}

}

