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

package org.opendcs.odcsapi.opendcs_dep;

import java.util.ArrayList;

import org.opendcs.odcsapi.beans.ApiCompTestRequest;
import org.opendcs.odcsapi.beans.ApiCompTestResults;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.beans.ApiTsGroupRef;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiCompParmData;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbCompResolver;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.DuplicateTimeSeriesException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.VarFlags;

import java.util.logging.Level;
import java.util.logging.Logger;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.DataTypeDAI;
import opendcs.dai.EnumDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.TsGroupDAO;

public class CompRunner
{
	private static final String module = "CompRunner";
	private static long lastDecodesInitMsec = 0L;
	
	public synchronized ArrayList<ApiTimeSeriesIdentifier> 
		resolveCompInputs(ApiComputation apiComp, DbInterface dbi)
		throws DbException, WebAppException
	{
		ArrayList<ApiTimeSeriesIdentifier> ret = new ArrayList<ApiTimeSeriesIdentifier>();
		
		TimeSeriesDb tsdb = TsdbManager.makeTsdb(dbi);
		initDecodes(tsdb);
		
		try(AlgorithmDAI algoDao = tsdb.makeAlgorithmDAO();
			TimeSeriesDAI tsDao = tsdb.makeTimeSeriesDAO();
			TsGroupDAI grpDao = tsdb.makeTsGroupDAO()
			)
		{
			// Convert comp to OpenDCS DbComputation object
			DbComputation dbComp = apiComp2dbComp(apiComp);
			DbCompAlgorithm dbAlgo = algoDao.getAlgorithmById(dbComp.getAlgorithmId());

			// If this is a SINGLE (not group) computation, 
			// All inputs should be completely specified. Just add them to ret.
			if (DbKey.isNull(dbComp.getGroupId()))
			{
				for(DbCompParm compParm : dbComp.getParmList())
				{
					DbAlgoParm algoParm = dbAlgo.getParm(compParm.getRoleName());
					if (algoParm.getParmType().startsWith("i"))
					{
						TimeSeriesIdentifier tsid = tsDao.getTimeSeriesIdentifier(compParm.getSiteDataTypeId());
						ret.add(new ApiTimeSeriesIdentifier(tsid.getUniqueString(), tsid.getKey().getValue(),
							tsid.getDescription(), tsid.getStorageUnits()));
					}
				}
				return ret;
			}
			// Else this is a a group comp, evaluate group and expand.
			TsGroup grp = grpDao.getTsGroupById(dbComp.getGroupId());
			GroupHelper grpHelper = tsdb.makeGroupHelper();
			grpHelper.expandTsGroup(grp);
			
			// Find the 1st input parm
			DbCompParm firstInput = null;
			for(DbCompParm compParm : dbComp.getParmList())
			{
				DbAlgoParm algoParm = dbAlgo.getParm(compParm.getRoleName());
				if (algoParm.getParmType().startsWith("i"))
				{
					firstInput = compParm;
					break;
				}
			}
			if (firstInput == null)
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "Computation algorithm has no input parms.");
			
			// Morph each TSID in group by 1st input parm and add to ret.
			for(TimeSeriesIdentifier tsid : grp.getExpandedList())
			{
				try
				{
					tsid = tsdb.transformTsidByCompParm(tsid, firstInput, false, false, "");
					ret.add(new ApiTimeSeriesIdentifier(tsid.getUniqueString(), tsid.getKey().getValue(),
							tsid.getDescription(), tsid.getStorageUnits()));
				}
				catch (BadTimeSeriesException e)
				{
					// Can't happen because create flag is false.
					continue;
				}
			}
		}
		catch (DbIoException | NoSuchObjectException ex)
		{
			String msg = module + ".resolveCompInputs error from tsdb interface: " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}
	
	/**
	 * Synchronized to make sure multiple concurrent sessions don't init at the same time.
	 * Also, only initialize once per minute.
	 * @param tsdb
	 * @throws DbException
	 */
	public static synchronized void initDecodes(TimeSeriesDb tsdb)
		throws DbException
	{
		// Don't init more than once per minute.
		long now = System.currentTimeMillis();
		if (now - lastDecodesInitMsec < 60*1000L)
			return;
		lastDecodesInitMsec = now;
		
		if (decodes.db.Database.getDb() == null)
			decodes.db.Database.setDb(new decodes.db.Database());
		
		String action = "creating DAOs";
		try (DataTypeDAI dtDAO = tsdb.makeDataTypeDAO();
			EnumDAI enumDAO = tsdb.makeEnumDAO()
			)
		{
			action = "loading enum list";
			enumDAO.readEnumList(decodes.db.Database.getDb().enumList);
			action = "loading data types";
			dtDAO.readDataTypeSet(decodes.db.Database.getDb().dataTypeSet);
		}
		catch (DbIoException ex)
		{
			String msg = module + ".initDecodes error while " + action + ": " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public ApiCompTestResults testComp(ApiCompTestRequest req, DbInterface dbi)
		throws DbException, WebAppException
	{
		ApiCompTestResults ret = new ApiCompTestResults();
		TraceLogger traceLogger = null;
		ilex.util.Logger origLogger = ilex.util.Logger.instance();
		
		if (req.isTraceOutput())
		{
			traceLogger = new TraceLogger(ret.getLogMessages());
			traceLogger.setMinLogPriority(ilex.util.Logger.E_DEBUG3);
			ilex.util.Logger.setLogger(traceLogger);
		}
		
		TimeSeriesDb tsdb = TsdbManager.makeTsdb(dbi);


		try(AlgorithmDAI algoDao = tsdb.makeAlgorithmDAO();
			TimeSeriesDAI tsDao = tsdb.makeTimeSeriesDAO();
			TsGroupDAI grpDao = tsdb.makeTsGroupDAO()
			)
		{
			// Create a new DataCollection populated with the input values
			DataCollection compData = new DataCollection();
			TimeSeriesIdentifier tsid = tsDao.getTimeSeriesIdentifier(DbKey.createDbKey(req.getTsid().getKey()));
			CTimeSeries triggerTs = tsDao.makeTimeSeries(tsid);
			compData.addTimeSeries(triggerTs);
			int n = tsDao.fillTimeSeries(triggerTs, req.getSince(), req.getUntil());
			for (int pos = 0; pos < triggerTs.size(); pos++)
				VarFlags.setWasAdded(triggerTs.sampleAt(pos));

			Logger.getLogger(ApiConstants.loggerName).info(module + ".testComp: " + n + " input values retrieved.");
			
			// Convert comp and algo to OpenDCS objects
			DbComputation dbComp = apiComp2dbComp(req.getComputation());
			dbComp.setAlgorithm(algoDao.getAlgorithmById(dbComp.getAlgorithmId()));

			// If it's a group comp, make it concrete using the triggering tsid.
			if (!DbKey.isNull(dbComp.getGroupId()))
				dbComp = DbCompResolver.makeConcrete(tsdb, tsid, dbComp, true);
			
			// Fill in any other fully-defined inputs.
			Logger.getLogger(ApiConstants.loggerName).info("Filling other input data...");
			for(DbCompParm dcp : dbComp.getParmList())
			{
				DbAlgoParm dap = dbComp.getAlgorithm().getParm(dcp.getRoleName());
				if (dap != null)
					dcp.setAlgoParmType(dap.getParmType());
				
Logger.getLogger(ApiConstants.loggerName).info("   parm '" + dcp.getRoleName() + "' sdi=" + dcp.getSiteDataTypeId() + ", isInput=" + dcp.isInput()
+", algoParmType=" + dcp.getAlgoParmType());
				// Parm IS assigned a time series and it's not the trigger TS that we already filled.
				if (dcp.isInput() && !DbKey.isNull(dcp.getSiteDataTypeId()) 
					&& (dcp.getSiteDataTypeId().getValue() != req.getTsid().getKey()))
				{
					CTimeSeries ts = new CTimeSeries(dcp);
					tsDao.fillTimeSeries(ts, req.getSince(), req.getUntil());
					compData.addTimeSeries(ts);
				}
			}
			
			// Prepare and execute the computation against the DataCollection
			dbComp.prepareForExec(tsdb);
			dbComp.apply(compData, tsdb);
			
			// Build and return results.
		nextParm:
			for(DbCompParm dcp : dbComp.getParmList())
				if (!DbKey.isNull(dcp.getSiteDataTypeId()))
					// This parm was used. Find the time series in the data collection.
					for(CTimeSeries cts : compData.getAllTimeSeries())
						if (dcp.getSiteDataTypeId().equals(cts.getSDI()))
						{
							ApiCompParmData acpd = new ApiCompParmData();
							acpd.setAlgoRoleName(dcp.getRoleName());
							acpd.setParmData(cts2apits(cts));
							ret.getCompParmData().add(acpd);
							continue nextParm;
						}
			
			return ret;
		}
		catch(NoSuchObjectException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, 
				"Specified triggering time series does not exist in the database: " + ex);
		}
		catch (BadTimeSeriesException ex)
		{
			Logger.getLogger(ApiConstants.loggerName).log(Level.WARNING, "Error in RawMessageBlockParser: %s", ex);
			throw new WebAppException(500, String.format("Error in RawMessageBlockParser: %s", ex));
		}
		catch (DuplicateTimeSeriesException ex)
		{
			Logger.getLogger(ApiConstants.loggerName).log(Level.WARNING,
					"Unexpected DuplicateTimeSeriesException: %s", ex);
			throw new WebAppException(500, String.format("Unexpected DuplicateTimeSeriesException: %s", ex));
		}
		catch (DbCompException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "Error in computation exec: " + ex);
		}
		catch (DbIoException ex)
		{
			Logger.getLogger(ApiConstants.loggerName).log(Level.WARNING,
					"testComp error from tsdb interface: %s", ex);
			throw new DbException(module, ex, "testComp error from tsdb interface");
		}
		finally
		{
			if (traceLogger != null)
				ilex.util.Logger.setLogger(origLogger);
		}
	}
	
	private static ApiTimeSeriesData cts2apits(CTimeSeries cts)
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();
		ret.setTsid(new ApiTimeSeriesIdentifier(cts.getTimeSeriesIdentifier().getUniqueString(),
			cts.getSDI().getValue(), cts.getBriefDescription(), cts.getUnitsAbbr()));
		for(int idx=0; idx < cts.size(); idx++)
		{
			ilex.var.TimedVariable tv = cts.sampleAt(idx);
			try { ret.getValues().add(new ApiTimeSeriesValue(tv.getTime(), tv.getDoubleValue(), tv.getFlags())); }
			catch(Exception ex) {}
		}
		
		return ret;
	}
	
	public DbComputation apiComp2dbComp(ApiComputation comp)
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
		ApiPropertiesUtil.copyProps(dbComp.getProperties(), comp.getProps());
		dbComp.setGroupId(comp.getGroupId() == null ? DbKey.NullKey :
			DbKey.createDbKey(comp.getGroupId()));

		// Get _EU and _missing props from parms
		for (ApiCompParm cp : comp.getParmList())
			dbComp.addParm(apiCompParm2dbCompParm(cp, dbComp));
		
		dbComp.setLastModified(comp.getLastModified());
		
		dbComp.setValidStart(comp.getEffectiveStartDate());
		if ((dbComp.getValidStart() == null) && (ApiTextUtil.strEqualIgnoreCase(comp.getEffectiveStartType(), "Now -")))
		{
			dbComp.setProperty("EffectiveStart", "now - " + comp.getEffectiveStartInterval());
		}
		dbComp.setValidEnd(comp.getEffectiveEndDate());
		if (dbComp.getValidEnd() == null)
		{
			if (ApiTextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now"))
				dbComp.setProperty("EffectiveEnd", "now");
			else if (ApiTextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now -"))
				dbComp.setProperty("EffectiveEnd", "now - " + comp.getEffectiveEndInterval());
			else if (ApiTextUtil.strEqualIgnoreCase(comp.getEffectiveEndType(), "now +"))
				dbComp.setProperty("EffectiveEnd", "now + " + comp.getEffectiveEndInterval());
			
		}
		
		return dbComp;
	}

	private DbCompParm apiCompParm2dbCompParm(ApiCompParm cp, DbComputation dbComp)
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
		
		if (DbInterface.isHdb)
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
	
	public ArrayList<ApiTimeSeriesIdentifier> evalGroup(ApiTsGroup apiGrp, DbInterface dbi) 
		throws DbException
	{
		TimeSeriesDb tsdb = TsdbManager.makeTsdb(dbi);
		ArrayList<ApiTimeSeriesIdentifier> ret = new ArrayList<ApiTimeSeriesIdentifier>();

		try(TsGroupDAI tsdbGrpDao = tsdb.makeTsGroupDAO();
			)
		{
			// Force DB read to bypass any cached version.
			TsGroup tsGrp = apiGroup2TsdbGroup(apiGrp, tsdb, tsdbGrpDao);
			ArrayList<TimeSeriesIdentifier> tsdbTsids = tsdb.expandTsGroup(tsGrp);
			
			// convert tsids back to api beans.
			for(TimeSeriesIdentifier tsdbTsid : tsdbTsids)
			{
				// convert to Api TSID and add to return data
				ApiTimeSeriesIdentifier apiTsid = new ApiTimeSeriesIdentifier();
				apiTsid.setKey(tsdbTsid.getKey().getValue());
				apiTsid.setUniqueString(tsdbTsid.getUniqueString());
				apiTsid.setDescription(tsdbTsid.getDescription());
				apiTsid.setStorageUnits(tsdbTsid.getStorageUnits());

				ret.add(apiTsid);
			}
			return ret;
		}
		catch (DbIoException ex)
		{
			throw new DbException(module + ".evalGroup", ex, "Error expanding group '" + apiGrp.getGroupName() + "': " + ex);
		}
	}
	
	private TsGroup apiGroup2TsdbGroup(ApiTsGroup apiGrp, TimeSeriesDb tsdb, TsGroupDAI tsdbGrpDao)
		throws DbException
	{
		initDecodes(tsdb);
		
		TsGroup ret = new TsGroup();
		ret.setTransient();
		ret.setGroupId(DbKey.createDbKey(
			apiGrp.getGroupId() == null ? DbKey.NullKey.getValue() : apiGrp.getGroupId()));
		ret.setGroupName(apiGrp.getGroupName());
		ret.setGroupType(apiGrp.getGroupType());
		ret.setDescription(apiGrp.getDescription());

		for(ApiSiteRef asr : apiGrp.getGroupSites())
			ret.addSiteId(DbKey.createDbKey(asr.getSiteId()));
		
		for(ApiDataType adt : apiGrp.getGroupDataTypes())
			ret.addDataTypeId(DbKey.createDbKey(adt.getId()));
		

		for(String attr : apiGrp.getGroupAttrs())
		{
			int eq = attr.indexOf('=');
			if (eq < 0 || eq == attr.length()-1)
				continue;
			ret.addOtherMember(attr.substring(0, eq), attr.substring(eq+1));
		}
		
		try(TimeSeriesDAI tsDao = tsdb.makeTimeSeriesDAO())
		{
			// Calling the list method will fill the cache needed to expand.
			ArrayList<TimeSeriesIdentifier> tsids = tsDao.listTimeSeries();
			Logger.getLogger(ApiConstants.loggerName).info("There are "
				+ tsids.size() + " TSIDs in the cache.");
			for(ApiTimeSeriesIdentifier atsid : apiGrp.getTsIds())
			{
				ret.addTsMemberID(atsid.getUniqueString());
				try
				{
					ret.addTsMember(tsDao.getTimeSeriesIdentifier(DbKey.createDbKey(atsid.getKey())));
				}
				catch (NoSuchObjectException ex)
				{
					Logger.getLogger(ApiConstants.loggerName).warning(
						module + ".apiGroup2TdbGroup: error getting tsid for '" 
						+ atsid.getUniqueString() + "' with id=" + atsid.getKey() + ": " + ex);
				}
			}

			for(ApiTsGroupRef gr : apiGrp.getIncludeGroups())
				ret.getIncludedSubGroups().add(tsdbGrpDao.getTsGroupById(
					DbKey.createDbKey(gr.getGroupId())));
			for(ApiTsGroupRef gr : apiGrp.getExcludeGroups())
				ret.getExcludedSubGroups().add(tsdbGrpDao.getTsGroupById(
					DbKey.createDbKey(gr.getGroupId())));
			for(ApiTsGroupRef gr : apiGrp.getIntersectGroups())
				ret.getIntersectedGroups().add(tsdbGrpDao.getTsGroupById(
					DbKey.createDbKey(gr.getGroupId())));
		}
		catch(DbIoException ex)
		{
			throw new DbException(module + ".apiGroup2TsdbGroup", ex, "Error reading sub group: " + ex);
		}
		
		return ret;

	}
}
