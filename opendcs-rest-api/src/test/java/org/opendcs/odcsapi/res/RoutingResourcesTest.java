package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import decodes.db.DataSource;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.RoutingStatus;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import ilex.util.Logger;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiDacqEvent;
import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiRoutingExecStatus;
import org.opendcs.odcsapi.beans.ApiRoutingRef;
import org.opendcs.odcsapi.beans.ApiRoutingStatus;
import org.opendcs.odcsapi.beans.ApiScheduleEntry;
import org.opendcs.odcsapi.beans.ApiScheduleEntryRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opendcs.odcsapi.res.RoutingResources.map;
import static org.opendcs.odcsapi.res.RoutingResources.statusMap;

final class RoutingResourcesTest
{
	@Test
	void testRoutingRefListMap() throws Exception
	{
		RoutingSpecList routingSpecList = new RoutingSpecList();
		RoutingSpec routingSpec = buildRoutingSpec();
		routingSpecList.add(routingSpec);

		List<ApiRoutingRef> apiRoutingRefs = map(routingSpecList);

		ApiRoutingRef apiRoutingRef = apiRoutingRefs.get(0);
		assertNotNull(apiRoutingRef);
		assertEquals(apiRoutingRef.getRoutingId(), routingSpec.getId().getValue());
		assertEquals(apiRoutingRef.getName(), routingSpec.getName());
		assertEquals(apiRoutingRef.getDestination(), routingSpec.consumerArg);
		assertEquals(apiRoutingRef.getDataSourceName(), routingSpec.dataSource.getName());
		assertEquals(apiRoutingRef.getLastModified(), routingSpec.lastModifyTime);
	}

	@Test
	void testApiRoutingMap() throws Exception
	{
		RoutingSpec routingSpec = buildRoutingSpec();
		ApiRouting apiRouting = map(routingSpec);
		assertNotNull(apiRouting);
		assertEquals(apiRouting.getRoutingId(), routingSpec.getId().getValue());
		assertEquals(apiRouting.getName(), routingSpec.getName());
		assertEquals(apiRouting.getOutputTZ(), routingSpec.outputTimeZoneAbbr);
		assertEquals(apiRouting.getLastModified(), routingSpec.lastModifyTime);
		assertEquals(apiRouting.isEnableEquations(), routingSpec.enableEquations);
		assertEquals(new Vector<>(apiRouting.getNetlistNames()), routingSpec.networkListNames);
	}

	@Test
	void testRoutingSpecStatusMap()
	{
		List<RoutingStatus> statuses = new ArrayList<>();
		RoutingStatus status = new RoutingStatus(DbKey.createDbKey(1234L));
		status.setLastMessageTime(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		status.setLastActivityTime(Date.from(Instant.parse("2021-02-01T12:00:00Z")));
		status.setNumDecodesErrors(10);
		status.setNumMessages(20);
		status.setRoutingSpecId(DbKey.createDbKey(5678L));
		status.setEnabled(true);
		status.setName("TestRoutingSpec");
		status.setAppId(DbKey.createDbKey(9012L));
		status.setRunInterval("1h");
		status.setAppName("TestAppName");
		status.setManual(true);
		status.setScheduleEntryId(DbKey.createDbKey(3456L));
		statuses.add(status);

		List<ApiRoutingStatus> results = map(statuses);

		assertNotNull(results);
		assertEquals(1, results.size());
		ApiRoutingStatus result = results.get(0);
		assertNotNull(result);
		assertEquals(status.getRoutingSpecId().getValue(), result.getRoutingSpecId());
		assertEquals(status.getName(), result.getName());
		assertEquals(status.getAppId().getValue(), result.getAppId());
		assertEquals(status.getAppName(), result.getAppName());
		assertEquals(status.getScheduleEntryId().getValue(), result.getScheduleEntryId());
		assertEquals(status.getRunInterval(), result.getRunInterval());
		assertEquals(status.isEnabled(), result.isEnabled());
		assertEquals(status.isManual(), result.isManual());
		assertEquals(status.getNumMessages(), result.getNumMessages());
		assertEquals(status.getNumDecodesErrors(), result.getNumErrors());
		assertEquals(status.getLastActivityTime(), result.getLastActivity());
		assertEquals(status.getLastMessageTime(), result.getLastMsgTime());
	}

	@Test
	void testRoutingSpecMap() throws Exception
	{
		ApiRouting apiRouting = new ApiRouting();
		apiRouting.setRoutingId(1234L);
		apiRouting.setName("TestRoutingSpec");
		RoutingSpec routingSpec = map(apiRouting);
		assertNotNull(routingSpec);
		assertEquals(routingSpec.getId().getValue(), apiRouting.getRoutingId());
		assertEquals(routingSpec.getName(), apiRouting.getName());
		if (routingSpec.outputTimeZone != null)
		{
			assertEquals(routingSpec.outputTimeZone.getID(), apiRouting.getOutputTZ());
		}
		else if (apiRouting.getOutputTZ() != null)
		{
			fail("routingSpec.outputTimeZone is null, but apiRouting.getOutputTZ() is not null");
		}
		assertNotNull(routingSpec.lastModifyTime);
		assertEquals(routingSpec.enableEquations, apiRouting.isEnableEquations());
		assertEquals(routingSpec.networkListNames, new Vector<>(apiRouting.getNetlistNames()));
	}

	@Test
	void testScheduleEntryRefMap()
	{
		List<ScheduleEntry> scheduleEntries = new ArrayList<>();
		ScheduleEntry scheduleEntry = new ScheduleEntry(DbKey.createDbKey(1234L));
		scheduleEntry.setEnabled(true);
		scheduleEntry.setName("TestScheduleEntry");
		scheduleEntry.setRoutingSpecId(DbKey.createDbKey(5678L));
		scheduleEntry.setLastModified(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		scheduleEntry.setLoadingAppName("TestAppName");
		scheduleEntry.setRoutingSpecName("TestRoutingSpec");
		scheduleEntry.setTimezone("UTC");
		scheduleEntry.setStartTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		scheduleEntries.add(scheduleEntry);
		List<ApiScheduleEntryRef> apiScheduleEntryRefs = RoutingResources.entryMap(scheduleEntries);
		assertNotNull(apiScheduleEntryRefs);
		ApiScheduleEntryRef apiScheduleEntryRef = apiScheduleEntryRefs.get(0);
		assertNotNull(apiScheduleEntryRef);
		assertEquals(apiScheduleEntryRef.getSchedEntryId(), scheduleEntry.getKey().getValue());
		assertEquals(apiScheduleEntryRef.getName(), scheduleEntry.getName());
		assertEquals(apiScheduleEntryRef.getAppName(), scheduleEntry.getLoadingAppName());
		assertEquals(apiScheduleEntryRef.getRoutingSpecName(), scheduleEntry.getRoutingSpecName());
		assertEquals(apiScheduleEntryRef.getLastModified(), scheduleEntry.getLastModified());
	}

	@Test
	void testScheduleEntryMap() throws Exception
	{
		ApiScheduleEntry apiScheduleEntry = new ApiScheduleEntry();
		apiScheduleEntry.setSchedEntryId(1234L);
		apiScheduleEntry.setName("TestScheduleEntry");
		apiScheduleEntry.setAppName("TestAppName");
		apiScheduleEntry.setRoutingSpecName("TestRoutingSpec");
		apiScheduleEntry.setLastModified(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		apiScheduleEntry.setAppId(5678L);
		apiScheduleEntry.setRoutingSpecId(9012L);
		apiScheduleEntry.setStartTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		apiScheduleEntry.setEnabled(true);
		apiScheduleEntry.setTimeZone("UTC");
		apiScheduleEntry.setRunInterval("1h");
		ScheduleEntry scheduleEntry = map(apiScheduleEntry);
		assertNotNull(scheduleEntry);
		assertEquals(scheduleEntry.getKey().getValue(), apiScheduleEntry.getSchedEntryId());
		assertEquals(scheduleEntry.getName(), apiScheduleEntry.getName());
		assertEquals(scheduleEntry.getLoadingAppName(), apiScheduleEntry.getAppName());
		assertEquals(scheduleEntry.getRoutingSpecName(), apiScheduleEntry.getRoutingSpecName());
		assertEquals(scheduleEntry.getLastModified(), apiScheduleEntry.getLastModified());
		assertEquals(scheduleEntry.getLoadingAppId().getValue(), apiScheduleEntry.getAppId());
		assertEquals(scheduleEntry.getRoutingSpecId().getValue(), apiScheduleEntry.getRoutingSpecId());
		assertEquals(scheduleEntry.getStartTime(), apiScheduleEntry.getStartTime());
		assertEquals(scheduleEntry.isEnabled(), apiScheduleEntry.isEnabled());
		assertEquals(scheduleEntry.getTimezone(), apiScheduleEntry.getTimeZone());
		assertEquals(scheduleEntry.getRunInterval(), apiScheduleEntry.getRunInterval());
	}

	@Test
	void testApiScheduleEntryListMap()
	{
		ArrayList<ScheduleEntry> scheduleEntries = new ArrayList<>();
		ScheduleEntry scheduleEntry = new ScheduleEntry(DbKey.createDbKey(1234L));
		scheduleEntry.setName("TestScheduleEntry");
		scheduleEntry.setLoadingAppName("TestAppName");
		scheduleEntry.setRoutingSpecName("TestRoutingSpec");
		scheduleEntry.setLastModified(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		scheduleEntry.setLoadingAppId(DbKey.createDbKey(5678L));
		scheduleEntry.setRoutingSpecId(DbKey.createDbKey(9012L));
		scheduleEntry.setStartTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		scheduleEntry.setEnabled(true);
		scheduleEntry.setTimezone("UTC");
		scheduleEntry.setRunInterval("1h");
		scheduleEntries.add(scheduleEntry);

		List<ApiScheduleEntryRef> results = RoutingResources.entryMap(scheduleEntries);

		assertNotNull(results);
		assertEquals(1, results.size());
		ApiScheduleEntryRef result = results.get(0);
		assertNotNull(result);
		assertEquals(scheduleEntry.getKey().getValue(), result.getSchedEntryId());
		assertEquals(scheduleEntry.getName(), result.getName());
		assertEquals(scheduleEntry.getLoadingAppName(), result.getAppName());
		assertEquals(scheduleEntry.getRoutingSpecName(), result.getRoutingSpecName());
		assertEquals(scheduleEntry.getLastModified(), result.getLastModified());
	}

	@Test
	void testRoutingStatusMap()
	{
		ArrayList<ScheduleEntryStatus> scheduleEntries = new ArrayList<>();
		ScheduleEntryStatus scheduleEntry = new ScheduleEntryStatus(DbKey.createDbKey(1234L));
		scheduleEntry.setScheduleEntryName("TestScheduleEntry");
		scheduleEntry.setLastModified(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		scheduleEntry.setHostname("TestHost");
		scheduleEntry.setRunStatus("TestStatus");
		scheduleEntry.setNumDecodesErrors(10);
		scheduleEntry.setNumMessages(20);
		scheduleEntry.setNumPlatforms(30);
		scheduleEntry.setRunStop(Date.from(Instant.parse("2021-03-01T00:00:00Z")));
		scheduleEntry.setRunStart(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		scheduleEntry.setLastModified(Date.from(Instant.parse("2021-04-01T00:00:00Z")));
		scheduleEntries.add(scheduleEntry);

		ArrayList<ApiRoutingExecStatus> results = statusMap(scheduleEntries);
		assertNotNull(results);
		assertEquals(1, results.size());
		ApiRoutingExecStatus result = results.get(0);
		assertNotNull(result);
		assertEquals(scheduleEntry.getScheduleEntryId().getValue(), result.getScheduleEntryId());
		assertEquals(scheduleEntry.getHostname(), result.getHostname());
		assertEquals(scheduleEntry.getRunStatus(), result.getRunStatus());
		assertEquals(scheduleEntry.getNumDecodesErrors(), result.getNumErrors());
		assertEquals(scheduleEntry.getNumMessages(), result.getNumMessages());
		assertEquals(scheduleEntry.getNumPlatforms(), result.getNumPlatforms());
		assertEquals(scheduleEntry.getRunStop(), result.getRunStop());
		assertEquals(scheduleEntry.getRunStart(), result.getRunStart());
		assertEquals(scheduleEntry.getLastModified(), result.getLastActivity());
	}

	@Test
	void testApiScheduleEntryMap()
	{
		ScheduleEntry scheduleEntry = new ScheduleEntry(DbKey.createDbKey(1234L));
		scheduleEntry.setName("TestScheduleEntry");
		scheduleEntry.setLoadingAppName("TestAppName");
		scheduleEntry.setRoutingSpecName("TestRoutingSpec");
		scheduleEntry.setLastModified(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		scheduleEntry.setLoadingAppId(DbKey.createDbKey(5678L));
		scheduleEntry.setRoutingSpecId(DbKey.createDbKey(9012L));
		scheduleEntry.setStartTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		scheduleEntry.setEnabled(true);
		scheduleEntry.setTimezone("UTC");
		scheduleEntry.setRunInterval("1h");

		ApiScheduleEntry apiScheduleEntry = map(scheduleEntry);
		assertNotNull(apiScheduleEntry);
		assertEquals(apiScheduleEntry.getSchedEntryId(), scheduleEntry.getKey().getValue());
		assertEquals(apiScheduleEntry.getName(), scheduleEntry.getName());
		assertEquals(apiScheduleEntry.getAppName(), scheduleEntry.getLoadingAppName());
		assertEquals(apiScheduleEntry.getRoutingSpecName(), scheduleEntry.getRoutingSpecName());
		assertEquals(apiScheduleEntry.getLastModified(), scheduleEntry.getLastModified());
		assertEquals(apiScheduleEntry.getAppId(), scheduleEntry.getLoadingAppId().getValue());
		assertEquals(apiScheduleEntry.getRoutingSpecId(), scheduleEntry.getRoutingSpecId().getValue());
		assertEquals(apiScheduleEntry.getStartTime(), scheduleEntry.getStartTime());
		assertEquals(apiScheduleEntry.isEnabled(), scheduleEntry.isEnabled());
		assertEquals(apiScheduleEntry.getTimeZone(), scheduleEntry.getTimezone());
		assertEquals(apiScheduleEntry.getRunInterval(), scheduleEntry.getRunInterval());
	}

	@Test
	void testDacqEventMap()
	{
		DacqEvent dacqEvent = new DacqEvent();
		dacqEvent.setAppId(DbKey.createDbKey(1234L));
		dacqEvent.setDacqEventId(DbKey.createDbKey(5678L));
		dacqEvent.setEventText("TestEvent");
		dacqEvent.setEventTime(Date.from(Instant.parse("2021-02-01T00:00:00Z")));
		dacqEvent.setEventPriority(Logger.E_DEBUG1);
		dacqEvent.setMsgRecvTime(Date.from(Instant.parse("2021-02-01T12:00:00Z")));
		dacqEvent.setSubsystem("TestSubsystem");
		dacqEvent.setScheduleEntryStatusId(DbKey.createDbKey(9012L));
		dacqEvent.setPlatformId(DbKey.createDbKey(3456L));

		ApiDacqEvent apiDacqEvent = map(dacqEvent);

		assertNotNull(apiDacqEvent);
		assertEquals(apiDacqEvent.getAppId(), dacqEvent.getAppId().getValue());
		assertEquals(apiDacqEvent.getEventId(), dacqEvent.getDacqEventId().getValue());
		assertEquals(apiDacqEvent.getEventText(), dacqEvent.getEventText());
		assertEquals(apiDacqEvent.getEventTime(), dacqEvent.getEventTime());
		assertEquals(apiDacqEvent.getPriority(), Logger.priorityName[dacqEvent.getEventPriority()]);
		assertEquals(apiDacqEvent.getMsgRecvTime(), dacqEvent.getMsgRecvTime());
		assertEquals(apiDacqEvent.getSubsystem(), dacqEvent.getSubsystem());
		assertEquals(apiDacqEvent.getRoutingExecId(), dacqEvent.getScheduleEntryStatusId().getValue());
		assertEquals(apiDacqEvent.getPlatformId(), dacqEvent.getPlatformId().getValue());
	}

	private RoutingSpec buildRoutingSpec() throws Exception
	{
		DataSource dataSource = new DataSource();
		dataSource.setName("TestDataSource");
		dataSource.setId(DbKey.createDbKey(2345L));
		dataSource.setDataSourceArg("TestDataSourceArg");
		RoutingSpec routingSpec = new RoutingSpec();
		routingSpec.setName("TestRoutingSpec");
		routingSpec.setId(DbKey.createDbKey(1234L));
		routingSpec.outputTimeZone = TimeZone.getTimeZone("UTC");
		routingSpec.lastModifyTime = Date.from(Instant.parse("2021-02-01T00:00:00Z"));
		routingSpec.enableEquations = true;
		routingSpec.networkListNames = new Vector<>(Arrays.asList("TestNet", "TestNet2"));
		routingSpec.dataSource = dataSource;
		return routingSpec;
	}
}
