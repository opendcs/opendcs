package opendcs.opentsdb;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.ScheduleEntryDAO;
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
	
	private PreparedStatement getMinStmt = null, getTaskListStmt;
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

	/**
	 * TSDB version 5 & above use a join with CP_COMP_DEPENDS to determine
	 * not only what the new data is, but what computations depend on it.
	 * The dependent computation IDs are stored inside each CTimeSeries.
	 */
	@Override
	public DataCollection getNewData(DbKey applicationId)
		throws DbIoException
	{
		// Reload the TSID cache every hour.
		if (System.currentTimeMillis() - lastTsidCacheRead > 3600000L)
		{
			lastTsidCacheRead = System.currentTimeMillis();
			TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
			try { timeSeriesDAO.reloadTsIdCache(); }
			finally { timeSeriesDAO.close(); }
		}

		DataCollection dataCollection = new DataCollection();

		// MJM 2/14/18 - From Dave Portin. Original failTimeClause was:
		//		" and (a.FAIL_TIME is null OR "
		//		+ "SYSDATE - to_date("
		//		+ "to_char(a.FAIL_TIME,'dd-mon-yyyy hh24:mi:ss'),"
		//		+ "'dd-mon-yyyy hh24:mi:ss') >= 1/24)";

		int minRecNum = -1;
		String what = "Preparing min statement query";
		try
		{
			if (getMinStmt == null)
			{
				// 1st query gets min record num so that I can do a range query afterward.
				String failTimeClause = "";
				if (DecodesSettings.instance().retryFailedComputations)
					failTimeClause = " and (a.FAIL_TIME is null OR "
						+ System.currentTimeMillis() + " -  a.FILE_TIME >= 3600000)";
				
				getMinStmtQuery = "select min(a.record_num) from cp_comp_tasklist a "
					+ "where a.LOADING_APPLICATION_ID = " + applicationId
					+ failTimeClause;
				debug1("getMinStmtQuer=" + getMinStmtQuery);
				getMinStmt = conn.prepareStatement(getMinStmtQuery);
	
				// 2nd query gets tasklist recs within record_num range.
				getTaskListStmtQuery = 
					"select a.RECORD_NUM, a.TS_ID, a.num_value, a.sample_time, "
					+ "a.DELETE_FLAG, a.flags "
					+ "from CP_COMP_TASKLIST a "
					+ "where a.LOADING_APPLICATION_ID = " + applicationId
					+ failTimeClause;

				if (isOracle())
				{
					// ROWNUM needs to be part of where clause before ORDER BY clause
					getTaskListStmtQuery = getTaskListStmtQuery + " and ROWNUM < 20000"
						+ " order by a.ts_id, a.sample_time";
				}
				else // PostgreSQL
				{
					// LIMIT goes after the ORDER BY clause.
					getTaskListStmtQuery = getTaskListStmtQuery 
						+ " order by a.ts_id, a.sample_time"
						+ " limit 20000";
				}
				debug1("getTaskListStmtQuery=" + getTaskListStmtQuery);
				what = "Preparing tasklist query statment";
				getTaskListStmt = conn.prepareStatement(getTaskListStmtQuery);
			}
			
			what = "Executing prepared stmt '" + getMinStmtQuery + "'";
			debug3(what);
			ResultSet rs = getMinStmt.executeQuery();
			
			if (rs == null || !rs.next())
			{
				debug1("No new data for appId=" + applicationId);
				reclaimTasklistSpace();
				return dataCollection;
			}
			
			minRecNum = rs.getInt(1);
			if (rs.wasNull())
			{
				debug1("No new data for appId=" + applicationId);
				minRecNum = -1;
				reclaimTasklistSpace();
				return dataCollection;
			}
		}
		catch(SQLException ex)
		{
			warning("getNewData error while" + what + ": " + ex);
			return dataCollection;
		}

		ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
		ArrayList<Integer> badRecs = new ArrayList<Integer>();
		TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
		try
		{
			what = "Executing '" + getTaskListStmtQuery + "'";
			debug3(what);
			ResultSet rs = getTaskListStmt.executeQuery();
			while (rs.next())
			{
				// Extract the info needed from the result set row.
				int recordNum = rs.getInt(1);
				DbKey sdi = DbKey.createDbKey(rs, 2);
				double value = rs.getDouble(3);
				boolean valueWasNull = rs.wasNull();
				Date timeStamp = new Date(rs.getLong(4));
				String df = rs.getString(5);
				boolean deleted = TextUtil.str2boolean(df);
				int flags = rs.getInt(6);
				
				TasklistRec rec = new TasklistRec(recordNum, sdi, value,
					valueWasNull, timeStamp, deleted, null, null, flags);
				tasklistRecs.add(rec);
			}

			RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);
			
			// Process the real-time records collected above.
			for(TasklistRec rec : tasklistRecs)
				processTasklistEntry(rec, dataCollection, rrhandle, badRecs, applicationId);
			
			dataCollection.setTasklistHandle(rrhandle);
			
			// Delete the bad tasklist recs, 250 at a time.
			if (badRecs.size() > 0)
				Logger.instance().debug1("getNewDataSince deleting " + badRecs.size()
					+ " bad tasklist records.");
			while (badRecs.size() > 0)
			{
				StringBuilder inList = new StringBuilder();
				int n = badRecs.size();
				int x=0;
				for(; x<250 && x<n; x++)
				{
					if (x > 0)
						inList.append(", ");
					inList.append(badRecs.get(x).toString());
				}
				String q = "delete from CP_COMP_TASKLIST "
					+ "where RECORD_NUM IN (" + inList.toString() + ")";
				doModify(q);
				for(int i=0; i<x; i++)
					badRecs.remove(0);
			}
			
			// Show each tasklist entry in the log if we're at debug level 3
			if (Logger.instance().getMinLogPriority() <= Logger.E_DEBUG3)
			{
				List<CTimeSeries> allts = dataCollection.getAllTimeSeries();
				debug3("getNewData, returning " + allts.size() + " TimeSeries.");
				for(CTimeSeries ts : allts)
					debug3("ts " + ts.getTimeSeriesIdentifier().getUniqueString() + " " 
						+ ts.size() + " values.");
			}
			
			return dataCollection;
		}
		catch(SQLException ex)
		{
			System.err.println("Error while " + what + ": " + ex);
			ex.printStackTrace();
			throw new DbIoException("Error while " + what + ": " + ex);
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}


	private void processTasklistEntry(TasklistRec rec,
		DataCollection dataCollection, RecordRangeHandle rrhandle,
		ArrayList<Integer> badRecs, DbKey applicationId)
		throws DbIoException
	{
		// Find time series if already in data collection.
		// If not construct one and add it.
		CTimeSeries cts = dataCollection.getTimeSeriesByUniqueSdi(rec.getSdi());
		if (cts == null)
		{
			TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
			try
			{
				TimeSeriesIdentifier tsid = 
					timeSeriesDAO.getTimeSeriesIdentifier(rec.getSdi());
				String tabsel = tsid.getPart("paramtype") + "." + 
					tsid.getPart("duration") + "." + tsid.getPart("version");
				cts = new CTimeSeries(rec.getSdi(), tsid.getInterval(),
					tabsel);
				cts.setModelRunId(-1);
				cts.setTimeSeriesIdentifier(tsid);
				
				// NOTE: In OpenTsdb, tasklist values are always in storage units.
				cts.setUnitsAbbr(tsid.getStorageUnits());
				if (fillDependentCompIds(cts, applicationId) == 0)
				{
					warning("Deleting tasklist rec for '" + tsid.getUniqueString() 
						+ "' because no dependent comps.");
					if (badRecs != null)
						badRecs.add(rec.getRecordNum());
					return;
				}

				try { dataCollection.addTimeSeries(cts); }
				catch(decodes.tsdb.DuplicateTimeSeriesException ex)
				{ // won't happen -- already verified it's not there.
				}
			}
			catch(NoSuchObjectException ex)
			{
				warning("Deleting tasklist rec for non-existent ts_code "
					+ rec.getSdi());
				if (badRecs != null)
					badRecs.add(rec.getRecordNum());
				return;
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		if (rrhandle != null)
			rrhandle.addRecNum(rec.getRecordNum());

		// Construct timed variable with appropriate flags & add it.
		TimedVariable tv = new TimedVariable(rec.getValue());
		tv.setTime(rec.getTimeStamp());
		tv.setFlags((int)rec.getQualityCode());
		
		if (!rec.isDeleted() && !rec.isValueWasNull())
		{
			VarFlags.setWasAdded(tv);
			cts.addSample(tv);
			// Remember which tasklist records are in this timeseries.
			cts.addTaskListRecNum(rec.getRecordNum());
			Logger.instance().debug3("Added value " + tv + " to time series "
				+ cts.getTimeSeriesIdentifier().getUniqueString()
				+ " flags=0x" + Integer.toHexString(tv.getFlags())
				+ " cwms qualcode=0x" + Long.toHexString(rec.getQualityCode()));
		}
		else
		{
			VarFlags.setWasDeleted(tv);
			Logger.instance().warning("Discarding deleted value " + tv.toString()
				+ " for time series " + cts.getTimeSeriesIdentifier().getUniqueString()
				+ " flags=0x" + Integer.toHexString(tv.getFlags())
				+ " cwms qualcode=0x" + Long.toHexString(rec.getQualityCode()));
		}
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
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId)
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
