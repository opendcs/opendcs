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
	 * Lookup a data type record by std & code and return the surrogate key ID.
	 * @param std
	 * @param code
	 * @return
	 * @throws SQLException 
	 * @throws DbIoException
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
		Long id = getKey("DATATYPE");
		String q = "insert into DATATYPE(ID, STANDARD, CODE) values (?, ?, ?)";
		doModifyV(q, id, std, code);
		return id;
	}
}
