/*
 * $Id$
 */
package decodes.cwms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import opendcs.dai.IntervalDAI;
import opendcs.dai.SiteDAI;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.OpenTsdbIntervalDAO;

import lrgs.gui.DecodesInterface;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

import decodes.db.*;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.sql.SqlDbObjIo;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;

/**
 * This class extends decodes.sql.SqlDatabaseIO for reading/writing the
 * USACE (U.S. Army Corps of Engineers) CWMS (Corps Water Management System)
 * database, which is hosted on an Oracle DBMS.<p>
 */
public class CwmsSqlDatabaseIO
	extends SqlDatabaseIO
	implements DatabaseConnectionOwner
{
	public final static String module = "CwmsSqlDatabaseIO";
	/** The office ID associated with this connection. This implicitely
	 * filters the records that are visible.
	 */
	private String dbOfficeId = null;
	private String dbOfficePrivilege = null;
	private DbKey dbOfficeCode = Constants.undefinedId;

	
	private String sqlDbLocation = null;
	private int cwmsSchemaVersion = CwmsTimeSeriesDb.CWMS_V_2_1;
	
	/**
 	* Constructor.  The argument is the "location" of the
 	* database from the "decodes.properties" file.
	* This should be a string in the form:
	* 	jdbc:oracle:thin:@hostname:1521:dbname,
	* where hostname and dbname specify the Oracle CWMS database.
	* @param sqlDbLocation the location string from decodes.properties file
 	*/
	public CwmsSqlDatabaseIO(String sqlDbLocation)
		throws DatabaseException
	{
		// No-args base class ctor doesn't connect to DB.
		super();
		
        writeDateFmt = new SimpleDateFormat(
			"'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')");
		String tz = DecodesSettings.instance().sqlTimeZone;
		if (tz == null)
			tz = "GMT"; // default to GMT. CWMS 2.0 and later alway use GMT.
        writeDateFmt.setTimeZone(TimeZone.getTimeZone(tz));
		
		this.sqlDbLocation = sqlDbLocation;
		
		connectToDatabase(sqlDbLocation);

		// The key generator is always for Oracle.
		keyGenerator = new CwmsSequenceKeyGenerator(cwmsSchemaVersion);
		
		/* 
		 * Oracle does not require a COMMIT after each block of nested SELECTs.
		 * The following causes the parent class to do this.
		 */
		commitAfterSelect = false;

		// Likewise we need a special platform IO to do office ID filtering.
		_platformListIO = 	new CwmsPlatformListIO(this, _configListIO, 
			_equipmentModelListIO, _decodesScriptIO);

		
		// Make sure the CWMS name type enumeration exists.
		DbEnum nameTypeList = Database.getDb().enumList.getEnum(
			Constants.enum_SiteName);
		if (nameTypeList != null
		 && nameTypeList.findEnumValue(Constants.snt_CWMS) == null)
		{
			try
			{
				nameTypeList.addValue(
					Constants.snt_CWMS, "CWMS Site Names", null, null);
			}
			catch(Exception ex) {}
		}

		// Oracle 11g requires that backslashes NOT be escaped in SQL strings.
		SqlDbObjIo.escapeBackslash = false;
		_isOracle = true;
	}

	/**
	 * Connects to the CWMS database.
	 * @param sqlDbLocation URI
	 */
	public void connectToDatabase(String sqlDbLocation)
		throws DatabaseException
	{
		// Placeholder for connecting from web where connection is from a DataSource.
		if (sqlDbLocation == null || sqlDbLocation.trim().length() == 0)
			return;

		CwmsGuiLogin cgl = CwmsGuiLogin.instance();
		if (DecodesInterface.isGUI())
		{
			try 
			{
				if (!cgl.isLoginSuccess())
				{
					cgl.doLogin(null);
					if (!cgl.isLoginSuccess()) // user hit cancel
						throw new DatabaseException("Login aborted by user.");
				}
				super.connectToDatabase(sqlDbLocation, cgl.getUserName(),
					new String(cgl.getPassword()));
				cgl.setLoginSuccess(true);
			}
			catch(DatabaseException ex)
			{
				cgl.setLoginSuccess(false);
				throw ex;
			}
			catch(Exception ex)
			{
				throw new DatabaseException(
					"Cannot display login dialog: " + ex);
			}
		}
		else // Non-GUI can try auth file mechanism.
		{
			Logger.instance().info("This is not a GUI app.");
			super.connectToDatabase(sqlDbLocation);
		}

		String q = null;
		Statement st = null;
		int tsdbVersion = TsdbDatabaseVersion.VERSION_2;  // earliest possible value.

		try
		{
			oracle.jdbc.OracleConnection ocon = (oracle.jdbc.OracleConnection)getConnection();
			ocon.setSessionTimeZone("GMT");

			st = getConnection().createStatement();
			
			// Hard-code date & timestamp format for reads. Always use GMT.
			q = "ALTER SESSION SET TIME_ZONE = 'GMT'";
			Logger.instance().info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_date_format = 'yyyy-mm-dd hh24:mi:ss'";
			Logger.instance().info(q);
			st.execute(q);
			
			q = "ALTER SESSION SET nls_timestamp_format = 'yyyy-mm-dd hh24:mi:ss'";
			Logger.instance().info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_timestamp_tz_format = 'yyyy-mm-dd hh24:mi:ss'";
			Logger.instance().info(q);
			st.execute(q);
			
			q = "SELECT * FROM tsdb_database_version";
			ResultSet rs = st.executeQuery(q);
			if (rs != null && rs.next())
			{
				tsdbVersion = rs.getInt(1);
			}
			Logger.instance().info("DECODES IF Connected to TSDB Version " + tsdbVersion);

		}
		catch(SQLException ex)
		{
			String msg = "Error in '" + q + "': " + ex
				+ " -- will proceed anyway.";
			Logger.instance().failure(msg + " " + ex);
		}
		finally
		{
			try { st.close(); } catch(Exception ex) {}
		}

		cwmsSchemaVersion = CwmsTimeSeriesDb.determineCwmsSchemaVersion(getConnection(),
			tsdbVersion);
		// CWMS 2.2 = Tsdb Version 8
		if (cwmsSchemaVersion >= CwmsTimeSeriesDb.CWMS_V_2_2)
		{
			Logger.instance().debug1(
				"Connected to CWMS " + cwmsSchemaVersion + " database. Will set office ID context.");

			dbOfficeId = null;
			ArrayList<StringPair> officePrivileges = null;
			try
			{
				officePrivileges = CwmsTimeSeriesDb.determinePrivilegedOfficeIds(
					getConnection(), cwmsSchemaVersion);
			}
			catch (SQLException ex)
			{
				String msg = "Cannot determine privileged office IDs: " + ex;
				Logger.instance().failure(module + " " + msg);
				cgl.setLoginSuccess(false);
				close();
				throw new DatabaseException(msg);
			}
			// Make sure office  matches for case with one of the privileged
			for(StringPair op : officePrivileges)
				if (TextUtil.strEqualIgnoreCase(op.first, DecodesSettings.instance().CwmsOfficeId))
				{
					dbOfficeId = op.first;
					dbOfficePrivilege = op.second;
					break;
				}
Logger.instance().debug3("isGUI=" + DecodesInterface.isGUI()+ ", #OfficePrivileges=" +
officePrivileges.size());
			// If GUI, allow user to select from the privileged offices
			if (DecodesInterface.isGUI() && officePrivileges.size() > 0)
			{
				if (!cgl.isOfficeIdSelected())
					cgl.selectOfficeId(null, officePrivileges, dbOfficeId);
				dbOfficeId = cgl.getDbOfficeId();
				dbOfficePrivilege = cgl.getDbOfficePrivilege();
			}
			else if (officePrivileges.size() > 0 && dbOfficeId == null)
			{
				// Not a GUI and not selected in properties.
				dbOfficeId = officePrivileges.get(0).first;
				dbOfficePrivilege = officePrivileges.get(0).second;
			}
			if (dbOfficeId == null)
			{
				close();
				cgl.setLoginSuccess(false);
				throw new DatabaseException("No office ID with any CCP Privilege!");
			}
			
			dbOfficeCode = CwmsTimeSeriesDb.officeId2code(getConnection(), dbOfficeId);
			try
			{
				CwmsTimeSeriesDb.setCtxDbOfficeId(getConnection(), dbOfficeId,
					dbOfficeCode, dbOfficePrivilege, tsdbVersion);
			}
			catch (Exception ex)
			{
				close();
				String msg = "Cannot set username/officeId username='" + _dbUser
					+ "', officeId='" + dbOfficeId + "' " + ex.getMessage();
				Logger.instance().failure(module + " " + msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
				cgl.setLoginSuccess(false);
				throw new DatabaseException(msg);
			}
		}
		else // CWMS 2.1 or earlier
		{
			dbOfficeId = DecodesSettings.instance().CwmsOfficeId;
			dbOfficeCode = CwmsTimeSeriesDb.officeId2code(getConnection(), dbOfficeId);
		}
		
		cgl.setLoginSuccess(true);
		Logger.instance().info(module + 
			" Connected to DECODES CWMS " + cwmsSchemaVersion 
			+ " Database " + sqlDbLocation + " as user " + _dbUser
			+ " with officeID=" + dbOfficeId + " (dbOfficeCode=" + dbOfficeCode + ")");
	}

	/** @return 'CWMS'. */
	public String getDatabaseType()
	{
		return "CWMS";
	}

	public String getOfficeId()
	{
		return dbOfficeId;
	}

	public String getSqlDbLocation()
	{
		return sqlDbLocation;
	}
	
	public boolean isCwms() { return true; }
	
	@Override
	public SiteDAI makeSiteDAO()
	{
		return new CwmsSiteDAO(this, dbOfficeId);
	}
	
	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new CwmsIntervalDAO(this, dbOfficeId);
	}

}
