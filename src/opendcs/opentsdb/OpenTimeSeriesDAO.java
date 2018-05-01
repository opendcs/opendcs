/*
* $Id$
*
* Copyright 2017 Cove Software, LLC. All Rights Reserved.
* 
* $Log$
*
*/
package opendcs.opentsdb;

import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;

import decodes.cwms.CwmsFlags;
import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.IntervalList;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.util.TSUtil;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DbObjectCache;
import opendcs.opentsdb.Interval;
import opendcs.opentsdb.OffsetErrorAction;

public class OpenTimeSeriesDAO
	extends DaoBase
	implements TimeSeriesDAI
{
	// Open TSDB Uses CWMS 6-part Time Series Identifiers
	protected static DbObjectCache<TimeSeriesIdentifier> cache = 
		new DbObjectCache<TimeSeriesIdentifier>(2 * 60 * 60 * 1000L, false); // 2 hr cache
	protected SiteDAI siteDAO = null;
	protected DataTypeDAI dataTypeDAO = null;
	protected Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	protected NumberFormat suffixFmt = NumberFormat.getIntegerInstance();
	private static final String ts_columns = "sample_time, ts_value, flags, source_id";
	private long lastCacheReload = 0L;
	private DbKey appId = DbKey.NullKey;
	private String appModule = null;
	
	/** data sources are immutable in the database so no need to refresh them. */
	private static HashMap<String, TsDataSource> key2ds = new HashMap<String, TsDataSource>();

	
	public static final long MSEC_PER_UTC_DAY = 24 * 3600 * 1000L;

	
	public static final String ts_spec_columns = 
		"TS_ID, SITE_ID, DATATYPE_ID, STATISTICS_CODE, INTERVAL_ID, " +
		"DURATION_ID, TS_VERSION, ACTIVE_FLAG, STORAGE_UNITS, " +
		"STORAGE_TABLE, STORAGE_TYPE, " +
		"MODIFY_TIME, DESCRIPTION, UTC_OFFSET, ALLOW_DST_OFFSET_VARIATION, " +
		"OFFSET_ERROR_ACTION";

	public OpenTimeSeriesDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "OpenTimeSeriesDAO");
		siteDAO = tsdb.makeSiteDAO();
		dataTypeDAO = tsdb.makeDataTypeDAO();
		suffixFmt.setMinimumIntegerDigits(4);
		suffixFmt.setGroupingUsed(false);
		appId = tsdb.getAppId();
	}

	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(String uniqueString)
		throws DbIoException, NoSuchObjectException
	{
		CwmsTsId tsid = (CwmsTsId)cache.getByUniqueName(uniqueString);
		if (tsid != null)
			return tsid;
			
		tsid = new CwmsTsId();
		tsid.setUniqueString(uniqueString);
		DbKey siteId = siteDAO.lookupSiteID(tsid.getSiteName());
		if (siteId.isNull())
			throw new NoSuchObjectException("No such site '" + tsid.getSiteName() + "'");
		DbKey dataTypeId = tsid.getDataTypeId();
		if (siteId.isNull())
			throw new NoSuchObjectException("No such data type for '" + uniqueString + "'");

		Interval interval = IntervalList.instance().getByName(tsid.getInterval());
		if (interval == null)
			throw new NoSuchObjectException("No such interval '" + tsid.getInterval() + "'");
		Interval duration = IntervalList.instance().getByName(tsid.getDuration());
		if (duration == null)
			throw new NoSuchObjectException("No such duration '" + tsid.getDuration() + "'");

		String q = "select ts_id from ts_spec where "
			+ "SITE_ID = " + siteId
			+ " and DATATYPE_ID = " + dataTypeId
			+ " and lower(STATISTICS_CODE) = " + sqlString(tsid.getStatisticsCode().toLowerCase())
			+ " and INTERVAL_ID = " + interval.getKey()
			+ " and DURATION_ID = " + duration.getKey()
			+ " and lower(TS_VERSION) = " + sqlString(tsid.getVersion().toLowerCase());
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
				return getTimeSeriesIdentifier(DbKey.createDbKey(rs, 1));
			else
				throw new NoSuchObjectException("No Time Series matching '" + uniqueString + "'");
		}
		catch (SQLException ex)
		{
			throw new DbIoException(ex.getMessage());
		}
	}

	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(DbKey key)
		throws DbIoException, NoSuchObjectException
	{
		TimeSeriesIdentifier ret = cache.getByKey(key);
		
		if (ret != null)
		{
			debug3("Received ts_code=" + key + ", id='" + ret.getUniqueString() + "' from cache.");
			return ret;
		}
		else
		{
			debug3("Not in cache ts_code=" + key);
		}
	
		String q = "SELECT " + ts_spec_columns + " from TS_SPEC "
			+ " where ts_id = " + key;
		
		try
		{
			long now = System.currentTimeMillis();
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				ret = rs2TsId(rs, true);
				ret.setReadTime(now);
				cache.put(ret);
				return ret;
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
			throw new DbIoException(
				"Error looking up TS Info for TS_CODE=" + key + ": " + ex);
		}
		throw new NoSuchObjectException("No time-series with ts_code=" + key);
	}
	
	/**
	 * Given a result set with columns specified by ts_spec_columns,
	 * parse a complete CwmsTsId.
	 * @param rs The result set
	 * @param createDataType true if we can create the data type record (CWMS only)
	 * @return the CwmsTsId
	 * @throws SQLException
	 * @throws DbIoException
	 * @throws NoSuchObjectException
	 */
	private CwmsTsId rs2TsId(ResultSet rs, boolean createDataType)
		throws SQLException, DbIoException, NoSuchObjectException
	{
		DbKey tsKey = DbKey.createDbKey(rs, 1);
		DbKey siteId = DbKey.createDbKey(rs, 2);
		DbKey dataTypeId = DbKey.createDbKey(rs, 3);
		String statCode = rs.getString(4);
		DbKey intervalId = DbKey.createDbKey(rs, 5);
		DbKey durationId = DbKey.createDbKey(rs, 6);
		String version = rs.getString(7);
		boolean active = TextUtil.str2boolean(rs.getString(8));
		String storageUnitsAbbr = rs.getString(9);
		int storageTable = rs.getInt(10);
		String storageType = rs.getString(11);
		Date lastModified = db.getFullDate(rs, 12);
		String description = rs.getString(13);
		int utcOffset = rs.getInt(14);
		boolean allowDstOffsetVariation = TextUtil.str2boolean(rs.getString(15));
		String s = rs.getString(16);
		OffsetErrorAction offsetErrorAction = null;
		if (s != null && !rs.wasNull())
		{
			try { offsetErrorAction = OffsetErrorAction.valueOf(s.toUpperCase()); }
			catch(Exception ex)
			{
				warning("Invalid value for offsetErrorAction '" + s + "'");
				offsetErrorAction = null;
			}
		}
		
		Site site = siteDAO.getSiteById(siteId);
		DataType dataType = DataType.getDataType(dataTypeId);
		if (dataType == null)
			throw new NoSuchObjectException("Invalid DataType ID = " + dataTypeId);
		Interval interval = IntervalList.instance().getById(intervalId);
		if (interval == null)
			throw new NoSuchObjectException("Invalid interval ID = " + intervalId);
		Interval duration = IntervalList.instance().getById(durationId);
		if (duration == null)
			throw new NoSuchObjectException("Invalid duration ID = " + durationId);
		
		String path = site.getPreferredName().getNameValue() + "."
			+ dataType.getCode() + "."
			+ statCode + "."
			+ interval.getName() + "."
			+ duration.getName() + "."
			+ version;
		
		CwmsTsId ret = new CwmsTsId(tsKey, path, dataType, 
			description, false, utcOffset, storageUnitsAbbr);

		ret.setSite(site);
		ret.setSiteDisplayName(site.getPublicName());
		ret.setDescription(description);
		ret.setActive(active);
		ret.setLastModified(lastModified);
		ret.setStorageTable(storageTable);
		ret.setStorageType(
			storageType == null || storageType.length()==0 ? 'N' :
				storageType.toUpperCase().charAt(0));
		ret.setAllowDstOffsetVariation(allowDstOffsetVariation);
		ret.setOffsetErrorAction(offsetErrorAction);

		return ret;
	}


	@Override
	public void close()
	{
		siteDAO.close();
		dataTypeDAO.close();

		super.close();
	}

	@Override
	public void fillTimeSeriesMetadata(CTimeSeries ts) throws DbIoException,
		BadTimeSeriesException
	{
		debug3("fillTimeSeriesMetadata " + ts.getBriefDescription());
		try
		{
			DbKey ts_code = ts.getSDI();
			CwmsTsId tsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
			if (tsid == null)
			{
				tsid = (CwmsTsId)getTimeSeriesIdentifier(ts_code);
				ts.setTimeSeriesIdentifier(tsid);
			}
			ts.setInterval(tsid.getInterval());
			ts.setBriefDescription(tsid.getDataType().getCode() + " @ "
				+ tsid.getSiteName());
			// Set table selector to ParamType.Duration.Version so it will match
			ts.setTableSelector(tsid.getParamType() + "." +
				tsid.getDuration() + "." + tsid.getVersion());

			String existingUnits = ts.getUnitsAbbr();
			if (existingUnits == null || existingUnits.isEmpty() || existingUnits.equalsIgnoreCase("unknown"))			
				ts.setUnitsAbbr(tsid.getStorageUnits());
		}
		catch(NoSuchObjectException ex)
		{
			warning("Error expanding SDI: " + ex);
			ts.setDisplayName("unknownSite:unknownType-unknownIntv");
			ts.setUnitsAbbr("EU??");
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
		CwmsTsId ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		if (ctsid == null)
		{
			fillTimeSeriesMetadata(ts);
			ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		}

		UnitConverter unitConverter = null;
		if (ctsid.getStorageType() == 'N'
		 && ts.getUnitsAbbr() != null
		 && !ts.getUnitsAbbr().equalsIgnoreCase(ctsid.getStorageUnits()))
			unitConverter = Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(ctsid.getStorageUnits()),
					EngineeringUnit.getEngineeringUnit(ts.getUnitsAbbr()));

		String tableName = makeDataTableName(ctsid);
		String q = "select " + ts_columns + " from " + tableName
			+ " where ts_id = " + ctsid.getKey();
		if (from != null)
			q = q + " and sample_time " + (include_lower ? " >= " : " > ")
				+ db.sqlDate(from);
		if (until != null)
			q = q + " and sample_time " + (include_upper ? " <= " : " < ")
			+ db.sqlDate(until);
		
		try
		{
			ResultSet rs = doQuery(q);
			int n = 0;
			while (rs != null && rs.next())
			{
				TimedVariable tv = rs2tv(rs, ctsid, unitConverter);

				// For computation processor, we never want to overwrite data
				// we already have. For a report generator, we DO.
				Date d = tv.getTime();
				if (!overwriteExisting
				 && ts.findWithin(d.getTime() / 1000L, 10) != null)
					continue;

				ts.addSample(tv);
				n++;
			}
			return n;
		}
		catch (SQLException ex)
		{
			String msg = "Error getting data for time series="
					+ ctsid.getUniqueName() + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public int fillTimeSeries(CTimeSeries ts, Collection<Date> queryTimes)
		throws DbIoException, BadTimeSeriesException
	{
		if (queryTimes.size() == 0)
			return 0;

		CwmsTsId ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		if (ctsid == null)
		{
			fillTimeSeriesMetadata(ts);
			ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		}
		
		UnitConverter unitConverter = null;
		if (ctsid.getStorageType() == 'N'
		 && ts.getUnitsAbbr() != null
		 && !ts.getUnitsAbbr().equalsIgnoreCase(ctsid.getStorageUnits()))
			unitConverter = Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(ctsid.getStorageUnits()),
					EngineeringUnit.getEngineeringUnit(ts.getUnitsAbbr()));

		String tableName = makeDataTableName(ctsid);
		StringBuilder q = new StringBuilder("select " + ts_columns + " from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and sample_time in (");
		int n = queryTimes.size();
		int i = 0;
		for(Date d : queryTimes)
		{
			q.append(db.sqlDate(d));
			if (++i < n)
				q.append(", ");
			else
				q.append(")");
		}

		try
		{
			ResultSet rs = doQuery(q.toString());
			n = 0;
			while (rs != null && rs.next())
			{
				TimedVariable tv = rs2tv(rs, ctsid, unitConverter);

				Date d = tv.getTime();
				if (ts.findWithin(d.getTime() / 1000L, 10) != null)
					continue;

				ts.addSample(tv);
				n++;
			}
			return n;
		}
		catch (SQLException ex)
		{
			String msg = "Error getting data for time series="
					+ ctsid.getUniqueName() + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public TimedVariable getPreviousValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		CwmsTsId ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		if (ctsid == null)
		{
			fillTimeSeriesMetadata(ts);
			ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		}
		
		UnitConverter unitConverter = null;
		if (ctsid.getStorageType() == 'N'
		 && ts.getUnitsAbbr() != null
		 && !ts.getUnitsAbbr().equalsIgnoreCase(ctsid.getStorageUnits()))
			unitConverter = Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(ctsid.getStorageUnits()),
					EngineeringUnit.getEngineeringUnit(ts.getUnitsAbbr()));

		String tableName = makeDataTableName(ctsid);
		String q = "select " + ts_columns + " from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and sample_time = "
			+ "(select max(sample_time) from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and sampleTime < " + db.sqlDate(refTime) + " )";

		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				TimedVariable tv = rs2tv(rs, ctsid, unitConverter);
				if (ts.findWithin(tv.getTime(), 10) == null)
					ts.addSample(tv);
				return tv;
			}
			else // there is no earlier value
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "getPreviousValue() Error in query '"
					+ q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public TimedVariable getNextValue(CTimeSeries ts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		CwmsTsId ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		if (ctsid == null)
		{
			fillTimeSeriesMetadata(ts);
			ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		}
		
		UnitConverter unitConverter = null;
		if (ctsid.getStorageType() == 'N'
		 && ts.getUnitsAbbr() != null
		 && !ts.getUnitsAbbr().equalsIgnoreCase(ctsid.getStorageUnits()))
			unitConverter = Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(ctsid.getStorageUnits()),
					EngineeringUnit.getEngineeringUnit(ts.getUnitsAbbr()));

		String tableName = makeDataTableName(ctsid);
		String q = "select " + ts_columns + " from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and sample_time = "
			+ "(select min(sample_time) from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and sampleTime > " + db.sqlDate(refTime) + " )";

		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				TimedVariable tv = rs2tv(rs, ctsid, unitConverter);
				if (ts.findWithin(tv.getTime(), 10) == null)
					ts.addSample(tv);
				return tv;
			}
			else // there is no earlier value
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "getPreviousValue() Error in query '"
					+ q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void saveTimeSeries(CTimeSeries ts) throws DbIoException,
		BadTimeSeriesException
	{
		TimeSeriesIdentifier tsid = ts.getTimeSeriesIdentifier();
		if (tsid == null)
			throw new BadTimeSeriesException("Cannot save time series without TSID.");
		
		CwmsTsId ctsid = (CwmsTsId)tsid;
		debug3("Saving " + tsid.getUniqueString() + ", from cp units="
			+ ts.getUnitsAbbr() + ", required=" + tsid.getStorageUnits());
		TSUtil.convertUnits(ts, tsid.getStorageUnits());
		
		String tableName = makeDataTableName(ctsid);
		
		// First rectify the times in the time series. Depending on the
		// settings we may need to ignore some or adjust the times.
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if ((VarFlags.mustWrite(tv) || VarFlags.mustDelete(tv)) // marked for modification
			 && !checkSampleTime(tv, (CwmsTsId)tsid))               // settings say to ignore.
				tv.setFlags(tv.getFlags() & ~(VarFlags.TO_DELETE|VarFlags.TO_WRITE));
		}
		
		int numNew = 0;
		int numUpdated = 0;
		int numDeleted = 0;
		int numProtected = 0;
		int numNoOverwrite = 0;
		int numErrors = 0;

		// Get list of times that are marked for write or delete.
		ArrayList<Date> times = new ArrayList<Date>();
		for (int i = 0; i < ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if (VarFlags.mustWrite(tv) || VarFlags.mustDelete(tv))
				times.add(tv.getTime());
		}
		if (times.size() == 0)
		{
			debug3(" No times marked for save or delete.");
			return;
		}

		// Determine if samples already exist at these time stamps.
		CTimeSeries alreadyInDb = 
			new CTimeSeries(ts.getSDI(), ts.getInterval(), ts.getTableSelector());
		fillTimeSeries(alreadyInDb, times);
		alreadyInDb.sort();
		
		DbKey daoSourceId = getTsDataSource().getAppId();
		Date now = new Date();

		// Go through samples in the time series I am supposed to write.
		// For each flagged sample, see if there is already one in the database.
		// Difference between CWMS and OpenTSDB: OpenTSDB stores flags exactly
		// as they are defined in CwmsFlags.java. CWMS shifts them around on
		// read and write.
		for (int idx = 0; idx < ts.size(); idx++)
		{
			TimedVariable tv2write = ts.sampleAt(idx);
			DbKey tvSourceId = tv2write.getSourceId();
			if (DbKey.isNull(tvSourceId))
				tvSourceId = daoSourceId;
			
			TimedVariable dbTv = alreadyInDb.findWithin(tv2write.getTime(), 5);

			String q = "";
			try
			{
				if (dbTv == null)
				{
					if (VarFlags.mustWrite(tv2write))
					{
						// New value!
						int flags = tv2write.getFlags()
							& ~(CwmsFlags.RESERVED_4_VAR | CwmsFlags.RESERVED_4_COMP);
						q = "insert into " 
							+ tableName + "(TS_ID, SAMPLE_TIME, TS_VALUE, FLAGS, SOURCE_ID, DATA_ENTRY_TIME) "
							+ " values("
							+ tsid.getKey() 
							+ ", " + db.sqlDate(tv2write.getTime())
							+ ", " + (ctsid.getStorageType() == 'N' ? tv2write.getDoubleValue()
							          : sqlString(tv2write.getStringValue()))
							+ ", " + flags 
							+ ", " + tvSourceId 
							+ ", " + db.sqlDate(now) + ")";
						doModify(q);
						numNew++;
					}
					// else if mustDelete do nothing. There is no DB value.
				}
				// If existing value is protected, we may not change it!
				else if ((dbTv.getFlags() & CwmsFlags.PROTECTED) != 0)
				{
					warning("DB Value for " + tsid.getUniqueString() + " at time " + 
						db.getLogDateFormat().format(dbTv.getTime()) + " is protected. "
						+ " Cannot modify!");
					numProtected++;
					continue;
				}
				else if (!VarFlags.isNoOverwrite(tv2write))
					// There is a db value and it is unprotected
					// Also, the NO_OVERWRITE bit means only write if it's new.
				{
					int flags = tv2write.getFlags()
						& ~(CwmsFlags.RESERVED_4_VAR | CwmsFlags.RESERVED_4_COMP);
					if (VarFlags.mustWrite(tv2write))
					{
						q = "update " + tableName + " set  ts_value = "
							+ (ctsid.getStorageType() == 'N' ? tv2write.getDoubleValue()
								: sqlString(tv2write.getStringValue()))
							+ ", flags = " + flags
							+ ", source_id = " + tvSourceId
							+ ", data_entry_time = " + db.sqlDate(now)
							+ " where ts_id = " + tsid.getKey()
							+ " and sample_time = " + db.sqlDate(dbTv.getTime());
						doModify(q);
						numUpdated++;
					}
					else if (VarFlags.mustDelete(tv2write))
					{
						q = "delete from " + tableName 
							+ " where ts_id = " + tsid.getKey()
							+ " and sample_time = " + db.sqlDate(dbTv.getTime());
						doModify(q);
						numDeleted++;
					}
				}
				else
					numNoOverwrite++;
			}
			catch(NoConversionException ex)
			{
				warning("Cannot convert " + tv2write + " to number to store in database: "
					+ ex);
			}
			catch(DbIoException ex)
			{
				warning("Error in query '" + q + "': " + ex);
				numErrors++;
			}
		}

		debug3("saveTimeSeries: New=" + numNew + ", updated=" + numUpdated
			+ ", deleted=" + numDeleted + ", protected="
				+ numProtected + ", noOverwrite=" + numNoOverwrite
				+ ", errors=" + numErrors);
	}

	@Override
	public void deleteTimeSeriesRange(CTimeSeries ts, Date from, Date until)
		throws DbIoException, BadTimeSeriesException
	{
		CwmsTsId ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		if (ctsid == null)
		{
			fillTimeSeriesMetadata(ts);
			ctsid = (CwmsTsId)ts.getTimeSeriesIdentifier();
		}
		
		String tableName = makeDataTableName(ctsid);
		String q = "delete from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and flags & " + CwmsFlags.PROTECTED + " = 0 ";
		if (from != null)
			q = q + " and sample_time " + " >= " + db.sqlDate(from);
		if (until != null)
			q = q + " and sample_time " + " <= " + db.sqlDate(until);
		
		doModify(q);
	}

	@Override
	public void deleteTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		CwmsTsId ctsid = (CwmsTsId)tsid;
		String tableName = makeDataTableName(ctsid);
		String q = "delete from " + tableName
			+ " where ts_id = " + ctsid.getKey()
			+ " and flags & " + CwmsFlags.PROTECTED + " = 0 ";
		doModify(q);
		doModify("delete from ts_property where ts_id = " + ctsid.getKey());
		doModify("delete from ts_spec where ts_id = " + ctsid.getKey());
	}

	@Override
	public CTimeSeries makeTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException, NoSuchObjectException
	{
		CTimeSeries ret = new CTimeSeries(tsid.getKey(), tsid.getInterval(), 
			tsid.getTableSelector());
		try { fillTimeSeriesMetadata(ret); }
		catch(BadTimeSeriesException ex) 
		{ throw new NoSuchObjectException(ex.getMessage()); }
		return ret;
	}
	
	/**
	 * Check the sample time against constraints dictated by the interval,
	 * database settings, and time series settings. If settings say to
	 * round the time, the sample time may be adjusted.
	 * This is called before writing a sample to the database.
	 * @param sample the sample to check
	 * @param tsid the time series ID.
	 * @return true if sample OK to save, false if sample should be discarded.
	 */
	private boolean checkSampleTime(TimedVariable sample, CwmsTsId tsid)
		throws DbIoException
	{
		Interval intv = tsid.getIntervalOb();
		
		if (intv == null || intv.getCalMultiplier() == 0)
			return true; // Irregular, so no checking

		OffsetErrorAction offsetErrorAction = tsid.getOffsetErrorAction();
		if (offsetErrorAction == null)
			offsetErrorAction = OpenTsdbSettings.instance().offsetErrorActionEnum;

		// If (ignore) then just return true. Don't bother with checking.
		if (offsetErrorAction == OffsetErrorAction.IGNORE)
			return true;
		
		// Compute a UTC Offset as follows:
		// - UTC Offset must always be less than the interval.
		// - It represents # of seconds to top of previous even UTC interval.
		// - Example interval=5minutes, time=00:18:05, then offset := 185 (3m 5s)

		utcCal.setTime(sample.getTime());
		utcCal.set(Calendar.SECOND, 0);
		switch(intv.getCalConstant())
		{
		case Calendar.MINUTE:
			utcCal.set(Calendar.MINUTE, 
				(utcCal.get(Calendar.MINUTE) / intv.getCalMultiplier()) * intv.getCalMultiplier());
			break;
		case Calendar.HOUR_OF_DAY:	// truncate to top of (hour*mult)
			utcCal.set(Calendar.MINUTE, 0);
			utcCal.set(Calendar.HOUR_OF_DAY, 
				(utcCal.get(Calendar.HOUR_OF_DAY) / intv.getCalMultiplier()) * intv.getCalMultiplier());
			break;
		case Calendar.DAY_OF_MONTH: // truncate to top of (day*mult)
			utcCal.set(Calendar.HOUR_OF_DAY, 0);
			utcCal.set(Calendar.MINUTE, 0);
			// Now truncate back, using number of days since epoch
			utcCal.setTimeInMillis(
				(daysSinceEpoch(utcCal.getTimeInMillis()) / intv.getCalMultiplier())
					* intv.getCalMultiplier() * MSEC_PER_UTC_DAY);
			break;
		case Calendar.WEEK_OF_YEAR:
			utcCal.set(Calendar.HOUR_OF_DAY, 0);
			utcCal.set(Calendar.MINUTE, 0);
			utcCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
			utcCal.set(Calendar.WEEK_OF_YEAR, 
				(utcCal.get(Calendar.WEEK_OF_YEAR) / intv.getCalMultiplier()) * intv.getCalMultiplier());
			break;
		case Calendar.MONTH:
			utcCal.set(Calendar.HOUR_OF_DAY, 0);
			utcCal.set(Calendar.MINUTE, 0);
			utcCal.set(Calendar.DAY_OF_MONTH, 1);
			utcCal.set(Calendar.MONTH, 
				(utcCal.get(Calendar.MONTH) / intv.getCalMultiplier()) * intv.getCalMultiplier());
			break;
		case Calendar.YEAR:
			utcCal.set(Calendar.HOUR_OF_DAY, 0);
			utcCal.set(Calendar.MINUTE, 0);
			utcCal.set(Calendar.DAY_OF_MONTH, 1);
			utcCal.set(Calendar.MONTH, Calendar.JANUARY);
			utcCal.set(Calendar.YEAR, 
				(utcCal.get(Calendar.YEAR) / intv.getCalMultiplier()) * intv.getCalMultiplier());
			break;
		}
		
		// Compute offset in seconds. Will always be positive because we truncate utcCal backward.
		int offset = (int)((sample.getTime().getTime() - utcCal.getTimeInMillis()) / 1000L);
		
		// if (utcOffset unassigned) update ts_spec and write offset to data
		// then return true.
		if (tsid.getIntervalUtcOffset() == -1)
		{
			String q = "update ts_spec set utc_offset = " + offset;
			doModify(q);
			tsid.setIntervalUtcOffset(offset);
			return true;
		}
		
		// Else check against the stored utc offset in seconds.
		int offsetError = offset - tsid.getIntervalUtcOffset();
		boolean violation = (offsetError == 0);
		
		if (!violation)
			return true; // UTC Offsets are exactly equal!
		
		
		if (intv.getCalConstant() == Calendar.MINUTE
		 || (intv.getCalConstant() == Calendar.HOUR_OF_DAY && intv.getCalMultiplier() == 1))
		{
			violation = true;
		}
		else
		{
			boolean allowDstVariation = tsid.isAllowDstOffsetVariation();
			
			if (allowDstVariation &&
				((intv.getCalConstant() == Calendar.HOUR_OF_DAY && intv.getCalMultiplier() > 1)
			  || intv.getCalConstant() == Calendar.DAY_OF_MONTH
			  || intv.getCalConstant() == Calendar.WEEK_OF_YEAR))
			{
				if (offsetError == -3600 || offsetError == 3600)
				{
					violation = false;
				}
			}			
			else if (intv.getCalConstant() == Calendar.MONTH)
			{
				// In monthly value, offset is seconds since start of month.
				// It may span a DST change.
				if (allowDstVariation &&
					(offsetError == -3600 || offsetError == 3600))
					violation = false;

				//TODO Consider use case where end of month is stored.
				// In march offset is 30d (31-1). In feb this is 27.
				// What if stored offset is 30d but this is 27d?
				// What if stored offset is 27d but this is 30d?
				// So (I think) offsetError can be +/- 1d, 2d, or 3d.
				// Also if allowDstVariation, it may also be +/- 1h.
			}
			else if (intv.getCalConstant() == Calendar.YEAR)
			{
				// max offsetError is 1day * (mult/4) + 1 (i.e. as much as 1 day for every 4 years)
				// DST variation may apply because rules occasionally change as to
				// when DST starts/stops in the year. So if allowDstVariation, it may also be +/- 1h.
				for(int x = 1; x <= intv.getCalMultiplier()/4 + 1; x++)
				{
					if (offsetError == 3600*24
					 || (allowDstVariation && offsetError == 3600*24 + 3600)
					 || (allowDstVariation && offsetError == 3600*24 - 3600)
					 || offsetError == -3600*24
					 || (allowDstVariation && offsetError == -3600*24 + 3600)
					 || (allowDstVariation && offsetError == -3600*24 - 3600))
					{
						violation = false;
						break;
					}
				}
			}
		}
		if (violation)
		{
			String msg = "Offset violation in time series '" + tsid.getUniqueName()
				+ "' stored offset=" + tsid.getIntervalUtcOffset()
				+ ", computed offset=" + offset 
				+ " at time " + db.getLogDateFormat().format(sample.getTime());
			
			if (offsetErrorAction == OffsetErrorAction.ROUND)
			{
				// utcCal stores the time before the sample time.
				// offset already is # seconds between this and actual sample time.
				Date before = utcCal.getTime();
				utcCal.add(intv.getCalConstant(), intv.getCalMultiplier());
				Date after = utcCal.getTime();
				int afterOffset = (int)
					((utcCal.getTimeInMillis() - sample.getTime().getTime()) / 1000L);
				sample.setTime(offset < afterOffset ? before : after);
				violation = false;
				warning(msg + " Rounded to " + db.getLogDateFormat().format(sample.getTime()));
			}
			else if (offsetErrorAction == OffsetErrorAction.REJECT)
			{
				warning(msg + " -- REJECTING.");
				return false;
			}
		}
		return !violation;
	}
	
	private int daysSinceEpoch(long msecTime)
	{
		return (int)(msecTime /= MSEC_PER_UTC_DAY);
	}
	
	public String makeDataTableName(CwmsTsId tsid)
		throws DbIoException
	{
		int tableNum = tsid.getStorageTable();
		if (tableNum == -1)
			tableNum = allocateTable(tsid);
		
		return (tsid.getStorageType() == 'N' ? "TS_NUM_" : "TS_STRING_")
			+ suffixFmt.format(tableNum);
	}
	
	private int allocateTable(CwmsTsId tsid)
		throws DbIoException
	{
		String q = "select * from storage_table_list "
			+ "where storage_type = '" + tsid.getStorageType() + "' "
			+ "and est_annual_values = "
			+ "(select min(est_annual_values) from storage_table_list where storage_type = '"
			+ tsid.getStorageType() + "') order by table_num";
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
			{
				int tableNum = rs.getInt(1);
				int numTsPresent = rs.getInt(3);
				int estAnnualValues = rs.getInt(4);
				numTsPresent++;
				Interval intv = tsid.getIntervalOb();
				estAnnualValues +=
					intv.getCalConstant() == Calendar.YEAR ? 1 :
					intv.getCalConstant() == Calendar.MONTH ? 12/intv.getCalMultiplier() :
					intv.getCalConstant() == Calendar.WEEK_OF_YEAR ? 52/intv.getCalMultiplier() :
					intv.getCalConstant() == Calendar.DAY_OF_MONTH ? 365/intv.getCalMultiplier() :
					intv.getCalConstant() == Calendar.HOUR_OF_DAY ? (365*24)/intv.getCalMultiplier() :
					intv.getCalConstant() == Calendar.MINUTE ? (365*24*60)/intv.getCalMultiplier() :
					1;
				q = "update storage_table_list set num_ts_present = " + numTsPresent
					+ ", est_annual_values = " + estAnnualValues
					+ " where storage_type = '" + tsid.getStorageType() + "' "
					+ " and table_num = " + tableNum;
				doModify(q);
				tsid.setStorageTable(tableNum);
				return tableNum;
			}
			else
				throw new DbIoException("No storage tables available!");
		}
		catch (SQLException ex)
		{
			throw new DbIoException(ex.getMessage());
		}
	}
	
	private TimedVariable rs2tv(ResultSet rs, CwmsTsId ctsid, UnitConverter unitConverter) 
		throws SQLException
	{
		// sample_time, ts_value, flags, source_id
		Date timeStamp = db.getFullDate(rs, 1);
		TimedVariable tv = null;
		if (ctsid.getStorageType() == 'N')
		{
			double d = rs.getDouble(2);
			if (unitConverter != null)
				try { d = unitConverter.convert(d); }
				catch(Exception ex)
				{
					warning("Cannot convert value for " + ctsid.getUniqueName()
						+ " with unit converter from " + unitConverter.getFromAbbr()
						+ " to " + unitConverter.getToAbbr() + ": " + ex);
				}
			tv = new TimedVariable(d);
		}
		else
			tv = new TimedVariable(rs.getString(2));
		tv.setTime(timeStamp);
		tv.setFlags((int)(rs.getLong(3) & 0xffffffffL));
		tv.setSourceId(DbKey.createDbKey(rs, 4));
		return tv;
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> listTimeSeries()
		throws DbIoException
	{
		// MJM 20161025 don't reload more if already done within threshold.
		if (System.currentTimeMillis() - lastCacheReload > cacheReloadMS)
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
			lastCacheReload = 0L;
		return listTimeSeries();
	}
	
	@Override
	public synchronized void reloadTsIdCache()
		throws DbIoException
	{
		cache.clear();
		String q = "SELECT " + ts_spec_columns + " from TS_SPEC";
		
		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				try
				{
					cache.put(rs2TsId(rs, true));
				}
				catch(NoSuchObjectException ex)
				{
					warning("Cannot create tsid for key=" + rs.getLong(1) + ": " + ex + " -- skipped.");
				}
			}
		}
		catch(Exception ex)
		{
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
			throw new DbIoException(
				"Error reading TS_SPEC table: " + ex);
		}
		lastCacheReload = System.currentTimeMillis();
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
		CwmsTsId ctsid = (CwmsTsId)tsid;
		DbKey siteId = Constants.undefinedId;
		Site site = tsid.getSite();
		if (site != null)
			siteId = site.getId();
		else
		{
			siteId = siteDAO.lookupSiteID(tsid.getSiteName());
			if (siteId.isNull())
				throw new NoSuchObjectException("No such site for tsid '" + tsid + "'");
			tsid.setSite(siteDAO.getSiteById(siteId));
		}
		DataType dataType = ctsid.getDataType();
		DbKey dataTypeId = dataType.getId();
		if (dataTypeId.isNull())
			throw new NoSuchObjectException("No such datatype for tsid '" + tsid + "'");
		Interval interval = ctsid.getIntervalOb();
		if (interval == null)
			throw new NoSuchObjectException("Invalid interval in tsid '" + tsid + "'");
		DbKey intervalId = interval.getKey();
		Interval duration = IntervalList.instance().getByName(ctsid.getDuration());
		if (duration == null)
			throw new NoSuchObjectException("Invalid duration in tsid '" + tsid + "'");
		DbKey durationId = duration.getKey();
		String storageUnits = ctsid.getStorageUnits();
		if (storageUnits == null)
		{
			decodes.db.Database db = decodes.db.Database.getDb();
			String pgName = OpenTsdbSettings.instance().storagePresentationGroup;
			PresentationGroup dbpg = db.presentationGroupList.find(pgName);
			if (dbpg == null)
				throw new DbIoException("No such storagePresentationGroup '"
					+ pgName + "'");
			DataPresentation dp = dbpg.findDataPresentation(dataType);
			if (dp == null)
			{
				int idx = dataType.getCode().indexOf('-');
				String baseCode = dataType.getCode().substring(0,idx);
				if (idx != -1)
					dp = dbpg.findDataPresentation(
						DataType.getDataType(dataType.getStandard(), 
							baseCode));
				if (dp == null)
					throw new NoSuchObjectException("No entry in presentation group '"
						+ pgName + "' for datatype " + dataType + ", or its subtype.");
			}
			storageUnits = dp.getUnitsAbbr();
			ctsid.setStorageUnits(storageUnits);
		}
		
		
		DbKey key = this.getKey("TS_SPEC");
		String q = "insert into TS_SPEC(" + ts_spec_columns + ") values ("
			+ key + ", "
			+ siteId + ", "
			+ dataTypeId + ", "
			+ sqlString(ctsid.getStatisticsCode()) + ", "
			+ intervalId + ", "
			+ durationId + ", "
			+ sqlString(ctsid.getVersion()) + ", "
			+ sqlBoolean(ctsid.isActive()) + ", "
			+ sqlString(storageUnits) + ", "
			+ "-1, " // storage table to be allocated on first write
			+ "'" + ctsid.getStorageType() + "', "
			+ db.sqlDate(new Date()) + ", "
			+ sqlString(ctsid.getDescription()) + ", "
			+ "-1, " // UTC_OFFSET set on first write
			+ sqlBoolean(ctsid.isAllowDstOffsetVariation()) + ", "
			+ sqlString(ctsid.getOffsetErrorAction().toString())
			+ ")";
		doModify(q);
		return key;
	}
	
	@Override
	public void setAppModule(String module)
	{
		this.appModule = module;
	}
	
	public TsDataSource getTsDataSource()
		throws DbIoException
	
	{
		if (DbKey.isNull(appId))
			throw new DbIoException("Cannot retrieve data source record when appId is null.");
		
		String key = appId.toString();
		if (appModule != null)
			key = key + "-" + appModule;
		TsDataSource ret = key2ds.get(key);
		if (ret == null)
		{
			String q = "select SOURCE_ID from TSDB_DATA_SOURCE where LOADING_APPLICATION_ID = " + appId 
				+ " and MODULE " + (appModule == null ? "IS NULL" : ("= " + sqlString(appModule)));
			ResultSet rs = doQuery2(q);
			try
			{
				if (rs != null && rs.next())
				{
					DbKey sourceId = DbKey.createDbKey(rs, 1);
					ret = new TsDataSource(sourceId, appId, appModule);
					key2ds.put(key, ret);
				}
				else
				{
					DbKey sourceId = this.getKey("TSDB_DATA_SOURCE");
					ret = new TsDataSource(sourceId, appId, appModule);
					q = "insert into TSDB_DATA_SOURCE(SOURCE_ID, LOADING_APPLICATION_ID, MODULE) "
						+ " values(" + sourceId + ", " + appId + ", " + sqlString(appModule) + ")";
					doModify(q);
					key2ds.put(key, ret);
				}
			}
			catch(SQLException ex)
			{
				throw new DbIoException("Exception in query '" + q + "': " + ex);
			}
		}
		return ret;
	}
}
