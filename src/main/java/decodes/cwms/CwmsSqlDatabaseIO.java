/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;

import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.OpenTsdbSettings;
import opendcs.org.opendcs.jmx.ConnectionPoolMXBean;
import opendcs.org.opendcs.jmx.connections.JMXTypes;
import opendcs.util.sql.WrappedConnection;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;
import usace.cwms.db.dao.util.connection.CwmsDbConnectionPool;
import lrgs.gui.DecodesInterface;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.UserAuthFile;
import decodes.db.*;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.sql.SqlDatabaseIO;
import decodes.sql.SqlDbObjIo;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
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
	private CwmsConnectionInfo conInfo = null;
	private String sqlDbLocation = null;
	private CwmsConnectionPool pool = null;
	
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
		DecodesSettings.instance().sqlTimeZone = "GMT";
        writeDateFmt.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone));
		
		this.sqlDbLocation = sqlDbLocation;

		connectToDatabase(sqlDbLocation);

		/* 
		 * Oracle does not require a COMMIT after each block of nested SELECTs.
		 * The following causes the parent class to do this.
		 */
		commitAfterSelect = false;

		// Likewise we need a special platform IO to do office ID filtering.
		_platformListIO = new CwmsPlatformListIO(this, _configListIO, _equipmentModelListIO);

		// Make sure the CWMS name type enumeration exists.
		DbEnum nameTypeList = Database.getDb().enumList.getEnum(
			Constants.enum_SiteName);
		if (nameTypeList != null
		 && nameTypeList.findEnumValue(Constants.snt_CWMS) == null)
		{
			try
			{
				nameTypeList.addValue(Constants.snt_CWMS, "CWMS Site Names", null, null);
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

		// MJM 2018-12-05 The new HEC/RMA connection facility requires that office ID
		// be known before getting a connection from the pool. Therefore I cannot set
		// it dynamically from the database or from user selection.
		dbOfficeId = DecodesSettings.instance().CwmsOfficeId;
		
		// CWMS is Always GMT.
		DecodesSettings.instance().sqlTimeZone = "GMT";

		String password = null;

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
				_dbUser = cgl.getUserName();
				password = new String(cgl.getPassword());
			}
			catch(DatabaseException ex)
			{
				cgl.setLoginSuccess(false);
				throw ex;
			}
			catch(Exception ex)
			{
				String msg = "Cannot display login dialog: " + ex;
				System.err.println(msg);
				ex.printStackTrace(System.err);
				throw new DatabaseException(msg);
			}
		}
		else // Non-GUI can try auth file mechanism.
		{
			Logger.instance().info("This is not a GUI app.");
			
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

			_dbUser = authFile.getUsername();
			password = authFile.getPassword();
		}
		if( conInfo == null)
		{
			conInfo = new CwmsConnectionInfo();
			ConnectionLoginInfo info = new ConnectionLoginInfoImpl(sqlDbLocation, _dbUser,password, dbOfficeId);
			conInfo.setLoginInfo(info);
		}

		try
		{
			pool = CwmsConnectionPool.getPoolFor(conInfo);
			if( pool != null)
			{
				try(Connection conn = pool.getConnection();)
				{
					Logger.instance().info(module +
						" Connected to DECODES CWMS Database " + sqlDbLocation + " as user " + _dbUser
						+ " with officeID=" + dbOfficeId);
					readVersionInfo(this, conn);

					// CWMS OPENDCS-16 for DB version >= 68, use old OracleSequenceKeyGenerator,
					// which assumes a separate sequence for each table. Do not use CWMS_SEQ for anything.
					int decodesDbVersion = getDecodesDatabaseVersion();
					Logger.instance().info(module + " decodesDbVersion=" + decodesDbVersion);
					keyGenerator = decodesDbVersion >= DecodesDatabaseVersion.DECODES_DB_68 ?
							new OracleSequenceKeyGenerator() :
							new CwmsSequenceKeyGenerator(decodesDbVersion);
						TimeSeriesDb.readVersionInfo(this, conInfo.getConnection());
						cgl.setLoginSuccess(true);

					String q = null;
					try(Statement st = conn.createStatement();)
					{
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

						Logger.instance().info("DECODES IF Connected to TSDB Version " + tsdbVersion);
					}
					catch(SQLException ex)
					{
						String msg = "Error in '" + q + "': " + ex
							+ " -- will proceed anyway.";
						Logger.instance().failure(msg + " " + ex);
					}

					cgl.setLoginSuccess(true);
					Logger.instance().info(module + 
						" Connected to DECODES CWMS Database " + sqlDbLocation + " as user " + _dbUser
						+ " with officeID=" + dbOfficeId + " (dbOfficeCode=" + conInfo.getDbOfficeCode() + ")");

					// CWMS-8979 Allow settings in the database to override values in user.properties.
					String settingsApp = System.getProperty("SETTINGS");
					if (settingsApp != null)
					{
						Logger.instance().info("SqlDatabaseIO Overriding Decodes Settings with properties in "
							+ "Process Record '" + settingsApp + "'");
						LoadingAppDAI loadingAppDAO = makeLoadingAppDAO();
						try
						{
							CompAppInfo cai = loadingAppDAO.getComputationApp(settingsApp);
							PropertiesUtil.loadFromProps(DecodesSettings.instance(), cai.getProperties());
						}
						catch (DbIoException ex)
						{
							Logger.instance().warning("Cannot load settings from app '" + settingsApp + "': " + ex);
						}
						catch (NoSuchObjectException ex)
						{
							Logger.instance().warning("Cannot load settings from non-existent app '" 
								+ settingsApp + "': " + ex);
						}
						finally
						{
							loadingAppDAO.close();
						}
					}

					cgl.setLoginSuccess(true);
					
				}
				catch(SQLException ex)
				{
					throw new DatabaseException("Pool was able to start but not retrieve connection",ex);
				}
			}
			else
			{
				throw new DatabaseException("unable to initialize pool for " + conInfo);
			}
		}
		catch(BadConnectException ex)
		{
			throw new DatabaseException("Unable to initialize for " + conInfo,ex);
		}
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

	@Override
	public Connection getConnection()
	{
		// Called from DAOs and DbIo to get a new connection from the pool.
		if (conInfo == null || conInfo.getLoginInfo() == null || pool == null)
		{
			Logger.instance().failure(module + ".getConnection -- loginInfo is null! DB not initialized?");
			return null;
		}
		try
		{
			return pool.getConnection();
		}
		catch(SQLException ex)
		{
			Logger.instance().fatal("Unabled to get connection " + ex.getLocalizedMessage());
			return null;
		}
	}


	@Override
	public void freeConnection(Connection con)
	{
		try
		{
			pool.returnConnection(con);
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("Unable to close returned connection: " + ex.getLocalizedMessage());
		}
	}
	
}
