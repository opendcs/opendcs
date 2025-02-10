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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class EnumSqlDao extends DaoBase implements EnumDAI
{
	private static final Logger log = LoggerFactory.getLogger(EnumSqlDao.class);
	private static DbObjectCache<DbEnum> cache = new DbObjectCache<DbEnum>(3600000, false);
	
	public EnumSqlDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "EnumSqlDao");
	}

	@Override
	public DataTransaction getTransaction() throws OpenDcsDataException
	{
		try
		{
			return new SimpleTransaction(db.getConnection());
		}
		catch (SQLException ex)
		{
			throw new OpenDcsDataException("Unable to get connection.", ex);
		}
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
	public void writeEnum(DbEnum dbenum) throws DbIoException
	{
		try (DataTransaction tx = this.getTransaction())
		{
			this.writeEnum(tx, dbenum);
		}
		catch (OpenDcsDataException ex)
		{
			throw new DbIoException("Unable to save DbEnum", ex);
		}	
	}
	
	private void readValues(DbEnum dbenum)throws SQLException, DbIoException
	{
		readValues(this, dbenum);
	}
	private void readValues(DaoBase dao, DbEnum dbenum) throws SQLException, DbIoException
	{
		int dbVer = db.getDecodesDatabaseVersion();

		String q = 
			"SELECT enumId, enumValue, description, " +
			"execClass, editClass";
		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", sortNumber";
		q = q + " FROM EnumValue WHERE EnumID = ?";// + dbenum.getId();
		//ResultSet rs = doQuery2(q);
		dao.doQuery(q,(rs)-> {
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
	public void writeEnumValue(EnumValue ev) throws DbIoException
	{
		writeEnumValue(this, ev);
	}

	/**
	* Write a single EnumValue to the database.
	* Assume no conflict with EnumValues already in the database.
	* @param ev the EnumValue
	*/
	private void writeEnumValue(DaoBase dao, EnumValue ev) throws DbIoException
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
			dao.doModify(q,args.toArray());
		} 
		catch(SQLException er)
		{
			debug3(er.getLocalizedMessage());
			throw new DbIoException("Failed to add enum to database", er);
		}
		
	}

	@Override
	public Collection<DbEnum> getEnums(DataTransaction tx) throws OpenDcsDataException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getEnums'");
	}

	@Override
	public Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException
	{
		synchronized(cache)
		{
			DbEnum ret = cache.getByUniqueName(enumName);
			if (ret != null)
			{
				return Optional.of(ret);
			}
			
			int dbVer = db.getDecodesDatabaseVersion();
			String q = "SELECT " + getEnumColumns(dbVer) + " FROM Enum";
			q = q + " where lower(name) = lower(?)";// + sqlString(enumName.toLowerCase());
			Connection conn = tx.connection(Connection.class)
						        .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
			try (DaoHelper helper = new DaoHelper(this.db, "helper-enum", conn))
			{
				ret = helper.getSingleResult(q, rs -> rs2Enum(rs, dbVer), enumName);
				if (ret == null)
				{
					warning("No such enum '" + enumName + "'");
					return Optional.empty();
				}
				else
				{
					readValues(helper, ret);
					cache.put(ret);
					return Optional.of(ret);
				}		
			}
			catch (DbIoException | SQLException ex)
			{
				throw new OpenDcsDataException("Error retrieving Enum values",ex);
			}
		}
	}

	@Override
	public Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getEnum'");
	}

	@Override
	public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException
	{
		// should this be part of DataTransaction?
		int dbVer = db.getDecodesDatabaseVersion();
		String q = "";
		ArrayList<Object> args = new ArrayList<>();
		if (dbEnum.idIsSet())
		{			
			args.add(dbEnum.getUniqueName());
			q = "update enum set name = ?";// + sqlString(dbenum.getUniqueName());
			if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + ", defaultvalue = ?";// + sqlString(dbenum.getDefault());
				args.add(dbEnum.getDefault());
				if (dbVer >= DecodesDatabaseVersion.DECODES_DB_10)
					q = q + ", description = ?";// + sqlString(dbenum.getDescription());
					args.add(dbEnum.getDescription());
			}
			q = q + " where id = ?" /*+ dbenum.getId()*/;
			args.add(dbEnum.getId().getValue());
		}
		else // New enum, allocate a key and insert
		{
			DbKey id;
			try
			{
				id = getKey("Enum");
			}
			catch (DbIoException ex)
			{
				throw new OpenDcsDataException("Unable to generate new key for dbEnum", ex);
			}
			dbEnum.forceSetId(id);
			q = "insert into enum";
			if (dbVer < DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + "(id, name) values (?,?)"; 
					//+ id + ", " + sqlString(dbenum.getUniqueName()) + ")";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
			}
			else if (dbVer < DecodesDatabaseVersion.DECODES_DB_10)
			{
				q = q + "(id, name, defaultValue) values (?,?,?)";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
				args.add(dbEnum.getDefault());
			}
			else
			{
				q = q + "(id, name, defaultValue, description) values (?,?,?,?)";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
				args.add(dbEnum.getDefault());
				args.add(dbEnum.getDescription());
			}
			cache.put(dbEnum);
		}
		
		Connection conn = tx.connection(Connection.class)
							.orElseThrow(() -> new OpenDcsDataException("Unable to get JDBC connection to perform DbEnum Save."));		
		try (DaoHelper helper = new DaoHelper(this.db, q, conn))
		{
			helper.doModify(q,args.toArray());

			// Delete all enum values. They'll be re-added below.
			//info("writeEnum deleting values from enum '" + dbenum.enumName + "'");
			q = "DELETE FROM EnumValue WHERE enumId = ?";
			helper.doModify(q, dbEnum.getId().getValue());
			
			for (Iterator<EnumValue> it = dbEnum.iterator(); it.hasNext(); )
			{
				writeEnumValue(helper, it.next());
			}
			return dbEnum;
		}
		catch(DbIoException | SQLException ex)
		{
			throw new OpenDcsDataException("enum modify/delete failed for " + dbEnum.toString(), ex);
		}
	}

}
