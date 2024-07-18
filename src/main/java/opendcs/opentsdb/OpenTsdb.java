package opendcs.opentsdb;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import opendcs.dai.DaiBase;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.CachableDbObject;
import opendcs.dao.DaoBase;
import opendcs.dao.ScheduledReloadDbObjectCache;
import opendcs.dao.ScheduleEntryDAO;
import opendcs.dao.SiteDAO;
import opendcs.dao.XmitRecordDAO;
import decodes.cwms.CwmsGroupHelper;
import decodes.cwms.CwmsTsId;
import decodes.cwms.TsidMorpher;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.PresentationGroup;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.RecordRangeHandle;
import decodes.tsdb.TasklistRec;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;

public class OpenTsdb extends TimeSeriesDb
{
	private String jdbcOracleDriver = null;
	private String databaseLocation = null;

	public static final char TABLE_TYPE_NUMERIC = 'N';
	public static final char TABLE_TYPE_STRING = 'S';

	String getMinStmtQuery = null, getTaskListStmtQuery = null;

	public OpenTsdb()
	{
		super();
		module = "OpenTsdb";
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
				DriverManager.getConnection(dbUri, username, password));

			setupKeyGenerator();

			// MJM 2018-2/21 Force autoCommit on.
			try (Connection c = getConnection();)
			{
				c.setAutoCommit(true);
			}
			catch(SQLException ex)
			{
				Logger.instance().warning("Cannot set SQL AutoCommit to true: " + ex);
			}

			postConnectInit(appName, conn);
			OpenTsdbSettings.instance().setFromProperties(props);

			return appId;
		}
		catch (Exception ex)
		{
			String msg = "Error getting JDBC connection using driver '"
				+ settings.jdbcDriverClass + "' to database at '"
				+ settings.editDatabaseLocation
				+ "' for user '" + username + "': " + ex.toString();
			throw new BadConnectException(msg, ex);
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
		// In OpenTSDB, the SDI is the surrogate key to a unique time series.
		DbKey sdi = parm.getSiteDataTypeId();
		DbKey siteId = parm.getSiteId();
		DbKey datatypeId = parm.getDataTypeId();

		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
		TimeSeriesIdentifier tsid = null;
		try
		{
			if (!DbKey.isNull(sdi))
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(sdi);
				parm.setSite(tsid.getSite());
				parm.setDataType(tsid.getDataType());
			}
			else
			{
				// Some comp-parms are only partially specified by site and/or datatype.
				if (!DbKey.isNull(siteId))
					parm.setSite(this.getSiteById(siteId));
				if (!DbKey.isNull(datatypeId))
					parm.setDataType(DataType.getDataType(datatypeId));
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}

		return tsid;
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
	public TimeSeriesIdentifier transformTsidByCompParm(
		TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
		boolean fillInParm, String timeSeriesDisplayName) throws DbIoException,
		NoSuchObjectException, BadTimeSeriesException
	{
		if (tsid == null)
			tsid = makeEmptyTsId();

		String origString = tsid.getUniqueString();
		TimeSeriesIdentifier tsidRet = tsid.copyNoKey();
		boolean transformed = transformUniqueString(tsidRet, parm);

		if (transformed)
		{
			String uniqueString = tsidRet.getUniqueString();
			debug3(module + " origString='" + origString + "', new string='"
				+ uniqueString + "', parm=" + parm);
			TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();

			try
			{
				tsidRet = timeSeriesDAO.getTimeSeriesIdentifier(uniqueString);
				debug3(module + " time series '" + uniqueString + "' exists OK.");
			}
			catch(NoSuchObjectException ex)
			{
				if (createTS)
				{
					if (timeSeriesDisplayName != null)
						tsidRet.setDisplayName(timeSeriesDisplayName);

					// If the datatype has changed, set units to null. This will force the DAO
					// to lookup the correct units for the param.
					if (!TextUtil.strEqualIgnoreCase(tsid.getPart("param"), tsidRet.getPart("param")))
						tsidRet.setStorageUnits(null);

					timeSeriesDAO.createTimeSeries(tsidRet);
					fillInParm = true;
				}
				else
				{
					debug3(module + " no such time series '" + uniqueString + "'");
					return null;
				}
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		else
			tsidRet = tsid;

		if (fillInParm)
		{
			parm.setSiteDataTypeId(tsidRet.getKey());
			parm.setInterval(tsidRet.getInterval());
			parm.setTableSelector(
				tsidRet.getPart("ParamType") + "."
				+ tsidRet.getPart("Duration") + "."
				+ tsidRet.getPart("Version"));
			parm.setDataType(tsidRet.getDataType());
			parm.setSite(tsidRet.getSite());
		}

		return tsidRet;
	}

	@Override
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
		DbCompParm parm)
	{
		boolean transformed = false;
		if (!(tsidRet instanceof CwmsTsId))
			return false;
		CwmsTsId ctsid = (CwmsTsId) tsidRet;

		SiteName parmSiteName = parm.getSiteName();
		if (parmSiteName != null)
		{
			tsidRet.setSiteName(parmSiteName.getNameValue());
			transformed = true;
			if (parmSiteName.site != null)
				tsidRet.setSite(parmSiteName.site);
			else
			{
				// Also lookup the site and set the ID and site object.
				SiteDAI siteDAO = makeSiteDAO();
				try
				{
					DbKey siteId = siteDAO.lookupSiteID(parmSiteName);
					tsidRet.setSite(siteDAO.getSiteById(siteId));
				}
				catch (Exception ex)
				{
					Logger.instance().warning("Cannot get site for sitename " + parmSiteName + ": " + ex);
				}
				finally
				{
					siteDAO.close();
				}
			}
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getLocSpec() != null && parm.getLocSpec().length() > 0)
		{
			String morphed = TsidMorpher.morph(ctsid.getSiteName(), parm.getLocSpec());
			debug2("TSID site name '" + ctsid.getSiteName() + "' with loc spec '"
				+ parm.getLocSpec() + "' morphed to '" + morphed + "'");
			if (morphed == null)
				morphed = parm.getLocSpec();
			tsidRet.setSite(null);
			tsidRet.setSiteName("");
			SiteDAI siteDAO = makeSiteDAO();
			try
			{
				DbKey siteId = siteDAO.lookupSiteID(morphed);
				if (!DbKey.isNull(siteId))
				{
					tsidRet.setSite(siteDAO.getSiteById(siteId));
					tsidRet.setSiteName(morphed);
				}
			}
			catch (Exception ex)
			{
				Logger.instance().warning("Cannot get site for morphed sitename "
					+ morphed + ": " + ex);
			}
			finally
			{
				siteDAO.close();
			}
			transformed = true;
		}
		DataType dt = parm.getDataType();
		if (dt != null)
		{
			tsidRet.setDataType(dt);
			transformed = true;
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getParamSpec() != null && parm.getParamSpec().length() > 0)
		{
			String morphed = TsidMorpher.morph(ctsid.getPart("param"), parm.getParamSpec());
			if (morphed == null)
				debug2("Unable to morph param '" + ctsid.getPart("param") + "' with param spec '"
					+ parm.getParamSpec() + "'");
			else
			{
				tsidRet.setDataType(null);
				tsidRet.setDataType(DataType.getDataType(Constants.datatype_CWMS, morphed));
				transformed = true;
			}
		}

		String s = parm.getParamType();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("ParamType", s);
			transformed = true;
		}
		s = parm.getInterval();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Interval", s);
			transformed = true;
		}
		s = parm.getDuration();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Duration", s);
			transformed = true;
		}
		s = parm.getVersion();
		if (s != null && s.trim().length() > 0)
		{
			if (s.contains("*"))
			{
				String morphed = TsidMorpher.morph(ctsid.getPart("version"), s);
				if (morphed == null)
					debug2("Unable to morph param '" + ctsid.getPart("version")
						+ "' with version spec '" + s + "'");
				else
				{
					ctsid.setVersion(morphed);
					transformed = true;
				}
			}
			else
				tsidRet.setPart("Version", s);
			transformed = true;
		}
		return transformed;
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
		return OpenTsdbFlags.flags2screeningString(flags);
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

		DaiBase dao = new DaoBase(this, "OpenTsdb");
		try
		{
			ResultSet rs = dao.doQuery(q);
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("OpenTsdb.listParamTypes: " + ex);
		}
		finally
		{
			dao.close();
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

		DaiBase dao = new DaoBase(this, "OpenTsdb");
		try
		{
			ResultSet rs = dao.doQuery(q);
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("OpenTsdb.listVersions: " + ex);
		}
		finally
		{
			dao.close();
		}

		return ret;
	}

	public String getStorageUnitsForDataType(DataType dt)
	{
		String pgname = OpenTsdbSettings.instance().storagePresentationGroup;

		PresentationGroup pg = Database.getDb().presentationGroupList.find(pgname);
		if (pg == null)
			return null;
		DataPresentation dp = pg.findDataPresentation(dt);
		return dp == null ? null : dp.getUnitsAbbr();
	}

	@Override
	public String flags2display(int flags)
	{
		return OpenTsdbFlags.flags2screeningString(flags);
	}

	@Override
	public GroupHelper makeGroupHelper()
	{
		return new CwmsGroupHelper(this);
	}

	/**
	 * For CWMS we show all 5 path components for the site.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId, DaiBase dao)
		throws DbIoException
	{
		String header[] = new String[5];
		header[0] = "Param";
		header[1] = "Param Type";
		header[2] = "Interval";
		header[3] = "Duration";
		header[4] = "Version";

		ArrayList<String[]> ret = new ArrayList<String[]>();
		ret.add(header);

		TimeSeriesDAI tsDAO = makeTimeSeriesDAO();
		try
		{
			ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();
			for(TimeSeriesIdentifier tsid : tsids)
			{
				if (tsid.getSite().getId().equals(siteId))
					ret.add(new String[] { tsid.getPart("param"), tsid.getPart("paramtype"),
						tsid.getInterval(), tsid.getPart("duration"), tsid.getPart("version")});
			}
		}
		finally
		{
			tsDAO.close();
		}

		return ret;
	}
}
