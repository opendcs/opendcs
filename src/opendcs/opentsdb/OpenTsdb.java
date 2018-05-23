package opendcs.opentsdb;

import ilex.util.Logger;

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
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.ScheduleEntryDAO;
import opendcs.dao.XmitRecordDAO;
import decodes.cwms.CwmsFlags;
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
	private String jdbcOracleDriver = null;
	private String databaseLocation = null;
	
	public static final char TABLE_TYPE_NUMERIC = 'N';
	public static final char TABLE_TYPE_STRING = 'S';

	public OpenTsdb()
	{
		super();
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
		DecodesSettings settings = DecodesSettings.instance();
		
		String driverClass = this.jdbcOracleDriver != null ? this.jdbcOracleDriver :
			DecodesSettings.instance().jdbcDriverClass;
		String dbUri = this.databaseLocation != null ? this.databaseLocation :
			DecodesSettings.instance().editDatabaseLocation;

		
		String username = credentials.getProperty("username");
		String password = credentials.getProperty("password");

		try
		{
			Class.forName(driverClass);
		
			// setConnection will also get the TSDB Version Info and read tsdb_properties
			setConnection(
				DriverManager.getConnection(
					settings.editDatabaseLocation, username, password));
		
			setupKeyGenerator();

			// MJM 2018-2/21 Force autoCommit on.
			try { getConnection().setAutoCommit(true); }
			catch(SQLException ex)
			{
				Logger.instance().warning("Cannot set SQL AutoCommit to true: " + ex);
			}

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
		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
		try
		{
			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(
				parm.getSiteDataTypeId());
			parm.setSite(tsid.getSite());
			parm.setDataType(tsid.getDataType());
			return tsid;
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	@Override
	public DataCollection getNewData(DbKey applicationId)
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
	public String[] getTsIdParts()
	{
		// OpenTSDB uses CWMS TSID Structure
		return CwmsTsId.tsIdParts;
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
		return new OpenTimeSeriesDAO(this);
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
	
	@Override
	public String flags2LimitCodes(int flags)
	{
		StringBuilder sb = new StringBuilder();
		if ((flags & CwmsFlags.SCREENED) != 0)
		{
			sb.append('S');
			if ((flags & CwmsFlags.VALIDITY_MISSING) != 0)
				sb.append('M');
			if ((flags & CwmsFlags.VALIDITY_REJECTED) != 0)
				sb.append('R');
			if ((flags & CwmsFlags.VALIDITY_QUESTIONABLE) != 0)
				sb.append('Q');
		}
		return sb.toString();
	}

	public void setJdbcOracleDriver(String jdbcOracleDriver)
	{
		this.jdbcOracleDriver = jdbcOracleDriver;
	}

	public void setDatabaseLocation(String databaseLocation)
	{
		this.databaseLocation = databaseLocation;
	}

	@Override
	public boolean isOpenTSDB() { return true; }
	
	@Override
	public ArrayList<String> listParamTypes()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		String q = "select distinct statistics_code FROM TS_SPEC";

		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("OpenTsdb.listParamTypes: " + ex);
		}

		// MJM - these are the ones we know about for sure:
		if (!ret.contains("Inst"))
			ret.add("Inst");
		if (!ret.contains("Ave"))
			ret.add("Ave");
		if (!ret.contains("Max"))
			ret.add("Max");
		if (!ret.contains("Min"))
			ret.add("Min");
		if (!ret.contains("Total"))
			ret.add("Total");
		return ret;
	}

	@Override
	public ArrayList<String> listVersions()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		String q = "select distinct ts_version FROM TS_SPEC order by ts_version";

		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("OpenTsdb.listVersions: " + ex);
		}

		return ret;
	}
	
}
