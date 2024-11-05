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

import opendcs.dai.EnumDAI;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
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
			
			for (Iterator<EnumValue> it = dbenum.iterator(); it.hasNext(); )
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
