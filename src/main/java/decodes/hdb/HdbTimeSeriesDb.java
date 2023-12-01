/*
*  $Id: HdbTimeSeriesDb.java,v 1.25 2020/02/12 15:18:03 mmaloney Exp $
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
*  $Log: HdbTimeSeriesDb.java,v $
*  Revision 1.25  2020/02/12 15:18:03  mmaloney
*  Remove unneeded debugs
*
*  Revision 1.24  2019/12/11 14:37:10  mmaloney
*  Support OS authentication for HDB (issue 771)
*
*  Revision 1.23  2019/06/26 14:23:09  mmaloney
*  Issue 706 transformUniqueString expand SDI if it isn't already expanded.
*
*  Revision 1.22  2019/06/10 19:23:22  mmaloney
*  Added getStorageUnitsForDataType
*
*  Revision 1.21  2019/02/26 17:16:44  mmaloney
*  HDB 660
*
*  Revision 1.20  2019/02/25 20:02:55  mmaloney
*  HDB 660 Allow Computation Parameter Site and Datatype to be set independently in group comps.
*
*  Revision 1.19  2018/12/19 19:56:30  mmaloney
*  Remove references to classes in oracle.jdbc and oracle.sql, except in HDB branch.
*
*  Revision 1.18  2018/05/01 17:38:54  mmaloney
*  Code cleanup of base classes.
*
*  Revision 1.17  2018/02/21 14:34:19  mmaloney
*  Set autocommit true always.
*
*  Revision 1.16  2018/02/21 14:33:03  mmaloney
*  Set autocommit true always.
*
*  Revision 1.15  2018/02/19 16:23:03  mmaloney
*  Attempt to reclaim tasklist space if tasklist is empty and feature is enabled.
*
*  Revision 1.14  2018/02/14 17:03:31  mmaloney
*  Refactor: Get rid of the 'getNewDataSince()' method because it was unused.
*
*  Revision 1.13  2018/02/05 15:51:32  mmaloney
*  Added cache of HDB Object Types
*
*  Revision 1.12  2017/10/03 12:32:09  mmaloney
*  Code cleanup
*
*  Revision 1.11  2017/05/31 21:25:04  mmaloney
*  Fill HdbDataType cache.
*  Implement expandSDI.
*
*  Revision 1.10  2017/05/01 19:23:26  mmaloney
*  Remove all references to SITE and SITENAME. In HDB these don't exist.
*
*  Revision 1.9  2017/03/23 16:06:00  mmaloney
*  Throw BadTimeSeriesException when can't create time series. It's probably due to running on a read-only HDB.
*
*  Revision 1.8  2017/03/22 19:33:05  mmaloney
*  Throw BadTimeSeriesException when can't create time series. It's probably due to running on a read-only HDB.
*
*  Revision 1.7  2016/11/03 19:03:05  mmaloney
*  Remove expandTsGroup. This is now done with a helper class.
*
*  Revision 1.6  2016/07/20 15:37:03  mmaloney
*  Remove getDataTypesByStandard.
*
*  Revision 1.5  2016/06/27 15:14:37  mmaloney
*  Numerous updates for 6.2 testing.
*
*  Revision 1.4  2016/06/07 21:51:27  mmaloney
*  Added makeSiteDAO. Cleaned up imports.
*
*  Revision 1.3  2016/03/24 19:07:43  mmaloney
*  Refactor: Have expandSDI return the TimeSeriesID that it uses. This saves the caller from
*  having to re-look it up. Needed for PythonAlgorithm.
*
*  Revision 1.2  2016/01/27 21:58:44  mmaloney
*  Init Optimization
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.117  2013/07/25 15:21:21  mmaloney
*  DbKey in lookup data type bug fix.
*
*  Revision 1.116  2013/07/24 18:11:11  mmaloney
*  dev
*
*  Revision 1.115  2013/07/24 13:36:36  mmaloney
*  Cleanup the listCompsForGui stuff. This is all now portable across databases.
*
*  Revision 1.114  2013/06/26 18:35:14  mmaloney
*  createTimeSeriesInDb was incorrectly using data type ID rather than code.
*
*  Revision 1.113  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.hdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.Date;
import java.util.GregorianCalendar;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import opendcs.dai.ComputationDAI;
import opendcs.dai.DaiBase;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import oracle.jdbc.OraclePreparedStatement;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import ilex.var.NoConversionException;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.sql.OracleDateParser;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;

/**
This is the base class for the time-series database implementation.
Sub classes must override all the abstract methods and provide
a mechanism to persistently store time series and computational meta
data.
*/
public class HdbTimeSeriesDb
	extends TimeSeriesDb
{
	private OracleDateParser oracleDateParser = null;
	
	private HdbSdiCache hdbSdiCache = new HdbSdiCache();
	
//	private NumberFormat valueFmt = NumberFormat.getNumberInstance();
	private OraclePreparedStatement insertTasklist = null;
	
	private ArrayList<HdbDataType> hdbDataTypes = null;
	
	private ArrayList<HdbObjectType> hdbObjectTypes = null; 


	/**
	 * No args constructor required because this is instantiated from
	 * the class name.
	 */
	public HdbTimeSeriesDb()
	{
		super();
		module = "hdb";
		sdiIsUnique = false;

		// NOTE: We always read & write dates & timestamps in GMT (aka UTC).
		// the connect method below will "alter session set time_zone" to GMT.

		// Overwrite dateFmt created by base class. We will use the
		// Oracle to_date function:
		curTimeName = "sysdate";
		maxCompRetryTimeFrmt = "%d*1/24";

		Site.explicitList = true;
//		valueFmt.setGroupingUsed(false);
//		valueFmt.setMaximumFractionDigits(5);
	}

	/**
	 * Connect this app to the database and return appID. 
	 * The credentials property set contains username, password,
	 * etc, for connecting to database.
	 * @param appName must match an application in the database.
	 * @param credentials must contain all needed login parameters.
	 * @return application ID.
	 * @throws BadConnectException if failure to connect.
	 */
	public DbKey connect( String appName, Properties credentials )
		throws BadConnectException
	{
		String driverClass = DecodesSettings.instance().jdbcDriverClass;
		String dbUri = DecodesSettings.instance().editDatabaseLocation;
		String username = credentials.getProperty("username");
		String password = credentials.getProperty("password");
		try 
		{
			Class.forName(driverClass);
			
			if (DecodesSettings.instance().tryOsDatabaseAuth)
			{
				// 12/3/2019 Empty props will cause OS (IDENT) authentication
				Properties emptyProps = new Properties();
				try { conn = DriverManager.getConnection(dbUri, emptyProps); }
				catch(SQLException ex)
				{
					Logger.instance().info(module + " Connection using OS authentication failed. "
						+ "Will attempt username/password auth.");
					conn = null;
				}
			}
			
			if (conn == null)
				conn = DriverManager.getConnection(dbUri, username, password);
		}
		catch (Exception ex) 
		{
			conn = null;
			throw new BadConnectException(
				"Error getting JDBC connection using driver '"
				+ driverClass + "' to database at '" + dbUri
				+ "' for user '" + username + "': " + ex.toString());
		}

		keyGenerator = new OracleSequenceHDBGenerator();

		String q = null;
		Statement st = null;
		try
		{
			st = conn.createStatement();
			
			q = "SELECT PARAM_VALUE FROM REF_DB_PARAMETER WHERE PARAM_NAME = 'TIME_ZONE'";
			ResultSet rs = null;
//			Logger.instance().info(q);

			try
			{ 
				rs = st.executeQuery(q);
				if (rs != null && rs.next())
				{
					databaseTimezone = rs.getString(1);
					DecodesSettings.instance().sqlTimeZone = databaseTimezone;
				}
//				Logger.instance().info("databaseTimezone is '" + databaseTimezone + "'");
			}
			catch(SQLException ex)
			{
				databaseTimezone = DecodesSettings.instance().sqlTimeZone;
				Logger.instance().info(
					" -- failed, using sqlTimeZone setting of '"
					+ databaseTimezone + "'");
			}
			finally
			{
				try { rs.close(); } catch(Exception ex) {}
				rs = null;
			}
			conn.unwrap(oracle.jdbc.OracleConnection.class).setSessionTimeZone(databaseTimezone);

			// Hard-code date & timestamp format for reads. Always use GMT.
			q = "ALTER SESSION SET TIME_ZONE = '" + databaseTimezone + "'";
			info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_date_format = 'yyyy-mm-dd hh24:mi:ss'";
			info(q);
			st.execute(q);
			
			q = "ALTER SESSION SET nls_timestamp_format = 'yyyy-mm-dd hh24:mi:ss'";
			info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_timestamp_tz_format = 'yyyy-mm-dd hh24:mi:ss'";
			info(q);
			st.execute(q);
			
			// MJM 2018-2/21 Force autoCommit on.
			try { conn.setAutoCommit(true);}
			catch(SQLException ex)
			{
				Logger.instance().warning("Cannot set SQL AutoCommit to true: " + ex);
			}

			writeDateFmt = new SimpleDateFormat(
				"'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')");
			writeDateFmt.setTimeZone(TimeZone.getTimeZone(databaseTimezone));
			info("Set date fmt to time zone '" + databaseTimezone + "' current time="
				+ writeDateFmt.format(new Date()));
		}
		catch(SQLException ex)
		{
			String msg = "Error in '" + q + "': " + ex
				+ " -- will proceed anyway.";
			failure(msg);
		}
		finally
		{
			try { st.close(); } catch(Exception ex) {}
		}
		
		readDateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		readDateFmt.setTimeZone(TimeZone.getTimeZone(databaseTimezone));

		this.postConnectInit(appName, conn);
		
		try
		{
			q = "insert into cp_comp_tasklist("
			    + "record_num, loading_application_id,"
			    + "site_datatype_id,interval,table_selector,"
			    + "value,date_time_loaded,start_date_time,"
			    + "delete_flag,model_run_id,validation,data_flags)"
			    + " values (cp_tasklist_sequence.nextval, ?, "
			    + "?, ?, ?,"
			    + "?, sysdate, ?, 'N', ?, ?, ?)";
			
			insertTasklist = (OraclePreparedStatement)conn.prepareStatement(q);
		}
		catch (SQLException ex)
		{
			warning("Cannot prepare statement '" + q + "': " + ex);
		}
		
		return appId;
	}

	public void setParmSDI(DbCompParm parm, DbKey siteId, String dtcode)
		throws DbIoException, NoSuchObjectException
	{
		DataTypeDAI dtDao = this.makeDataTypeDAO();
		try
		{
			DataType dt = dtDao.lookupDataType(dtcode);
			if (!dt.getStandard().equalsIgnoreCase(Constants.datatype_HDB))
			{
				DataType hdt = dt.findEquivalent(Constants.datatype_HDB);
				if (hdt == null)
					throw new NoSuchObjectException(
						"Cannot determine HDB datatype equivalence for " + dt);
				dt = hdt;
			}
	
			// We now have an HDB data type.
			DbKey sdi = lookupSDI(siteId, dt.getCode());
			parm.setSiteDataTypeId(sdi);
		}
		finally
		{
			dtDao.close();
		}
	}

	@Override
	public TimeSeriesIdentifier expandSDI(DbCompParm parm)
		throws DbIoException, NoSuchObjectException
	{
		DbKey sdi = parm.getSiteDataTypeId();
		DbKey siteId = parm.getSiteId();
		DbKey datatypeId = parm.getDataTypeId();
		
//Logger.instance().debug1("HdbTimeSeriesDb.expandSDI sdi=" + sdi + ", siteId=" + siteId + ", datatypeId=" + datatypeId);
		
		if (!DbKey.isNull(sdi))
		{
			HdbSiteDatatype hsdi = getHSDI(sdi);
			if (hsdi == null)
				throw new NoSuchObjectException("No such site-datatype with SDI=" + sdi);
			siteId = hsdi.getSiteId();
			datatypeId = hsdi.getDatatypeId();
//Logger.instance().debug1("HdbTimeSeriesDb.expandSDI derived from sdi, siteId=" + siteId + ", dtid=" + datatypeId);
		}
		parm.setSite(getSiteById(siteId));
//Logger.instance().debug1("HdbTimeSeriesDb.expandSDI after getSiteById, siteId=" + siteId
//	+ ", sitename=" + 
//	(parm.getSite().getPreferredName() != null ? parm.getSite().getPreferredName().getNameValue() : "(none)"));
		
		if (!DbKey.isNull(datatypeId))
		{
			DataType dt = DataType.getDataType(datatypeId);
			if (dt == null)
				dt = DataType.getDataType(Constants.datatype_HDB, "" + datatypeId);
			parm.setDataType(dt);
		}
		TimeSeriesIdentifier tsid = null;
		
		if (!DbKey.isNull(siteId) && !DbKey.isNull(datatypeId) && parm.getInterval() != null)
		{
			String tsidstr = "" + siteId + "." + datatypeId + "."
				+ parm.getInterval() + "." + parm.getTableSelector();
			if (parm.getTableSelector().toLowerCase().contains("m"))
				tsidstr = tsidstr + "." + parm.getModelId();
			TimeSeriesDAI hdbtsdao = this.makeTimeSeriesDAO();
			try
			{
				tsid = hdbtsdao.getTimeSeriesIdentifier(tsidstr);
			}
			catch(NoSuchObjectException ex)
			{
				warning("Invalid tsidstr '" + tsidstr + "': " + ex);
			}
			finally
			{
				hdbtsdao.close();
			}
		}
		return tsid;
	}

	
	/**
	 * Given a site surrogate key ID, and an HDB data type code, return 
	 * the site-datatype-ID.
	 * The dataTypeCode must be valid for the database being accessed.
	 * For NWIS, this is the DD Number. For HDB, this is the datatype ID.
	 * @return sdi or Constants.undefinedId if not found.
	 * @throws DbIoException on Database IO error.
	 */
	public DbKey lookupSDI( DbKey siteId, String dataTypeCode )
		throws DbIoException
	{
		DbKey datatypeId = Constants.undefinedId;
		try { datatypeId = DbKey.createDbKey(Integer.parseInt(dataTypeCode.trim())); }
		catch(Exception ex)
		{
			warning("lookupSDI invalid HDB dataTypeCode '" + dataTypeCode + "'");
		}
		
		HdbSiteDatatype hsdi = hdbSdiCache.get(siteId, datatypeId);
		if (hsdi != null)
			return hsdi.getSdi();
		
		String q = "select SITE_DATATYPE_ID from HDB_SITE_DATATYPE"
			+ " where SITE_ID = " + siteId
			+ " and DATATYPE_ID = " + dataTypeCode;
		DaiBase dao = new DaoBase(this, "HDB");
		try
		{
			ResultSet rs = dao.doQuery(q);
			if (rs.next())
			{
				hsdi = new HdbSiteDatatype(DbKey.createDbKey(rs, 1), siteId, datatypeId);
				hdbSdiCache.add(hsdi);
				return hsdi.getSdi();
			}

			if (TextUtil.str2boolean(this.getProperty("autoCreateTs")))
			{
				String mq = "INSERT into HDB_SITE_DATATYPE values("
					+ siteId + ", " + dataTypeCode + ", 0)";
				dao.doModify(mq);
				//commit();
			}
			rs = dao.doQuery(q);
			if (rs.next())
			{
				hsdi = new HdbSiteDatatype(DbKey.createDbKey(rs, 1), siteId, datatypeId);
				hdbSdiCache.add(hsdi);
				return hsdi.getSdi();
			}
			return Constants.undefinedId;
		}
		catch(Exception ex)
		{
			String msg= "Error getting SDI from siteId=" + siteId
				+ ", dtCode='" + dataTypeCode + "': " + ex;
			logger.warning(msg);
			return DbKey.NullKey;
			//throw new DbIoException(msg);
		}
		finally
		{
			dao.close();
		}
	}
	
	/**
	 * Validate the passed information to make sure it represents a valid
	 * parameter within this database. If not, throw ConstraintException.
	 */
	public void validateParm(DbKey siteId, String dtcode, String interval, 
		String tabSel, int modelId)
		throws ConstraintException, DbIoException
	{
		if (tabSel.equals("M_") && interval.equalsIgnoreCase("instant"))
			throw new ConstraintException("Modeled data cannot be interval INSTANT");
//TODO:
// HDB imposes restrictions on combination of sdi and interval.
// HDB probably has restrictions on which params can be modeled.
// If modelId is not Constants.undefinedId, make sure it's valid.
	}

	/**
	 * Returns the maximum valid run-id for the specified model.
	 * @param modelId the ID of the model
	 * @return the maximum valid run-id for the specified model.
	 */
	public int findMaxModelRunId(int modelId)
		throws DbIoException
	{
		String q = "select max(MODEL_RUN_ID) from REF_MODEL_RUN "
			+ "where MODEL_ID = " + modelId;
		int r = Constants.undefinedIntKey;
		DaiBase dao = new DaoBase(this,"HDB");
		try
		{
			ResultSet rs = dao.doQuery(q);
			if (rs.next())
			{
				r = rs.getInt(1);
				if (rs.wasNull())
					r = Constants.undefinedIntKey;
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("findMaxModelRunId: " + ex);
			r = Constants.undefinedIntKey;
		}
		finally
		{
			dao.close();
		}
//Logger.instance().info("findMaxModelRunId(modelId=" + modelId 
//+ ") returning " + r);
		return r;
	}

	/**
	 * HDB stores coefficients in the 'stat' tables. Example:
	 * Hourly Real Stat Table is R_HOURSTAT. This method returns the
	 * coefficent for the specified SDI in the specified table, for the
	 * specified time-period.
	 * @see decodes.tsdb.TimeSeriesDb#getCoeff(int, java.lang.String, java.lang.String, java.util.Date)
	 */
	public double getCoeff(DbKey sdi, String ts, String interval, Date date)
		throws DbIoException, DbCompException
	{
		double coeff = 0.0;
		int statIndex;
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		
		if (interval == null)
			throw new DbCompException("Trying to get coefficient for SDI=" + sdi + ", with null interval.");
		if (ts == null)
			throw new DbCompException("Trying to get coefficient for SDI=" + sdi + ", with null table selector.");
	
		//ugly dependency on interval names not changing
		if (interval.equalsIgnoreCase("day")) 
		{
			//day 1 is Oct 1, water year
			statIndex = cal.get(Calendar.DAY_OF_YEAR) +92; // 92 days in Oct-Dec 
			if(statIndex >=152 && !cal.isLeapYear(cal.get(Calendar.YEAR))) {
				statIndex++; //skip Feb 29 for non leap years
			}
			if(statIndex > 366) { // wrap around on Oct 1
				statIndex -= 366;
			}
		}
		else if (interval.equalsIgnoreCase("month")) 
		{//offset month by 3 to handle starting in water year, subtract 12 to get in valid range
			statIndex = cal.get(Calendar.MONTH)+3+1;//value is zero based, r_monthstat is one based
			if (statIndex > 12) statIndex -= 12;
		}
		else if (interval.equalsIgnoreCase("hour")) 
		{
			statIndex = cal.get(Calendar.HOUR_OF_DAY);
		}
		else if (interval.equalsIgnoreCase("year")) 
		{
			statIndex = cal.get(Calendar.YEAR);
		}
		else if (interval.equalsIgnoreCase("wy")) 
		{
			statIndex = cal.get(Calendar.YEAR);
			if (cal.get(Calendar.MONTH)>=10) 
			{
				statIndex++; //wy begins Oct 1
			}
		}
		else 
		{
			throw new DbIoException("Cannot recognize interval name "
				+ interval + " for coefficient selection");
		}
		

		String q = "select value from "+ts+interval+"stat where " +
				interval+" = "+statIndex+" and site_datatype_id = "+sdi;
		DaiBase dao = new DaoBase(this,"HDB");
		try 
		{
			ResultSet rs = dao.doQuery(q);

			if (rs.next())
			{
				coeff = rs.getDouble(1);
				if (rs.wasNull()) 
				{ //should never happen
					throw new SQLException("coefficient was null");
				}
			}
			else throw new SQLException("unable to select coefficient");
		}
		catch (SQLException ex) 
		{
			String msg = "Cannot find coefficient for interval " 
				+ interval + " sdi " + sdi +": "+ex;
			throw new DbIoException(msg);	
		}
		finally
		{
			dao.close();
		}

		return coeff;
 	}

	/** @return label to use for 'limit' column in tables. */
	public String getLimitLabel() { return "Der"; }

	public String flags2LimitCodes(int flags)
	{
		return HdbFlags.flag2HdbDerivation(flags);
	}

	@Override
	public String getRevisionLabel() { return "Val"; }

	@Override
	public String flags2RevisionCodes(int flags)
	{
		char c = HdbFlags.flag2HdbValidation(flags);
		if (c == (char)0)
			return "";
		else
			return "" + c;
	}

//	@Override
//	public DataCollection getNewData(DbKey applicationId)
//		throws DbIoException
//	{
//		// Reload the TSID cache every hour.
//		if (System.currentTimeMillis() - lastTsidCacheRead > 3600000L)
//		{
//			lastTsidCacheRead = System.currentTimeMillis();
//			TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
//			try { timeSeriesDAO.reloadTsIdCache(); }
//			finally
//			{
//				timeSeriesDAO.close();
//			}
//		}
//		
//		String q = "";
//		String attrList = "RECORD_NUM, SITE_DATATYPE_ID, INTERVAL, "
//			+ "TABLE_SELECTOR, VALUE, START_DATE_TIME, DELETE_FLAG, "
//			+ "MODEL_RUN_ID, VALIDATION, DATA_FLAGS";
//
//		DataCollection dataCollection = new DataCollection();
//
//		q = "select " + attrList + " from CP_COMP_TASKLIST "
//		  + "where LOADING_APPLICATION_ID = " + applicationId + " and rownum < 10000 ";
//
//		if (tsdbVersion >= 4)
//			q = q + " and (FAIL_TIME is null OR "
//				+ "SYSDATE - to_date("
//					+ "to_char(FAIL_TIME,'dd-mon-yyyy hh24:mi:ss'),"
//					+ "'dd-mon-yyyy hh24:mi:ss') >= 1/24)";
//
////		now add the order by record_num to insure last change wins
//		q = q + " order by record_num";
//
//		ArrayList<TasklistRec> tasklistRecs = new ArrayList<TasklistRec>();
//		RecordRangeHandle rrhandle = new RecordRangeHandle(applicationId);
//		try
//		{
//			ResultSet rs = doQuery(q);
//			while (rs != null && rs.next())
//			{
//				// Extract the info needed from the result set row.
//				int recordNum = rs.getInt(1);
//				DbKey sdi = DbKey.createDbKey(rs, 2);
//				String interval = rs.getString(3);
//				String tabsel = rs.getString(4);
//				double value = rs.getDouble(5);
//				Date timeStamp = getFullDate(rs, 6);
//				boolean deleted = getBoolean(rs, 7);
//				int modelRunId = rs.getInt(8);
//				if (rs.wasNull())
//					modelRunId = Constants.undefinedIntKey;
//				String valstr = rs.getString(9);
//				char valchar = (rs.wasNull() || valstr.length() == 0) ?
//					HdbFlags.HDB_BLANK_VALIDATION : valstr.charAt(0);
//				String derivation = rs.getString(10);
//				if (rs.wasNull())
//					derivation = "";
//
//				// Convert the HDB derivation, validation & deletion flags into
//				// a single 32-bit integer.
//				int flags = HdbFlags.hdbDerivation2flag(derivation)
//					| HdbFlags.hdbValidation2flag(valchar);
//				if (!deleted)
//					flags |= VarFlags.DB_ADDED;
//				else
//					flags |= VarFlags.DB_DELETED;
//				
//				
//				tasklistRecs.add(
//					new TasklistRec(recordNum, sdi, value,
//						timeStamp, deleted,
//						flags, interval, tabsel, modelRunId));
//			}
//			
//			if (tasklistRecs.size() == 0)
//			{
//				// MJM 6.4 RC08 this means tasklist is likely empty.
//				reclaimTasklistSpace();
//			}
//			
//			ArrayList<Integer> badRecs = new ArrayList<Integer>();
//			for(TasklistRec rec : tasklistRecs)
//			{
//				// Find time series if already in data collection.
//				// If not construct one and add it.
//				CTimeSeries cts = getTimeSeriesFor(dataCollection, 
//					rec.getSdi(), rec.getInterval(), rec.getTableSelector(),
//					rec.getModelRunId(), applicationId);
//				if (cts == null)
//				{
//					badRecs.add(rec.getRecordNum());
//					continue;
//				}
//				
//				// Keep track of record number range seen.
//				rrhandle.addRecNum(rec.getRecordNum());
//
//				// Construct timed variable & add it.
//				TimedVariable tv = new TimedVariable(rec.getValue());
//				tv.setTime(rec.getTimeStamp());
//				tv.setFlags(rec.getFlags());
//				
//				cts.addSample(tv);
//				
//				// Remember which tasklist records are in this timeseries.
//				cts.addTaskListRecNum(rec.getRecordNum());
//			}
//			
//			dataCollection.setTasklistHandle(rrhandle);
//			
//			// Delete the bad tasklist recs, 250 at a time.
//			while (badRecs.size() > 0)
//			{
//				StringBuilder inList = new StringBuilder();
//				int n = badRecs.size();
//				int x=0;
//				for(; x<250 && x<n; x++)
//				{
//					if (x > 0)
//						inList.append(", ");
//					inList.append(badRecs.get(x).toString());
//				}
//				q = "delete from CP_COMP_TASKLIST "
//					+ "where RECORD_NUM IN (" + inList.toString() + ")";
//				doModify(q);
////				commit();
//				for(int i=0; i<x; i++)
//					badRecs.remove(0);
//			}
//
//			return dataCollection;
//		}
//		catch(SQLException ex)
//		{
//			System.err.println("Error reading new data: " + ex);
//			ex.printStackTrace();
//			throw new DbIoException("Error reading new data: " + ex);
//		}
//	}



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
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId, DaiBase dao)
		throws DbIoException
	{
		String header[] = new String[5];
		header[0] = "HDB DataType"; // Equivalent to the datatype ID
		header[1] = "Name";
		header[2] = "Interval";
		header[3] = "Real/Model";
		header[4] = "Model ID";
		
		ArrayList<String[]> ret = new ArrayList<String[]>();
		ret.add(header);

		String q = "SELECT c.DATATYPE_ID, c.DATATYPE_COMMON_NAME, a.INTERVAL, "
			+ " a.TABLE_SELECTOR, a.MODEL_ID "
			+ " FROM CP_TS_ID a, HDB_SITE_DATATYPE b, HDB_DATATYPE c "
			+ " WHERE a.SITE_DATATYPE_ID = b.SITE_DATATYPE_ID "
			+ " AND b.DATATYPE_ID = c.DATATYPE_ID "
			+ " AND b.SITE_ID = " + siteId;
		
		try
		{
			ResultSet rs = dao.doQuery(q);
			while(rs.next())
			{
				String dtl[] = new String[5];
				dtl[0] = "" + rs.getInt(1);
				dtl[1] = rs.getString(2);
				dtl[2] = rs.getString(3);
				String tabsel = rs.getString(4);
				dtl[3] = tabsel != null && tabsel.length() > 0 
					&& Character.toLowerCase(tabsel.charAt(0)) == 'm' ? "Modeled" : "Real";
				int mid = rs.getInt(5);
				dtl[4] = dtl[3].charAt(0) == 'M' ? ""+mid : "N/A";
				ret.add(dtl);
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning("Error reading datatypes for siteId="
				+ siteId + ": " + ex);
		}
		return ret;
	}


	public boolean isHdb() { return true; }

	@Override
	public String[] getTsIdParts()
	{
		return HdbTsId.tsIdParts;
	}

	@Override
	public TimeSeriesIdentifier transformTsidByCompParm(
			TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
			boolean fillInParm, String timeSeriesDisplayName) 
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		if (tsid == null)
			tsid = makeEmptyTsId();

		HdbTsId tsidRet = (HdbTsId)tsid.copyNoKey();
		boolean transformed = this.transformUniqueString(tsidRet, parm);
		
		if (transformed)
		{
			String uniqueString = tsidRet.getUniqueString();
			Logger.instance().debug3("HdbTimeSeriesDb.transformTsid new string='"
				+ uniqueString);
			TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();

			try 
			{
				tsidRet = (HdbTsId)timeSeriesDAO.getTimeSeriesIdentifier(uniqueString);
				Logger.instance().debug3("HdbTimeSeriesDb.transformTsid "
					+ "time series '" + uniqueString + "' exists previously.");
			}
			catch(NoSuchObjectException ex)
			{
				debug3("transformTsidByCompParm: No existing TS for '"
					+ uniqueString + "': " + ex.getMessage());
				if (createTS)
				{
					debug3("Will attempt to create.");
					if (timeSeriesDisplayName != null)
						tsidRet.setDisplayName(timeSeriesDisplayName);
					timeSeriesDAO.createTimeSeries(tsidRet);
				}
				else
				{
					debug3("transformTsid "
						+ "no such time series '" + uniqueString + "' and createFlag=false");
					return null;
				}
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}
		else
		{
			tsidRet = (HdbTsId)tsid;
//debug3("transformTsidByCompParm transform left tsid unchanged");
		}
		
		
		if (fillInParm)
		{
			parm.setSiteDataTypeId(tsidRet.getSdi());
			parm.setInterval(tsidRet.getInterval());
			parm.setTableSelector(tsidRet.getPart(HdbTsId.TABSEL_PART));
			parm.setModelId(tsidRet.modelId);
			parm.setDataType(tsidRet.getDataType());
			parm.setSite(tsidRet.getSite());
		}

		return tsidRet;
	}
	
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet, DbCompParm parm)
	{
		if (parm.getSiteName() == null
		 && (!DbKey.isNull(parm.getSiteDataTypeId()) || !DbKey.isNull(parm.getSiteId())))
		{
			try
			{
				expandSDI(parm);
			}
			catch (Exception ex)
			{
				debug3(module + ".transformUniqueString error in expandSDI: " + ex);
			}
		}
		
		SiteName sn = parm.getSiteName();
		if (sn == null && !DbKey.isNull(parm.getSiteId()))
			sn = new SiteName(null, "HDB", "" + parm.getSiteId());
		
//debug3("transformUniqueString() before tsid=" + tsidRet.getUniqueString()
//+ ", parm.getSiteName=" + (sn==null ? "null" : sn.toString()) + ", parm.siteId=" + parm.getSiteId());
		boolean transformed = false;
		if (sn != null)
		{
			tsidRet.setSiteName(sn.getNameValue());
			tsidRet.setSite(sn.getSite());
			transformed = true;
			// Setting site will invalidate SDI. Force a new lookup.
			((HdbTsId)tsidRet).setSdi(Constants.undefinedId);
		}
		DataType dt = parm.getDataType();
		if (dt != null)
		{
			tsidRet.setDataType(dt);
			transformed = true;
			// Setting data type will invalidate SDI. Force a new lookup.
			((HdbTsId)tsidRet).setSdi(Constants.undefinedId);
		}
		String s = parm.getInterval();
		if (s != null && s.length() > 0)
		{
			tsidRet.setPart(HdbTsId.INTERVAL_PART, s);
			transformed = true;
		}
		s = parm.getTableSelector();
		if (s != null && s.length() > 0)
		{
			tsidRet.setPart(HdbTsId.TABSEL_PART, s);
			transformed = true;
		}
		int mid = parm.getModelId();
		if (mid != Constants.undefinedIntKey)
		{
			tsidRet.setPart(HdbTsId.MODELID_PART, ""+mid);
		}
//debug3("transformUniqueString() after tsid=" + tsidRet.getUniqueString());
		return transformed;
	}


	/**
	 * @return a full date, including time information.
	 */
	public Date getFullDate(ResultSet rs, int column)
	{
//Logger.instance().debug3("HdbTimeSeriesDb getFullDate");
		if (oracleDateParser == null)
		{
			oracleDateParser = makeOracleDateParser(TimeZone.getTimeZone(databaseTimezone));
		}
		Date ret = oracleDateParser.getTimeStamp(rs, column);
		if (ret == null)
			ret = super.getFullDate(rs, column);
		return ret;
	}
	
	@Override
	public TimeSeriesIdentifier makeEmptyTsId()
	{
		return new HdbTsId();
	}

	/**
	 * Get an HdbSiteDatatype from the cache, or if necessary from HDB and add
	 * it to the cache.
	 * @param sdi the integer site_datatype_id
	 * @return the HdbSiteDatatype or null if no such SDI in HDB.
	 * @throws DbIoException on database error.
	 */
	private HdbSiteDatatype getHSDI(DbKey sdi)
		throws DbIoException
	{
		if (hdbSdiCache.size() == 0)
			fillHdbSdiCache();
		HdbSiteDatatype hsdi = hdbSdiCache.get(sdi);
		if (hsdi != null)
			return hsdi;

		String q = "select SITE_ID, DATATYPE_ID from HDB_SITE_DATATYPE where SITE_DATATYPE_ID = " + sdi;
		DaiBase dao = new DaoBase(this,"HDB");
		try
		{
			ResultSet rs = dao.doQuery(q);
			if(rs.next())
				hdbSdiCache.add(
					hsdi = new HdbSiteDatatype(sdi, DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2)));
			return hsdi;
		}
		catch(SQLException ex)
		{
			throw new DbIoException("getHSDI error in query '" + q + "': " + ex);
		}
		finally
		{
			dao.close();
		}
	}
	
	public void fillHdbSdiCache()
		throws DbIoException
	{
		String q = "select SITE_DATATYPE_ID, SITE_ID, DATATYPE_ID from HDB_SITE_DATATYPE";
		DaiBase dao = new DaoBase(this, "HDB");
		try
		{
			ResultSet rs = dao.doQuery(q);
			if(rs.next())
				hdbSdiCache.add(
					new HdbSiteDatatype(DbKey.createDbKey(rs, 1), 
						DbKey.createDbKey(rs, 2), DbKey.createDbKey(rs, 3)));
		}
		catch(SQLException ex)
		{
			throw new DbIoException("fillHdbSdiCache error in query '" + q + "': " + ex);
		}
		finally
		{
			dao.close();
		}
	}

//	/**
//	 * Used by the CpCompDependsUpdater daemon to enqueue tasklist records that
//	 * may have been missed in the latency between 1.) the trigger crating the
//	 * notify record and 2.) the updater adding the depends record.
//	 */
//	public void writeTasklistRecords(TimeSeriesIdentifier tsid, Date since)
//		throws NoSuchObjectException, DbIoException, BadTimeSeriesException
//	{
//		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
//		CTimeSeries ts = null;
//		try 
//		{
//			ts = timeSeriesDAO.makeTimeSeries(tsid);
//			timeSeriesDAO.fillTimeSeriesMetadata(ts);
//		}
//		finally
//		{
//			timeSeriesDAO.close();
//		}
//		
//		
//		String dataTable = ts.getTableSelector() + ts.getInterval();
//		String fields = "START_DATE_TIME, VALUE";
//		String where = "SITE_DATATYPE_ID = " + ts.getSDI() 
//			+ " AND DATE_TIME_LOADED >= " + sqlDate(since);
//		boolean isModeled = ts.getTableSelector().equalsIgnoreCase("M_");
//		String tables = dataTable;
//		if (!isModeled)
//			fields = fields + ", VALIDATION, DERIVATION_FLAGS";
//		else // modeled data has to join with REF_MODEL_RUN to get DATE_TIME_LOADED
//		{
//			tables = tables + ", REF_MODEL_RUN";
//			where = where + " AND " + dataTable 
//				+ ".MODEL_RUN_ID = REF_MODEL_RUN.MODEL_RUN_ID";
//		}
//		
//		String q = "select " + fields + " from " + tables
//			+ " where " + where;
//		
//		try
//		{
//			ResultSet rs = doQuery(q);
//			while (rs.next())
//			{
//				Date timeStamp = getFullDate(rs, 1);
//				double value = rs.getDouble(2);
//				String validation = isModeled ? "" : rs.getString(3);
//				char cval = (validation == null || validation.length() == 0) ? 
//					HdbFlags.HDB_BLANK_VALIDATION : validation.charAt(0);
//				String derivation = isModeled ? "" : rs.getString(4);
//				TimedVariable tv = new TimedVariable(value);
//				tv.setTime(timeStamp);
//				
//				tv.setFlags(HdbFlags.hdbDerivation2flag(derivation)
//					| HdbFlags.hdbValidation2flag(cval));
//				ts.addSample(tv);
//			}
//			debug1("Found " + ts.size() + " values.");
//		}
//		catch(SQLException ex)
//		{
//			String msg= "Error reading data with query '" + q
//				+ "': " + ex;
//			logger.warning(msg);
//			throw new DbIoException(msg);
//		}
//
//		HdbTsId hdbTsId = (HdbTsId)tsid;
//		
////		q = "insert into cp_comp_tasklist("
////		    + "record_num, loading_application_id,"
////		    + "site_datatype_id,interval,table_selector,"
////		    + "value,date_time_loaded,start_date_time,"
////		    + "delete_flag,model_run_id,validation,data_flags)"
////		    + " values (cp_tasklist_sequence.nextval, ?, "
////		    + "?, ?, ?,"
////		    + "?, sysdate, ?, 'N', ?, ?, ?)";
//
//		// Settable Arguments to prepared statement are:
//		// 1: LOADING_APPLICATION_ID
//		// 2: SITE_DATATYPE_ID
//		// 3: INTERVAL
//		// 4: TABLE_SELECTOR
//		// 5: VALUE
//		// 6: START_DATE_TIME
//		// 7: MODEL_RUN_ID (0 for real data)
//		// 8: VALIDATION
//		// 9: DATA_FLAGS
//
//		try
//		{
//			insertTasklist.setLong(1, appId.getValue());
//			insertTasklist.setLong(2, hdbTsId.getSdi().getValue());
//			insertTasklist.setString(3, hdbTsId.getInterval());
//			insertTasklist.setString(4, hdbTsId.getTableSelector());
//			insertTasklist.setInt(7, 
//				hdbTsId.getModelRunId() == Constants.undefinedIntKey ? 0 :
//				hdbTsId.getModelRunId());
//			for(int idx = 0; idx < ts.size(); idx++)
//			{
//				TimedVariable tv = ts.sampleAt(idx);
//				
//				double value = 0.0;
//				try { insertTasklist.setDouble(5, value = tv.getDoubleValue()); }
//				catch (NoConversionException ex)
//				{
//					warning("writeTasklistRecord with non-numeric tv: " + tv);
//					continue;
//				}
//				
//				insertTasklist.setDATE(6, 
//					((HdbOracleDateParser)oracleDateParser).toDATE(tv.getTime()));
//				insertTasklist.setString(8, "" + HdbFlags.flag2HdbValidation(tv.getFlags()));
//				insertTasklist.setString(9, HdbFlags.flag2HdbDerivation(tv.getFlags()));
//				debug1("inserting tasklist for " + hdbTsId.getUniqueString()
//					+ ", sdi=" + hdbTsId.getSdi() + ", tsKey=" + hdbTsId.getKey()
//					+ ", " + tv.getTime() + ": " + value);
//				
//				insertTasklist.execute();
//			}
//		}
//		catch(SQLException ex)
//		{
//			warning("writeTasklistRecord: Error in prepared statement: "
//				+ ex);
//		}
//	}

	public boolean isOracle()
	{
		return true;
	}

	@Override
	public TimeSeriesDAI makeTimeSeriesDAO()
	{
		return new HdbTimeSeriesDAO(this);
	}
	
	public OracleDateParser getOracleDateParser()
	{
		return oracleDateParser;
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new HdbIntervalDAO(this);
	}

	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		// Schedule Entries not supported in HDB
		return null;
	}

	@Override
	public String[] getValidPartChoices(String part)
	{
		if (part.equalsIgnoreCase(HdbTsId.TABSEL_PART))
			return new String[]{"R_", "M_"};
		return super.getValidPartChoices(part);
	}

	@Override
	public ComputationDAI makeComputationDAO()
	{
		return new HdbComputationDAO(this);
	}
	
	@Override
	public SiteDAI makeSiteDAO()
	{
		return new HdbSiteDAO(this);
	}

	@Override
	public GroupHelper makeGroupHelper()
	{
		return new HdbGroupHelper(this);
	}

	public ArrayList<HdbDataType> getHdbDataTypes()
	{
		if (hdbDataTypes == null)
		{
			hdbDataTypes = new ArrayList<HdbDataType>();
			String q = "select a.datatype_id, a.datatype_common_name, b.unit_common_name "
				+ "FROM HDB_DATATYPE a, HDB_UNIT b where a.unit_id = b.unit_id "
				+ "order by a.datatype_id";
			DaiBase dao = new DaoBase(this,"HDB");
			try
			{
				ResultSet rs = dao.doQuery(q);
				while(rs.next())
				{
					HdbDataType hdt = new HdbDataType(DbKey.createDbKey(rs, 1), 
						rs.getString(2), rs.getString(3));
					hdbDataTypes.add(hdt);
				}
			}
			catch (Exception ex)
			{
				warning("Cannot read HDB Data Types with query '" + q + ": " + ex);
			}
			finally
			{
				dao.close();
			}
		}
		return hdbDataTypes;
	}
	
	public HdbDataType getHdbDataType(DbKey dtid)
	{
		for(HdbDataType hdt : hdbDataTypes)
			if (hdt.getDataTypeId().equals(dtid))
				return hdt;
		return null;
	}
	
	public ArrayList<HdbObjectType> getHdbObjectTypes()
	{
		if (hdbObjectTypes == null)
		{
			hdbObjectTypes = new ArrayList<HdbObjectType>();
			String q = "select a.objecttype_id, a.objecttype_name, a.objecttype_tag, a.objecttype_parent_order "
				+ "FROM HDB_OBJECTTYPE a "
				+ "order by a.objecttype_id";
			DaiBase dao = new DaoBase(this,"HDB");
			try
			{
				ResultSet rs = dao.doQuery(q);
				while(rs.next())
				{
					HdbObjectType hot = new HdbObjectType(DbKey.createDbKey(rs, 1), 
						rs.getString(2), rs.getString(3), rs.getInt(4));
					hdbObjectTypes.add(hot);
				}
			}
			catch (Exception ex)
			{
				warning("Cannot read HDB Object Types with query '" + q + ": " + ex);
			}
			finally
			{
				dao.close();
			}
		}
		return hdbObjectTypes;
	}

	@Override
	public OracleDateParser makeOracleDateParser(TimeZone tz)
	{
		return new HdbOracleDateParser(tz);
	}

	@Override
	public String getStorageUnitsForDataType(DataType dt)
	{
		if (!dt.getStandard().equalsIgnoreCase(Constants.datatype_HDB))
			dt = dt.findEquivalent(Constants.datatype_HDB);
		if (dt == null)
			return null;
		
		for(HdbDataType hdt : getHdbDataTypes())
			if (hdt.getDataTypeId().equals(dt.getId()))
				return hdt.getUnitsAbbr();
		return null;
	}
	
	@Override
	public String flags2display(int flags)
	{
		return HdbFlags.flag2HdbDerivation(flags);
	}

}
