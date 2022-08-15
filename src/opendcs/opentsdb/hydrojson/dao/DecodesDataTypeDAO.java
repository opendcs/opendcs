package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;

public class DecodesDataTypeDAO
	extends DaoBase
{
	public static String module = "decodesDataTypeDAO";

	public DecodesDataTypeDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}
	
	/**
	 * Lookup a data type record by std & code and return the surrogate key ID.
	 * @param std
	 * @param code
	 * @return
	 * @throws DbIoException
	 */
	public Long lookup(String std, String code)
		throws DbIoException
	{
		String q = "select ID from DATATYPE where lower(STANDARD) = " + sqlString(std.toLowerCase())
		+ " and lower(CODE) = " + sqlString(code.toLowerCase());
		ResultSet rs = doQuery(q);
		try
		{
			if (!rs.next())
				return null;
			else
				return rs.getLong(1);
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Error in query '" + q + "'" + ex);
		}
	}
	
	/**
	 * Create a data type record and return its surrogate key ID.
	 * @param std
	 * @param code
	 * @return
	 * @throws DbIoException
	 */
	public long create(String std, String code)
		throws DbIoException
	{
		long id = getKey("DATATYPE").getValue();
		String q = "insert into DATATYPE(ID, STANDARD, CODE) values ("
			+ id + ", " + sqlString(std) + ", " + sqlString(code) + ")";
		doModify(q);
		return id;
	}
}
