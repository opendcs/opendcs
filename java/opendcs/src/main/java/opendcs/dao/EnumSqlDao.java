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
package opendcs.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.mappers.dbenum.DbEnumBuilderMapper;
import org.opendcs.database.model.mappers.dbenum.DbEnumBuilderReducer;
import org.opendcs.database.model.mappers.dbenum.EnumValueMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.slf4j.Logger;

import decodes.db.EnumValue;
import decodes.db.ValueNotFoundException;
import decodes.db.DbEnum.DbEnumBuilder;
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
public class EnumSqlDao extends DaoBase implements EnumDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
					log.warn("No such enum '{}'", enumName);
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
				String msg = "Error in query '" + q + "'";
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
					log.warn("No such enum with id '" + enumId.getValue() + "'");
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
				String msg = "Error in query '" + q + "'";
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
					log.warn("No such enum '" + enumName + "'");
					return null;
				}
				else
				{
					return ret;
				}
			}
			catch (SQLException ex)
			{
				String msg = "Error in query '" + q + "'";
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
				doQuery("SELECT " + getEnumColumns(dbVer) + " FROM Enum", 
							  (rs) -> {
								DbEnum en = rs2Enum(rs, dbVer);
								cache.put(en);
							    top.addEnum(en);
							});
				
				String q = "SELECT enumId, enumValue, ev.description, execClass, editClass";
				if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
					q = q + ", sortNumber";
				q = q + " FROM EnumValue ev,";
				q = q + " Enum  e";
				q = q + " WHERE ev.enumId = e.id";
				doQuery(q, (rs) -> {
					DbKey key = DbKey.createDbKey(rs, 1);
					DbEnum dbEnum = cache.getByKey(key);
					if (dbEnum != null)
					{
						rs2EnumValue(rs, dbEnum);
					}
					else
					{
						log.warn("Enum not in cache for key {}. Orphaned enum value {}", key, rs.getString("enumValue"));
					}
				});
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query";
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
				log.info("writeEnumList Deleting enum '{}'", oldenum.enumName);
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
			log.info("deleteEnum Deleting enums with id '{}'", enumId.getValue());
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
		q = q + " FROM EnumValue WHERE EnumID = ?";
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
	public Collection<DbEnum> getEnums(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
	{
		var handle = tx.connection(Handle.class)
					   .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
		final String queryText = """
				select e.id e_id, e.name e_name, e.defaultValue e_defaultValue, e.description e_description,
				       v.enumvalue v_enumvalue, v.description v_description, v.execclass v_execclass,
					   v.editclass v_editclass, v.sortnumber v_sortnumber
				  from enum e
				  join enumvalue v on e.id = v.enumid
				""";
		// todo limit/offset
		try (var query =handle.createQuery(queryText))
		{
			return query.registerRowMapper(DbEnumBuilder.class, DbEnumBuilderMapper.withPrefix("e"))
					 	.registerRowMapper(EnumValue.class, EnumValueMapper.withPrefix("v"))
					 	.reduceRows(DbEnumBuilderReducer.DBENUM_BUILDER_REDUCER)
					 	.map(DbEnumBuilder::build)
					 	.collect(Collectors.toList());
		}
	}

	@Override
	public Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException
	{
		var handle = tx.connection(Handle.class)
					   .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
		try (var query = handle.createQuery("select id from enum where name = :name"))
		{
			var id = query.bind(GenericColumns.NAME, enumName)
						  .mapTo(DbKey.class)
						  .findOne();
			return getEnum(tx, id.orElse(DbKey.NullKey));
		}
	}

	@Override
	public Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException
	{
		if (DbKey.isNull(id))
		{
			throw new OpenDcsDataException("Unable to search for enum with null ID");
		}
		final String queryText ="""
				select e.id e_id, e.name e_name, e.defaultValue e_defaultValue, e.description e_description,
				       v.enumvalue v_enumvalue, v.description v_description, v.execclass v_execclass,
					   v.editclass v_editclass, v.sortnumber v_sortnumber
				  from enum e
				  join enumvalue v on e.id = v.enumid
				 where e.id = :id
				""";
		var handle = tx.connection(Handle.class)
					   .orElseThrow(() -> new OpenDcsDataException("Unable to get Database connection object."));
		try (var query = handle.createQuery(queryText))
		{
		    return query.bind(GenericColumns.ID, id)
						.registerRowMapper(DbEnumBuilder.class, DbEnumBuilderMapper.withPrefix("e"))
						.registerRowMapper(EnumValue.class, EnumValueMapper.withPrefix("v"))
						.reduceRows(DbEnumBuilderReducer.DBENUM_BUILDER_REDUCER)
						.map(DbEnumBuilder::build)
						.findFirst();
		}
		     
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
