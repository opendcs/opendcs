package org.opendcs.odcsapi.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import ilex.var.TimedVariable;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiComputationRef;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;

import static java.util.stream.Collectors.toList;

public final class DTOMappers
{
	private DTOMappers()
	{
		throw new IllegalStateException("Utility class");
	}

	public static ApiTimeSeriesData dataMap(CTimeSeries cts, Date start, Date end)
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();
		ret.setTsid(mapTsId(cts.getTimeSeriesIdentifier()));
		ret.setValues(map(cts, start, end));
		return ret;
	}

	public static List<ApiTimeSeriesValue> map(CTimeSeries cts, Date start, Date end)
	{
		List<ApiTimeSeriesValue> ret = new ArrayList<>();
		Date current = start;
		TimedVariable tv = cts.findWithin(current, 5);
		if (tv != null && !tv.getTime().before(current))
		{
			current = processSample(tv, current, end, ret);
		}

		while (current.before(end) || current.equals(end))
		{
			TimedVariable value = cts.findNext(current);

			if (value == null)
			{
				break;
			}
			current = processSample(value, current, end, ret);
		}
		return ret;
	}

	public static Date processSample(TimedVariable value, Date current, Date end, List<ApiTimeSeriesValue> ret)
	{
		double val = Double.parseDouble(value.valueString());
		ApiTimeSeriesValue apiValue = new ApiTimeSeriesValue(value.getTime(), val, value.getFlags());
		ret.add(apiValue);
		if (current.equals(end))
		{
			return Date.from(end.toInstant().plusSeconds(1));
		}
		else
		{
			return value.getTime();
		}
	}

	public static Site mapSite(ApiSite apiSite) throws DatabaseException
	{
		Site site = new Site();
		site.setPublicName(apiSite.getPublicName());
		site.setLocationType(apiSite.getLocationType());
		site.setElevation(apiSite.getElevation());
		site.setElevationUnits(apiSite.getElevUnits());
		site.latitude = apiSite.getLatitude();
		site.longitude = apiSite.getLongitude();
		if (apiSite.getSiteId() != null)
		{
			site.setId(DbKey.createDbKey(apiSite.getSiteId()));
		}
		site.setLastModifyTime(Date.from(apiSite.getLastModified()));
		site.setDescription(apiSite.getDescription());
		site.timeZoneAbbr = apiSite.getTimezone();
		site.nearestCity = apiSite.getNearestCity();
		site.state = apiSite.getState();
		site.country = apiSite.getCountry();
		site.region = apiSite.getRegion();
		site.setActive(apiSite.isActive());

		for (String props : apiSite.getProperties().stringPropertyNames())
		{
			site.setProperty(props, apiSite.getProperties().getProperty(props));
		}
		return site;
	}

	public static ApiTimeSeriesIdentifier mapTsId(TimeSeriesIdentifier tsid)
	{
		if (tsid instanceof CwmsTsId cTsId)
		{
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(cTsId.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(cTsId.getUniqueString());
			ret.setDescription(cTsId.getDescription());
			ret.setStorageUnits(cTsId.getStorageUnits());
			ret.setActive(cTsId.isActive());
			return ret;
		}
		else
		{
			// Active flag is not set here because it is not part of the TimeSeriesIdentifier
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(tsid.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(tsid.getUniqueString());
			ret.setDescription(tsid.getDescription());
			ret.setStorageUnits(tsid.getStorageUnits());
			return ret;
		}
	}

	public static DbCompParm map(ApiCompParm parm) throws DatabaseException
	{
		if (parm == null)
		{
			return null;
		}
		DbCompParm ret = new DbCompParm(parm.getAlgoRoleName(),
				parm.getDataTypeId() != null ? DbKey.createDbKey(parm.getDataTypeId()) : DbKey.NullKey,
				parm.getInterval(), parm.getTableSelector(), parm.getDeltaT());
		if (parm.getDataTypeId() != null || parm.getDataType() != null)
		{
			String[] parts = parm.getDataType().split(":");
			DataType dt = new DataType(parts[0], parts[1]);
			if (parm.getDataTypeId() != null)
			{
				dt.setId(DbKey.createDbKey(parm.getDataTypeId()));
			}
			ret.setDataType(dt);
		}
		ret.setInterval(parm.getInterval());
		if (parm.getSiteId() != null)
		{
			Site site = new Site();
			site.setPublicName(parm.getSiteName());
			ret.setSite(site);
			ret.setSiteId(DbKey.createDbKey(parm.getSiteId()));
		}
		else
		{
			ret.setSiteId(DbKey.NullKey);
		}
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		ret.setAlgoParmType(parm.getAlgoParmType());
		ret.setRoleName(parm.getAlgoRoleName());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		ret.setDeltaTUnits(parm.getDeltaTUnits());
		if (parm.getModelId() != null)
		{
			ret.setModelId(parm.getModelId());
		}
		ret.setTableSelector(parm.getTableSelector());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		return ret;
	}

	public static ApiComputation map(DbComputation comp)
	{
		ApiComputation ret = new ApiComputation();
		if (comp.getId() != null)
		{
			ret.setComputationId(comp.getId().getValue());
		}
		else
		{
			ret.setComputationId(DbKey.NullKey.getValue());
		}
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithmId(comp.getAlgorithmId().getValue());
		}
		else
		{
			ret.setAlgorithmId(DbKey.NullKey.getValue());
		}
		ret.setComment(comp.getComment());
		if (comp.getAppId() != null)
		{
			ret.setAppId(comp.getAppId().getValue());
		}
		else
		{
			ret.setAppId(DbKey.NullKey.getValue());
		}
		ret.setEnabled(comp.isEnabled());
		ret.setEffectiveEndDate(comp.getValidEnd());
		ret.setEffectiveStartDate(comp.getValidStart());
		ret.setAlgorithmName(comp.getAlgorithmName());
		ret.setApplicationName(comp.getApplicationName());
		ret.setGroupName(comp.getGroupName());
		ret.setName(comp.getName());
		ret.setLastModified(comp.getLastModified());
		if (comp.getGroupId() != null)
		{
			ret.setGroupId(comp.getGroupId().getValue());
		}
		else
		{
			ret.setGroupId(DbKey.NullKey.getValue());
		}
		ret.setProps(comp.getProperties());
		ret.setParmList(new ArrayList<>(comp.getParmList()
				.stream()
				.map(DTOMappers::map)
				.collect(toList())));
		return ret;
	}

	public static ApiCompParm map(DbCompParm parm)
	{
		ApiCompParm ret = new ApiCompParm();
		if (parm.getDataType() != null)
		{
			ret.setDataType(parm.getDataType().getDisplayName());
		}
		ret.setInterval(parm.getInterval());
		if (parm.getSiteName() != null)
		{
			ret.setSiteName(parm.getSiteName().getDisplayName());
		}
		if (parm.getSiteId() != null)
		{
			ret.setSiteId(parm.getSiteId().getValue());
		}
		else
		{
			ret.setSiteId(DbKey.NullKey.getValue());
		}
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		ret.setAlgoParmType(parm.getAlgoParmType());
		ret.setAlgoRoleName(parm.getRoleName());
		ret.setDuration(parm.getDuration());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		if (parm.getDataTypeId() != null)
		{
			ret.setDataTypeId(parm.getDataTypeId().getValue());
		}
		else
		{
			ret.setDataTypeId(DbKey.NullKey.getValue());
		}
		ret.setDeltaTUnits(parm.getDeltaTUnits());
		ret.setVersion(parm.getVersion());
		ret.setModelId(parm.getModelId());
		ret.setTableSelector(parm.getTableSelector());
		ret.setParamType(parm.getParamType());
		return ret;
	}

	public static DbComputation map(ApiComputation comp) throws DatabaseException
	{
		DbComputation ret;
		if (comp.getComputationId() != null)
		{
			ret = new DbComputation(DbKey.createDbKey(comp.getComputationId()), comp.getName());
		}
		else
		{
			ret = new DbComputation(DbKey.NullKey, comp.getName());
		}
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithmId(DbKey.createDbKey(comp.getAlgorithmId()));
		}
		if (comp.getAppId() != null)
		{
			ret.setAppId(DbKey.createDbKey(comp.getAppId()));
		}
		ret.setComment(comp.getComment());
		ret.setEnabled(comp.isEnabled());
		ret.setValidEnd(comp.getEffectiveEndDate());
		ret.setValidStart(comp.getEffectiveStartDate());
		ret.setAlgorithmName(comp.getAlgorithmName());
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithm(new DbCompAlgorithm(DbKey.createDbKey(comp.getAlgorithmId()),
					comp.getAlgorithmName(), null, comp.getComment()));
		}
		ret.setApplicationName(comp.getApplicationName());
		ret.setGroup(new TsGroup().copy(comp.getGroupName()));
		ret.setLastModified(comp.getLastModified());
		if (comp.getGroupId() != null)
		{
			ret.setGroupId(DbKey.createDbKey(comp.getGroupId()));
		}
		else
		{
			ret.setGroupId(DbKey.NullKey);
		}
		for (String prop : comp.getProps().stringPropertyNames())
		{
			ret.setProperty(prop, comp.getProps().getProperty(prop));
		}
		for (ApiCompParm parm : comp.getParmList())
		{
			ret.addParm(DTOMappers.map(parm));
		}
		return ret;
	}

	static ApiComputationRef mapRef(DbComputation comp)
	{
		ApiComputationRef ref = new ApiComputationRef();
		ref.setComputationId(comp.getId().getValue());
		if (comp.getAlgorithmId() != null)
		{
			ref.setAlgorithmId(comp.getAlgorithmId().getValue());
		}
		else
		{
			ref.setAlgorithmId(DbKey.NullKey.getValue());
		}
		ref.setAlgorithmName(comp.getAlgorithmName());
		ref.setName(comp.getName());
		ref.setEnabled(comp.isEnabled());
		ref.setDescription(comp.getComment());
		ref.setProcessName(comp.getApplicationName());
		if (comp.getAppId() != null)
		{
			ref.setProcessId(comp.getAppId().getValue());
		}
		else
		{
			ref.setProcessId(DbKey.NullKey.getValue());
		}
		return ref;
	}


	public static <R> R map(Object obj, Class<R> klass) throws DatabaseException
	{
		Object ret = obj;
		if(obj instanceof ApiCompParm apiParm)
		{
			ret = map(apiParm);
		}
		else if(obj instanceof ApiSite apiSite)
		{
			ret = mapSite(apiSite);
		}
		else if (obj instanceof TimeSeriesIdentifier tsId)
		{
			ret = mapTsId(tsId);
		}
		else if (obj instanceof DbComputation dbComp
				&& klass.equals(ApiComputationRef.class))
		{
			ret = mapRef(dbComp);
		}
		else if (obj instanceof DbComputation dbComp)
		{
			ret = map(dbComp);
		}
		else if (obj instanceof ApiComputation apiComp)
		{
			ret = map(apiComp);
		}
		return klass.cast(ret);
	}
}
