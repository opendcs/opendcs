/*
* $Id$
* 
* Open Source Software
* 
* $Log$
* Revision 1.5  2013/03/21 18:27:39  mmaloney
* DbKey Implementation
*
*/

package decodes.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.TextUtil;
import opendcs.dao.DaoBase;
import ilex.util.Pair;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.DataPresentation;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;


/**
* This class handles reading and writing the PresentationGroupList from/to
* the SQL database.
*/
public class PresentationGroupListIO extends SqlDbObjIo
{
	/**
	* Transient storage for PresentationGroupList being read or written.
	*/
	PresentationGroupList _pgList;
	
	/** A rouding rule with this upper limit means good for all */
	private static final double SQL_MAX_DOUBLE = 999999999999.0;

	
	/** 
	  Constructor. 
	  @param dbio the SqlDatabaseIO to which this IO object belongs
	*/
	public PresentationGroupListIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}

	/** 
	  Reads in the PresentationGroupList from the SQL database. 
	  @param pgList the PresentationGroupList to populate
	*/
	public void read(PresentationGroupList pgList)
		throws SQLException, DatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG1,"Reading PresentationGroups...");

		_pgList = pgList;

		// This temporary Vector will be used to store the parent IDs
		// of each PresentationGroup, if any.  (I.e. the group that this
		// group inherits from).
		Vector<Pair> parents = new Vector<Pair>();

		String q =
			"SELECT id, name, inheritsFrom, " +
			"lastModifyTime, isProduction " +
			"FROM PresentationGroup";

		try(Connection conn = getConnection();
			DaoBase dao = new DaoBase(this._dbio,"Presentation",conn);)
		{
			dao.doQuery(q, rs -> {
				DbKey id = DbKey.createDbKey(rs, 1);

			// We may already have this PG in memory. If so, skip it.
			if (_pgList.getById(id) != null)
			{
				return;
			}

			// Make a new PresentationGroup object out of this
			// ResultSet, but defer resolving any "inheritsFrom"
			// values
			try
			{
				makePG(rs, false);
			}
			catch(DatabaseException ex)
			{
				throw new SQLException("Unable to make presentation group object.",ex);
			}

			// Get the InheritsFrom field, and, if not null, store it
			DbKey inheritsFromId = DbKey.createDbKey(rs, 3);
			if (!inheritsFromId.isNull())
				parents.add(new Pair(id, inheritsFromId));
			});
		}

		// Now resolve all the parents:
		for (int i = 0; i < parents.size(); ++i) 
		{
			Pair p = (Pair) parents.get(i);
			DbKey id = (DbKey)p.first;
			DbKey inheritsFromId = (DbKey)p.second;

			PresentationGroup pg = _pgList.getById(id);

			PresentationGroup parent = _pgList.getById(inheritsFromId);
			if (parent != null)
				pg.inheritsFrom = parent.groupName;
		}
	}

	/**
	* Get a PresentationGroup object by its ID number.
	* This first looks in the list in memory, and if not found there, this
	* attempts to go to the database to find it.
	* This is necessary to ensure that the PresentationGroupList is from
	* the same Database as the PresentationGroup.
	*
	* @param conn Connection used for queries
	* @param dbObj used, if necessary, to retrieve the correct database
	* @param id the database ID
	* @throws  DatabaseException if no matching PresentationGroup is found.
	*/
	private PresentationGroup get(Connection conn, DatabaseObject dbObj, DbKey id)
		throws DatabaseException
	{
		_pgList = dbObj.getDatabase().presentationGroupList;

		PresentationGroup pg = _pgList.getById(id);
		if (pg == null)
		{
			pg = new PresentationGroup();
			pg.setId(id);
			readPresentationGroup(conn, pg, true);
		}

		return pg;
	}

	/**
	* Get a PresentationGroup object by its ID number.
	* This first looks in the list in memory, and if not found there, this
	* attempts to go to the database to find it.
	* This is necessary to ensure that the PresentationGroupList is from
	* the same Database as the PresentationGroup.
	*
	* @param dbObj used, if necessary, to retrieve the correct database
	* @param id the database ID
	* @throws  DatabaseException if no matching PresentationGroup is found.
	*/
	private PresentationGroup get(DatabaseObject dbObj, DbKey id)
		throws DatabaseException
	{
		try (Connection conn = getConnection();)
		{
			return get(conn,dbObj,id);
		}
		catch (SQLException ex)
		{
			throw new DatabaseException("Unable to get Presentation Group",ex);
		}
	}

	/**
	  Passed a partially read PresentationGroup, read the entire contents
	  from the database and fill-in the passed object.
	  @param pg the PresentationGroup
	  @param resolveInh if true, resolve inheritance also
	*/
	public void readPresentationGroup(PresentationGroup pg, boolean resolveInh) throws DatabaseException
	{
		try(Connection conn = getConnection();)
		{
			readPresentationGroup(conn, pg, resolveInh);
		}
		catch (SQLException ex)
		{
			throw new DatabaseException("Unable to read Presentation group " + pg.getDisplayName(),ex);
		}
	}

	/**
	  * Passed a partially read PresentationGroup, read the entire contents
	  * from the database and fill-in the passed object.
	  * @param conn connection to use for this request
	  * @param pg the PresentationGroup
	  * @param resolveInh if true, resolve inheritance also
	  *
	  */
	private void readPresentationGroup(Connection conn, PresentationGroup pg, boolean resolveInh)
		throws DatabaseException
	{
		if (pg.getId() == Constants.undefinedId
		 && pg.groupName != null && pg.groupName.length() > 0)
		{
			try { pg.setId(name2id(pg.groupName)); }
			catch(SQLException ex) { pg.clearId(); }
		}
		if (!pg.idIsSet())
			throw new DatabaseException(
				"Cannot retrieve PresentationGroup with no name or ID.");

		String query = "SELECT id, name, inheritsFrom, " +
			           "lastModifyTime, isProduction " +
			           "FROM PresentationGroup " + 
			           "WHERE id = ?";
		try (DaoBase dao = new DaoBase(this._dbio,"Presentation",conn);)
		{
			Boolean found = dao.getSingleResult(query,(rs)-> {
				pg.groupName = rs.getString(2);
				// Add to database list of all PGs:
				pg.getDatabase().presentationGroupList.add(pg);

				// Resolve inheritance if we're supposed to:
				if (resolveInh)
				{
					DbKey inheritsFromId = DbKey.createDbKey(rs, 3);
					if (!inheritsFromId.isNull())
					{
						try
						{
							PresentationGroup inheritsFrom = get(conn, pg, inheritsFromId);
							pg.inheritsFrom = inheritsFrom.groupName;
						}
						catch(DatabaseException ex)
						{
							throw new SQLException("Unable to get inherited group.",ex);
						}
					}
				}

				pg.lastModifyTime = getTimeStamp(rs, 4, pg.lastModifyTime);
				pg.isProduction = TextUtil.str2boolean(rs.getString(5));
				readDataPresentations(conn, pg, pg.getId());
				return true;
			},pg.getId());

			if (found != true)
			{
				throw new DatabaseException("No PresentationGroup found with id " + pg.getId());
			}
		}
		catch (SQLException sqle) 
		{
			throw new DatabaseException("Error on query '" + query
				+ "': " + sqle.toString(),sqle);
		}
	}

	/**
	* Create a new PresentationGroup object from the current row in
	* a ResultSet.  This returns the new object created.
	* If the argument resolveInh is true, then, if this PresentationGroup
	* inherits from another, this method will attempt to resolve that
	* iheritance by getting the parent PresentationGroup.  If the argument
	* resolveInh is false, however, this method will not attempt to
	* resolve any inheritances.
	* @param rs  the JDBC result set
	  @param resolveInh if true, resolve inheritance also
	  @param readDPs if true, read the data presentation elements.
	*/
	private PresentationGroup makePG(ResultSet rs, boolean resolveInh)
		throws DatabaseException, SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		String name = rs.getString(2);
		// Create the new PresentationGroup object

		PresentationGroup pg = new PresentationGroup(name);
		pg.setId(id);
		_pgList.add(pg);

		// Resolve the inheritance, if we're spozed to

		if (resolveInh) 
		{
			DbKey inheritsFromId = DbKey.createDbKey(rs, 3);

			if (!inheritsFromId.isNull())
			{
				PresentationGroup inheritsFrom = get(pg, inheritsFromId);
				pg.inheritsFrom = inheritsFrom.groupName;
			}
		}

		// Fill in the rest of the fields from the ResultSet.

		pg.lastModifyTime = getTimeStamp(rs, 4, pg.lastModifyTime);
		//Timestamp ts = rs.getTimestamp(4);
		//if (!rs.wasNull())
			//pg.lastModifyTime = ts;

		pg.isProduction = TextUtil.str2boolean(rs.getString(5));
//MJM 20090109 - Don't read the DataPresentations
//		readDataPresentations(pg, id);

		return pg;
	}

	/**
	* This reads the DataPresentation objects associated with
	* a PresentationGroup.
	* @param conn Connection used to query the database
	* @param pg the PresentationGroup
	* @param pgId the PresentationGroup database ID
	*/
	private void readDataPresentations(Connection conn, PresentationGroup pg, DbKey pgId)
		throws SQLException
	{
		try (DaoBase dao = new DaoBase(this._dbio,"Presentation",conn);)
		{
			StringBuilder q = new StringBuilder();
			q.append("SELECT DataPresentation.id, DataTypeId, UnitAbbr, " +
				"EquipmentID, Standard, Code");

			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				q.append(", maxDecimals");
			}
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			{
				q.append(", MAX_VALUE, MIN_VALUE");
			}
			q.append(" FROM DataPresentation, DataType " +
				"WHERE GroupId = ?" +
				" AND DataTypeId = DataType.id");

			dao.doQuery(q.toString(),rs -> makeDataPresentation(rs,pg),pgId);
		}
		catch (SQLException ex)
		{
			throw new SQLException("Unable to read specific presentation group.",ex);
		}
	}

	/**
	* This method makes a new DataPresentation from a single row of a
	* ResultSet.
	* @param rs  the JDBC result set
	* @param pg the PresentationGroup
	*/
	private void makeDataPresentation(ResultSet rs, PresentationGroup pg)
		throws SQLException
	{
		try
		{
			DbKey dpId = DbKey.createDbKey(rs, 1);
			DbKey dataTypeId = DbKey.createDbKey(rs, 2);
			String unitAbbr = rs.getString(3);

			String st = rs.getString(5);
			String code = rs.getString(6);

			DataPresentation dp = new DataPresentation(pg);
			dp.setId(dpId);

			dp.setUnitsAbbr(unitAbbr);
			dp.setDataType(pg.getDatabase().dataTypeSet.get(dataTypeId, st, code));

			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				int md = rs.getInt(7);
				if (rs.wasNull())
					dp.setMaxDecimals(Integer.MAX_VALUE);
				else
					dp.setMaxDecimals(md);
			}
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			{
				double v = rs.getDouble(8);
				if (!rs.wasNull())
					dp.setMaxValue(v);
				v = rs.getDouble(9);
				if (!rs.wasNull())
					dp.setMinValue(v);
			}

			pg.addDataPresentation(dp);
		}
		catch (DatabaseException ex)
		{
			throw new SQLException("Unable to set data Presentation ID.",ex);
		}
	}

	/**
	* Write a PresentationGroup out to the database.  The object might
	* either be new or old-and-changed.
	* @param pg the PresentationGroup
	*/
	public void write(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		if (pg.idIsSet())
			update(pg);
		else
		{
			DbKey id = name2id(pg.groupName);
			if (!id.isNull())
			{
				pg.setId(id);
				update(pg);
			}
			else
				insert(pg);
		}
	}

	/**
	* Update an existing PresentationGroup with changed data.
	* This also goes through each of the DataPresentations and
	* RoundingRules, deleting, inserting or updating them in the
	* database, as needed.
	*
	* @param pg the PresentationGroup
	*/
	private void update(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		String q =
			"UPDATE PresentationGroup SET " +
			  "Name = " + sqlReqString(pg.groupName) + ", " +
			  "InheritsFrom = " + parentSqlStr(pg) + ", " +
			  "LastModifyTime = " + sqlDate(pg.lastModifyTime) + ", " +
			  "IsProduction = " + sqlString(pg.isProduction) + " " +
			"WHERE ID = " + pg.getId();

		executeUpdate(q);

		// Next, delete and then re-insert each DataPresentation
		deleteAllDataPresentations(pg);
		insertAllDataPresentations(pg);
	}

	/**
	* This deletes all the DataPresentations associated with a
	* PresentationGroup.  Note that all the associated RoundingRules
	* will also be deleted.
	* This also goes through the heirarchy and unsets any SQL DB ID
	* values that might have been set.
	* The DataPresentations might or might not have their ID set.
	* @param pg the PresentationGroup
	*/
	private void deleteAllDataPresentations(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		DbKey id = pg.getId();

		// First delete all the RoundingRules.  This is tricky, because
		// the DataPresentations which refer to them might be gone.

		// MJM 20050606 Modification for Oracle Compatibility:
		// String q = "DELETE FROM RoundingRule " +
		//		   "WHERE DataPresentationID = DataPresentation.ID " +
		//		   "AND DataPresentation.GroupId = " + id;
		String q = "DELETE FROM RoundingRule " +
                   "WHERE EXISTS (SELECT 'x' FROM DataPresentation " +
               "WHERE RoundingRule.DataPresentationID = DataPresentation.ID " +
               "AND DataPresentation.GroupId = " + id + ")";
		tryUpdate(q);

		// Next, delete all the DataPresentations from the database.
		// Again, we won't count on the set in the database being the
		// same as the current children of the PresentationGroup.
		q = "DELETE FROM DataPresentation " +
			"WHERE GroupId = " + id;
		tryUpdate(q);

		// Now just unset the SQL database ID of any of the
		// DataPresentation children
		Vector<DataPresentation> v = pg.dataPresentations;
		for (int i = 0; i < v.size(); ++i) 
		{
			DataPresentation dp = v.get(i);
			if (dp.idIsSet()) 
				dp.clearId();
		}
	}

	/**
	* Insert a new PresentationGroup into the database.
	* This assigns the object a new ID number.
	* This also inserts all the DataPresentations and RoundingRules
	* belonging to this group.
	* In this case, we are sure that all of the kids and grandkids are
	* new.
	* @param pg the PresentationGroup
	*/
	private void insert(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("PresentationGroup");
		pg.setId(id);

		// re-insert with ID - list guarantees no duplicate.
		pg.getDatabase().presentationGroupList.add(pg);

		String q =
			"INSERT INTO PresentationGroup(id, name, inheritsfrom, lastmodifytime, "
			+ "isproduction) VALUES (" +
			  id + ", " +
			  sqlReqString(pg.groupName) + ", " +
			  parentSqlStr(pg) + ", " +
			  sqlDate(pg.lastModifyTime) + ", " +
			  sqlString(pg.isProduction) +
			")";

		executeUpdate(q);

		insertAllDataPresentations(pg);
	}

	/**
	* This inserts all the DataPresentations of a PresentationGroup.
	* @param pg the PresentationGroup
	*/
	private void insertAllDataPresentations(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		Vector<DataPresentation> v = pg.dataPresentations;
		for (int i = 0; i < v.size(); ++i)
			insert(v.get(i));
	}

	/**
	* Insert a new DataPresentation into the database.
	* We assume that this DataPresentation does not have its SQL DB ID
	* set (that's what identifies it as new).
	* However, it must have its myGroup member set, to point to its
	* parent PresentationGroup, which must not be new.
	* This also inserts all the RoundingRules belonging to this
	* DataPresentation.
	* @param dp the DataPresentation
	*/
	private void insert(DataPresentation dp)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("DataPresentation");
		dp.setId(id);
		PresentationGroup pg = dp.getGroup();

		if (dp.getDataType() == null)
			return;
		
		// MJM 5/4/07 - DT may be new. If so, add to Database & get ID.
		if (dp.getDataType().getId() == Constants.undefinedId)
			_dbio.writeDataType(dp.getDataType());

		String q =
			"INSERT INTO DataPresentation VALUES (" +
			  id + ", " +
			  pg.getId() + ", " +
			  dp.getDataType().getId() + ", " +
			  sqlString(dp.getUnitsAbbr()) + ", "
			  + "NULL"; // legacy equipment model id.

		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			int md = dp.getMaxDecimals();
			q = q + ", " + (md == Integer.MAX_VALUE ? "NULL" : ("" + md));
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			{
				q = q + ", " + sqlOptDouble(dp.getMaxValue());
				q = q + ", " + sqlOptDouble(dp.getMinValue());
			}
		}
		q = q + ")";

		// MJM 20201126 - Groups that came from XML files may have duplicates. So if the
		// input fails because it violates "pres_dt_unique" constraint, log a warning but do not
		// throw an exception.
		try { executeUpdate(q); }
		catch(SQLException ex)
		{
			if (ex.toString().toLowerCase().contains("pres_dt_unique"))
			{
				Logger.instance().warning("Failed to insert duplicate presentation entry "
					+ " for data type '" + dp.getDataType().toString() + "'");
			}
			else
				throw ex;
		}
	}

	/**
	* This computes the SQL string representation of the "InheritsFrom"
	* field, from a PresentationGroup's inheritsFrom member.
	* @param pg the PresentationGroup
	*/
	public String parentSqlStr(PresentationGroup pg)
		throws DatabaseException
	{
		if (pg.inheritsFrom == null ||
			pg.inheritsFrom.equals("")) return "NULL";

		PresentationGroup parentGroup = 
			pg.getDatabase().presentationGroupList.find(pg.inheritsFrom);
		if (parentGroup == null)
		{
			Logger.instance().warning("PresentationGroupListIO.parentSqlStr cannot "
				+ "find ID for parent group '" + pg.inheritsFrom + "'");
			return "NULL";
		}
		DbKey parentId = parentGroup.getId();
		return parentId.toString();
	}

	/**
	* Delete a PresentationGroup from the database.  This also deletes
	* the DataPresentation entities that belong to that group, and all
	* the RoundingRules belonging to all of those.
	* @param pg the PresentationGroup
	*/
	public void delete(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		DbKey id = pg.getId();

		String q = "";

		for (int i = 0; i < pg.dataPresentations.size(); ++i)
		{
			DataPresentation dp = pg.dataPresentations.get(i);

			// Delete all the RoundingRules belonging to this DataPresentation

			q = "DELETE FROM RoundingRule " +
				"WHERE DataPresentationID = " + dp.getId();
			tryUpdate(q);
		}

		// Finally, delete all the DataPresentations
		q = "DELETE FROM DataPresentation WHERE GroupId = " + id;
		tryUpdate(q);

		q = "DELETE FROM PresentationGroup WHERE ID = " + id;
		executeUpdate(q);
	}


	private DbKey name2id(String name)
		throws SQLException
	{
		try(Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT id FROM PresentationGroup where name = ?");)
		{
			stmt.setString(1,name);
			try(ResultSet rs = stmt.executeQuery();)
			{
				DbKey ret = Constants.undefinedId;
				if (rs != null && rs.next())
				{
					ret = DbKey.createDbKey(rs, 1);
				}
				return ret;
			}
		}
	}

	/**
	* Returns the last-modify-time for this presentation group in the database.
	* @param pg the PresentationGroup
	*/
	public Date getLMT(PresentationGroup pg)
	{
		try(Connection conn = getConnection();)
		{
			DbKey id = pg.getId();
			if (id != null && !id.isNull()) // NOTE: pretty sure this should be id == null || !id.isNull()
			{
				id = name2id(pg.groupName);    // will throw if unsuccessfull
				try { pg.setId(id); }
				catch(DatabaseException ex) {} // guaranteed not to happen.
			}
			String q = 
				"SELECT lastModifyTime FROM PresentationGroup WHERE id = ?";
			try(PreparedStatement stmt = conn.prepareStatement(q);)
			{
				stmt.setLong(1,id.getValue());
				try(ResultSet rs = stmt.executeQuery();)
				{
								// Should be only 1 record returned.
					if (rs == null || !rs.next())
					{
						Logger.instance().log(Logger.E_WARNING,
							"Cannot get SQL LMT for Presentation Group '"
							+ pg.groupName + "' id=" + pg.getId());
						return null;
					}

					Date ret = getTimeStamp(rs, 1, (Date)null);
					return ret;
				}
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"SQL Error reading LMT for Presentation Group '"
				+ pg.groupName + "' ID=" + pg.getId() + ": " + ex);
			return null;
		}
	}
}
