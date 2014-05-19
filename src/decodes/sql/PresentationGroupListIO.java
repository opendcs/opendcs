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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.Pair;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DatabaseObject;
import decodes.db.DataPresentation;
import decodes.db.EquipmentModel;
import decodes.db.EquipmentModelList;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;
import decodes.db.RoundingRule;


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

		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id, name, inheritsFrom, " +
			"lastModifyTime, isProduction " +
			"FROM PresentationGroup"
		);

		while (rs != null && rs.next()) 
		{
			DbKey id = DbKey.createDbKey(rs, 1);

			// We may already have this PG in memory. If so, skip it.
			if (_pgList.getById(id) != null)
				continue;

			// Make a new PresentationGroup object out of this
			// ResultSet, but defer resolving any "inheritsFrom"
			// values
			makePG(rs, false);

			// Get the InheritsFrom field, and, if not null, store it
			DbKey inheritsFromId = DbKey.createDbKey(rs, 3);
			if (!inheritsFromId.isNull())
				parents.add(new Pair(id, inheritsFromId));
		}
		stmt.close();

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
	* @param dbObj used, if necessary, to retrieve the correct database
	* @param id the database ID
	* @throws  DatabaseException if no matching PresentationGroup is found.
	*/
	public PresentationGroup get(DatabaseObject dbObj, DbKey id)
		throws DatabaseException
	{
		_pgList = dbObj.getDatabase().presentationGroupList;

		PresentationGroup pg = _pgList.getById(id);
		if (pg == null)
		{
			pg = new PresentationGroup();
			pg.setId(id);
			readPresentationGroup(pg, true);
		}

		return pg;
	}

	/**
	  Passed a partially read PresentationGroup, read the entire contents
	  from the database and fill-in the passed object.
	  @param pg the PresentationGroup
	  @param resolveInh if true, resolve inheritance also
	*/
	public void readPresentationGroup(PresentationGroup pg, boolean resolveInh)
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
			           "WHERE id = " + pg.getId();
		try 
		{
			Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs == null) throw new DatabaseException(
				"No PresentationGroup found with id " + pg.getId());

			// There should be only one row in the result set
			rs.next();
			pg.groupName = rs.getString(2);

			// Add to database list of all PGs:
			pg.getDatabase().presentationGroupList.add(pg);

			// Resolve inheritance if we're supposed to:
			if (resolveInh)
			{
				DbKey inheritsFromId = DbKey.createDbKey(rs, 3);
				if (!inheritsFromId.isNull())
				{
					PresentationGroup inheritsFrom = get(pg, inheritsFromId);
					pg.inheritsFrom = inheritsFrom.groupName;
				}
			}
		
			pg.lastModifyTime = getTimeStamp(rs, 4, pg.lastModifyTime);
			pg.isProduction = TextUtil.str2boolean(rs.getString(5));
			readDataPresentations(pg, pg.getId());

			stmt.close();
		}
		catch (SQLException sqle) 
		{
			throw new DatabaseException("Error on query '" + query
				+ "': " + sqle.toString());
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
	* @param pg the PresentationGroup
	* @param pgId the PresentationGroup database ID
	*/
	public void readDataPresentations(PresentationGroup pg, DbKey pgId)
		throws DatabaseException
	{
		try 
		{
			Statement stmt = createStatement();
			String q =
				"SELECT DataPresentation.id, DataTypeId, UnitAbbr, " +
				"EquipmentID, Standard, Code";
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
				q = q + ", maxDecimals";
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
				q = q + ", MAX_VALUE, MIN_VALUE";

			q = q + " FROM DataPresentation, DataType " +
				"WHERE GroupId = " + pgId +
				" AND DataTypeId = DataType.id";

			ResultSet rs = stmt.executeQuery(q);

			if (rs != null) 
			{
				while (rs.next())
					makeDataPresentation(rs, pg);
			}

			stmt.close();
		}
		catch (SQLException sqle) {
			throw new DatabaseException(sqle.toString());
		}
	}

	/**
	* This method makes a new DataPresentation from a single row of a
	* ResultSet.
	* @param rs  the JDBC result set
	* @param pg the PresentationGroup
	*/
	private void makeDataPresentation(ResultSet rs, PresentationGroup pg)
		throws SQLException, DatabaseException
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

//		// Now read this DataPresentation's RoundingRules
//
//		readRoundingRules(dpId, dp);
	}

//	/**
//	* Reads the RoundingRules associated with a DataPresentation
//	* @param dpId the DataPresentation database ID
//	* @param dp the DataPresentation
//	*/
//	public void readRoundingRules(DbKey dpId, DataPresentation dp)
//		throws SQLException, DatabaseException
//	{
//		Statement stmt = createStatement();
//		ResultSet rs = stmt.executeQuery(
//			"SELECT dataPresentationId, upperLimit, sigDigits " +
//			"FROM RoundingRule " +
//			"WHERE DataPresentationID = " + dpId
//		);
//
//		if (rs != null) {
//			while (rs.next()) {
//				makeRoundingRule(rs, dp);
//			}
//		}
//		stmt.close();
//	}

//	/**
//	* Make a new RoundingRule associated with a particular DataPresentation
//	* from a single row of a ResultSet.
//	* @param rs  the JDBC result set
//	* @param dp the DataPresentation
//	*/
//	private void makeRoundingRule(ResultSet rs, DataPresentation dp)
//		throws SQLException, DatabaseException
//	{
//		RoundingRule rr = new RoundingRule(dp);
//		
//		double upperLimit = rs.getDouble(2);
//		if (!rs.wasNull() && upperLimit != SQL_MAX_DOUBLE)
//			rr.setUpperLimit(upperLimit);
//		rr.sigDigits = rs.getInt(3);
//		dp.addRoundingRule(rr);
//	}

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
	public void update(PresentationGroup pg)
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
	public void deleteAllDataPresentations(PresentationGroup pg)
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
	* This deletes a single DataPresentation.  All the RoundingRules
	* associated with this DataPresentation are also deleted.
	* The SQL database ID value of the DataPresentation must be set.
	* @param pg the PresentationGroup
	*/
	public void delete(DataPresentation dp)
		throws DatabaseException, SQLException
	{
		DbKey id = dp.getId();

		String q = "DELETE FROM DataPresentation WHERE ID = " + id;
		executeUpdate(q);

		dp.setId(Constants.undefinedId);

		q = "DELETE FROM RoundingRule " +
			"WHERE DataPresentationID = " + id;
		tryUpdate(q);
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
	public void insert(PresentationGroup pg)
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
	public void insertAllDataPresentations(PresentationGroup pg)
		throws DatabaseException, SQLException
	{
		Vector<DataPresentation> v = pg.dataPresentations;
		for (int i = 0; i < v.size(); ++i)
			insert(v.get(i));
	}

	/**
	* This writes a DataPresentation into the database, when we're not
	* sure whether it's a new object or not.  Its SQL DB ID is examined,
	* and if not set, this is considered to be a new object.
	* Regardless, db must have its myGroup member set to point to its
	* PresentationGroup parent, and that must not be new.
	* @param dp the DataPresentation
	*/
	public void write(DataPresentation dp)
		throws DatabaseException, SQLException
	{
		if (dp.idIsSet()) update(dp);
		else insert(dp);
	}

	/**
	* This updates an existing DataPresentation in the database.
	* This DataPresentation must also have its myGroup member be a valid
	* reference to a PresentationGroup, which is not new.
	* Note that there is no update method for RoundingRules.  When a set
	* of RoundingRules belonging to a DataPresentation has possibly changed,
	* this I/O class deletes all of the existing RoundingRules in the
	* database, and then adds them all back fresh.
	* @param dp the DataPresentation
	*/
	public void update(DataPresentation dp)
		throws DatabaseException, SQLException
	{
		DbKey id = dp.getId();
		DbKey pgId = dp.getGroup().getId();

		String q =
			"UPDATE DataPresentation SET " +
			  "GroupId = " + pgId + ", " +
			  "DataTypeId = " + dp.getDataType().getId() + ", " +
			  "UnitAbbr = " + sqlString(dp.getUnitsAbbr());
//			  "EquipmentID = " + getEqIdStr(dp);
		int md = dp.getMaxDecimals();
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			q = q + ", maxDecimals = " + 
				(md == Integer.MAX_VALUE ? "NULL" : ("" + md));
			if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			{
				q = q + ", MAX_VALUE = " + sqlOptDouble(dp.getMaxValue());
				q = q + ", MIN_VALUE = " + sqlOptDouble(dp.getMinValue());
			}
		}

		q = q + " WHERE ID = " + id;
		executeUpdate(q);

		// Delete all the existing RoundingRules in the database.

		q = "DELETE FROM RoundingRule WHERE DataPresentationID = " + id;
		tryUpdate(q);

		// Now insert them all back in

//		insertAllRoundingRules(dp);
	}

//	/**
//	* This method inserts all of the RoundingRules belonging to a
//	* particular DataPresentation into the database.
//	* @param dp the DataPresentation
//	*/
//	public void insertAllRoundingRules(DataPresentation dp)
//		throws DatabaseException, SQLException
//	{
//		Vector<RoundingRule> rrv = dp.roundingRules;
//		for (int i = 0; i < rrv.size(); ++i)
//			insert(rrv.get(i) );
//	}

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
	public void insert(DataPresentation dp)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("DataPresentation");
		dp.setId(id);
		PresentationGroup pg = dp.getGroup();

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

		executeUpdate(q);
	}


	/**
	* Insert a new RoundingRule into the database.
	* rr must have a valid DataPresentation as its parent member, and
	* that parent must not be new (i.e. that DataPresentation must have
	* its ID set.)
	* Note that there is no update method for RoundingRules.  When a set
	* of RoundingRules belonging to a DataPresentation has possibly changed,
	* this I/O class deletes all of the existing RoundingRules in the
	* database, and then adds them all back fresh.
	* @param rr the RoundingRule
	*/
	public void insert(RoundingRule rr)
		throws DatabaseException, SQLException
	{
		DbKey dpId = rr.getParent().getId();

		String upperLimit = "" +
			(rr.getUpperLimit() == Constants.undefinedDouble ?
				SQL_MAX_DOUBLE : rr.getUpperLimit());
		
		String q;
		if (getDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
			q = "INSERT INTO RoundingRule VALUES (" +
			  	dpId + ", " + upperLimit
			  	+ rr.sigDigits + ", " +
			  	"100" +
				")";
		else // No max decimals in new table def.
			q = "INSERT INTO RoundingRule VALUES (" +
			  	dpId + ", " + upperLimit + ", " + rr.sigDigits + ")";
		executeUpdate(q);
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
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM PresentationGroup where name = "
			+ sqlReqString(name));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}

	/**
	* Returns the last-modify-time for this presentation group in the database.
	* @param pg the PresentationGroup
	*/
	public Date getLMT(PresentationGroup pg)
	{
		try
		{
			DbKey id = pg.getId();
			if (id != null && !id.isNull())
			{
				id = name2id(pg.groupName);    // will throw if unsuccessfull
				try { pg.setId(id); }
				catch(DatabaseException ex) {} // guaranteed not to happen.
			}

			Statement stmt = createStatement();
			String q = 
				"SELECT lastModifyTime FROM PresentationGroup WHERE id = " + id;
			ResultSet rs = stmt.executeQuery(q);

			// Should be only 1 record returned.
			if (rs == null || !rs.next())
			{
				Logger.instance().log(Logger.E_WARNING,
					"Cannot get SQL LMT for Presentation Group '"
					+ pg.groupName + "' id=" + pg.getId());
				return null;
			}

			Date ret = getTimeStamp(rs, 1, (Date)null);
			stmt.close();
			return ret;
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

