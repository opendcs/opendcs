/*
 * $Id: SqlDatabaseIO.java,v 1.15 2020/02/14 15:13:44 mmaloney Exp $
 *
 * Open Source Software
 *
 * $Log: SqlDatabaseIO.java,v $
 * Revision 1.15  2020/02/14 15:13:44  mmaloney
 * Implement isOpenTSDB
 *
 * Revision 1.14  2019/12/11 14:34:02  mmaloney
 * Support OS authentication for HDB (issue 771)
 *
 * Revision 1.13  2019/06/10 19:24:41  mmaloney
 * code cleanup
 *
 * Revision 1.12  2018/12/19 19:56:30  mmaloney
 * Remove references to classes in oracle.jdbc and oracle.sql, except in HDB branch.
 *
 * Revision 1.11  2018/02/21 14:33:03  mmaloney
 * Set autocommit true always.
 *
 * Revision 1.10  2017/03/03 19:14:01  mmaloney
 * Remove commit() code. Now just a stub. Everything now is autocommit.
 *
 * Revision 1.9  2016/10/01 14:59:56  mmaloney
 * CWMS-8979 Allow DecodesSettings params in database to override the file(s).
 *
 * Revision 1.8  2016/03/24 19:08:18  mmaloney
 * Refactor: Have expandSDI return the TimeSeriesID that it uses. This saves the caller from
 * having to re-look it up. Needed for PythonAlgorithm.
 *
 * Revision 1.7  2015/11/12 15:22:13  mmaloney
 * Added makeScreeningDAO method.
 *
 * Revision 1.6  2015/01/22 19:52:07  mmaloney
 * log message improvements
 *
 * Revision 1.5  2014/12/11 20:29:11  mmaloney
 * Added DacqEventLogging capability.
 *
 * Revision 1.4  2014/11/19 16:09:23  mmaloney
 * Additions for dcpmon
 *
 * Revision 1.3  2014/08/29 18:22:50  mmaloney
 * 6.1 Schema Mods
 *
 * Revision 1.2  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.28  2013/06/26 19:35:49  mmaloney
 * Don't print stack trace if fail to write due to "insufficient privilege".
 * This is for CWMS which enforces privileges based on the database user ID.
 *
 * Revision 1.27  2013/04/22 16:38:48  mmaloney
 * In dt equivalence, I can get bogus equivalencies because the referenced datatype is in a different office.
 *
 * Revision 1.26  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.sql;

import static org.slf4j.helpers.Util.getCallingClass;

import decodes.cwms.CwmsSqlDatabaseIO;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.*;
import decodes.hdb.HdbSqlDatabaseIO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import ilex.util.EnvExpander;
import org.opendcs.authentication.AuthSourceService;

import org.slf4j.LoggerFactory;

import opendcs.dai.AlarmDAI;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.CompDependsNotifyDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.DeviceStatusDAI;
import opendcs.dai.EnumDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dai.XmitRecordDAI;
import opendcs.dao.AlarmDAO;
import opendcs.dao.AlgorithmDAO;
import opendcs.dao.ComputationDAO;
import opendcs.dao.DacqEventDAO;
import opendcs.dao.DataTypeDAO;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.DeviceStatusDAO;
import opendcs.dao.EnumSqlDao;
import opendcs.dao.LoadingAppDao;
import opendcs.dao.PlatformStatusDAO;
import opendcs.dao.PropertiesSqlDao;
import opendcs.dao.ScheduleEntryDAO;
import opendcs.dao.SiteDAO;
import opendcs.dao.TsGroupDAO;
import opendcs.dao.XmitRecordDAO;
import opendcs.util.sql.WrappedConnection;
import ilex.util.AuthException;
import ilex.util.Logger;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;


/**
 * This class allows you to read database information from a Postgres SQL
 * database.
 */
public class SqlDatabaseIO
    extends DatabaseIO
    implements DatabaseConnectionOwner
{
    private static org.slf4j.Logger log = LoggerFactory.getLogger(getCallingClass());
    /**
     * The "location" of the SQL database, as passed into the constructor.
     * This is the full string from either the "DatabaseLocation" or the
     * "EditDatabaseLocation" property.
     * For example, "jdbc:postgresql:decodessample:testuser"
     */
    protected String _sqlDbLocation;

    /** Holds the object describing the connection to the database. */
    private Connection _conn = null;

    /** This is used to read and write the EngineeringUnit table. */
    private EngineeringUnitIO _engineeringUnitIO;

    /** This is used to read and write the UnitConverter table. */
    UnitConverterIO _unitConverterIO;

    /**
     * This holds the sql.NetworkListListIO object that's used to read
     * and write the NetworkListList.
     */
    private NetworkListListIO _networkListListIO;

    /**
     * This holds the sql.ConfigListIO object that's used to read and
     * write the PlatformConfigList.
     */
    protected ConfigListIO _configListIO;

    /**
     * This holds the sql.EquipmentModelListIO object that's used to read
     * and write the EquipmentModelList.
     */
    protected EquipmentModelListIO _equipmentModelListIO;

    /**
     * This holds the sql.PlatformListIO object that's used to read and
     * write the PlatformList.
     */
    protected PlatformListIO _platformListIO;

    /**
     * This holds the sql.RoutingSpecListIO object that's used to read and
     * write the RoutingSpecList.
     */
    private RoutingSpecListIO _routingSpecListIO;

    /**
     * This holds the sql.DataSourceListIO object that's used to read and
     * write the DataSourceList.
     */
    DataSourceListIO _dataSourceListIO;

    /**
     * This holds the sql.PresentationGroupListIO object that's used to read
     * and write the PresentationGroupList.
     */
    private PresentationGroupListIO _presentationGroupListIO;

    /** DECODES Database Version */
    protected int databaseVersion;

    /** Bit fields determine options used by this database. */
    protected String databaseOptions;

    /** TSDB Database Version, if this is a TSDB */
    protected int tsdbVersion = TsdbDatabaseVersion.VERSION_2;

    /// Used for legacy databases that don't have LMT.
    private long lastLMT;

    /// Used to generate surrogate keys for newly created records.
    protected KeyGenerator keyGenerator;

    /** Ingres requires COMMIT after each block of SELECTs, and will set the
     * following to true. Default = false.
     */
    protected boolean commitAfterSelect;

    /** User name used to connect to the database. */
    protected String _dbUser;

    /** Kludge for Oracle DATE data types - the database time zone: */
    protected String databaseTimeZone = "UTC";

    protected boolean _isOracle = false;

    protected Calendar readCal = null;
    private OracleDateParser oracleDateParser = null;
    /** Used to format timestamps written to the database. */
    protected SimpleDateFormat writeDateFmt = null;

    /** Used to format timestamps read from the database. */
    protected SimpleDateFormat readDateFmt = null;

    /** Used for log messages */
    protected SimpleDateFormat debugDateFmt = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss z");

    private boolean _isConnected = false;

    /** Facilitates OPENTSDB and HDB connection pooling. CWMS is handled differently. */
    private javax.sql.DataSource poolingDataSource = null;

    /**
     * Default constructor -- all initialization that doesn't depend on
     * a database connection goes here.
     */
    public SqlDatabaseIO()
    {
        // Initialize the child IO objects
        // Their are dependencies among them, which should be enforced
        // by their various constructors.  For example, the PlatformListIO
        // can't be instantiated until after the ConfigListIO and the
        // SiteIO objects are created.

        _engineeringUnitIO = new EngineeringUnitIO(this);
        _unitConverterIO = new UnitConverterIO(this);
        _networkListListIO = new NetworkListListIO(this);
        _configListIO = new ConfigListIO(this, _unitConverterIO);
        _equipmentModelListIO = new EquipmentModelListIO(this);
        _platformListIO = new PlatformListIO(this, _configListIO, _equipmentModelListIO);
        _dataSourceListIO = new DataSourceListIO(this);
        _presentationGroupListIO = new PresentationGroupListIO(this);
        _routingSpecListIO = new RoutingSpecListIO(this, _presentationGroupListIO, _networkListListIO,
            _dataSourceListIO);

        // Truncate lastLMT back to half-hour boundary
        lastLMT = (System.currentTimeMillis() / 1800000L) * 1800000L;
        commitAfterSelect = false;
    }

    /**
     * Constructor.  The argument, sqlDbName, is the "location" of the
     * database.  In the case of a SQL database, this is the name of
     * the database, as used in the JDBC getConnection method.
    * @param sqlDbLocation the location string from decodes.properties file
     */
    public SqlDatabaseIO(String sqlDbLocation)
        throws DatabaseException
    {
        this();

        connectToDatabase(sqlDbLocation);
        keyGenerator = KeyGeneratorFactory.makeKeyGenerator(
            DecodesSettings.instance().sqlKeyGenerator);
    }

    /**
     * Initialize the database connection using the username and password
     * provided in the hidden DECODES auth file.
     * @param sqlDbLocation URL (may vary for different DBs)
     */
    public void connectToDatabase(String sqlDbLocation)
        throws DatabaseException
    {
        // Placeholder for connecting from web where connection is from a DataSource.
        if (sqlDbLocation == null || sqlDbLocation.trim().length() == 0)
            return;
        _sqlDbLocation = sqlDbLocation;

        String driverClass = DecodesSettings.instance().jdbcDriverClass;
        // Load the JDBC Driver Class
        try
        {
            log.trace("Initializing driver class '{}'", driverClass);
            Class.forName(driverClass).newInstance();
            log.trace("...success.");
        }
        catch (Exception ex)
        {
            String msg = String.format("Cannot load JDBC driver class '%s'", driverClass);
            log.atError().setCause(ex).log(msg);
            throw new DatabaseConnectException(msg, ex);
        }

        // Try to authenticate using OS authentication (IDENT)
        if (DecodesSettings.instance().tryOsDatabaseAuth)
        {
            Properties emptyProps = new Properties();
            log.trace("Trying OS authentication with location '{}'", _sqlDbLocation);
            try
            {
                _conn = DriverManager.getConnection(_sqlDbLocation, emptyProps);
                log.trace("...Success.");
            }
            catch(SQLException ex)
            {
                log.info("SqlDatabaseIO Connection using OS authentication failed. "
                    + "Will attempt username/password auth.", ex);
                _conn = null;
            }
        }

        if (_conn == null)
        {
            // Retrieve username and password for database
            String authFileName = DecodesSettings.instance().DbAuthFile;
            Properties credentials = null;
            try
            {
                credentials = AuthSourceService.getFromString(authFileName)
                                               .getCredentials();
            }
            catch(AuthException ex)
            {
                String msg = "Cannot read username and password from '{}'. Run setDecodesUser first.";
                log.atError()
                   .setCause(ex)
                   .log(msg, EnvExpander.expand(authFileName));
                throw new DatabaseConnectException(msg, ex);
            }

            connectUserPassword(credentials.getProperty("username"),
                                credentials.getProperty("password"));
        }

        // MJM 2018-2/21 Force autoCommit on.
        try { _conn.setAutoCommit(true);}
        catch(SQLException ex)
        {
            log.warn("Cannot set SQL AutoCommit to true: ", ex);
        }

        determineVersion(_conn);

        try {
            setDBDatetimeFormat(_conn);
        } catch (SQLException ex) {
            log.warn("Unable to set DB Date/Time format", ex);
        }

        postConnectInit();
    }

    private void connectUserPassword(String user, String pw)
        throws DatabaseException
    {
        _dbUser = user;

        // MJM Try for up to 30 sec to initialize the database because
        // this might be a service and we need to wait for postgres to init.
        long startTry = System.currentTimeMillis();
        _conn = null;
        while(_conn == null)
        {
            try
            {
                log.info("Connecting to {} as user '{}'", _sqlDbLocation, user);

                _conn = DriverManager.getConnection(_sqlDbLocation, user, pw);
            }
            catch (Exception ex)
            {
                _conn = null;
                   if (System.currentTimeMillis() - startTry > 30000L)
                    throw new DatabaseException(
                        "Error getting JDBC connection using driver '"
                        + DecodesSettings.instance().jdbcDriverClass
                        + "' to database at '" + _sqlDbLocation
                        + "' for user '" + user + "': " + ex.toString(), ex);
                else
                {
                    log.debug("JDBC Connection Failed (will retry): ", ex);
                    try { Thread.sleep(5000L); }
                    catch(InterruptedException ex2) {}
                }
            }
        }

    }

    /**
     * A subclass can override this method to perform initialization tasks after
     * a successful database connection.
     */
    protected void postConnectInit()
        throws DatabaseException
    {
        _isConnected = true;
    }

    protected void setDBDatetimeFormat(Connection conn)
        throws SQLException
    {
        if (!_isOracle)
        {
            return;
        }
        String q = null;
        try(Statement stmnt = conn.createStatement())
        {
            q = "SELECT PARAM_VALUE FROM REF_DB_PARAMETER WHERE PARAM_NAME = 'TIME_ZONE'";
            try (ResultSet rs = stmnt.executeQuery(q);)
            {
                if (rs != null && rs.next())
                {
                    databaseTimeZone = rs.getString(1);
                }
            }
            catch(SQLException ex)
            {
                databaseTimeZone = DecodesSettings.instance().sqlTimeZone;
                log.trace("{} -- failed, must not be HDB, using sqlTimeZone setting of '{}'", q, databaseTimeZone);
            }

            q = "ALTER SESSION SET TIME_ZONE = '" + databaseTimeZone + "'";
            Logger.instance().debug3(q);
            stmnt.execute(q);

            q = "ALTER SESSION SET nls_date_format = 'yyyy-mm-dd hh24:mi:ss'";
            Logger.instance().debug3(q);
            stmnt.execute(q);

            q = "ALTER SESSION SET nls_timestamp_format = 'yyyy-mm-dd hh24:mi:ss'";
            Logger.instance().debug3(q);
            stmnt.execute(q);

            q = "ALTER SESSION SET NLS_TIMESTAMP_TZ_FORMAT = 'yyyy-mm-dd hh24:mi:ss'";
            Logger.instance().debug3(q);
            stmnt.execute(q);
        }
    }

    public synchronized void determineVersion(Connection conn)
    {
        try
        {
            String productName = conn.getMetaData().getDatabaseProductName();
            _isOracle = productName.toLowerCase().contains("oracle");
        }
        catch (SQLException ex)
        {
            Logger.instance().warning("SqlDatabaseIO.determineVersion() "
                + "Cannot determine Database Product Name: " + ex);
        }

        TimeZone tz = TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone);
        String writeFmt = DecodesSettings.instance().sqlDateFormat;
        String readFmt = DecodesSettings.instance().SqlReadDateFormat;
        if (_isOracle)
        {
            oracleDateParser = makeOracleDateParser(tz);
            writeFmt = "'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')";
            readFmt = "yyyy-MM-dd HH:mm:ss";
        }
        writeDateFmt = new SimpleDateFormat(writeFmt);
        writeDateFmt.setTimeZone(tz);
        readDateFmt = new SimpleDateFormat(readFmt);
        readDateFmt.setTimeZone(tz);
        readCal = Calendar.getInstance(tz);

        readVersionInfo(this, conn);
        TimeSeriesDb.readVersionInfo(this, conn);
    }

    public static void readVersionInfo(DatabaseConnectionOwner dco, Connection conn)
    {
        /*
          Attempt to read the database's version number.
          Catch exception. Database version < 6 will not have the version
          table.
        */
        int databaseVersion = DecodesDatabaseVersion.DECODES_DB_5;  // earliest possible value.
        String databaseOptions = "";
        Statement stmt = null;
        try
        {
            stmt = conn.createStatement();
            ResultSet rs = null;
            try
            {
                rs = stmt.executeQuery(
                    "SELECT * FROM DecodesDatabaseVersion");
            }
            catch(SQLException ex)
            {
                Logger.instance().warning("Cannot read DecodesDatabaseVersion table. " +
                        "Will attempt legacy DatabaseVersion (" + ex + ")");
                rs = stmt.executeQuery(
                    "SELECT version, options FROM DatabaseVersion");
            }
            while (rs != null && rs.next())
            {
                int v = rs.getInt(1);
                if (v > databaseVersion)
                {
                    databaseVersion = v;
                    databaseOptions = rs.getString(2);
                }
            }
            if (rs != null)
            {
                rs.close();
            }
            stmt.close();
        }
        catch(SQLException ex)
        {
            log.warn("Cannot read DatabaseVersion table. Assuming version 5.", ex);
            databaseVersion = DecodesDatabaseVersion.DECODES_DB_5;
            databaseOptions = "";
        }
        finally
        {
            if (stmt != null)
            {
                try { stmt.close(); }
                catch(Exception ex) {}
            }
        }
        dco.setDecodesDatabaseVersion(databaseVersion, databaseOptions);
        log.info("Connected to DECODES SQL database version {}", databaseVersion);
    }

    /** @return 'SQL'. */
    public String getDatabaseType()
    {
        return "SQL";
    }

    /** @return the location string specified in decodes.properties. */
    public String getDatabaseName()
    {
        return _sqlDbLocation;
    }

    @Override
    public int getDecodesDatabaseVersion()
    {
        return databaseVersion;
    }

    /**
      @return Options contain bit fields for future use.
    */
    public String getDatabaseOptions()
    {
        return databaseOptions;
    }

    /**
      Closes the JDBC connection.
    */
    public void close( )
    {
        try
        {
            log.info("Closing  database connection.");
            if (_conn == null || _conn.isClosed())
            {
                return;
            }
            _conn.close();
        }
        catch (SQLException ex)
        {
            log.warn("Error closing database connection", ex);
        }
    }

    /**
    * Reads the set of known enumeration objects in this database.
    * @param top the EnumList object to populate
    */
    @Override
    public synchronized void readEnumList(EnumList top)
        throws DatabaseException
    {
        EnumDAI enumSqlDao = makeEnumDAO();

        try
        {
            enumSqlDao.readEnumList(top);
        }
        catch (DbIoException ex)
        {
            throw new DatabaseException("Error reading enum list", ex);
        }
        finally
        {
            enumSqlDao.close();
        }
    }

    public synchronized DbEnum readEnum(String enumName)
        throws DatabaseException
    {
        EnumDAI enumSqlDao = makeEnumDAO();
        try
        {
            return enumSqlDao.getEnum(enumName);
        }
        catch (DbIoException ex)
        {
            throw new DatabaseException(String.format("failed to read enum '%s' from database",enumName), ex);
        }
        finally
        {
            enumSqlDao.close();
        }
    }

    /**
    * Reads the set of known data-type objects in this database.
    * Objects in this collection are complete.
    * @param dts the object to populate from the database.
    */
    @Override
    public synchronized void readDataTypeSet(DataTypeSet dts)
        throws DatabaseException
    {

        try (DataTypeDAI dtdao = this.makeDataTypeDAO())
        {
            dtdao.readDataTypeSet(dts);
        }
        catch(DbIoException ex)
        {
            throw new DatabaseException("Failed to read site datatype set", ex);
        }
    }

    /**
      Reads a single data-type object given its numeric key.
      @return data type or null if not found
    */
    @Override
    public synchronized DataType readDataType(DbKey id)
        throws DatabaseException
    {

        try (DataTypeDAI dtdao = this.makeDataTypeDAO())
        {
            return dtdao.getDataType(id);
        }
        catch(DbIoException ex)
        {
            throw new DatabaseException(String.format("Error reading datatype with ID = %d",id.getValue(), ex));
        }
    }

    /**
    * Reads the EngineeringUnitList object and all of the EngineeringUnit
    * objects that that contains -- these are read from the EngineeringUnit
    * table in the SQL database.  This also reads the UnitConverterSet and
    * all the UnitConverterDb objects that that contains -- these are read
    * from the UnitConverter table.
    * <p>
    *   Note that the UnitConverterSet is retrieved from a reference to the
    *   database contained in the euList argument.  That is, the
    *   UnitConverterSet that is populated here is the one from the same
    *   database as the euList.
    * </p>
    * <p>
    *   Objects in these collections are complete.
    * </p>
    * @param euList the object to populate from the database.
    */
    @Override
    public synchronized void readEngineeringUnitList(EngineeringUnitList euList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _engineeringUnitIO.setConnection(conn);
            _engineeringUnitIO.read(euList);

            _unitConverterIO.setConnection(conn);
            _unitConverterIO.read(euList.getDatabase().unitConverterSet);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _engineeringUnitIO.setConnection(null);
            _unitConverterIO.setConnection(null);
        }
    }

    /**
    * Reads the list of Site objects defined in this database.
    * Objects in this list may be only partially populated (key values
    * and primary display attributes only).
    * @param sl the object to populate from the database.
    */
    @Override
    public synchronized void readSiteList(SiteList sl)
        throws DatabaseException
    {

        try (SiteDAI siteDao = makeSiteDAO())
        {
            siteDao.read(sl);
        }
        catch(DbIoException ex)
        {
            log.error("Unable to read site list", ex);
            throw new DatabaseException("Failed to read site list", ex);
        }
    }

    /**
    * Read the platform list cross reference file and populate the passed
    * PlatformList object.
    * @param platformList the object to populate from the database.
    */
    @Override
    public synchronized void readPlatformList(PlatformList platformList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            _platformListIO.read(platformList);
        }
        catch (SQLException ex)
        {
            log.error("Unable to read platform list", ex);
            throw new DatabaseException("Unable to read platform list", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    /**
    * Reads the list of PlatformConfig objects defined in this database.
    * @param pcList the object to populate from the database.
    * @throws DatabaseException if there's an error.
    */
    @Override
    public synchronized void readConfigList(PlatformConfigList pcList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _configListIO.setConnection(conn);
            _configListIO.read(pcList);
        }
        catch (SQLException ex)
        {
            log.error("Unable to read config list", ex);
            throw new DatabaseException("Unable to read config list", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _configListIO.setConnection(null);
        }
    }

    @Override
    public synchronized PlatformConfig newPlatformConfig(PlatformConfig pc, String deviceId,
        String originator)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _configListIO.setConnection(conn);
            return(_configListIO.newPlatformConfig(pc, deviceId, originator));
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable to create new platform config", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _configListIO.setConnection(null);
        }
    }

    /**
    * Returns the list of EquipmentModel objects defined in this database.
    * Objects in this collection are complete.
    * @param eml the object to populate from the database.
    */
    @Override
    public synchronized void readEquipmentModelList(EquipmentModelList eml)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _equipmentModelListIO.setConnection(conn);
            _equipmentModelListIO.read(eml);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable to read equipment list", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _equipmentModelListIO.setConnection(null);
        }
    }

    /**
    * Returns the list of RoutingSpec objects defined in this database.
    * Objects in this list may be only partially populated (key values
    * and primary display attributes only).
    * @param rsList the object to populate from the database.
    */
    @Override
    public synchronized void readRoutingSpecList(RoutingSpecList rsList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _routingSpecListIO.setConnection(conn);
            _routingSpecListIO.read(rsList);
        }
        catch (SQLException ex)
        {
            log.error("Unable to read routing spec list.", ex);
            throw new DatabaseException("Unable to read routing spec list", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _routingSpecListIO.setConnection(null);
        }
    }

    /**
    * Returns the list of DataSource objects defined in this database.
    * Objects in this list may be only partially populated (key values
    * and primary display attributes only).
    * @param dsList the object to populate from the database.
    * @throws DatabaseException if can't list the directory.
    */
    @Override
    public synchronized void readDataSourceList(DataSourceList dsList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _dataSourceListIO.setConnection(conn);
            _dataSourceListIO.read(dsList);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable to read datasource list", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _dataSourceListIO.setConnection(null);
        }
    }

    /**
    * Returns the list of NetworkList objects defined in this database.
    * Objects in this list may be only partially populated (key values
    * and primary display attributes only).
    * @param nlList the object to populate from the database.
    */
    @Override
    public synchronized void readNetworkListList(NetworkListList nlList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _networkListListIO.setConnection(conn);
            _networkListListIO.read(nlList);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable to read network lists", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _networkListListIO.setConnection(null);
        }
    }

    /**
     * Non-cached method to read the list of network list specs currently
     * defined in the database.
     * @return ArrayList of currently defined network list specs.
     */
    @Override
    public synchronized ArrayList<NetworkListSpec> getNetlistSpecs()
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _networkListListIO.setConnection(conn);
            ArrayList<NetworkListSpec> ret =
                _networkListListIO.getNetlistSpecs();
            return ret;
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable retrieve NetListSpecs", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _networkListListIO.setConnection(null);
        }
    }


    /**
    * Returns the list of PresentationGroup objects defined in this
    * database.Objects in this list may be only partially populated
    * (key values and primary display attributes only).
    * @param pgList the object to populate from the database.
    */
    @Override
    public synchronized void readPresentationGroupList(PresentationGroupList pgList)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _presentationGroupListIO.setConnection(conn);
            _presentationGroupListIO.read(pgList);
        }
        catch (SQLException ex)
        {
            log.error("Unable to read presentation groups", ex);
            throw new DatabaseException("Unable to read presentation groups", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _presentationGroupListIO.setConnection(null);
        }
    }


    /**
     * Writes the DataTypeSet to the SQL database.
    * @param dts the object to write to the database.
     */
    @Override
    public void writeDataTypeSet( DataTypeSet dts )
        throws DatabaseException
    {
        Database oldDb = Database.getDb();
        Database.setDb(dts.getDatabase());


        try (DataTypeDAI dtdao = this.makeDataTypeDAO())
        {
            dtdao.writeDataTypeSet(dts);
        }
        catch(Exception ex)
        {
            throw new DatabaseException("Unable to write DataTypeSet", ex);
        }
        finally
        {
            Database.setDb(oldDb);
        }
    }

    /**
     * Writes the DataTypeSet to the SQL database.
    * @param dt the object to write to the database.
     */
    public void writeDataType( DataType dt )
        throws DatabaseException
    {

        try (DataTypeDAI dtdao = this.makeDataTypeDAO())
        {
            dtdao.writeDataType(dt);
        }
        catch(DbIoException ex)
        {
            throw new DatabaseException("Unable to write data type", ex);
        }
    }
    /**
     * Returns the list of UnitConverter objects defined in this database.
     * Objects in this list may be only partially populated (key values
     * and primary display attributes only).
    * @param ucs the object to populate from the database.
     */
    @Override
    public synchronized void readUnitConverterSet(UnitConverterSet ucs)
        throws DatabaseException
    {
    }


    /**
     * Writes the entire collection of engineering units to the database.
    * @param top the object to write to the database.
     */
    @Override
    public synchronized void writeEngineeringUnitList(EngineeringUnitList top)
        throws DatabaseException
    {
        log.info("Writing engineering unit list.");

        Connection conn = null;

        try
        {
            conn = getConnection();
            _engineeringUnitIO.setConnection(conn);
            _unitConverterIO.setConnection(conn);

            _engineeringUnitIO.write(top);
            _unitConverterIO.write(top.getDatabase().unitConverterSet);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writeEngineeringUnitList: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _engineeringUnitIO.setConnection(null);
            _unitConverterIO.setConnection(null);
        }
    }


    /**
      Reads site information. The passed Site object may only be partially
      populated (e.g. from a site list containing names only).
      <p>
      This method searches for a file matching any of the SiteNames assigned
      to this site.
      @param site the object to populate from the database.
    */
    @Override
    public synchronized void readSite(Site site)
        throws DatabaseException
    {
        try (SiteDAI siteDao = makeSiteDAO())
        {
            siteDao.readSite(site);
        }
        catch (Exception ex)
        {
            log.error("Unable to read site.", ex);
            throw new DatabaseException("Unable to read site.", ex);
        }
    }

    /**
      Writes site information back to the database.
      @param site the object to write to the database.
      @throws DatabaseException if no name is assigned to this object or
      if the write fails.
    */
    @Override
    public synchronized void writeSite(Site site)
        throws DatabaseException
    {
        try (SiteDAI siteDao = makeSiteDAO())
        {
            siteDao.writeSite(site);
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to write site {}.", site.toString());
            throw new DatabaseException(String.format("Unable to write site %s.", site.toString()), ex);
        }
    }


    /**
      Deletes a particular Site from the database, along with all its
      SiteNames.  The Site argument must have the ID set, as this is used
      to locate the correct records in the database for deletion.
      @param site the object to delete from the database.
    */
    @Override
    public synchronized void deleteSite(Site site)
        throws DatabaseException
    {
        try (SiteDAI siteDao = makeSiteDAO())
        {
            siteDao.deleteSite(site.getKey());
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to delete site {}", site.toString());
            throw new DatabaseException(String.format("Unable to delete site %s.", site.toString()), ex);
        }
    }

    @Override
    public synchronized Site getSiteBySiteName(SiteName sn)
        throws DatabaseException
    {
        try (SiteDAI siteDao = makeSiteDAO();)
        {
            return siteDao.getSiteBySiteName(sn);
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to delete site by name {}", sn.toString());
            throw new DatabaseException(String.format("Unable to get site by name %s.", sn.toString()), ex);
        }
    }


    /**
      Reads a complete platform from the database.
      This uses this object's platformId member to
      uniquely identify the record in the database.
      <p>
      The resulting platform object will be "complete"; i.e. it will be
      populated with links to sites, platform configs, platform sensors,
      and transport media.
      </p>
      @param p the object to populate from the database.
    */
    @Override
    public synchronized void readPlatform(Platform p)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            _platformListIO.readPlatform(p);
        }
        catch (SQLException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to read platform {}", p.toString());
            throw new DatabaseException(String.format("Unable to read platform %s.", p.toString()), ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    @Override
    public synchronized DbKey lookupPlatformId(String mediumType, String mediumId,
        Date timeStamp)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            DbKey id = _platformListIO.lookupPlatformId(
                mediumType, mediumId, timeStamp);
            return id;
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to lookup platform by mediumType={}, mediumId={}, timeStamp={}", mediumType, mediumId, timeStamp);
            throw new DatabaseException("Unable to lookup platform ID.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    /** Find a platform ID by site name, and optionally, designator */
    @Override
    public synchronized DbKey lookupCurrentPlatformId(SiteName sn,
        String designator, boolean useDesignator)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            DbKey id = _platformListIO.lookupCurrentPlatformId(sn,
                designator, useDesignator);
            return id;
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to lookup platform id for site={}, designator = {};used={},", sn.toString(), designator, useDesignator);
            throw new DatabaseException("Unable to lookup platform id for site.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }



    /**
      Writes a complete platform back to the database.
      @param p the object to write to the database.
    */
    @Override
    public synchronized void writePlatform( Platform p )
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            _platformListIO.writePlatform(p);
        }
        catch (SQLException ex)
        {
            String msg = ex.getMessage();
            if (!msg.toLowerCase().contains("insufficient priv"))
            {
                log.error("Unable to write Platform", ex);
            }
            throw new DatabaseException("writePlatform: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    /**
      @param p the object to check in the database.
      @return Date object representing the last modify time for this
      platform in the database, or null if the platform no longer exists
      in the database.
    */
    @Override
    public synchronized Date getPlatformLMT(Platform p)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            Date d = _platformListIO.getLMT(p);
            return d;
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    @Override
    public synchronized Date getPlatformListLMT()
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            return _platformListIO.getListLMT();
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }


    /**
      Writes the platform list to the database.
      In SQL this does nothing, listing is done with a SQL query of the
      Platform table.
      @param pl ignored
    */
    @Override
    public void writePlatformList(PlatformList pl)
        throws DatabaseException
    {
        log.trace("SqlDatabaseIO.writePlatformList() - doing nothing");
    }


    /**
      Deletes a platform from to the database, including its transport
      media. It's configuration is not deleted.
      @param p the object to delete from the database.
    */
    @Override
    public synchronized void deletePlatform( Platform p )
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _platformListIO.setConnection(conn);
            _platformListIO.delete(p);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deletePlatform: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _platformListIO.setConnection(null);
        }
    }

    /**
      Reads a platform config object from the database.
      This uses the object's configName member to uniquely identify
      it in the database (not its ID number).
      <p>
      The resulting PlatformConfig will be complete with links to config-
      sensors, decodes scripts (and subordinate script data), and equipment
      model.
      </p>
      @param pc the object to populate from the database.
    */
    // MJM NOT Synchronized because it can be called from the SQL Platform Helper
    @Override
    public void readConfig(PlatformConfig pc)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _configListIO.setConnection(conn);
            _configListIO.readConfig(pc.getId());
        }
        catch (SQLException ex)
        {
            log.error("Unable to read config", ex);
            throw new DatabaseException("Unable to read config", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _configListIO.setConnection(null);
        }
    }


    /**
      Writes a complete platform back to the database.
      This uses the object's configName member to uniquely identify
      it in the database (not its ID number).
      @param pc the object to write to the database.
    */
    @Override
    public synchronized void writeConfig(PlatformConfig pc)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _configListIO.setConnection(conn);
            _configListIO.write(pc);
        }
        catch (SQLException ex)
        {
            String msg = ex.getMessage();
            if (!msg.toLowerCase().contains("insufficient priv"))
            {
                log.error("Unable to write config", ex);
            }
            throw new DatabaseException("writeConfig: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _configListIO.setConnection(null);
        }
    }

    /**
      Deletes a platform configuration from the database.
      This uses the object's configName member to uniquely identify
      it in the database (not its ID number).
      @param pc the object to delete from the database.
    */
    @Override
    public synchronized void deleteConfig(PlatformConfig pc)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _configListIO.setConnection(conn);
            _configListIO.delete(pc);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deleteConfig: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _configListIO.setConnection(null);
        }

    }

    /**
      Read (or re-read) a single EquipmentModel from the database.
      This uses the EquipmentModel's name (not it's ID number) to
      uniquely identify the record in the database.
      @param em the object to populate from the database.
    */
    @Override
    public void readEquipmentModel( EquipmentModel em )
        throws DatabaseException
    {
    }


    /**
      Write an EquipmentModel to the database.
      This uses the EquipmentModel's name (not it's ID number) to
      uniquely identify the record in the database.
      @param eqm the object to write to the database.
    */
    @Override
    public void writeEquipmentModel(EquipmentModel eqm)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _equipmentModelListIO.setConnection(conn);
            _equipmentModelListIO.write(eqm);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writeEquipmentModel: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _equipmentModelListIO.setConnection(null);
        }
    }

    /**
      Deletes an EquipmentModel from the database.
      @param eqm the object to delete from the database.
    */
    @Override
    public synchronized void deleteEquipmentModel(EquipmentModel eqm)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _equipmentModelListIO.setConnection(conn);
            _equipmentModelListIO.delete(eqm);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deleteEquipmentModel: ", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _equipmentModelListIO.setConnection(null);
        }
    }

    /**
      Attempts to read the PresentationGroup's data from the
      database.  It uses the argument's groupName member to find the
      correct record in the database.
      This throws a DatabaseException if the object is not
      found or if there's some other error.  Note that, since this is
      called from the PresentationGroupList.find() method, it's a normal
      occurance for this object not to be in the database.
      @param pg the object to populate from the database.
    */
    @Override
    public synchronized void readPresentationGroup(PresentationGroup pg)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _presentationGroupListIO.setConnection(conn);
            _presentationGroupListIO.readPresentationGroup(pg, true);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _presentationGroupListIO.setConnection(null);
        }
    }


    /**
      Write a PresentationGroup out to the database.  The object
      might be either new or old-and-changed.
      @param pg the object to write to the database.
    */
    @Override
    public synchronized void writePresentationGroup(PresentationGroup pg)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _presentationGroupListIO.setConnection(conn);
            _presentationGroupListIO.write(pg);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writePresentationGroup.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _presentationGroupListIO.setConnection(null);
        }
    }

    /**
      Deletes a presentation group from the database.
      @param pg the object to delete from the database.
    */
    @Override
    public synchronized void deletePresentationGroup(PresentationGroup pg)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _presentationGroupListIO.setConnection(conn);
            _presentationGroupListIO.delete(pg);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deletePresentationGroup.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _presentationGroupListIO.setConnection(null);
        }
    }

    /**
      @param pg the object to check in the database.
      @return Date object representing the last modify time for this
      presentation group in the database, or null if the group no
      longer exists in the database.
    */
    @Override
    public Date getPresentationGroupLMT(PresentationGroup pg)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _presentationGroupListIO.setConnection(conn);
            return _presentationGroupListIO.getLMT(pg);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _presentationGroupListIO.setConnection(null);
        }
    }

    /**
      Attempt to read a RoutingSpec from the database.  The argument
      rs should have already had the name initialized; and that's used
      to access the correct object in the database.  This then fills
      in all the other information in the object.
      @throws DatabaseException if the RoutingSpec is not found in the
      database.
      @param rs the object to populate from the database.
    */
    @Override
    public synchronized void readRoutingSpec(RoutingSpec rs)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _routingSpecListIO.setConnection(conn);
            _routingSpecListIO.readRoutingSpec(rs);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _routingSpecListIO.setConnection(null);
        }
    }

    /**
      Writes a routing spec to the database.
      @param rs the object to write to the database.
    */
    @Override
    public synchronized void writeRoutingSpec(RoutingSpec rs)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _routingSpecListIO.setConnection(conn);
            _routingSpecListIO.write(rs);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writeRoutingSpec.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _routingSpecListIO.setConnection(null);
        }
    }

    /**
      Deletes a routing spec from the database.
      @param rs the object to delete from the database.
    */
    @Override
    public synchronized void deleteRoutingSpec(RoutingSpec rs)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _routingSpecListIO.setConnection(conn);
            _routingSpecListIO.delete(rs);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deleteRoutingSpec.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _routingSpecListIO.setConnection(null);
        }
    }

    /**
      @param rs the object to check in the database.
      @return Date object representing the last modify time for this
      routing spec in the database, or null if the routing spec no longer
      exists in the database.
    */
    @Override
    public synchronized Date getRoutingSpecLMT(RoutingSpec rs)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _routingSpecListIO.setConnection(conn);
            return _routingSpecListIO.getLMT(rs);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _routingSpecListIO.setConnection(null);
        }
    }


    /** Does nothing. */
    @Override
    public synchronized void readDataSource(DataSource ds)
        throws DatabaseException
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            _dataSourceListIO.setConnection(conn);
            DataSource tds = _dataSourceListIO.readDS(ds.getId());
            if (tds != null)
            {
                ds.setName(tds.getName());
                ds.dataSourceType = tds.dataSourceType;
                ds.setDataSourceArg(tds.getDataSourceArg());
                ds.groupMembers.clear();
                Collections.copy(ds.groupMembers, tds.groupMembers);
            }
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _dataSourceListIO.setConnection(null);
        }

    }


    /**
     * Write a DataSource out to the database.  The DataSource can either be
     * pre-existing (presumably with changed data), or it can be new.
    * @param ds the object to write to the database.
     */
    @Override
    public void writeDataSource(DataSource ds)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _dataSourceListIO.setConnection(conn);
            _dataSourceListIO.write(ds);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writeDataSource.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _dataSourceListIO.setConnection(null);
        }
    }

    /**
     * Delete a DataSource from the database.  The object must have a valid
     * ID number.
    * @param ds the object to delete from the database.
     */
    @Override
    public synchronized void deleteDataSource(DataSource ds)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _dataSourceListIO.setConnection(conn);
            _dataSourceListIO.delete(ds);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deleteDataSource.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _dataSourceListIO.setConnection(null);
        }
    }


    /**
      Reads (or re-reads) a NetworkList from the database.  This uses
      the object's name member (not its ID) to uniquely identify the
      record in the database.
      @param ob the object to populate from the database.
    */
    @Override
    public synchronized void readNetworkList( NetworkList ob )
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _networkListListIO.setConnection(conn);
            _networkListListIO.readNetworkList(ob);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("readNetworkList.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _networkListListIO.setConnection(null);
        }
    }

    /**
      Writes a NetworkList to the database.  This uses
      the object's name member (not its ID) to uniquely identify the
      record in the database.
      @param nl the object to write to the database.
    */
    @Override
    public synchronized void writeNetworkList(NetworkList nl)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _networkListListIO.setConnection(conn);
            _networkListListIO.write(nl);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("writeNetworkList.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _networkListListIO.setConnection(null);
        }
    }

    /**
      Deletes a network list from the database.
      @param nl the object to delete from the database.
    */
    @Override
    public synchronized void deleteNetworkList(NetworkList nl)
        throws DatabaseException
    {
        Connection conn = null;

        try
        {
            conn = getConnection();
            _networkListListIO.setConnection(conn);
            _networkListListIO.delete(nl);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("deleteNetworkList.", ex);
        }
        finally
        {
            if (conn != null)
            {
                freeConnection(conn);
            }
            _networkListListIO.setConnection(null);
        }
    }

    /**
      @param nl the object to check in the database.
      @return Date object representing the last modify time for this
      network list in the database, or null if the network list no longer
      exists in the database.
    */
    @Override
    public synchronized Date getNetworkListLMT(NetworkList nl)
        throws DatabaseException
    {
        if (getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
        {
            Connection conn = null;

            try
            {
                conn = getConnection();
                _networkListListIO.setConnection(conn);
                return _networkListListIO.getLMT(nl);
            }
            catch(Exception ex)
            {
                String msg = "getNetworkListLMT - Can't read Network List LMT: ";
                log.warn(msg, ex);
                throw new DatabaseException(msg, ex);
            }
            finally
            {
                if (conn != null)
                {
                    freeConnection(conn);
                }
                _networkListListIO.setConnection(null);
            }
        }

        /*
          Old NetworkList table didn't have LMT. So, if we have an old
          DB, just return the current date truncated to last half hour.
          That way, the record will be reloaded every half hour regardless.
        */
        long now = System.currentTimeMillis();
        if (now - lastLMT > 1800000L)
        {
            lastLMT = (now / 1800000L) * 1800000L;
        }
        return new Date(lastLMT);
    }

    /**
     * Writes the enumeration list data to the SQL database.
    * @param enumList the object to write to the database.
     */
    @Override
    public synchronized void writeEnumList(EnumList enumList)
        throws DatabaseException
    {
        try (EnumDAI enumSqlDao = makeEnumDAO();)
        {
            enumSqlDao.writeEnumList(enumList);
        }
        catch (DbIoException ex)
        {
            throw new DatabaseException("Unable to write EnumList", ex);
        }
    }

    /**
      Generates a surrogate key using the installed KeyGenerator.
      @param tableName name of the table for which a new key is needed.
    */
    public DbKey getKey(String tableName, Connection conn)
        throws DatabaseException
    {
        DbKey k = keyGenerator.getKey(tableName, conn);
        return k;
    }

    /**
      Returns the JDBC Connection object.
    */
    @Override
    public Connection getConnection()
    {
        if (poolingDataSource != null)
        {
            try
            {
                return poolingDataSource.getConnection();
            }
            catch (SQLException ex)
            {
                log.warn("SqlDatabaseIO.getConnection() Cannot get pooled connection.", ex);
            }
        }
        return new WrappedConnection(_conn, (c)-> {/* do nothing */});
    }

    public void setConnection(Connection conn)
    {
        _conn = conn;
    }

    public boolean commitAfterSelectStatus()
    {
        return commitAfterSelect;
    }
    public void setCommitAfterSelect(boolean status)
    {
        commitAfterSelect=status;
    }

    public boolean isOracle()
    {
        return _isOracle;
    }

    public boolean isCwms() { return false; }

    @Override
    public int getTsdbVersion()
    {
        return this.tsdbVersion;
    }

    @Override
    public void setDecodesDatabaseVersion(int version, String options)
    {
        this.databaseVersion = version;
        this.databaseOptions = options;
    }

    @Override
    public void setTsdbVersion(int version, String description)
    {
        this.tsdbVersion = version;
    }

    @Override
    public KeyGenerator getKeyGenerator()
    {
        return keyGenerator;
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
            {
                return null;
            }
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
            catch(Exception pex)
            {
                log.atWarn().setCause(pex)
                   .log("Bad date format '{}' (using default): ", s);
                return null;
            }
        }
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
        {
            return "NULL";
        }
        String ts = writeDateFmt.format(d);
        if (ts.startsWith("to_date"))
        {
            return ts;
        }
        return "'" + ts + "'";
    }

    @Override
    public SiteDAI makeSiteDAO()
    {
        return new SiteDAO(this);
    }

    @Override
    public XmitRecordDAI makeXmitRecordDao(int maxDays)
    {
        return new XmitRecordDAO(this, maxDays);
    }

    @Override
    public LoadingAppDAI makeLoadingAppDAO()
    {
        return new LoadingAppDao(this);
    }

    @Override
    public String sqlBoolean(boolean v)
    {
        if (isOracle())
        {
            return v ? "'Y'" : "'N'";
        }
        else
        {
            return v ? "TRUE" : "FALSE";
        }
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
    public TimeSeriesIdentifier expandSDI(DbCompParm parm) throws DbIoException,
        NoSuchObjectException
    {
        // This method should never be called in the DECODES db interface.
        return null;
    }

    @Override
    public boolean isHdb()
    {
        return false;
    }

    @Override
    public SimpleDateFormat getLogDateFormat()
    {
        return debugDateFmt;
    }

    @Override
    public int findMaxModelRunId(int modelId) throws DbIoException
    {
        // This method should never be called in the DECODES db interface.
        return 0;
    }

    @Override
    public int getWriteModelRunId()
    {
        // This method should never be called in the DECODES db interface.
        return 0;
    }

    @Override
    public DbKey getAppId()
    {
        // This method should never be called in the DECODES db interface.
        return Constants.undefinedId;
    }

    @Override
    public String getDatabaseTimezone()
    {
        return DecodesSettings.instance().sqlTimeZone;
    }

    @Override
    public TimeSeriesDAI makeTimeSeriesDAO()
    {
        // This method should never be called in the DECODES db interface.
        return null;
    }

    @Override
    public UnitConverter makeUnitConverterForRead(CTimeSeries cts)
    {
        // This method should never be called in the DECODES db interface.
        return null;
    }

    @Override
    public CompDependsDAI makeCompDependsDAO()
    {
        // This method should never be called in the DECODES db interface.
        return null;
    }

    @Override
    public CompDependsNotifyDAI makeCompDependsNotifyDAO()
    {
        // As CompDependsDAI above, should not be called from DECODES db interface.
        return null;
    }

    @Override
    public IntervalDAI makeIntervalDAO()
    {
        return null;
    }

    @Override
    public ScheduleEntryDAI makeScheduleEntryDAO()
    {
        if (getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            return new ScheduleEntryDAO(this);
        }
        else
        {
            return null;
        }
    // ISSUE: I need to have an xml schedule entry DAO or none at all?
    // XmlScheduleEntryDAO wants parent to be XmlDatabaseIO.
    }

    @Override
    public ArrayList<TimeSeriesIdentifier> expandTsGroup(TsGroup tsGroup) throws DbIoException
    {
        // This method should not be called.
        return null;
    }

    @Override
    public TimeSeriesIdentifier transformTsidByCompParm(TimeSeriesIdentifier tsid, DbCompParm parm,
        boolean createTS, boolean fillInParm, String timeSeriesDisplayName) throws DbIoException,
        NoSuchObjectException, BadTimeSeriesException
    {
        // This method should not be called.
        return null;
    }

    @Override
    public PlatformStatusDAI makePlatformStatusDAO()
    {
        if (getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            return new PlatformStatusDAO(this);
        }
        else
        {
            return null;
        }
    }

    @Override
    public DeviceStatusDAI makeDeviceStatusDAO()
    {
        return new DeviceStatusDAO(this);
    }

    @Override
    public DacqEventDAI makeDacqEventDAO()
    {
        if (databaseVersion >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            return new DacqEventDAO(this);
        }
        else
        {
            return null;
        }
    }

    @Override
    public ScreeningDAI makeScreeningDAO() throws DbIoException
    {
        // This is a CWMS thing. Base class returns null.
        return null;
    }

    public OracleDateParser makeOracleDateParser(TimeZone tz)
    {
        return new OracleDateParser(tz);
    }

    @Override
    public AlarmDAI makeAlarmDAO()
    {
        return new AlarmDAO(this);
    }

    @Override
    public boolean isOpenTSDB()
    {
        if ((this instanceof HdbSqlDatabaseIO) || (this instanceof CwmsSqlDatabaseIO))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void freeConnection(Connection conn)
    {
        // If we are pooling, close the connection which puts it back into the pool.
        if (poolingDataSource != null)
        {
            try
            {
                conn.close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public boolean isConnected()
    {
        return _isConnected;
    }

    public javax.sql.DataSource getPoolingDataSource()
    {
        return poolingDataSource;
    }

    public void setPoolingDataSource(javax.sql.DataSource poolingDataSource)
    {
        this.poolingDataSource = poolingDataSource;
    }

    public void setKeyGenerator(KeyGenerator keyGenerator)
    {
        this.keyGenerator = keyGenerator;
    }
}
