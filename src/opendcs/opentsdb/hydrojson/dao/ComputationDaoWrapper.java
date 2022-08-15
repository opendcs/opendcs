package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.CompFilter;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.compedit.ComputationInList;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import opendcs.dai.SiteDAI;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.Computation;
import opendcs.opentsdb.hydrojson.beans.ComputationRef;
import opendcs.opentsdb.hydrojson.beans.CompParm;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;
import opendcs.dao.ComputationDAO;
import opendcs.dao.DatabaseConnectionOwner;

public class ComputationDaoWrapper
	extends ComputationDAO
{
	SiteDAI siteDao = null;

	public ComputationDaoWrapper(DatabaseConnectionOwner tsdb)
	{
		super(tsdb);
		this.siteDao = tsdb.makeSiteDAO();
		this.module = "ComputationDaoWrapper";
		// Have base class initialize the db connection & make siteDAO subordinate
		siteDao.setManualConnection(this.getConnection());
	}

	public void close()
	{
		super.close();
		siteDao.close();
	}

	public ArrayList<ComputationRef> getComputationRefs(
		String site, String algorithm, String datatype, String group,
		String process, Boolean enabled, String interval)
		throws DbIoException, WebAppException
	{
		// Create a filter with the arguments
		CompFilter filter = new CompFilter();

		if (site != null)
		{
			DbKey siteId = siteDao.lookupSiteID(site);
			if (!DbKey.isNull(siteId))
				filter.setSiteId(siteId);
		}
		
		if (algorithm != null)
		{
			try
			{
				DbKey algoId = algorithmDAO.getAlgorithmId(algorithm);
				if (!DbKey.isNull(algoId))
					filter.setAlgoId(algoId);
			}
			catch (NoSuchObjectException e)
			{
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No such algorithm '" + algorithm + "'");
			}
		}
		
		if (datatype != null)
		{
			try
			{
				DataType dt = dataTypeDAO.lookupDataType(datatype);
				if (dt != null && !DbKey.isNull(dt.getId()))
					filter.setDataTypeId(dt.getId());
			}
			catch (NoSuchObjectException e)
			{
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No such datatype '" + datatype + "'");
			}
		}
		
		if (group != null)
		{
			String q = "select GROUP_ID from TSDB_GROUP"
				+ " where lower(GROUP_NAME) = " + sqlString(group);
			ResultSet rs = doQuery(q);
			try
			{
				if (rs.next())
					filter.setGroupId(DbKey.createDbKey(rs, 1));
			}
			catch(SQLException ex)
			{
				throw new DbIoException(module + " Error in query '" + q + "': " + ex);
			}
		}
		
		if (process != null)
		{
			try
			{
				filter.setProcessId(loadingAppDAO.lookupAppId(process));
			}
			catch (NoSuchObjectException e)
			{
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No such datatype '" + datatype + "'");
			}
		}
		
		if (enabled != null)
			filter.setEnabledOnly(enabled);
		if (interval != null)
			filter.setIntervalCode(interval);
		
		ArrayList<ComputationRef> ret = new ArrayList<ComputationRef>();
		ArrayList<ComputationInList> cils = compEditList(filter);
		if (cils != null)
			for(ComputationInList cil : cils)
				ret.add(new ComputationRef(cil));
	
		return ret;
	}
	
	public Computation getComputation(long id)
		throws DbIoException, WebAppException
	{
		// Force read to come from the database and not the cache.
		compCache.clear();
		
		try
		{
			return dbComp2apiComp(getComputationById(DbKey.createDbKey(id)));
		}
		catch (NoSuchObjectException e)
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
				"No such computation with id=" + id);
		}
	}
	
	public Computation writeComputation(Computation comp) 
		throws DbIoException, WebAppException
	{
		DbComputation dbComp = apiComp2dbComp(comp);
		writeComputation(dbComp);
		
		// A new key may have been allocated and the lmt updated.
		// Also, ensure that table selector is parsed consistently.
		return getComputation(dbComp.getKey().getValue());
	}
	
	private Computation dbComp2apiComp(DbComputation dbComp)
	{
		Computation ret = new Computation();

		ret.setComputationId(dbComp.getId().getValue());
		ret.setName(dbComp.getName());
		ret.setComment(dbComp.getComment());
		if (dbComp.getAppId() != null)
			ret.setAppId(dbComp.getAppId().getValue());
		ret.setApplicationName(dbComp.getApplicationName());
		ret.setLastModified(dbComp.getLastModified());
		ret.setEnabled(dbComp.isEnabled());

		ret.setEffectiveStartDate(dbComp.getValidStart());
		if (ret.getEffectiveStartDate() != null)
			ret.setEffectiveStartType("Calendar");
		else
		{
			String s = dbComp.getProperty("EffectiveStart");
			if (s == null)
				ret.setEffectiveStartType("No Limit");
			else
			{
				int idx = s.indexOf('-');
				if (idx >= 0)
				{
					ret.setEffectiveStartInterval(s.substring(idx+1).trim());
					ret.setEffectiveStartType("Now -");
				}
				else
					ret.setEffectiveStartType("No Limit");
			}
		}
		
		ret.setEffectiveEndDate(dbComp.getValidEnd());
		if (ret.getEffectiveEndDate() != null)
			ret.setEffectiveEndType("Calendar");
		else
		{
			String s = dbComp.getProperty("EffectiveEnd");
			if (s == null)
				ret.setEffectiveEndType("No Limit");
			else if (s.equalsIgnoreCase("now"))
				ret.setEffectiveEndType("Now");
			else
			{
				int idx = s.indexOf('-');
				if (idx > 0)
				{
					ret.setEffectiveEndType("Now -");
					ret.setEffectiveEndInterval(s.substring(idx+1).trim());
				}
				else if ((idx = s.indexOf('+')) > 0)
				{
					ret.setEffectiveEndType("Now +");
					ret.setEffectiveEndInterval(s.substring(idx+1).trim());
				}
			}
		}

		if (dbComp.getAlgorithmId() != null)
			ret.setAlgorithmId(dbComp.getAlgorithmId().getValue());
		ret.setAlgorithmName(dbComp.getAlgorithmName());
		
		for(DbCompParm dcp : dbComp.getParmList())
			ret.getParmList().add(dbCompParm2apiCompParm(dcp));
		
		for(String propName : dbComp.getProperties().stringPropertyNames())
		{
			String propValue = dbComp.getProperties().getProperty(propName);
			
			if (propName.toLowerCase().endsWith("_eu"))
			{
				String parmName = propName.substring(0, propName.length()-3);
				CompParm cp = ret.findParm(parmName);
				if (cp != null)
					cp.setUnitsAbbr(propValue);
			}
			else if (propName.toLowerCase().endsWith("_missing"))
			{
				String parmName = propName.substring(0, propName.length()-8);
				CompParm cp = ret.findParm(parmName);
				if (cp != null)
					cp.setIfMissing(propValue);
			}
			else if (!propName.equalsIgnoreCase("effectivestart") 
				&& !propName.equalsIgnoreCase("effectiveend"))
				ret.getProps().setProperty(propName, propValue);
		}
		
		if (dbComp.getGroupId() != null)
			ret.setGroupId(dbComp.getGroupId().getValue());
		ret.setGroupName(dbComp.getGroupName());

		return ret;
	}
	
	private CompParm dbCompParm2apiCompParm(DbCompParm dcp)
	{
		CompParm ret = new CompParm();
		ret.setAlgoParmType(dcp.getAlgoParmType());
		ret.setAlgoRoleName(dcp.getRoleName());
		
		if (!DbKey.isNull(dcp.getDataTypeId()))
		{
			ret.setDataTypeId(dcp.getDataTypeId().getValue());
			if (dcp.getDataType() != null)
				ret.setDataType(dcp.getDataType().getCode());
		}
		else if (db.isCwms() || db.isOpenTSDB())
		{
			// could be a partial param spec with wildcard
			if (dcp.getParamSpec() != null)
				ret.setDataType(dcp.getParamSpec());
		}
		
		ret.setInterval(dcp.getInterval());
		ret.setDeltaT(dcp.getDeltaT());
		ret.setDeltaTUnits(dcp.getDeltaTUnits());
		ret.setUnitsAbbr(dcp.getUnitsAbbr());
		
		if (!DbKey.isNull(dcp.getSiteId()))
		{
			ret.setSiteId(dcp.getSiteId().getValue());
			if (dcp.getSiteName() != null)
				ret.setSiteName(dcp.getSiteName().getNameValue());
		}
		else if (db.isCwms() || db.isOpenTSDB())
		{
			// could be a partial location spec with wildcard
			if (dcp.getLocSpec() != null)
				ret.setSiteName(dcp.getLocSpec());
		}
		
		if (db.isHdb())
		{
			ret.setTableSelector(dcp.getTableSelector());
			if (dcp.getModelId() != -1)
				ret.setModelId(dcp.getModelId());
		}
		ret.setParamType(dcp.getParamType());
		ret.setDuration(dcp.getDuration());
		ret.setVersion(dcp.getVersion());
		ret.setTsKey(DbKey.isNull(dcp.getSiteDataTypeId()) ? null :
			dcp.getSiteDataTypeId().getValue());
		return ret;
	}
	
	private DbComputation apiComp2dbComp(Computation comp)
	{
		DbKey key = comp.getComputationId() == null ? DbKey.NullKey : 
			DbKey.createDbKey(comp.getComputationId());
		DbComputation dbComp = new DbComputation(key, comp.getName());
		
		dbComp.setComment(comp.getComment());
		dbComp.setAppId(comp.getAppId() == null ? DbKey.NullKey 
			: DbKey.createDbKey(comp.getAppId()));
		dbComp.setApplicationName(comp.getApplicationName());
		dbComp.setEnabled(comp.isEnabled());
		dbComp.setAlgorithmId(comp.getAlgorithmId() == null ? DbKey.NullKey :
			DbKey.createDbKey(comp.getAlgorithmId()));
		dbComp.setAlgorithmName(comp.getAlgorithmName());
		PropertiesUtil.copyProps(dbComp.getProperties(), comp.getProps());
		dbComp.setGroupId(comp.getGroupId() == null ? DbKey.NullKey :
			DbKey.createDbKey(comp.getGroupId()));

		// Get _EU and _missing props from parms
		for (CompParm cp : comp.getParmList())
			dbComp.addParm(apiCompParm2dbCompParm(cp, dbComp));
		
		dbComp.setLastModified(comp.getLastModified());
		
		dbComp.setValidStart(comp.getEffectiveStartDate());
		if (dbComp.getValidStart() == null)
		{
			if (TextUtil.strEqualIgnoreCase(comp.getEffectiveStartType(), "Now -"))
				dbComp.setProperty("EffectiveStart", "now - " + comp.getEffectiveStartInterval());
		}
		dbComp.setValidEnd(comp.getEffectiveEndDate());
		if (dbComp.getValidEnd() == null)
		{
			if (TextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now"))
				dbComp.setProperty("EffectiveEnd", "now");
			else if (TextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now -"))
				dbComp.setProperty("EffectiveEnd", "now - " + comp.getEffectiveEndInterval());
			else if (TextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now +"))
				dbComp.setProperty("EffectiveEnd", "now + " + comp.getEffectiveEndInterval());
			
		}
		
		return dbComp;
	}
	
	private DbCompParm apiCompParm2dbCompParm(CompParm cp, DbComputation dbComp)
	{
		DbCompParm dcp = new DbCompParm(cp.getAlgoRoleName(),
			cp.getTsKey() == null ? DbKey.NullKey : DbKey.createDbKey(cp.getTsKey()),
			cp.getInterval(), cp.getTableSelector(), cp.getDeltaT());
		if (cp.getDataTypeId() != null)
			dcp.setDataTypeId(DbKey.createDbKey(cp.getDataTypeId()));
		if (cp.getDeltaTUnits() != null)
			dcp.setDeltaTUnits(cp.getDeltaTUnits());
		if (cp.getSiteId() != null)
			dcp.setSiteId(DbKey.createDbKey(cp.getSiteId()));
		
		if (db.isHdb())
		{
			dcp.setTableSelector(cp.getTableSelector());
			if (cp.getModelId() != null)
				dcp.setModelId(cp.getModelId());
		}
		else // Either CWMS or OpenTSDB
		{
			String parmType = cp.getParamType() == null ? "" : cp.getParamType();
			String duration = cp.getDuration() == null ? "" : cp.getDuration();
			String version = cp.getVersion() == null ? "" : cp.getVersion();
			
			String tabsel = parmType + "." + duration + "." + version;
			
			String location = null, parm = null;
			if (cp.getSiteId() == null && cp.getSiteName() != null)
				location = cp.getSiteName();
			if (cp.getDataTypeId() == null && cp.getDataType() != null)
				parm = cp.getDataType();
			
			if (location != null || parm != null)
				tabsel = tabsel + "." + (location==null ? "" : location)
					+ "." + (parm==null ? "" : parm);
			
			dcp.setTableSelector(tabsel);
		}
		
		if (cp.getUnitsAbbr() != null)
			dbComp.getProperties().setProperty(cp.getAlgoRoleName() + "_EU", cp.getUnitsAbbr());
		
		if (cp.getIfMissing() != null)
			dbComp.getProperties().setProperty(cp.getAlgoRoleName() + "_MISSING", cp.getIfMissing());

		return dcp;
	}
}
