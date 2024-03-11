package decodes.cwms;

import ilex.util.TextUtil;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.SiteName;
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
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import decodes.util.TSUtil;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DbObjectCache;
import opendcs.util.sql.WrappedConnection;
import usace.cwms.db.dao.ifc.ts.CwmsDbTs;
import usace.cwms.db.dao.util.services.CwmsDbServiceLookup;

public class CwmsTimeSeriesDAO
    extends DaoBase
    implements TimeSeriesDAI
{
    private final Logger log = LoggerFactory.getLogger(CwmsTimeSeriesDAO.class);
    protected static final  DbObjectCache<TimeSeriesIdentifier> cache =
        new DbObjectCache<TimeSeriesIdentifier>(60 * 60 * 1000L, false);
    protected SiteDAI siteDAO = null;
    protected DataTypeDAI dataTypeDAO = null;
    private String dbOfficeId = null;
    private static boolean noUnitConv = false;
    private static long lastCacheReload = 0L;
    private String cwmsTsidQueryBase = "SELECT a.CWMS_TS_ID, a.VERSION_FLAG, a.INTERVAL_UTC_OFFSET, "
            + "a.UNIT_ID, a.PARAMETER_ID, '', a.TS_CODE, a.LOCATION_CODE, "
            + "a.LOCATION_ID, a.TS_ACTIVE_FLAG FROM CWMS_V_TS_ID a";
    String getMinStmtQuery = null, getTaskListStmtQuery = null;


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
        if (DbKey.isNull(key))
        {
            try
            {
                throw new NoSuchObjectException("Request for TSID with null ts_code");
            }
            catch(NoSuchObjectException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Unable to retrieve timeseries identifier by key {}", key.getValue());
            }
        }

        synchronized(cache)
        {
            CwmsTsId ret = (CwmsTsId)cache.getByKey(key);

            if (ret != null)
            {
                return ret;
            }
            else
            {
                log.trace("Not in cache ts_code={}", key.getValue());
            }

            String q = cwmsTsidQueryBase
                + " WHERE a.TS_CODE = ?";
            // Don't need to add DB_OFFICE_ID because TS_CODE is unique.

            try
            {
                final long now = System.currentTimeMillis();
                ret = getSingleResult(q,  rs ->
                {
                    try
                    {
                        CwmsTsId tmp = rs2TsId(rs, true);
                        tmp.setReadTime(now);
                        return tmp;
                    }
                    catch (Exception ex)
                    {
                        throw new SQLException("Unable to process data returned from database.", ex);
                    }
                }, key);

                if (ret != null)
                {
                    cache.put(ret);
                    return ret;
                }
            }
            catch(Exception ex)
            {
                throw new DbIoException("Error looking up TS Info for TS_CODE=" + key + ": ", ex);
            }
        }
        throw new NoSuchObjectException("No time-series with ts_code=" + key);
    }

    private CwmsTsId rs2TsId(ResultSet rs, boolean createDataType)
        throws SQLException, DbIoException, NoSuchObjectException
    {
//        private String cwmsTsidQueryBase = "SELECT a.CWMS_TS_ID, a.VERSION_FLAG, a.INTERVAL_UTC_OFFSET, "
//            + "a.UNIT_ID, a.PARAMETER_ID, '', a.TS_CODE, a.LOCATION_CODE, "
//            + "a.LOCATION_ID, a.TS_ACTIVE_FLAG FROM CWMS_V_TS_ID a, CWMS_V_LOC c";

        DbKey key = DbKey.createDbKey(rs, 7);
        String desc = rs.getString(1);
        String param = rs.getString(5);
//        String publicSiteName = rs.getString(6);
        DataType dt =
            DataType.getDataType(Constants.datatype_CWMS, param);

        int x = rs.getInt(3);
        Integer utcOffset = rs.wasNull() ? null : x;

        CwmsTsId ret = new CwmsTsId(key, desc, dt,
            desc, TextUtil.str2boolean(rs.getString(2)),
            utcOffset, rs.getString(4));


        DbKey siteId = DbKey.createDbKey(rs, 8);
        Site site = null;
        try
        {
            site = siteDAO.getSiteById(siteId);
        }
        catch(NoSuchObjectException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("rs2TsId No such Site for TS_ID '{}' with ts_code={} and location_code={}", desc, key, siteId);
            throw ex;
        }
        site.addName(new SiteName(site, Constants.snt_CWMS, rs.getString(9)));
        ret.setSite(site);

        ret.setSiteDisplayName(site.getPublicName());
        ret.setDescription(param + " at " + site.getPublicName());

        ret.setActive(TextUtil.str2boolean(rs.getString(10)));

        if (decodes.db.Database.getDb().getDbIo().getDatabaseType().equalsIgnoreCase("XML"))
        {
            return ret;
        }

        if (createDataType && dt.getId() == Constants.undefinedId)
        {
            DataType dbdt = null;
            try
            {
                dbdt = dataTypeDAO.lookupDataType(param);
            }
            catch(NoSuchObjectException ex)
            {
                dbdt = null;
            }

            if (dbdt == null || !dbdt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS))
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

        int paren = uniqueString.lastIndexOf('(');
        String displayName = null;
        if (paren > 0 && uniqueString.trim().endsWith(")"))
        {
            displayName = uniqueString.substring(paren+1);
            uniqueString = uniqueString.substring(0,  paren);
            int endParen = displayName.indexOf(')');
            if (endParen > 0)
            {
                displayName = displayName.substring(0,  endParen);
            }
        }

        TimeSeriesIdentifier ret = null;
        synchronized(cache) { ret = cache.getByUniqueName(uniqueString); }
        if (ret != null)
        {
            if (displayName != null)
            {
                log.trace("Setting display name to '{}'", displayName);
                ret.setDisplayName(displayName);
            }
            return ret;
        }
        else
        {
            log.trace("cache does not have '{}'", uniqueString);
        }

        DbKey ts_code = ts_id2ts_code(uniqueString);
        if (ts_code == Constants.undefinedId)
        {
            throw new NoSuchObjectException("No CWMS Time Series for ID '"
                + uniqueString + "' and office ID "
                + ((CwmsTimeSeriesDb)db).getDbOfficeId());
        }

        ret = getTimeSeriesIdentifier(ts_code);
        if (displayName != null)
        {
            log.trace("Setting display name to '{}'", displayName);
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
            + "WHERE upper(CWMS_TS_ID) = upper(?)";
        ArrayList<Object> parameters = new ArrayList<>();
        parameters.add(tsid);
        if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
        {
            q = q + " AND upper(DB_OFFICE_ID) = upper(?)";
            parameters.add(dbOfficeId);
        }

        try
        {
            return getSingleResultOr(q, rs -> DbKey.createDbKey(rs, "ts_code"),
                                     Constants.undefinedId, parameters.toArray(new Object[0]));
        }
        catch(Exception ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Error looking up TS_CODE for TS_ID '{}'", tsid);
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
        StringBuffer q = new StringBuffer();
        ArrayList<Object> parameters = new ArrayList<>();
        q.append("SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
            + " WHERE TS_CODE = ?");
        parameters.add(ts_code);

        TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
        if (tsid == null)
        {
            try
            {
                tsid = getTimeSeriesIdentifier(ts_code);
                cts.setTimeSeriesIdentifier(tsid);
            }
            catch(NoSuchObjectException ex)
            {
                log.warn("unable to fillTimeSeries",ex);
            }
        }

        // Part of the contract is to honor the units already specified
        // in the CTimeSeries.
        UnitConverter unitConverter = db.makeUnitConverterForRead(cts);

        if (from != null)
        {
            q.append(" AND DATE_TIME " + lower_check + " ?");
            parameters.add(from);
        }
        if (until != null)
        {
            q.append(" AND DATE_TIME " + upper_check + " ?");
            parameters.add(until);
        }
        q.append(" AND VALUE IS NOT INFINITE");
        try
        {
            int numAdded[] = new int[1];
            numAdded[0] = 0;
            doQuery(q.toString(), rs ->
            {
                TimedVariable tv = null;
                try
                {
                    tv = rs2TimedVariable(rs);
                    if (tv == null)
                    {
                        return;
                    }
                }
                catch(SQLException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error getting data for time series='{}'",cts.getNameString());
                    return;
                }

                // For computation processor, we never want to overwrite data
                // we already have. For a report generator, we DO.
                Date d = tv.getTime();
                if (!overwriteExisting && cts.findWithin(d.getTime()/1000L, 10) != null)
                {
                    return;
                }

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
                            log.warn("unable to fillTimeSeries: ", ex);
                        }
                    }
                    cts.addSample(tv);
                    numAdded[0]++;
                }
            },
            parameters.toArray(new Object[0]));

            return numAdded[0];
        }
        catch(SQLException ex)
        {
            String msg = "Error getting data for time series=%s";
            throw new DbIoException(String.format(msg,cts.getNameString()), ex);
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

        String qbase = "SELECT DATE_TIME, ROUND(VALUE,8), QUALITY_CODE FROM CWMS_V_TSV "
            + " WHERE TS_CODE = " + cts.getSDI()
            + " and DATE_TIME IN (";

        int datesPerQuery = 300;
        int start = 0;
        int end = 0;
        int numAdded[] = new int[1];
        numAdded[0] = 0;
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
                    doQuery(q, rs ->
                    {
                        TimedVariable tv = null;
                        try
                        {
                            tv = rs2TimedVariable(rs);
                            if (tv == null)
                            {
                                return;
                            }
                        }
                        catch(SQLException ex)
                        {
                            String msg= "Error reading data with query '{}'";
                            log.atWarn()
                               .setCause(ex)
                               .log(msg, q);
                            return;
                        }

                        if (tv != null)
                        {
                            // For computation processor, we never want to overwrite data
                            // we already have.
                            Date d = tv.getTime();
                            if (cts.findWithin(d.getTime()/1000L, 10) != null)
                            {
                                return;
                            }

                            if (unitConverter != null)
                            {
                                try
                                {
                                    tv.setValue(unitConverter.convert(tv.getDoubleValue()));
                                }
                                catch (Exception ex)
                                {
                                    log.warn("unable to set value.", ex);
                                }
                            }
                            cts.addSample(tv);
                            numAdded[0]++;
                        }
                    });
                }
                catch(SQLException ex)
                {

                    String msg= "Error reading data with query '{}'";
                    log.atWarn()
                       .setCause(ex)
                       .log(msg, q);
                }
            }
            else if (end < size)
            {
                sb.append(", ");
            }
        }
        return numAdded[0];
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
            + " WHERE TS_CODE = ?"
            + " and DATE_TIME = "
            +   "(select max(date_time) from CWMS_V_TSV "
            +       "where TS_CODE = ?"
             +       " and date_time < ?"
            +         " AND VALUE IS NOT NULL "
            +         " AND BITAND(QUALITY_CODE, ?) = 0 "
            +    ")"
            + " AND VALUE IS NOT INFINITE";

        ArrayList<Object> parameters = new ArrayList<>();
        parameters.add(cts.getSDI());
        parameters.add(cts.getSDI());
        parameters.add(refTime);
        parameters.add(CwmsFlags.QC_MISSING_OR_REJECTED);

        try
        {
            return getSingleResult(q, rs ->
            {
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
                            log.warn("getPreviousValue: Error in unit conversion: ", ex);
                        }
                    }
                    cts.addSample(tv);
                }
                return tv;
            },
            parameters.toArray(new Object[0]));
        }
        catch(SQLException ex)
        {
            String msg= "Error reading data with query '{}'";
            throw new DbIoException(String.format(msg,q), ex);
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
            + " WHERE TS_CODE = ?"
            + " and DATE_TIME = "
            +   "(select min(date_time) from CWMS_V_TSV "
            +       "where TS_CODE = ?"
             +       " and date_time > ?"
            +         " AND VALUE IS NOT NULL "
            +         " AND BITAND(QUALITY_CODE, ?) = 0 "
            +    ")"
            + " AND VALUE IS NOT INFINITE";
        ArrayList<Object> parameters = new ArrayList<>();
        parameters.add(cts.getSDI());
        parameters.add(cts.getSDI());
        parameters.add(refTime);
        parameters.add(CwmsFlags.QC_MISSING_OR_REJECTED);

        try
        {
            return getSingleResult(q, rs ->
            {
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
                            log.warn("getNextValue: Error in unit conversion: ", ex);
                        }
                    }
                    cts.addSample(tv);
                }
                return tv;
            },
            parameters.toArray(new Object[0]));
        }
        catch(SQLException ex)
        {
            String msg= "Error reading data with query '%s'";
            throw new DbIoException(String.format(msg,q), ex);
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
            {
                tsId = getTimeSeriesIdentifier(ts.getSDI());
            }
        }
        catch(NoSuchObjectException ex)
        {
            throw new BadTimeSeriesException("Could not find timeseries: " + ts.getDisplayName(), ex);
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
            log.trace("Time Series '{}' have units '{}' require units '{}'",
                      tsId.getUniqueString(), unitsAbbr, tsId.getStorageUnits());
            if (!unitsAbbr.equalsIgnoreCase(tsId.getStorageUnits()))
            {
                TSUtil.convertUnits(ts, tsId.getStorageUnits());
                unitsAbbr = tsId.getStorageUnits();
            }
        }
        else
        {
            log.trace("Will write time series {} with unit '{}'",
                      tsId.getUniqueString(),  unitsAbbr);
        }

        // We do not yet support versioned data
        java.sql.Timestamp versionDate = null;
        // We will always use overrideProtection false
        boolean overrideProtection = false;

        // We use the RMA Java interface to write to DB
        try (Connection conn = getConnection())
        {
            CwmsDbTs cwmsDbTs = CwmsDbServiceLookup.buildCwmsDb(CwmsDbTs.class, conn);

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

                    log.trace("sample[{}] time={}, value={}, qual=0x{}",
                              idx, db.getLogDateFormat().format(new Date(times[idx])),
                              values[idx], Integer.toHexString(qualities[idx]));
                }
                // The "Replace All" store-rule means:
                //  -- Values at same time stamp I provide will replace existing values
                //  -- Any new values will be written.
                //  -- Existing values at different time stamps will be left alone.
                log.debug(" Calling store for ts_id='{}', office='{}' with {} values, units='{}'",
                          path, dbOfficeId, num2write, ts.getUnitsAbbr());
                cwmsDbTs.store(
                    conn,
                    dbOfficeId, path, ts.getUnitsAbbr(), times, values,
                    qualities, num2write, CwmsConstants.REPLACE_ALL,
                    overrideProtection, versionDate,false);
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

                    log.trace("sample[{}] time={}, value={}, qual=0x{}",
                              idx, db.getLogDateFormat().format(new Date(times[idx])),
                              values[idx], Integer.toHexString(qualities[idx]));
                }
                // The "REPLACE_MISSING_VALUES_ONLY" store-rule means:
                //  -- Do not overwrite if a value exists at that time-slice.
                log.debug(" Calling store (no overwrite) for ts_id='{}' with {} values, units='{}'",
                       path, num2write, ts.getUnitsAbbr());

                cwmsDbTs.store(conn, dbOfficeId, path, ts.getUnitsAbbr(), times, values,
                    qualities, num2write, CwmsConstants.REPLACE_MISSING_VALUES_ONLY,
                    overrideProtection, versionDate, false);
            }
        }
        catch(SQLException ex)
        {
            String msg = "Error in cwmsTsJdbc.store for '{}'";
            log.atError()
               .setCause(ex)
               .log(msg, path);

            if (msg.contains("read from socket") || msg.contains("connection is closed"))
            {
                throw new DbIoException(msg, ex);
            }
            // Note: There are so many business rules in CWMS which can
            // cause the store to fail, so don't throw DBIO.
            //            throw new DbIoException(msg);
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

            if (noOverwrite)
            {
                // We only want variables flagged for noOverwrite
                if (!VarFlags.isNoOverwrite(tv))
                {
                    continue;
                }
            }
            else // !noOverwrite -- this is for normal write/delete values
            {
                if (VarFlags.isNoOverwrite(tv))
                {
                    continue;
                }
                if (VarFlags.mustDelete(tv))
                {
                    qc = 5; // As per HEC direction. SCREENED | MISSING.
                }
                else if (!VarFlags.mustWrite(tv))
                {
                    continue;
                }
            }

            try
            {
                // get the required primitive values first so
                // that errors will bypass the time getting added
                // and offsetting the data.
                long tvTimeMs = tv.getTime().getTime();
                double tvValue = tv.getDoubleValue();
                msecArray.add(tvTimeMs);
                valueArray.add(tvValue);
                qualArray.add(qc);
            }
            catch(NoConversionException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log("Cannot prepare value for save '{}' - not a number.", tv.getStringValue());
            }
            VarFlags.clearToDelete(tv);
            VarFlags.clearToWrite(tv);
        }
    }



    @Override
    public void deleteTimeSeriesRange(CTimeSeries cts, Date from, Date until)
        throws DbIoException, BadTimeSeriesException
    {
        if (from == null || until == null)
        {
            return;
        }

        if (!cts.isExpanded())
        {
            fillTimeSeriesMetadata(cts); // may throw BadTimeSeriesException
            cts.setIsExpanded();
        }

        String tsid = cts.getTimeSeriesIdentifier().getUniqueString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try (Connection conn = getConnection())
        {
            log.debug("Calling deleteTs for tsid '{}' for date range: {} to {}",
                      tsid, sdf.format(from), sdf.format(until));

            CwmsDbTs cwmsDbTs = CwmsDbServiceLookup.buildCwmsDb(CwmsDbTs.class, conn);
            cwmsDbTs.deleteTs(conn, dbOfficeId, tsid,
                from, until, true, true,   // date range inclusive on both ends
                null,                      // version date
                null,                      // NavigableSet<Date> use to delete specific dates
                true,                      // use latest version if version date is null (which it is)
                1,                         // item mask. 1 means delete values only
                "F");                      // Override Protection, F=do not delete protected data.
        }
        catch(SQLException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("deleteTimeSeriesRange - Error in CwmsDbTs.deleteTs for tsid '{}' from {} to {}.",
                       tsid, sdf.format(from), sdf.format(until));
        }

//        // For CWMS Comps, there are no physical deletes, so first we read, then
//        // we set the MISSING flag, then we store.
//        int n = fillTimeSeries(cts, from, until, true, true, true);
//        if (n == 0)
//            return;
//
//        int sz = cts.size();
//        int num2delete = 0;
//        for(int i=0; i<sz; i++)
//        {
//            TimedVariable tv = cts.sampleAt(i);
//            Date d = tv.getTime();
//            if (d.compareTo(from) >= 0 && d.compareTo(until) <= 0)
//            {
//                VarFlags.setToDelete(tv);
//                num2delete++;
//            }
//        }
//
//        if (num2delete > 0)
//            saveTimeSeries(cts);
    }

    @Override
    public void fillTimeSeriesMetadata(CTimeSeries ts) throws DbIoException,
        BadTimeSeriesException
    {
        log.trace("fillTimeSeriesMetadata sdi={}", ts.getSDI());
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
            {
                ts.setUnitsAbbr(tsid.getStorageUnits());
            }
        }
        catch(NoSuchObjectException ex)
        {
            log.warn("Error expanding SDI: ", ex);
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
        {
            return null;
        }
        long lf = rs.getLong(3);
        int f =    CwmsFlags.cwmsQuality2flag(lf);
        if ((f & CwmsFlags.VALIDITY_MISSING) != 0)
        {
            return null;
        }

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
            try (CompDependsDAI compDependsDAO = db.makeCompDependsDAO();)
            {
                compDependsDAO.removeTsDependencies(tsid);
            }
        }
        try (Connection conn = getConnection();)
        {
            CwmsDbTs cwmsDbTs = CwmsDbServiceLookup.buildCwmsDb(CwmsDbTs.class, conn);
            log.debug("Deleting TSID '{}' from office ID={}", tsid.getUniqueString(), dbOfficeId);
            cwmsDbTs.deleteAll(conn, dbOfficeId, tsid.getUniqueString());
            synchronized(cache)
            {
                cache.remove(tsid.getKey());
            }
            refreshTsView();
        }
        catch(Exception ex)
        {
            String msg = "Cannot delete time series '{}'";
            log.atWarn()
               .setCause(ex)
               .log(msg, tsid.getUniqueString());
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
        // MJM 20161025 don't reload more if already done within threshold.
        if (System.currentTimeMillis() - lastCacheReload > cacheReloadMS)
        {
            reloadTsIdCache();
        }

        ArrayList<TimeSeriesIdentifier> ret = new ArrayList<TimeSeriesIdentifier>();
        synchronized(cache)
        {
            for (Iterator<TimeSeriesIdentifier> tsidit = cache.iterator(); tsidit.hasNext();)
            {
                ret.add(tsidit.next());
            }
        }
        return ret;
    }

    @Override
    public ArrayList<TimeSeriesIdentifier> listTimeSeries(boolean forceRefresh)
        throws DbIoException
    {
        if (forceRefresh)
        {
            lastCacheReload = 0L;
        }
        return listTimeSeries();
    }


    @Override
    public void reloadTsIdCache() throws DbIoException
    {
        log.debug("reloadTsIdCache()");

        // Each TSID will need a site, so prefill the site cache to prevent
        // it from doing individual reads for each site.
//        siteDAO.fillCache();

        String q = cwmsTsidQueryBase + " WHERE upper(a.DB_OFFICE_ID) = upper(?)";
        int origFetchSize = getFetchSize();
        try
        {
            int tsidFetchSize = DecodesSettings.instance().tsidFetchSize;
            if (tsidFetchSize > 0)
            {
                setFetchSize(tsidFetchSize);
            }


            List<TimeSeriesIdentifier> tsidList = getResultsIgnoringNull(q, rs ->
            {
                CwmsTsId retVal = null;
                try
                {
                    retVal = rs2TsId(rs, false);
                }
                catch (DbIoException | NoSuchObjectException ex)
                {
                    log.atWarn()
                       .setCause(ex)
                       .log("Error creating Cwms TSID -- skipped.");
                }
                return retVal;
            }, dbOfficeId);

            synchronized(cache)
            {
                cache.clear();
                for(TimeSeriesIdentifier tsid: tsidList)
                {
                    cache.put(tsid);
                }
            }

            log.debug("After fill, cache has {} TSIDs.", cache.size());
        }
        catch (SQLException ex)
        {
            final String msg = "Failed to reload CwmsTimeSeriesDb TsId Cache.";
            log.atError()
               .setCause(ex)
               .log(msg);
            throw new DbIoException(msg, ex);
        }
        finally
        {
            setFetchSize(origFetchSize);
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
        Site site = tsid.getSite();
        if (site == null)
        {
            String sn = tsid.getSiteName();
            if (sn == null || sn.trim().length() == 0)
            {
                throw new BadTimeSeriesException("TSID '" + tsid.getUniqueString()
                    + "' has no location part.");
            }
            DbKey siteID = siteDAO.lookupSiteID(sn);
            if (DbKey.isNull(siteID))
            {
                throw new NoSuchObjectException("TSID '" + tsid.getUniqueString()
                    + "' cannot find location '" + sn + "' in this database.");
            }
        }
        String path = tsid.getUniqueString();
        try (Connection conn = getConnection();)
        {
            CwmsDbTs cwmsDbTs = CwmsDbServiceLookup.buildCwmsDb(CwmsDbTs.class, conn);
            int utcOffset =
                IntervalCodes.getIntervalSeconds(tsid.getInterval()) == 0 ?
                HecConstants.NO_UTC_OFFSET : HecConstants.UNDEFINED_UTC_OFFSET;
            DbKey tsKey = Constants.undefinedId;

            BigInteger tsCode = cwmsDbTs.createTsCodeBigInteger(
                conn,
                dbOfficeId,
                path,   // 6-part path name
                utcOffset, // utcOfficeMinutes
                null,   // intervalForward
                null,   // intervalBackward
                false,  // versionFlag
                true);  // active
                tsKey = DbKey.createDbKey(tsCode.longValue());
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
                        {
                            throw new NoSuchObjectException(
                                "Error creating time series for '" + path + "' with officeId '"
                                + dbOfficeId + "': " + ex, ex);
                        }
                    }
                    catch(NumberFormatException ex2) { /* fall through & throw DbIoException */ }
                }
            }
            // This must be more serious. Assume db connection is now hosed.
            throw new DbIoException(
                "Error creating time series for '" + path + "' with officeId '"
                + dbOfficeId, ex);
        }
    }

    private void refreshTsView()
    {
        if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
        {
            return;
        }

        // MJM 1/12/2011 After we delete a time-series we must explicitly
        // refresh the CWMS_V_TS_ID view, otherwise it can take up to 5 min.
        try
        {
            log.warn("calling legacy database method: 'cwms_util.refresh_mv_cwms_ts_id()'");
            doModify("call cwms_util.refresh_mv_cwms_ts_id()", new Object[0]);
        }
        catch(Exception ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Error in cwms_util.refresh_mv_cwms_ts_id.");
        }
    }

    public static void setNoUnitConv(boolean noUnitConv)
    {
        CwmsTimeSeriesDAO.noUnitConv = noUnitConv;
    }

    @Override
    public void setAppModule(String module)
    {
        // Do nothing. CWMS doesn't use modules.
    }

    @Override
    public void modifyTSID(TimeSeriesIdentifier tsid)
            throws DbIoException, NoSuchObjectException, BadTimeSeriesException
    {
        // Not implemented for CWMS
    }

    @Override
    public DataCollection getNewData(DbKey applicationId)
        throws DbIoException
    {
        DataCollection dataCollection = new DataCollection();

        String failTimeClause =
                    DecodesSettings.instance().retryFailedComputations
                    ? " and (a.FAIL_TIME is null OR SYSDATE - a.FAIL_TIME >= 1/24)"
                    : "";

        getTaskListStmtQuery =
            "select a.RECORD_NUM, a.SITE_DATATYPE_ID, a.VALUE, a.START_DATE_TIME, "
            + "a.DELETE_FLAG, a.UNIT_ID, a.VERSION_DATE, a.QUALITY_CODE, a.MODEL_RUN_ID "
            + "from CP_COMP_TASKLIST a "
            + "where a.LOADING_APPLICATION_ID = ?"
            + " and ROWNUM < 20000"
            + failTimeClause
            + " ORDER BY a.site_datatype_id, a.start_date_time";

        try (Connection conn = getConnection();
             PreparedStatement getTaskListStmt = conn.prepareStatement(getTaskListStmtQuery);
            )
        {
            getTaskListStmt.setLong(1,applicationId.getValue());

            ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
            ArrayList<Integer> badRecs = new ArrayList<Integer>();
            log.trace("Executing '{}' with appId={}", getTaskListStmtQuery, applicationId);
            try (ResultSet rs = getTaskListStmt.executeQuery())
            {
                Date lastTimestamp = null;
                while (rs.next())
                {
                    // Extract the info needed from the result set row.
                    Date timeStamp = new Date(rs.getDate(4).getTime());
                    boolean exceedsMaxTimeGap = exceedsMaxTimeGap(lastTimestamp, timeStamp);
                    if(exceedsMaxTimeGap)
                    {
                        break;
                    }
                    lastTimestamp = timeStamp;
                    int recordNum = rs.getInt(1);
                    DbKey sdi = DbKey.createDbKey(rs, 2);
                    double value = rs.getDouble(3);
                    boolean valueWasNull = rs.wasNull();
                    String df = rs.getString(5);
                    char c = df.toLowerCase().charAt(0);
                    boolean deleted = false;
                    if (c == 'u')
                    {
                        // msg handler will send this when he gets
                        // TsCodeChanged. It tells me to update my cache.
                        DbObjectCache<TimeSeriesIdentifier> cache = getCache();
                        synchronized(cache)
                        {
                            TimeSeriesIdentifier tsid = cache.getByKey(sdi);
                            if (tsid != null)
                            {
                                DbKey newCode = DbKey.createDbKey(rs, 9);
                                cache.remove(sdi);
                                tsid.setKey(newCode);
                                cache.put(tsid);
                                continue;
                            }
                        }
                    }
                    else
                    {
                        deleted = TextUtil.str2boolean(df);
                    }

                    String unitsAbbr = rs.getString(6);
                    Date versionDate = db.getFullDate(rs, 7);
                    BigDecimal qc = rs.getBigDecimal(8);
                    long qualityCode = qc == null ? 0 : qc.longValue();

                    TasklistRec rec = new TasklistRec(recordNum, sdi, value,
                        valueWasNull, timeStamp, deleted,
                        unitsAbbr, versionDate, qualityCode);
                    tasklistRecs.add(rec);
                }

                RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);

                // Process the real-time records collected above.
                for(TasklistRec rec : tasklistRecs)
                {
                    processTasklistEntry(rec, dataCollection, rrhandle, badRecs, applicationId);
                }

                dataCollection.setTasklistHandle(rrhandle);

                // Delete the bad tasklist recs, 250 at a time.
                if (badRecs.size() > 0)
                {
                    log.debug("getNewDataSince deleting {} bad tasklist records.", + badRecs.size());
                    doModifyBatch("delete from cp_comp_tasklist where record_num = ?",
                                  (v) -> new Object[] {v},
                                  badRecs, 250);
                    badRecs.clear();
                }

                // Show each tasklist entry in the log if we're at debug level 3
                if (log.isTraceEnabled())
                {
                    List<CTimeSeries> allTs = dataCollection.getAllTimeSeries();
                    log.trace("getNewData, returning {} TimeSeries.", allTs.size());
                    for(CTimeSeries ts : allTs)
                    {
                        log.trace("ts '{}' {} values.", ts.getTimeSeriesIdentifier().getUniqueString(), ts.size());
                    }
                }

                return dataCollection;
            }
        }
        catch(SQLException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Error reading new data.");
            throw new DbIoException("Error reading new data: " + ex.getLocalizedMessage(), ex);
        }
    }

    final boolean exceedsMaxTimeGap(Date lastTimestamp, Date currentTimestamp)
    {
        if(lastTimestamp == null || currentTimestamp == null)
        {
            return false;
        }
        long daysBetween = TimeUnit.MILLISECONDS.toDays(currentTimestamp.getTime() - lastTimestamp.getTime());
        return daysBetween >= DecodesSettings.instance().cp_cwmstsdb_getNewData_max_timegap_days;
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
            try
            {
                TimeSeriesIdentifier tsid = getTimeSeriesIdentifier(rec.getSdi());
                String tabsel = tsid.getPart("paramtype") + "." +
                    tsid.getPart("duration") + "." + tsid.getPart("version");
                cts = new CTimeSeries(rec.getSdi(), tsid.getInterval(),
                    tabsel);
                cts.setModelRunId(-1);
                cts.setTimeSeriesIdentifier(tsid);
                cts.setUnitsAbbr(rec.getUnitsAbbr());
                if (((TimeSeriesDb)db).fillDependentCompIds(cts, applicationId, this) == 0)
                {
                    log.warn("Deleting tasklist rec for '{}' because no dependent comps.", tsid.getUniqueString());
                    if (badRecs != null)
                    {
                        badRecs.add(rec.getRecordNum());
                    }
                    return;
                }

                dataCollection.addTimeSeries(cts);
            }
            catch(decodes.tsdb.DuplicateTimeSeriesException ex)
            { // won't happen -- already verified it's not there.
                log.trace("An exception has been thrown that shouldn't have been. Please inform the project.", ex);
            }
            catch(NoSuchObjectException ex)
            {
                log.warn("Deleting tasklist rec for non-existent ts_code {}", rec.getSdi());
                if (badRecs != null)
                {
                    badRecs.add(rec.getRecordNum());
                }
                return;
            }
        }
        else
        {
            // The time series already existed from a previous tasklist rec in this run.
            // Make sure this rec's unitsAbbr matches the CTimeSeries.getUnitsAbbr().
            // If not, convert it to the CTS units.
            String recUnitsAbbr = rec.getUnitsAbbr();
            String ctsUnitsAbbr = cts.getUnitsAbbr();
            if (ctsUnitsAbbr == null) // no units yet assigned?
            {
                cts.setUnitsAbbr(ctsUnitsAbbr = recUnitsAbbr);
            }
            else if (recUnitsAbbr == null) // for some reason, this tasklist record doesn't have units
            {
                recUnitsAbbr = ctsUnitsAbbr;
            }
            else if (!TextUtil.strEqualIgnoreCase(recUnitsAbbr, ctsUnitsAbbr))
            {
                EngineeringUnit euOld = EngineeringUnit.getEngineeringUnit(recUnitsAbbr);
                EngineeringUnit euNew = EngineeringUnit.getEngineeringUnit(ctsUnitsAbbr);

                UnitConverter converter = Database.getDb().unitConverterSet.get(euOld, euNew);
                if (converter != null)
                {
                    try
                    {
                        rec.setValue(converter.convert(rec.getValue()));
                    }
                    catch (Exception ex)
                    {
                        log.atWarn()
                           .setCause(ex)
                           .log(
                            "Tasklist for '{}' exception converting {} to {} -- will use as-is.",
                            cts.getTimeSeriesIdentifier().getUniqueString(), rec.getValue(), rec.getUnitsAbbr());
                    }
                }
                else
                {
                    log.warn("Tasklist for '{}' cannot convert {} {} to {}",
                        cts.getTimeSeriesIdentifier().getUniqueString(), rec.getValue(), rec.getUnitsAbbr(), cts.getUnitsAbbr());
                }
            }
        }
        if (rrhandle != null)
        {
            rrhandle.addRecNum(rec.getRecordNum());
        }

        // Construct timed variable with appropriate flags & add it.
        TimedVariable tv = new TimedVariable(rec.getValue());
        tv.setTime(rec.getTimeStamp());
        tv.setFlags(CwmsFlags.cwmsQuality2flag(rec.getQualityCode()));

        if (!rec.isDeleted() && !rec.isValueWasNull())
        {
            VarFlags.setWasAdded(tv);
            cts.addSample(tv);
            // Remember which tasklist records are in this timeseries.
            cts.addTaskListRecNum(rec.getRecordNum());
            log.trace("Added value {} to time series '{}' flags=0x{} cwms qualcode=0x{}",
                tv.toString(), cts.getTimeSeriesIdentifier().getUniqueString(),
                Integer.toHexString(tv.getFlags()), Long.toHexString(rec.getQualityCode()));
        }
        else
        {
            VarFlags.setWasDeleted(tv);
            log.warn("Discarding deleted value {} for time series '{}' flags=0x{} cwms qualcode=0x{}",
                     tv.toString(), cts.getTimeSeriesIdentifier().getUniqueString(),
                     Integer.toHexString(tv.getFlags()), Long.toHexString(rec.getQualityCode()));
        }
    }

    @Override
    protected Connection getConnection()
    {
        // local getConnection() method that saves the connection locally
        if (myCon == null)
        {
            myCon = db.getConnection();
        }
        siteDAO.setManualConnection(myCon);
        dataTypeDAO.setManualConnection(myCon);
        // NOTE: there should already be a pooled connection used by the
        // CWMS Components. This is primarily to cover the cases we haven't gotten to yet.
        return new WrappedConnection(myCon, rs -> {},true);
    }
}
