/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import decodes.cwms.CwmsTsId;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import ilex.var.TimedVariable;
import opendcs.dai.IntervalDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.opentsdb.Interval;
import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesSpec;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.beans.ApiTsGroupRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

import ilex.util.IDateFormat;

/**
 * HTTP resources relating to Time Series data and descriptors
 * @author mmaloney
 *
 */
@Path("/")
public final class TimeSeriesResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getTimeSeriesRefs(@QueryParam("active") Boolean activeOnly) throws DbException
	{
		boolean filterActive = activeOnly != null && activeOnly;

		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		try (TimeSeriesDAI dai = tsdb.makeTimeSeriesDAO())
		{
			List<ApiTimeSeriesIdentifier> tsIds = idMap(dai.listTimeSeries());
			List<ApiTimeSeriesIdentifier> returnList = new ArrayList<>();
			for (ApiTimeSeriesIdentifier tsId : tsIds)
			{
				if (!filterActive || tsId.isActive())
				{
					returnList.add(tsId);
				}
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(returnList)
					.build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series", ex);
		}
	}

	static List<ApiTimeSeriesIdentifier> idMap(List<TimeSeriesIdentifier> identifiers)
	{
		List<ApiTimeSeriesIdentifier> ret = new ArrayList<>();
		for(TimeSeriesIdentifier id : identifiers)
		{
			if (id instanceof CwmsTsId)
			{
				CwmsTsId ctsid = (CwmsTsId)id;
				ApiTimeSeriesIdentifier apiId = new ApiTimeSeriesIdentifier();
				if (id.getKey() != null)
				{
					apiId.setKey(id.getKey().getValue());
				}
				else
				{
					apiId.setKey(DbKey.NullKey.getValue());
				}
				apiId.setActive(ctsid.isActive());
				apiId.setDescription(id.getDescription());
				apiId.setStorageUnits(id.getStorageUnits());
				apiId.setUniqueString(id.getUniqueString());
				ret.add(apiId);
			} else {
				ApiTimeSeriesIdentifier apiId = new ApiTimeSeriesIdentifier();
				if (id.getKey() != null)
				{
					apiId.setKey(id.getKey().getValue());
				}
				else
				{
					apiId.setKey(DbKey.NullKey.getValue());
				}
				apiId.setDescription(id.getDescription());
				apiId.setStorageUnits(id.getStorageUnits());
				apiId.setUniqueString(id.getUniqueString());
				ret.add(apiId);
			}
		}
		return ret;
	}

	@GET
	@Path("tsspec")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response getTimeSeriesSpec(@QueryParam("key") Long tsKey) throws WebAppException, DbException
	{
		if (tsKey == null)
		{
			throw new MissingParameterException("Missing required tskey parameter.");
		}

		try (TimeSeriesDAI dai = getLegacyTimeseriesDB().makeTimeSeriesDAO())
		{
			TimeSeriesIdentifier identifier = dai.getTimeSeriesIdentifier(DbKey.createDbKey(tsKey));
			ApiTimeSeriesSpec spec = specMap(identifier);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(spec).build();
		}
		catch (NoSuchObjectException e)
		{
			throw new DatabaseItemNotFoundException(String.format("Time series with key: %dnot found", tsKey));
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series spec", ex);
		}
	}

	static ApiTimeSeriesSpec specMap(TimeSeriesIdentifier id)
	{
		ApiTimeSeriesSpec ret = new ApiTimeSeriesSpec();
		ApiTimeSeriesIdentifier tsId = map(id);
		ret.setTsid(tsId);
		if (id instanceof CwmsTsId)
		{
			CwmsTsId ctsid = (CwmsTsId)id;
			ret.setActive(ctsid.isActive());
			ret.setInterval(ctsid.getInterval());
			ret.setDuration(ctsid.getDuration());
			ret.setVersion((ctsid).getVersion());
			if (ctsid.getDataTypeId() != null)
			{
				ret.setDatatypeId(ctsid.getDataTypeId().getValue());
			}
			else
			{
				ret.setDatatypeId(DbKey.NullKey.getValue());
			}
			if (ctsid.getSite() != null && ctsid.getSite().getId() != null)
			{
				ret.setSiteId(ctsid.getSite().getId().getValue());
			}
			else
			{
				ret.setSiteId(DbKey.NullKey.getValue());
			}
			if (ctsid.getSubLoc() != null)
			{
				ret.setLocation(ctsid.getBaseLoc() + "-" + ctsid.getSubLoc());
			}
			else
			{
				ret.setLocation(ctsid.getBaseLoc());
			}
		}
		else if (id instanceof HdbTsId)
		{
			HdbTsId htsid = (HdbTsId)id;
			ret.setInterval(htsid.getInterval());
			if (htsid.getDataTypeId() != null)
			{
				ret.setDatatypeId(htsid.getDataTypeId().getValue());
			}
			else
			{
				ret.setDatatypeId(DbKey.NullKey.getValue());
			}
			if (htsid.getSite() != null && htsid.getSite().getId() != null)
			{
				ret.setSiteId(htsid.getSite().getId().getValue());
			}
			else
			{
				ret.setSiteId(DbKey.NullKey.getValue());
			}
		}
		ret.setDatatypeId(id.getDataTypeId().getValue());
		if (ret.getLocation() == null || ret.getLocation().isEmpty())
		{
			ret.setLocation(id.getSiteName());
		}
		return ret;
	}

	@GET
	@Path("tsdata")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getTimeSeriesData(@QueryParam("key") Long tsKey, @QueryParam("start") String start,
			@QueryParam("end") String end)
			throws WebAppException, DbException
	{
		if (tsKey == null)
		{
			throw new MissingParameterException("Missing required tskey parameter.");
		}

		Date dStart = null;
		Date dEnd = null;
		if (start != null)
		{
			try
			{
				dStart = IDateFormat.parse(start);
			}
			catch(IllegalArgumentException ex)
			{
				throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST,
						"Invalid start time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.");
			}
		}
		if (end != null)
		{
			try
			{
				dEnd = IDateFormat.parse(end);
			}
			catch (IllegalArgumentException ex)
			{
				throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST,
						"Invalid end time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.");
			}
		}

		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		try (TimeSeriesDAI dai = tsdb.makeTimeSeriesDAO())
		{
			TimeSeriesIdentifier tsId = dai.getTimeSeriesIdentifier(DbKey.createDbKey(tsKey));
			CTimeSeries cts = tsdb.makeTimeSeries(tsId);
			dai.fillTimeSeries(cts, dStart, dEnd);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(dataMap(cts, dStart, dEnd)).build();
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException("Time series with key=" + tsKey + " not found");
		}
		catch (DbIoException | BadTimeSeriesException ex)
		{
			throw new DbException("Unable to retrieve time series data", ex);
		}
	}

	static ApiTimeSeriesData dataMap(CTimeSeries cts, Date start, Date end)
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();
		ret.setTsid(map(cts.getTimeSeriesIdentifier()));
		ret.setValues(map(cts, start, end));
		return ret;
	}

	static List<ApiTimeSeriesValue> map(CTimeSeries cts, Date start, Date end)
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

	static Date processSample(TimedVariable value, Date current, Date end, List<ApiTimeSeriesValue> ret)
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

	static ApiTimeSeriesIdentifier map(TimeSeriesIdentifier tsid)
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

	@GET
	@Path("intervals")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getIntervals()
			throws DbException
	{
		try (IntervalDAI dai = getLegacyTimeseriesDB().makeIntervalDAO())
		{
			dai.loadAllIntervals();
			List<ApiInterval> intervals = new ArrayList<>();
			for (String code : dai.getValidIntervalCodes())
			{
				Interval intV = IntervalCodes.getInterval(code);
				intervals.add(map(intV));
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(intervals).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve intervals", ex);
		}
	}

	@POST
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postInterval(ApiInterval intv)
			throws DbException
	{
		try (IntervalDAI dai = getLegacyTimeseriesDB().makeIntervalDAO())
		{
			Interval interval = map(intv);
			dai.writeInterval(interval);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(interval)).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to store interval", ex);
		}
	}

	static Interval map(ApiInterval intv)
	{
		Interval ret = new Interval(intv.getName());
		if (intv.getIntervalId() != null)
		{
			ret.setKey(DbKey.createDbKey(intv.getIntervalId()));
		}
		else
		{
			ret.setKey(DbKey.NullKey);
		}
		ret.setCalConstant(str2const(intv.getCalConstant()));
		ret.setCalMultiplier(intv.getCalMultilier());
		return ret;
	}

	static ApiInterval map(Interval intv)
	{
		ApiInterval ret = new ApiInterval();
		if (intv.getKey() != null)
		{
			ret.setIntervalId(intv.getKey().getValue());
		}
		else
		{
			ret.setIntervalId(DbKey.NullKey.getValue());
		}

		ret.setName(intv.getName());
		ret.setCalConstant(IntervalCodes.getCalConstName(intv.getCalConstant()));
		ret.setCalMultilier(intv.getCalMultiplier());
		return ret;
	}

	static int str2const(String s)
	{
		if (s.isEmpty())
			return -1;
		s = s.toUpperCase();
		if (s.charAt(0) == 'H')
			return Calendar.HOUR_OF_DAY;
		else if (s.charAt(0) == 'D')
			return Calendar.DAY_OF_MONTH;
		else if (s.charAt(0) == 'W')
			return Calendar.WEEK_OF_YEAR;
		else if (s.charAt(0) == 'Y')
			return Calendar.YEAR;
		else if (s.startsWith("MI"))
			return Calendar.MINUTE;
		else if (s.startsWith("MO"))
			return Calendar.MONTH;
		else return -1;
	}

	@DELETE
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteInterval(@QueryParam("intvid") Long intvId)
			throws MissingParameterException
	{
		if (intvId == null)
		{
			throw new MissingParameterException("Missing required intvid parameter.");
		}
		throw new UnsupportedOperationException("Deletion of intervals is not implemented");
	}

	@GET
	@Path("tsgrouprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getTsGroupRefs() throws DbException
	{
		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			return Response.status(HttpServletResponse.SC_OK)
					.entity(mapRef(dai.getTsGroupList(null))).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series group references", ex);
		}
	}

	static ArrayList<ApiTsGroupRef> mapRef(ArrayList<TsGroup> groups)
	{
		ArrayList<ApiTsGroupRef> ret = new ArrayList<>();
		for(TsGroup group : groups)
		{
			ApiTsGroupRef ref = new ApiTsGroupRef();
			if (group.getGroupId() != null)
			{
				ref.setGroupId(group.getGroupId().getValue());
			}
			else
			{
				ref.setGroupId(DbKey.NullKey.getValue());
			}
			ref.setGroupName(group.getGroupName());
			ref.setDescription(group.getDescription());
			ref.setGroupType(group.getGroupType());
			ret.add(ref);
		}
		return ret;
	}

	@GET
	@Path("tsgroup")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getTsGroup(@QueryParam("groupid") Long groupId) throws DbException, WebAppException
	{
		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			TsGroup group = dai.getTsGroupById(DbKey.createDbKey(groupId));
			if (group == null)
			{
				throw new DatabaseItemNotFoundException("Time series group with ID=" + groupId + " not found");
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(group)).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series group by ID", ex);
		}
	}

	static ApiTsGroup map(TsGroup group)
	{
		if (group == null)
		{
			return null;
		}
		ApiTsGroup ret = new ApiTsGroup();
		ret.setGroupName(group.getGroupName());
		ret.setDescription(group.getDescription());
		ret.setGroupType(group.getGroupType());
		if (group.getGroupId() != null)
		{
			ret.setGroupId(group.getGroupId().getValue());
		}
		else
		{
			ret.setGroupId(DbKey.NullKey.getValue());
		}
		ret.getIntersectGroups().addAll(mapRef(group.getIntersectedGroups()));
		List<ApiTimeSeriesIdentifier> tsids = new ArrayList<>();
		for (TimeSeriesIdentifier tsid : group.getTsMemberList())
		{
			tsids.add(map(tsid));
		}
		ret.getTsIds().addAll(tsids);
		List<ApiSiteRef> sites = new ArrayList<>();
		for (int i = 0; i < group.getSiteNameList().size(); i++)
		{
			ApiSiteRef site = new ApiSiteRef();
			site.setSiteId(group.getSiteIdList().get(i).getValue());
			site.setPublicName(group.getSiteNameList().get(i));
			sites.add(site);
		}
		ret.getGroupSites().addAll(sites);
		List<ApiDataType> dts = new ArrayList<>();
		for (DbKey dtid : group.getDataTypeIdList())
		{
			ApiDataType dt = new ApiDataType();
			dt.setId(dtid.getValue());
			dts.add(dt);
		}
		ret.getGroupDataTypes().addAll(dts);
		ret.getIncludeGroups().addAll(mapRef(group.getIncludedSubGroups()));
		ret.getExcludeGroups().addAll(mapRef(group.getExcludedSubGroups()));
		return ret;
	}

	@POST
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postTsGroup(ApiTsGroup grp) throws DbException
	{
		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			TsGroup group = map(grp);
			dai.writeTsGroup(group);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(group)).build();
		}
		catch (DbIoException | BadTimeSeriesException ex)
		{
			throw new DbException("Unable to store time series group", ex);
		}
	}

	static TsGroup map(ApiTsGroup grp) throws BadTimeSeriesException
	{
		TsGroup ret = new TsGroup();
		ret.setDescription(grp.getDescription());
		ret.setGroupName(grp.getGroupName());
		ret.setGroupType(grp.getGroupType());
		ret.setIntersectedGroups(map(grp.getIntersectGroups()));
		if (grp.getGroupId() != null)
		{
			ret.setGroupId(DbKey.createDbKey(grp.getGroupId()));
		}
		else
		{
			ret.setGroupId(DbKey.NullKey);
		}
		for (ApiTimeSeriesIdentifier ident : grp.getTsIds())
		{
			ret.addTsMember(map(ident));
		}
		for (ApiSiteRef site : grp.getGroupSites())
		{
			ret.addSiteId(DbKey.createDbKey(site.getSiteId()));
			ret.addSiteName(site.getPublicName());
		}
		for (ApiDataType dt : grp.getGroupDataTypes())
		{
			ret.addDataTypeId(DbKey.createDbKey(dt.getId()));
		}
		for (ApiTsGroupRef include : grp.getIncludeGroups())
		{
			TsGroup inc = new TsGroup();
			inc.setGroupName(include.getGroupName());
			inc.setDescription(include.getDescription());
			inc.setGroupId(DbKey.createDbKey(include.getGroupId()));
			inc.setGroupType(include.getGroupType());

			ret.addSubGroup(inc,'A');
		}
		for (ApiTsGroupRef exclude : grp.getExcludeGroups())
		{
			TsGroup exc = new TsGroup();
			exc.setGroupName(exclude.getGroupName());
			exc.setDescription(exclude.getDescription());
			exc.setGroupId(DbKey.createDbKey(exclude.getGroupId()));
			exc.setGroupType(exclude.getGroupType());

			ret.addSubGroup(exc,'S');
		}
		return ret;
	}

	static TimeSeriesIdentifier map(ApiTimeSeriesIdentifier tsid) throws BadTimeSeriesException
	{
		TimeSeriesIdentifier ret = new CwmsTsId();
		if (tsid.getKey() != null)
		{
			ret.setKey(DbKey.createDbKey(tsid.getKey()));
		}
		else
		{
			ret.setKey(DbKey.NullKey);
		}
		ret.setUniqueString(tsid.getUniqueString());
		ret.setDescription(tsid.getDescription());
		ret.setStorageUnits(tsid.getStorageUnits());
		return ret;
	}

	static ArrayList<TsGroup> map(List<ApiTsGroupRef> groupRefs)
	{
		ArrayList<TsGroup> ret = new ArrayList<>();
		for(ApiTsGroupRef ref : groupRefs)
		{
			TsGroup group = new TsGroup();
			group.setGroupName(ref.getGroupName());
			group.setDescription(ref.getDescription());
			group.setGroupType(ref.getGroupType());
			if (ref.getGroupId() != null)
			{
				group.setGroupId(DbKey.createDbKey(ref.getGroupId()));
			}
			else
			{
				group.setGroupId(DbKey.NullKey);
			}
			ret.add(group);
		}
		return ret;
	}

	@DELETE
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteTsGroup(@QueryParam("groupid") Long groupId) throws WebAppException, DbException
	{
		if (groupId == null)
		{
			throw new MissingParameterException("Missing required groupid parameter.");
		}

		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			dai.deleteTsGroup(DbKey.createDbKey(groupId));
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("tsgroup with ID=" + groupId + " deleted").build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to delete time series group", ex);
		}
	}
}