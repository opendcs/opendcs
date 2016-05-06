/**
 * $Id$
 * 
 * $Log$
 * Revision 1.11  2016/01/27 21:41:04  mmaloney
 * Rm references to CWMS_V_TSV_DQU. Always use CWMS_V_TSV and then do
 * our own conversions if necessary.
 *
 * Revision 1.10  2015/09/10 21:18:29  mmaloney
 * Development on Screening
 *
 * Revision 1.9  2015/08/31 00:34:50  mmaloney
 * Log messages with flag values should print in hex.
 *
 * Revision 1.8  2015/07/14 17:52:18  mmaloney
 * Catch error on TS creation.
 *
 * Revision 1.7  2015/01/16 16:11:04  mmaloney
 * RC01
 *
 * Revision 1.6  2014/08/29 18:24:50  mmaloney
 * 6.1 Schema Mods
 *
 * Revision 1.5  2014/08/15 16:23:36  mmaloney
 * Get rid of error-prone intermediate variable for unitsAbbr. Always use get method.
 *
 * Revision 1.4  2014/07/10 17:06:52  mmaloney
 * Always include office code in query when loading tsid cache.
 * Even the 5.1 will have this field, and it should be populated correctly.
 *
 * Revision 1.3  2014/07/03 12:43:23  mmaloney
 * debug improvements.
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.io.PrintStream;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import cwmsdb.CwmsTsJdbc;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesHelper;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DbObjectCache;

public class CwmsTimeSeriesDAO 
	extends DaoBase 
	implements TimeSeriesDAI
{
	protected static DbObjectCache<TimeSeriesIdentifier> cache = 
		new DbObjectCache<TimeSeriesIdentifier>(60 * 60 * 1000L, false);
	private static boolean firstCall = true;
	protected SiteDAI siteDAO = null;
	protected DataTypeDAI dataTypeDAO = null;
	private String dbOfficeId = null;
	private static boolean noUnitConv = false;

	protected CwmsTimeSeriesDAO(DatabaseConnectionOwner tsdb, String dbOfficeId)
	{
		super(tsdb, "CwmsTimeSeriesDAO");
		siteDAO = tsdb.makeSiteDAO();
		dataTypeDAO = tsdb.makeDataTypeDAO();
		this.dbOfficeId = dbOfficeId;
	}

	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(DbKey key)
		throws DbIoException, NoSuchObjectException
	{
		if (firstCall)
		{
			reloadTsIdCache();
			firstCall = false;
		}
		CwmsTsId ret = (CwmsTsId)cache.getByKey(key);
		
		if (ret != null)
		{
			debug3("Received ts_code=" + key + ", id='" + ret.getUniqueString() + "' from cache.");
			return ret;
		}
		else
		{
			debug3("Not in cache ts_code=" + key);
		}
	
		String q = "SELECT a.CWMS_TS_ID, a.VERSION_FLAG, a.INTERVAL_UTC_OFFSET, "
			+ "a.UNIT_ID, a.PARAMETER_ID, c.PUBLIC_NAME, a.TS_CODE, a.LOCATION_CODE, "
			+ "a.LOCATION_ID "
			+ "FROM CWMS_V_TS_ID a, CWMS_V_LOC c "
			+ " WHERE a.TS_CODE = " + key
			+ " AND a.LOCATION_CODE = c.LOCATION_CODE "
			+ " AND c.UNIT_SYSTEM = 'SI'";
		// Don't need to add DB_OFFICE_ID because TS_CODE is unique.
		
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

	private CwmsTsId rs2TsId(ResultSet rs, boolean createDataType)
		throws SQLException, DbIoException, NoSuchObjectException
	{
		DbKey key = DbKey.createDbKey(rs, 7);
		String desc = rs.getString(1);
		String param = rs.getString(5);
		String publicSiteName = rs.getString(6);
		DataType dt = 
			DataType.getDataType(Constants.datatype_CWMS, param);
		
		CwmsTsId ret = new CwmsTsId(key, rs.getString(1), dt, 
			desc, TextUtil.str2boolean(rs.getString(2)),
			rs.getInt(3), rs.getString(4));
		
		ret.setSiteDisplayName(publicSiteName);
		ret.setDescription(param + " at " + publicSiteName);
		
		DbKey siteId = DbKey.createDbKey(rs, 8);
		Site site = siteDAO.getSiteById(siteId);
		site.addName(new SiteName(site, Constants.snt_CWMS, rs.getString(9)));
		site.setDescription(publicSiteName);
		ret.setSite(site);
	
		if (decodes.db.Database.getDb().getDbIo().getDatabaseType().equalsIgnoreCase("XML"))
			return ret;
		
		
		// TODO: Check to see if lastModified is available in the view. If so, get it
		// and call ret.setLastModified()
		// TODO: Get active flag and call ret.setActive()

	
		if (createDataType && dt.getId() == Constants.undefinedId)
		{
			DataType dbdt = null;
			try { dbdt = dataTypeDAO.lookupDataType(param); }
			catch(NoSuchObjectException ex) { dbdt = null; }
			
			if (dbdt == null 
			 || !dbdt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS))
			{
				dataTypeDAO.writeDataType(dt);
			}
			else // The datatype already exists, add it to the cache.
			{
				decodes.db.Database.getDb().dataTypeSet.add(dbdt);
				ret.setDataType(dbdt);
			}
		}

		return ret;
	}


	@Override
	public TimeSeriesIdentifier getTimeSeriesIdentifier(String uniqueString)
		throws DbIoException, NoSuchObjectException
	{
		if (firstCall)
		{
			reloadTsIdCache();
			firstCall = false;
		}

		int paren = uniqueString.lastIndexOf('(');
		String displayName = null;
debug3("getTimeSeriesIdentifier('" + uniqueString + "')");
		if (paren > 0 && uniqueString.trim().endsWith(")"))
		{
			displayName = uniqueString.substring(paren+1);
			uniqueString = uniqueString.substring(0,  paren);
			int endParen = displayName.indexOf(')');
			if (endParen > 0)
				displayName = displayName.substring(0,  endParen);
debug3("using display name '" + displayName + "', unique str='" + uniqueString + "'");
		}
	
		TimeSeriesIdentifier ret = cache.getByUniqueName(uniqueString);
		if (ret != null)
		{
			if (displayName != null)
			{
				debug3("Setting display name to '" + displayName + "'");
				ret.setDisplayName(displayName);
			}
			return ret;
		}
		else
			debug3("cache does not have '" + uniqueString + "'");

		DbKey ts_code = ts_id2ts_code(uniqueString);
		if (ts_code == Constants.undefinedId)
			throw new NoSuchObjectException("No CWMS Time Series for ID '"
				+ uniqueString + "' and office ID " 
				+ ((CwmsTimeSeriesDb)db).getDbOfficeId());
		
		ret = getTimeSeriesIdentifier(ts_code);
		if (displayName != null)
		{
			debug3("Setting display name to '" + displayName + "'");
			ret.setDisplayName(displayName);
		}
		return ret;
	}
	
	/**
	 * Given a 6-part time-series identifier, return the unique ts_code.
	 * Implicit filtering by office ID is also done.
	 * @param tsid location.parm.parmtype.interval.duration.version
	 * @return ts_code surrogate key, or Constants.undefinedId if not found.
	 */
	private DbKey ts_id2ts_code(String tsid)
	{
		String q = "SELECT TS_CODE FROM CWMS_V_TS_ID "
			+ "WHERE upper(CWMS_TS_ID) = " + sqlString(tsid.toUpperCase());
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			q = q + " AND upper(DB_OFFICE_ID) = " + sqlString(dbOfficeId.toUpperCase());

		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
				return DbKey.createDbKey(rs, 1);
		}
		catch(Exception ex)
		{
			warning("Error looking up TS_CODE for TS_ID '" + tsid
				+ "': " + ex);
		}
		return Constants.undefinedId;
	}


	@Override
	public void close()
	{
		dataTypeDAO.close();
		siteDAO.close();
		super.close();
	}
	
	@Override
	public int fillTimeSeries( CTimeSeries ts, Date from, Date until )
		throws DbIoException, BadTimeSeriesException
	{
		return fillTimeSeries(ts, from, until, true, true, true);
	}

	@Override
	public int fillTimeSeries(CTimeSeries cts, Date from, Date until,
		boolean include_lower, boolean include_upper, boolean overwriteExisting)
		throws DbIoException, BadTimeSeriesException
	{
		String lower_check = include_lower ? " >= " : " > ";
		String upper_check = include_upper ? " <= " : " < ";
		DbKey ts_code = cts.getSDI();
		if (!cts.isExpanded())
		{
			fillTimeSeriesMetadata(cts); // may throw BadTimeSeriesException
			cts.setIsExpanded();
		}
//debug3("After fillTimeSeriesMetadata dn='" + cts.getDisplayName() + "'");
		StringBuffer q = new StringBuffer();
		q.append("SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
			+ " WHERE TS_CODE = " + ts_code);
		
		TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
		if (tsid == null)
		{
			try
			{
				tsid = getTimeSeriesIdentifier(ts_code);
				cts.setTimeSeriesIdentifier(tsid);
//debug3("After re-getting tsid dn='" + cts.getDisplayName() + "'");
			}
			catch(NoSuchObjectException ex)
			{
				warning("fillTimeSeries: " + ex);
			}
		}
		
		// Part of the contract is to honor the units already specified
		// in the CTimeSeries.
		UnitConverter unitConverter = db.makeUnitConverterForRead(cts);

		if (from != null)
			q.append(" AND DATE_TIME " + lower_check + db.sqlDate(from));
		if (until != null)
			q.append(" AND DATE_TIME " + upper_check + db.sqlDate(until));
		q.append(" AND VALUE IS NOT INFINITE");
		try
		{
			ResultSet rs = doQuery(q.toString());
			int numAdded = 0;
			while(rs != null && rs.next())
			{
				TimedVariable tv = null;
				try
				{
					tv = rs2TimedVariable(rs);
					if (tv == null)
						continue;
				}
				catch(SQLException ex)
				{
					String msg = "Error getting data for time series="
						+ cts.getNameString() + ": " + ex;
					warning(msg);
					ex.printStackTrace(Logger.instance().getLogOutput());
					continue;
				}
				
				// For computation processor, we never want to overwrite data
				// we already have. For a report generator, we DO.
				Date d = tv.getTime();
				if (!overwriteExisting
				 && cts.findWithin(d.getTime()/1000L, 10) != null)
					continue;

				if (tv != null)
				{
					if (unitConverter != null)
					{
						try
						{
							tv.setValue(unitConverter.convert(tv.getDoubleValue()));
						}
						catch (Exception ex)
						{
							warning("fillTimeSeries: " + ex);
						}
					}
					cts.addSample(tv);
					numAdded++;
				}
			}
			return numAdded;
		}
		catch(SQLException ex)
		{
			String msg = "Error getting data for time series="
				+ cts.getNameString() + ": " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
	}

	@Override
	public int fillTimeSeries(CTimeSeries cts, Collection<Date> queryTimes)
		throws DbIoException, BadTimeSeriesException
	{
		if (!cts.isExpanded())
		{
			fillTimeSeriesMetadata(cts); // may throw BadTimeSeriesException
			cts.setIsExpanded();
		}

		// Part of the contract is to honor the units already specified
		// in the CTimeSeries.
		UnitConverter unitConverter = db.makeUnitConverterForRead(cts);

//		String qbase = "select DATE_TIME, ROUND(VALUE,8), QUALITY_CODE "
//			+ "FROM CWMS_V_TSV_DQU " 
//			+ "where TS_CODE = " + cts.getSDI()
//			+ " AND UNIT_ID = " + sqlString(cts.getUnitsAbbr()) + " "
//			+ " and DATE_TIME IN (";
		
		
		String qbase = "SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
			+ " WHERE TS_CODE = " + cts.getSDI()
			+ " and DATE_TIME IN (";


		int datesPerQuery = 300;
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
				sb.append(" AND VALUE IS NOT INFINITE");
				String q = qbase + sb.toString();
				sb.setLength(0);
				start = end;
				try
				{
					ResultSet rs = doQuery(q);
					while (rs.next())
					{
						TimedVariable tv = null;
						try
						{
							tv = rs2TimedVariable(rs);
							if (tv == null)
								continue;
						}
						catch(SQLException ex)
						{
							String msg= "Error reading data with query '" + q
								+ "': " + ex;
							warning(msg);
							System.err.println(msg);
							ex.printStackTrace(System.err);
							continue;
						}
						
						if (tv != null)
						{
							// For computation processor, we never want to overwrite data
							// we already have.
							Date d = tv.getTime();
							if (cts.findWithin(d.getTime()/1000L, 10) != null)
								continue;
							
							if (unitConverter != null)
							{
								try
								{
									tv.setValue(unitConverter.convert(tv.getDoubleValue()));
								}
								catch (Exception ex)
								{
									warning("fillTimeSeries: " + ex);
								}
							}
							cts.addSample(tv);
							numAdded++;
						}
						numAdded++;
					}
				}
				catch(SQLException ex)
				{
					String msg= "Error reading data with query '" + q
						+ "': " + ex;
					warning(msg);
					System.err.println(msg);
					ex.printStackTrace(System.err);
				}
			}
			else if (end < size)
				sb.append(", ");
		}
		return numAdded;
	}

	@Override
	public TimedVariable getPreviousValue(CTimeSeries cts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		if (!cts.isExpanded())
		{
			fillTimeSeriesMetadata(cts); // may throw BadTimeSeriesException
			cts.setIsExpanded();
		}

		// Part of the contract is to honor the units already specified
		// in the CTimeSeries.
		UnitConverter unitConverter = db.makeUnitConverterForRead(cts);

		String q = "SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
			+ " WHERE TS_CODE = " + cts.getSDI()
			+ " and DATE_TIME = "
			+   "(select max(date_time) from CWMS_V_TSV "
			+   	"where TS_CODE = " + cts.getSDI()
 			+   	" and date_time < " + db.sqlDate(refTime)
			+ 		" AND VALUE IS NOT NULL "
			+ 		" AND BITAND(QUALITY_CODE, " 
						+ CwmsFlags.QC_MISSING_OR_REJECTED + ") = 0 "
			+	")"
			+ " AND VALUE IS NOT INFINITE";

//		String q = "select DATE_TIME, ROUND(VALUE,8), QUALITY_CODE "
//			+ "FROM CWMS_V_TSV_DQU " 
//			+ "where TS_CODE = " + cts.getSDI()
//			+ " AND UNIT_ID = " + sqlString(cts.getUnitsAbbr())
//			+ " and date_time = "
//			+   "(select max(date_time) from CWMS_V_TSV_DQU "
//			+   	"where TS_CODE = " + cts.getSDI()
// 			+   	" and date_time < " + db.sqlDate(refTime)
//			+ 		" AND VALUE IS NOT NULL "
//			+ 		" AND BITAND(QUALITY_CODE, " 
//						+ CwmsFlags.QC_MISSING_OR_REJECTED + ") = 0 "
//			+	")";
//		q = q + " AND VALUE IS NOT INFINITE";

		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
				return null;  // There is no previous value.
			TimedVariable tv = rs2TimedVariable(rs);
			
			if (tv != null)
			{
				if (unitConverter != null)
				{
					try
					{
						tv.setValue(unitConverter.convert(tv.getDoubleValue()));
					}
					catch (Exception ex)
					{
						warning("getNextValue: Error in unit conversion: " + ex);
					}
				}
				cts.addSample(tv);
			}
			return tv;
		}
		catch(SQLException ex)
		{
			String msg= "Error reading data with query '" + q
				+ "': " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
	}

	@Override
	public TimedVariable getNextValue(CTimeSeries cts, Date refTime)
		throws DbIoException, BadTimeSeriesException
	{
		if (!cts.isExpanded())
		{
			fillTimeSeriesMetadata(cts); // may throw BadTimeSeriesException
			cts.setIsExpanded();
		}

		// Part of the contract is to honor the units already specified
		// in the CTimeSeries.
		UnitConverter unitConverter = db.makeUnitConverterForRead(cts);

		String q = "SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
			+ " WHERE TS_CODE = " + cts.getSDI()
			+ " and DATE_TIME = "
			+   "(select min(date_time) from CWMS_V_TSV "
			+   	"where TS_CODE = " + cts.getSDI()
 			+   	" and date_time > " + db.sqlDate(refTime)
			+ 		" AND VALUE IS NOT NULL "
			+ 		" AND BITAND(QUALITY_CODE, " 
						+ CwmsFlags.QC_MISSING_OR_REJECTED + ") = 0 "
			+	")"
			+ " AND VALUE IS NOT INFINITE";

		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
				return null;  // There is no next value.
			
			TimedVariable tv = rs2TimedVariable(rs);
			if (tv != null)
			{
				if (unitConverter != null)
				{
					try
					{
						tv.setValue(unitConverter.convert(tv.getDoubleValue()));
					}
					catch (Exception ex)
					{
						warning("getNextValue: Error in unit conversion: " + ex);
					}
				}
				cts.addSample(tv);
			}
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
		// NOTE: For CWMS, we don't need separate methods for write/delete
		// because a deleted value is just one with a null value and the 
		// VALIDITY mask set to MISSING.

		TimeSeriesIdentifier tsId = ts.getTimeSeriesIdentifier();
		try
		{
			if (tsId == null)
				tsId = getTimeSeriesIdentifier(ts.getSDI());
		}
		catch(NoSuchObjectException ex)
		{
			throw new BadTimeSeriesException(ex.toString());
		}
		String path = tsId.getUniqueString();

		String unitsAbbr = ts.getUnitsAbbr();
		if (unitsAbbr == null || unitsAbbr.trim().length() == 0 
			|| unitsAbbr.equalsIgnoreCase("unknown"))
		{
			fillTimeSeriesMetadata(ts);
			unitsAbbr = ts.getUnitsAbbr();
		}
		
		// noUnitConv is used for testing by TsImport only
		if (!noUnitConv)
		{
			// Convert to the required 'storage units'.
			debug3("Time Series '" + tsId.getUniqueString() + "' have units '"
				+ unitsAbbr + "' require units '" + tsId.getStorageUnits() + "'");
			if (!unitsAbbr.equalsIgnoreCase(tsId.getStorageUnits()))
			{
				TimeSeriesHelper.convertUnits(ts, tsId.getStorageUnits());
				unitsAbbr = tsId.getStorageUnits();
			}
		}
		else
			debug3("Will write time series " + tsId.getUniqueString() + " with unit " + unitsAbbr);
		
		// We do not yet support versioned data
		java.sql.Timestamp versionDate = null;
		// We will always use overrideProtection false
		boolean overrideProtection = false;
		
		// We use the RMA Java interface to write to DB
		try
		{
			CwmsTsJdbc cwmsTsJdbc = new CwmsTsJdbc(db.getConnection());
			
			ArrayList<Long> msecArray = new ArrayList<Long>();
			ArrayList<Double> valueArray = new ArrayList<Double>();
			ArrayList<Integer> qualArray = new ArrayList<Integer>();

			
			// Handle the normal WRITE/DELETE values.
			fillTsArrays(ts, msecArray, valueArray, qualArray, false);
			int num2write = msecArray.size();
			if (num2write > 0)
			{
				long[] times = new long[num2write];
				double[] values = new double[num2write];
				int[] qualities = new int[num2write];
				for(int idx = 0; idx<num2write; idx++)
				{
					times[idx] = msecArray.get(idx);
					values[idx] = valueArray.get(idx);
					qualities[idx] = qualArray.get(idx);
					
					debug3("sample[" + idx + "] time=" + 
						db.getLogDateFormat().format(new Date(times[idx]))
						+ ", value=" + values[idx] 
						+ ", qual=0x" + Integer.toHexString(qualities[idx]));
				}
				// The "Replace All" store-rule means: 
				//  -- Values at same time stamp I provide will replace existing values
				//  -- Any new values will be written. 
				//  -- Existing values at different time stamps will be left alone.
				debug1(" Calling store for ts_id="
					+ path + ", office='" + dbOfficeId 
					+ "' with " + num2write + " values, units=" + ts.getUnitsAbbr());
				cwmsTsJdbc.store(dbOfficeId, path, ts.getUnitsAbbr(), times, values,
					qualities, num2write, CwmsConstants.REPLACE_ALL, 
					overrideProtection, versionDate);
			}

			// Handle the special values with No OVERWRITE flag:
			fillTsArrays(ts, msecArray, valueArray, qualArray, true);
			num2write = msecArray.size();
			if (num2write > 0)
			{
				long[] times = new long[num2write];
				double[] values = new double[num2write];
				int[] qualities = new int[num2write];
				for(int idx = 0; idx<num2write; idx++)
				{
					times[idx] = msecArray.get(idx);
					values[idx] = valueArray.get(idx);
					qualities[idx] = qualArray.get(idx);
					
					debug3("sample[" + idx + "] time=" 
						+ db.getLogDateFormat().format(times[idx])
						+ ", value=" + values[idx] + ", qual=" + qualities[idx]);
				}
				// The "REPLACE_MISSING_VALUES_ONLY" store-rule means: 
				//  -- Do not overwrite if a value exists at that time-slice.
				debug1(" Calling store (no overwrite) for ts_id="
						+ path + " with " + num2write + " values, units=" + ts.getUnitsAbbr());

				cwmsTsJdbc.store(dbOfficeId, path, ts.getUnitsAbbr(), times, values,
					qualities, num2write, CwmsConstants.REPLACE_MISSING_VALUES_ONLY,
					overrideProtection, versionDate);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in cwmsTsJdbc.store for '" + path + "': " + ex;
			failure(msg);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
			// Note: There are so many business rules in CWMS which can
			// cause the store to fail, so don't throw DBIO.
//			throw new DbIoException(msg);
		}
	}
	
	/**
	 * Prepare the arrays needed to pass to the store method
	 * If 'noOverwrite' is true, then only get the values that are flagged
	 * for no-overwrite. This is necessary because we must write in two
	 * phases with different store rules.
	 * @param ts The time series
	 * @param msecArray the milliseconds
	 * @param valueArray the values
	 * @param qualArray the quality codes
	 * @param noOverwrite false for normal writes, true for 2nd phase.
	 */
	private void fillTsArrays(CTimeSeries ts, 
		ArrayList<Long> msecArray, ArrayList<Double> valueArray, 
		ArrayList<Integer> qualArray, boolean noOverwrite)
	{
		msecArray.clear();
		valueArray.clear();
		qualArray.clear();
		for(int i=0; i<ts.size(); i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			int qc = CwmsFlags.flag2CwmsQualityCode(tv.getFlags());
			boolean deleting = false;

			if (noOverwrite)
			{
				// We only want variables flagged for noOverwrite
				if (!VarFlags.isNoOverwrite(tv))
					continue;
			}
			else // !noOverwrite -- this is for normal write/delete values
			{
				if (VarFlags.isNoOverwrite(tv))
					continue;
				if (VarFlags.mustDelete(tv))
				{
					deleting = true;
					qc = 5; // As per HEC direction. SCREENED | MISSING.
				}
				else if (!VarFlags.mustWrite(tv))
					continue;
			}
			
			try 
			{
//				double value = ;
//				Double dv = deleting ? null : new Double(value);
				msecArray.add(tv.getTime().getTime());
				valueArray.add(tv.getDoubleValue());
				qualArray.add(qc);
			}
			catch(NoConversionException ex)
			{
				String errMsg = "Cannot prepare value for save '" + 
					tv.getStringValue()	+ "' - not a number.";
				warning(errMsg);
			}
			VarFlags.clearToDelete(tv);
			VarFlags.clearToWrite(tv);
		}
	}
	


	@Override
	public void deleteTimeSeriesRange(CTimeSeries ts, Date from, Date until)
		throws DbIoException, BadTimeSeriesException
	{
		if (from == null || until == null)
			return;
		
		// For CWMS Comps, there are no physical deletes, so first we read, then
		// we set the MISSING flag, then we store.
		int n = fillTimeSeries(ts, from, until, true, true, true);
		if (n == 0)
			return;
		
		int sz = ts.size();
		int num2delete = 0;
		for(int i=0; i<sz; i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			Date d = tv.getTime();
			if (d.compareTo(from) >= 0 && d.compareTo(until) <= 0)
			{
				VarFlags.setToDelete(tv);
				num2delete++;
			}
		}
		
		if (num2delete > 0)
			saveTimeSeries(ts);
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
	
	/**
	 * Pass ResultSet containing DATE_TIME, VALUE, QUALITY_CODE.
	 * @return TimedVariable or null if this value flagged as missing.
	 */
	private TimedVariable rs2TimedVariable(ResultSet rs)
		throws SQLException
	{
		Date timeStamp = db.getFullDate(rs, 1);
		double value = rs.getDouble(2);
		
		// Check for missing, deleted, or rejected data
		if (rs.wasNull())
			return null;
		long lf = rs.getLong(3);
		int f =	CwmsFlags.cwmsQuality2flag(lf);
		if ((f & CwmsFlags.VALIDITY_MISSING) != 0)
			return null;
		
		TimedVariable tv = new TimedVariable(value);
		tv.setTime(timeStamp);
		tv.setFlags(f);
		return tv;
	}


	@Override
	public void deleteTimeSeries(TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		// Note CWMS with db version 7 -- the cp_comp_depends table is updated
		// by Gang's message-queue handler.
		if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_7)
		{
			CompDependsDAI compDependsDAO = db.makeCompDependsDAO();
			try { compDependsDAO.removeTsDependencies(tsid); }
			finally
			{
				compDependsDAO.close();
			}
		}
		try
		{
			CwmsTsJdbc cwmsTsJdbc = new CwmsTsJdbc(db.getConnection());
			cwmsTsJdbc.deleteAll(dbOfficeId, tsid.getUniqueString());
			cache.remove(tsid.getKey());
			refreshTsView();
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
		DbKey sdi = tsid.getKey();
		CTimeSeries ret = new CTimeSeries(sdi, tsid.getInterval(), 
			tsid.getTableSelector());
		ret.setTimeSeriesIdentifier(tsid);
		ret.setDisplayName(tsid.getDisplayName());
		return ret;
	}

	@Override
	public ArrayList<TimeSeriesIdentifier> listTimeSeries()
		throws DbIoException
	{
		reloadTsIdCache();
		ArrayList<TimeSeriesIdentifier> ret = new ArrayList<TimeSeriesIdentifier>();
		for (Iterator<TimeSeriesIdentifier> tsidit = cache.iterator(); tsidit.hasNext(); )
			ret.add(tsidit.next());
		return ret;
	}
	
	
	@Override
	public void reloadTsIdCache() throws DbIoException
	{
		String q = "SELECT a.CWMS_TS_ID, a.VERSION_FLAG, a.INTERVAL_UTC_OFFSET, "
			+ "a.UNIT_ID, a.PARAMETER_ID, c.PUBLIC_NAME, a.TS_CODE, a.LOCATION_CODE, "
			+ "a.LOCATION_ID "
			+ "FROM CWMS_V_TS_ID a, CWMS_V_LOC c "
			+ " WHERE a.LOCATION_CODE = c.LOCATION_CODE "
			+ " AND c.UNIT_SYSTEM = 'SI' ";
//		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			q = q + "and upper(a.DB_OFFICE_ID) = " + sqlString(dbOfficeId.toUpperCase());

		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
				try
				{
					cache.put(rs2TsId(rs, false));
				}
				catch (NoSuchObjectException ex)
				{
					warning("Error creating Cwms TSID for key=" + rs.getInt(1) 
						+ ": " + ex + " -- skipped.");
				}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("CwmsTimeSeriesDb.reloadTsIdCache: " + ex);
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
		String path = tsid.getUniqueString();
		try
		{
			CwmsTsJdbc cwmsTsJdbc = new CwmsTsJdbc(db.getConnection());
			int utcOffset = 
				IntervalCodes.getIntervalSeconds(tsid.getInterval()) == 0 ?
				HecConstants.NO_UTC_OFFSET : HecConstants.UNDEFINED_UTC_OFFSET;
			DbKey tsKey = Constants.undefinedId;

			// CWMS 2.2 = TSDB Database Version 8. Less than this is CWMS v2.1
			if (db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_8)
			{
				@SuppressWarnings("deprecation")
				int tsCode = cwmsTsJdbc.createTsCode(dbOfficeId,
					path,   // 6-part path name 
					utcOffset, // utcOfficeMinutes 
					null,   // intervalForward
					null,   // intervalBackward
					false,  // versionFlag
					true);  // active
				tsKey = DbKey.createDbKey((long)tsCode);
			}
			else // CWMS 2.2 or later
			{
Logger.instance().debug3("createTsCodeBigInteger(" + path + ")");
				BigInteger tsCode = cwmsTsJdbc.createTsCodeBigInteger(dbOfficeId,
					path,   // 6-part path name 
					utcOffset, // utcOfficeMinutes 
					null,   // intervalForward
					null,   // intervalBackward
					false,  // versionFlag
					true);  // active
				tsKey = DbKey.createDbKey(tsCode.longValue());
Logger.instance().debug3("createTsCodeBigInteger returned code=" + tsKey);
			}
			tsid.setKey(tsKey);
			
			refreshTsView();
			
			return tsKey;
		}
		catch(SQLException ex)
		{
			// CWMS-5773 If the create error is due to some kind of user-level constraint,
			// then throw NoSuchObject, meaning that the tsid is bad but the connection is still
			// Ok. Prasad says ORA numbers > 20000 mean user-defined. Probably some kind of 
			// constraint violation.
			String exs = ex.toString();
			int oraidx = exs.indexOf("ORA-");
			if (oraidx >= 0)
			{
				exs = exs.substring(oraidx+4);
				int intlen = 0;
				for(; intlen < exs.length() && Character.isDigit(exs.charAt(intlen)); intlen++);
				if (intlen > 0)
				{
					try
					{
						int oraerr = Integer.parseInt(exs.substring(0, intlen));
						if (oraerr >= 20000)
							throw new NoSuchObjectException(
								"Error creating time series for '" + path + "' with officeId '"
								+ dbOfficeId + "': " + ex);
					}
					catch(NumberFormatException ex2) { /* fall through & throw DbIoException */ }
				}
			}
			// This must be more serious. Assume db connection is now hosed.
			throw new DbIoException(
				"Error creating time series for '" + path + "' with officeId '"
				+ dbOfficeId + "': " + ex);
		}
	}
	
	private void refreshTsView()
	{
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
			return;
		
		// MJM 1/12/2011 After we delete a time-series we must explicitly
		// refresh the CWMS_V_TS_ID view, otherwise it can take up to 5 min.
		try { doModify("call cwms_util.refresh_mv_cwms_ts_id()"); }
		catch(Exception ex)
		{
			warning("Error in cwms_util.refresh_mv_cwms_ts_id: " + ex);
			ex.printStackTrace(Logger.instance().getLogOutput());
		}
	}

	public static void setNoUnitConv(boolean noUnitConv)
	{
		CwmsTimeSeriesDAO.noUnitConv = noUnitConv;
	}



}
