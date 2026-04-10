package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.Date;
import java.util.Properties;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TsdbCompLock;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendcs.odcsapi.res.AppResources.map;
import static org.opendcs.odcsapi.res.AppResources.mapLoading;

final class AppResourcesTest
{
	@Test
	void testAppRefMap()
	{
		CompAppInfo compAppInfo = new CompAppInfo();
		compAppInfo.setAppName("Computation application");
		compAppInfo.setComment("Computation to find the volume of a river");
		compAppInfo.setLastModified(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		compAppInfo.setAppId(DbKey.createDbKey(151615L));
		compAppInfo.setNumComputations(1);
		// Properties are not mapped to the ApiAppRef object
		Properties properties = new Properties();
		properties.setProperty("compRef", "applicationValue");
		compAppInfo.setProperties(properties);

		ApiAppRef appRef = map(compAppInfo);

		assertNotNull(appRef);
		assertEquals(compAppInfo.getAppName(), appRef.getAppName());
		assertEquals(compAppInfo.getComment(), appRef.getComment());
		assertEquals(compAppInfo.getLastModified(), appRef.getLastModified());
		assertEquals(compAppInfo.getAppId().getValue(), appRef.getAppId());
		assertEquals(compAppInfo.getAppType(), appRef.getAppType());
	}

	@Test
	void testCompAppMap()
	{
		ApiLoadingApp app = new ApiLoadingApp();
		app.setAppId(151615L);
		app.setAppName("Calculation application");
		app.setAppType("Computation");
		app.setComment("Application to calculate the flow of a river");
		app.setLastModified(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		app.setManualEditingApp(true);
		Properties properties = new Properties();
		properties.setProperty("algorithm", "processName");
		app.setProperties(properties);

		CompAppInfo compAppInfo = map(app);

		assertNotNull(compAppInfo);
		assertEquals(app.getAppId(), compAppInfo.getAppId().getValue());
		assertEquals(app.getAppName(), compAppInfo.getAppName());
		assertEquals(app.getAppType(), compAppInfo.getAppType());
		assertEquals(app.getComment(), compAppInfo.getComment());
		assertEquals(app.getLastModified(), compAppInfo.getLastModified());
		assertEquals(app.isManualEditingApp(), compAppInfo.getManualEditApp());
		assertEquals(app.getProperties(), compAppInfo.getProperties());
	}

	@Test
	void testStatusMap() throws Exception
	{
		DbKey appId = DbKey.createDbKey(151615L);
		int pid = 12345;
		String host = "localhost";
		Date heartbeat = Date.from(Instant.parse("2021-07-01T00:00:00Z"));
		String status = "Running";
		TsdbCompLock compLock = new TsdbCompLock(appId, pid, host, heartbeat, status);
		compLock.setAppName("Computation Application");

		ApiAppStatus appStatus = map(null, compLock);

		assertNotNull(appStatus);
		assertEquals(compLock.getAppId().getValue(), appStatus.getAppId());
		assertEquals(compLock.getPID(), appStatus.getPid());
		assertEquals(compLock.getHost(), appStatus.getHostname());
		assertEquals(compLock.getHeartbeat(), appStatus.getHeartbeat());
		assertEquals(compLock.getStatus(), appStatus.getStatus());
		assertEquals(compLock.getAppName(), appStatus.getAppName());
	}

	@Test
	void testLoadingAppMap()
	{
		CompAppInfo compAppInfo = new CompAppInfo();
		compAppInfo.setAppName("Dijkstra's Algorithm");
		compAppInfo.setComment("Runs Dijkstra's algorithm on a graph");
		compAppInfo.setLastModified(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		compAppInfo.setAppId(DbKey.createDbKey(151615L));
		compAppInfo.setNumComputations(1); // This field is not mapped to the ApiLoadingApp object
		Properties properties = new Properties();
		properties.setProperty("appType", "computation");
		compAppInfo.setProperties(properties);

		ApiLoadingApp app = mapLoading(compAppInfo);

		assertNotNull(app);
		assertEquals(compAppInfo.getAppName(), app.getAppName());
		assertEquals(compAppInfo.getComment(), app.getComment());
		assertEquals(compAppInfo.getLastModified(), app.getLastModified());
		assertEquals(compAppInfo.getAppId().getValue(), app.getAppId());
		assertEquals(compAppInfo.getAppType(), app.getAppType());
		assertEquals(compAppInfo.getManualEditApp(), app.isManualEditingApp());
		assertEquals(compAppInfo.getProperties(), app.getProperties());
	}
}
