package opendcs.opentsdb;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.ScheduleEntryDAO;
import opendcs.dao.XmitRecordDAO;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.util.DecodesSettings;

public class OpenTsdb extends TimeSeriesDb
{
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
			warning("Cannot convert date!");
			return null;
		}
	}

	/**
	* In open TSDB, date/times are represented as long integer.
	* @return the numeric date falue or NULL if passed a null date.
	*/
	public String sqlDate(Date d)
	{
		if (d == null)
			return "NULL";
		return "" + d.getTime();
	}

	
	@Override
	public DbKey connect(String appName, Properties credentials)
		throws BadConnectException
	{
		String username = credentials.getProperty("username");
		String password = credentials.getProperty("password");
		DecodesSettings settings = DecodesSettings.instance();

		LoadingAppDAI loadingAppDAO = null;
		try
		{
			Class.forName(settings.jdbcDriverClass);
		
			// setConnection will also get the TSDB Version Info and read tsdb_properties
			setConnection(
				DriverManager.getConnection(
					settings.editDatabaseLocation, username, password));
		
			setupKeyGenerator();
			
			postConnectInit(appName);
			OpenTsdbSettings.instance().setFromProperties(props);
			
			return appId;
		}
		catch (Exception ex)
		{
			String msg = "Error getting JDBC connection using driver '"
				+ settings.jdbcDriverClass + "' to database at '" 
				+ settings.editDatabaseLocation
				+ "' for user '" + username + "': " + ex.toString();
			System.err.println(msg);
			conn = null;
			throw new BadConnectException(msg);
		}
		finally
		{
			if (loadingAppDAO != null)
				loadingAppDAO.close();
		}
	}

	@Override
	public void setParmSDI(DbCompParm parm, DbKey siteId, String dtcode)
		throws DbIoException, NoSuchObjectException
	{
		// Stub - Not implemented.
	}

	@Override
	public TimeSeriesIdentifier expandSDI(DbCompParm parm) throws DbIoException,
		NoSuchObjectException
	{
		throw new NoSuchObjectException("OpenTsdb.expandSDI not implemented.");
	}

	@Override
	public DataCollection getNewDataSince(DbKey applicationId, Date sinceTime)
		throws DbIoException
	{
		// Stub - Not implemented.
		return null;
	}

	@Override
	public void validateParm(DbKey siteId, String dtcode, String interval,
		String tabSel, int modelId) throws ConstraintException, DbIoException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int findModelId(int modelRunId) throws DbIoException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int findMaxModelRunId(int modelId) throws DbIoException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String flags2LimitCodes(int flags)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String flags2RevisionCodes(int flags)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getTsIdParts()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeSeriesIdentifier makeEmptyTsId()
	{
		return new CwmsTsId();
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> expandTsGroup(TsGroup tsGroup)
		throws DbIoException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeSeriesIdentifier transformTsidByCompParm(
		TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
		boolean fillInParm, String timeSeriesDisplayName) throws DbIoException,
		NoSuchObjectException, BadTimeSeriesException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
		DbCompParm parm)
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	public XmitRecordDAO makeXmitRecordDao(int maxDays)
	{
		return new OpenTsdbXmitRecordDao(this, maxDays);
	}

	@Override
	public TimeSeriesDAI makeTimeSeriesDAO()
	{
		return null;
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new OpenTsdbIntervalDAO(this);
	}

	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		return new ScheduleEntryDAO(this);
	}
}
