package opendcs.opentsdb;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import opendcs.dai.IntervalDAI;

import decodes.db.DatabaseException;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;

public class OpenTsdbSqlDbIO extends SqlDatabaseIO
{

	public OpenTsdbSqlDbIO(String location)
		throws DatabaseException
	{
		super(location);
		Logger.instance().info("Constructing OpenTsdbSqlDbIO");
	}

	@Override
	public void connectToDatabase(String sqlDbLocation, String user,
		String pw, boolean threadCon)
		throws DatabaseException
	{
		super.connectToDatabase(sqlDbLocation, user, pw, threadCon);
		IntervalDAI intervalDAO = makeIntervalDAO();
		try { intervalDAO.loadAllIntervals(); }
		catch (DbIoException ex)
		{
			Logger.instance().warning("Cannot read intervals: " + ex);
		}
		finally
		{
			intervalDAO.close();
		}
	}

	@Override
	public Date getFullDate(ResultSet rs, int column)
	{
		// In OpenTSDB, date/times are stored as long integer
		try
		{
			long t = rs.getLong(column);
			if (rs.wasNull())
				return null;
			return new Date(t);
		}
		catch (SQLException ex)
		{
			Logger.instance().warning("Cannot convert date!");
			return null;
		}
	}

	@Override
	public String sqlDate(Date d)
	{
		if (d == null)
			return "NULL";
		return "" + d.getTime();
	}
	
	@Override
	public String sqlBoolean(boolean b)
	{
		return b ? "'TRUE'" : "'FALSE'";
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new OpenTsdbIntervalDAO(this);
	}

}
