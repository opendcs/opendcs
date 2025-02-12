/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other 
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import decodes.db.EnumValue;
import decodes.db.ValueNotFoundException;
import opendcs.dai.EnumDAI;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;

/**
 * Data Access Object for writing/reading DbEnum objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class EnumSqlDao 
	extends DaoBase 
	implements EnumDAI
{
	private static DbObjectCache<DbEnum> cache = new DbObjectCache<DbEnum>(3600000, false);
	
	public EnumSqlDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "EnumSqlDao");
	}
	
	private String getEnumColumns(int dbVer)
	{
		return "id, name"
			+ (dbVer >= DecodesDatabaseVersion.DECODES_DB_10 ? ", defaultValue, description "
			: dbVer >= DecodesDatabaseVersion.DECODES_DB_6 ? ", defaultvalue " 
			: " ");
	}
	
	private DbEnum rs2Enum(ResultSet rs, int dbVer)
		throws SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		DbEnum en = new DbEnum(id, rs.getString(2));

		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			String def = rs.getString(3);
			if (!rs.wasNull())
				en.setDefault(def.trim());
		}
		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_10)
		{
			en.setDescription(rs.getString(4));
		}
		return en;
	}
	
	@Override
	public DbEnum getEnum(String enumName) 
		throws DbIoException
	{
		synchronized(cache)
		{
			DbEnum ret = cache.getByUniqueName(enumName);
			if (ret != null)
				return ret;
			
			int dbVer = db.getDecodesDatabaseVersion();
			String q = "SELECT " + getEnumColumns(dbVer) + " FROM Enum";
			q = q + " where lower(name) = lower(?)";// + sqlString(enumName.toLowerCase());
			
			try
			{
				ret = getSingleResult(q,(rs) -> {
					DbEnum en = rs2Enum(rs, dbVer);
					return en;
				},enumName);
				if (ret == null)
				{
					warning("No such enum '" + enumName + "'");
					return null;
				}
				else
				{
					readValues(ret);
					cache.put(ret);
					return ret;
				}		
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "': " + ex;
				warning(msg);
				throw new DbIoException(msg,ex);
			}
		}
	}

	@Override
	public DbEnum getEnumById(DbKey enumId)
			throws DbIoException
	{
		return getEnumById(enumId, false);
	}

	private DbEnum getEnumById(DbKey enumId, boolean skipCache)
			throws DbIoException
	{
		synchronized(cache)
		{
			DbEnum ret;
			if (!skipCache)
			{
				ret = cache.getByKey(enumId);
				if (ret != null)
					return ret;
			}

			int dbVer = db.getDecodesDatabaseVersion();
			String q = "SELECT " + getEnumColumns(dbVer) + " FROM Enum";
			q = q + " where id = ?";

			try
			{
				ret = getSingleResult(q, rs -> rs2Enum(rs, dbVer), enumId.getValue());
				if (ret == null)
				{
					warning("No such enum with id '" + enumId.getValue() + "'");
					return null;
				}
				else
				{
					readValues(ret);
					cache.put(ret);
					return ret;
				}
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "': " + ex;
				warning(msg);
				throw new DbIoException(msg,ex);
			}
		}
	}

	@Override
	public DbKey getEnumId(String enumName)
			throws DbIoException
	{
		synchronized(cache)
		{
			DbKey ret = cache.getByUniqueName(enumName).getKey();
			if (ret != null)
			{
				return ret;
			}

			int dbVer = db.getDecodesDatabaseVersion();
			String q = "SELECT " + getEnumColumns(dbVer) + " FROM Enum";
			q = q + " where lower(name) = lower(?)";

			try
			{
				ret = getSingleResult(q, rs -> {
					DbEnum en = rs2Enum(rs, dbVer);
					return en.getKey();
				},enumName);
				if (ret == null)
				{
					warning("No such enum '" + enumName + "'");
					return null;
				}
				else
				{
					return ret;
				}
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "': " + ex;
				warning(msg);
				throw new DbIoException(msg,ex);
			}
		}
	}

	@Override
	public void readEnumList(EnumList top) 
		throws DbIoException
	{
		int dbVer = db.getDecodesDatabaseVersion();

		try
		{
			synchronized(cache)
			{
				/**				 
				 * This could also be a single query with a join.
				 * though a little trickier with the different versions columns
				 */
				doQuery("SELECT " + getEnumColumns(dbVer) + " FROM Enum", 
							  (rs) -> {
								DbEnum en = rs2Enum(rs, dbVer);
								cache.put(en);
							});
				
				String q = "SELECT enumId, enumValue, description, execClass, editClass";
				if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
					q = q + ", sortNumber";
				q = q + " FROM EnumValue";
				doQuery(q, (rs) -> {
					DbKey key = DbKey.createDbKey(rs, 1);
					DbEnum dbEnum = cache.getByKey(key);
					if (dbEnum != null)
						rs2EnumValue(rs, dbEnum);
					top.addEnum(dbEnum);
				});				
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void writeEnumList(EnumList enumList) throws DbIoException
	{
		// Save off the values I want to be in the database
		ArrayList<DbEnum> newenums = new ArrayList<DbEnum>();
		for(Iterator<DbEnum> evit = enumList.iterator(); evit.hasNext(); )
			newenums.add(evit.next());

		// CLear the list and read whats currently in the database
		enumList.clear();
		readEnumList(enumList);
		
		// Write the new stuff & check it off from the old.
		for (DbEnum newenum : newenums)
		{
			DbEnum oldenum = enumList.getEnum(newenum.enumName);
			if (oldenum != null)
			{
				enumList.remove(oldenum);
				newenum.forceSetId(oldenum.getId());
			}
			writeEnum(newenum);
		}
		// Anything left in the list is an enumeration that needs to be completely removed.
		for(DbEnum oldenum : enumList.getEnumList())
		{
			try
			{
				info("writeEnumList Deleting enum '" + oldenum.enumName + "'");
				String q = "DELETE FROM EnumValue WHERE enumId = ?";// + oldenum.getId();
				long id = oldenum.getId().getValue();
				doModify(q,id);
				q = "delete from enum where id = ?";// + oldenum.getId();
				doModify(q,id);
			}
			catch(SQLException ex)
			{
				throw new DbIoException("Failed to clean up " + oldenum.toString(),ex);
			}
			
		}
		for(DbEnum newenum : newenums)
			enumList.addEnum(newenum);
	}

	@Override
	public void deleteEnumList(DbKey enumId)
		throws DbIoException
	{
		try
		{
			info("deleteEnum Deleting enums with id '" + enumId.getValue() + "'");
			String q = "DELETE FROM EnumValue WHERE enumId = ?";
			doModify(q, enumId.getValue());
			q = "delete from enum where id = ?";
			doModify(q, enumId.getValue());
			cache.remove(enumId);
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Failed to delete enum list with id " + enumId.getValue(), ex);
		}
	}

	public EnumValue getEnumValue(DbKey id, String enumVal)
			throws DbIoException
	{
		if (enumVal == null || enumVal.isEmpty())
		{
			throw new DbIoException("Must provide an EnumValue abbreviation to retrieve an EnumValue");
		}
		try
		{
			DbEnum dbenum = getEnumById(id);
			 List<EnumValue> retList = new ArrayList<>();

			String enumValLower = enumVal.toLowerCase();
			String q = "select enumValue, description, editClass, sortNumber from EnumValue "
				+ "where enumId = ? and lower(enumValue) = ?";

			doQuery(q, rs ->
			{
				EnumValue ret = new EnumValue(dbenum, enumVal);
				ret.setValue(rs.getString(1));
				ret.setDescription(rs.getString(2));
				ret.setEditClassName(rs.getString(3));
				ret.setSortNumber(rs.getInt(4));
				retList.add(ret);
			}, id.getValue(), enumValLower);

			if (retList.isEmpty() || retList.get(0).getValue() == null || retList.get(0).getValue().isEmpty())
			{
				Throwable notFound = new ValueNotFoundException("No EnumValue with abbreviation '" + enumVal + "'");
				throw new DbIoException(String.format("No EnumValue with abbreviation '%s'", enumVal), notFound);
			}
			if (retList.size() > 1)
			{
				throw new DbIoException(String.format("Multiple values found for EnumValue with abbreviation '%s'", enumVal));
			}
			return retList.get(0);
		}
		catch(SQLException ex)
		{
			throw new DbIoException(String.format("Failed to get EnumValue with abbreviation %s", enumVal), ex);
		}
	}

	@Override
	public void deleteEnumValue(DbKey id, String enumVal)
		throws DbIoException
	{
		try
		{
			String q = "DELETE FROM EnumValue WHERE enumId = ? and lower(enumValue) = ?";
			doModify(q, id.getValue(), enumVal.toLowerCase());
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Failed to delete EnumValue with abbreviation " + enumVal, ex);
		}
	}

	@Override
	public void writeEnumValue(DbKey enumId, EnumValue enumVal, String fromEnumVal, int sortNum)
		throws DbIoException
	{
		try
		{
			EnumValue existing = this.checkExistingEnumValue(enumId, enumVal.getValue());

			// This checks whether the enum exists in the database.
			// If it is only in the cache, it will be written to the database.
			DbEnum en = this.getEnumById(enumId, true);
			if (en == null)
			{
				en = this.getEnumById(enumId);
				if (en == null)
				{
					throw new DbIoException(String.format("No such Enum with id '%s'.", enumId));
				}
				cache.remove(enumId);
				en.forceSetId(DbKey.NullKey);
				this.writeEnum(en);
				cache.put(en);
				enumId = en.getId();
				DbEnum internalEnum = enumVal.getDbenum();
				internalEnum.forceSetId(enumId);
				enumVal.setDbenum(internalEnum);
			}

			// fromEnumVal is the existing enumVal that this one is updating.
			EnumValue fromExisting = null;
			if (fromEnumVal != null && !fromEnumVal.isEmpty())
			{
				fromExisting = this.checkExistingEnumValue(enumId, fromEnumVal);
			}

			String startEndTz = enumVal.getEditClassName();

			String q;
			if (fromExisting != null)
			{
				if (existing != null)
				{
					throw new DbIoException(
							String.format("Cannot update EnumValue from %s to %s. The EnumValue '%s' already exists",
									fromEnumVal, enumVal.getValue(), enumVal.getValue()));
				}
				q = "update enumvalue set enumvalue = ?, description = ?, editclass = ?, sortnumber = ? "
						+ "where enumid = ? and lower(enumvalue) = ?";
				doModify(q, enumVal.getValue(), enumVal.getDescription(), startEndTz, fromEnumVal.toLowerCase(), sortNum);
			}
			else if ((fromEnumVal == null || fromEnumVal.isEmpty()) && existing == null)
			{
				q = "insert into enumvalue(enumid, enumvalue, description, editclass, sortnumber) values(?,?,?,?,?)";
				doModify(q, enumId, enumVal.getValue(), enumVal.getDescription(), startEndTz, sortNum);
			}
			else if (fromEnumVal == null || fromEnumVal.isEmpty())
			{
				q = "update enumvalue set enumvalue = ?, description = ?, editclass = ?, sortnumber = ? "
						+ "where enumid = ? and lower(enumvalue) = ?";
				doModify(q, enumVal.getValue(), enumVal.getDescription(), startEndTz,
						sortNum, enumId.getValue(), fromEnumVal);
			}
			else
			{
				Throwable cause = new ValueNotFoundException(String.format("No such EnumValue with abbr '%s'.", fromEnumVal));
				throw new DbIoException(String.format("No such EnumValue with abbr '%s'.", fromEnumVal), cause);
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Failed to write EnumValue with abbreviation " + enumVal.getValue(), ex);
		}
	}

	private EnumValue checkExistingEnumValue(DbKey id, String abbr)
	{
		try
		{
			return getEnumValue(id, abbr);
		}
		catch (DbIoException e)
		{
			return null;
		}
	}

	@Override
	public void writeEnum(DbEnum dbenum)
		throws DbIoException
	{
		int dbVer = db.getDecodesDatabaseVersion();
		String q = "";
		ArrayList<Object> args = new ArrayList<>();
		if (dbenum.idIsSet())
		{			
			args.add(dbenum.getUniqueName());
			q = "update enum set name = ?";// + sqlString(dbenum.getUniqueName());
			if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + ", defaultvalue = ?";// + sqlString(dbenum.getDefault());
				args.add(dbenum.getDefault());
				if (dbVer >= DecodesDatabaseVersion.DECODES_DB_10)
					q = q + ", description = ?";// + sqlString(dbenum.getDescription());
					args.add(dbenum.getDescription());
			}
			q = q + " where id = ?" /*+ dbenum.getId()*/;
			args.add(dbenum.getId().getValue());
		}
		else // New enum, allocate a key and insert
		{
			DbKey id = getKey("Enum");
			dbenum.forceSetId(id);
			q = "insert into enum";
			if (dbVer < DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + "(id, name) values (?,?)"; 
					//+ id + ", " + sqlString(dbenum.getUniqueName()) + ")";
				args.add(id.getValue());
				args.add(dbenum.getUniqueName());
			}
			else if (dbVer < DecodesDatabaseVersion.DECODES_DB_10)
			{
				q = q + "(id, name, defaultValue) values (?,?,?)";
				args.add(id.getValue());
				args.add(dbenum.getUniqueName());
				args.add(dbenum.getDefault());
					/*+ id + ", " + sqlString(dbenum.getUniqueName())
					+ ", " + sqlString(dbenum.getDefault()) + ")";*/
			}
			else
			{
				q = q + "(id, name, defaultValue, description) values (?,?,?,?)";
				args.add(id.getValue());
				args.add(dbenum.getUniqueName());
				args.add(dbenum.getDefault());
				args.add(dbenum.getDescription());
					/*+ id + ", " + sqlString(dbenum.getUniqueName())
					+ ", " + sqlString(dbenum.getDefault()) 
					+ ", " + sqlString(dbenum.getDescription()) + ")";*/
			}
			cache.put(dbenum);
		}
		try
		{
			doModify(q,args.toArray());

			// Delete all enum values. They'll be re-added below.
			info("writeEnum deleting values from enum '" + dbenum.enumName + "'");
			q = "DELETE FROM EnumValue WHERE enumId = ?";// + dbenum.getId();
			doModify(q,dbenum.getId().getValue());
			
			for (Iterator<decodes.db.EnumValue> it = dbenum.iterator(); it.hasNext(); )
			{
				writeEnumValue(it.next());
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException("enum modify/delete failed for " + dbenum.toString(), ex);
		}
	}
	
	private void readValues(DbEnum dbenum)
		throws SQLException, DbIoException
	{
		int dbVer = db.getDecodesDatabaseVersion();

		String q = 
			"SELECT enumId, enumValue, description, " +
			"execClass, editClass";
		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", sortNumber";
		q = q + " FROM EnumValue WHERE EnumID = ?";// + dbenum.getId();
		//ResultSet rs = doQuery2(q);
		doQuery(q,(rs)-> {
			rs2EnumValue(rs, dbenum);
		},dbenum.getId());
	}
	
	private void rs2EnumValue(ResultSet rs, DbEnum dbEnum)
		throws SQLException
	{
		String enumValue = rs.getString(2);
		String description = rs.getString(3);
		String execClass = rs.getString(4);
		String editClass = rs.getString(5);

		int sn = 0;
		boolean setSortNumber = false;
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			sn = rs.getInt(6);
			if (!rs.wasNull())
				setSortNumber = true;
		}
		EnumValue ev = dbEnum.replaceValue(enumValue, description, execClass, editClass);
		if (setSortNumber)
			ev.setSortNumber(sn);
	}

	/**
	* Write a single EnumValue to the database.
	* Assume no conflict with EnumValues already in the database.
	* @param ev the EnumValue
	*/
	public void writeEnumValue(EnumValue ev)
		throws DbIoException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(ev.getDbenum().getId().getValue());
		args.add(ev.getValue());
		args.add(ev.getDescription());
		args.add(ev.getExecClassName());
		args.add(ev.getEditClassName());
		String q =
			"INSERT INTO EnumValue VALUES(" +
				"?," + /*ev.getDbenum().getId() + ", " +*/
				"?," + /*sqlString(ev.getValue()) + ", " +*/
				"?," + /*sqlString(ev.getDescription()) + ", " +*/
				"?," + /*sqlString(ev.getExecClassName()) + ", " +*/
				"?"; /*sqlString(ev.getEditClassName());*/
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
		{
			q += ")";
		}			
		else if (ev.getSortNumber() == EnumValue.UNDEFINED_SORT_NUMBER)
		{
			q += ", NULL)";
		}
		else
		{
			q = q + ", ?)";
			args.add(ev.getSortNumber());
		}
		try
		{
			doModify(q,args.toArray());
		} 
		catch(SQLException er)
		{
			debug3(er.getLocalizedMessage());
			throw new DbIoException("Failed to add enum to database", er);
		}
		
	}

}
