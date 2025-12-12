/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import decodes.cwms.CwmsTsId;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import opendcs.opentsdb.Interval;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesSpec;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.beans.ApiTsGroupRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.odcsapi.res.TimeSeriesResources.dataMap;
import static org.opendcs.odcsapi.res.TimeSeriesResources.idMap;
import static org.opendcs.odcsapi.res.TimeSeriesResources.map;
import static org.opendcs.odcsapi.res.TimeSeriesResources.mapRef;
import static org.opendcs.odcsapi.res.TimeSeriesResources.processSample;
import static org.opendcs.odcsapi.res.TimeSeriesResources.specMap;
import static org.opendcs.odcsapi.res.TimeSeriesResources.str2const;

final class TimeSeriesResourcesTest
{
	@Test
	void testTSIdentifierMap() throws Exception
	{
		ArrayList<TimeSeriesIdentifier> identifiers = new ArrayList<>();
		TimeSeriesIdentifier id = new CwmsTsId();
		id.setDescription("Local TimeSeries");
		id.setUniqueString("Davis.Flow.Inst.1Hour.0.GOES");
		Site site = new Site();
		site.setPublicName("Instantaneous Flow TimeSeries");
		site.setActive(true);
		site.setLastModifyTime(Date.from(Instant.parse("2021-08-01T00:00:00Z")));
		site.setElevation(100.0);
		site.setLocationType("PUMP");
		site.setDescription("Pump located in Davis, CA");
		id.setSite(site);
		id.setInterval("hour");
		id.setTableSelector("TS");
		id.setStorageUnits("cms");
		id.setSiteName("Davis pump station");
		id.setDisplayName("Davis Pump Station Flow");
		id.setKey(DbKey.createDbKey(88795L));
		id.setReadTime(55933124L);
		identifiers.add(id);

		List<ApiTimeSeriesIdentifier> apiIdentifiers = idMap(identifiers);

		assertNotNull(apiIdentifiers);
		assertEquals(identifiers.size(), apiIdentifiers.size());
		ApiTimeSeriesIdentifier apiId = apiIdentifiers.get(0);
		assertNotNull(apiId);
		assertMatch(id, apiId);
	}

	@Test
	void testCTimeSeriesMap() throws Exception
	{
		CTimeSeries cts = new CTimeSeries(DbKey.createDbKey(86795L), null, null);
		cts.setBriefDescription("Computational TimeSeries");
		cts.setComputationId(DbKey.createDbKey(86775L));
		cts.setInterval("hour");
		cts.addDependentCompId(DbKey.createDbKey(86785L));
		cts.addTaskListRecNum(56);
		cts.setDisplayName("TimeSeries for computation");
		TimeSeriesIdentifier id = new CwmsTsId();
		id.setDescription("TimeSeries data used for computation");
		id.setUniqueString("SAC.Flow.Inst.1Hour.0.GOES");
		id.setKey(DbKey.createDbKey(88795L));
		id.setInterval("hour");
		id.setSiteName("Sacramento River");
		id.setStorageUnits("m");
		id.setTableSelector("Test");
		Site site = new Site();
		site.setPublicName("Sacramento River Pump Station");
		site.setActive(true);
		site.setLastModifyTime(Date.from(Instant.parse("2021-08-01T00:00:00Z")));
		site.setElevation(100.0);
		site.setLocationType("PUMP");
		site.setId(DbKey.createDbKey(885L));
		site.setDescription("Pump located on Sacramento River");
		id.setSite(site);
		cts.setTimeSeriesIdentifier(id);
		TimedVariable tv = new TimedVariable(Date.from(Instant.parse("2021-08-01T00:00:00Z")), 10.0, 0);
		cts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2021-08-01T01:00:00Z")), 20.0, 0);
		cts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2021-08-01T02:00:00Z")), 30.0, 0);
		cts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2021-08-01T03:00:00Z")), 40.0, 0);
		cts.addSample(tv);

		ApiTimeSeriesData apiTimeSeriesData = dataMap(cts,
				Date.from(Instant.parse("2021-08-01T00:00:00Z")),
				Date.from(Instant.parse("2021-08-01T03:00:00Z")));

		assertNotNull(apiTimeSeriesData);
		assertMatch(cts.getTimeSeriesIdentifier(), apiTimeSeriesData.getTsid());
		List<ApiTimeSeriesValue> values = apiTimeSeriesData.getValues();
		assertNotNull(values);
		assertEquals(cts.size(), values.size());
		for (int i = 0; i < cts.size(); i++)
		{
			boolean found = false;
			for (int j = 0; j < values.size(); j++)
			{
				if (cts.sampleAt(i).getTime() == values.get(i).getSampleTime())
				{
					assertEquals(cts.sampleAt(i).getTime(), values.get(i).getSampleTime());
					assertEquals(cts.sampleAt(i).getDoubleValue(), values.get(i).getValue());
					assertEquals(cts.sampleAt(i).getFlags(), values.get(i).getFlags());
					found = true;
				}
			}
			assertTrue(found);
		}
	}

	@Test
	void testProcessSample() throws NoConversionException
	{
		TimedVariable tv = new TimedVariable(Date.from(Instant.parse("2021-08-01T00:15:00Z")), 10.0, 0);
		Date start = Date.from(Instant.parse("2021-08-01T00:00:00Z"));
		Date end = Date.from(Instant.parse("2021-08-01T01:00:00Z"));

		List<ApiTimeSeriesValue> values = new ArrayList<>();

		Date current = processSample(tv, start, end, values);
		assertNotNull(current);
		assertTrue(current.after(start));
		assertTrue(current.before(end));
		assertEquals(1, values.size());
		assertEquals(tv.getTime(), values.get(0).getSampleTime());
		assertEquals(tv.getDoubleValue(), values.get(0).getValue());
	}

	private void assertMatch(TimeSeriesIdentifier id, ApiTimeSeriesIdentifier apiId)
	{
		assertEquals(id.getDescription(), apiId.getDescription());
		assertEquals(id.getUniqueString(), apiId.getUniqueString());
		assertEquals(id.getKey().getValue(), apiId.getKey());
		assertEquals(id.getStorageUnits(), apiId.getStorageUnits());
	}

	@Test
	void testTimeSeriesIdMap() throws BadTimeSeriesException
	{
		ApiTimeSeriesIdentifier apiId = new ApiTimeSeriesIdentifier();
		apiId.setDescription("TimeSeries data for USACE");
		apiId.setKey(1234L);
		apiId.setUniqueString("USACE_TF.Vol.Avg.1Minute.1.latest");
		apiId.setStorageUnits("m3");
		apiId.setActive(true);

		TimeSeriesIdentifier id = map(apiId);

		assertNotNull(id);
		assertEquals(apiId.getDescription(), id.getDescription());
		assertEquals(apiId.getUniqueString(), id.getUniqueString());
		assertEquals(apiId.getKey(), id.getKey().getValue());
		assertEquals(apiId.getStorageUnits(), id.getStorageUnits());
	}

	@Test
	void testIntervalMap()
	{
		ApiInterval apiInterval = new ApiInterval();
		apiInterval.setIntervalId(1234L);
		apiInterval.setCalConstant("YEAR");
		apiInterval.setName("Hourly");
		apiInterval.setCalMultilier(2);

		Interval interval = map(apiInterval);

		assertNotNull(interval);
		assertEquals(apiInterval.getIntervalId(), interval.getKey().getValue());
		assertEquals(str2const(apiInterval.getCalConstant()), interval.getCalConstant());
		assertEquals(apiInterval.getName(), interval.getName());
		assertEquals(apiInterval.getCalMultilier(), interval.getCalMultiplier());
	}

	@Test
	void testApiIntervalMap()
	{
		Interval interval = new Interval("Daily");
		interval.setKey(DbKey.createDbKey(1234L));
		interval.setCalConstant(Calendar.DAY_OF_MONTH);
		interval.setCalMultiplier(2);

		ApiInterval apiInterval = map(interval);

		assertNotNull(apiInterval);
		assertEquals(interval.getKey().getValue(), apiInterval.getIntervalId());
		assertEquals(IntervalCodes.getCalConstName(interval.getCalConstant()), apiInterval.getCalConstant());
		assertEquals(interval.getName(), apiInterval.getName());
		assertEquals(interval.getCalMultiplier(), apiInterval.getCalMultilier());
	}

	@Test
	void testTSGroupRefMap()
	{
		ArrayList<TsGroup> tsGroups = new ArrayList<>();
		TsGroup tsGroup = new TsGroup();
		tsGroup.setGroupId(DbKey.createDbKey(1234L));
		tsGroup.setGroupName("Pump Group");
		tsGroup.setGroupType("Average");
		tsGroup.setDescription("Average flow data TimeSeries");
		tsGroup.setIsExpanded(false);
		ArrayList<TsGroup> groups = new ArrayList<>();
		TsGroup tsGroup2 = new TsGroup();
		tsGroup2.setGroupId(DbKey.createDbKey(1235L));
		tsGroup2.setGroupName("Lock Group");
		tsGroup2.setGroupType("Instantaneous");
		tsGroup2.setDescription("Instantaneous flow data TimeSeries");
		groups.add(tsGroup2);
		tsGroup.setIntersectedGroups(groups);
		tsGroups.add(tsGroup);

		ArrayList<ApiTsGroupRef> apiTsGroupRefs = mapRef(tsGroups);

		assertNotNull(apiTsGroupRefs);
		ApiTsGroupRef apiTsGroupRef = apiTsGroupRefs.get(0);
		assertNotNull(apiTsGroupRef);
		assertEquals(tsGroup.getGroupId().getValue(), apiTsGroupRef.getGroupId());
		assertEquals(tsGroup.getGroupName(), apiTsGroupRef.getGroupName());
		assertEquals(tsGroup.getGroupType(), apiTsGroupRef.getGroupType());
		assertEquals(tsGroup.getDescription(), apiTsGroupRef.getDescription());
	}

	@Test
	void testTSGroupMap()
	{
		TsGroup tsGroup = new TsGroup();
		tsGroup.setGroupId(DbKey.createDbKey(1234L));
		tsGroup.setGroupName("TimeSeries group");
		tsGroup.setGroupType("Instantaneous");
		tsGroup.setDescription("A group of instantaneous time series");
		tsGroup.setIsExpanded(false);
		ArrayList<TsGroup> groups = new ArrayList<>();
		TsGroup tsGroup2 = new TsGroup();
		tsGroup2.setGroupId(DbKey.createDbKey(1235L));
		tsGroup2.setGroupName("TimeSeries group 2");
		tsGroup2.setGroupType("Average");
		tsGroup2.setDescription("A group of average time series");
		groups.add(tsGroup2);
		tsGroup.setIntersectedGroups(groups);

		ApiTsGroup apiTsGroup = map(tsGroup);

		assertNotNull(apiTsGroup);
		assertEquals(tsGroup.getGroupId().getValue(), apiTsGroup.getGroupId());
		assertEquals(tsGroup.getGroupName(), apiTsGroup.getGroupName());
		assertEquals(tsGroup.getGroupType(), apiTsGroup.getGroupType());
		assertEquals(tsGroup.getDescription(), apiTsGroup.getDescription());
		assertEquals(tsGroup.getIntersectedGroups().size(), apiTsGroup.getIntersectGroups().size());
		TsGroup intersectGroup = tsGroup.getIntersectedGroups().get(0);
		ApiTsGroupRef apiIntersectGroup = apiTsGroup.getIntersectGroups().get(0);
		assertNotNull(apiIntersectGroup);
		assertNotNull(intersectGroup);
		assertEquals(intersectGroup.getGroupId().getValue(), apiIntersectGroup.getGroupId());
		assertEquals(intersectGroup.getGroupName(), apiIntersectGroup.getGroupName());
		assertEquals(intersectGroup.getGroupType(), apiIntersectGroup.getGroupType());
		assertEquals(intersectGroup.getDescription(), apiIntersectGroup.getDescription());
	}

	@Test
	void testApiTSGroupMap() throws Exception
	{
		ApiTsGroup apiTsGroup = new ApiTsGroup();
		apiTsGroup.setDescription("Basin TimeSeries data group");
		apiTsGroup.setGroupName("Basin group");
		apiTsGroup.setGroupType("Average");
		apiTsGroup.setGroupId(1234L);

		TsGroup tsGroup = map(apiTsGroup);

		assertNotNull(tsGroup);
		assertEquals(apiTsGroup.getGroupId(), tsGroup.getGroupId().getValue());
		assertEquals(apiTsGroup.getGroupName(), tsGroup.getGroupName());
		assertEquals(apiTsGroup.getGroupType(), tsGroup.getGroupType());
		assertEquals(apiTsGroup.getDescription(), tsGroup.getDescription());
	}

	@Test
	void testGroupRefListMap()
	{
		ArrayList<TsGroup> groups = new ArrayList<>();
		TsGroup tsGroup = new TsGroup();
		tsGroup.setGroupId(DbKey.createDbKey(1234L));
		tsGroup.setGroupName("River Group 1");
		tsGroup.setGroupType("River");
		tsGroup.setDescription("TimeSeries data for river");
		tsGroup.setIsExpanded(false);
		groups.add(tsGroup);
		TsGroup tsGroup2 = new TsGroup();
		tsGroup2.setGroupId(DbKey.createDbKey(1235L));
		tsGroup2.setGroupName("Delta Group 2");
		tsGroup2.setGroupType("Delta");
		tsGroup2.setDescription("TimeSeries data for delta");
		tsGroup2.setIsExpanded(true);
		tsGroup2.setIntersectedGroups(groups);
		groups.add(tsGroup2);

		ArrayList<ApiTsGroupRef> apiTsGroupRefs = mapRef(groups);

		assertNotNull(apiTsGroupRefs);
		assertEquals(groups.size(), apiTsGroupRefs.size());
		ApiTsGroupRef apiTsGroupRef = apiTsGroupRefs.get(0);
		assertNotNull(apiTsGroupRef);
		assertEquals(tsGroup.getGroupId().getValue(), apiTsGroupRef.getGroupId());
		assertEquals(tsGroup.getGroupName(), apiTsGroupRef.getGroupName());
		assertEquals(tsGroup.getGroupType(), apiTsGroupRef.getGroupType());
		assertEquals(tsGroup.getDescription(), apiTsGroupRef.getDescription());
		ApiTsGroupRef apiTsGroupRef2 = apiTsGroupRefs.get(1);
		assertNotNull(apiTsGroupRef2);
		assertEquals(tsGroup2.getGroupId().getValue(), apiTsGroupRef2.getGroupId());
		assertEquals(tsGroup2.getGroupName(), apiTsGroupRef2.getGroupName());
		assertEquals(tsGroup2.getGroupType(), apiTsGroupRef2.getGroupType());
		assertEquals(tsGroup2.getDescription(), apiTsGroupRef2.getDescription());
	}

	@Test
	void testApiTimeSeriesSpecMap() throws Exception
	{
		TimeSeriesIdentifier id = new CwmsTsId();
		id.setDescription("Volume TimeSeries");
		id.setKey(DbKey.createDbKey(88795L));
		id.setInterval("minute");
		id.setSiteName("USACE Test Facility");
		id.setStorageUnits("m3");
		id.setTableSelector("VOL");
		Site site = new Site();
		site.setPublicName("TimeSeries data for USACE");
		site.setActive(true);
		site.setLastModifyTime(Date.from(Instant.parse("2021-08-01T00:00:00Z")));
		site.setElevation(100.0);
		site.setLocationType("USACE");
		site.setDescription("System Testing Facility");
		id.setSite(site);
		id.setUniqueString("USACE_TF.Vol.Avg.1Minute.1.latest");

		ApiTimeSeriesSpec apiSpec = specMap(id);

		assertNotNull(apiSpec);
		assertEquals(id.getDescription(), apiSpec.getTsid().getDescription());
		assertEquals(id.getUniqueString(), apiSpec.getTsid().getUniqueString());
		assertEquals(id.getKey().getValue(), apiSpec.getTsid().getKey());
		assertEquals(id.getSiteName(), apiSpec.getLocation());
		assertEquals(id.getInterval(), apiSpec.getInterval());
		assertEquals(id.getStorageUnits(), apiSpec.getTsid().getStorageUnits());
	}

	@Test
	void testApiTimeSeriesGroupRefMap()
	{
		ArrayList<ApiTsGroupRef> apiTsGroupRefs = new ArrayList<>();
		ApiTsGroupRef apiTsGroupRef = new ApiTsGroupRef();
		apiTsGroupRef.setDescription("TimeSeries data for USBR");
		apiTsGroupRef.setGroupId(1234L);
		apiTsGroupRef.setGroupName("USBR rainfall");
		apiTsGroupRef.setGroupType("Average");
		apiTsGroupRefs.add(apiTsGroupRef);

		ArrayList<TsGroup> groups = map(apiTsGroupRefs);
		assertNotNull(groups);
		assertEquals(apiTsGroupRefs.size(), groups.size());
		TsGroup tsGroup = groups.get(0);
		assertNotNull(tsGroup);
		assertEquals(apiTsGroupRef.getGroupId(), tsGroup.getGroupId().getValue());
		assertEquals(apiTsGroupRef.getGroupName(), tsGroup.getGroupName());
		assertEquals(apiTsGroupRef.getGroupType(), tsGroup.getGroupType());
		assertEquals(apiTsGroupRef.getDescription(), tsGroup.getDescription());
	}

	@Test
	void testStringToConstant()
	{
		String calConst = "HOUR OF DAY";
		int constant = str2const(calConst);
		assertEquals(Calendar.HOUR_OF_DAY, constant);

		calConst = "DAY OF MONTH";
		constant = str2const(calConst);
		assertEquals(Calendar.DAY_OF_MONTH, constant);

		calConst = "WEEK OF YEAR";
		constant = str2const(calConst);
		assertEquals(Calendar.WEEK_OF_YEAR, constant);

		calConst = "YEAR";
		constant = str2const(calConst);
		assertEquals(Calendar.YEAR, constant);

		calConst = "MINUTE";
		constant = str2const(calConst);
		assertEquals(Calendar.MINUTE, constant);

		calConst = "MONTH";
		constant = str2const(calConst);
		assertEquals(Calendar.MONTH, constant);
	}
}