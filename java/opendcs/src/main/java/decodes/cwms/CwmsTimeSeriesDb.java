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
*
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*/
package decodes.cwms;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.GregorianCalendar;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;

import org.opendcs.database.ExceptionHelpers;
import org.opendcs.utils.FailableResult;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.DaiBase;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DaoHelper;

import java.sql.PreparedStatement;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;
import hec.lang.Const;
import ilex.util.PropertiesUtil;
import ilex.var.NamedVariable;
import ilex.var.Variable;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.ScreeningDAO;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;

/**
This is the base class for the time-series database implementation.
Sub classes must override all the abstract methods and provide
a mechanism to persistently store time series and computational meta
data.
*/
public class CwmsTimeSeriesDb extends TimeSeriesDb
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private final String dbOfficeId;
	private final DbKey dbOfficeCode;

	private String[] currentlyUsedVersions = { "" };
	GregorianCalendar saveTsCal = new GregorianCalendar(
		TimeZone.getTimeZone("UTC"));

	CwmsGroupHelper cwmsGroupHelper = null;

	public boolean requireCcpTables = true;

	private String dbUri = null;

	private BaseParam baseParam = new BaseParam();
	private PreparedStatement getMinStmt = null, getTaskListStmt;
	String getMinStmtQuery = null, getTaskListStmtQuery = null;

	/**
	 * Constructs a CwmsTimeSeriesDb object with the specified parameters.
	 *
	 * @param appName the name of the application using the database
	 * @param dataSource the javax.sql.DataSource object for the database connection
	 * @param settings the DecodesSettings object containing database settings
	 * @throws DatabaseException if there is an issue with the database connection
	 */
	public CwmsTimeSeriesDb(String appName, javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException
	{
		super(appName, dataSource, settings);

		Site.explicitList = true;
		this.dbOfficeId = settings.CwmsOfficeId;

		// CWMS uses ts_code as a unique identifier of a time-series
		// Internally our SDI (site datatype id) is equivalent to CWMS ts_code
		sdiIsUnique = true;

		curTimeName = "sysdate";
		maxCompRetryTimeFrmt = "%d*1/24";
		module = "CwmsTimeSeriesDb";
		try (Connection conn = this.getConnection();
			 DaoHelper dao = new DaoHelper(this, appName, conn);
		)
		{
			this.dbOfficeCode = dao.getSingleResult("select office_code from cwms_v_office where office_id = ?", rs -> DbKey.createDbKey(rs, 1), dbOfficeId);
			baseParam.load(this);
		}
		catch (DbIoException | SQLException ex)
		{
			throw new DatabaseException("Failed to load base parameters", ex);
		}
	}

	public static Connection getDbConnection(final CwmsConnectionInfo info) throws BadConnectException
	{
		try
		{
			Connection conn = CwmsConnectionPool.getPoolFor(info).getConnection();
			return conn;
		}
		catch(SQLException ex)
		{
			throw new BadConnectException("Unable to get connection from pool",ex);
		}
	}

	public void setParmSDI(DbCompParm parm, DbKey siteId, String dtcode)
		throws DbIoException, NoSuchObjectException
	{
		log.trace("setParmSDI siteId={}, dtcode={}", siteId, dtcode);

		DataType dt = null;
		DataTypeDAI dataTypeDao = makeDataTypeDAO();
		try 
		{ 
			try { dataTypeDao.lookupDataType(dtcode); }
			catch(NoSuchObjectException ex)
			{
				// This combo of CWMS Param-SubParam doesn't exist yet in the
				// database as a 'datatype' object. Create it.
				dt = DataType.getDataType(Constants.datatype_CWMS, dtcode);
				dataTypeDao.writeDataType(dt);
			}

			String q = 
				"SELECT TS_CODE, LOCATION_ID FROM CWMS_V_TS_ID "
				+ "WHERE LOCATION_CODE = " + siteId
				+ " AND TS_ACTIVE_FLAG = 'T'"
				+ " AND upper(PARAMETER_ID) = "
						+ sqlString(dtcode.toUpperCase())
				+ " AND INTERVAL_ID = " + sqlString(parm.getInterval())
				+ " AND VERSION_ID = " + sqlString(parm.getVersion())
				+ " AND DURATION_ID = " + sqlString(parm.getDuration())
				+ " AND PARAMETER_TYPE_ID = " + sqlString(parm.getParamType());
			// Don't need to select on office id. It is implied by location code.

			ResultSet rs = dataTypeDao.doQuery(q);
			if (rs != null && rs.next())
			{
				DbKey sdi = DbKey.createDbKey(rs, 1);
				parm.setSiteDataTypeId(sdi);
				parm.addSiteName(new SiteName(null, Constants.snt_CWMS,
					rs.getString(2)));
			}
			else
				throw new NoSuchObjectException(
					"No Time Series with specified identifiers.");
			Site site = this.getSiteById(siteId);
			if (site != null)
				for(Iterator<SiteName> snit = site.getNames(); snit.hasNext(); )
					parm.addSiteName(snit.next());
		}
		catch(SQLException ex)
		{
			throw new DbIoException("setParmSDI: ", ex);
		}
		finally
		{
			dataTypeDao.close();
		}
	}

	@Override
	public TimeSeriesIdentifier expandSDI(DbCompParm parm)
		throws DbIoException, NoSuchObjectException
	{
		DbKey sdi = parm.getSiteDataTypeId();
		DbKey siteId = parm.getSiteId();
		DbKey datatypeId = parm.getDataTypeId();

		TimeSeriesDAI timeSeriesDAO = makeTimeSeriesDAO();
		TimeSeriesIdentifier tsid = null;
		try
		{
			if (!DbKey.isNull(sdi))
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(parm.getSiteDataTypeId());
				parm.setSite(tsid.getSite());
				parm.setDataType(tsid.getDataType());
			}
			else
			{
				if (!DbKey.isNull(siteId))
					parm.setSite(this.getSiteById(siteId));
				if (!DbKey.isNull(datatypeId))
					parm.setDataType(DataType.getDataType(datatypeId));
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}

		return tsid;
	}

	/**
	 * CWMS TSDB stores ParamType.Duration.Version in the tab selector.
	 */
	public String getTableSelectorLabel()
	{
		return "Type.Dur.Version";
	}

	/**
	 * For CWMS we show all 5 path components for the site.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public ArrayList<String[]> getDataTypesForSite(DbKey siteId, DaiBase dao)
		throws DbIoException
	{
		String header[] = new String[5];
		header[0] = "Param";
		header[1] = "Param Type";
		header[2] = "Interval";
		header[3] = "Duration";
		header[4] = "Version";

		ArrayList<String[]> ret = new ArrayList<String[]>();
		ret.add(header);

		String q = "SELECT PARAMETER_ID, PARAMETER_TYPE_ID, INTERVAL_ID, "
			+ "DURATION_ID, VERSION_ID "
			+ "FROM CWMS_V_TS_ID "
			+ " where location_code = " + siteId
			+ " order by PARAMETER_ID, PARAMETER_TYPE_ID, INTERVAL_ID";
		try
		{
			ResultSet rs = dao.doQuery(q);
			while(rs.next())
			{
				String dtl[] = new String[5];
				for(int i=0; i<5; i++)
					dtl[i] = rs.getString(i+1);
				ret.add(dtl);
			}
		}
		catch(SQLException ex)
		{
			log.atWarn().setCause(ex).log("Error reading Time series types for Location Code={}", siteId);
		}
		return ret;
	}

	/**
	 * Validate the passed information to make sure it represents a valid
	 * parameter within this database. If not, throw ConstraintException.
	 */
	public void validateParm(DbKey siteId, String dtcode, String interval,
		String tabSel, int modelId)
		throws ConstraintException, DbIoException
	{
	}

	/** @return label to use for 'limit' column in tables. */
	public String getLimitLabel() { return "Qual Code"; }

	@Override
	public String flags2LimitCodes(int flags)
	{
		StringBuilder sb = new StringBuilder();
		if ((flags & CwmsFlags.SCREENED) != 0)
		{
			sb.append('S');
			if ((flags & CwmsFlags.VALIDITY_MISSING) != 0)
				sb.append('M');
			if ((flags & CwmsFlags.VALIDITY_REJECTED) != 0)
				sb.append('R');
			if ((flags & CwmsFlags.VALIDITY_QUESTIONABLE) != 0)
				sb.append('Q');
			if ((flags & CwmsFlags.PROTECTED) != 0)
				sb.append('P');
		}
		return sb.toString();
	}

	/** @return label to use for 'revision' column in tables. */
	public String getRevisionLabel() { return "Qual Code(hex)"; }

	@Override
	public String flags2RevisionCodes(int flags)
	{
		
		return String.format("%x", CwmsFlags.flag2CwmsQualityCode(flags));
	}
	
	public boolean isCwms() { return true; }

	@Override
	public String[] getTsIdParts()
	{
		return CwmsTsId.tsIdParts;
	}

	@Override
	public TimeSeriesIdentifier transformTsidByCompParm(TimeSeriesDAI tsDAI,
			TimeSeriesIdentifier tsid, DbCompParm parm, boolean createTS,
			boolean fillInParm, String timeSeriesDisplayName)
		throws DbIoException, NoSuchObjectException, BadTimeSeriesException
	{
		if (tsid == null)
		{
			tsid = makeEmptyTsId();
		}

		String origString = tsid.getUniqueString();
		TimeSeriesIdentifier tsidRet = tsid.copyNoKey();
		boolean transformed = transformUniqueString(tsidRet, parm);

		if (transformed)
		{
			String uniqueString = tsidRet.getUniqueString();
			log.trace("CwmsTimeSeriesDb.transformTsid origString='{}', new string='{}', parm={}", origString, uniqueString, parm);
						
			FailableResult<TimeSeriesIdentifier,TsdbException> tmpTsId = tsDAI.findTimeSeriesIdentifier(uniqueString);

			if (tmpTsId.isFailure())
			{
				if (tmpTsId.getFailure() instanceof NoSuchObjectException)
				{
					if (createTS)
					{
						tsDAI.createTimeSeries(tsidRet);
						fillInParm = true;
					}
					else
					{
						log.trace("CwmsTimeSeriesDb.transformTsid no such time series '{}'", uniqueString);
						return null;
					}
				}
				else
				{
					ExceptionHelpers.throwDbIoNoSuchObject(tmpTsId.getFailure());
				}
			}
			else
			{
				log.trace("CwmsTimeSeriesDb.transformTsid time series '{}' exists OK.", uniqueString);
				tsidRet = tmpTsId.getSuccess();
			}
		}
		else
		{
			tsidRet = tsid;
		}

		if (timeSeriesDisplayName != null)
		{
			tsidRet.setDisplayName(timeSeriesDisplayName);
		}

		if (fillInParm)
		{
			parm.setSiteDataTypeId(tsidRet.getKey());
			parm.setInterval(tsidRet.getInterval());
			parm.setTableSelector(
				tsidRet.getPart("ParamType") + "."
				+ tsidRet.getPart("Duration") + "."
				+ tsidRet.getPart("Version"));
			parm.setDataType(tsidRet.getDataType());
			parm.setSite(tsidRet.getSite());
		}

		return tsidRet;
	}

	/**
	 * Overloaded from base class, transform the TSID unique string.
	 * @param tsidRet the time-series id to transform
	 * @param parm the templeat db comp parameter
	 * @return true if changes were made.
	 */
	public boolean transformUniqueString(TimeSeriesIdentifier tsidRet,
		DbCompParm parm)
	{
		boolean transformed = false;
		if (!(tsidRet instanceof CwmsTsId))
			return false;
		CwmsTsId ctsid = (CwmsTsId) tsidRet;

		SiteName sn = parm.getSiteName();
		if (sn != null)
		{
			tsidRet.setSiteName(sn.getNameValue());
			transformed = true;
			if (sn.site != null)
				tsidRet.setSite(sn.site);
			else
			{
				// Also lookup the site and set the ID and site object.
				SiteDAI siteDAO = makeSiteDAO();
				try
				{

					DbKey siteId = siteDAO.lookupSiteID(sn);
					tsidRet.setSite(siteDAO.getSiteById(siteId));
				}
				catch (Exception ex)
				{
					log.atWarn().setCause(ex).log("Cannot get site for sitename {}", sn);
				}
				finally
				{
					siteDAO.close();
				}
			}
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getLocSpec() != null && parm.getLocSpec().length() > 0)
		{
			String morphed = morph(ctsid.getSiteName(), parm.getLocSpec());
			log.trace("TSID site name '{}' with loc spec '{}' morphed to '{}'",
					  ctsid.getSiteName(), parm.getLocSpec(), morphed + "'");
			if (morphed == null)
				morphed = parm.getLocSpec();
			tsidRet.setSite(null);
			tsidRet.setSiteName("");
			SiteDAI siteDAO = makeSiteDAO();
			try
			{
				DbKey siteId = siteDAO.lookupSiteID(morphed);
				if (!DbKey.isNull(siteId))
				{
					tsidRet.setSite(siteDAO.getSiteById(siteId));
					tsidRet.setSiteName(morphed);
				}
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot get site for sitename {}", morphed);
			}
			finally
			{
				siteDAO.close();
			}
			transformed = true;
		}
		DataType dt = parm.getDataType();
		if (dt != null)
		{
			tsidRet.setDataType(dt);
			transformed = true;
		}
		else if (this.tsdbVersion >= TsdbDatabaseVersion.VERSION_14
			  && parm.getParamSpec() != null && parm.getParamSpec().length() > 0)
		{
			String morphed = morph(ctsid.getPart("param"), parm.getParamSpec());
			if (morphed == null)
				log.trace("Unable to morph param '{}' with param spec '{}'",
						  ctsid.getPart("param"), parm.getParamSpec());
			else
			{
				tsidRet.setDataType(null);
				tsidRet.setDataType(DataType.getDataType(Constants.datatype_CWMS, morphed));
				transformed = true;
			}
		}

		String s = parm.getParamType();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("ParamType", s);
			transformed = true;
		}
		s = parm.getInterval();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Interval", s);
			transformed = true;
		}
		s = parm.getDuration();
		if (s != null && s.trim().length() > 0)
		{
			tsidRet.setPart("Duration", s);
			transformed = true;
		}
		s = parm.getVersion();
		if (s != null && s.trim().length() > 0)
		{
			if (s.contains("*"))
			{
				String morphed = morph(ctsid.getPart("version"), s);
				if (morphed == null)
					log.trace("Unable to morph param '{}' with version spec '{}'",
							  ctsid.getPart("version"), s);
				else
				{
					ctsid.setVersion(morphed);
					transformed = true;
				}
			}
			else
				tsidRet.setPart("Version", s);
			transformed = true;
		}
		return transformed;
	}

	/**
	 * For version 6.3, morph the tsid part by the computation param part.
	 * @param tsidComponent The component from the TSID
	 * @param parmComponent The component in the comp parm, which may contain wildcards.
	 * @return the tsid component masked by the parm component, or null if can't match.
	 */
	public static String morph(String tsidComponent, String parmComponent)
	{
		// Examples:
		// tsid: A-B-C   parm: D-*-F   result: D-B-F
		// tsid: A-B     parm: *-E-F   result: A-E-F
		// tsid: A-B-C   parm: D-*     result: D-B-C
		// tsid: A       parm: D-*     result: null
		// tsid: A-B-C   parm: *-D     result: A-D
		// tsid: A-B     parm: *-      result: A

		// Check for a partial location specification (OpenDCS 6.3)
		String tps[] = tsidComponent.split("-");
		String pps[] = parmComponent.split("-");
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < pps.length; )
		{
			if (pps[idx].equals("*"))
			{
				if (idx >= tps.length)
					return null;
				else
				{
					// A trailing asterisk in the mask means copy in rest of tsid.
					// However a trailing hyphen means lop off the rest of tsid.
					if (idx == pps.length - 1
					 && !parmComponent.endsWith("-"))
					{
						for(int tidx = idx; tidx < tps.length; tidx++)
							sb.append(tps[tidx] + (tidx < tps.length-1 ? "-" : ""));
					}
					else
						sb.append(tps[idx]);
				}
			}
			else
				sb.append(pps[idx]);
			if (++idx < pps.length)
				sb.append("-");
		}
		return sb.toString();

		/*
		 * Note: the table_selector in cp_comp_ts_parm will be empty for a component that is
		 * completely undefined. The syntax is ParamType.Duration.Version[.SiteSpec.ParmSpec],
		 * So "Total.1Hour." means that Version is undefined and shows as <var> in the gui.
		 * This is different from "Total.1Hour.Something-*". Meaning that the first part of
		 * the subversion can be anything.
		 */
	}


	public String getDbOfficeId()
	{
		return dbOfficeId;
	}

	/**
	 * @return the parameter types as String[]
	 */
	@Override
	public String[] getParamTypes() throws DbIoException
	{
		String retStr[];

		try
		{
			ArrayList<String> ret = listParamTypes();
			retStr = new String[ret.size()];
			for (int i=0; i<ret.size(); i++)
				retStr[i] = ret.get(i);
		} catch(DbIoException ex)
		{
			retStr = null;
			throw new DbIoException("CwmsTimeSeriesDb.getParamTypes: ", ex);
		}
		return retStr;
	}

	@Override
	public ArrayList<String> listParamTypes()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		String q = "select distinct parameter_type_id FROM CWMS_V_TS_ID"
			+ " WHERE upper(DB_OFFICE_ID) = " + sqlString(dbOfficeId.toUpperCase());

		DaiBase dao = new DaoBase(this, "CWMSDB");
		try
		{
			ResultSet rs = dao.doQuery(q);
			while (rs != null && rs.next())
				ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("CwmsTimeSeriesDb.listParamTypes: ", ex);
		}
		finally
		{
			dao.close();
		}

		// MJM - these are the ones we know about for sure:
		if (!ret.contains("Inst"))
			ret.add("Inst");
		if (!ret.contains("Ave"))
			ret.add("Ave");
		if (!ret.contains("Max"))
			ret.add("Max");
		if (!ret.contains("Min"))
			ret.add("Min");
		if (!ret.contains("Total"))
			ret.add("Total");
		return ret;

	}

	@Override
	public String[] getValidPartChoices(String part)
	{
		if (part.equalsIgnoreCase("version"))
			return currentlyUsedVersions;
		return super.getValidPartChoices(part);
	}

	@Override
	public TimeSeriesIdentifier makeEmptyTsId()
	{
		return new CwmsTsId();
	}


	
	/**
	 * Use database-specific flag definitions to determine whether the
	 * passed variable should be considered 'questionable'.
	 * @param v the variable whose flags to check
	 * @return true if flags are questionable, false if okay.
	 */
	public boolean isQuestionable(NamedVariable v)
	{
		return (v.getFlags() & CwmsFlags.VALIDITY_MASK) == CwmsFlags.VALIDITY_QUESTIONABLE;
	}

	/**
	 * Use database-specific flag definitions to set the passed variable
	 * as 'questionable'.
	 * @param v the variable whose flags to set
	 */
	public void setQuestionable(Variable v)
	{
		v.setFlags( (v.getFlags() & (~CwmsFlags.VALIDITY_MASK)) | CwmsFlags.VALIDITY_QUESTIONABLE);
	}

	public boolean isOracle()
	{
		return true;
	}

	@Override
	public SiteDAI makeSiteDAO()
	{
		return new CwmsSiteDAO(this, dbOfficeId);
	}

	@Override
	public TimeSeriesDAI makeTimeSeriesDAO()
	{
		return new CwmsTimeSeriesDAO(this, dbOfficeId);
	}

	@Override
	public IntervalDAI makeIntervalDAO()
	{
		return new CwmsIntervalDAO(this, dbOfficeId);
	}

	@Override
	public ScheduleEntryDAI makeScheduleEntryDAO()
	{
		return null;
	}	

	public DbKey getDbOfficeCode()
	{
		return dbOfficeCode;
	}

	public BaseParam getBaseParam()
	{
		return baseParam;
	}

	@Override
	public ScreeningDAI makeScreeningDAO()
		throws DbIoException
	{
		return new ScreeningDAO(this);
	}

	@Override
	public GroupHelper makeGroupHelper()
	{
		return new CwmsGroupHelper(this);
	}

	@Override
	public double rating(String specId, Date timeStamp, double... indeps)
		throws DbCompException, RangeException
	{
		// int nIndeps = indeps.length;
		// NOTE: indeps is already an array of doubles. I can pass
		// it directly to the rateOne function.
		
		String action = "reading rating";
		try (Connection tc = getConnection();
			CwmsRatingDao crd = new CwmsRatingDao(this))
		{
			crd.setManualConnection(tc);
			RatingSet ratingSet = crd.getRatingSet(specId);
			action = "rateOne";
			double d = ratingSet.rateOne(tc, timeStamp.getTime(), indeps);
			
			if (d == Const.UNDEFINED_DOUBLE)
			{
				StringBuilder sb = new StringBuilder();
				for(double x : indeps)
				{
					sb.append(x + ",");
				}
				sb.deleteCharAt(sb.length()-1);
				String msg = "Input values (" + sb.toString() + ") outside rating range.";
				log.warn(msg);
				throw new RangeException(msg);
			}
			return d;
		}
		catch (RatingException ex)
		{
			String msg = "Error while " + action + ", specId=" + specId;
			throw new RangeException(msg, ex);
		}
		catch (SQLException ex)
		{
			throw new DbCompException("Error during database operations.", ex);
		}
	}

	@Override
	public ArrayList<String> listVersions()
		throws DbIoException
	{
		ArrayList<String> ret = new ArrayList<String>();
		String q = "select distinct version_id from cwms_v_ts_id order by version_id";

		DaiBase dao = new DaoBase(this, "CWMS");
		try
		{
			ResultSet rs = dao.doQuery(q);
			while (rs != null && rs.next())
					ret.add(rs.getString(1));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("CwmsTimeSeriesDb.listVersions", ex);
		}
		finally
		{
			dao.close();
		}

		return ret;
	}

	@Override
	public String getStorageUnitsForDataType(DataType dt)
	{
		String cwmsParam = null;
		if (dt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS))
			cwmsParam = dt.getCode();
		else
		{
			DataType equiv = dt.findEquivalent(Constants.datatype_CWMS);
			if (equiv == null)
				return null;
			cwmsParam = equiv.getCode();
		}

		// Truncate to just base param
		int hyphen = cwmsParam.indexOf('-');
		if (hyphen > 0)
			cwmsParam = cwmsParam.substring(0, hyphen);
		return baseParam.getStoreUnits4Param(cwmsParam);
	}

	@Override
	public String flags2display(int flags)
	{
		return CwmsFlags.flags2Display(flags);
	}

	@Override
	public Connection getConnection()
	{
		try
		{
			return dataSource.getConnection();
		}
		catch(SQLException ex)
		{
			throw new RuntimeException("Error retrieving connection.",ex);
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
			log.atWarn().setCause(ex).log("Unable to close returned connection");
		}
	}

	@Override
	public void postConInit(Connection conn) throws SQLException
	{
		readVersionInfo(this, conn);

		// CWMS OPENDCS-16 for DB version >= 68, use old OracleSequenceKeyGenerator,
		// which assumes a separate sequence for each table. Do not use CWMS_SEQ for anything.
		int decodesDbVersion = getDecodesDatabaseVersion();
		log.info("decodesDbVersion={}", decodesDbVersion);
		keyGenerator = decodesDbVersion >= DecodesDatabaseVersion.DECODES_DB_68 ?
				new OracleSequenceKeyGenerator() :
				new CwmsSequenceKeyGenerator(decodesDbVersion);
			TimeSeriesDb.readVersionInfo(this, conn);

		String q = null;
		try(Statement st = conn.createStatement();)
		{
			// Hard-code date & timestamp format for reads. Always use GMT.
			q = "ALTER SESSION SET TIME_ZONE = 'GMT'";
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

			log.info("DECODES IF Connected to TSDB Version {}", tsdbVersion);
		}
		catch(SQLException ex)
		{
			log.atError().setCause(ex).log("Error in '{}' -- will proceed anyway. ", q);
		}

		// CWMS-8979 Allow settings in the database to override values in user.properties.
		String settingsApp = System.getProperty("SETTINGS");
		if (settingsApp != null)
		{
			log.info("SqlDatabaseIO Overriding Decodes Settings with properties in "
				+ "Process Record '{}'", settingsApp);

			try (LoadingAppDAI loadingAppDAO = makeLoadingAppDAO())
			{
				CompAppInfo cai = loadingAppDAO.getComputationApp(settingsApp);
				PropertiesUtil.loadFromProps(DecodesSettings.instance(), cai.getProperties());
			}
			catch (DbIoException ex)
			{
				log.atWarn().setCause(ex).log("Cannot load settings from app '{}'", settingsApp);
			}
			catch (NoSuchObjectException ex)
			{
				log.atWarn().setCause(ex).log("Cannot load settings from non-existent app '{}'", settingsApp);
			}
		}
	}	
	/**
     * Given a unique time-series identifier string, make a CTimeSeries
     * object, populated with meta-data from the database.
     * @param tsidStr the unique string identifying this time series.
     * @return The CTimeSeries object
     * @throws DbIoException on database I/O error
     * @throws NoSuchObjectException if no such time series exists in the database.
     */
	@Override
    public CTimeSeries makeTimeSeries(String tsidStr)
        throws DbIoException, NoSuchObjectException
    {
        try (TimeSeriesDAI timeSeriesDAO = this.makeTimeSeriesDAO())
        {
            FailableResult<TimeSeriesIdentifier,TsdbException> tsid = timeSeriesDAO.findTimeSeriesIdentifier(tsidStr, true);
			if (tsid.isSuccess())
			{
				TimeSeriesIdentifier tsId = tsid.getSuccess();
				// There is an odd situation that happens were a TimeSeries ID has been loaded, but never had the storage units attached.
				// Several downstream components depend on the storage units being present and this ensures that the value is available.
				// This is likely due to the more aggressive cache usages that CWMS is using to speed up various operations.
				if (tsId.getStorageUnits() == null )
				{
					tsId.setStorageUnits(this.getStorageUnitsForDataType(tsId.getDataType()));
				}
				return makeTimeSeries(tsId);
			}
            else
			{
				return ExceptionHelpers.throwDbIoNoSuchObject(tsid.getFailure());
			}
        }
    }
	
}
