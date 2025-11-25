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

import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
public class EnumSqlDao implements EnumDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static DbObjectCache<DbEnum> cache = new DbObjectCache<DbEnum>(3600000, false);
	private final OpenDcsDatabase db;

	public EnumSqlDao(OpenDcsDatabase db)
	{
		this.db = db;
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

	@Override
	public void close() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'close'");
	}

}
