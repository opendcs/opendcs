/*
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.ldds.BadPasswordException;
import lrgs.lrgsmain.LrgsConfig;
import decodes.db.DatabaseException;
import decodes.sql.KeyGenerator;
import decodes.sql.KeyGeneratorFactory;
import ilex.util.Logger;
import ilex.util.TextUtil;

/**
 * This is the base class for the LRGS database implementation.
 * This class contains all methods needed to insert, update, 
 * retrieve and delete from the lrgs database tables.
 *  
*/
public class LrgsDatabase
{
	/** The JDBC connection, must be provided by the connect method. */
	private Connection conn;

	/** The default statement for queries */
	private Statement queryStmt;

	/** The default statement for modifies */
	private Statement modStmt;

	/** The logger */
	private Logger logger;
	
	/** Used to format timestamps written to the database */
	private SimpleDateFormat dateFmt;
	
	/** Used to format timestamps read from the database */
	private SimpleDateFormat readDateFmt;

	/** Used to generate new surrogate keys. */
	private KeyGenerator keyGenerator;

	/** Used when getting DateTime fields from the database */
	private Calendar resultSetCalendar;

	/** The fields of the lrgs_database */
	private int db_ver;
	private Date db_createTime;
	private String db_createBy;
	private String db_description;

	/** The URL we're currently connected to. */
	private String dbUrl;
	
	private String myHostName = "unknown";

	/**
	 * Lrgs constructor. Initialize all private members.
	 */
	public LrgsDatabase()
	{
		logger = Logger.instance();
		LrgsConfig cfg = LrgsConfig.instance();
		dateFmt = new SimpleDateFormat(cfg.sqlWriteDateFormat);
		readDateFmt = new SimpleDateFormat(cfg.sqlReadDateFormat);
		TimeZone tz = TimeZone.getTimeZone(cfg.sqlTimeZone);
		dateFmt.setTimeZone(tz);
		readDateFmt.setTimeZone(tz);
		keyGenerator = null;
		resultSetCalendar = Calendar.getInstance(tz);
		try { myHostName = InetAddress.getLocalHost().getHostName(); }
		catch (UnknownHostException e) { myHostName = "localhost"; }
	}

	/**
	 * This method returns the current JDBC connection.
	 * 
	 * @return Connection the JDBC Database Connection object
	 */
	public Connection getConnection() 
	{ 
		return conn;
	}
	
	public synchronized Statement createStatement()
		throws SQLException
	{
		return conn.createStatement();
	}
	
	/**
	 * This method returns a KeyGenerator interface by using SQL sequences.
	 *  
	 * @return KeyGenerator interface by using SQL sequences
	 */
	public KeyGenerator getKeyGenerator() 
	{
		return keyGenerator; 
	}

	/**
	 * Does a SQL query with the default static statement & returns the
	 * result set.
	 * Warning: this method is not thread and nested-loop safe.
	 * If you need to do nested queries, you must create a separate
	 * statement and do the inside query yourself. Likewise, if called
	 * from multiple threads, an external synchronization mechanism is
	 * needed.
	 * 
	 * @param q the query
	 * @return the result set
	 */
	public ResultSet doQuery(String q)
		throws LrgsDatabaseException
	{
		try
		{
			if (queryStmt != null)
				queryStmt.close();
			queryStmt = createStatement();
			Logger.instance().debug3("Querying '" + q + "'");
			return queryStmt.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	* Executes an UPDATE or INSERT query.
	* Thread safe: internally synchronized on the modify-statement.
	* 
	* @param q the query string
	* @throws DatabaseException  if the update fails.
	* @return number of records modified
	*/
	public int doModify(String q)
		throws LrgsDatabaseException
	{
		try
		{
			modStmt = createStatement();
			if (!q.equals("COMMIT"))
				logger.debug2("Executing statement '" + q + "'");
			int numChanged = modStmt.executeUpdate(q);
			modStmt.close();
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
			Logger.instance().warning(msg);
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	* Returns an SQL representation of an optional Date / timestamp
	* value.  If the argument is null, this returns the string value
	* "NULL" (without the quotes).  If the argument is not null, then
	* this returns a formated version of the date/time, enclosed in
	* single quotes.
	* 
	* @param d the date value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlDate(Date d)
	{
		if (d == null)
			return "NULL";
		return dateFmt.format(d);
	}

	/**
	 * This method converts a boolean type to a DB String representation.
	 * 
	 * @param boolean value from user to be converted to SQL representation 
	 * @return string representation for a boolean value in this db. 
	 * */
	public String sqlBoolean(boolean v)
	{
		return v ? "'Y'" : "'N'";
	}

	/**
	 * This method returns a string representation for a given string value.
	 * 
	 * @param field the value to be saved in the database
	 * @return string representation for an string value in this db
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
	
	/**
	 * This method returns a string representation for a given value.
	 * 
	 * @param field the value to be saved in the database
	 * @return string representation for a char value in this db. 
	 * */
	public String sqlChar(char field)
	{
		return "'" + field + "'";
	}

	/**
	 * This method returns a boolean representation
	 * for a given SQL ResultSet value.
	 * 
	 * @param rs the result set
	 * @param column the identifier that contains the SQL value
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
	
	/**
	 * This method returns a char representation
	 * for a given SQL ResultSet value.
	 * 
	 * @param rs the result set
	 * @param column the identifier that contains the SQL value
	 * @return char value from the result set.
	 */
	public char getChar(ResultSet rs, int column)
	{
		char returnValue = '\0';
		try
		{
			if (rs != null)
			{
				String s = rs.getString(column);
				if (rs.wasNull())
					returnValue = '\0';
				else
					if (s != null)
						return s.charAt(0);
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("Error retrieving char: " + ex);
			return '\0';
		}
		return returnValue;
	}
	
	/**
	 * Commit a database transaction.
	 */
	public void commit()
		throws LrgsDatabaseException
	{
		try
		{
			conn.commit();
			conn.clearWarnings();
		}
		catch(SQLException ex) {}
	}

	/**
	 * Rollback a database transaction.
	 */
	public void rollback()
	{
		try { doModify("ROLLBACK"); }
		catch(Exception ex) {}
	}

	/**
	 * This method returns a Date representation
	 * for a given SQL ResultSet value.
	 * 
	 * @param rs the result set
	 * @param column the identifier that contains the SQL value
	 * @return a full date, including time information.
	 */
	public Date getFullDate(ResultSet rs, int column)
	{
		String ds = "";
		try
		{
			try
			{
				ds = rs.getString(column);
				if (ds == null)
					return null;
				//Logger.instance().info("Reading date value '" + ds + "'");
				return readDateFmt.parse(ds);
			}
			catch(Exception ex)
			{
				java.sql.Timestamp ts = rs.getTimestamp(column, resultSetCalendar);
				if (rs.wasNull())
					return null;
				else
				{
					Date d = new Date(ts.getTime());
					return d;
				}
			}
		}
		catch(SQLException ex2)
		{
			Logger.instance().warning("Error retrieving date/time: " + ex2);
			return null;
		}
	}
	
	private boolean isOracle()
	{
		return dbUrl != null && dbUrl.toLowerCase().contains("oracle");
	}

	/**
	 * Establish connection to the LRGS Database. The credentials property 
	 * set contains username, password, etc, for connecting to database.
	 * In addition this method reads the values from the lrgs_database table.
	 * 
	 * @param credentials must contain all needed login parameters.
	 * @throws LrgsDatabaseException if failure to connect.
	 */
	public void connect(Properties credentials)	throws LrgsDatabaseException
	{
		LrgsConfig cfg = LrgsConfig.instance();
		String driverClass = cfg.JdbcDriverClass;
		String keyGeneratorClass = cfg.keyGeneratorClass;
		dbUrl = cfg.dbUrl;
		String username = credentials.getProperty("username");
		String password = credentials.getProperty("password");

		try 
		{
			Class.forName(driverClass);		
			conn = DriverManager.getConnection(dbUrl, username, password);
		}
		catch (Exception ex) 
		{
			conn = null;
			throw new LrgsDatabaseException(
				"Error getting JDBC connection using driver '"
				+ driverClass + "' to database at '" + dbUrl
				+ "' for user '" + username + "': " + ex.toString());
		}

		try
		{
			keyGenerator = KeyGeneratorFactory.makeKeyGenerator(
				keyGeneratorClass, conn);
		}
		catch (Exception ex) 
		{
			conn = null;
			throw new LrgsDatabaseException(
				"Cannot initialize key generator from class '"
				+ keyGeneratorClass + "' :"
				+ ex.toString());
		}
		
		if (isOracle())
		{
			String q = "alter session set nls_timestamp_format = 'YYYY-MM-DD HH24:MI:SS'";
			doQuery(q);
			readDateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dateFmt = new SimpleDateFormat("''yyyy-MM-dd HH:mm:ss''");
			Logger.instance().info("dateFmtSpec=" + dateFmt.toPattern() + ", current time="
				+ dateFmt.format(new Date()));
			readDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
		
		// Read the Information from the lrgs_database table and
		// store the values in the local private variables.
		getLrgsDatabase();

		Logger.instance().info("Connected to LRGS database version " + db_ver);
	}

	/**
	 * Get the information from the lrgs_database table and 
	 * set the following private members: db_ver, db_createTime,
	 * db_createdBy and db_description.
	 * 
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	private void getLrgsDatabase() throws LrgsDatabaseException
	{
		String q = 
		"SELECT db_ver, create_time, created_by, description FROM lrgs_database ";			

		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
			{   // Lrgs_database table cannot be empty
				String msg = "Lrgs_database table is empty";
				throw new LrgsDatabaseException(msg);
			} 
			else
			{
				// Set the local values.
				db_ver = rs.getInt(1);            	// the database_ver    
				db_createTime = getFullDate(rs, 2);	// the create_time
				db_createBy = rs.getString(3);    	// created by value
				db_description = rs.getString(4); 	// description
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error while reading the lrgs_database table " + ex;
			logger.failure(msg);
			closeConnection();
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	 * This method reads a Data Source object from the lrgs data_source table.
	 * It fills out a Data Source object with the values read from the table.
	 * 
	 * @param type the type value to search for
	 * @param name the name value to search for
	 * @return DataSource the Data Source object read from DB, or null if no Data Source is found
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	public DataSource getDataSource(String type, String name) throws LrgsDatabaseException
	{
		DataSource dataSource = null;
		String q = 
			"SELECT data_source_id, lrgs_host, data_source_name, data_source_type " +
			"FROM data_source WHERE data_source_type = " +
			sqlString(type) + " AND data_source_name = " + sqlString(name) + "";
		
		try
		{
			ResultSet rs = doQuery(q);
			if (rs != null && rs.next())
			{
				// Set DataSource object.
				dataSource = new DataSource(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4) );	
			}			
		}
		catch(SQLException ex)
		{
			String msg = "Error while reading the data_source table " + ex;
			logger.failure(msg);
			closeConnection();
			throw new LrgsDatabaseException(msg);
		}		
		return dataSource;
	}

	/**
	 * This method returns all the Data Source records found on the data_source table.
	 * 
	 * @return List of DataSource objects, an empty list if no Data Sources are found
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	public List<DataSource> getDataSources(boolean localOnly) throws LrgsDatabaseException
	{
		ArrayList<DataSource> dataSourceList = new ArrayList<DataSource>();
		DataSource dataSource = null;
		String q = 
			"SELECT data_source_id, lrgs_host, data_source_name, data_source_type " +
			"FROM data_source";
		if (localOnly)
			q = q + " WHERE lrgs_host = " + sqlString(myHostName);
		try
		{
			ResultSet rs = doQuery(q);
			// Set DataSource Array List.
			while(rs.next())
			{
				dataSource = new DataSource(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
				dataSourceList.add(dataSource);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error while reading the data_source table " + ex;
			logger.failure(msg);
			closeConnection();
			throw new LrgsDatabaseException(msg);
		}		
		return dataSourceList;
	}
	
	/**
	 * This method saves a Data Source object in the lrgs data_source table.
	 * 
	 * @param dataSource the Data Source object containing the information
	 * @throws LrgsDatabaseException exception if it fails to save the Data Source object
	 */
	public void saveDataSource(DataSource dataSource) throws LrgsDatabaseException
	{
		// If we have a valid dataSource object.
		if (dataSource != null)
		{
			int dataSourceID = dataSource.getDataSourceId();
			String dataSourceName = dataSource.getDataSourceName();
			String dataSourceType = dataSource.getDataSourceType();
			String table = LrgsConstants.dataSourceTable;

			// New Object to insert - name and type cannot be null.
			// Get data source id from Data_sourceIdseq

			try
			{
				// Special for data source -- always try update first.
				int n = 0;
				if (dataSourceID != LrgsConstants.undefinedId)
				{
					// Object to modify, name and type cannot be null.
					String q = "UPDATE data_source SET data_source_name = " + 
						sqlString(dataSourceName) + ", data_source_type = " +
						sqlString(dataSourceType) +
						" WHERE data_source_id = " + dataSourceID;				
					n = doModify(q);
					commit();
					if (n > 0)
						return;
				}
				if (dataSourceID == LrgsConstants.undefinedId)
				{
					dataSourceID = (int)keyGenerator.getKey(table, conn).getValue();
					dataSource.setDataSourceId(dataSourceID);
				}
				
				String q = 
					"INSERT INTO data_source(data_source_id, lrgs_host, data_source_name, "+
					"data_source_type) VALUES (" + dataSourceID + ", " 
					+ sqlString(dataSource.getLrgsHost()) + ", "
					+ sqlString(dataSourceName) + ", " + sqlString(dataSourceType) + ")";
				doModify(q);
				commit();
			}
			catch(DatabaseException ex)
			{
				throw new LrgsDatabaseException("Cannot save data source:" + ex);
			}
		}
	}
	
	/**
	 * This method stores the Dds Connection Stats information in the
	 * lrgs dds_connection table.
	 * 
	 * @param stats the DdsConnectionStats object containing the data to be 
		saved
	 * @throws LrgsDatabaseException exception if it fails to save the Dds 
		Connection Stats object
	 */
	public void logDdsConn(DdsConnectionStats stats) 
		throws LrgsDatabaseException
	{
		// If we have a valid DdsConnectionStats object.
		if (stats != null)
		{
			int connectionID = stats.getConnectionId();

			if (!stats.getInDb())
			{
				// New Object to insert
				// Get connectionID from ConnectionIdseq
				String table = LrgsConstants.ddsConnectionTable;
				try
				{
					connectionID = (int)keyGenerator.getKey(table, conn).getValue();
					stats.setConnectionId(connectionID);
					String fromIpAddr = stats.getFromIpAddr();
					if (fromIpAddr != null && fromIpAddr.length() > 64)
						fromIpAddr = fromIpAddr.substring(0,64);
					String q = 
			"INSERT INTO dds_connection(connection_id, lrgs_host, start_time, " +
			"end_time, from_ip_addr, success_code, username, " +
			"msgs_received, admin_done, protocol_version, last_activity) VALUES (" 
			+ connectionID + ", " + sqlString(myHostName) + ", " +
			sqlDate(stats.getStartTime()) + ", " + sqlDate(stats.getEndTime()) + 
			", " + sqlString(stats.getFromIpAddr()) + ", " + sqlChar(stats.getSuccessCode()) + 
			", " + sqlString(stats.getUserName()) + ", " + stats.getMsgsReceived() + 
			", " + sqlBoolean(stats.isAdmin_done()) 
			+ ", " + stats.getProtocolVersion()
			+ ", " + sqlDate(stats.getLastActivity())
			+ ")";
					doModify(q);
					commit();
					stats.setInDb(true);
				}
				catch(DatabaseException ex)
				{
					throw new LrgsDatabaseException("Cannot create key for table '" + table
						+ "': " + ex);					
				}
			}
			else
			{
				// Object to modify.
				String q = "UPDATE dds_connection SET start_Time = " + sqlDate(stats.getStartTime()) + 
					", end_time = " + sqlDate(stats.getEndTime()) + ", from_ip_addr = " +
					sqlString(stats.getFromIpAddr()) + ", success_code = " + sqlChar(stats.getSuccessCode()) +
					", username = " + sqlString(stats.getUserName()) + ", msgs_received = " + 
					stats.getMsgsReceived() + ", admin_done = " + sqlBoolean(stats.isAdmin_done())
					+ ", protocol_version = " + stats.getProtocolVersion()
					+ ", last_activity = " + sqlDate(stats.getLastActivity())
					+ " WHERE connection_id = " + connectionID;				
				doModify(q);
				commit();
			}
		}
	}
	
	/**
	 * This method returns all the Dds Connection Stats records found on the 
	 * dds_connection table based on the given start and end time. If both start
	 * and end time are given returns all records in between those two dates. 
	 * In addition, it will return all records where end time is null. If
	 * startTime is null, returns all records until endTime. If endTime is null, 
	 * returns all records after the start time. If both times are null, returns
	 * all records.
	 * 
	 * @param startTime specifies the time to start reading Dds Connection records
	 * @param endTime specifies the time to stop reading Dds Connection records
	 * @return List of DdsConnectionStats objects
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	public List<DdsConnectionStats> getConnectionStats(Date startTime, Date endTime) throws LrgsDatabaseException
	{
		ArrayList<DdsConnectionStats> ddsConnectionStatsList = new ArrayList<DdsConnectionStats>();
		DdsConnectionStats ddsConnectionStats = null;
		StringBuffer q = new StringBuffer();
		
		q.append( 
			"SELECT connection_id, lrgs_host, start_time, end_time, from_ip_addr, success_code, " +
			" username, msgs_received, admin_done, protocol_version, last_activity" +
			" FROM dds_connection");
		if (startTime != null && endTime != null)
		{
			q.append(" WHERE start_time < " + sqlDate(endTime) +
					" AND ( end_time > " + sqlDate(startTime) +
					" OR end_time IS NULL)");
		}
		else if (startTime == null && endTime != null)
		{
			q.append(" WHERE end_time <= " + sqlDate(endTime));
		}
		else if (startTime != null && endTime == null)
		{
			q.append(" WHERE start_time >= " + sqlDate(startTime));
		}

		try
		{
			ResultSet rs = doQuery(q.toString());
			// Set DdsConnectionStats Array List.
			while(rs.next())
			{
				ddsConnectionStats = new DdsConnectionStats(rs.getInt(1), rs.getString(2),
					getFullDate(rs, 3),
						getFullDate(rs, 4), rs.getString(5), getChar(rs, 6),
						rs.getString(7), rs.getInt(8), getBoolean(rs, 9),
						rs.getInt(10), getFullDate(rs, 10));
				ddsConnectionStats.setInDb(true);
				ddsConnectionStatsList.add(ddsConnectionStats);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error while reading the dds_connection table " + ex;
			logger.failure(msg);
			closeConnection();
			throw new LrgsDatabaseException(msg);
		}		
		return ddsConnectionStatsList;
	}

	/**
	 * This method stores the Dds Period Stats information in the
	 * lrgs dds_period_Stats table.
	 * 
	 * @param stats the DdsPeriodStats object containing the data to be saved
	 * @throws LrgsDatabaseException exception if it fails to save the Dds Period Stats object
	 */
	public void logDdsPeriodStats(DdsPeriodStats stats) 
		throws LrgsDatabaseException
	{
		// If we have a valid DdsPeriodStats object.
		if (stats != null)
		{
			// Verify if the given DdsPeriodStats object exists in the database
			// or not. If in the DB, do an update otherwise do an insert.
			// Do the check based on the startTime.
			if (!ddsPeriodStatExists(stats.getStartTime()))
			{
				// New Object to insert
				String q = 
			"INSERT INTO dds_period_stats(start_time, lrgs_host, period_duration, " +
			"num_auth, num_unauth, bad_passwords, bad_usernames, " +
			"max_clients, min_clients, ave_clients, msgs_delivered) " +
			"VALUES (" + sqlDate(stats.getStartTime()) + ", " + sqlString(myHostName) + ", " + 
			sqlChar(stats.getPeriodDuration()) + ", " + 
			stats.getNumAuth() + ", " + stats.getNumUnAuth() +
			", " + stats.getBadPasswords() + ", " + stats.getBadUsernames() + 
			", " + stats.getMaxClients() + ", " + stats.getMinClients() + 
			", " + stats.getAveClients() + ", " + stats.getMsgsDelivered()+ ")";
					doModify(q);
					commit();
			}
			else
			{
				// Object to modify.
				String q = "UPDATE dds_period_stats SET period_duration = " + 
						sqlChar(stats.getPeriodDuration()) + 
						", num_auth = " + stats.getNumAuth() + 
						", num_unauth = " + stats.getNumUnAuth() +
						", bad_passwords = " + stats.getBadPasswords() +
						", bad_usernames = " + stats.getBadUsernames() + 
						", max_clients = " + stats.getMaxClients() +
						", min_clients = " + stats.getMinClients() +
						", ave_clients = " + stats.getAveClients() +
						", msgs_delivered = " + stats.getMsgsDelivered() +
						" WHERE start_time = " + sqlDate(stats.getStartTime())
						+ " and lrgs_host = " + sqlString(myHostName);				
				doModify(q);
				commit();
			}
		}
	}
	
	/**
	 * This method is used to verify if the DdsPeriodStats object exists in
	 * the Database or not.
	 * 
	 * @param startTime the time to look up
	 * @return boolean returns true if Dds Period exists false otherwise
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	private boolean ddsPeriodStatExists(Date startTime) throws LrgsDatabaseException
	{
		boolean returnValue = false;
		String  q = "SELECT start_time FROM dds_period_stats WHERE start_time = " + sqlDate(startTime)
			+ " and lrgs_host = " + sqlString(myHostName);
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				returnValue = true;
			}
		} catch (LrgsDatabaseException e)
		{
			throw new LrgsDatabaseException(e.getMessage());
		} catch (SQLException e)
		{
			throw new LrgsDatabaseException(e.getMessage());
		}		
		return returnValue;
	}
	
	/**
	 * This method returns all the Dds Period Stats records found on the 
	 * dds_period_stats table based on the given start and end time.
	 * If both start and end time are given returns all records in between 
	 * those two dates. If startTime is null, returns all records until 
	 * endTime. If endTime is null, returns all records after the start time. 
	 * If both times are null, returns all records.
	 * 
	 * @param startTime specifies the time to start reading Dds Period Stats records
	 * @param endTime specifies the time to stop reading Dds Period Stats records
	 * @return List of DdsPeriodStats objects, empty list if no records found
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	public List<DdsPeriodStats> getPeriodStats(Date startTime, Date endTime)
		throws LrgsDatabaseException
	{
		ArrayList<DdsPeriodStats> ddsPeriodStatsList = 
			new ArrayList<DdsPeriodStats>();
		DdsPeriodStats ddsPeriodStats = null;
		StringBuffer q = new StringBuffer();
		q.append(
	"SELECT start_time, lrgs_host, period_duration, num_auth, num_unauth, bad_passwords," +
	" bad_usernames, max_clients, min_clients, ave_clients, msgs_delivered" +
			" FROM dds_period_stats");

		if (startTime != null && endTime != null)
		{
			q.append(" WHERE start_time >= " + sqlDate(startTime) +
					" AND start_time <= " + sqlDate(endTime));
		}
		else if (startTime == null && endTime != null)
		{
			q.append(" WHERE start_time <= " + sqlDate(endTime));
		}
		else if (startTime != null && endTime == null)
		{
			q.append(" WHERE start_time >= " + sqlDate(startTime));
		}
		
		try
		{
			ResultSet rs = doQuery(q.toString());
			// Set DdsPeriodStats Array List.
			while(rs.next())
			{
				ddsPeriodStats = new DdsPeriodStats(getFullDate(rs, 1), rs.getString(2),
					getChar(rs, 3), rs.getInt(4), rs.getInt(5), rs.getInt(6), 
					rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getDouble(10), 
					rs.getInt(11));
				ddsPeriodStatsList.add(ddsPeriodStats);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error while reading the dds_period_stats table " + ex;
			logger.failure(msg);
			closeConnection();
			throw new LrgsDatabaseException(msg);
		}		
		return ddsPeriodStatsList;
	}

	/**
	 * This method stores the Outage information in one of following lrgs tables:
	 * domsat_gap, system_outage or damsnt_outage. The Outage object will be stored
	 * on these tables depending on the outageType field. If outageType is S "System"
	 * the information will be stored on the system_outage table, if the outageType is
	 * G "Domsat Gap" the information will be stored on the domsat_gap table and if
	 * the outageType is C "Dams-nt outage" the information will be stored on the 
	 * damsnt_outage table.
	 * 
	 * @param outage the Outage object containing the data to be saved
	 * @throws LrgsDatabaseException exception if it fails to save the Outage object
	 */
	public void saveOutage(Outage outage) throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		if (outage == null)
//			return;
//
//		if (!outage.getInDb())
//		{
//			// New Object to insert
//			// Get outageID from OutageIdseq
//			String table = LrgsConstants.outageTable;
//			// Check the outage Type.					
//			if (outage.getOutageType() == LrgsConstants.systemOutageType)
//			{ // Outage Type = S - store in system_outage
//				saveSystemOutage(outage);						
//			}
//			else if (outage.getOutageType() 
//				== LrgsConstants.domsatGapOutageType)
//			{ // Outage Type = G - store in domsat_gap
//				saveDomsatGapOutage(outage);
//			} 
//			else if (outage.getOutageType() 
//				== LrgsConstants.damsntOutageType)
//			{ // Outage Type = C - store in damsnt_outage
//				saveDamsntOutage(outage);
//			}
//			outage.setInDb(true);
//		}
//		else
//		{
//			// Object to modify.
//			// Outage Type = S - store in system_outage
//			if (outage.getOutageType() == LrgsConstants.systemOutageType)
//			{
//				updateSystemOutage(outage);
//			}
//			else if (outage.getOutageType() 
//				== LrgsConstants.domsatGapOutageType)
//			{ // Outage Type = G - store in domsat_gap
//				updateDomsatGapOutage(outage);
//			}
//			else if (outage.getOutageType() == LrgsConstants.damsntOutageType)
//			{ // Outage Type = C - store in damsnt_outage
//				updateDamsntOutage(outage);
//			}
//		}
	}

	/**
	 * Update the Damsnt Outage table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to update the 
	 *  Outage object
	 */
	private void updateDamsntOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = "UPDATE damsnt_outage SET data_source_id = " +
//		outage.getSourceId() +
//		", begin_Time = " + sqlDate(outage.getBeginTime()) + 
//		", end_time = " + sqlDate(outage.getEndTime()) + 
//		", status_code = " + sqlChar(outage.getStatusCode())+
//		" WHERE outage_id = " + outage.getOutageId();
//		doModify(q);
//		commit();
	}

	/**
	 * Update the Domsat Gap table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to update the 
	 *  Outage object
	 */
	private void updateDomsatGapOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = "UPDATE domsat_gap SET " +
//		" begin_Time = " + sqlDate(outage.getBeginTime()) +
//		", begin_seq = " + outage.getBeginSeq() +
//		", end_time = " + sqlDate(outage.getEndTime()) +
//		", end_seq = " + outage.getEndSeq() +
//		", status_code = " + sqlChar(outage.getStatusCode())+
//		" WHERE outage_id = " + outage.getOutageId();
//		doModify(q);
//		commit();
	}
	
	/**
	 * Update the System Outage table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to update the 
	 *  Outage object
	 */
	private void updateSystemOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = "UPDATE system_outage SET begin_Time = " 
//			+ sqlDate(outage.getBeginTime()) + 
//			", end_time = " + sqlDate(outage.getEndTime()) + 
//			", status_code = " + sqlChar(outage.getStatusCode())+
//			" WHERE outage_id = " + outage.getOutageId();
//		doModify(q);
//		commit();
	}

	/**
	 * Insert the Damsnt Outage table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to insert the 
	 *  Outage object
	 */
	private void saveDamsntOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = 
//			"INSERT INTO damsnt_outage(outage_id, data_source_id, " +
//			"begin_time, end_time, status_code) VALUES (" 
//			+ outage.getOutageId()+ ", " +
//			outage.getSourceId() + ", " + sqlDate(outage.getBeginTime()) + 
//			", " + sqlDate(outage.getEndTime()) + 
//			", " + sqlChar(outage.getStatusCode()) + ")";
//		doModify(q);
//		commit();
//		outage.setInDb(true);
	}

	/**
	 * Insert the Domsat Gap table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to insert the 
	 *  Outage object
	 */
	private void saveDomsatGapOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = 
//			"INSERT INTO domsat_gap(outage_id, begin_time, " +
//			"begin_seq, end_time, end_seq, status_code) " +
//			"VALUES (" + outage.getOutageId() + ", " +
//			sqlDate(outage.getBeginTime()) +
//			", " + outage.getBeginSeq() +
//			", " + sqlDate(outage.getEndTime()) + 
//			", " + outage.getEndSeq() +
//			", " + sqlChar(outage.getStatusCode()) + ")";
//		doModify(q);
//		commit();
//		outage.setInDb(true);
	}

	/**
	 * Insert the System Outage table.
	 * 
	 * @param outage object to update
	 * @param outageID unique id
	 * @throws LrgsDatabaseException exception if it fails to insert the 
	 *  Outage object
	 */
	private void saveSystemOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		String q = 
//			"INSERT INTO system_outage(outage_id, begin_time, " +
//			"end_time, status_code) VALUES (" + outage.getOutageId() + ", " +
//			sqlDate(outage.getBeginTime()) + ", " + 
//			sqlDate(outage.getEndTime()) + 
//			", " + sqlChar(outage.getStatusCode()) + ")";
//		doModify(q);
//		commit();
//		outage.setInDb(true);
	}
	
	/**
	 * This method returns all the Outage records found on the 
	 * system_outage, domsat_gap and damsnt_outage tables, based
	 * on the given start and end time.
	 * If both start and end time are given returns all records in between 
	 * those two dates. If startTime is null, returns all records until 
	 * endTime. If endTime is null, returns all records after the start time. 
	 * If both times are null, returns all records.
	 * 
	 * @param startTime specifies the time to start reading Outage records
	 * @param endTime specifies the time to stop reading Outage records
	 * @return List of Outages objects, empty list if no records found
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  reading from DB
	 */
	public ArrayList<Outage> getOutages(Date startTime, Date endTime) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

		ArrayList<Outage> outageList = new ArrayList<Outage>();

//		// Select from system_outage table
//		outageList = selectSystemOutage(startTime, endTime, outageList);
//		// Select from domsat_gap table
//		outageList = selectDomsatGap(startTime, endTime, outageList);
//		// Select from damsnt_outage table
//		outageList = selectDamsntOutage(startTime, endTime, outageList);
//
//		Collections.sort(outageList);
		return outageList;
	}

	/**
	 * This method retrieves outage information from the system_outage table.
	 *   
	 * @param startTime specifies the time to start reading Outage records
	 * @param endTime specifies the time to stop reading Outage records
	 * @param outageList the outage list to be filled with the table info
	 * @return outageList the outage list containing the info
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  reading from DB
	 */
	private ArrayList<Outage> selectSystemOutage(Date startTime, Date endTime, 
		ArrayList<Outage> outageList) throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		Outage outage = null;
//		StringBuffer q = new StringBuffer();
//		q.append( 
//			"SELECT outage_id, begin_time, end_time, status_code" +
//			" FROM system_outage");
//		if (startTime != null && endTime != null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime) +
//					" AND end_time <= " + sqlDate(endTime));
//		}
//		else if (startTime == null && endTime != null)
//		{
//			q.append(" WHERE end_time <= " + sqlDate(endTime));
//		} 
//		else if (startTime != null && endTime == null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime));
//		}
//		try
//		{
//			ResultSet rs = doQuery(q.toString());
//			// Set Outage Array List.
//			while(rs.next())
//			{   // Set outageId, beginTime, endTime, statusCode and
//				// outageType
//				// Set sourceId, dcpAddress, beginSeq and endSeq to 0
//				outage = new Outage(rs.getInt(1), getFullDate(rs, 2), 
//						getFullDate(rs, 3), LrgsConstants.systemOutageType,
//						getChar(rs, 4), 0, 0, 0, 0);
//				outage.setInDb(true);
//				outageList.add(outage);
//			}
//		}
//		catch(SQLException ex)
//		{
//			String msg = "Error while reading the system_outage table " + ex;
//			logger.failure(msg);
//			closeConnection();
//			throw new LrgsDatabaseException(msg);
//		}
		return outageList;
	}
	
	/**
	 * This method retrieves outage information from the Domsat Gap table.
	 *   
	 * @param startTime specifies the time to start reading Outage records
	 * @param endTime specifies the time to stop reading Outage records
	 * @param outageList the outage list to be filled with the table info
	 * @return outageList the outage list containing the info
	 * @throws LrgsDatabaseException exception thrown in case of error while reading from DB
	 */
	private ArrayList<Outage>  selectDomsatGap(Date startTime, Date endTime, 
		ArrayList<Outage> outageList) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		Outage outage = null;
//		StringBuffer q = new StringBuffer();
//		q.append( 
//			"SELECT outage_id, begin_time, begin_seq, end_time, end_seq, status_code" +
//			" FROM domsat_gap");
//		if (startTime != null && endTime != null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime) +
//					" AND end_time <= " + sqlDate(endTime));
//		}
//		else if (startTime == null && endTime != null)
//		{
//			q.append(" WHERE end_time <= " + sqlDate(endTime));
//		} 
//		else if (startTime != null && endTime == null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime));
//		}
//		try
//		{
//			ResultSet rs = doQuery(q.toString());
//			// Set Outage Array List.
//			while(rs.next())
//			{   // Set outageId, beginTime, beginSeq, endTime, endSeq , 
//				// statusCode and outageType
//				// Set sourceId and dcpAddress to 0
//				outage = new Outage(rs.getInt(1), getFullDate(rs, 2), 
//						getFullDate(rs, 4), LrgsConstants.domsatGapOutageType,
//						getChar(rs, 6), 0, 0, rs.getInt(3), rs.getInt(5));
//				outage.setInDb(true);
//				outageList.add(outage);
//			}
//		}
//		catch(SQLException ex)
//		{
//			String msg = "Error while reading the domsat_gap table " + ex;
//			logger.failure(msg);
//			closeConnection();
//			throw new LrgsDatabaseException(msg);
//		}
		return outageList;
	}
	
	/**
	 * This method retrieves outage information from the damsnt_outage table.
	 *   
	 * @param startTime specifies the time to start reading Outage records
	 * @param endTime specifies the time to stop reading Outage records
	 * @param outageList the outage list to be filled with the table info
	 * @return outageList the outage list containing the info
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  reading from DB
	 */
	private ArrayList<Outage> selectDamsntOutage(Date startTime, Date endTime, 
		ArrayList<Outage> outageList) 
		throws LrgsDatabaseException
	{
		//MJM OpenDCS 6.2 does not support Outage recovery

//		Outage outage = null;
//		StringBuffer q = new StringBuffer();
//		q.append(
//			"SELECT outage_id, data_source_id, begin_time, end_time, status_code" +
//			" FROM damsnt_outage");
//		if (startTime != null && endTime != null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime) +
//					" AND end_time <= " + sqlDate(endTime));
//		}
//		else if (startTime == null && endTime != null)
//		{
//			q.append(" WHERE end_time <= " + sqlDate(endTime));
//		} 
//		else if (startTime != null && endTime == null)
//		{
//			q.append(" WHERE begin_time >= " + sqlDate(startTime));
//		}
//		try
//		{
//			ResultSet rs = doQuery(q.toString());
//			// Set Outage Array List.
//			while(rs.next())
//			{   // Set outageId, dataSourceId, beginTime, endTime 
//				// statusCode and outageType
//				// Set dcpAddress, beginSeq and endSeq to 0
//				outage = new Outage(rs.getInt(1), getFullDate(rs, 3), 
//						getFullDate(rs, 4), LrgsConstants.damsntOutageType,
//						getChar(rs, 5), rs.getInt(2), 0, 0, 0);
//				outage.setInDb(true);
//				outageList.add(outage);
//			}
//		}
//		catch(SQLException ex)
//		{
//			String msg = "Error while reading the damsnt_outage table " + ex;
//			logger.failure(msg);
//			closeConnection();
//			throw new LrgsDatabaseException(msg);
//		}
		return outageList;
	}

	/**
	 * This method deletes a Data Source record from the lrgs data_source table.
	 *   
	 * @param DataSource the Data Source object to be deleted
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  deleting from DB 
	 */
	public void deleteDataSource(DataSource dataSource) 
		throws LrgsDatabaseException
	{
		// Verify that we have a valid Data Source object.
		if (dataSource != null)
		{
			String q = 
				"DELETE FROM data_source WHERE data_source_id = " + 
				dataSource.getDataSourceId();
			try
			{
				doModify(q);
				commit();
				dataSource.setDataSourceId(LrgsConstants.undefinedId);
			} catch (LrgsDatabaseException e)
			{
				String msg = "Error while deleting from the data_source table " 
					+ e;
				throw new LrgsDatabaseException(msg);
			}
		}
	}
	
	/**
	 * This method deletes Dds stats records from the lrgs dds_connection and/or
	 * dds_period_stats tables. It deletes records based on the given date.
	 * For the dds_connection table it deletes all the records before the specified 
	 * date using the end_time field. For the dds_period_stats records table it deletes
	 * all the records before the given date using the start_time field.   
	 *   
	 * @param beforeDate specifies the date used to removed records 
	 * @throws LrgsDatabaseException exception thrown in case of error while deleting from DB 
	 */
	public void deleteDdsStatsBefore(Date beforeDate) throws LrgsDatabaseException
	{
		// Verify that we have a valid Date object.
		if (beforeDate != null)
		{
			deleteFromDdsConnection(beforeDate);
			deleteFromDdsPeriodStats(beforeDate);
		}
	}

	/**
	 * Delete records from the dds_period_stats table.
	 * 
	 * @param beforeDate specifies to delete records before this date
	 * @throws LrgsDatabaseException exception thrown in case of error while deleting from DB 
	 */
	private void deleteFromDdsPeriodStats(Date beforeDate) throws LrgsDatabaseException
	{
		String q = 
			"DELETE FROM dds_period_stats WHERE start_time <= " + 
			sqlDate(beforeDate) + " and lrgs_host = " + sqlString(myHostName);
		try
		{
			doModify(q);
			commit();

		} catch (LrgsDatabaseException e)
		{
			String msg = "Error while deleting from the dds_period_stats table " + e;
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	 * Delete records from the dds_connection table.
	 * 
	 * @param beforeDate specifies to delete records before this date
	 * @throws LrgsDatabaseException exception thrown in case of error while deleting from DB
	 */
	private void deleteFromDdsConnection(Date beforeDate) throws LrgsDatabaseException
	{
		String q = 
			"DELETE FROM dds_connection WHERE end_time <= " + 
			sqlDate(beforeDate);
		try
		{
			doModify(q);
			commit();

		} catch (LrgsDatabaseException e)
		{
			String msg = "Error while deleting from the dds_connection table " + e;
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	 * This method deletes an Outage record from the lrgs Database. It will
	 * remove from one of the following tables depending on the Outage type:
	 * system_outage, domsat_gap or damsnt.
	 *   
	 * @param Outage the Outage object to be deleted
	 * @throws LrgsDatabaseException exception thrown in case of error while deleting from DB 
	 */
	public void deleteOutage(Outage outage) throws LrgsDatabaseException
	{
		// Verify that we have a valid Outage object.
		if (outage != null)
		{
			if (outage.getOutageType() == LrgsConstants.systemOutageType)
			{ // If Outage Type = S - delete outage from the system_outage table.
				deleteFromSystemOutage(outage);
			}
			else if (outage.getOutageType() == LrgsConstants.domsatGapOutageType)
			{ // If Outage Type = G - delete outage from the domsat_gap table
				deleteFromDomsatGap(outage);
			}
			else if (outage.getOutageType() == LrgsConstants.damsntOutageType)
			{ // If Outage Type = C - delete outage from the damsnt_outage tale
				deleteFromDamsntOutage(outage);
			}
		}
	}

	/**
	 * Delete records from the Damsnt Outage table.
	 * 
	 * @param outage the object to delete
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  deleting from DB
	 */
	private void deleteFromDamsntOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		String q = 
			"DELETE FROM damsnt_outage WHERE outage_id = " + 
			outage.getOutageId();
		try
		{
			doModify(q);
			commit();
		} catch (LrgsDatabaseException e)
		{
			String msg = "Error while deleting from the damsnt_outage table " + e;
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	 * Delete records from the Domsat Gap table.
	 * 
	 * @param outage the object to delete
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  deleting from DB
	 */
	private void deleteFromDomsatGap(Outage outage) throws LrgsDatabaseException
	{
		String q = 
			"DELETE FROM domsat_gap WHERE outage_id = " + 
			outage.getOutageId();
		try
		{
			doModify(q);
			commit();
		} catch (LrgsDatabaseException e)
		{
			String msg = "Error while deleting from the domsat_gap table " + e;
			throw new LrgsDatabaseException(msg);
		}
	}

	/**
	 * Delete records from the System Outage table.
	 * 
	 * @param outage the object to delete
	 * @throws LrgsDatabaseException exception thrown in case of error while 
	 *  deleting from DB
	 */
	private void deleteFromSystemOutage(Outage outage) 
		throws LrgsDatabaseException
	{
		String q = 
			"DELETE FROM system_outage WHERE outage_id = " + 
			outage.getOutageId();
		try
		{
			doModify(q);
			commit();
		} catch (LrgsDatabaseException e)
		{
			String msg = "Error while deleting from the system_outage table " + e;
			throw new LrgsDatabaseException(msg);
		}
	}
	
	/**
	 * Unconditionally close the connection.
	 */
	public void closeConnection()
	{
		if (conn != null)
		{
			Logger.instance().info("LrgsDatabase.closeConnection()");
			try { conn.close(); }
			catch(Exception ex) {}
			conn = null;
		}
	}

	/**
	 * Verify if there is a current connection to the database.
	 * 
	 * @return true if the database is currently connected. False otherwise.
	 */
	public boolean isConnected()
	{
		try { return conn != null && !conn.isClosed(); }
		catch(Exception ex)
		{
			Logger.instance().warning(
				"Error testing whether connection closed: " + ex);
			closeConnection();
			return false;
		}
	}

	/**
	 * This method returns the Lrgs Database version.
	 * 
	 * @return db_ver database version number
	 */
	public int getDatabaseVersion()
	{
		return db_ver;
	}

	/**
	 * This method returns the Lrgs Database created time.
	 *  
	 * @return db_createTime database creation time
	 */
	public Date getDatabaseCreateTime()
	{
		return db_createTime;
	}
	
	/**
	 * This method returns the Lrgs Database created-by value.
	 * 
	 * @return db_createby database createdby user value
	 */
	public String getDatabaseCreateBy()
	{
		return db_createBy;
	}
	
	/**
	 * This method returns the Lrgs Database description value.
	 * 
	 * @return db_description database description value
	 */
	public String getDatabaseDescription()
	{
		return db_description;
	}

	/** @return the URL we're currently connected to. */
	public String getDbUrl() { return dbUrl; }

	/**
	 * This method sets the dds_connection end_time field to the 
	 * given date for all records that have a null value on end_time. 
	 * 
	 * @param inDate date to be used to set the end_time
	 * @throws LrgsDatabaseException exception thrown in case of error 
	 * while updating the DB
	 */
	public void terminateConnection(Date inDate) 
		throws LrgsDatabaseException
	{
		if (inDate != null)
		{
			String q = "UPDATE dds_connection SET end_time = " +
						sqlDate(inDate) + " WHERE end_time IS NULL";
			doModify(q);
			commit();
		}
	}

	public String getMyHostName()
	{
		return myHostName;
	}
	
	/**
	 * This method implemented for the NOAA password checker.
	 * Hard-coded limits: new PW cannot match a password within last 2 years or 8 previous passwords.
	 * This method also deletes any old passwords beyond those limits.
	 * If no match found, the new password is saved in the database with the current time.
	 * @param username the user name
	 * @param hexPwHash the new password has as a hex string
	 * 
	 * @throws BadPasswordException if password is in the history
	 */
	public void checkHistoricalPassword(String username, String hexPwHash)
		throws BadPasswordException
	{
		String q = "select set_time, pw_hash from pw_history where username = " + sqlString(username)
			+ " order by set_time";
		Logger.instance().debug3("checkHistoricalPassword '" + q + "'");
		Statement stat = null;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -2);
		Date cutoff = cal.getTime();
		try
		{
			stat = createStatement();
			ResultSet rs = stat.executeQuery(q);
			ArrayList<OldPasswordHash> ophs = new ArrayList<OldPasswordHash>();
			while(rs != null && rs.next())
				ophs.add(new OldPasswordHash(this.getFullDate(rs,  1), rs.getString(2)));

//System.out.println("Read " + ophs.size() + " old passwords for user '" + username + "'");
//for(OldPasswordHash oph : ophs)
//System.out.println("" + oph.getSetTime() + " " + oph.getPwHash());
			
			// Trim old passwords 
			while(ophs.size() > 8 && ophs.get(0).getSetTime().before(cutoff))
			{
				q = "delete from pw_history where username = " + sqlString(username)
					+ " and set_time = " + sqlDate(ophs.get(0).getSetTime());
				stat.executeUpdate(q);
//System.out.println("Removing " + ophs.get(0).getSetTime());
				ophs.remove(0);
			}
			
//System.out.println("Checking for match to: " + hexPwHash);
			// Now check for a match
			for(OldPasswordHash oph : ophs)
				if (oph.getPwHash().equals(hexPwHash))
					throw new BadPasswordException("This password matches the password set on "
						+ oph.getSetTime());
			
			// No match. Insert the new entry.
			q = "insert into pw_history(username, set_time, pw_hash) values("
				+ sqlString(username) + ", " + sqlDate(new Date()) + ", " + sqlString(hexPwHash) + ")";
			stat.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new BadPasswordException(msg);
		}
		finally
		{
			if (stat != null)
				try {stat.close(); } catch(Exception ex) {}
		}
	}
	
	/**
	 * @param username
	 * @return number of consecutive bad passwords after all other sessions.
	 */
	public int getNumConsecutiveBadPasswords(String username)
	{
		Statement stat = null;
		String q = "select count(*) from dds_connection where username = " + sqlString(username)
			+ " and success_code = 'P' and start_time > (select max(start_time) from dds_connection "
			+ "where username = " + sqlString(username) + " and success_code != 'P')";
		try
		{
			stat = createStatement();
			ResultSet rs = stat.executeQuery(q);
			if (rs != null && rs.next())
				return rs.getInt(1);
			else
				return 0;
		}
		catch(Exception ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			return 0;
		}
		finally
		{
			if (stat != null)
				try {stat.close(); } catch(Exception ex) {}
		}

	}
	
	
}
