package decodes.hdb;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.opendcs.database.ExceptionHelpers;
import org.opendcs.utils.FailableResult;

import oracle.jdbc.OracleCallableStatement;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.RecordRangeHandle;
import decodes.tsdb.TasklistRec;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.TsdbException;
import decodes.tsdb.VarFlags;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.TSUtil;
import opendcs.dai.AlarmDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DbObjectCache;
import opendcs.dao.DbObjectCache.CacheIterator;

public class HdbTimeSeriesDAO extends DaoBase implements TimeSeriesDAI
{
	// TODO: Integrate the cache with the methods below.
	protected static DbObjectCache<TimeSeriesIdentifier> cache =
		new DbObjectCache<TimeSeriesIdentifier>(60 * 60 * 1000L, false);
	private SiteDAI siteDAO = null;
	protected DataTypeDAI dataTypeDAO = null;
	private static SimpleDateFormat rwdf = null;

	// MJM 2016/1/8 Calls to reloadTsIdCache only does a refresh for time series
	// since the last call.
	// Once per hour only it does a full load.
	private static long lastCacheLoad = 0L;
	private static final long CACHE_RELOAD_INTERVAL = 3600000L;
	private static long lastCacheRefresh = 0L;
	private static final long CACHE_REFRESH_OVERLAP = 120000L;

	private String tsidQuery =
		"SELECT a.ts_id, a.site_datatype_id, a.interval, a.table_selector, a.model_id, "
		+ "b.SITE_ID, b.DATATYPE_ID, d.UNIT_COMMON_NAME, e.site_common_name, c.datatype_common_name "
		+ "FROM CP_TS_ID a, HDB_SITE_DATATYPE b, HDB_DATATYPE c, HDB_UNIT d, HDB_SITE e ";
	private String tsidJoinClause =
		  " WHERE a.site_datatype_id = b.site_datatype_id "
		+ " AND b.DATATYPE_ID = c.DATATYPE_ID "
		+ " and c.UNIT_ID = d.UNIT_ID"
        + " and b.SITE_ID = e.SITE_ID";

	private long lastTsidCacheRead = 0L;


	protected HdbTimeSeriesDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "HdbTimeSeriesDAO");
		siteDAO = tsdb.makeSiteDAO();
		dataTypeDAO = tsdb.makeDataTypeDAO();
		if (rwdf == null)
		{
			rwdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			rwdf.setTimeZone(TimeZone.getTimeZone(db.getDatabaseTimezone()));
		}
	}

	@Override
	public FailableResult<TimeSeriesIdentifier,TsdbException> findTimeSeriesIdentifier(String uniqueString)
	{
		return findTimeSeriesIdentifier(uniqueString, false);
	}

	@Override
	public FailableResult<TimeSeriesIdentifier,TsdbException> findTimeSeriesIdentifier(String uniqueString, boolean ignoreCacheTime)
	{
		debug3("getTimeSeriesIdentifier for '" + uniqueString + "'");

		try
		{
			int paren = uniqueString.lastIndexOf('(');
			String displayName = null;
			if (paren > 0 && uniqueString.trim().endsWith(")"))
			{
				displayName = uniqueString.substring(paren+1);
				uniqueString = uniqueString.substring(0,  paren);
				int endParen = displayName.indexOf(')');
				if (endParen > 0)
					displayName = displayName.substring(0,  endParen);
				debug3("using display name '" + displayName + "', unique str='" + uniqueString + "'");
			}

			HdbTsId ret = (HdbTsId)cache.getByUniqueName(uniqueString);
			if (ret != null)
			{
				if (displayName != null)
				{
					debug3("Setting display name to '" + displayName + "'");
					ret.setDisplayName(displayName);
				}
				return FailableResult.success(ret);
			}


			HdbTsId htsid = new HdbTsId(uniqueString);
			String tsSiteName = htsid.getSiteName();
			DbKey siteId = siteDAO.lookupSiteID(tsSiteName);
			if (siteId == Constants.undefinedId)
			{
				return FailableResult.failure(new NoSuchObjectException("No hdb site with name '" + tsSiteName + "'"));
			}
			DbKey sdi = ((HdbTimeSeriesDb)db).lookupSDI(siteId, htsid.getDataType().getCode());
			if (sdi == Constants.undefinedId)
			{
				return FailableResult.failure(new NoSuchObjectException("No SDI for '" + uniqueString + "'"));
			}
			htsid.setSite(siteDAO.getSiteById(siteId));
			htsid.setSdi(sdi);
			if (htsid.getTableSelector() == null)
				htsid.setTableSelector("R_");

			// Unique name in cache may be from a different site name type, so
			// now re-search the cache with the site datatype ID.
			for(CacheIterator cit = cache.iterator(); cit.hasNext(); )
			{
				HdbTsId tsid = (HdbTsId)cit.next();
				if (sdi.equals(tsid.getSdi())
				&& tsid.getInterval().equalsIgnoreCase(htsid.getInterval())
				&& tsid.getTableSelector().equalsIgnoreCase(htsid.getTableSelector()))
				{
					return FailableResult.success(tsid);
				}
			}

			debug3("cache does not have '" + uniqueString + "'");

			String q = "SELECT TS_ID "
				+ "FROM CP_TS_ID "
				+ "WHERE SITE_DATATYPE_ID = " + htsid.getSdi()
				+ " AND LOWER(INTERVAL) = " + sqlString(htsid.getInterval().toLowerCase())
				+ " AND TABLE_SELECTOR = " + sqlString(htsid.getTableSelector());
			if (htsid.getTableSelector().equalsIgnoreCase("M_"))
			{
				q = q + " AND MODEL_ID = " + htsid.getPart(HdbTsId.MODELID_PART);
			}
			try
			{
				ResultSet rs = doQuery(q);
				if (rs != null && rs.next())
				{
					htsid.setKey(DbKey.createDbKey(rs, 1));
				}
				else
				{
					String msg = "No CP_TS_ID for '" + htsid.getUniqueString() + "' NoSuchObject";
					warning(msg);
					return FailableResult.failure(new NoSuchObjectException(msg));
				}
			}
			catch(DbIoException ex)
			{
				return FailableResult.failure(ex);
			}
			catch(SQLException ex)
			{
				return FailableResult.failure(new NoSuchObjectException("Cannot get TS_ID for '" + htsid.getUniqueString() + "'"));
			}
			FailableResult<TimeSeriesIdentifier,TsdbException> tmp = findTimeSeriesIdentifier(htsid.getKey());
			
			if (tmp.isSuccess())
			{
				// Preserve the modelRunId if it was set in the uniqueString. Also the display name.
				HdbTsId tsId = (HdbTsId)tmp.getSuccess();
				tsId.modelRunId = htsid.modelRunId;
				if (displayName != null)
				{
					debug3("Setting display name to '" + displayName + "'");
					tsId.setDisplayName(displayName);
				}
			}
			return tmp;
		}
		catch (TsdbException ex)
		{
			return FailableResult.failure(ex);
		}
	}

	@Override
	public FailableResult<TimeSeriesIdentifier,TsdbException> findTimeSeriesIdentifier(DbKey key)
	{
		String q = tsidQuery + tsidJoinClause + " and a.ts_id = " + key;

		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				return FailableResult.success(rs2TsId(rs));
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
			return FailableResult.failure(new DbIoException("Error looking up TS Info for ts_id =" + key + ": " + ex));
		}
		return FailableResult.failure(new NoSuchObjectException("No time-series with ts_code=" + key));
	}

	/**
	 * Columns in the following order:
	 * ts_id, site_datatype_id, interval, table_selector, model_id, SITE_ID, DATATYPE_ID, UNITS_ABBR
	 * @param rs the result set
	 * @return a new HdbTimeSeriesIdentifier object
	 * @throws SQLException on unexpected SQL error
	 * @throws DbIoException on query error
	 * @throws NoSuchObjectException if site doesn't exist with specified ID.
	 */
	private HdbTsId rs2TsId(ResultSet rs)
		throws SQLException, DbIoException, NoSuchObjectException
	{
		HdbTsId tsid = new HdbTsId();
		tsid.setKey(DbKey.createDbKey(rs, 1));
		tsid.setSdi(DbKey.createDbKey(rs, 2));
		tsid.setInterval(rs.getString(3));
		tsid.setPart(HdbTsId.TABSEL_PART, rs.getString(4));
		int modelId = rs.getInt(5);
		if (rs.wasNull())
			modelId = Constants.undefinedIntKey;
		tsid.setPart(HdbTsId.MODELID_PART, ""+modelId);
		DbKey siteId = DbKey.createDbKey(rs, 6);
		try
		{
			tsid.setSite(siteDAO.getSiteById(siteId));
		}
		catch(NoSuchObjectException ex)
		{
			warning("TSID with key=" + tsid.getKey() + " has reference to non-existant site ID="
				+ siteId + ", ignored.");
			throw ex;
		}
		DataType dt = DataType.getDataType(DbKey.createDbKey(rs, 7));
		tsid.setDataType(dt);
		tsid.setStorageUnits(rs.getString(8));

		String siteCommonName = rs.getString(9);
		String dtCommonName = rs.getString(10);
		tsid.setDescription(dtCommonName + " at " + siteCommonName);

		return tsid;
	}

	@Override
	public void close()
	{
		dataTypeDAO.close();
		siteDAO.close();
		super.close();
	}

	@Override
	public void fillTimeSeriesMetadata(CTimeSeries ts) throws DbIoException,
		BadTimeSeriesException
	{
		HdbTsId tsid = (HdbTsId)ts.getTimeSeriesIdentifier();
		if (tsid == null)
		{
			try
			{
				DbKey ts_id = lookupTsId(ts.getSDI(), ts.getInterval(),
					ts.getTableSelector(), ts.getModelId());

				tsid = (HdbTsId)getTimeSeriesIdentifier(ts_id);
				ts.setTimeSeriesIdentifier(tsid);
			}
			catch(NoSuchObjectException ex)
			{
				return;
			}
		}

		ts.setDisplayName(tsid.getDisplayName());
		ts.setUnitsAbbr(tsid.getStorageUnits());
	}

	/**
 	 * Lookup a ts_id from cp_ts_id table.
	 * @param sdi
	 * @param interval
	 * @param tabsel
	 * @param modelId
	 * @return ts_id
	 * @throws DbIoException on SQL Error
	 * @throws NoSuchObjectException if no such ts_id exists.
	 */
	private DbKey lookupTsId(DbKey sdi, String interval, String tabsel, int modelId)
		throws DbIoException, NoSuchObjectException
	{
		if (DbKey.isNull(sdi) || interval == null || tabsel == null)
		{
			String msg = module + ".lookupTsId invalid time series sdi="
				+ sdi + ", interval=" + interval + ", tabsel=" + tabsel;
			warning(msg);
			throw new NoSuchObjectException(msg);
		}
		String q = "select ts_id from cp_ts_id "
			+ "where site_datatype_id = " + sdi
			+ " and lower(interval) = " + sqlString(interval.toLowerCase())
			+ " and table_selector = " + sqlString(tabsel);
		if (tabsel.equals("M_"))
			q = q + " and model_id = " + modelId;

		try
		{
			ResultSet rs = doQuery(q);
			if (rs == null || !rs.next())
			{
				warning("Cannot read meta data for '" + q + "'");
				throw new NoSuchObjectException(q);
			}
			return DbKey.createDbKey(rs, 1);
		}
		catch(SQLException ex)
		{
			String msg = "Error in '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public int fillTimeSeries( CTimeSeries ts, Date from, Date until )
		throws DbIoException, BadTimeSeriesException
	{
		return fillTimeSeries(ts, from, until, true, true, true);
	}


	@Override
	public int fillTimeSeries(CTimeSeries ts, Date from, Date until,
		boolean include_lower, boolean include_upper, boolean overwriteExisting)
		throws DbIoException, BadTimeSeriesException
	{
		fillTimeSeriesMetadata(ts);

		UnitConverter unitConverter = db.makeUnitConverterForRead(ts);

		String lower_check = " >= ";
		String upper_check = " <= ";
		if (!include_lower) lower_check = " > ";
		if (!include_upper) upper_check = " < ";
		String table = ts.getTableSelector() + ts.getInterval();

		String tabsel = ts.getTableSelector();
		boolean isModeled = tabsel != null && TextUtil.startsWithIgnoreCase(ts.getTableSelector(), "M");

		String fields = "START_DATE_TIME, VALUE";
		if (!isModeled)
			fields = fields + ", DERIVATION_FLAGS"; // Get derivation flags for REAL data only

		String q = "select " + fields + " from " + table
			+ " where SITE_DATATYPE_ID = " + ts.getSDI()
			+ " and START_DATE_TIME " + lower_check  + db.sqlDate(from)
			+ " and START_DATE_TIME " + upper_check + db.sqlDate(until);
		if (isModeled)
		{
			if (ts.getModelRunId() == Constants.undefinedIntKey)
			{
				int modelId = ts.getModelId();
				if (modelId == Constants.undefinedIntKey)
				{
					warning("Cannot query for data for '" + ts.getTimeSeriesIdentifier().getUniqueString()
						+ "' because model and model Run ID are both undefined.");
					return 0;
				}
				ts.setModelRunId(db.findMaxModelRunId(modelId));
			}
			q = q + " and model_run_id = " + ts.getModelRunId();
		}

		try
		{
			ResultSet rs = doQuery(q);
			int numAdded = 0;
			while (rs.next())
			{
				Date timeStamp = db.getFullDate(rs, 1);

				// Don't overwrite data already in the time series!
				if (!overwriteExisting
				 && ts.findWithin(timeStamp.getTime()/1000L, 10) != null)
					continue;

				double value = rs.getDouble(2);
				TimedVariable tv = new TimedVariable(value);
				if (unitConverter != null)
				{
					try { tv.setValue(unitConverter.convert(value)); }
					catch (DecodesException ex)
					{
						warning("fillTimeSeries: " + ex);
					}
				}
				tv.setTime(timeStamp);
				if (!isModeled)
				{
					String sf = rs.getString(3);
					if (sf != null)
						tv.setFlags(HdbFlags.screening2flags(sf));
				}
				ts.addSample(tv);
				numAdded++;
			}
			return numAdded;
		}
		catch(SQLException ex)
		{
			String msg= "Error reading data with query '" + q
				+ "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public int fillTimeSeries(CTimeSeries ts, Collection<Date> queryTimes)
		throws DbIoException, BadTimeSeriesException
	{
		fillTimeSeriesMetadata(ts);
		HdbTsId tsid = (HdbTsId)ts.getTimeSeriesIdentifier();
		UnitConverter unitConverter = db.makeUnitConverterForRead(ts);

		String table = tsid.getTableSelector() + tsid.getInterval();
		String fields = "START_DATE_TIME, VALUE";
		String qbase = "select " + fields + " from " + table
			+ " where SITE_DATATYPE_ID = " + tsid.getSdi();

		String tabsel = ts.getTableSelector();
		if (tabsel != null && tabsel.length() > 0
		 && tabsel.toLowerCase().charAt(0) == 'm')
			qbase = qbase + " and model_run_id = " + ts.getModelRunId();

		qbase = qbase + " and START_DATE_TIME IN (";

		int datesPerQuery = 10;
		int start = 0;
		int end = 0;
		int numAdded = 0;
		StringBuilder sb = new StringBuilder();
		int size = queryTimes.size();
		Date times[] = new Date[size];
		times = queryTimes.toArray(times);
		while (end < size)
		{
			sb.append(db.sqlDate(times[end]));
			end++;
			if (end - start >= datesPerQuery || end == size)
			{
				sb.append(")");
				String q = qbase + sb.toString();
				sb.setLength(0);
				start = end;
				try
				{
					ResultSet rs = doQuery(q);
					while (rs.next())
					{
						Date timeStamp = db.getFullDate(rs, 1);

						// Don't overwrite data already in the time series!
						if (ts.findWithin(timeStamp.getTime()/1000L, 10)!=null)
							continue;

						double value = rs.getDouble(2);
						TimedVariable tv = new TimedVariable(value);

						if (unitConverter != null)
						{
							try { tv.setValue(unitConverter.convert(value)); }
							catch (DecodesException ex)
							{
								warning("fillTimeSeries: " + ex);
							}
						}

						tv.setTime(timeStamp);
						ts.addSample(tv);
						numAdded++;
					}
				}
				catch(SQLException ex)
				{
					String msg= "Error reading data with query '" + q
						+ "': " + ex;
					warning(msg);
					throw new DbIoException(msg);
				}
			}
			else if (end < size)
				sb.append(", ");
		}
		return numAdded;
	}

	@Override
	public TimedVariable getPreviousValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		String table = ts.getTableSelector() + ts.getInterval();
		String fields = "START_DATE_TIME, VALUE";
		String q = "select " + fields
			+ " from " + table
			+ " where SITE_DATATYPE_ID = " + ts.getSDI();

		String tabsel = ts.getTableSelector();
		if (tabsel != null && tabsel.length() > 0
		 && tabsel.toLowerCase().charAt(0) == 'm')
			q = q + " and model_run_id = " + ts.getModelRunId();

		q = q
			+ " and start_date_time = "
			+   " (select max(start_date_time) from " + table
			+   " where SITE_DATATYPE_ID = " + ts.getSDI()
 			+   " and start_date_time < " + db.sqlDate(refTime)
			+	")";

		UnitConverter unitConverter = db.makeUnitConverterForRead(ts);

		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
				return null;  // There is no previous value.

			Date timeStamp = db.getFullDate(rs, 1);
			double value = rs.getDouble(2);
			TimedVariable tv = new TimedVariable(value);

			if (unitConverter != null)
			{
				try { tv.setValue(unitConverter.convert(value)); }
				catch (DecodesException ex)
				{
					warning("fillTimeSeries: " + ex);
				}
			}

			tv.setTime(timeStamp);
			ts.addSample(tv);
			return tv;
		}
		catch(SQLException ex)
		{
			String msg= "Error reading data with query '" + q
				+ "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public TimedVariable getNextValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		String table = ts.getTableSelector() + ts.getInterval();
		String fields = "START_DATE_TIME, VALUE";
		String q = "select " + fields
			+ " from " + table
			+ " where SITE_DATATYPE_ID = " + ts.getSDI();

		String tabsel = ts.getTableSelector();
		if (tabsel != null && tabsel.length() > 0
		 && tabsel.toLowerCase().charAt(0) == 'm')
			q = q + " and model_run_id = " + ts.getModelRunId();

		q = q
			+ " and start_date_time = "
			+   " (select min(start_date_time) from " + table
			+   " where SITE_DATATYPE_ID = " + ts.getSDI()
 			+   " and start_date_time > " + db.sqlDate(refTime)
			+   ")";

		UnitConverter unitConverter = db.makeUnitConverterForRead(ts);

		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
				return null;  // There is no next value.

			Date timeStamp = db.getFullDate(rs, 1);
			double value = rs.getDouble(2);
			TimedVariable tv = new TimedVariable(value);

			if (unitConverter != null)
			{
				try { tv.setValue(unitConverter.convert(value)); }
				catch (DecodesException ex)
				{
					warning("fillTimeSeries: " + ex);
				}
			}

			tv.setTime(timeStamp);
			ts.addSample(tv);
			return tv;
		}
		catch(SQLException ex)
		{
			String msg= "Error reading data with query '" + q
				+ "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void saveTimeSeries(CTimeSeries ts) throws DbIoException,
		BadTimeSeriesException
	{
		// Make sure the units are correct and if not, convert them.
		TimeSeriesIdentifier tsid = ts.getTimeSeriesIdentifier();
		if (tsid == null)
		{
			try
			{
				DbKey tsKey = lookupTsId(ts.getSDI(), ts.getInterval(),
					ts.getTableSelector(), ts.getModelId());
				tsid = this.getTimeSeriesIdentifier(tsKey);
				ts.setTimeSeriesIdentifier(tsid);
			}
			catch (NoSuchObjectException ex)
			{
				warning("saveTimeSeries: TSID="
					+ (ts.getTimeSeriesIdentifier()==null?"null":ts.getTimeSeriesIdentifier().getUniqueString())
					+ " Cannot lookup tsid: " + ex);
			}
		}
		if (tsid != null)
		{
			debug3("Saving " + tsid.getUniqueString() + ", from cp units="
				+ ts.getUnitsAbbr() + ", required=" + tsid.getStorageUnits());
			TSUtil.convertUnits(ts, tsid.getStorageUnits());
		}
		else
		{
			warning("saveTimeSeries: Cannot save with null tsid.");
			return;
		}

		// If at least one sample is marked TO_WRITE, call doSave.
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if (VarFlags.mustWrite(tv))
			{
				doSave(ts);
				break;
			}
		}

		// If at least one sample is marked TO_DELETE, call doDelete.
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if (VarFlags.mustDelete(tv))
			{
				doDelete(ts);
				break;
			}
		}
	}

	/**
	 * Internal method to write data to the database using the write_to_hdb
	 * procedure.
	 */
	private void doSave(CTimeSeries ts)
		throws DbIoException, BadTimeSeriesException
	{
		String tabsel = ts.getTableSelector();
		long modelrun_id = 0;
		if (tabsel.equalsIgnoreCase("M_"))
		{
			modelrun_id = ts.getModelRunId();
			if (modelrun_id == Constants.undefinedIntKey)
				modelrun_id = db.getWriteModelRunId();
			if (modelrun_id == Constants.undefinedIntKey)
			{
				int model_id = ts.getModelId();
				modelrun_id = db.findMaxModelRunId(model_id);
			}
		}
	// Added the four default arguments from the write_to_hdb procedure, so that the CP can set the OVERWRITE_FLAG if specified

	// PROCEDURE write_to_hdb
	//  Argument Name                  Type                    In/Out Default?
	//  ------------------------------ ----------------------- ------ --------
	//1 SAMPLE_SDI                     NUMBER                  IN
	//2 SAMPLE_DATE_TIME               DATE                    IN
	//3 SAMPLE_VALUE                   NUMBER(126)             IN
	//4 SAMPLE_INTERVAL                VARCHAR2                IN
	//5 LOADING_APP_ID                 NUMBER                  IN
	//6 COMPUTE_ID                     NUMBER                  IN
	//7 MODELRUN_ID                    NUMBER                  IN
	//8 VALIDATION_FLAG                CHAR                    IN
	//9 DATA_FLAGS                     VARCHAR2                IN
	//10 TIME_ZONE					   VARCHAR2				   IN	DEFAULT NULL
	//11 OVERWRITE_FLAG				   VARCHAR2				   IN	DEFAULT NULL
	//12 AGEN_ID					   NUMBER				   IN	DEFAULT NULL
	//13 SAMPLE_END_DATE_TIME		   DATE				   	   IN	DEFAULT NULL

		String q =
			"{ call write_to_hdb(?,to_date(?,'DD.MM.YYYY HH24:MI:SS'),?,?,?,?,?,?,?,?,?)}";

		DbKey compId = ts.getComputationId();
		if (compId == Constants.undefinedId)
			compId = DbKey.createDbKey(1); // Default, meaning 'no computation'

		String idString = " sdi=" + ts.getSDI()
				+ ", intv=" + ts.getInterval()
				+ ", appId=" + db.getAppId()
				+ ", compId=" + compId
				+ ", modRunId=" + modelrun_id;

		Logger.instance().debug3("Saving to time series '" + idString + "'");

		try (Connection c = getConnection();
			 CallableStatement cstmt = c.prepareCall(q);
			)
		{
			// Set all the proc-input that are the same for all samples.
			cstmt.setLong(1, ts.getSDI().getValue());
			cstmt.setString(4, ts.getInterval());
			cstmt.setLong(5, db.getAppId().getValue());
			cstmt.setLong(6, compId.getValue());
			cstmt.setLong(7, modelrun_id);
			cstmt.setNull(10,java.sql.Types.VARCHAR); //parameter 10, time zone, defaults to null


			int nsaved = 0;
			int nerrors = 0;
			for(int i=0; i<ts.size(); i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if (VarFlags.mustWrite(tv))
				{
					String timestr = "";
					try
					{
						// Populate these fields for each variable:
						//2 SAMPLE_DATE_TIME               DATE                    IN
						//3 SAMPLE_VALUE                   NUMBER(126)             IN
						//8 VALIDATION_FLAG                CHAR                    IN
						//9 DATA_FLAGS                     VARCHAR2                IN
						//11 OVERWRITE_FLAG				   VARCHAR2				   IN	DEFAULT NULL
						// Populate & execute the stored procedure call.
						timestr = rwdf.format(tv.getTime());
						cstmt.setString(2, timestr);
						double v = tv.getDoubleValue();
						cstmt.setDouble(3, v);
						char valf = HdbFlags.flag2HdbValidation(tv.getFlags());
						String valfs = valf == HdbFlags.HDB_BLANK_VALIDATION ? null
							: "" + valf;
						cstmt.setString(8, valfs);
						String derf = HdbFlags.flag2HdbDerivation(tv.getFlags());
						cstmt.setString(9, derf);

						if(HdbFlags.flag2Overwrite(tv.getFlags()))
						{
							cstmt.setString(11,Character.toString(HdbFlags.HDB_OVERWRITE_FLAG));
						}
						else
						{
							cstmt.setNull(11,java.sql.Types.VARCHAR);
						}

						Logger.instance().debug3("Saving variable " + v + " at time " + timestr
							+ ", val=" + valfs + ", der=" + derf);
						cstmt.execute();
						VarFlags.clearToWrite(tv);
						nsaved++;
					}
					catch(NoConversionException ex)
					{
						String msg = "Cannot save value for " + idString
							+ " value='" + tv.getStringValue()
							+ "' - not a number.";
						Logger.instance().warning(msg);
						nerrors++;
					}
					catch(IllegalArgumentException ex)
					{
						String msg = "Cannot save value for " + idString
								+ " value='" + tv.getStringValue()
								+ "' - not a number.";
						Logger.instance().warning(msg);
						nerrors++;
					}
					catch(SQLException ex)
					{
						String msg = "SQL Error saving value for " + idString
							+ " value='" + tv.getStringValue()
							+ "', date='" + timestr + "': " + ex;
						Logger.instance().warning(msg);
						nerrors++;
					}
				}
			}
			Logger.instance().debug1("Saved " + nsaved + " samples.");
		}
		catch(SQLException ex)
		{
			String msg = "Cannot prepare statement '" + q + "' "
				+ " for " + idString + ": " + ex;
			Logger.instance().warning(msg);
			throw new BadTimeSeriesException(msg,ex);
		}
	}

	/**
	 * Internal method to delete data from the database.
	 * Called from above with a time series that has at least one sample
	 * marked TO_DELETE.
	 */
	private void doDelete(CTimeSeries ts)
		throws DbIoException, BadTimeSeriesException
	{
		String idstr = "SDI=" + ts.getSDI() + ", intv=" + ts.getInterval();
		String q =  "{ call delete_from_hdb(?,?,?,?,?,?)}";

		GregorianCalendar gc = new GregorianCalendar(
			TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone));
		try (Connection c = getConnection();
			 oracle.jdbc.OracleCallableStatement cstmt =
			 	(OracleCallableStatement)c.unwrap(oracle.jdbc.OracleConnection.class).prepareCall(q);
			)
		{
			cstmt.setLong(1, ts.getSDI().getValue());
			cstmt.setString(4, ts.getInterval());
			cstmt.setLong(5, db.getAppId().getValue());

			String tabsel = ts.getTableSelector();
			long modelRunId = tabsel.equalsIgnoreCase("M_") ? ts.getModelRunId() : 0L;
			cstmt.setLong(6, modelRunId);

			info("delete_from_hdb args: 1(sdi)=" + ts.getSDI() + ", 4(intv)=" + ts.getInterval()
				+ ", 5(appId)=" + db.getAppId() + ", 6(modelRunId)=" + modelRunId);
			for(int i=0; i<ts.size(); i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if (VarFlags.mustDelete(tv))
				{
					Date startTime = tv.getTime();
					gc.setTime(startTime);
					String intv = ts.getInterval();
					if (intv.equalsIgnoreCase(IntervalCodes.int_hour))
					{
						gc.add(Calendar.HOUR, 1);
//						gc.add(Calendar.SECOND, -1);
					}
					else if (intv.equalsIgnoreCase(IntervalCodes.int_day))
					{
						gc.add(Calendar.DAY_OF_YEAR, 1);
//						gc.add(Calendar.SECOND, -1);
					}
					else if (intv.equalsIgnoreCase(IntervalCodes.int_month))
					{
						gc.add(Calendar.MONTH, 1);
//						gc.add(Calendar.SECOND, -1);
					}
					else if (
						intv.equalsIgnoreCase(IntervalCodes.int_year)
					 || intv.equalsIgnoreCase(IntervalCodes.int_wy))
					{
						gc.add(Calendar.YEAR, 1);
//						gc.add(Calendar.SECOND, -1);
					}
					Date endTime = gc.getTime();

					try
					{
						debug3("   Deleting " + idstr
							+ " at start=" + db.getLogDateFormat().format(startTime)
							+ " end=" + db.getLogDateFormat().format(endTime));
						HdbTimeSeriesDb hdb = (HdbTimeSeriesDb)db;
						oracle.sql.DATE startd =
							((HdbOracleDateParser)hdb.getOracleDateParser()).toDATE(startTime);
						oracle.sql.DATE endd =
							((HdbOracleDateParser)hdb.getOracleDateParser()).toDATE(endTime);

//						StringBuilder sb = new StringBuilder();
//						for(byte b : startd.getBytes())
//							sb.append(" " + (int)b);
//						debug3("In callable statement, start bytes =" + sb.toString());
//						sb.setLength(0);
//						for(byte b : endd.getBytes())
//							sb.append(" " + (int)b);
//						debug3("In callable statement,   end bytes =" + sb.toString());

						cstmt.setDATE(2, startd);
						cstmt.setDATE(3, endd);
						cstmt.execute();
						VarFlags.clearToDelete(tv);
					}
					catch(SQLException ex)
					{
						String msg = "SQL Error deleting value for " + idstr
							+ ", time=" + db.getLogDateFormat().format(startTime) + ": " + ex;
						Logger.instance().warning(msg);
					}
				}
			}
		}
		catch(SQLException ex)
		{
			String msg = "Cannot prepare statement '" + q + "' "
				+ "for " + idstr;
			Logger.instance().warning(msg);
			throw new BadTimeSeriesException(msg);
		}
	}

	@Override
	public void deleteTimeSeriesRange(CTimeSeries ts, Date from, Date until)
		throws DbIoException, BadTimeSeriesException
	{
		AlarmDAI alarmDAO = db.makeAlarmDAO();
		try
		{
			alarmDAO.deleteCurrentAlarm(ts.getTimeSeriesIdentifier().getKey(), null);
			alarmDAO.deleteHistoryAlarms(ts.getTimeSeriesIdentifier().getKey(), from, until);
		}
		catch(Exception ex)
		{
			warning("deleteTimeSeries error deleting alarm records: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}

		int n = fillTimeSeries(ts, from, until, true, true, true);
		if (n == 0)
			return;

		int sz = ts.size();
		for(int i=0; i<sz; i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			Date d = tv.getTime();
			if (d.compareTo(from) >= 0 && d.compareTo(until) <= 0)
				VarFlags.setToDelete(tv);
		}

		doDelete(ts);
	}

	@Override
	public void deleteTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		AlarmDAI alarmDAO = db.makeAlarmDAO();
		try
		{
			alarmDAO.deleteCurrentAlarm(tsid.getKey(), null);
			alarmDAO.deleteHistoryAlarms(tsid.getKey(), null, null);
		}
		catch(Exception ex)
		{
			warning("deleteTimeSeries error deleting alarm records: " + ex);
		}
		finally
		{
			alarmDAO.close();
		}

		try
		{
			deleteTimeSeriesRange(makeTimeSeries(tsid), new Date(0L), new Date());
			String q = "delete from cp_ts_id where ts_id = " + tsid.getKey();
			doModify(q);
		}
		catch(Exception ex)
		{
			String msg = "Cannot delete time series '"
				+ tsid.getUniqueString() + ": " + ex;
			warning(msg);
		}
	}

	@Override
	public CTimeSeries makeTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException, NoSuchObjectException
	{
		DbKey sdi = ((HdbTsId)tsid).getSdi();
		CTimeSeries ret = new CTimeSeries(sdi, tsid.getInterval(),
			tsid.getTableSelector());
		ret.setTimeSeriesIdentifier(tsid);
		ret.setDisplayName(tsid.getDisplayName());
		String s = tsid.getPart(HdbTsId.MODELID_PART);
		try
		{
			if (s != null)
				ret.setModelId(Integer.parseInt(s));
		}
		catch(Exception ex)
		{
			warning("makeTimeSeries - Bad modelId '" + s + "' -- ignored.");
		}
		s = tsid.getPart(HdbTsId.MODELRUNID_PART);
		try
		{
			if (s != null)
				ret.setModelRunId(Integer.parseInt(s));
			else
				ret.setModelRunId(db.findMaxModelRunId(ret.getModelId()));
		}
		catch(Exception ex)
		{
			warning("makeTimeSeries - Bad modelRunId '" + s + "' -- ignored.");
		}
		return ret;
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> listTimeSeries()
		throws DbIoException
	{
		// MJM 20161025 don't reload more if already done within threshold.
		if (System.currentTimeMillis() - lastCacheRefresh > cacheReloadMS)
			reloadTsIdCache();

		ArrayList<TimeSeriesIdentifier> ret = new ArrayList<TimeSeriesIdentifier>();
		for (Iterator<TimeSeriesIdentifier> tsidit = cache.iterator(); tsidit.hasNext(); )
			ret.add(tsidit.next());
		return ret;
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> listTimeSeries(boolean forceRefresh)
		throws DbIoException
	{
		if (forceRefresh)
			lastCacheRefresh = 0L;
		return listTimeSeries();
	}


	@Override
	public synchronized void reloadTsIdCache() throws DbIoException
	{
		// Each TSID will need a site, so prefill the site cache to prevent
		// it from doing individual reads for each site.
		if (System.currentTimeMillis() - siteDAO.getLastCacheFillMsec() > 60000L * 10)
			siteDAO.fillCache();

		String q = tsidQuery + tsidJoinClause;

		// MJM 2016/1/8 Added this block of code to minimize reloading the entire cache.
		boolean doFullLoad = System.currentTimeMillis() - lastCacheLoad > CACHE_RELOAD_INTERVAL;
		debug3("reloadTsIdCache doFullLoad=" + doFullLoad + ", lastCacheLoad=" + new Date(lastCacheLoad)
			+ ", lastCacheRefresh=" + new Date(lastCacheRefresh));
		if (!doFullLoad)
			q = q + " and a.date_time_loaded > "
				  + db.sqlDate(new Date(lastCacheRefresh-CACHE_REFRESH_OVERLAP));
		lastCacheRefresh = System.currentTimeMillis();
		if (doFullLoad)
			lastCacheLoad = lastCacheRefresh;

		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				try { cache.put(rs2TsId(rs)); }
				catch(NoSuchObjectException ex)
				{
					// do nothing, warning already issued from rs2TsId
				}
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
			throw new DbIoException("HdbTimeSeriesDAO: Error listing time series: " + ex);
		}
	}

	@Override
	public DbObjectCache<TimeSeriesIdentifier> getCache()
	{
		return cache;
	}

	@Override
	public DbKey createTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		tsid.checkValid();

		HdbTsId hdbTsId = (HdbTsId)tsid;
		HdbTimeSeriesDb hdbDb = (HdbTimeSeriesDb)db;

		debug3("createTimeSeries '" + tsid.getUniqueString() + "'");

		// If this is a new SDI add an entry to HDB_SITE_DATATYPE.
		DbKey sdi = hdbTsId.getSdi();
		if (sdi == Constants.undefinedId)
		{
			Site site = tsid.getSite();
			if (site == null)
				throw new NoSuchObjectException("Cannot create Time Series: no site!");
			DbKey siteId = site.getId();
			DataType dt = tsid.getDataType();
			if (dt == null)
				throw new NoSuchObjectException("Cannot create Time Series: no data type!");

			sdi = hdbDb.lookupSDI(siteId, dt.getCode());
			if (DbKey.isNull(sdi))
			{
				// Create the SDI here
				String q = "insert into hdb_site_datatype values("
					+ siteId + ", " + dt.getCode() + ", 0";
				try { doModify(q); }
				catch(Exception ex)
				{
					throw new BadTimeSeriesException("Cannot create time series for "
						+ tsid.getUniqueString());
				}
				sdi = hdbDb.lookupSDI(siteId, dt.getCode());
				if (sdi == Constants.undefinedId)
					throw new NoSuchObjectException("Cannot create SDI with siteId="
						+ siteId +", dataTypeCode=" + dt.getCode());
			}
			hdbTsId.setSdi(sdi);
		}

		// Now we have a valid SDI. Lookup the CP_TS_ID
		String q = "select ts_id from cp_ts_id "
			+ "where site_datatype_id = " + sdi
			+ " and lower(interval) = " + sqlString(tsid.getInterval().toLowerCase())
			+ " and table_selector = " + sqlString(tsid.getTableSelector());
		if (tsid.getTableSelector().equalsIgnoreCase("M_"))
			q = q + " and model_id = " + hdbTsId.modelId;
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				DbKey tsId = DbKey.createDbKey(rs, 1);
				tsid.setKey(tsId);
				return tsId;
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in '" + q + "': " + ex);
		}

		// No such entry yet in CP_TS_ID, create one.
		// TS_ID and DATE_TIME_LOADED will be provided by the trigger.
		q = "insert into cp_ts_id values(0, " + sdi + ", "
			+ sqlString(tsid.getInterval().toLowerCase()) + ", "
			+ sqlString(tsid.getTableSelector()) + ", "
			+ hdbTsId.modelId + ", null)";
		doModify(q);
		return lookupTsIdKey(hdbTsId);
	}

	@Override
	public void setAppModule(String module)
	{
		// Do nothing. HDB doesn't use modules.
	}

	@Override
	public void modifyTSID(TimeSeriesIdentifier tsid)
			throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		// Not implemented for HDB
	}


	@Override
	public DataCollection getNewData(DbKey applicationId)
		throws DbIoException
	{
		// Since DAO is recreated every compApp loop, removed cache load
		// todo: investigate tsid cache location

		String q = "";
		String attrList = "RECORD_NUM, SITE_DATATYPE_ID, INTERVAL, "
			+ "TABLE_SELECTOR, VALUE, START_DATE_TIME, DELETE_FLAG, "
			+ "MODEL_RUN_ID, VALIDATION, DATA_FLAGS";

		DataCollection dataCollection = new DataCollection();

		q = "select " + attrList + " from CP_COMP_TASKLIST "
		  + "where LOADING_APPLICATION_ID = " + applicationId + " and rownum < 10000 ";

		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_4)
			q = q + " and (FAIL_TIME is null OR "
				+ "SYSDATE - to_date("
					+ "to_char(FAIL_TIME,'dd-mon-yyyy hh24:mi:ss'),"
					+ "'dd-mon-yyyy hh24:mi:ss') >= 1/24)";

//		now add the order by record_num to insure last change wins
		q = q + " order by record_num";

		ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
		RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);
		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				// Extract the info needed from the result set row.
				int recordNum = rs.getInt(1);
				DbKey sdi = DbKey.createDbKey(rs, 2);
				String interval = rs.getString(3);
				String tabsel = rs.getString(4);
				double value = rs.getDouble(5);
				Date timeStamp = db.getFullDate(rs, 6);
				boolean deleted = TextUtil.str2boolean(rs.getString(7));
				int modelRunId = rs.getInt(8);
				if (rs.wasNull())
					modelRunId = Constants.undefinedIntKey;
				String valstr = rs.getString(9);
				char valchar = (rs.wasNull() || valstr.length() == 0) ?
					HdbFlags.HDB_BLANK_VALIDATION : valstr.charAt(0);
				String derivation = rs.getString(10);
				if (rs.wasNull())
					derivation = "";

				// Convert the HDB derivation, validation & deletion flags into
				// a single 32-bit integer.
				int flags = HdbFlags.hdbDerivation2flag(derivation)
					| HdbFlags.hdbValidation2flag(valchar);
				if (!deleted)
					flags |= VarFlags.DB_ADDED;
				else
					flags |= VarFlags.DB_DELETED;


				tasklistRecs.add(
					new TasklistRec(recordNum, sdi, value,
						timeStamp, deleted,
						flags, interval, tabsel, modelRunId));
			}

			if (tasklistRecs.size() == 0)
			{
				// MJM 6.4 RC08 this means tasklist is likely empty.
				((TimeSeriesDb)db).reclaimTasklistSpace(this);
			}

			ArrayList<Integer> badRecs = new ArrayList<Integer>();
			for(TasklistRec rec : tasklistRecs)
			{
				// Find time series if already in data collection.
				// If not construct one and add it.
				CTimeSeries cts = getTimeSeriesFor(dataCollection,
					rec.getSdi(), rec.getInterval(), rec.getTableSelector(),
					rec.getModelRunId(), applicationId);
				if (cts == null)
				{
					badRecs.add(rec.getRecordNum());
					continue;
				}

				// Keep track of record number range seen.
				rrhandle.addRecNum(rec.getRecordNum());

				// Construct timed variable & add it.
				TimedVariable tv = new TimedVariable(rec.getValue());
				tv.setTime(rec.getTimeStamp());
				tv.setFlags(rec.getFlags());

				cts.addSample(tv);

				// Remember which tasklist records are in this timeseries.
				cts.addTaskListRecNum(rec.getRecordNum());
			}

			dataCollection.setTasklistHandle(rrhandle);

			// Delete the bad tasklist recs, 250 at a time.
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
				q = "delete from CP_COMP_TASKLIST "
					+ "where RECORD_NUM IN (" + inList.toString() + ")";
				doModify(q);
//				commit();
				for(int i=0; i<x; i++)
					badRecs.remove(0);
			}

			return dataCollection;
		}
		catch(SQLException ex)
		{
			System.err.println("Error reading new data: " + ex);
			ex.printStackTrace();
			throw new DbIoException("Error reading new data: " + ex);
		}
	}

	/**
	 * Get the CTimeSeries object for the passed parameters. If a match
	 * is already in dataCollection, return it. Else construct a new one
	 * and add it to dataCollection.
	 * @param dataCollection the DataCollection
	 * @param sdi the site-datatype-id
	 * @param interval the interval code
	 * @param tabsel the table selector
	 * @param modelRunId the model run id.
	 * @throws NoSuchObjectException
	 */
	private CTimeSeries getTimeSeriesFor(DataCollection dataCollection,
		DbKey sdi, String interval, String tabsel, int modelRunId, DbKey appId)
		throws DbIoException
	{
		CTimeSeries cts =
			dataCollection.getTimeSeries(sdi, interval, tabsel, modelRunId);

		if (cts == null)
		{
			cts = new CTimeSeries(sdi, interval, tabsel);
			cts.setModelRunId(modelRunId);
			if (modelRunId != Constants.undefinedIntKey
			 && cts.getModelId() == Constants.undefinedIntKey)
				cts.setModelId(findModelId(modelRunId));

			try
			{
				DbKey tsKey = lookupTsId(sdi, interval, tabsel, cts.getModelId());
				TimeSeriesIdentifier tsid = getTimeSeriesIdentifier(tsKey);
				cts.setTimeSeriesIdentifier(tsid);
				cts.setUnitsAbbr(tsid.getStorageUnits());
				((TimeSeriesDb)db).fillDependentCompIds(cts, appId, this);
			}
			catch(NoSuchObjectException ex)
			{
				warning("Error reading TSID in getTimeSeriesFor: "
					+ "sdi=" + sdi + ", interval=" + interval
					+ ", tabsel=" + tabsel + ", modelId=" + cts.getModelId()
					+ ": " + ex);
				return null;
			}

			try { dataCollection.addTimeSeries(cts); }
			catch(decodes.tsdb.DuplicateTimeSeriesException ex)
			{ // won't happen -- already verified it's not there.
			}
		}
		return cts;
	}

	/**
	 * Returns the modelID for a given modelRunId.
	 * @param modelRunId the model run ID
	 * @return the modelID for a given modelRunId, or -1 if not found.
	 */
	private int findModelId(int modelRunId)
		throws DbIoException
	{
		String q = "select MODEL_ID from REF_MODEL_RUN "
			+ "where MODEL_RUN_ID = " + modelRunId;
		try
		{
			ResultSet rs = doQuery2(q);
			if (rs.next())
			{
				int r = rs.getInt(1);
				if (!rs.wasNull())
					return r;
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("findModelId: " + ex);
		}
		return Constants.undefinedIntKey;
	}

	private DbKey lookupTsIdKey(HdbTsId tsid)
		throws DbIoException
	{
		// Now we have a valid SDI. Lookup the CP_TS_ID
		String q = "select ts_id from cp_ts_id "
			+ "where site_datatype_id = " + tsid.getSdi()
			+ " and lower(interval) = " + sqlString(tsid.getInterval().toLowerCase())
			+ " and table_selector = " + sqlString(tsid.getTableSelector())
			+ " and model_id = " + tsid.modelId;
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				DbKey tsId = DbKey.createDbKey(rs, 1);
				tsid.setKey(tsId);
				return tsId;
			}
			return Constants.undefinedId;
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in '" + q + "': " + ex);
		}
	}

	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(String uniqueString) throws DbIoException, NoSuchObjectException 
	{
		FailableResult<TimeSeriesIdentifier,TsdbException> ret = findTimeSeriesIdentifier(uniqueString);
		if (ret.isSuccess())
		{
			return ret.getSuccess();
		}
		else
		{
			return ExceptionHelpers.throwDbIoNoSuchObject(ret.getFailure());
		}
	}

	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(DbKey key) throws DbIoException, NoSuchObjectException
	{
		FailableResult<TimeSeriesIdentifier,TsdbException> ret = findTimeSeriesIdentifier(key);
		if (ret.isSuccess())
		{
			return ret.getSuccess();
		}
		else
		{
			return ExceptionHelpers.throwDbIoNoSuchObject(ret.getFailure());
		}
	}

}
