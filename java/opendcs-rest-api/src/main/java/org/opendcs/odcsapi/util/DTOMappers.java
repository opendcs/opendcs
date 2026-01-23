package org.opendcs.odcsapi.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import ilex.var.TimedVariable;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;

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

	public static ApiTimeSeriesIdentifier mapTsId(TimeSeriesIdentifier tsid)
	{
		if (tsid instanceof CwmsTsId)
		{
			CwmsTsId cTsId = (CwmsTsId)tsid;
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
}
