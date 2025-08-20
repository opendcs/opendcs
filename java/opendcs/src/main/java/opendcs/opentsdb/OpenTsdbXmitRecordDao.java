package opendcs.opentsdb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.XmitRecordDAO;

public class OpenTsdbXmitRecordDao extends XmitRecordDAO
{

	public OpenTsdbXmitRecordDao(DatabaseConnectionOwner tsdb,
		int maxXmitDays)
	{
		super(tsdb, maxXmitDays);
	}

	/**
	 * In OpenTSDB timestamps are stored as long integer
	 * @param ps the prepared statement
	 * @param column the column number
	 * @param msecTime long value representing msec since the epoch
	 * @throws SQLException 
	 */
	protected void setPrepStatementTimestamp(PreparedStatement ps, int column, long msecTime) 
		throws SQLException
	{
		ps.setLong(column, msecTime);
	}


	protected long getResultSetTimestamp(ResultSet rs, int column)
		throws SQLException
	{
		return rs.getLong(column);
	}

}
