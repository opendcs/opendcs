package opendcs.opentsdb;

import ilex.util.Logger;
import ilex.var.TimedVariable;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import lrgs.gui.DecodesInterface;

import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.ScheduleEntryDAO;
import opendcs.dao.XmitRecordDAO;

import decodes.cwms.CwmsGuiLogin;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
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

		// If this is a GUI, ask user for username & password.
		CwmsGuiLogin cgl = CwmsGuiLogin.instance();
//		if (DecodesInterface.isGUI())
//		{
//			try 
//			{
//				if (!cgl.isLoginSuccess())
//				{
//					cgl.doLogin(null);
//					if (!cgl.isLoginSuccess()) // user hit cancel
//						throw new BadConnectException("Login aborted by user.");
//
//				}
//				
//				username = cgl.getUserName();
//				password = new String(cgl.getPassword());
//			}
//			catch(Exception ex)
//			{
//				throw new BadConnectException(
//					"Cannot display login dialog: " + ex);
//			}
//		}

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
		// TODO Auto-generated method stub

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
	public int fillTimeSeries(CTimeSeries ts, Date from, Date until,
		boolean include_lower, boolean include_upper, boolean overwriteExisting)
		throws DbIoException, BadTimeSeriesException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fillTimeSeries(CTimeSeries ts, Collection<Date> queryTimes)
		throws DbIoException, BadTimeSeriesException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TimedVariable getPreviousValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimedVariable getNextValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public DataCollection getNewDataSince(DbKey applicationId, Date sinceTime)
		throws DbIoException
	{
		// TODO Auto-generated method stub
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
