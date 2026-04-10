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
package decodes.sql;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.util.sql.WrappedConnection;
import decodes.cwms.CwmsConnectionPool;
import decodes.db.Constants;
import decodes.db.IdDatabaseObject;
import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

/**
 * This class encapsulates some of the data and methods that are common
 * to all the I/O classes that are designed to read and write specific
 * tables from and to the SQL database.
 */
public class SqlDbObjIo
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	* Stores a reference to the SqlDatabaseIO object, which is the
	* parent of this object.
	*/
	SqlDatabaseIO _dbio;

	/** Determines whether backslash is escaped in SQL strings written to DB. */
	public static boolean escapeBackslash = true;

	protected Connection connection = null;

	/**
	* Construct with a reference to this object's parent.
	* @param dbio the parent dbio object.
	*/
	public SqlDbObjIo(SqlDatabaseIO dbio)
	{
		_dbio = dbio;
	}

	/** @return the DECODES database version number. */
	public int getDatabaseVersion()
	{
		return _dbio.getDecodesDatabaseVersion();
	}

	/** @return the DECODES database options. */
	public String getDatabaseOptions()
	{
		return _dbio.getDatabaseOptions();
	}

	/**
	* Returns an SQL representation of a String value that's required
	* in the SQL database, but when the argument might be null.
	* This simply returns the string enclosed in single quotes.
	* Note that if the argument is null, this returns an empty set of
	* single quotes.
	* @return String suitable for use in a SQL statement
	*/
	public String sqlString(String arg)
	{
		if (arg == null) return "''";
		return sqlReqString(arg);
	}

	/**
	* Boolean values are stored in the database as strings because Oracle
	* doesn't support a boolean data type :-(, This method converts a boolean
	* to a SQL string with a value of TRUE or FALSE.
	* @return String suitable for use in a SQL statement
	*/
	public String sqlString(boolean arg)
	{
		return arg ? "'TRUE'" : "'FALSE'";
	}

	/**
	* Return an SQL representation of an optional String value.
	* If the argument is null, then this returns the string value "NULL"
	* (without the quotes).  If the argument is not null, this returns
	* "'<value>'"; i.e. the value of the string in single-quotes.
	* @param arg the raw string value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlOptString(String arg)
	{
		if (arg == null) return "NULL";
		return sqlReqString(arg);
	}

	public String sqlOptString(String arg, int maxlen)
	{
		if (arg != null && arg.length() > maxlen)
			arg = arg.substring(0, maxlen);
		return sqlOptString(arg);
	}

	/**
	* Returns an SQL representation of a string that's required in the
	* database, and the argument is guaranteed not to be null.
	* @param arg the raw string value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlReqString(String arg)
	{
		String a = "";
		int from = 0;
		int to;
		while ( (to = arg.indexOf('\'', from)) != -1 )
		{
			a += arg.substring(from, to) + "''";
			from = to + 1;
		}
		a += arg.substring(from);

		return "'" + a + "'";
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
	public String sqlOptDate(Date d)
	{
		if (d == null) return "NULL";
		return sqlDate(d);
	}

	/**
	* Returns an SQL representation of a Date.
	* The argument must not be null.
	* @param d the date value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlDate(Date d)
	{
		return _dbio.sqlDate(d);
	}

	/**
	* Returns an SQL representation of an optional object that is refered
	* to in the SQL database by an ID number.  If the argument is null,
	* this returns "NULL" (without the quotes).  If not, this returns the
	* string representation of the object's ID number.
	* @param obj the database object
	*/
	public String sqlOptHasId(IdDatabaseObject obj)
	{
		if (obj == null || !obj.idIsSet())
			return "NULL";
		else
			return obj.getId().toString();
	}

	/**
	  Returns an SQL string representation of a boolean.
	  @deprecated  for compatibility with Oracle, which doesn't support the
	  SQL boolean data type.
	*/
	@Deprecated
	public String sqlBool(boolean b)
	{
		return b ? "true" : "false";
	}

	/**
	* This returns the SQL representation of an optional integer value.
	* We assume that "-1" is a special value that the argument takes to
	* indicate that there is no value.  If the argument is -1, this returns
	* "NULL" (without the quotes).  Otherwise, this returns the string
	* representation of the number.
	* @param val integer value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlOptInt(int val)
	{
		if (val == -1) return "NULL";
		return Integer.toString(val);
	}

	/**
	* This returns the SQL representation of an optional double.
	* The double is considered "not to exist" if it has the value
	* Constants.undefinedDouble.  In that case, this will return the
	* string "NULL".  Otherwise, this will return the string representation
	* of the double.
	* @param d double value
	* @return String suitable for use in a SQL statement
	*/
	public String sqlOptDouble(double d)
	{
		if (d == Constants.undefinedDouble) return "NULL";
		return Double.toString(d);
	}


	//=====================================================================
	// METHODS THAT EXECUTE SQL QUERIES
	//=====================================================================

	/**
	* Create a new SQL Statement object.
	* A connection must have been already established.
	* @return JDBC Statement object
	*/
	public Statement createStatement()
		throws SQLException
	{
		return connection().createStatement();
	}

	/**
	* Executes an UPDATE or INSERT query.
	* @param q the query string
	* @throws DatabaseException  if the update fails.
	*/
	public void executeUpdate(String q)
		throws DatabaseException, SQLException
	{
		try (Statement stmt = createStatement();)
		{
			log.trace("Executing update query '{}'", q);
			int numChanged = stmt.executeUpdate(q);
			if (numChanged == 0)
				throw new DatabaseException("Failed to update the " +
					"SQL database.  The query was \"" + q + "\"");
		}
	}

	/**
	* This tries to do a query.  If the number of rows affected is one or
	* greater, this returns true.  If the number of rows is zero, this
	* returns false.
	* @param q the query string
	* @return true if query was successfully executed.
	*/
	public boolean tryUpdate(String q)
		throws SQLException
	{
		Statement stmt = createStatement();

		log.trace("Trying update query '{}'", q);

		int numChanged = stmt.executeUpdate(q);
		stmt.close();

		return numChanged > 0;
	}

	/** @return the database connection. */
	public Connection connection()
	{
		if (connection == null)
		{
			log.warn("using connection without initializing the DbIo object first!");
			try
			{
				connection = _dbio.getConnection();
			}
			catch (SQLException ex)
			{
				throw new RuntimeException("Unable to get connection.", ex);
			}
		}
		return new WrappedConnection(connection, c -> {});
	}

	public SqlDatabaseIO getDbio()
	{
		return _dbio;
	}

	/**
	  Returns a timestamp value from the specified column of a result set.
	  Timestamps have been problematic between different databases and
	  different versions of the JDK. This method encapsulates reading a
	  timestamp to prevent SQLExceptions from being propegated and crashing
	  a program.
	  If an exception occurs or value is null, the defaultDate is returned.
	  <p>
	  Specific error seen: Create TIMESTAMP with TIMEZONE in PGSQL using
	  JDK1.3, Attempt to retrieve it using JDK1.4. - SQLException.

	  @param rs the JDBC result set
	  @param column column number in result set containing the date
	  @param defaultDate returned if error getting date from result set

	  @return Date parsed from result set
	*/
	public Date getTimeStamp(ResultSet rs, int column, Date defaultDate)
	{
		Date ret = _dbio.getFullDate(rs, column);
		return ret == null ? defaultDate : ret;
	}

	/**
	  @return the next ID value.
	*/
	protected DbKey getKey(String tableName)
		throws DatabaseException
	{
		return _dbio.getKey(tableName, connection());
	}

	/**
	  Adds an extra backslash before any backslash in the string.
	  @param s input string
	  @return escaped string
	*/
	public String escapeString(String s)
	{
		if ( s == null ) return("null");

		//If the SQL database is Oracle, set escapeBackslash = false
		if (_dbio.isOracle())
			escapeBackslash = false;

		s = sqlReqString(s);
		if (!escapeBackslash)
			return s;

		StringBuilder ret = new StringBuilder();
		boolean escaped = false;
		for(int i=0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			if (c == '\\')
			{
				escaped = true;
				ret.append(c);
			}
			ret.append(c);
		}
		return (escaped ? "E" : "") + ret.toString();
	}

	public Connection getConnection()
	{
		// needed as we shift in proper usage of the Connection objects.
		return new WrappedConnection(connection, (c) -> {});
	}

	public void setConnection(Connection conn)
	{
		log.debug("setConnection({})", (conn!=null?conn.hashCode():"null"));
		this.connection = conn;
	}
}
