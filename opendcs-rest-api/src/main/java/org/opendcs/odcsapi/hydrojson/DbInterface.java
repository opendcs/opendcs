/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.hydrojson;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.opendcs.odcsapi.dao.ApiDaoBase;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.lrgsclient.StaleClientChecker;
import org.opendcs.odcsapi.sec.TokenManager;
import org.opendcs.odcsapi.start.StartException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * This class is constructed for each request and is used to access the TSDB.
 * @author mmaloney
 *
 */
public class DbInterface
	implements AutoCloseable
{
	static final String module = "DbInterface";
	static public String dbType = "opentsdb";
	static public boolean isOracle = false;
	static public boolean isCwms = false;
	static public boolean isHdb = false;
	static public boolean isOpenTsdb = false;
	private static DataSource dataSource = null;
	private static final String sequenceSuffix = "IdSeq";
	static public String siteNameTypePreference = "CWMS";
	static public Properties decodesProperties = new Properties();
	static public boolean secureMode = false;
	
	/** Provides reason for last error return. */
	private String reason = null;
	
	/** The Connection used by this instance of DbInterface. */
	private Connection connection = null;
	
	private static TokenManager tokenManager = null;
	private static StaleClientChecker staleClientChecker = null;

	
	public DbInterface() 
		throws DbException
	{
		try
		{
			if (dataSource == null)
			{
				Context initialCtx = new InitialContext();
				Context envCtx = (Context)initialCtx.lookup("java:comp/env");
				dataSource = (DataSource)envCtx.lookup("jdbc/opentsdb");
			}
			else
				Logger.getLogger(ApiConstants.loggerName).info("Using DataSource provided by Jetty main class.");
			connection = dataSource.getConnection();
			isOracle = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
		}
		catch(SQLException ex)
		{
			String msg = "Cannot connect to database for jdbc/opentsdb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbException(module, ex, msg);
		}
		catch (NamingException ex)
		{
			String msg = "Cannot lookup envCtx java:comp/env, and then jdbc/opentsdb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbException(module, ex, msg);
		}

		if (staleClientChecker == null)
		{
			staleClientChecker = new StaleClientChecker();
			staleClientChecker.start();
		}

		Logger.getLogger(ApiConstants.loggerName).info("isHdb=" + isHdb + ", isCwms=" + isCwms 
			+ ", isOpenTsdb=" + isOpenTsdb + ", isOracle=" + isOracle);
	}
	
	public boolean isUserValid(String username, String password)
			throws DbException, SQLException {
		if (isOracle)
			throw new DbException(module, null, "User validation not implemented for Oracle.");
		
		Connection poolCon = getConnection();
		Connection userCon = null;

		try (ApiDaoBase daoBase = new ApiDaoBase(this, "DbInterface"))
		{
			// The only way to verify that user/pw is valid is to attempt to establish a connection:
			DatabaseMetaData metaData = poolCon.getMetaData();
			String url = metaData.getURL();
			
			// This validates the username & password.
			userCon = DriverManager.getConnection(url, username, password);
			// Above with throw SQLException if user/pw is not valid.
			
			// Now verify that user has appropriate privilege. This only works on Postgress currently:
			String q = "select pm.roleid, pr.rolname from pg_auth_members pm, pg_roles pr "
				//+ "where pm.member = (select oid from pg_roles where rolname = '" + username + "') "
				+ "where pm.member = (select oid from pg_roles where rolname = '?') "
				+ "and pm.roleid = pr.oid";
			//ResultSet rs = daoBase.doQuery(q);
			Connection conn = null;
			ResultSet rs = daoBase.doQueryPs(conn, q, username);
			while(rs.next())
			{
				int roleid = rs.getInt(1);
				String role = rs.getString(2);
				Logger.getLogger(ApiConstants.loggerName).info("User '" + username + "' has role " + roleid + "=" + role);
				if (role.equalsIgnoreCase("OTSDB_ADMIN") || role.equalsIgnoreCase("OTSDB_MGR"))
					return true;
			}
			reason = "User " + username + " does not have OTSDB_ADMIN or OTSDB_MGR privilege "
					+ "- Not Authorized.";
			Logger.getLogger(ApiConstants.loggerName).warning("isUserValid(" + username + ") failed: " + reason);
			userCon.close();
			return false;
		}
		catch (Exception e)
		{
			Logger.getLogger(ApiConstants.loggerName).warning("isUserValid - Authentication failed: " + e);
			if (userCon != null)
			{
                try
				{
                    userCon.close();
                }
				catch (SQLException ex)
				{
                    throw new SQLException("There was an issue closing the connection.", ex);
                }
            }
			reason = e.getMessage();
			return false;
		}
	}

	public String getReason()
	{
		return reason;
	}

	public static void setDataSource(DataSource dataSource)
	{
		DbInterface.dataSource = dataSource;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
	
	public void close()
	{
		try
		{
			//TODO if I'm using a connection pool, return the connection to the pool here.
			connection.close();
		}
		catch(Exception ex)
		{
			Logger.getLogger(ApiConstants.loggerName).warning("close connection error: " + ex);
		}
	}

	public Long getKey(Sequences sequence)
			throws DbException, SQLException
	{
		String q = sequence.getNextVal(isOracle);

		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery(q))
		{
			if (rs == null || !rs.next())
			{
				String err = "Cannot read sequence value from '" + sequence.name + sequenceSuffix
						+ "': " + (rs == null ? "Null Return" : "Empty Return");
				throw new DbException(module, null, err);
			}

			long lv = rs.getLong(1);
			return (rs.wasNull() ? null : (Long) lv);
		}
		catch (SQLException ex)
		{
			String msg = "SQL Error executing '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public static void setDatabaseType(String dbType)
		throws StartException
	{
		DbInterface.dbType = dbType;
		isHdb = isOpenTsdb = isCwms = false;
		if (dbType.equalsIgnoreCase("xml"))
			throw new StartException("API cannot run over an XML database.");
		else if (dbType.equalsIgnoreCase("url") || dbType.equalsIgnoreCase("sql"))
		{
			isHdb = isOpenTsdb = isCwms = false;
		}
		else if (dbType.equalsIgnoreCase("cwms"))
		{
			isHdb = isOpenTsdb = false;
			isCwms = true;
		}
		else if (dbType.equalsIgnoreCase("opentsdb"))
		{
			isHdb = isCwms = false;
			isOpenTsdb = true;
		}
		else if (dbType.equalsIgnoreCase("hdb"))
		{
			isHdb = true;
			isCwms = isOpenTsdb = false;
		}
	}

	public Date getFullDate(ResultSet rs, int column)
		throws DbException
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
			throw new DbException(module, ex, "Cannot convert date!");
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
	
	public Long sqlDateV(Date d)
	{
		if (d == null)
			return null;
		return d.getTime();
	}

	public static TokenManager getTokenManager()
	{
		if (tokenManager == null)
			tokenManager = new TokenManager(secureMode);
		
		return tokenManager;
	}

	/*
	*   List of sequences in the database.  If the code needs to reference a sequence or access one, it is done here.
	 */
	public enum Sequences
	{
		SITE("SITE"),
		DATATYPE("DATATYPE"),
		CP_ALGORITHM("CP_ALGORITHM"),
		HDB_LOADING_APPLICATION("HDB_LOADING_APPLICATION"),
		CP_COMPUTATION("CP_COMPUTATION"),
		PLATFORMCONFIG("PLATFORMCONFIG"),
		DECODESSCRIPT("DECODESSCRIPT"),
		UNITCONVERTER("UNITCONVERTER"),
		DATASOURCE("DATASOURCE"),
		NETWORKLIST("NETWORKLIST"),
		PLATFORM("PLATFORM"),
		PRESENTATIONGROUP("PRESENTATIONGROUP"),
		DATAPRESENTATION("DATAPRESENTATION"),
		INTERVAL_CODE("INTERVAL_CODE"),
		ENUM("ENUM"),
		ROUTINGSPEC("ROUTINGSPEC"),
		SCHEDULE_ENTRY("SCHEDULE_ENTRY"),
		TSDB_GROUP("TSDB_GROUP");
		private final String name;

		Sequences(String name) {
			this.name = name;

		}

		String getNextVal(boolean isOracle)
		{
			String sequenceName = name + sequenceSuffix;
			return isOracle ? ("SELECT " + sequenceName + ".nextval from dual")
					: ("SELECT nextval('" + sequenceName + "')"); // postgresql syntax
		}
	}
}
