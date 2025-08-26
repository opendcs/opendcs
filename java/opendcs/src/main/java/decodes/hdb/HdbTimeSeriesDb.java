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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.ComputationDAI;
import opendcs.dai.DaiBase;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import oracle.jdbc.OraclePreparedStatement;
import ilex.util.TextUtil;
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
public class HdbTimeSeriesDb extends TimeSeriesDb
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private OracleDateParser oracleDateParser = null;

	private OraclePreparedStatement  insertTasklist = null;
	private HdbSdiCache hdbSdiCache = new HdbSdiCache();

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
					log.atError()
					   .setCause(ex)
					   .log("Connection using OS authentication failed. Will attempt username/password auth.");
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
				+ "' for user '" + username + "'", ex);
		}

		keyGenerator = new OracleSequenceHDBGenerator();

		String q = null;

		try (Statement st = conn.createStatement())
		{

			q = "SELECT PARAM_VALUE FROM REF_DB_PARAMETER WHERE PARAM_NAME = 'TIME_ZONE'";

			try (ResultSet rs = st.executeQuery(q))
			{
				if (rs != null && rs.next())
				{
					databaseTimezone = rs.getString(1);
					DecodesSettings.instance().sqlTimeZone = databaseTimezone;
				}
			}
			catch(SQLException ex)
			{
				databaseTimezone = DecodesSettings.instance().sqlTimeZone;
				log.atInfo()
				   .setCause(ex)
				   .log(" -- failed, using sqlTimeZone setting of '{}'", databaseTimezone);
			}
			conn.unwrap(oracle.jdbc.OracleConnection.class).setSessionTimeZone(databaseTimezone);

			// Hard-code date & timestamp format for reads. Always use GMT.
			q = "ALTER SESSION SET TIME_ZONE = '" + databaseTimezone + "'";
			log.info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_date_format = 'yyyy-mm-dd hh24:mi:ss'";
			log.info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_timestamp_format = 'yyyy-mm-dd hh24:mi:ss'";
			log.info(q);
			st.execute(q);

			q = "ALTER SESSION SET nls_timestamp_tz_format = 'yyyy-mm-dd hh24:mi:ss'";
			log.info(q);
			st.execute(q);

			// MJM 2018-2/21 Force autoCommit on.
			try { conn.setAutoCommit(true);}
			catch(SQLException ex)
			{
				log.atWarn().setCause(ex).log("Cannot set SQL AutoCommit to true.");
			}

			writeDateFmt = new SimpleDateFormat(
				"'to_date'(''dd-MMM-yyyy HH:mm:ss''',' '''DD-MON-YYYY HH24:MI:SS''')");
			writeDateFmt.setTimeZone(TimeZone.getTimeZone(databaseTimezone));
			log.info("Set date fmt to time zone '{}' current time={}",
					 databaseTimezone, writeDateFmt.format(new Date()));
		}
		catch(SQLException ex)
		{
			log.atError().setCause(ex).log("Error in '{}' -- will proceed anyway.", q);
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
			log.atWarn().setCause(ex).log("Cannot prepare statement '{}'", q);
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


		if (!DbKey.isNull(sdi))
		{
			HdbSiteDatatype hsdi = getHSDI(sdi);
			if (hsdi == null)
				throw new NoSuchObjectException("No such site-datatype with SDI=" + sdi);
			siteId = hsdi.getSiteId();
			datatypeId = hsdi.getDatatypeId();
		}
		parm.setSite(getSiteById(siteId));

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
				log.atWarn().setCause(ex).log("Invalid tsidstr '{}'", tsidstr);
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
			log.atWarn().setCause(ex).log("lookupSDI invalid HDB dataTypeCode '{}'", dataTypeCode);
		}

		HdbSiteDatatype hsdi = hdbSdiCache.get(siteId, datatypeId);
		if (hsdi != null)
			return hsdi.getSdi();

		String q = "select SITE_DATATYPE_ID from HDB_SITE_DATATYPE"
			+ " where SITE_ID = " + siteId
			+ " and DATATYPE_ID = " + dataTypeCode;
		DaiBase dao = new DaoBase(this, "HDB");
		try (ResultSet rs = dao.doQuery(q))
		{
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
			try(ResultSet rs2 = dao.doQuery(q))
			{
				if (rs.next())
				{
					hsdi = new HdbSiteDatatype(DbKey.createDbKey(rs, 1), siteId, datatypeId);
					hdbSdiCache.add(hsdi);
					return hsdi.getSdi();
				}
			}
			return Constants.undefinedId;
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Error getting SDI from siteId={}, dtCode='{}'", siteId, dataTypeCode);
			return DbKey.NullKey;
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
		try (ResultSet rs = dao.doQuery(q))
		{
			if (rs.next())
			{
				r = rs.getInt(1);
				if (rs.wasNull())
					r = Constants.undefinedIntKey;
			}
		}
		catch(SQLException ex)
		{
			log.atWarn().setCause(ex).log("unable to findMaxModelRunId.");
			r = Constants.undefinedIntKey;
		}
		finally
		{
			dao.close();
		}
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
		try (ResultSet rs = dao.doQuery(q))
		{
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
			String msg = "Cannot find coefficient for interval " + interval + " sdi " + sdi;
			throw new DbIoException(msg, ex);
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

		try (ResultSet rs = dao.doQuery(q))
		{
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
			log.atWarn().setCause(ex).log("Error reading datatypes for siteId={}", siteId);
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
	public TimeSeriesIdentifier transformTsidByCompParm(TimeSeriesDAI timeSeriesDAO,
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
			log.trace("HdbTimeSeriesDb.transformTsid new string='{}'", uniqueString);

			try
			{
				tsidRet = (HdbTsId)timeSeriesDAO.getTimeSeriesIdentifier(uniqueString);
				log.trace("HdbTimeSeriesDb.transformTsid time series '{}' exists previously.", uniqueString);
			}
			catch(NoSuchObjectException ex)
			{
				log.atTrace()
				   .setCause(ex)
				   .log("transformTsidByCompParm: No existing TS for '{}'", uniqueString);
				if (createTS)
				{
					log.trace("Will attempt to create.");
					if (timeSeriesDisplayName != null)
						tsidRet.setDisplayName(timeSeriesDisplayName);
					timeSeriesDAO.createTimeSeries(tsidRet);
				}
				else
				{
					log.trace("transformTsid no such time series '{}' and createFlag=false", uniqueString);
					return null;
				}
			}
		}
		else
		{
			tsidRet = (HdbTsId)tsid;
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
				log.atTrace().setCause(ex).log("transformUniqueString error in expandSDI");
			}
		}

		SiteName sn = parm.getSiteName();
		if (sn == null && !DbKey.isNull(parm.getSiteId()))
			sn = new SiteName(null, "HDB", "" + parm.getSiteId());

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
		return transformed;
	}


	/**
	 * @return a full date, including time information.
	 */
	public Date getFullDate(ResultSet rs, int column)
	{
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
		try (ResultSet rs = dao.doQuery(q))
		{
			if(rs.next())
				hdbSdiCache.add(
					hsdi = new HdbSiteDatatype(sdi, DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2)));
			return hsdi;
		}
		catch(SQLException ex)
		{
			throw new DbIoException("getHSDI error in query '" + q + "'", ex);
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
		try (ResultSet rs = dao.doQuery(q))
		{
			if(rs.next())
				hdbSdiCache.add(
					new HdbSiteDatatype(DbKey.createDbKey(rs, 1),
						DbKey.createDbKey(rs, 2), DbKey.createDbKey(rs, 3)));
		}
		catch(SQLException ex)
		{
			throw new DbIoException("fillHdbSdiCache error in query '" + q + "'", ex);
		}
		finally
		{
			dao.close();
		}
	}

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
			try (ResultSet rs = dao.doQuery(q))
			{
				while(rs.next())
				{
					HdbDataType hdt = new HdbDataType(DbKey.createDbKey(rs, 1),
						rs.getString(2), rs.getString(3));
					hdbDataTypes.add(hdt);
				}
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot read HDB Data Types with query '{}'", q);
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
			try (ResultSet rs = dao.doQuery(q))
			{
				while(rs.next())
				{
					HdbObjectType hot = new HdbObjectType(DbKey.createDbKey(rs, 1),
						rs.getString(2), rs.getString(3), rs.getInt(4));
					hdbObjectTypes.add(hot);
				}
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot read HDB Object Types with query '{}'", q);
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
