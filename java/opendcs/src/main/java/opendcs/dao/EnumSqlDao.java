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

import org.jdbi.v3.core.Handle;
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
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.KeyGenerator;
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
		String q = "";
		ArrayList<Object> args = new ArrayList<>();
		KeyGenerator keyGenerator = this.db.getSettings(KeyGenerator.class).get();
		DbKey id;
		
		try (var handle = tx.connection(Handle.class)
							.orElseThrow(() -> new OpenDcsDataException("Unable to get JDbi Handle istance to perform DbEnum Save.")))
		{
			if (dbEnum.idIsSet())
			{	
				q = """
	update enum set name = :name, defaultvalue = :default, description = :description where id = :id returning id, name, defaultvalue, description"
	""";
				id = dbEnum.getId();
			}
			else // New enum, allocate a key and insert
			{
				q = "insert into enum(id, name, defaultValue, description) values (:name,:default,:description) returning id, name, defaultValue, description";
				id = keyGenerator.getKey("enum", handle.getConnection());
			}
			var savedEnum = handle.createQuery(q)
							   .bind("default", dbEnum.getDefault())
							   .bind("description", dbEnum.getDescription())
							   .bind("name", dbEnum.getUniqueName())
							   .bind("id", id)
							   .map(r -> r.getColumn("id", Long.class))
							   .findOne()
							   .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve the enum we just saved."))
							   ;
			

			cache.put(dbEnum);
		}		
		
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
