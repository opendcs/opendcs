/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.7  2016/06/27 15:29:01  mmaloney
*  Code cleanup.
*
*  Revision 1.6  2015/11/12 15:22:46  mmaloney
*  Added makeScreeningDAO method.
*
*  Revision 1.5  2014/12/19 19:25:35  mmaloney
*  Handle version change for column name tsdb_group_member_ts data_id vs. ts_id.
*
*  Revision 1.4  2014/12/11 20:29:31  mmaloney
*  Added DacqEventLogging capability.
*
*  Revision 1.3  2014/11/19 16:09:48  mmaloney
*  Additions for dcpmon
*
*  Revision 1.2  2014/08/29 18:21:19  mmaloney
*  For opendcs-oracle, determine _isOracle AFTER connection.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.133  2013/07/31 15:27:18  mmaloney
*  Added methods to check for questionable and set questionable.
*
*  Revision 1.132  2013/07/25 18:22:05  mmaloney
*  dev
*
*  Revision 1.131  2013/07/24 17:37:48  mmaloney
*  dev
*
*  Revision 1.130  2013/07/24 15:37:57  mmaloney
*  dev
*
*  Revision 1.129  2013/07/24 15:28:39  mmaloney
*  dev
*
*  Revision 1.128  2013/07/24 14:16:20  mmaloney
*  dev
*
*  Revision 1.127  2013/07/24 13:52:28  mmaloney
*  dev
*
*  Revision 1.126  2013/07/24 13:41:54  mmaloney
*  Cleanup the listCompsForGui stuff. This is all now portable across databases.
*
*  Revision 1.125  2013/07/12 11:50:53  mmaloney
*  Added tasklist queue stuff.
*
*  Revision 1.124  2013/05/03 13:05:33  mmaloney
*  Fixed version reference for CWMS 2.1
*
*  Revision 1.123  2013/04/12 17:09:37  mmaloney
*  column mask on inserts required for VPD
*
*  Revision 1.122  2013/03/25 16:58:25  mmaloney
*  Refactor comp lock stale time.
*
*  Revision 1.121  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.120  2013/02/20 15:07:24  gchen
*  Enhance a new feature to allow to use the maxComputationRetries property to limit the number of retries for those failed computations. There will be unlimited retries if maxComputationRetires=0.
*
*  This feature will apply to Tempest DB, CWMS, and HDB.
*
*  Revision 1.119  2012/11/28 23:00:31  mmaloney
*  Added javadoc
*
*  Revision 1.118  2012/11/20 15:58:35  mmaloney
*  Improve wording of Exception message.
*
*  Revision 1.117  2012/10/25 15:04:18  mmaloney
*  For HDB, if modelRunId is not set for a modeled time series,
*  set it to the maximum run ID for that model ID.
*
*  Revision 1.116  2012/10/24 15:17:29  mmaloney
*  Changed getDcpXmitSuffix to public for retrieving stats.
*
*  Revision 1.115  2012/10/23 20:16:44  mmaloney
*  In listTimeSeries, if useCache for sites, then replace the site object in the
*  TSID with the full one from the cache so that TSID has all of the site names
*  and properties.
*
*  Revision 1.114  2012/10/15 15:39:40  shweta
*  reverted back to 1.112
*
*  Revision 1.112  2012/10/04 14:51:15  shweta
*  set isreloaded flag if computation is reloaded.
*
*  Revision 1.111  2012/10/03 14:24:22  mmaloney
*  Reduce debug.
*
*  Revision 1.110  2012/09/11 13:03:34  mmaloney
*  dev
*
*  Revision 1.109  2012/08/29 13:59:49  mmaloney
*  database version cleanup.
*
*  Revision 1.108  2012/08/24 12:58:12  mmaloney
*  Implement methods to write to the tasklist table from Java.
*
*  Revision 1.107  2012/08/23 19:05:16  mmaloney
*  Enqueue past data to the tasklist table back to notification date/time.
*
*  Revision 1.106  2012/08/22 19:56:08  mmaloney
*  added countCompsUsingGroup method
*
*  Revision 1.105  2012/08/22 19:53:00  mmaloney
*  added countCompsUsingGroup method
*
*  Revision 1.104  2012/08/20 20:08:05  mmaloney
*  Implement HDB Convert2Group utility.
*
*  Revision 1.103  2012/08/17 14:22:49  mmaloney
*  dev
*
*  Revision 1.102  2012/08/17 13:21:00  mmaloney
*  debugs in listCompsForGUI
*
*  Revision 1.101  2012/08/13 15:22:03  mmaloney
*  Needed makeEmptyTsId method so that TsImport can create a time-series if it doesn't
*  already exist.
*
*  Revision 1.100  2012/08/11 20:13:02  mmaloney
*  When saving a computation, update lastModified in the passed object.
*
*  Revision 1.99  2012/08/09 15:41:38  mmaloney
*  Added makeTimeSeries method. Several utilities were duplicating this code.
*  Moving it here means that it will be consistent in all programs.
*
*  Revision 1.98  2012/07/31 14:50:45  mmaloney
*  Fix obtainLock bug.
*
*  Revision 1.97  2012/07/30 20:58:40  mmaloney
*  Allow subclass to change 'module'.
*
*  Revision 1.96  2012/07/27 19:40:36  mmaloney
*  getTimeSeriesFor() moved to HDB subclass.
*
*  Revision 1.95  2012/07/25 18:40:58  mmaloney
*  Remove nuisance debugs.
*
*  Revision 1.94  2012/07/24 14:29:47  mmaloney
*  getDataTypesByStandard moved to base class decodes.tsdb.TimeSeriesDb.
*
*  Revision 1.93  2012/07/19 14:50:49  mmaloney
*  Updated for USBR HDB.
*
*  Revision 1.92  2012/07/18 13:42:30  mmaloney
*  Fix timezone issues.
*
*  Revision 1.91  2012/07/17 20:57:17  mmaloney
*  Fix timezone issues.
*
*  Revision 1.90  2012/07/17 20:51:18  mmaloney
*  Fix timezone issues.
*
*  Revision 1.89  2012/07/15 21:33:55  mmaloney
*  Refactor read/write DateFmt. For HDB, always use GMT/UTC.
*
*  Revision 1.88  2012/07/15 19:53:46  mmaloney
*  Refactor read/write DateFmt. For HDB, always use GMT/UTC.
*
*  Revision 1.87  2012/07/12 19:43:06  mmaloney
*  timestamp debugs.
*
*  Revision 1.86  2012/07/12 19:20:53  mmaloney
*  timestamp debugs.
*
*  Revision 1.85  2012/07/12 18:30:46  mmaloney
*  timestamp debugs.
*
*  Revision 1.84  2012/07/12 18:10:21  mmaloney
*  timestamp debugs.
*
*  Revision 1.83  2012/07/12 17:23:44  mmaloney
*  Added debugDateFmt for debug messages
*
*  Revision 1.82  2012/07/11 18:39:56  mmaloney
*  First cut of new daemon to update CP_COMP_DEPENDS.
*
*  Revision 1.81  2012/07/11 18:09:07  mmaloney
*  First cut of new daemon to update CP_COMP_DEPENDS.
*
*  Revision 1.80  2012/07/11 15:41:26  mmaloney
*  First cut of new daemon to update CP_COMP_DEPENDS.
*
*  Revision 1.79  2012/07/05 18:27:04  mmaloney
*  tsKey is stored as a long.
*
*  Revision 1.78  2012/06/18 15:15:39  mmaloney
*  Moved TS ID cache to base class.
*
*  Revision 1.77  2012/06/06 19:15:53  mmaloney
*  writeComputationApp for HDB does auto-sequence.
*
*  Revision 1.76  2012/05/30 21:00:50  mmaloney
*  sql bug fix.
*
*  Revision 1.75  2012/05/30 20:27:03  mmaloney
*  Modifications for TSDB Version 8
*
*  Revision 1.74  2012/05/15 14:27:39  mmaloney
*  1. createTimeSeries calls checkValid, which can throw BadTimeSeriesException.
*  2. transformTsidByCompParm can throw BadTimeSeriesException because
*  it calls createTimeSeries if create flag == true.
*
*  Revision 1.73  2012/05/09 15:33:03  mmaloney
*  Read maxval(version) from tsdb_database_version.
*  This is a work-around to a bug in the updater whereby multiple records were being
*  written into this table with different version numbers.
*
*  Revision 1.72  2012/04/30 18:50:17  mmaloney
*  fillDependentCompIds must filter on loadingAppId.
*
*  Revision 1.71  2012/03/26 19:31:45  mmaloney
*  comment
*
*  Revision 1.70  2011/11/01 22:42:12  mmaloney
*  added getMediumIdForPlatform
*
*  Revision 1.69  2011/10/05 17:07:40  mmaloney
*  moved determineTsdbVersion to this base-class setConnection method. This fixes a bug
*  in TempestWeb where the version had not been set.
*
*  Revision 1.68  2011/06/16 14:06:55  mmaloney
*  move doQuery2 to base class
*
*  Revision 1.67  2011/05/31 17:59:33  mmaloney
*  cwms checks
*
*  Revision 1.66  2011/05/16 13:57:27  mmaloney
*  Don't evaluateCompDepends if CWMS and >= version 7
*
*  Revision 1.65  2011/04/26 18:12:02  mmaloney
*  bug vix.
*
*  Revision 1.64  2011/03/18 14:16:46  mmaloney
*  Made some members public for validations
*
*  Revision 1.63  2011/03/01 15:56:44  mmaloney
*  Implement deleteTimeSeries
*
*  Revision 1.62  2011/02/17 19:22:27  mmaloney
*  bugfix for GUI
*
*  Revision 1.61  2011/02/15 16:52:34  mmaloney
*  Defensive programming: Don't join the task list with any other tables because the
*  join might fail, leaving bogus tasklist entries on the queue forever.
*
*  Revision 1.60  2011/02/08 13:28:56  mmaloney
*  All tsdb reads must be units-savvy.
*
*  Revision 1.59  2011/02/07 18:34:34  mmaloney
*  Got rid of PgTimeSeriesDb intermediate class.
*  TempestTsdb now extends TimeSeriesDb directly.
*
*  Revision 1.58  2011/02/02 20:42:11  mmaloney
*  Implement getValidPartChoices for group editor.
*
*  Revision 1.57  2011/02/02 14:38:53  mmaloney
*  debugs
*
*  Revision 1.56  2011/02/01 15:32:23  gchen
*  *** empty log message ***
*
*  Revision 1.55  2011/01/26 20:49:02  mmaloney
*  Have getValidDurations return getValidIntervalCodes by default.
*
*  Revision 1.54  2011/01/26 19:48:46  gchen
*  Add an abstract method getValidDuration()
*
*  Revision 1.53  2011/01/25 15:59:11  mmaloney
*  For TSDB >= 5 do not cache computations in the resolver.
*
*  Revision 1.52  2011/01/24 18:36:11  mmaloney
*  Fix bug where lock heartbeat timezone was wrong.
*
*  Revision 1.51  2011/01/20 13:09:38  mmaloney
*  Added abstract method listTimeSeries
*
*  Revision 1.50  2011/01/18 15:07:54  mmaloney
*  Count procs using when listing applications
*
*  Revision 1.49  2011/01/18 13:15:13  mmaloney
*  Fix comp parm filtering for site-id for all 3 database types.
*
*  Revision 1.48  2011/01/17 20:49:11  mmaloney
*  Speed up load of computation editor. It was VERY slow because it was doing a one-by-one
*  query for all computations, and doing a deep read. Now it uses a single query to read
*  just what it needs to populate the lists.
*
*  Revision 1.47  2011/01/12 20:39:28  mmaloney
*  dev
*
*  Revision 1.46  2011/01/12 20:35:03  mmaloney
*  dev
*
*  Revision 1.45  2011/01/10 18:56:46  mmaloney
*  When looking up a data type, use UPPER
*
*  Revision 1.44  2011/01/01 21:28:53  mmaloney
*  CWMS Testing
*
*  Revision 1.43  2010/12/23 18:23:36  mmaloney
*  udpated for groups
*
*  Revision 1.42  2010/12/22 16:55:48  mmaloney
*  udpated for groups
*
*  Revision 1.41  2010/12/21 19:20:52  mmaloney
*  group computations
*
*  Revision 1.40  2010/12/08 13:41:01  mmaloney
*  Specify Columns in INSERT statements.
*
*  Revision 1.39  2010/12/05 15:51:34  mmaloney
*  Comp Parm Edits for DCSTool 5.0
*
*  Revision 1.38  2010/11/28 21:05:24  mmaloney
*  Refactoring for CCP Time-Series Groups
*
*  Revision 1.37  2010/11/05 18:21:29  mmaloney
*  Modifications for CMWS
*
*  Revision 1.36  2010/10/22 18:01:24  mmaloney
*  CCP Refactoring
*
*  Revision 1.35  2010/10/05 21:49:48  mmaloney
*  Removed extraneous method to write Computation with boolean 'requireCompId'.
*
*/
package decodes.tsdb;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.HasProperties;
import ilex.var.NamedVariable;
import ilex.var.TimedVariable;
import ilex.var.Variable;

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
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

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
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.AlgorithmDAO;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.ComputationDAO;
import opendcs.dao.DacqEventDAO;
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
import decodes.tsdb.compedit.AlgorithmInList;
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
public abstract class TimeSeriesDb
	implements HasProperties, DatabaseConnectionOwner
{
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

	/** The JDBC connection, must be provided by the connect method. */
	protected Connection conn;

	/** The default statement for queries */
	private Statement queryStmt;
	private ResultSet queryResults = null;
	private Statement queryStmt2;


	/** The default statement for modifies */
	private Statement modStmt;

	/** The logger */
	protected Logger logger;

	/** Used to format timestamps written to the database. */
	protected SimpleDateFormat writeDateFmt = null;

	/** Used to format timestamps read from the database. */
	protected SimpleDateFormat readDateFmt = null;

	/** Used to generate new surrogate keys. */
	protected KeyGenerator keyGenerator;

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

//	private PreparedStatement lockCheckStmt = null;

	protected static String curTimeName = "current_timestamp";
	protected static String maxCompRetryTimeFrmt = "INTERVAL '%d hour'";

	protected int decodesDatabaseVersion = -1;
	protected String decodesDatabaseOptions = "";

	protected String dbUser = "unknown";
	
	protected Properties props = new Properties();

	protected String cpCompDepends_col1 = null;
//	protected TsIdCache tsIdCache = null;
	protected long lastTsidCacheRead = 0L;
	protected SimpleDateFormat debugDateFmt = 
		new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss z");

	protected Calendar readCal = null;
	private OracleDateParser oracleDateParser = null;
	protected String databaseTimezone = "UTC";
	protected boolean _isOracle = false;
	
	/**
	 * Lazy initialization, called at the first time a date or timestamp
	 * is needing to be parsed or formatted. Can't do this in the constructor
	 * because the database is not yet connected so _dbio.isOracle doesn't work.
	 */
	/**
	 * No args constructor required.
	 */
	public TimeSeriesDb()
	{
//		DecodesSettings settings = DecodesSettings.instance();
		
		writeModelRunId = Constants.undefinedIntKey;
		testMode = false;
		conn = null;
		queryStmt = modStmt = queryStmt2 = null;
		logger = Logger.instance();
		tsdbVersion = 1;


		keyGenerator = null;

		cpCompDepends_col1 = isHdb() ? "TS_ID" : "SITE_DATATYPE_ID";
		getLogDateFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/** @return the JDBC connection in use by this object. */
	public Connection getConnection() { return conn; }

	/**
	 * Sets the JDBC connection in use by this object. 
	 * @param conn the connection
	 */
	public void setConnection(Connection conn)
	{
		this.conn = conn;
		determineTsdbVersion();
	}

	public KeyGenerator getKeyGenerator() { return keyGenerator; }
	
	/**
	 * Uses the class in DecodesSettings to create a key generator.
	 * @throws BadConnectException on any error.
	 */
	protected void setupKeyGenerator()
		throws BadConnectException
	{
		String keyGenClass = DecodesSettings.instance().sqlKeyGenerator;
		try
		{
			keyGenerator = KeyGeneratorFactory.makeKeyGenerator(
				keyGenClass, conn);
		}
		catch (Exception ex)
		{
			throw new BadConnectException(
				"Cannot initialize key generator from class '" + keyGenClass
					+ "' :" + ex.toString());
		}
	}	

	
	public DbKey getKey(String tableName)
		throws DbIoException
	{
		try { return keyGenerator.getKey(tableName, conn); }
		catch(decodes.db.DatabaseException ex)
		{
			throw new DbIoException("Cannot generate key for '" + tableName
				+ "': " + ex);
		}

	}

	//==================================================================
	// The following helper-methods may be called or overloaded by the
	// concrete subclass.
	//==================================================================

	/**
	 * Does a SQL query with the default static statement & returns the
	 * result set.
	 * Warning: this method is not thread and nested-loop safe.
	 * If you need to do nested queries, you must create a separate
	 * statement and do the inside query yourself. Likewise, if called
	 * from multiple threads, an external synchronization mechanism is
	 * needed.
	 * @param q the query
	 * @return the result set
	 */
	public ResultSet doQuery(String q)
		throws DbIoException
	{
		if (queryStmt != null)
		{
			try { queryStmt.close(); }
			catch(Exception ex) {}
			queryStmt = null;
		}
		if (queryResults != null)
		{
			try { queryResults.close(); }
			catch(Exception ex) {}
			queryResults = null;
		}
		try
		{
			if (conn == null)
			{
				String msg = "doQuery() Invalid connection" +
							" object, conn = " + conn;
				warning(msg);
				throw new DbIoException(msg);
			}
			queryStmt = conn.createStatement();
			debug3("Query1 '" + q + "'");
			return queryResults = queryStmt.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	/** An extra do-query for inside-loop queries. */
	public ResultSet doQuery2(String q) throws DbIoException
	{
		if (queryStmt2 != null)
		{
			try
			{
				queryStmt2.close();
			}
			catch (Exception ex)
			{
			}
			queryStmt2 = null;
		}
		try
		{
			if (queryStmt2 != null)
				queryStmt2.close();
			if (conn == null)
			{
				String msg = "doQuery2() Invalid connection"
						+ " object, conn = " + conn;
				warning(msg);
				throw new DbIoException(msg);
			}
			queryStmt2 = conn.createStatement();
			Logger.instance().debug3("Query2 '" + q + "'");
			return queryStmt2.executeQuery(q);
		}
		catch (SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}



	/**
	* Executes an UPDATE or INSERT query.
	* Thread safe: internally synchronized on the modify-statement.
	* @param q the query string
	* @param silent false to print warning on error, true if not.
	* @throws DatabaseException  if the update fails.
	*/
	public int doModify(String q, boolean silent)
		throws DbIoException
	{
		try
		{
			if (conn == null)
			{
				String msg = "TimeSeriesDb doModify() Invalid connection" +
							" object, conn = " + conn;
				warning(msg);
				throw new DbIoException(msg);
			}
			modStmt = conn.createStatement();
			if (!q.equals("COMMIT"))
				logger.debug3("Executing statement '" + q + "'");
			int numChanged = modStmt.executeUpdate(q);
			return numChanged;
// Don't do this. Some queries are OK if they modify nothing.
//			if (numChanged == 0) 
//			{
//				String msg = "Failure in modify query '" + q + "'";
//				Logger.instance().warning(msg);
//				throw new DbIoException(msg);
//			}
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			if (!silent)
				warning(msg);
			throw new DbIoException(msg);
		}
		finally
		{
			if (modStmt != null)
			{
				try { modStmt.close(); }
				catch(Exception ex) {}
				modStmt = null;
			}
		}
	}

	/**
	 * Execute a modify query.
	 * Print warning on error.
	 */
	public int doModify(String q)
		throws DbIoException
	{
		return doModify(q, false);
	}


	/** @return string representation for a boolean value in this db. */
	public String sqlBoolean(boolean v)
	{
		return v ? "'Y'" : "'N'";
	}

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
		try 
		{
			conn.commit();
			conn.clearWarnings(); 
		}
		catch(SQLException ex) {}
	}

	public void rollback()
	{
		try { doModify("ROLLBACK"); }
		catch(Exception ex) {}
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
			Logger.instance().warning("Error retrieving boolean: " + ex);
			return false;
		}
	}
	


	//===================================================================
	// The following methods form the main interface for a time series 
	// database. The abstract ones must be overloaded.
	//===================================================================

	/**
	 * Connect this app to the database and return appID. 
	 * The credentials property set contains username, password,
	 * etc, for connecting to database.
	 * <p>
	 * Implementation: if the method fails, it MUST set conn = null.
	 * @param appName must match an application in the database.
	 * @param credentials must contain all needed login parameters.
	 * @return application ID.
	 * @throws BadConnectException if failure to connect.
	 */
	public abstract DbKey connect( String appName, Properties credentials )
		throws BadConnectException;
	
	
	/**
	 * Provides common post-connect initialization like loading the intervals
	 * and setting the application ID.
	 */
	public void postConnectInit(String appName)
		throws BadConnectException
	{
		determineTsdbVersion();

		// If an application name is provided, lookup the ID.
		if (appName != null && appName.trim().length() > 0)
		{	
			LoadingAppDAI loadingAppDAO = makeLoadingAppDAO();
			try
			{
				appId = loadingAppDAO.lookupAppId(appName);
				info("Connected to " + module + " with app name '"
					+ appName + "' appId=" + appId + ", tsdbVersion=" + tsdbVersion);
			}
			catch(Exception ex)
			{
				String msg = "Cannot determine app ID for name '" + appName + "': " + ex;
				failure(msg);
				throw new BadConnectException(msg);
			}
		}
		
		// Load the intervals
		IntervalDAI intervalDAO = this.makeIntervalDAO();
		try { intervalDAO.loadAllIntervals(); }
		catch(Exception ex)
		{
			String msg = "Cannot load intervals: " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Unconditionally close the connection.
	 */
	public void closeConnection()
	{
		info("Closing database connection.");
		try
		{
			if (queryStmt != null)
				queryStmt.close();
			if (queryStmt2 != null)
				queryStmt2.close();
			if (conn != null)
				conn.close();
		}
		catch(Exception ex) {}
		conn = null;
		queryStmt = null;
		queryStmt2 = null;
//		lockCheckStmt = null;
	}

	/**
	 * @return true if the database is currently connected. False otherwise.
	 */
	public boolean isConnected()
	{
		try { return conn != null && !conn.isClosed(); }
		catch(Exception ex)
		{
			warning(
				"Error testing whether connection closed: " + ex);
			closeConnection();
			return false;
		}
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
	 * Called from GUI when making assignment from site ID & data type.
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
	 * Returns an DataCollection containing zero or more TimeSeries,
	 * containing all data added or deleted since the last call of this 
	 * method by this application ID.
	 * <p>
	 * New values are marked with the DB_ADDED flag. Deleted values 
	 * marked with the DB_DELETED flag.
	 * @param applicationId used to lookup & save the since time.
	 * @return DataCollection with newly added or deleted values.
	 * @throws DbIoException on Database IO error.
	 */
	public DataCollection getNewData( DbKey applicationId )
		throws DbIoException
	{
		return getNewDataSince(applicationId, null);
	}
	
	/**
	 * Similar to getNewData, this method allows caller to explicitly
	 * specify a since time.
	 * @param applicationId used to lookup & save the since time.
	 * @return an IDataCollection containing several ITimeSeries
	 * representing all data added to the database since the
	 * specified date (inclusive).
	 * @throws DbIoException on Database IO error.
	 */
	public abstract DataCollection getNewDataSince( DbKey applicationId, 
		Date sinceTime)
		throws DbIoException;

	/**
	 * Releases triggers associated with the new data in the passed collection.
	 * The implementation may use information contained in the collection's
	 * opaque handle.
	 * @param dc the data collection to be released.
	 */
	public void releaseNewData(DataCollection dc)
		throws DbIoException
	{
		RecordRangeHandle rrh = dc.getTasklistHandle();
		if (rrh == null)
			return;
		while(rrh.size() > 0)
		{
			String q = "delete from CP_COMP_TASKLIST "
			  + "where RECORD_NUM IN (" + rrh.getRecNumList(250) + ")";
			doModify(q);
			commit();
		}

		int maxRetries = DecodesSettings.instance().maxComputationRetries;
		boolean doRetryFailed = DecodesSettings.instance().retryFailedComputations;

		while(rrh.getFailedRecnums().size() > 0)
		{
			String failRecNumList = rrh.getFailedRecNumList(250);

			// Add the retry limit for failed computations
			if (doRetryFailed && maxRetries > 0)
			{
				String q = "delete from CP_COMP_TASKLIST "
						  + "where RECORD_NUM IN (" + failRecNumList + ") "
						  + "and ((" + curTimeName + " - DATE_TIME_LOADED) > "
						  + String.format(maxCompRetryTimeFrmt, maxRetries) + ")";
				doModify(q);
				commit();
				q = "update CP_COMP_TASKLIST set FAIL_TIME = " + curTimeName +
					" where RECORD_NUM in(" + failRecNumList + ")" + 
					" and ((" + curTimeName + " - DATE_TIME_LOADED) <= "
					+ String.format(maxCompRetryTimeFrmt, maxRetries) + ")";
				doModify(q);
				commit();
			} 
			else
			{
				// DB V5 handles failed computations by setting a FAIL_TIME
				// on the task list record. Previous version just delete record.
				String q = (tsdbVersion >= 4 && doRetryFailed)
				  ?	("UPDATE CP_COMP_TASKLIST SET FAIL_TIME = " + curTimeName)
				  : "DELETE FROM CP_COMP_TASKLIST ";
				q = q + " WHERE RECORD_NUM IN (" 
					+ failRecNumList + ")";
				doModify(q);
				commit();
			}
		}
	}

	/**
	 * @return a list of all computation names defined in the database.
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> listComputations( )
		throws DbIoException
	{
		String q = "select COMPUTATION_NAME from CP_COMPUTATION";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing computations: " + ex;
			logger.warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	/**
	 * @return a list of computation names defined in the database.
	 * thats has an id in the passed inlist
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> ComputationsIn(String inList)
		throws DbIoException
	{
		String q = "select COMPUTATION_NAME from CP_COMPUTATION";
		String w = "";
		if (inList.length() != 0 && inList.indexOf(":") == -1)
		{
			w = " where computation_id in (" +
			inList  + ")";
			q = q + w;	
		}
		else if (inList.length() != 0 && inList.indexOf(":") != -1)
		{
			int i = inList.indexOf(":");
			w = " where computation_id between " +
			inList.substring(0,i)  + " and "
			+ inList.substring(i+1);
			q = q + w;	
		}
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing computations: " + ex;
			logger.warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	/**
	 * Return a list of all computations that use the specified algorithm.
	 * @param algorithmName the algorithm name
	 * @return a list of all computations that use the specified algorithm.
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> listComputationsByAlgorithm(String algorithmName)
		throws DbIoException
	{
		String q = 
			"select COMPUTATION_NAME from CP_COMPUTATION, CP_ALGORITHM "
		  + "where CP_COMPUTATION.ALGORITHM_ID = CP_ALGORITHM.ALGORITHM_ID "
		  + "and CP_ALGORITHM.ALGORITHM_NAME = '" + algorithmName + "'";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing computations: " + ex;
			logger.warning(msg);
			throw new DbIoException(msg);
		}
	}
	

	/**
	 * @return a list of names of all algorithms defined in thie database.
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> listAlgorithms( )
		throws DbIoException
	{
		String q = "select ALGORITHM_NAME from CP_ALGORITHM";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing algorithms: " + ex;
			logger.warning(msg);	
			throw new DbIoException(msg);
		}
	}

	/**
	 * @return an in list of names of algorithms defined in the database.
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> AlgorithmsIn(String inList)
		throws DbIoException
	{
		String q = "select ALGORITHM_NAME from CP_ALGORITHM";
                String w = "";
                if (inList.length() != 0 )
                {
                        w = " where algorithm_id in (" +
                        inList  + ")";
                        q = q + w;
                }

		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing algorithms: " + ex;
			logger.warning(msg);	
			throw new DbIoException(msg);
		}
	}

	public ArrayList<AlgorithmInList> listAlgorithmsForGui()
		throws DbIoException
	{
		String q = "select algorithm_id, algorithm_name, "
			+ "exec_class, cmmnt "
			+ "from cp_algorithm";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<AlgorithmInList> ret = new ArrayList<AlgorithmInList>();
			while(rs != null && rs.next())
				ret.add(new AlgorithmInList(DbKey.createDbKey(rs, 1), rs.getString(2),
					rs.getString(3), 0, TextUtil.getFirstLine(rs.getString(4))));
			
			q = "select a.algorithm_id, count(1) as CompsUsingAlgo "
				+ "from cp_algorithm a, cp_computation b "
				+ "where a.algorithm_id = b.algorithm_id "
				+ "group by a.algorithm_id";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey algoId = DbKey.createDbKey(rs, 1);
				int numCompsUsing = rs.getInt(2);
				for(AlgorithmInList ail : ret)
				{
					if (ail.getAlgorithmId().equals(algoId))
					{
						ail.setNumCompsUsing(numCompsUsing);
						break;
					}
				}
			}
		
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing algorithms for GUI: " + ex;
			logger.warning(msg);	
			throw new DbIoException(msg);
		}
	}
	

//	/**
//	 * Deletes a computation with the specified computation ID.
//	 * Does nothing if no such computation exists.
//	 * @param id the computation ID
//	 * @throws DbIoException on Database IO error.
//	 * @throws ConstraintException if this comp cannot be deleted because
//	 *  other records in the DB (like data) depend on it.
//	 */
//	public void deleteComputation(DbKey id)
//		throws DbIoException, ConstraintException
//	{
//		String q = "delete from CP_COMP_TS_PARM where COMPUTATION_ID = " + id;
//		doModify(q);
//		q = "delete from CP_COMP_PROPERTY where COMPUTATION_ID = " + id;
//		doModify(q);
//		q = "delete from CP_COMPUTATION where COMPUTATION_ID = "+id;
//		doModify(q);
//		
//		// Remove the CP_COMP_DEPENDS records & re-add.
//		if (tsdbVersion >= 5 && !isHdb())
//		{
//			deleteCompDependsForCompId(id);
//		}
//		
//		commit();
//	}

	/**
	 * Given a computation name, return the unique ID.
	 * @param name the name
	 * @return the id
	 * @throws NoSuchObjectException if no match found
	 */
	public DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select COMPUTATION_ID from CP_COMPUTATION "
			+ "where COMPUTATION_NAME = '" + name + "'";
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			throw new NoSuchObjectException("No computation for name '" + name 
				+ "'");
		}
		catch(SQLException ex)
		{
			String msg = "Error getting computation ID for name '" 
				+ name + "': " + ex;
			logger.failure(msg);
			throw new DbIoException(msg);
		}
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
	 * Returns the modelID for a given modelRunId.
	 * @param modelRunId the model run ID
	 * @return the modelID for a given modelRunId.
	 */
	public abstract int findModelId(int modelRunId)
		throws DbIoException;

	/**
	 * Returns the maximum valid run-id for the specified model.
	 * @param modelId the ID of the model
	 * @return the maximum valid run-id for the specified model.
	 */
	public abstract int findMaxModelRunId(int modelId)
		throws DbIoException;

	/**
	 * Finds the correct coefficient stored in the database for a specific set
	 * of sdi, table selector, interval, and date. Assumes Oct 1 as beginning 
	 * of year.
	 * @param sdi the Site datatype id
	 * @param ts the table selector
	 * @param interval the interval
	 * @param date the date of interest
	 * @return the value of the coefficient
	 */
	public double getCoeff(DbKey sdi, String ts, String interval, 
		Date date)
		throws DbIoException
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
		try
		{
			String q = "SELECT id, standard FROM DataType WHERE upper(code) = " 
				+ sqlString(dtcode.toUpperCase());
			DataType pref = null;
			DataType first = null;
			
			ResultSet rs = doQuery2(q);
			while(rs != null && rs.next())
			{
				DbKey id = DbKey.createDbKey(rs, 1);
				String std = rs.getString(2);
				DataType dt = DataType.getDataType(std, dtcode, id);
				if (first == null)
					first = dt;
				if (std.equalsIgnoreCase(
					DecodesSettings.instance().dataTypeStdPreference))
					pref = dt;
			}
			if (pref != null)
				return pref;
			else if (first == null)
				throw new NoSuchObjectException(
			"No data type with code '"+dtcode+"' exists. "
			+ " We suggest adding it to one of your presentation groups" +
			" in the DECODES Database Editor, Presentation tab.");
			return first;
		}
		catch(SQLException ex)
		{
			throw new DbIoException("lookupDataTypeId: " + ex);
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
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId)
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

	/** Given int flags value, return char limit codes. */
	public abstract String flags2LimitCodes(int flags);
	
	/** @return label to use for 'revision' column in tables. */
	public String getRevisionLabel() { return "Rev"; }

	/** Given int flags value, return char revision codes. */
	public abstract String flags2RevisionCodes(int flags);
	
	/** Creates and return the CompFilter class used in the
	 * Comp Edit Db Computation List. This CompFilter is used to filter
	 * the Db Computations that will show up in the list */
	public CompFilter getCompFilter()
	{
		return new CompFilter();
	}

	public int getDataSourceId(DbKey appId, DbComputation comp)
		throws DbIoException
	{
		return 0;
	}

	/** @return the Time Series Database Version */
	public int getTsdbVersion() { return tsdbVersion; }

	/** @return the DECODES Database Version */
	public int getDecodesDatabaseVersion()
	{
		return decodesDatabaseVersion;
	}

	/**
	 * Given an EU abbreviation, read the EU object with full name.
	 */
	public EngineeringUnit readEngineeringUnit(String abbr)
		throws DbIoException
	{
		EngineeringUnit eu = EngineeringUnit.getEngineeringUnit(abbr);
		if (eu.getFamily() != null)
			return eu;

		String q = "SELECT name, family, measures "
			+ "FROM EngineeringUnit "
			+ "WHERE unitAbbr = " + sqlString(abbr);
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				eu.setName(rs.getString(1));
				eu.setFamily(rs.getString(2));		
				eu.setMeasures(rs.getString(3));
			}
		}
		catch(SQLException ex)
		{
			String msg = "readEngineeringUnit: " + ex;
			logger.warning(msg);
		}
		return eu;
	}
	
	public void determineTsdbVersion()
	{
		try
		{
			DatabaseMetaData metaData = getConnection().getMetaData();
			String dbName = metaData.getDatabaseProductName();
			Logger.instance().info("Connected to database server: "
				+ dbName + " " + metaData.getDatabaseProductVersion());
			_isOracle = dbName.toLowerCase().contains("oracle");
		}
		catch (SQLException ex)
		{
			String msg = "determineTsdbVersion() " + 
				"Cannot determine Database Product name and/or version: " + ex;
			Logger.instance().warning(msg);
		}
		
		TimeZone tz = TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone);
		String writeFmt = DecodesSettings.instance().sqlDateFormat;
		String readFmt = DecodesSettings.instance().SqlReadDateFormat;
		if (_isOracle)
		{
			oracleDateParser = new OracleDateParser(tz);
			writeFmt = "'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')";
			readFmt = "yyyy-MM-dd HH:mm:ss";
		}
		writeDateFmt = new SimpleDateFormat(writeFmt);
		writeDateFmt.setTimeZone(tz);
		readDateFmt = new SimpleDateFormat(readFmt);
		readDateFmt.setTimeZone(tz);
		readCal = Calendar.getInstance(tz);

		SqlDatabaseIO.readVersionInfo(this);
		readVersionInfo(this);
		info("Connected to TSDB Version " + tsdbVersion + ", Description: " + tsdbDescription);
		readTsdbProperties();
		cpCompDepends_col1 = isHdb() || tsdbVersion >= TsdbDatabaseVersion.VERSION_9 
			? "TS_ID" : "SITE_DATATYPE_ID";
		
	}
	
	public static void readVersionInfo(DatabaseConnectionOwner dco)
	{
		/*
		  Attempt to read the database's version number.
		*/
		int tsdbVersion = TsdbDatabaseVersion.VERSION_2;  // earliest possible value.
		String tsdbDescription = "";	
		String q = "SELECT * FROM tsdb_database_version";
		Statement stmt = null;
		try
		{
			stmt = dco.getConnection().createStatement();

			ResultSet rs = stmt.executeQuery(q);
			while (rs != null && rs.next())
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
			String msg = "readVersionInfo() " + 
				"Cannot determine TimeSeries Database version: " + ex;
			Logger.instance().warning(msg);
			tsdbVersion = TsdbDatabaseVersion.VERSION_2;  // earliest possible value.
		}
		finally
		{
			if (stmt != null)
			{
				try { stmt.close(); }
				catch(Exception ex) {}
			}
		}
		dco.setTsdbVersion(tsdbVersion, tsdbDescription);
	}

	public void readTsdbProperties()
	{
		String q = "SELECT prop_name, prop_value FROM tsdb_property";
		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				String nm = rs.getString(1);
				String vl = rs.getString(2);
				setProperty(nm, vl);
			}
		}
		catch(Exception ex)
		{
			String msg = "readTsdbProperties() " + 
				"Cannot read TimeSeries Database properties: " + ex;
			logger.warning(msg);
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

	public void debug(String msg)
	{
		Logger.instance().debug2(module + " " + msg);
	}
	public void debug1(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}
	public void debug2(String msg)
	{
		Logger.instance().debug2(module + " " + msg);
	}
	public void debug3(String msg)
	{
		Logger.instance().debug3(module + " " + msg);
	}
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.instance().warning(module + " " + msg);
	}
	public void failure(String msg)
	{
		Logger.instance().failure(module + " " + msg);
	}
	public void fatal(String msg)
	{
		Logger.instance().fatal(module + " " + msg);
	}
	
	/**
	 * Construct a new TSID object appropriate for this DB, but do no I/O.
	 * @return new, empty TSID object.
	 */
	public abstract TimeSeriesIdentifier makeEmptyTsId();

	/**
	 * Lists the Time Series Groups.
	 * @param groupType type of groups to list, null to list all groups.
	 * @return ArrayList of un-expanded TS Groups.
	 */
	public ArrayList<TsGroup> getTsGroupList(String groupType)
		throws DbIoException
	{
		TsGroupDAI tsGroupDAO = makeTsGroupDAO();
		try
		{
			return tsGroupDAO.getTsGroupList(groupType);
		}
		finally
		{
			tsGroupDAO.close();
		}
	}
	
	/**
	 * @return a TsGroup by its unique name.
	 */
	public TsGroup getTsGroupByName(String grpName)
		throws DbIoException
	{
		TsGroupDAI tsGroupDAO = makeTsGroupDAO();
		try
		{
			return tsGroupDAO.getTsGroupByName(grpName);
		}
		finally
		{
			tsGroupDAO.close();
		}
	}

	/**
	 * @return a TsGroup by its surrogate key.
	 */
	public TsGroup getTsGroupById(DbKey id)
		throws DbIoException
	{
		if (id == null || id.isNull())
			return null;
		TsGroupDAI tsGroupDAO = makeTsGroupDAO();
		try
		{
			return tsGroupDAO.getTsGroupById(id);
		}
		finally
		{
			tsGroupDAO.close();
		}
	}

	/**
	 * Writes a group to the database.
	 * @param group the group
	 */
	public void writeTsGroup(TsGroup group)
		throws DbIoException
	{
		// Save ID before write
		DbKey id = group.getGroupId();
		
		TsGroupDAI tsGroupDAO = makeTsGroupDAO();
		try
		{
			tsGroupDAO.writeTsGroup(group);
		}
		finally
		{
			tsGroupDAO.close();
		}

		// If previously existed and is used by any comps, then we have
		// to re-evalute the computation dependencies with the new group
		// definition.
		if (!id.isNull())
		{
			ComputationDAI computationDAO = makeComputationDAO();
			CompDependsDAI compDependsDAO = makeCompDependsDAO();
			try
			{
				ArrayList<DbKey> affected = new ArrayList<DbKey>();
				affected.add(id);
				findAffectedGroups(id, affected);

				StringBuilder whereClause = new StringBuilder("where group_id in (");
				for(DbKey groupId : affected)
					whereClause.append("" + groupId + ",");
				whereClause.deleteCharAt(whereClause.length()-1);
				whereClause.append(")");
				
				String q = tsdbVersion < TsdbDatabaseVersion.VERSION_6
					? ("SELECT DISTINCT COMPUTATION_ID FROM CP_COMP_TS_PARM "
						+ whereClause.toString())
					: ("SELECT COMPUTATION_ID FROM CP_COMPUTATION "
						+ whereClause.toString());
				
				ArrayList<DbKey> compIds = new ArrayList<DbKey>();
				ResultSet rs = doQuery(q);
				while (rs.next())
					compIds.add(DbKey.createDbKey(rs, 1));
				for(DbKey compId : compIds)
				{
					try
					{
						DbComputation comp = computationDAO.getComputationById(compId);
						compDependsDAO.writeCompDepends(comp);
					}
					catch(NoSuchObjectException ex) {}
				}
				commit();
			}
			catch(SQLException ex)
			{
				String msg = " Error setting comp-dependencies: " + ex;
				warning(msg);
				throw new DbIoException(msg);
			}
			finally
			{
				computationDAO.close();
				compDependsDAO.close();
			}
		}
	}
	
	/**
	 * Recursive function to find all of the group IDs that are 'affected' by
	 * the passed groupId. That is, find groups that either include or exclude
	 * the passed groupId.
	 * @param groupId
	 * @param affectedGroupIds
	 */
	private void findAffectedGroups(DbKey groupId, ArrayList<DbKey> affectedGroupIds)
		throws DbIoException, SQLException
	{
		String q = "select parent_group_id from tsdb_group_member_group "
			+ "where child_group_id = " + groupId;
		ResultSet rs = doQuery(q);
		ArrayList<DbKey> tmp = new ArrayList<DbKey>();
		while (rs.next())
		{
			DbKey parentGroupId = DbKey.createDbKey(rs, 1);
			tmp.add(parentGroupId);
		}
		for(DbKey parentGroupId : tmp)
		{
			if (!affectedGroupIds.contains(parentGroupId))
			{
				affectedGroupIds.add(parentGroupId);
				findAffectedGroups(parentGroupId, affectedGroupIds);
			}
		}
	}

	/**
	 * Deletes a group from the database.
	 * @param groupId the group id to delete
	 */
	public void deleteTsGroup(DbKey groupId)
		throws DbIoException
	{
		if (groupId == null || groupId.isNull())
			return;

		// If previously existed and is used by any comps, then we have
		// to re-evalute the computation dependencies with the deleted group
		// definition.
		if (groupId != Constants.undefinedId)
		{
			String q = tsdbVersion < TsdbDatabaseVersion.VERSION_6
				? "SELECT DISTINCT COMPUTATION_ID FROM CP_COMP_TS_PARM "
				: "SELECT COMPUTATION_ID FROM CP_COMPUTATION ";
			q = q + " where group_id = " + groupId;
					
			try
			{
				int numDependentComps = 0;
				ResultSet rs = doQuery(q);
				StringBuilder whereClause = new StringBuilder("where computation_id in(");
				while (rs.next())
				{
					whereClause.append("" + rs.getInt(1) + ",");
					numDependentComps++;
				}
				whereClause.deleteCharAt(whereClause.length()-1);
				whereClause.append(")");
				if (numDependentComps > 0)
				{
					q = tsdbVersion < 6
						? ("UPDATE CP_COMP_TS_PARM SET GROUP_ID = ")
						: ("UPDATE CP_COMPUTATION SET GROUP_ID = ");
					q = q + Constants.undefinedId + " " + whereClause.toString();
					doModify(q);
					q = "UPDATE CP_COMPUTATION SET ENABLED = 'N' " 
						+ whereClause.toString();
					doModify(q);
					if (!isHdb())
					{
						q = "DELETE FROM CP_COMP_DEPENDS " + whereClause.toString();
						doModify(q);
					}
				}

				TsGroupDAI tsGroupDAO = makeTsGroupDAO();
				try
				{
					tsGroupDAO.deleteTsGroup(groupId);
				}
				finally
				{
					tsGroupDAO.close();
				}

				commit();
			}
			catch(SQLException ex)
			{
				String msg = " Error deleting ts group & dependencies: " + ex;
				warning(msg);
				throw new DbIoException(msg);
			}
		}

	}

//	/**
//	 * Reads the members of an individual group.
//	 * @param group the group
//	 */
//	public void readTsGroupMembers(TsGroup group)
//		throws DbIoException
//	{
//		TsGroupDAI tsGroupDAO = makeTsGroupDAO();
//		try
//		{
//			tsGroupDAO.readTsGroupMembers(group);
//		}
//		finally
//		{
//			tsGroupDAO.close();
//		}
//	}

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


	//TODO: After CWMS uses daemon CompDependsUpdater, remove this method
	/**
	 * Rebuilds the entire CP_COMP_DEPENDS table. This is done after database
	 * upgrades only.
	 * @throws DbIoException
	 */
	public void makeCpCompDepends()
		throws DbIoException
	{
		if (isHdb())
			return;
		String q = "DELETE from CP_COMP_DEPENDS";
		doModify(q);

		q = "SELECT COMPUTATION_ID FROM CP_COMPUTATION where ENABLED = 'Y'";
		ArrayList<DbKey> compIds = new ArrayList<DbKey>();
		ResultSet rs = doQuery(q);
		ComputationDAI computationDAO = makeComputationDAO();
		CompDependsDAI compDependsDAO = makeCompDependsDAO();
		try
		{
			while (rs.next())
				compIds.add(DbKey.createDbKey(rs, 1));
			for(DbKey compId : compIds)
			{
				try
				{
					DbComputation comp = computationDAO.getComputationById(compId);
					compDependsDAO.writeCompDepends(comp);
				}
				catch(NoSuchObjectException ex) {}
			}
		}
		catch(SQLException ex)
		{
			warning("makeCpCompDepends: " + ex);
		}
		finally
		{
			computationDAO.close();
			compDependsDAO.close();
		}
		commit();
	}

	
	public String getDbUser() { return dbUser; }
	
	public boolean isCwms() { return false; }
	public boolean isHdb() { return false; }

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
		Statement stmt = null;
		try
		{
			stmt = conn.createStatement();
			debug("getDataTypesByStandard '" + q + "'");
			ResultSet rs = stmt.executeQuery(q);
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			warning(msg);
		}
		finally
		{
			try { if (stmt != null) stmt.close(); } catch (Exception ex) {}
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
	 * @param cts
	 * @return
	 */
	public int fillDependentCompIds(CTimeSeries cts, DbKey loadingAppId)
	{
		cts.getDependentCompIds().clear();
		String q = "select a.computation_id from cp_comp_depends a, cp_computation b"
			+ " where a." + cpCompDepends_col1 + " = " + cts.getTimeSeriesIdentifier().getKey()
			+ " and a.computation_id = b.computation_id"
			+ " and b.loading_application_id = " + loadingAppId;
		try
		{
			ResultSet rs = doQuery(q);
			while (rs.next())
				cts.addDependentCompId(DbKey.createDbKey(rs, 1));
		}
		catch(Exception ex)
		{
			warning("fillDependentCompIds: " + ex);
		}
		return cts.getDependentCompIds().size();
	}
	
//	public void removeTsDependencies(TimeSeriesIdentifier tsid)
//		throws DbIoException
//	{
//		DbKey key = tsid.getKey();
//		// Remove any computation dependencies to this time-series.
//		doModify("DELETE FROM CP_COMP_DEPENDS WHERE " + cpCompDepends_col1 + " = " 
//			+ key);
//		
//		// Disable any computations that use this time-series as input or
//		// output.
//		String q = "select distinct a.computation_id from "
//			+ "cp_computation a, cp_comp_ts_parm b "
//			+ "where a.computation_id = b.computation_id "
//			+ "and a.enabled = 'Y' "
//			+ "and b.site_datatype_id = " + key;
//		String mq = "update cp_computation set enabled = 'N' "
//			+ "where computation_id in (" + q + ")";
//		doModify(mq);
//		
//		// If this ts is explicitly included in a group, remove it.
//		q = "delete from tsdb_group_member_ts where "
//			+ (getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 ? "ts_id" : "data_id")
//			+ " = "+key;
//		doModify(q);
//	}
//	
	/**
	 * Return the platform ID of a given type for the specified platform, or null
	 * if there is none.
	 * @param platformID the platform ID
	 * @param mediumType type medium type
	 * @return
	 */
	protected String getMediumIdForPlatform(DbKey platformID, String mediumType)
	{
		String q = "select mediumid from transportmedium"
			+ " where platformId = " + platformID
			+ " and upper(mediumtype) = " + sqlString(mediumType.toUpperCase());
		
		try
		{
			ResultSet rs = doQuery2(q);
			if (rs != null && rs.next())
				return rs.getString(1);
			else
				return null;
		}
		catch(Exception ex)
		{
			warning("Error getting medium ID for platform " + platformID
				+ ": " + ex);
			return null;
		}
	}
	
	/**
	 * Get next CP_COMP_DEPENDS_NOTIFY record and remove it from the table.
	 * @return next CP_COMP_DEPENDS_NOTIFY record or null if none.
	 */
	public CpDependsNotify getCpCompDependsNotify()
	{
		if (this.tsdbVersion < TsdbDatabaseVersion.VERSION_8)
			return null;
		String q = "select RECORD_NUM, EVENT_TYPE, KEY, DATE_TIME_LOADED "
				 + "from CP_DEPENDS_NOTIFY "
				 + "where DATE_TIME_LOADED = "
				 + "(select min(DATE_TIME_LOADED) from CP_DEPENDS_NOTIFY)";
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				CpDependsNotify ret = new CpDependsNotify();
				ret.setRecordNum(rs.getLong(1));
				String s = rs.getString(2);
				if (s != null && s.length() >= 1)
					ret.setEventType(s.charAt(0));
				ret.setKey(DbKey.createDbKey(rs, 3));
				ret.setDateTimeLoaded(getFullDate(rs, 4));
				
				doModify("delete from CP_DEPENDS_NOTIFY where RECORD_NUM = " 
					+ ret.getRecordNum(), true);
				
				return ret;
			}
		}
		catch(Exception ex)
		{
			warning("Error CpCompDependsNotify: " + ex);
		}
		return null;
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
				warning("makeTimeSeries - Bad modelId '" + s + "' -- ignored.");
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
				warning("makeTimeSeries - Bad modelRunId '" + s + "' -- ignored.");
			}
		}
		return ret;
	}

	/**
	 * The underlying database subclass should overload this method if it wants
	 * to implement this capability. The base class implementation logs a warning
	 * and returns.
	 * @param tsid The time series identifier.
	 * @param since write tasklist records that were _loaded_ since this time.
	 */
	public void writeTasklistRecords(TimeSeriesIdentifier tsid, Date since)
		throws NoSuchObjectException, DbIoException, BadTimeSeriesException
	{
		warning("writeTasklistRecord not implemented");
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
			catch(Exception pex)
			{
				warning("Bad date format '" + s + "' (using default): " + pex);
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
		GroupHelper groupHelper = this.makeGroupHelper();
		groupHelper.expandTsGroup(tsGroup);
		return tsGroup.getExpandedList();
	}

}