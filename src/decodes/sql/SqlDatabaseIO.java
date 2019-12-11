/*
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
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

import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.db.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.AlarmDAI;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.CompDependsDAI;
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
import ilex.util.Counter;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.UserAuthFile;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.CompAppInfo;
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
	EngineeringUnitIO _engineeringUnitIO;

	/** This is used to read and write the UnitConverter table. */
	UnitConverterIO _unitConverterIO;

	/**
 	* This holds the sql.NetworkListListIO object that's used to read
 	* and write the NetworkListList.
 	*/
	NetworkListListIO _networkListListIO;

	/**
 	* This holds the sql.ConfigListIO object that's used to read and
 	* write the PlatformConfigList.
 	*/
	public ConfigListIO _configListIO;

	/**
 	* This is used to read and write FormatStatement records.
 	*/
	FormatStatementIO _formatStatementIO;


	/** This reads and writes the DecodesScript table. */
	protected DecodesScriptIO _decodesScriptIO;

	/** This reads and writes the ScriptSensor and related tables.  */
	ScriptSensorIO _scriptSensorIO;

	/**
 	* This holds the sql.EquipmentModelListIO object that's used to read
 	* and write the EquipmentModelList.
 	*/
	protected EquipmentModelListIO _equipmentModelListIO;

	/**
 	* This holds the sql.PlatformListIO object that's used to read and
 	* write the PlatformList.
 	*/
//	protected PlatformListIO _platformListIO;
	public PlatformListIO _platformListIO;

	/**
 	* This holds the sql.RoutingSpecListIO object that's used to read and
 	* write the RoutingSpecList.
 	*/
	RoutingSpecListIO _routingSpecListIO;

	/**
 	* This holds the sql.DataSourceListIO object that's used to read and
 	* write the DataSourceList.
 	*/
	DataSourceListIO _dataSourceListIO;

	/**
 	* This holds the sql.PresentationGroupListIO object that's used to read
 	* and write the PresentationGroupList.
 	*/
	PresentationGroupListIO _presentationGroupListIO;

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
	
	/** 
	 * Routing Spec Threads in certain apps establish their own connection.
	 * This is necessary to prevent them from interfering with other threads.
	 */
	HashMap<Thread, Connection> connectionMap = new HashMap<Thread, Connection>();

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

	/**
	 * Default constructor -- all initialization that doesn't depend on
	 * a database connection goes here.
	 */
	public SqlDatabaseIO()
	{
		DecodesSettings settings = DecodesSettings.instance();

		// Initialize the child IO objects
		// Their are dependencies among them, which should be enforced
		// by their various constructors.  For example, the PlatformListIO
		// can't be instantiated until after the ConfigListIO and the
		// SiteIO objects are created.
		
		_engineeringUnitIO = new EngineeringUnitIO(this);
		_unitConverterIO = new UnitConverterIO(this);
		_networkListListIO = new NetworkListListIO(this);
		_formatStatementIO = new FormatStatementIO(this);
		_scriptSensorIO = new ScriptSensorIO(this, _unitConverterIO);
		_decodesScriptIO = new DecodesScriptIO(this, _formatStatementIO, 
			_scriptSensorIO);
		_configListIO = new ConfigListIO(this, _decodesScriptIO);
		_equipmentModelListIO = new EquipmentModelListIO(this);
		_platformListIO = new PlatformListIO(this, _configListIO, 
			_equipmentModelListIO, _decodesScriptIO);
		_dataSourceListIO = new DataSourceListIO(this);
		_presentationGroupListIO = new PresentationGroupListIO(this);
		_routingSpecListIO = new RoutingSpecListIO(this,
		_presentationGroupListIO, _networkListListIO);

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
			DecodesSettings.instance().sqlKeyGenerator, _conn);
	}

	/**
	* To create a SqlDatabaseIO that uses an externally provided JDBC
	* connection, call the no-args constructor, then call this method.
	* The passed connection is connected and authenticated and ready for
	* SQL statements.
	*
	* @param conn the JDBC connection
	* @param keyGenerator the Key Generator to use
	* @param location the database location string for log messages
	*/
	public void useExternalConnection(Connection conn, 
		KeyGenerator keyGenerator, String location)
	{
		_conn = conn;
		this.keyGenerator = keyGenerator;
		_sqlDbLocation = location;
		if (writeDateFmt == null)
			determineVersion();
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
			Logger.instance().debug3("initializing driver class '" + driverClass + "'");
			Class.forName(driverClass).newInstance();
			Logger.instance().debug3("...success.");
		}
		catch (Exception ex)
		{
			String msg = "Cannot load JDBC driver class '" + driverClass + "': " + ex;
			Logger.instance().fatal(msg);
			throw new DatabaseConnectException(msg);
		}

		// Try to authenticate using OS authentication (IDENT)
		if (DecodesSettings.instance().tryOsDatabaseAuth)
		{
			Properties emptyProps = new Properties();
			Logger.instance().debug3("Trying OS authentication with location '" + _sqlDbLocation + "'");
			try
			{
				_conn = DriverManager.getConnection(_sqlDbLocation, emptyProps);
				Logger.instance().debug3("...Success.");
			}
			catch(SQLException ex)
			{
				Logger.instance().info("SqlDatabaseIO Connection using OS authentication failed. "
					+ "Will attempt username/password auth. (" + ex + ")");
				_conn = null;
			}
		}
		
		if (_conn == null)
		{
			// Retrieve username and password for database
			String authFileName = DecodesSettings.instance().DbAuthFile;
			UserAuthFile authFile = new UserAuthFile(authFileName);
			try { authFile.read(); }
			catch(Exception ex)
			{
				String msg = "Cannot read username and password from '"
					+ authFileName + "' (run setDecodesUser first): " + ex;
				System.err.println(msg);
				Logger.instance().log(Logger.E_FATAL, msg);
				throw new DatabaseConnectException(msg);
			}

			connectUserPassword(authFile.getUsername(), authFile.getPassword());
		}
		
		// MJM 2018-2/21 Force autoCommit on.
		try { _conn.setAutoCommit(true);}
		catch(SQLException ex)
		{
			Logger.instance().warning("Cannot set SQL AutoCommit to true: " + ex);
		}

		determineVersion();
		
		try {
			setDBDatetimeFormat();
		} catch (SQLException e) {
			Logger.instance().debug3(e.toString());
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
				Logger.instance().info("Connecting to " + _sqlDbLocation
					+ " as user '" + user + "'");
				
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
						+ "' for user '" + user + "': " + ex.toString());
				else
				{
					Logger.instance().debug1("JDBC Connection Failed (will retry): " + ex);
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
		
	}

	protected void setDBDatetimeFormat()
		throws SQLException
	{
		String q = null;
		Statement stmnt = null;
		try
		{
			if (_isOracle)
			{
				
				stmnt = getConnection().createStatement();

				q = "SELECT PARAM_VALUE FROM REF_DB_PARAMETER WHERE PARAM_NAME = 'TIME_ZONE'";
				ResultSet rs = null;
//				Logger.instance().info(q);
				try
				{ 
					rs = stmnt.executeQuery(q);
					if (rs != null && rs.next())
						databaseTimeZone = rs.getString(1);
//					Logger.instance().info("databaseTimeZone is '" + databaseTimeZone + "'");
				}
				catch(SQLException ex)
				{
					databaseTimeZone = DecodesSettings.instance().sqlTimeZone;
					Logger.instance().debug3(q +
						" -- failed, must not be HDB, using sqlTimeZone setting of '"
						+ databaseTimeZone + "'");
				}
				finally
				{
					try { rs.close(); } catch(Exception ex) {}
					rs = null;
				}

				stmnt = getConnection().createStatement();
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
		catch(SQLException ex)
		{
			
		}
		finally
		{
			if (stmnt != null)
				try { stmnt.close(); } catch (Exception ex) {}
		}
	}	
	
	public synchronized void determineVersion()
	{
		try
		{
			String productName = getConnection().getMetaData().getDatabaseProductName();
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

		readVersionInfo(this);
		TimeSeriesDb.readVersionInfo(this);
	}
	
	public static void readVersionInfo(DatabaseConnectionOwner dco)
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
			stmt = dco.getConnection().createStatement();
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
			stmt.close();
		}
		catch(SQLException ex)
		{
			Logger.instance().warning(
				"Cannot read DatabaseVersion table. Assuming version 5."
				+ "(Exception=" + ex + ")" );
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
		Logger.instance().info("Connected to DECODES SQL database version " + databaseVersion);
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
	  @return true if this database type requires login.
	  @deprecated This always returns false -- implemented a different way.
	*/
	public boolean requiresLogin( )
	{
		//System.out.println("SqlDatabaseIO.requiresLogin()");
		return false;
	}

	/** Does nothing. */
	public void doLogin(String user, String passwd)
	{
		//System.out.println("SqlDatabaseIO.doLogin()");
	}

	/** Does nothing. */
	public boolean isLoggedIn()
	{
		//System.out.println("SqlDatabaseIO.isLoggedIn()");
		return true;
	}

	/**
	  Closes the JDBC connection.
	*/
	public void close( )
	{
		try 
		{
			commit();
			Logger.instance().info("Closing  database connection.");
			_conn.close();
		}
		catch (SQLException e) {
			// ignore; we did our best
		}
	}

	/**
	* Reads the set of known enumeration objects in this database.
	* @param top the EnumList object to populate
	*/
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
			throw new DatabaseException(ex.toString());
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
			throw new DatabaseException(ex.toString());
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
	public synchronized void readDataTypeSet(DataTypeSet dts)
		throws DatabaseException
	{
		DataTypeDAI dtdao = this.makeDataTypeDAO();
		try { dtdao.readDataTypeSet(dts); }
		catch(DbIoException ex)
		{
			throw new DatabaseException(ex.getMessage());
		}
		finally { dtdao.close(); }
	}

	/**
	  Reads a single data-type object given its numeric key.
	  @return data type or null if not found
	*/
	public synchronized DataType readDataType(DbKey id)
		throws DatabaseException
	{
		DataTypeDAI dtdao = this.makeDataTypeDAO();
		try { return dtdao.getDataType(id); }
		catch(DbIoException ex)
		{
			throw new DatabaseException(ex.getMessage());
		}
		finally { dtdao.close(); }
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
	public synchronized void readEngineeringUnitList(EngineeringUnitList euList)
		throws DatabaseException
	{
		_engineeringUnitIO.read(euList);

		UnitConverterSet ucs = euList.getDatabase().unitConverterSet;
		_unitConverterIO.read(ucs);
		if (commitAfterSelect)
			try { commit(); }
			catch(SQLException ex) {}
	}

	/**
	* Reads the list of Site objects defined in this database.
	* Objects in this list may be only partially populated (key values
	* and primary display attributes only).
	* @param sl the object to populate from the database.
	*/
	public synchronized void readSiteList(SiteList sl)
		throws DatabaseException
	{
		SiteDAI siteDao = makeSiteDAO();
		try { siteDao.read(sl); }
		catch(DbIoException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
			throw new DatabaseException(ex.getMessage());
		}
		finally
		{
			siteDao.close();
		}
	}

	/**
	* Read the platform list cross reference file and populate the passed
	* PlatformList object.
	* @param platformList the object to populate from the database.
	*/
	public synchronized void readPlatformList(PlatformList platformList)
		throws DatabaseException
	{
		//System.out.println("  SqlDatabaseIO.readPlatformList()");

		try 
		{
			_platformListIO.read(platformList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			System.err.println(e);
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}

	/**
	* Reads the list of PlatformConfig objects defined in this database.
	* @param pcList the object to populate from the database.
	* @throws DatabaseException if there's an error.
	*/
	public synchronized void readConfigList(PlatformConfigList pcList)
		throws DatabaseException
	{
		//System.out.println("  SqlDatabaseIO.readConfigList()");

		try 
		{
			_configListIO.read(pcList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			System.err.println(e);
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}

	public synchronized PlatformConfig newPlatformConfig(PlatformConfig pc, String deviceId,
		String originator)
    	throws DatabaseException
	{
		try {
			return(_configListIO.newPlatformConfig(pc, deviceId, originator));
		} catch (SQLException e) {
			throw new DatabaseException(e.toString());
		}
	}

	/**
	* Returns the list of EquipmentModel objects defined in this database.
	* Objects in this collection are complete.
	* @param eml the object to populate from the database.
	*/
	public synchronized void readEquipmentModelList(EquipmentModelList eml)
		throws DatabaseException
	{
		try 
		{
			_equipmentModelListIO.read(eml);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
	}

	/**
	* Returns the list of RoutingSpec objects defined in this database.
	* Objects in this list may be only partially populated (key values
	* and primary display attributes only).
	* @param rsList the object to populate from the database.
	*/
	public synchronized void readRoutingSpecList(RoutingSpecList rsList)
		throws DatabaseException
	{
		//System.out.println("  SqlDatabaseIO.readRoutingSpecList()");

		try 
		{
			_routingSpecListIO.read(rsList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}

	/**
	* Returns the list of DataSource objects defined in this database.
	* Objects in this list may be only partially populated (key values
	* and primary display attributes only).
	* @param dsList the object to populate from the database.
	* @throws DatabaseException if can't list the directory.
	*/
	public synchronized void readDataSourceList(DataSourceList dsList)
		throws DatabaseException
	{
		//System.out.println("  SqlDatabaseIO.readDataSourceList()");

		try 
		{
			_dataSourceListIO.read(dsList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
	}

	/**
	* Returns the list of NetworkList objects defined in this database.
	* Objects in this list may be only partially populated (key values
	* and primary display attributes only).
	* @param nlList the object to populate from the database.
	*/
	public synchronized void readNetworkListList(NetworkListList nlList)
		throws DatabaseException
	{
		try 
		{
			_networkListListIO.read(nlList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
	}
	
	/**
	 * Non-cached method to read the list of network list specs currently
	 * defined in the database.
	 * @return ArrayList of currently defined network list specs.
	 */
	public synchronized ArrayList<NetworkListSpec> getNetlistSpecs()
		throws DatabaseException
	{
		try 
		{
			ArrayList<NetworkListSpec> ret = 
				_networkListListIO.getNetlistSpecs();
			if (commitAfterSelect)
				commit();
			return ret;
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
		
	}


	/**
	* Returns the list of PresentationGroup objects defined in this
	* database.Objects in this list may be only partially populated
	* (key values and primary display attributes only).
	* @param pgList the object to populate from the database.
	*/
	public synchronized void readPresentationGroupList(PresentationGroupList pgList)
		throws DatabaseException
	{
		try 
		{
			_presentationGroupListIO.read(pgList);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			System.err.println(e);
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}


	/**
 	* Writes the DataTypeSet to the SQL database.
	* @param dts the object to write to the database.
 	*/
	public void writeDataTypeSet( DataTypeSet dts )
		throws DatabaseException
	{
		Database oldDb = Database.getDb();
		Database.setDb(dts.getDatabase());

		DataTypeDAI dtdao = this.makeDataTypeDAO();
		try { dtdao.writeDataTypeSet(dts); }
		catch(Exception ex)
		{
			throw new DatabaseException(ex.getMessage());
		}
		finally 
		{
			dtdao.close();
			Database.setDb(oldDb);
		}
	}

	/**
 	* Writes the DataTypeSet to the SQL database.
	* @param dts the object to write to the database.
 	*/
	public void writeDataType( DataType dt )
		throws DatabaseException
	{
		DataTypeDAI dtdao = this.makeDataTypeDAO();
		try { dtdao.writeDataType(dt); }
		catch(DbIoException ex)
		{
			throw new DatabaseException(ex.getMessage());
		}
		finally 
		{
			dtdao.close();
		}
	}
	/**
 	* Returns the list of UnitConverter objects defined in this database.
 	* Objects in this list may be only partially populated (key values
 	* and primary display attributes only).
	* @param ucs the object to populate from the database.
 	*/
	public synchronized void readUnitConverterSet(UnitConverterSet ucs)
		throws DatabaseException
	{
		//System.out.println("SqlDatabaseIO.readUnitConverterSet()");
	}


	/**
 	* Writes the entire collection of engineering units to the database.
	* @param top the object to write to the database.
 	*/
	public synchronized void writeEngineeringUnitList(EngineeringUnitList top)
		throws DatabaseException
	{
		Logger.instance().log(Logger.E_INFORMATION,
			"Writing engineering unit list.");

		try 
		{
			_engineeringUnitIO.write(top);
			UnitConverterSet ucs = top.getDatabase().unitConverterSet;
			_unitConverterIO.write(ucs);
//			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}

			throw new DatabaseException(e.toString());
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
	public synchronized void readSite( Site site )
		throws DatabaseException
	{
		SiteDAI siteDao = makeSiteDAO();
		try { siteDao.readSite(site); }
		catch (Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			throw new DatabaseException(ex.getMessage());
		}
		finally
		{
			siteDao.close();
		}
	}

	/**
	  Writes site information back to the database.
	  @param site the object to write to the database.
	  @throws DatabaseException if no name is assigned to this object or
	  if the write fails.
	*/
	public synchronized void writeSite(Site site)
		throws DatabaseException
	{
		SiteDAI siteDao = makeSiteDAO();
		try { siteDao.writeSite(site); }
		catch (Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			throw new DatabaseException(ex.getMessage());
		}
		finally
		{
			siteDao.close();
		}
	}


	/**
	  Deletes a particular Site from the database, along with all its
	  SiteNames.  The Site argument must have the ID set, as this is used
	  to locate the correct records in the database for deletion.
	  @param site the object to delete from the database.
	*/
	public synchronized void deleteSite(Site site)
		throws DatabaseException
	{
		SiteDAI siteDao = makeSiteDAO();
		try { siteDao.deleteSite(site.getKey()); }
		catch (Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			throw new DatabaseException(ex.getMessage());
		}
		finally
		{
			siteDao.close();
		}
	}
	
	public synchronized Site getSiteBySiteName(SiteName sn)
		throws DatabaseException
	{
		SiteDAI siteDao = makeSiteDAO();
		try { return siteDao.getSiteBySiteName(sn); }
		catch (Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace(System.err);
			throw new DatabaseException(ex.getMessage());
		}
		finally
		{
			siteDao.close();
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
	public synchronized void readPlatform( Platform p )
		throws DatabaseException
	{
		//System.out.println("SqlDatabaseIO.readPlatform()");

		try 
		{
			_platformListIO.readPlatform(p); 
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException ex) 
		{
			System.err.println(ex);
			ex.printStackTrace();
			throw new DatabaseException(ex.toString());
		}
	}

	public synchronized DbKey lookupPlatformId(String mediumType, String mediumId,
		Date timeStamp)
		throws DatabaseException
	{
		try 
		{
			DbKey id = _platformListIO.lookupPlatformId(
				mediumType, mediumId, timeStamp);
			if (commitAfterSelect)
				commit();
			return id;
		}
		catch (SQLException ex) 
		{
			System.err.println(ex);
			ex.printStackTrace();
			throw new DatabaseException(ex.toString());
		}
	}
	
	/** Find a platform ID by site name, and optionally, designator */
	public synchronized DbKey lookupCurrentPlatformId(SiteName sn, 
		String designator, boolean useDesignator)
		throws DatabaseException
	{
			try 
			{
				DbKey id = _platformListIO.lookupCurrentPlatformId(sn, 
					designator, useDesignator);
				if (commitAfterSelect)
					commit();
				return id;
			}
			catch (SQLException ex) 
			{
				System.err.println(ex);
				ex.printStackTrace();
				throw new DatabaseException(ex.toString());
			}
	}



	/**
	  Writes a complete platform back to the database.
	  @param p the object to write to the database.
	*/
	public synchronized void writePlatform( Platform p )
		throws DatabaseException
	{
		try 
		{
			_platformListIO.writePlatform(p);
			commit();
		}
		catch (SQLException e) 
		{
			String msg = e.getMessage();
			if (!msg.toLowerCase().contains("insufficient priv"))
			{
				System.err.println("" + e);
				e.printStackTrace(System.err);
			}
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  @param p the object to check in the database.
	  @return Date object representing the last modify time for this 
	  platform in the database, or null if the platform no longer exists 
	  in the database.
	*/
	public synchronized Date getPlatformLMT(Platform p)
		throws DatabaseException
	{
		Date d = _platformListIO.getLMT(p);
//		try { commit(); }
//		catch(SQLException ex) {}
		return d;
	}

	public synchronized Date getPlatformListLMT()
	{
		return _platformListIO.getListLMT();
	}


	/**
	  Writes the platform list to the database.
	  In SQL this does nothing, listing is done with a SQL query of the
	  Platform table.
	  @param pl ignored
	*/
	public void writePlatformList(PlatformList pl)
		throws DatabaseException
	{
		//System.out.println("SqlDatabaseIO.writePlatformList()");
	}


	/**
	  Deletes a platform from to the database, including its transport
	  media. It's configuration is not deleted.
	  @param p the object to delete from the database.
	*/
	public synchronized void deletePlatform( Platform p )
		throws DatabaseException
	{
		try 
		{
			_platformListIO.delete(p);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
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
	public void readConfig(PlatformConfig pc)
		throws DatabaseException
	{
		try 
		{
			_configListIO.readConfig(pc.getId());
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			e.printStackTrace(System.err);
			throw new DatabaseException(e.toString());
		}
	}


	/**
	  Writes a complete platform back to the database.
	  This uses the object's configName member to uniquely identify
	  it in the database (not its ID number).
	  @param pc the object to write to the database.
	*/
	public synchronized void writeConfig(PlatformConfig pc)
		throws DatabaseException
	{
		try 
		{
			_configListIO.write(pc);
			commit();
		}
		catch (SQLException e) 
		{
			String msg = e.getMessage();
			if (!msg.toLowerCase().contains("insufficient priv"))
			{
				System.err.println(e);
				e.printStackTrace(System.err);
			}
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Deletes a platform configuration from the database.
	  This uses the object's configName member to uniquely identify
	  it in the database (not its ID number).
	  @param pc the object to delete from the database.
	*/
	public synchronized void deleteConfig(PlatformConfig pc)
		throws DatabaseException
	{
		try 
		{
			_configListIO.delete(pc);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Read (or re-read) a single EquipmentModel from the database.
	  This uses the EquipmentModel's name (not it's ID number) to
	  uniquely identify the record in the database.
	  @param em the object to populate from the database.
	*/
	public void readEquipmentModel( EquipmentModel em )
		throws DatabaseException
	{
		//System.out.println("SqlDatabaseIO.readEquipmentModel()");
	}


	/**
	  Write an EquipmentModel to the database.
	  This uses the EquipmentModel's name (not it's ID number) to
	  uniquely identify the record in the database.
	  @param eqm the object to write to the database.
	*/
	public void writeEquipmentModel(EquipmentModel eqm)
		throws DatabaseException
	{
		try 
		{
			_equipmentModelListIO.write(eqm);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Deletes an EquipmentModel from the database.
	  @param eqm the object to delete from the database.
	*/
	public synchronized void deleteEquipmentModel(EquipmentModel eqm)
		throws DatabaseException
	{
		try 
		{
			_equipmentModelListIO.delete(eqm);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
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
	public synchronized void readPresentationGroup(PresentationGroup pg)
		throws DatabaseException
	{
		_presentationGroupListIO.readPresentationGroup(pg, true);
		try { if ( commitAfterSelect ) commit(); }
		catch(SQLException ex) {}
	}


	/**
	  Write a PresentationGroup out to the database.  The object
	  might be either new or old-and-changed.
	  @param pg the object to write to the database.
	*/
	public synchronized void writePresentationGroup(PresentationGroup pg)
		throws DatabaseException
	{
		try 
		{
			_presentationGroupListIO.write(pg);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Deletes a presentation group from the database.
	  @param pg the object to delete from the database.
	*/
	public synchronized void deletePresentationGroup(PresentationGroup pg)
		throws DatabaseException
	{
		try 
		{
			_presentationGroupListIO.delete(pg);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  @param pg the object to check in the database.
	  @return Date object representing the last modify time for this 
	  presentation group in the database, or null if the group no 
	  longer exists in the database.
	*/
	public Date getPresentationGroupLMT(PresentationGroup pg)
		throws DatabaseException
	{
		Date d = _presentationGroupListIO.getLMT(pg);
		try { commit(); }
		catch(SQLException ex) {}
		return d;
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
	public synchronized void readRoutingSpec(RoutingSpec rs)
		throws DatabaseException
	{
		_routingSpecListIO.readRoutingSpec(rs);
		try { if ( commitAfterSelect ) commit(); }
		catch(SQLException ex) {}
	}

	/**
	  Writes a routing spec to the database.
	  @param rs the object to write to the database.
	*/
	public synchronized void writeRoutingSpec(RoutingSpec rs)
		throws DatabaseException
	{
		try 
		{
			_routingSpecListIO.write(rs);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Deletes a routing spec from the database.
	  @param rs the object to delete from the database.
	*/
	public synchronized void deleteRoutingSpec(RoutingSpec rs)
		throws DatabaseException
	{
		try 
		{
			_routingSpecListIO.delete(rs);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  @param rs the object to check in the database.
	  @return Date object representing the last modify time for this 
	  routing spec in the database, or null if the routing spec no longer 
	  exists in the database.
	*/
	public synchronized Date getRoutingSpecLMT(RoutingSpec rs)
		throws DatabaseException
	{
		Date d = _routingSpecListIO.getLMT(rs);
		try { if ( commitAfterSelect ) commit(); }
		catch(SQLException ex) {}
		return d;
	}


	/** Does nothing. */
	public synchronized void readDataSource(DataSource ds)
		throws DatabaseException
	{
		//System.out.println("SqlDatabaseIO.readDataSource()");
	}


	/**
 	* Write a DataSource out to the database.  The DataSource can either be
 	* pre-existing (presumably with changed data), or it can be new.
	* @param ds the object to write to the database.
 	*/
	public void writeDataSource(DataSource ds)
		throws DatabaseException
	{
		try 
		{
			_dataSourceListIO.write(ds);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
 	* Delete a DataSource from the database.  The object must have a valid
 	* ID number.
	* @param ds the object to delete from the database.
 	*/
	public synchronized void deleteDataSource(DataSource ds)
		throws DatabaseException
	{
		try 
		{
			_dataSourceListIO.delete(ds);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}


	/**
	  Reads (or re-reads) a NetworkList from the database.  This uses
	  the object's name member (not its ID) to uniquely identify the
	  record in the database.
	  @param ob the object to populate from the database.
	*/
	public synchronized void readNetworkList( NetworkList ob )
		throws DatabaseException
	{
		try 
		{
			_networkListListIO.readNetworkList(ob);
			if (commitAfterSelect)
				commit();
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Writes a NetworkList to the database.  This uses
	  the object's name member (not its ID) to uniquely identify the
	  record in the database.
	  @param nl the object to write to the database.
	*/
	public synchronized void writeNetworkList(NetworkList nl)
		throws DatabaseException
	{
		try 
		{
			_networkListListIO.write(nl);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  Deletes a network list from the database.
	  @param nl the object to delete from the database.
	*/
	public synchronized void deleteNetworkList(NetworkList nl)
		throws DatabaseException
	{
		try 
		{
			_networkListListIO.delete(nl);
			commit();
		}
		catch (SQLException e) 
		{
			try { rollback(); }
			catch (SQLException e1)
			{
				throw new DatabaseException(e1.toString());
			}
			throw new DatabaseException(e.toString());
		}
	}

	/**
	  @param nl the object to check in the database.
	  @return Date object representing the last modify time for this 
	  network list in the database, or null if the network list no longer 
	  exists in the database.
	*/
	public synchronized Date getNetworkListLMT(NetworkList nl)
		throws DatabaseException
	{
		if (getDecodesDatabaseVersion() >= 6)
		{
			try 
			{
				Date d = _networkListListIO.getLMT(nl); 
				if (commitAfterSelect)
					commit();
				return d; 
			}
			catch(Exception ex)
			{
				String msg = "getNetworkListLMT - Can't read Network List LMT: "
					+ ex;
				Logger.instance().warning(msg);
				throw new DatabaseException(msg);
			}
		}

		/*
		  Old NetworkList table didn't have LMT. So, if we have an old
		  DB, just return the current date truncated to last half hour.
		  That way, the record will be reloaded every half hour regardless.
		*/
		long now = System.currentTimeMillis();
		if (now - lastLMT > 1800000L)
			lastLMT = (now / 1800000L) * 1800000L;
		return new Date(lastLMT);
	}

	/**
 	* Writes the enumeration list data to the SQL database.
	* @param enumList the object to write to the database.
 	*/
	public synchronized void writeEnumList(EnumList enumList)
		throws DatabaseException
	{
		EnumDAI enumSqlDao = makeEnumDAO();

		try
		{
			enumSqlDao.writeEnumList(enumList);
		}
		catch (DbIoException ex) 
		{
			throw new DatabaseException(ex.toString());
		}
		finally
		{
			enumSqlDao.close();
		}
	}


	/** 
	  @deprecated Platform IDs are now assigned with the getKey method
	  in this class.
	  @see SqlDatabaseIO#getKey
	*/
	public Counter getPlatformIdCounter()
	{
		//System.out.println("SqlDatabaseIO.getPlatformIdCounter()");
		return null;
	}

	/**
	  Generates a surrogate key using the installed KeyGenerator.
	  @param tableName name of the table for which a new key is needed.
	*/
	public DbKey getKey(String tableName)
		throws DatabaseException
	{
		DbKey k = keyGenerator.getKey(tableName, getConnection());
/*
		try { if ( commitAfterSelect ) commit(); }
		catch(SQLException ex) {}
*/
		return k;
	}

	/**
	  Returns the JDBC Connection object.
	*/
	public Connection getConnection()
	{
		return _conn;
	}
	
	public void setConnection(Connection conn)
	{
		_conn = conn;
	}

	/**
	 * Commits any pending transactions.
	 */
	public void commit()
		throws SQLException
	{
		// MJM 2017/03/03 Everything is now auto commit.
//		Connection con = getConnection();
//		if (con != null)
//		{
//			con.commit();
//			try { con.clearWarnings(); }
//			catch(SQLException ex) {}
//		}
	}
	public void rollback()
		throws SQLException
	{
//		Connection con = getConnection();
//		if (con != null)
//		{
//			con.rollback();
//			try { con.clearWarnings(); }
//			catch(SQLException ex) {}
//		}
	}

	public boolean commitAfterSelectStatus()
	{
		return(commitAfterSelect);
	}
	public void setCommitAfterSelect(boolean status)
	{
		commitAfterSelect=status;
	}
	
	/**
	  Reads  names of NetworkList  from the database.  This uses
	  the transport id to uniquely identify the networklist containing that transport id
	   in the database.
	  @param transportId the value of transport id contained in network list.
	*/
	public synchronized ArrayList<String> readNetworkListName( String transportId )
		throws DatabaseException
	{
		try 
		{
			 ArrayList<String> networkListArray=_platformListIO.readNetworKListName(transportId);
			if (commitAfterSelect)
				commit();
			return networkListArray;
		}
		catch (SQLException e) 
		{
			throw new DatabaseException(e.toString());
		}
	}
	
//	/**
//	  Reads  names of NetworkList  from the database.  This uses
//	  the transport id to uniquely identify the networklist containing that transport id
//	   in the database.
//	  @param transportId the value of transport id contained in network list.
//	*/
//	public synchronized void updateTransportId( String oldtransportId, String newTransportId )
//		
//	{
//		try{
//		_platformListIO.updateTransportId(oldtransportId, newTransportId);
//		}
//		catch(DatabaseException ex)
//		{
//			//System.out.println(ex);
//			ex.printStackTrace(System.out);
//		}
//			catch (SQLException e) 
//			{
//				//System.out.println(e);
//				e.printStackTrace(System.out);
//				try { rollback(); }
//				catch (SQLException e1)
//				{
//					//System.err.println(e1);
//					e1.printStackTrace(System.err);
//
//				}
//				
//			}
//			
//	}
	
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
			catch(Exception pex)
			{
				Logger.instance().warning("Bad date format '" + s + "' (using default): " + pex);
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
			return "NULL";
		String ts = writeDateFmt.format(d);
		if (ts.startsWith("to_date"))
			return ts;
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
			return v ? "'Y'" : "'N'";
		else
			return v ? "TRUE" : "FALSE";
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
	public IntervalDAI makeIntervalDAO()
	{
		return null;
	}

	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		if (getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			return new ScheduleEntryDAO(this);
		else
			return null; 
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
			return new PlatformStatusDAO(this);
		else
			return null; 
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
			return new DacqEventDAO(this);
		else
			return null;
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


}
