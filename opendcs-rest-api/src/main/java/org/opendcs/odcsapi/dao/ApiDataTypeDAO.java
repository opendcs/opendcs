/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiDataTypeDAO
	extends ApiDaoBase
{
	public static String module = "apiDataTypeDAO";

	public ApiDataTypeDAO(DbInterface dbi)
	{
		super(dbi, module);
	}
	
	/**
	 * Lookup a data type record by std and code and return the surrogate key ID.
	 * @param std
	 * @param code
	 * @return
	 * @throws SQLException 
	 * @throws DbException
	 */
	public Long lookup(String std, String code)
		throws DbException, SQLException
	{
		Connection conn = null;
		String q = "select ID from DATATYPE where lower(STANDARD) = ? and lower(CODE) = ?";;
		ResultSet rs = doQueryPs(conn, q, std.toLowerCase(), code.toLowerCase());
		try
		{
			if (!rs.next())
				return null;
			else
				return rs.getLong(1);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	/**
	 * Create a data type record and return its surrogate key ID.
	 * @param std
	 * @param code
	 * @return
	 * @throws DbException
	 */
	public long create(String std, String code)
		throws DbException
	{
		Long id = getKey(DbInterface.Sequences.DATATYPE);
		String q = "insert into DATATYPE(ID, STANDARD, CODE) values (?, ?, ?)";
		doModifyV(q, id, std, code);
		return id;
	}
}
