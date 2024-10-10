// The DBAccess class will be contained in the some package
package decodes.hdb.dbutils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Public class DBAccess is used to provide ORACLE database connectivity close
 * the connection to the database and to provide query methods to query the
 * currect database. The system property db.property must be set inorder for the
 * database connection to function properly.
 * 
 * @author Mark A. Bogner
 * @version 1.0 Date: 09-April-2001
 */
public class DBAccess
{

	// conn instance variable is used to store the current connection object to
	// the database
	private Connection conn = null;
	private Logger log = Logger.getInstance();

	public DBAccess(Connection _conn)
	{
		conn = _conn;
	}

	/**
	 * Public method getConnection establishes the database connection if the
	 * connection instance variable conn is not already set. The database
	 * connectivity requires that the system property "db.property" must be set
	 * before calling this method. The db.property property must be a pointer to
	 * an actual property file that contains the following database properties:
	 * 
	 * 1. Database Driver: (eg. DRIVER=oracle.jdbc.driver.OracleDriver) 2.
	 * Database URL: (eg. URL=jdbc:oracle:thin:@stork.central:1521:CFUATAM) 3.
	 * Schema Identifier: (eg. USERNAME=cust_docs) 4. Schema Password: (eg.
	 * PASSWORD=cust_docs)
	 * 
	 * @author Mark A. Bogner
	 * @version 1.0
	 * @return Returns a Connection to the database. Date: 09-April-2001
	 */
	public Connection getConnection(DataObject do1)
	{
		// if the conn instance variable is already set then we have
		// already established a database connection so return its value.
		if (conn != null)
			return conn;
		try
		{
			String driver = null;
			String url = null;
			String uid = null;
			String password = null;

			// open the system property db.property file if the db.property
			// system property has been set and get the database
			// properties from there to make the database connection.
			if (System.getProperty("db.property") != null)
			{
				Properties prop = new Properties();
				prop.load(new BufferedInputStream(new FileInputStream(System.getProperty("db.property"))));
				driver = prop.getProperty("DRIVER");
				url = prop.getProperty("URL");
				uid = prop.getProperty("USERNAME");
				password = prop.getProperty("PASSWORD");
			}
			else
			// get the database connection properties from the supplied DO
			{
				driver = (String) do1.get("DRIVER");
				url = (String) do1.get("URL");
				uid = (String) do1.get("USERNAME");
				password = (String) do1.get("PASSWORD");
			}

			log.debug("Attempting Connection: " + url);
			// load the driver class
			Class.forName(driver).newInstance();
			// now make the connection

			conn = DriverManager.getConnection(url, uid, password);
			if (((String) do1.get("DEBUG")).equals("Y"))
				log.debug(url + " connection successful");

		}
		catch (SQLException e)
		{
			log.debug(" ERROR making connection: ");
			// log.debug(e.printStackTrace());
		}
		finally
		// in all case return the connection
		{
			return conn;
		}
	}

	/**
	 * Public method perform DML takes the supplied input variable String and
	 * executes this DML against the database. Tthis method will allow preformed
	 * DML statemants That do inserts, updates deletes and DDL statements can be
	 * used here
	 * 
	 * @author Mark A. Bogner
	 * @version 1.0
	 * @param query
	 *            the Actual sql formatted query that is to be performed. Date:
	 *            15-JANUARY-2003
	 */
	public String performDML(String query, DataObject do1)
	{
		Statement stmt = null;
		try
		{
			stmt = getConnection(do1).createStatement();
			int rows = stmt.executeUpdate(query);
			return "SUCCESS: " + rows;
		}
		catch (SQLException e)
		{
			return "ERROR: " + e.getMessage();
		}
		finally
		{
			try
			{
				stmt.close();
			}
			catch (SQLException e)
			{
			}

		}

	} // end of performDML method

	/**
	 * Public method perform query takes the supplied input variable String
	 * query and executes this query against the database. The results of this
	 * query are the placed into the dataobject do1 as a key value pair. If the
	 * results of the query are a single row then the data is placed into the
	 * dataobject as a key value pair of type Strings. If the query result is of
	 * multiple rows then the values are placed into the dataobject as a String,
	 * ArrayList pair.
	 * 
	 * @author Mark A. Bogner
	 * @version 1.0
	 * @param query
	 *            the Actual sql formatted query that is to be performed.
	 * @param do1
	 *            The dataobject the query results will be placed into. Date:
	 *            09-April-2001
	 */
	public String performQuery(String query, DataObject do1)
	{

		Statement stmt = null;
		ResultSet rset = null;
		try
		{
			/*
			 * performQuery --------------------------- Execute query Get Meta
			 * data for column names, column numbers For each row: create an
			 * arraylist of string values for all columns in the row add that
			 * row to the records arraylist increment row count If only one row
			 * add each column and its (String) value to the D.O.
			 * 
			 * ---------------------------
			 */
			stmt = getConnection(do1).createStatement();
			rset = stmt.executeQuery(query);

			ResultSetMetaData rsmd = rset.getMetaData();
			int cols = rsmd.getColumnCount();
			ArrayList<String> columns = new ArrayList<String>();

			for (int i = 0; i < cols; i++)
			{
				String temp = rsmd.getColumnName(i + 1);
				if (temp == null)
					temp = "";
//ilex.util.Logger.instance().debug3(
//	"Column " + (i + 1) + " name=" + temp + ", classname=" + rsmd.getColumnClassName(i + 1));
				columns.add(i, temp);
			}

			ArrayList<ArrayList<String>> records = new ArrayList<ArrayList<String>>();
			int rowCount = 0;

			// for every row that is returned from the query
			while (rset.next())
			{
				// create an arraylist for each row , get all the column values
				// and add each value to the row
				ArrayList<String> row = new ArrayList<String>();
				for (int i = 0; i < cols; i++)
				{
					String temp = rset.getString(i + 1);
//ilex.util.Logger.instance().debug3(
//	"resultset.getString(" + (i + 1) + ") returned '" + temp + "'");
//try { double d = rset.getDouble(i+1); ilex.util.Logger.instance().debug3("....as double=" + d); }
//catch(Exception ex) { ilex.util.Logger.instance().debug3("....getDouble() threw exception: " + ex); }
//try { double d = ((java.math.BigDecimal)rset.getObject(i+1)).doubleValue(); 
//ilex.util.Logger.instance().debug3("....from big decimal, double=" + d); }
//catch(Exception ex) { ilex.util.Logger.instance().debug3("....Big Decimal.doubleValue threw exception: " + ex); }

					if (temp == null)
						temp = "";
					// if (temp == null) temp = "<<NULL>>";
					row.add(i, temp);
				}
				// now add that row to the arrayList of records and increment
				// the
				// record count after the row has been inserted
				records.add(rowCount, row);
				rowCount++;
			}
			// we are done with the query and the result set so close both
			rset.close();
			stmt.close();

//ilex.util.Logger.instance().debug3(
//	"After query '" + query + "', row count = " + rowCount + ", cols=" + cols);
			// if no rows came back from the query then store each column
			// in the dataObject as a String, empty String pair
			if (rowCount == 0)
			{
				String temp = "";
				for (int i = 0; i < cols; i++)
				{
					// get the column key from metadata object and the empty
					// String temp
					// and add these entries to the dataObject hastable
					do1.put((String) columns.get(i), temp);
				}
			} // end of the no rows returned block

			// if only one row came back from the query then store each column
			// in the dataObject as a String, String pair
			if (rowCount == 1)
			{
				for (int i = 0; i < cols; i++)
				{

					// get the column key from metadata object and the value
					// from
					// the arraylist for that columns entry and add these
					// entries
					// to the dataObject hastable
					String v = records.get(0).get(i);
//ilex.util.Logger.instance().debug3(
//	"Single row result, col[" + i + "] (" + columns.get(i) + ")='" + v + "'");
					do1.put(columns.get(i), v);
				}
			}
			else if (rowCount > 1) // there were multiple rows returned
			{
				// for every column create an arraylist of objects from all the
				// records that were returned
				for (int i = 0; i < cols; i++)
				{
					ArrayList columnData = new ArrayList();
					for (int j = 0; j < rowCount; j++) // go to every row, get
														// column value
					{
						// add column data to the column arrayList
						columnData.add(((ArrayList) records.get(j)).get(i));
					}
					// done with this column, add this arrayList to the
					// dataObject
					do1.put((String) columns.get(i), columnData);
				}
			} // end of else rowcount must be greater than one

			return "SUCCESS:";
		}
		catch (SQLException e)
		{
			return "ERROR executing query: " + e.getMessage();
			// e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rset != null)
				{
					rset.close();
				}
				if (stmt != null)
				{
					stmt.close();
				}
			}
			catch (SQLException e)
			{
			}

		} // end of finally

	} // end of performQuery method

	/**
	 * Public method get_arraylist_query takes the supplied input variable
	 * String query and executes this query against the database. The results of
	 * this query are the placed into the dataobject do1 as a key value pair for
	 * each row. An Arraylist is then generated and returned for all rows
	 * gathered.
	 * 
	 * @author Mark A. Bogner
	 * @version 1.0
	 * @param query
	 *            the Actual sql formatted query that is to be performed.
	 * @param db_object
	 *            the object that will contain the info necessary to query the
	 *            database
	 * @param do_list
	 *            The arraylist the query results rows/dataobject will be placed
	 *            into. Date: 06-May-2004
	 */
	public ArrayList get_arraylist_query(String query, DataObject do1)
	{

		Statement stmt = null;
		ResultSet rset = null;
		ArrayList records = new ArrayList();

		try
		{
			/*
			 * performQuery --------------------------- Execute query Get Meta
			 * data for column names, column numbers For each row: create an
			 * DataObject of string values for all columns in the row add that
			 * row to the records input parameter arraylist increment row count
			 * 
			 * ---------------------------
			 */
			stmt = getConnection(do1).createStatement();
			rset = stmt.executeQuery(query);

			ResultSetMetaData rsmd = rset.getMetaData();
			int cols = rsmd.getColumnCount();
			ArrayList columns = new ArrayList();

			for (int i = 0; i < cols; i++)
			{
				String temp = rsmd.getColumnName(i + 1);
				if (temp == null)
					temp = "";
				columns.add(i, temp);
			}

			int rowCount = 0;

			// for every row that is returned from the query
			while (rset.next())
			{
				// create an dataobject for each row , get all the column values
				// and add each value to the row
				DataObject row = new DataObject();
				for (int i = 0; i < cols; i++)
				{
					String temp = rset.getString(i + 1);
					if (temp == null)
						temp = "";
					// if (temp == null) temp = "<<NULL>>";
					row.put((String) columns.get(i), temp);
				}
				// now add that row to the arrayList of records and increment
				// the
				// record count after the row has been inserted
				records.add(rowCount, row);
				rowCount++;
			}
			// we are done with the query and the result set so close both
			rset.close();
			stmt.close();

		}
		catch (SQLException e)
		{
			System.out.println("ERROR executing: " + query + "  Message: " + e.getMessage());
		}
		finally
		{
			try
			{
				if (rset != null)
					rset.close();
				if (stmt != null)
					stmt.close();
			}
			catch (SQLException e)
			{
			}

		} // end of finally

		return records;
	} // end of get_arraylist_query method

	public void get_col(DataObject do1, String table_names, String table_columns, String where_clause)
	// method used to get any columns from any tables
	// with a possible supplied where clause
	{

		String query = null;
		String result = null;
		String error_message = null;

		// get the values
		query = "select " + table_columns + " from " + table_names + " where " + where_clause;
		// System.out.println(query);
		result = performQuery(query, do1);
		if (result.startsWith("ERROR"))
		{
			error_message = "GET Columns Method FAILED" + result;
			log.debug(this, "  " + query + "  :" + error_message);
		}

	} // end of get_col method

	public boolean tbl_insert(String table_name, String table_columns, String value_clause)
	// method used to insert rows of data into passed in table
	{

		String dml = null;
		String result = null;
		String error_message = null;
		DataObject _do = null;

		// get the values
		dml = "insert into " + table_name + " (" + table_columns + ") values ( " + value_clause + ")";
		// System.out.println(dml);
		result = performDML(dml, _do);
		if (result.startsWith("ERROR"))
		{
			error_message = "TBL_INSERT Method FAILED" + result;
			log.debug(this, "  " + dml + "  :" + error_message);
			return false;
		}

		return true;

	} // end of tbl_insert method

	/**
	 * Public method closeConnection closes the database connection if the
	 * instance variable conn is not null.
	 * 
	 * @author Mark A. Bogner
	 * @version 1.0 Date: 09-April-2001
	 */
	public void closeConnection()
	{
		// If there is not a connection then don't try to close it
		if (conn != null)
		{
			// else there is a connection so close it.
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				log.debug("ERROR closing connection");
				// log.debug(e.printStackTrace());
			}
		} // End of if the conn is not null block
	} // End of closeConnection method

	/**
	 * Public method callProc performs the database procedure call The procedure
	 * is called with the _field input paramter and the returned String is
	 * placed into the passed in DataObject.
	 * 
	 * @author Mark A. Bogner
	 * @param _procname
	 *            The procedure to be run with entire signature
	 * 
	 *            Date: 08-November-2011
	 */
	public void callProc(String _procname, DataObject do1)
	{
		try
		{
			// initialize the procedure call, set the parameters , etc..
			String proc = "{ call  " + _procname + " }";
			CallableStatement stmt = getConnection(do1).prepareCall(proc);

			// now execute the procedure call and go get the return string value
			stmt.execute();

			// we are done with the procedure
			stmt.close();

		}
		catch (SQLException e)
		{
			log.debug(e.toString());
			// e.printStackTrace();
		}
	} // end of callProc method

	/**
	 * Public method performCall performs the database procedure call that
	 * requests the data specific to the mergefield that is sent in. The
	 * procedure is called with the _field input paramter and the returned
	 * String is placed into the passed in DataObject.
	 * 
	 * @author Mark A. Bogner
	 * @param _field
	 *            The merge field that is being requested data for
	 * @param do1
	 *            the Dataobject that the mereg field and its associated data is
	 *            to be placed
	 * 
	 *            Date: 17-April-2001
	 */
	public void performCall(String _field, DataObject do1)
	{
		try
		{
			// initialize the procedure call, set the parameters , etc..
			String proc = "{ call merge_fields_pkg_functions.common_proc(?,?) }";
			CallableStatement stmt = getConnection(do1).prepareCall(proc);
			stmt.setString(1, _field.toUpperCase());
			stmt.registerOutParameter(2, java.sql.Types.VARCHAR);

			// now execute the procedure call and go get the return string value
			stmt.execute();
			String result = stmt.getString(2);

			// we are done with the query and the result set so close both
			stmt.close();

			// if the procedure returned nothing then set it to an empty string
			// and then insert the pair into the data object
			if (result == null)
				result = "";
			do1.put(_field, result);
			log.debug("MergeField: " + _field + " Result: " + result);

		}
		catch (SQLException e)
		{
			log.debug(e.toString());
			// e.printStackTrace();
		}
	} // end of performCall method

	/**
	 * Public method initialCall performs an initial call to the database
	 * procedure which is required to set up the initial input parameters. these
	 * parameters are used to initislize the database connection so that the
	 * procedure call knows what contract, database language etc.. the document
	 * is related to.
	 * 
	 * @author Mark A. Bogner
	 * @param do1
	 *            The dataobject that will contain the data items this procedure
	 *            needs
	 * 
	 *            Date: 17-April-2001
	 */
	public void initialCall(DataObject do1)
	{
		try
		{
			// initialize the procedure call, create a statement
			// String proc =
			// "{ call merge_fields_pkg.proc_input_parameters(?,?,?,?,?) }";
			String proc = "{ call merge_fields_pkg.proc_input_parameters(?,?,?,?,?,?) }";
			CallableStatement stmt = getConnection(do1).prepareCall(proc);
			// set all the called procedures input variables from the passed in
			// DataObject
			stmt.setString(1, (String) do1.get("contract__t"));
			stmt.setString(2, (String) do1.get("seq_no"));
			stmt.setString(3, (String) do1.get("change_code"));
			stmt.setString(4, (String) do1.get("language"));
			stmt.setString(5, (String) do1.get("database"));
			stmt.setString(6, (String) do1.get("contact_sort"));
			// execute the stored procedure call
			stmt.execute();

			// we are done with the procedure call so close the statement
			stmt.close();
			log.debug("Initiated proc call: " + do1.get("contract__t"));

		}
		catch (SQLException e)
		{
			log.debug(e.toString());
			// e.printStackTrace();
		}
	} // end of initialCall method

} // End of DBAccess class
