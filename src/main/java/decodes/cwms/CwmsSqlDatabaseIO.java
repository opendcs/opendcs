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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.spi.authentication.AuthSource;

import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;
import lrgs.gui.DecodesInterface;
import ilex.util.AuthException;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
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
public class CwmsSqlDatabaseIO extends SqlDatabaseIO
{
	public final static String module = "CwmsSqlDatabaseIO";
	/** The office ID associated with this connection. This implicitely
	 * filters the records that are visible.
	 */
	private final String dbOfficeId;
	private String sqlDbLocation = null;
	
	/**
 	* Constructor.  The argument is the "location" of the
 	* database from the "decodes.properties" file.
	* This should be a string in the form:
	* 	jdbc:oracle:thin:@hostname:1521:dbname,
	* where hostname and dbname specify the Oracle CWMS database.
	* @param sqlDbLocation the location string from decodes.properties file
 	*/
	public CwmsSqlDatabaseIO(javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException
	{
		// No-args base class ctor doesn't connect to DB.
		super(dataSource, settings);
		this.dbOfficeId = settings.CwmsOfficeId;
        writeDateFmt = new SimpleDateFormat(
			"'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')");
		DecodesSettings.instance().sqlTimeZone = "GMT";
        writeDateFmt.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone));

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
		try
		{
			return super.getConnection();
		}
		catch(SQLException ex)
		{
			throw new RuntimeException("Error retrieving connection",ex);
		}
	}


	@Override
	public void freeConnection(Connection con)
	{
		try
		{
			con.close();
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("Unable to close returned connection: " + ex.getLocalizedMessage());
		}
	}
	
}
