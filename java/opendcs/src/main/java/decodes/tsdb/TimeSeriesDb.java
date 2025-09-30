/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.tsdb;

import ilex.util.TextUtil;
import ilex.util.HasProperties;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.Variable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.AlarmDAI;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.DaiBase;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.DeviceStatusDAI;
import opendcs.dai.EnumDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.AlarmDAO;
import opendcs.dao.AlgorithmDAO;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.CompDependsNotifyDAO;
import opendcs.dao.ComputationDAO;
import opendcs.dao.DacqEventDAO;
import opendcs.dao.DaoBase;
import opendcs.dao.DataTypeDAO;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DeviceStatusDAO;
import opendcs.dao.EnumSqlDao;
import opendcs.dao.LoadingAppDao;
import opendcs.dao.PlatformStatusDAO;
import opendcs.dao.PropertiesSqlDao;
import opendcs.dao.SiteDAO;
import opendcs.dao.TsGroupDAO;
import opendcs.dao.XmitRecordDAO;
import opendcs.util.sql.WrappedConnection;
import decodes.util.DecodesSettings;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.EngineeringUnit;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.UnitConverter;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.sql.KeyGeneratorFactory;
import decodes.sql.OracleDateParser;
import decodes.sql.SqlDatabaseIO;

/**
This is the base class for the time-series database implementation.
Sub classes must override all the abstract methods and provide
a mechanism to persistently store time series and computational meta
data.
*/
public abstract class TimeSeriesDb implements HasProperties, DatabaseConnectionOwner
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    public static String module = "tsdb";

    /** The application ID of the connected program */
    protected DbKey appId = Constants.undefinedId;

    /** The model run ID currently in use for writing data. */
    protected int writeModelRunId;

    /**
     * Flag indicating test mode: If set no changes should be made
     * to the database. Log messages only.
     */
    protected boolean testMode;
    protected final javax.sql.DataSource dataSource;

    /** Used to format timestamps written to the database. */
    protected SimpleDateFormat writeDateFmt = null;

    /** Used to format timestamps read from the database. */
    protected SimpleDateFormat readDateFmt = null;

    /** Used to generate new surrogate keys. */
    public KeyGenerator keyGenerator;

    /** The TSDB database version and description
     * comes from tsdb_database_version table */
    public int tsdbVersion;

    /** TSDB description */
    public String tsdbDescription;

    /**
    * Set to true if the SDI is sufficient to specify a unique time series.
    * For USBR-HDB: false, interval and table selector are also required.
    * for USACE-CWMS: true - SDI is ts_code, which is unique key.
    */
    public static boolean sdiIsUnique = false; // default for USBR HDB.

    protected static String curTimeName = "current_timestamp";
    protected static String maxCompRetryTimeFrmt = "INTERVAL '%d hour'";

    protected int decodesDatabaseVersion = -1;
    protected String decodesDatabaseOptions = "";

    protected String dbUser = "unknown";

    protected Properties props = new Properties();

    protected String cpCompDepends_col1 = null;
    protected SimpleDateFormat debugDateFmt =
        new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss z");

    protected Calendar readCal = null;
    private OracleDateParser oracleDateParser = null;
    protected String databaseTimezone = "UTC";
    protected boolean _isOracle = false;

    /**
     * Set by the reclaimTasklistSec Computation App Property.
     * Default=0, meaning that the feature is disabled.
     */
    protected int reclaimTasklistSec = 0;

    // If reclaimTasklistSec > 0, this is the time the reclaim was last done.
    protected long lastReclaimMsec = 0L;

    private boolean connected = false;

    protected final DecodesSettings settings;
    /**
     * Lazy initialization, called at the first time a date or timestamp
     * is needing to be parsed or formatted. Can't do this in the constructor
     * because the database is not yet connected so _dbio.isOracle doesn't work.
     */
    /**
     * No args constructor required.
     */
    public TimeSeriesDb(String appName, javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        this.settings = settings;

        writeModelRunId = Constants.undefinedIntKey;
        testMode = false;
        tsdbVersion = 1;
        this.dataSource = dataSource;
        setupKeyGenerator();

        try (Connection conn = dataSource.getConnection())
        {
            determineTsdbVersion(conn, this);
            postConnectInit(appName, conn);
        }
        catch (BadConnectException | SQLException ex)
        {
            throw new DatabaseException("Unable to initialize database.", ex);
        }
        cpCompDepends_col1 = isHdb() || this.tsdbVersion >= TsdbDatabaseVersion.VERSION_9 
                           ? "TS_ID" : "SITE_DATATYPE_ID";
        getLogDateFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** @return the JDBC connection in use by this object. */
    public Connection getConnection() throws SQLException
    {
        Connection conn = dataSource.getConnection();
        postConInit(conn);
        return conn;
    }

    /**
     * Handles any operations that need to be performed on the connection if it was freshly opened.
     * @param conn
     * @throws SQLException
     */
    public abstract void postConInit(Connection conn) throws SQLException;

    public KeyGenerator getKeyGenerator() { return keyGenerator; }

    /**
     * Uses the class in DecodesSettings to create a key generator.
     * @throws BadConnectException on any error.
     */
    protected void setupKeyGenerator() throws DatabaseException
    {
        String keyGenClass = settings.sqlKeyGenerator;
        try
        {
            keyGenerator = KeyGeneratorFactory.makeKeyGenerator(
                keyGenClass);
        }
        catch (Exception ex)
        {
            throw new DatabaseException(
                "Cannot initialize key generator from class '" + keyGenClass
                    + "'.", ex);
        }
    }


    //==================================================================
    // The following helper-methods may be called or overloaded by the
    // concrete subclass.
    //==================================================================

    /** @return string representation for a boolean value in this db. */
    public String sqlBoolean(boolean v)
    {
        return v ? "'Y'" : "'N'";
    }

    /**
     * wraps a string in single quotes for use in an SQL statement.
     * If the string is null, returns 'NULL'.
     * If the string contains a single quote, it is escaped by doubling it.
     */
    public String sqlString(String field)
    {
        if (field == null)
            return "NULL";
        else
        {
            // Have to escape any single quotes in the string.
            if (field.indexOf('\'') >= 0)
            {
                StringBuilder sb = new StringBuilder(field);
                for(int i=0; i<sb.length(); i++)
                    if (sb.charAt(i) == '\'')
                        sb.insert(i++, '\'');
                field = sb.toString();
            }
            return "'" + field + "'";
        }
    }

    public void commit()
        throws DbIoException
    {
    }

    public void rollback()
    {
    }

    /**
     * @return boolean value from the result set.
     */
    public boolean getBoolean(ResultSet rs, int column)
    {
        try
        {
            String s = rs.getString(column);
            if (rs.wasNull())
                return false;
            else
                return TextUtil.str2boolean(s);
        }
        catch(SQLException ex)
        {
            log.atWarn().setCause(ex).log("Error retrieving boolean.");
            return false;
        }
    }

    /**
     * Provides common post-connect initialization like loading the intervals
     * and setting the application ID.
     */
    public void postConnectInit(String appName, Connection conn)
        throws BadConnectException
    {
        determineTsdbVersion(conn, this);

        // If an application name is provided, lookup the ID.
        if (appName != null && appName.trim().length() > 0)
        {
            try(LoadingAppDAI loadingAppDAO = makeLoadingAppDAO())
            {
                loadingAppDAO.setManualConnection(conn);
                appId = loadingAppDAO.lookupAppId(appName);
                info("Connected to " + module + " with app name '"
                    + appName + "' appId=" + appId + ", tsdbVersion=" + tsdbVersion);
            }
            catch(Exception ex)
            {
                 String msg = "Cannot determine app ID for name '" + appName + "'";
                throw new BadConnectException(msg, ex);
            }
        }

        // Load the intervals
        try (IntervalDAI intervalDAO = this.makeIntervalDAO())
        {
            intervalDAO.setManualConnection(conn);
            intervalDAO.loadAllIntervals();
        }
        catch(Exception ex)
        {
            log.atWarn().setCause(ex).log("Cannot load intervals.");
        }

        connected = true;
    }


    /**
     * @return true if the database is currently connected. False otherwise.
     */
    public boolean isConnected()
    {
        return connected;
    }

    /**
     * Given a site name, return the database's surrogate key ID.
     * @param siteName the site name
     * @return the ID corresponding to the passed name, or null if not found.
     * @throws DbIoException on Database IO error.
     */
    public DbKey lookupSiteID( SiteName siteName )
        throws DbIoException
    {
        SiteDAI siteDao = makeSiteDAO();
        try { return siteDao.lookupSiteID(siteName); }
        finally
        {
            siteDao.close();
        }
    }

    /**
     * Given a site name value, return the database's surrogate key ID.
     * The database should attempt to find a name with the preferred type
     * defined in cfg.siteNameStandard. If that fails, it should look for
     * a match with other name types.
     * @param nameValue the site name
     * @return the site-ID to the passed name, or undefinedId if not found.
     * @throws DbIoException on Database IO error.
     */
    public DbKey lookupSiteID(String nameValue)
        throws DbIoException
    {
        SiteDAI siteDao = makeSiteDAO();
        try { return siteDao.lookupSiteID(nameValue); }
        finally
        {
            siteDao.close();
        }
    }

    /**
     * Looks up the SDI for and sets it within a DbCompParm.
     * Called from GUI when making assignment from site ID and data type.
     * @param parm the DbCompParm object
     * @param siteId the site ID
     * @param dtcode the data type code
     */
    public abstract void setParmSDI(DbCompParm parm, DbKey siteId, String dtcode)
        throws DbIoException, NoSuchObjectException;


    /**
     * Fills a time series with values from the given date range (inclusive).
     * Must handle any unit conversions required between the unitsAbbr
     * in the CTimeSeries and the storage units in the database.
     * @param from the lower-bound of the range
     * @param until the upper-bound of the range
     * @return number of values added to the time series.
     * @throws DbIoException on Database IO error.
     * @throws BadTimeSeriesException if time series doesn't exist.
     */
    public int fillTimeSeries( CTimeSeries ts, Date from, Date until )
        throws DbIoException, BadTimeSeriesException
    {
        TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
        try
        {
            return timeSeriesDAO.fillTimeSeries(ts, from, until);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }


    /**
     * Fills a time series with values from the given date range, where
     * you control how the boundaries are included.
     * Must handle any unit conversions required between the unitsAbbr
     * in the CTimeSeries and the storage units in the database.
     * @param from the lower-bound of the range
     * @param until the upper-bound of the range
     * @param include_lower true to include value at the lower-bound time.
     * @param include_upper true to include value at the upper-bound time.
     * @param overwriteExisting
     * @return number of values added to the time series.
     * @throws DbIoException on Database IO error.
     * @throws BadTimeSeriesException if time series doesn't exist.
     */
    public int fillTimeSeries( CTimeSeries ts, Date from, Date until,
        boolean include_lower, boolean include_upper, boolean overwriteExisting)
        throws DbIoException, BadTimeSeriesException
    {
        TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
        try
        {
            return timeSeriesDAO.fillTimeSeries(ts, from, until,
                include_lower, include_upper, overwriteExisting);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }


    /**
     * Fills a time series with values for specific times.
     * Must handle any unit conversions required between the unitsAbbr
     * in the CTimeSeries and the storage units in the database.
     * @param ts the time series
     * @param queryTimes the set of query times.
     * @return number of values added to the time series.
     * @throws DbIoException on Database IO error.
     * @throws BadTimeSeriesException if time series doesn't exist.
     */
    public int fillTimeSeries(CTimeSeries ts, Collection<Date> queryTimes)
        throws DbIoException, BadTimeSeriesException
    {
        TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
        try
        {
            return timeSeriesDAO.fillTimeSeries(ts, queryTimes);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }

    /**
     * Retrieves the previous value to the specified time and stores it in the
     * passed time series. That is the most recent value with a time stamp
     * that is BEFORE the reference time.
     * Must handle any unit conversions required between the unitsAbbr
     * in the CTimeSeries and the storage units in the database.
     * @param ts the time series
     * @param refTime the referenced time.
     * @return TimedVariable that was placed in the series, or null if none.
     * @throws DbIoException on Database IO error.
     * @throws BadTimeSeriesException if time series doesn't exist.
     */
    public TimedVariable getPreviousValue(CTimeSeries ts, Date refTime)
        throws DbIoException, BadTimeSeriesException
    {
        TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
        try
        {
            return timeSeriesDAO.getPreviousValue(ts, refTime);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }

    /**
     * Retrieves the next value to the specified time and stores it in the
     * passed time series. That is the earliest value with a time stamp
     * that is AFTER the reference time.
     * Must handle any unit conversions required between the unitsAbbr
     * in the CTimeSeries and the storage units in the database.
     * @param ts the time series
     * @param refTime the referenced time.
     * @return TimedVariable that was placed in the series, or null if none.
     * @throws DbIoException on Database IO error.
     * @throws BadTimeSeriesException if time series doesn't exist.
     */
    public TimedVariable getNextValue(CTimeSeries ts, Date refTime)
        throws DbIoException, BadTimeSeriesException
    {
        TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
        try
        {
            return timeSeriesDAO.getNextValue(ts, refTime);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }

    /**
     * Releases triggers associated with the new data in the passed collection.
     * The implementation may use information contained in the collection's
     * opaque handle.
     * @param dc the data collection to be released.
     */
    public void releaseNewData(DataCollection dc, TimeSeriesDAI tsDAO)
        throws DbIoException
    {
        RecordRangeHandle rrh = dc.getTasklistHandle();
        if (rrh == null)
            return;

        int maxRetries = DecodesSettings.instance().maxComputationRetries;
        boolean doRetryFailed = DecodesSettings.instance().retryFailedComputations;

        // Oracle was providing things in the wrong timestamp using current_timestamp.
        // TODO: needs to be checked against Postgres
        final String curTimeSqlCommand = this.isOracle() ? "sysdate" : "current_timestamp" ;
        final String intervalSqlCommand = this.isOracle() ? " ?/24 )" : " ? * INTERVAL '1' hour )" ; // Oracle date math
        
        try(Connection tcon = getConnection();
            PreparedStatement deleteNormal = tcon.prepareStatement("delete from CP_COMP_TASKLIST where RECORD_NUM = ?");
            PreparedStatement deleteFailedAfterMaxRetries = tcon.prepareStatement(
                      "delete from CP_COMP_TASKLIST "
                    + "where RECORD_NUM = ? " // failRecList
                    + "and ((" + curTimeSqlCommand + " - DATE_TIME_LOADED) > " // curTimeName
                    + intervalSqlCommand ); //String.format(maxCompRetryTimeFrmt, maxRetries) + ")"); //
            PreparedStatement updateFailedRetry = tcon.prepareStatement(
                "update CP_COMP_TASKLIST set FAIL_TIME = " + curTimeSqlCommand + " where RECORD_NUM = ? and "
            +    "( (" + curTimeSqlCommand + " - DATE_TIME_LOADED) <= " + intervalSqlCommand);
            PreparedStatement updateFailTime = tcon.prepareStatement(
                "UPDATE CP_COMP_TASKLIST "
            +    " SET FAIL_TIME = " + curTimeSqlCommand
            +   " where record_num = ?"
                );


        ){
            while(rrh.size() > 0)
            {
                String []records = rrh.getRecNumList(250).split(",");
                for( String rec: records ){
                    if( "".equalsIgnoreCase(rec) ) continue;
                    deleteNormal.setLong(1, Long.parseLong(rec.trim()));
                    deleteNormal.addBatch();
                }

                deleteNormal.executeBatch();
            }

            while(rrh.getFailedRecnums().size() > 0)
            {
                String failRecNumList = rrh.getFailedRecNumList(250);
                String records[] = failRecNumList.split(",");
                // Add the retry limit for failed computations
                if (doRetryFailed && maxRetries > 0)
                {
                    log.trace("updating failed records based on retry count");
                    for( String rec: records ){
                        if( "".equalsIgnoreCase(rec) ) continue;
                        deleteFailedAfterMaxRetries.setLong(1,Long.parseLong(rec.trim()));
                        deleteFailedAfterMaxRetries.setInt(2,maxRetries);
                        deleteFailedAfterMaxRetries.addBatch();
                    }
                    deleteFailedAfterMaxRetries.executeBatch();
                    log.info("deleted failed recs past retry in ({})", failRecNumList);
                    for( String rec: records){
                        if( "".equalsIgnoreCase(rec) ) continue;
                        updateFailedRetry.setLong(1,Long.parseLong(rec.trim()));
                        updateFailedRetry.setInt(2, maxRetries);
                        updateFailedRetry.addBatch();

                    }
                    updateFailedRetry.executeBatch();
                    log.info("updated fail time on ({})", failRecNumList);
                }
                else
                {
                    // DB V5 handles failed computations by setting a FAIL_TIME
                    // on the task list record. Previous version just delete record.
                    if( tsdbVersion >= 4 && doRetryFailed ) {
                        log.trace("updating failed records");
                        for( String rec: records ){
                            updateFailTime.setLong(1, Long.parseLong(rec.trim()));
                            updateFailTime.execute();
                        }
                        updateFailTime.executeBatch();
                         log.info("updated fail time on ({})", failRecNumList);
                    } else {
                        for( String rec: records ){
                            if( "".equalsIgnoreCase(rec) ) continue;
                            deleteNormal.setLong(1, Long.parseLong(rec.trim()));
                            deleteNormal.addBatch();
                        }

                        deleteNormal.executeBatch();
                         log.info("deleted failed records: ({})", failRecNumList);
                    }
                    commit();
                }

            }

        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error releasing new Data.", ex);
        }
    }

    /**
     * Called when allowed by properties and when the tasklist is empty.
     * Enabled by Decodes Setting reclaimTasklistSec. The default is 0 meaning
     * to never reclaim the tasklist. If set to a positive number of seconds,
     * then compproc will attempt to reclaim space this often, and only when the
     * tasklist is empty.
     */
    public void reclaimTasklistSpace(TimeSeriesDAI dao)
        throws DbIoException
    {
        if (isOracle()
         && reclaimTasklistSec > 0
         && System.currentTimeMillis() - lastReclaimMsec > (reclaimTasklistSec*1000L))
        {
             lastReclaimMsec = System.currentTimeMillis();
             log.debug("Reclaiming unused CP_COMP_TASKLIST space...");
             dao.doQuery("ALTER TABLE cp_comp_tasklist ENABLE ROW MOVEMENT");
             dao.doQuery("ALTER TABLE cp_comp_tasklist SHRINK SPACE CASCADE");
             dao.doQuery("ALTER TABLE cp_comp_tasklist DISABLE ROW MOVEMENT");
        }
        // Unnecessary for PostgreSQL because auto-vacuum should be on
    }

    /**
     * Sets the model run id for subsequent write operations of modeled data.
     * This default implementation simply sets a protected internal integer.
     */
    public void setWriteModelRunId( int modelRunId )
    {
        this.writeModelRunId = modelRunId;
    }

    @Override
    public int getWriteModelRunId()
    {
        return writeModelRunId;
    }

    /**
     * Sets the test mode flag.
     * @param tf the flag value.
     */
    public void setTestMode(boolean tf)
    {
        testMode = tf;
    }

    /**
     * @return a label with which to describe the abstract 'table selector'
     */
    public String getTableSelectorLabel()
    {
        return "Table Selector";
    }

    /**
     * Validate the passed information to make sure it represents a valid
     * parameter within this database. If not, throw ConstraintException.
     */
    public abstract void validateParm(DbKey siteId, String dtcode,
        String interval, String tabSel, int modelId)
        throws ConstraintException, DbIoException;

    /**
     * Returns the maximum valid run-id for the specified model.
     * This is only used in HDB. Base class always returns -1.
     * @param modelId the ID of the model
     * @return the maximum valid run-id for the specified model.
     */
    public int findMaxModelRunId(int modelId)
        throws DbIoException
    {
        return Constants.undefinedIntKey;
    }



    /**
     * Finds the correct coefficient stored in the database for a specific set
     * of sdi, table selector, interval, and date. Assumes Oct 1 as beginning
     * of year.
     * @param sdi the Site datatype id
     * @param ts the table selector
     * @param interval the interval
     * @param date the date of interest
     * @return the value of the coefficient
     * @throws DbCompException
     */
    public double getCoeff(DbKey sdi, String ts, String interval,
        Date date)
        throws DbIoException, DbCompException
    {
        throw new DbIoException("Method getCoeff not implemented.");
    }

    /**
     * Given a data type code of unknown standard, attempt to
     * interpret it as an existing data type in the database.
     * @return the data type ID if successful
     */
    public DataType lookupDataType(String dtcode)
        throws DbIoException, NoSuchObjectException
    {
        DataTypeDAI dtDao = this.makeDataTypeDAO();

        try
        {
            return dtDao.lookupDataType(dtcode);
        }
        finally
        {
            dtDao.close();
        }
    }

    /**
     * Used to present user with a list of valid datatypes
     * for a given site. Returns 2-dimensional array of Strings suitable
     * for populating a table from which the user can select.
     * The first row of the table (i.e. r[0]) must contain the column
     * header strings.
     * The first column must contain data-type codes. Residual columns
     * can contain other descriptive info about the data type.
     * @param siteId the ID of the site
     * @return 2-dimensional array of strings, containing data types.
     */
    public ArrayList<String[]> getDataTypesForSite(DbKey siteId, DaiBase dao)
        throws DbIoException
    {
        // Default impl here just returns an empty array with 1 column
        // labeled "Data Type".

        String header[] = new String[1];
        header[0] = "Data Type";
        ArrayList<String[]> ret = new ArrayList<String[]>();
        ret.add(header);
        return ret;
    }

    /** @return label to use for 'limit' column in tables. */
    public String getLimitLabel() { return "Lim"; }

    /**
     * @return character representation of limit status represented in the flag bits.
     */
    public String flags2LimitCodes(int flags)
    {
        return "";
    }

    /** @return label to use for 'revision' column in tables. */
    public String getRevisionLabel() { return "Rev"; }

    /**
     * @return string representation of revision status represented in the flag bits.
     */
    public String flags2RevisionCodes(int flags)
    {
        return null;
    }


    public DbKey getDataSourceId(DbKey appId, DbComputation comp)
        throws DbIoException
    {
        return DbKey.NullKey;
    }

    /** @return the Time Series Database Version */
    public int getTsdbVersion() { return tsdbVersion; }

    /** @return the DECODES Database Version */
    public int getDecodesDatabaseVersion()
    {
        return decodesDatabaseVersion;
    }

    public static void determineTsdbVersion(Connection con, TimeSeriesDb tsdb)
    {
        try
        {
            DatabaseMetaData metaData = con.getMetaData();
            String dbName = metaData.getDatabaseProductName();
            log.info("Connected to database server: {} {}", dbName, metaData.getDatabaseProductVersion());
            tsdb._isOracle = dbName.toLowerCase().contains("oracle");
        }
        catch (SQLException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("determineTsdbVersion() Cannot determine Database Product name and/or version");
        }

        TimeZone tz = TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone);
        String writeFmt = DecodesSettings.instance().sqlDateFormat;
        String readFmt = DecodesSettings.instance().SqlReadDateFormat;
        if (tsdb._isOracle)
        {
            tsdb.oracleDateParser = tsdb.makeOracleDateParser(tz);
            writeFmt = "'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')";
            readFmt = "yyyy-MM-dd HH:mm:ss";
        }
        tsdb.writeDateFmt = new SimpleDateFormat(writeFmt);
        tsdb.writeDateFmt.setTimeZone(tz);
        tsdb.readDateFmt = new SimpleDateFormat(readFmt);
        tsdb.readDateFmt.setTimeZone(tz);
        tsdb.readCal = Calendar.getInstance(tz);

        SqlDatabaseIO.readVersionInfo(tsdb, con);
        readVersionInfo(tsdb, con);
        log.info("Connected to TSDB Version {}, Description:", tsdb.tsdbVersion, tsdb.tsdbDescription);
        tsdb.readTsdbProperties(con);
        tsdb.cpCompDepends_col1 = tsdb.isHdb() || tsdb.tsdbVersion >= TsdbDatabaseVersion.VERSION_9
            ? "SITE_DATATYPE_ID" : "TS_ID";

    }

    public OracleDateParser makeOracleDateParser(TimeZone tz)
    {
        return new OracleDateParser(tz);
    }

    public static void readVersionInfo(DatabaseConnectionOwner dco, Connection conn)
    {
        /*
          Attempt to read the database's version number.
        */
        int tsdbVersion = TsdbDatabaseVersion.VERSION_2;  // earliest possible value.
        String tsdbDescription = "";
        String q = "SELECT * FROM tsdb_database_version";
        try (Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery(q);)
        {
            while (rs.next())
            {
                int v = rs.getInt(1);
                if (v > tsdbVersion)
                {
                    tsdbVersion = v;
                    tsdbDescription = rs.getString(2);
                }
            }
        }
        catch(Exception ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("readVersionInfo() Cannot determine TimeSeries Database version. Assuming {}",
                    TsdbDatabaseVersion.VERSION_2);
            tsdbVersion = TsdbDatabaseVersion.VERSION_2;  // earliest possible value.
        }
        dco.setTsdbVersion(tsdbVersion, tsdbDescription);
    }

    public void readTsdbProperties(Connection con)
    {
        String q = "SELECT prop_name, prop_value FROM tsdb_property";
        log.trace("Query1 '{}'",q);
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(q);)
        {
            while (rs != null && rs.next())
            {
                String nm = rs.getString(1);
                String vl = rs.getString(2);
                setProperty(nm, vl);
            }
        }
        catch(Exception ex)
        {
            log.atWarn().setCause(ex).log("readTsdbProperties() Cannot read TimeSeries Database properties.");
        }
    }

    public void writeTsdbProperties(Properties props)
        throws DbIoException
    {
        if (props == null)
            return;

        DaiBase dao = new DaoBase(this,"writeTsdbProperties");
        try
        {
            for(Object keyo : props.keySet())
            {
                String key = (String)keyo;
                String val = props.getProperty(key);
                if (val != null)
                    setProperty(key, val);
                else
                    props.remove(key);
                String q = "delete from tsdb_property where prop_name = " + sqlString(key);
                dao.doModify(q);
                if (val != null)
                {
                    q = "insert into tsdb_property values(" + sqlString(key)
                        + ", " + sqlString(val) + ")";
                    dao.doModify(q);
                }
            }
        }
        finally
        {
            dao.close();
        }
    }

    public Site getSiteById(DbKey id)
        throws DbIoException, NoSuchObjectException
    {
        SiteDAI siteDao = makeSiteDAO();
        try { return siteDao.getSiteById(id); }
        finally
        {
            siteDao.close();
        }
    }

    /**
     * Adds a property to this object's meta-data.
     * @param name the property name.
     * @param value the property value.
     */
    public void setProperty(String name, String value)
    {
        props.setProperty(name, value);
    }

    /**
     * Retrieve a property by name.
     * @param name the property name.
     * @return value of name property, or null if not defined.
     */
    public String getProperty(String name)
    {
        return props.getProperty(name);
    }

    /**
     * @return enumeration of all names in the property set.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration getPropertyNames()
    {
        return props.keys();
    }

    /**
     * The first string is the label for site/location
     * The second string is the label for data type/param
     *
     * @return
     */
    public abstract String[] getTsIdParts();

    /**
     * Removes a property assignment.
     * @param name the property name.
     */
    public void rmProperty(String name)
    {
        props.remove(name);
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void debug(String msg)
    {
        /* to be removed */
    }
    
    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void debug1(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void debug2(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void debug3(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void info(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void warning(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void failure(String msg)
    {
        /* to be removed */
    }

    /**
     *
     * @deprecated use class logger instance
     */
    @Deprecated
    public void fatal(String msg)
    {
        /* to be removed */
    }

    /**
     * Construct a new TSID object appropriate for this DB, but do no I/O.
     * @return new, empty TSID object.
     */
    public abstract TimeSeriesIdentifier makeEmptyTsId();

    public TimeSeriesIdentifier makeTsId(String uniqueString) throws BadTimeSeriesException
    {
        TimeSeriesIdentifier tsId = this.makeEmptyTsId();
        tsId.setUniqueString(uniqueString);
        tsId.setStorageUnits(this.getStorageUnitsForDataType(tsId.getDataType()));
        return tsId;
    }


    /**
     * @return number of computations that are using the passed group ID.
     */
    public int countCompsUsingGroup(DbKey groupId)
        throws DbIoException
    {
        TsGroupDAI tsGroupDAO = makeTsGroupDAO();
        try
        {
            return tsGroupDAO.countCompsUsingGroup(groupId);
        }
        finally
        {
            tsGroupDAO.close();
        }
    }

    /**
     * This method does the transformation of the unique string for the
     * time-series identifier.
     * IT MUST DO NO DATABASE I/O!
     * @param tsidRet the time-series identifier to transform
     * @param parm the parameter to transform by
     * @return true if transformed, false if not.
     */
    public abstract boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
        DbCompParm parm);

    public String getDbUser() { return dbUser; }

    @Override
    public boolean isCwms() { return false; }

    @Override
    public boolean isHdb() { return false; }

    @Override
    public boolean isOpenTSDB() { return false; }


    public ArrayList<String> listParamTypes()
        throws DbIoException
    {
        return new ArrayList<String>();
    }


    /**
     * @param dataTypeStandard
     * @return
     */
    public String[] getDataTypesByStandard(String dataTypeStandard)
      throws DbIoException
    {
        ArrayList<String> ret = new ArrayList<String>();

        String q = "SELECT code FROM DataType where lower(standard) = lower("
            + sqlString(dataTypeStandard) + ")";
        try (DaoBase dao = new DaoBase(this, "tsdb.getDataTypesByStd");
             ResultSet rs = dao.doQuery(q);)
        {
            while (rs.next())
                ret.add(rs.getString(1));
        }
        catch (SQLException ex)
        {
            log.atWarn().setCause(ex).log("SQL Error in query '{}'", q);
        }

        String retString[] = new String[ret.size()];
        int x = 0;
        for (String code : ret)
        {
            retString[x] = code;
            x++;
        }
        // Sort the list Alphabetically
        java.util.Arrays.sort(retString);

        return retString;
    }

    public String[] getParamTypes() throws DbIoException { return null; }

    /**
     * Passed one of the 'part' specifiers of a TimeSeriesIdentifier. If the
     * database defines a limited set of valid choices, return the list.
     * If no list is defined, return null
     * @param part the TimeSeriesIdentifier part
     * @return valid list of choices, or null if no list defined
     * @throws DbIoException
     */
    public String[] getValidPartChoices(String part)
    {
        IntervalDAI intervalDAO = this.makeIntervalDAO();
        try
        {
            if (part.equalsIgnoreCase("interval"))
                return intervalDAO.getValidIntervalCodes();
            if (part.equalsIgnoreCase("duration"))
                return intervalDAO.getValidDurationCodes();
        }
        finally
        {
            intervalDAO.close();
        }
        return null;
    }

    @Override
    public UnitConverter makeUnitConverterForRead(CTimeSeries cts)
    {
        UnitConverter ret = null;
        TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
        if (tsid != null)
        {
            if (cts.getUnitsAbbr() != null
             && tsid.getStorageUnits() != null
             && !cts.getUnitsAbbr().equalsIgnoreCase(tsid.getStorageUnits()))
            {
                ret = Database.getDb().unitConverterSet.get(
                    EngineeringUnit.getEngineeringUnit(tsid.getStorageUnits()),
                    EngineeringUnit.getEngineeringUnit(cts.getUnitsAbbr()));
            }
            else if (cts.getUnitsAbbr() == null)
                cts.setUnitsAbbr(tsid.getStorageUnits());
        }
        return ret;
    }

    /**
     * Passed a time series with valid meta data (tsid).
     * Determine the computations that depend on this time series.
     * Add these to the internal dependency list in the time-series.
     * @param cts The timeseries to add computation dependencies to
     * @param loadingAppId which instance of compproc
     * @param dao The current Dao to handle the query
     * @return
     */
    public int fillDependentCompIds(CTimeSeries cts, DbKey loadingAppId, DaoBase dao)
    {
        cts.getDependentCompIds().clear();
        String q = "select a.computation_id from cp_comp_depends a, cp_computation b"
            + " where a." + cpCompDepends_col1 + " = ?"
            + " and a.computation_id = b.computation_id"
            + " and b.loading_application_id = ?";
        try
        {
            dao.doQuery(q, rs ->
            {
                cts.addDependentCompId(DbKey.createDbKey(rs, 1));
            },
            cts.getTimeSeriesIdentifier().getKey(), loadingAppId);
        }
        catch(Exception ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("Unable to fill dependent comp IDs using Query '{}'", q);
        }
        return cts.getDependentCompIds().size();
    }

    /**
     * Given a unique time-series identifier string, make a CTimeSeries
     * object, populated with meta-data from the database.
     * @param tsidStr the unique string identifying this time series.
     * @return The CTimeSeries object
     * @throws DbIoException on database I/O error
     * @throws NoSuchObjectException if no such time series exists in the database.
     */
    public CTimeSeries makeTimeSeries(String tsidStr)
        throws DbIoException, NoSuchObjectException
    {
        TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO();
        try
        {
            TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsidStr);
            return makeTimeSeries(tsid);
        }
        finally
        {
            timeSeriesDAO.close();
        }
    }

    /**
     * Given a time-series identifier, make a CTimeSeries
     * object, populated with meta-data from the database.
     * @param tsid the time series identifier
     * @return The CTimeSeries object
     * @throws DbIoException on database I/O error
     * @throws NoSuchObjectException if no such time series exists in the database.
     */
    public CTimeSeries makeTimeSeries(TimeSeriesIdentifier tsid)
        throws DbIoException, NoSuchObjectException
    {
        DbKey sdi = isHdb() ? ((HdbTsId)tsid).getSdi() : tsid.getKey();
        CTimeSeries ret = new CTimeSeries(sdi, tsid.getInterval(),
            tsid.getTableSelector());
        ret.setTimeSeriesIdentifier(tsid);
        ret.setDisplayName(tsid.getDisplayName());
        if (isHdb())
        {
            String s = tsid.getPart(HdbTsId.MODELID_PART);
            try
            {
                if (s != null)
                    ret.setModelId(Integer.parseInt(s));
            }
            catch(Exception ex)
            {
                log.atWarn().setCause(ex).log("makeTimeSeries - Bad modelId '{}' -- ignored.", s);
            }
            s = tsid.getPart(HdbTsId.MODELRUNID_PART);
            try
            {
                if (s != null)
                    ret.setModelRunId(Integer.parseInt(s));
                else
                    ret.setModelRunId(findMaxModelRunId(ret.getModelId()));
            }
            catch(Exception ex)
            {
                 log.atWarn().setCause(ex).log("makeTimeSeries - Bad modelRunId '{}' -- ignored.", s);
            }
        }
        return ret;
    }

    /**
     * Use database-specific flag definitions to determine whether the
     * passed variable should be considered 'questionable'.
     * @param v the variable whose flags to check
     * @return true if flags are questionable, false if okay.
     */
    public boolean isQuestionable(NamedVariable v)
    {
        // The base class implementation here always returns false.
        return false;
    }

    /**
     * Use database-specific flag definitions to set the passed variable
     * as 'questionable'.
     * @param v the variable whose flags to set
     */
    public void setQuestionable(Variable v)
    {
        // The base class implementation here does nothing.
        return;
    }

    public void setTsdbVersion(int version, String description)
    {
        this.tsdbVersion = version;
        this.tsdbDescription = description;
    }

    public void setDecodesDatabaseVersion(int version, String options)
    {
        this.decodesDatabaseVersion = version;
        this.decodesDatabaseOptions = options;
    }

    @Override
    public EnumDAI makeEnumDAO()
    {
        return new EnumSqlDao(this);
    }

    @Override
    public PropertiesDAI makePropertiesDAO()
    {
        return new PropertiesSqlDao(this);
    }

    @Override
    public DataTypeDAI makeDataTypeDAO()
    {
        return new DataTypeDAO(this);
    }

    public boolean isOracle()
    {
        return _isOracle;
    }

    public Date getFullDate(ResultSet rs, int column)
    {
        if (oracleDateParser != null)
        {
            return oracleDateParser.getTimeStamp(rs, column);
        }
        try
        {
            java.sql.Timestamp ts = rs.getTimestamp(column, readCal);
            if (rs.wasNull())
                return null;
            else
            {
                Date d = new Date(ts.getTime());
                return d;
            }
        }
        catch(SQLException ex)
        {
            String s = "";
            try
            {
                s = rs.getString(column);
                return readDateFmt.parse(s);
            }
            catch(Exception ex2)
            {
                ex2.addSuppressed(ex);
                log.atWarn().setCause(ex2).log("Bad date format '{}' (using default): ", s);
                return null;
            }
        }
    }

    @Override
    public String getDatabaseTimezone()
    {
        return databaseTimezone;
    }

    @Override
    public DbKey getAppId()
    {
        return appId;
    }

    public void setAppId(DbKey appId)
    {
        this.appId = appId;
    }

    /**
    * Returns an SQL representation of an optional Date / timestamp
    * value.  If the argument is null, this returns the string value
    * "NULL" (without the quotes).  If the argument is not null, then
    * this returns a formated version of the date/time, enclosed in
    * single quotes.
    * @param d the date value
    * @return String suitable for use in a SQL statement
    */
    public String sqlDate(Date d)
    {
        if (d == null)
            return "NULL";
        String ts = writeDateFmt.format(d);
        if (ts.startsWith("to_date"))
            return ts;
        return "'" + ts + "'";
    }

    @Override
    public SimpleDateFormat getLogDateFormat() { return debugDateFmt; }

    @Override
    public SiteDAI makeSiteDAO()
    {
        return new SiteDAO(this);
    }

    public XmitRecordDAO makeXmitRecordDao(int maxDays)
    {
        return new XmitRecordDAO(this, maxDays);
    }

    @Override
    public LoadingAppDAI makeLoadingAppDAO()
    {
        return new LoadingAppDao(this);
    }

    @Override
    public AlgorithmDAI makeAlgorithmDAO()
    {
        return new AlgorithmDAO(this);
    }

    @Override
    public TsGroupDAI makeTsGroupDAO()
    {
        return new TsGroupDAO(this);
    }

    @Override
    public ComputationDAI makeComputationDAO()
    {
        return new ComputationDAO(this);
    }

    @Override
    public CompDependsDAI makeCompDependsDAO()
    {
        return new CompDependsDAO(this);
    }

    @Override
    public CompDependsNotifyDAI makeCompDependsNotifyDAO()
    {
        return new CompDependsNotifyDAO(this);
    }

    @Override
    public PlatformStatusDAI makePlatformStatusDAO()
    {
        return new PlatformStatusDAO(this);
    }

    @Override
    public DeviceStatusDAI makeDeviceStatusDAO()
    {
        return new DeviceStatusDAO(this);
    }

    @Override
    public DacqEventDAI makeDacqEventDAO()
    {
        return new DacqEventDAO(this);
    }

    @Override
    public ScreeningDAI makeScreeningDAO()
        throws DbIoException
    {
        // This is a CWMS thing. So Base class returns null.
        return null;
    }

    public GroupHelper makeGroupHelper()
    {
        return null;
    }

    @Override
    public ArrayList<TimeSeriesIdentifier> expandTsGroup(TsGroup tsGroup)
        throws DbIoException
    {
        GroupHelper groupHelper = makeGroupHelper();
        if (groupHelper == null)
            return new ArrayList<TimeSeriesIdentifier>();
        groupHelper.expandTsGroup(tsGroup);
        return tsGroup.getExpandedList();
    }

    /**
     * Perform a database-specific rating. This method should be overloaded by concrete
     * database class if the database supports rating.
     * @param specId unique string that identifies the rating table in the database
     * @param indeps array of independent parameters
     * @return the output of the rating
     * @throws DbCompException if no such rating or other problem with retrieving the rating.
     * @throws RangeException if any of the indep values are outside the rating range
     */
    public double rating(String specId, Date timeStamp, double... indeps)
        throws DbCompException, RangeException
    {
        throw new DbCompException("Rating not supported in this database.");
    }

    public ArrayList<String> listVersions()
        throws DbIoException
    {
        return new ArrayList<String>();
    }

    @Override
    public AlarmDAI makeAlarmDAO()
    {
        return new AlarmDAO(this);
    }

    /**
     * Given a datatype, return the default storage units for that data type
     * in this database. CWMS and HDB implement this differently. The default
     * impl here always returns null.
     * @param dt the data type
     * @return storage units abbreviation or null if it can't be determined.
     */
    public String getStorageUnitsForDataType(DataType dt)
    {
        return null;
    }

    /**
     * Convert a time series flag value into a character representation. Flag bits
     * are defined differently in the underlying databases.
     * @param flags the integer flag of a time series value
     * @return A string representation for display
     */
    public String flags2display(int flags)
    {
        // Base class does nothing.
        return "";
    }


    @Override
    public void freeConnection(Connection conn)
    {
        try
        {
            conn.close();
        }
        catch (SQLException ex)
        {
            log.atError().setCause(ex).log("unable to close connection.");
        }
    }

}