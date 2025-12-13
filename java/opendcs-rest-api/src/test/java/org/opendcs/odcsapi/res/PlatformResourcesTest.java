package org.opendcs.odcsapi.res;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import decodes.db.DatabaseIO;
import decodes.db.DecodesScript;
import decodes.db.EquipmentModel;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformList;
import decodes.db.PlatformSensor;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.sql.DbKey;
import decodes.xml.DecodesScriptParser;
import opendcs.dao.ScheduleEntryDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.ApiPlatformRef;
import org.opendcs.odcsapi.beans.ApiPlatformSensor;
import org.opendcs.odcsapi.beans.ApiPlatformStatus;
import org.opendcs.odcsapi.beans.ApiTransportMedium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.opendcs.odcsapi.res.PlatformResources.map;
import static org.opendcs.odcsapi.res.PlatformResources.statusListMap;

@ExtendWith(MockitoExtension.class)
final class PlatformResourcesTest
{
	@Mock
	DatabaseIO dbIo;

	@Mock
	ScheduleEntryDAO scheduleEntryDAO;

	@Test
	void testPlatformRefMap() throws Exception
	{
		DbKey platId1 = DbKey.createDbKey(556774L);
		DbKey platId2 = DbKey.createDbKey(557774L);
		PlatformList platformList = new PlatformList();
		Platform plat1 = new Platform();
		plat1.setAgency("USGS");
		plat1.setDescription("Platform 1");
		plat1.setPlatformDesignator("Platform Designator");
		plat1.setConfigName("Platform Configuration");
		plat1.setIsComplete(true);
		plat1.setId(platId1);
		PlatformConfig config = new PlatformConfig();
		config.setId(DbKey.createDbKey(33565L));
		config.configName = "Platform Configuration";
		config.numPlatformsUsing = 11;
		config.description = "A configuration for a platform";
		EquipmentModel model = new EquipmentModel();
		model.model = "Model 1";
		model.description = "An equipment model";
		model.setId(DbKey.createDbKey(555584L));
		model.name = "Equipped model";
		model.company = "RMA";
		Properties properties = new Properties();
		properties.setProperty("prop1", "value1");
		model.properties = properties;
		config.equipmentModel = model;
		Vector<DecodesScript> scripts = new Vector<>();
		DecodesScript script = new DecodesScript
				.DecodesScriptBuilder(new DecodesScriptParser()).platformConfig(config)
				.scriptName("Testing Script").build();
		ScriptSensor sensor = new ScriptSensor(script, 22);
		script.addScriptSensor(sensor);
		scripts.add(script);
		config.decodesScripts = scripts;
		plat1.setConfig(config);
		Vector<TransportMedium> transportMedia = new Vector<>();
		TransportMedium tm = new TransportMedium(plat1);
		tm.setTimeAdjustment(12);
		tm.setBaud(9600);
		tm.setLoggerType("Default");
		tm.channelNum = 20;
		tm.setTimeZone("America/Los_Angeles");
		tm.setMediumId("TM-test");
		tm.setMediumType("Serial");
		tm.setDataBits(8);
		tm.setStopBits(1);
		tm.setParity('N');
		tm.setUsername("user");
		tm.setPassword("password");
		EquipmentModel equipmentModel = new EquipmentModel();
		equipmentModel.model = "Model 1";
		equipmentModel.description = "An equipment model";
		equipmentModel.setId(DbKey.createDbKey(555584L));
		equipmentModel.name = "Equipped model";
		equipmentModel.company = "GEI";
		tm.equipmentModel = equipmentModel;
		TransportMedium tm2 = new TransportMedium(plat1);
		tm2.setTimeAdjustment(10);
		tm2.setBaud(1200);
		tm2.setLoggerType("Custom");
		tm2.setDataBits(18);
		tm2.setStopBits(2);
		tm2.setParity('U');
		tm2.setTimeZone("America/New_York");
		tm2.setMediumId("TM-test2");
		tm2.setMediumType("Analog");
		tm2.channelNum = 22;
		tm2.setUsername("user2");
		tm2.setPassword("password2");
		EquipmentModel equipmentModel2 = new EquipmentModel();
		equipmentModel2.model = "Model 2";
		equipmentModel2.description = "A second equipment model";
		equipmentModel2.setId(DbKey.createDbKey(5554L));
		equipmentModel2.name = "Equipped model";
		equipmentModel2.company = "GEI";
		tm2.equipmentModel = equipmentModel2;
		transportMedia.add(tm);
		transportMedia.add(tm2);
		plat1.transportMedia = transportMedia;

		Platform plat2 = new Platform();
		plat2.setAgency("USBR");
		plat2.setDescription("USBR Platform");
		plat2.setPlatformDesignator("Platform Designator 2");
		plat2.setConfigName("Platform Configuration 2");
		plat2.setIsComplete(true);
		plat2.setId(platId2);
		PlatformConfig config2 = new PlatformConfig();
		config2.setId(DbKey.createDbKey(34565L));
		config2.configName = plat2.getConfigName();
		config2.numPlatformsUsing = 34;
		config2.description = "Configuration data for a platform";
		EquipmentModel model2 = new EquipmentModel();
		model2.model = "Model 2";
		model2.description = "An equipment model";
		model2.setId(DbKey.createDbKey(565584L));
		model2.name = "Equipment model 2";
		model2.company = "GEI";
		Properties properties2 = new Properties();
		properties2.setProperty("prop2", "value2");
		model2.properties = properties2;
		config2.equipmentModel = model;
		Vector<DecodesScript> scripts2 = new Vector<>();
		DecodesScript script2 = new DecodesScript
				.DecodesScriptBuilder(new DecodesScriptParser()).platformConfig(config2)
				.scriptName("Testing Script 2").build();
		ScriptSensor sensor2 = new ScriptSensor(script2, 22);
		script2.addScriptSensor(sensor2);
		scripts2.add(script2);
		config2.decodesScripts = scripts2;
		plat2.setConfig(config2);
		platformList.add(plat1);
		platformList.add(plat2);

		List<ApiPlatformRef> platRefs = map(platformList);

		assertNotNull(platRefs);
		assertFalse(platRefs.isEmpty());
		assertEquals(2, platRefs.size());
		ApiPlatformRef plat1Ref = platRefs.get(0);
		ApiPlatformRef plat2Ref = platRefs.get(1);
		assertNotNull(plat1Ref);
		assertNotNull(plat2Ref);

		assertEquals(plat1.getAgency(), plat1Ref.getAgency());
		assertEquals(plat1.getDescription(), plat1Ref.getDescription());
		assertEquals(plat1.getPlatformDesignator(), plat1Ref.getDesignator());
		assertEquals(plat1.getDisplayName(), plat1Ref.getName());
		assertEquals(plat1.getId().getValue(), plat1Ref.getPlatformId());
		assertEquals(plat1.getConfig().getId().getValue(), plat1Ref.getConfigId());
		assertTransportPropertiesMatch(plat1.getTransportMedia(), plat1Ref.getTransportMedia());
		for (String propKey : plat1.getProperties().stringPropertyNames())
		{
			String expectedValue = plat1.getProperties().getProperty(propKey);
			String actualValue = plat1Ref.getTransportMedia().getProperty(propKey);
			assertEquals(expectedValue, actualValue);
		}
		if (plat1.getSite() != null && plat2Ref.getSiteId() != null)
		{
			assertEquals(plat1.getSite().getId().getValue(), plat2Ref.getSiteId());
		}
		else
		{
			assertNull(plat1.getSite());
			assertEquals(DbKey.NullKey.getValue(), plat1Ref.getSiteId());
		}
		assertEquals(plat2.getAgency(), plat2Ref.getAgency());
		assertEquals(plat2.getDescription(), plat2Ref.getDescription());
		assertEquals(plat2.getPlatformDesignator(), plat2Ref.getDesignator());
		assertEquals(plat2.getConfigName(), plat2Ref.getConfig());
		assertEquals(plat2.getId().getValue(), plat2Ref.getPlatformId());
		assertEquals(plat2.getConfig().getId().getValue(), plat2Ref.getConfigId());
		assertTransportPropertiesMatch(plat2.getTransportMedia(), plat2Ref.getTransportMedia());
		for (String propKey : plat2.getProperties().stringPropertyNames())
		{
			String expectedValue = plat2.getProperties().getProperty(propKey);
			String actualValue = plat2Ref.getTransportMedia().getProperty(propKey);
			assertEquals(expectedValue, actualValue);
		}
		if (plat2.getSite() != null && plat2Ref.getSiteId() != null)
		{
			assertEquals(plat2.getSite().getId().getValue(), plat2Ref.getSiteId());
		}
		else
		{
			assertNull(plat2.getSite());
			assertEquals(DbKey.NullKey.getValue(), plat2Ref.getSiteId());
		}
	}

	@Test
	void testPlatformMap() throws Exception
	{
		DbKey platId1 = DbKey.createDbKey(556774L);
		Platform plat1 = new Platform();
		plat1.setAgency("USGS");
		plat1.setDescription("Platform 1");
		plat1.setPlatformDesignator("Platform Designator");
		plat1.setConfigName("Platform Configuration");
		plat1.setIsComplete(true);
		plat1.setId(platId1);
		Site site = new Site();
		site.setId(DbKey.createDbKey(1234L));
		plat1.setSite(site);
		PlatformSensor platformSensor = new PlatformSensor(plat1, 1);
		platformSensor.setProperty("test", "value");
		platformSensor.setUsgsDdno(1534);
		plat1.platformSensors.add(platformSensor);
		PlatformConfig config = new PlatformConfig();
		config.setId(DbKey.createDbKey(33565L));
		config.configName = plat1.getConfigName();
		config.numPlatformsUsing = 11;
		config.description = "Platform configuration data";
		EquipmentModel model = new EquipmentModel();
		model.model = "Model 1";
		model.description = "Equipment model 1";
		model.setId(DbKey.createDbKey(555584L));
		model.name = "New Model";
		model.company = "RMA";
		Properties properties = new Properties();
		properties.setProperty("prop1", "value1");
		model.properties = properties;
		config.equipmentModel = model;
		Vector<DecodesScript> scripts = new Vector<>();
		DecodesScript script = new DecodesScript
				.DecodesScriptBuilder(new DecodesScriptParser()).platformConfig(config)
				.scriptName("Integration Testing Script").build();
		ScriptSensor sensor = new ScriptSensor(script, 22);
		script.addScriptSensor(sensor);
		scripts.add(script);
		config.decodesScripts = scripts;
		plat1.setConfig(config);
		Vector<TransportMedium> tm = new Vector<>();
		TransportMedium transportMedium = new TransportMedium(plat1);
		transportMedium.setBaud(9600);
		transportMedium.setMediumId("1234567890");
		transportMedium.setMediumType("Sutron Logger");
		transportMedium.setLoggerType("Logger");
		transportMedium.setTimeAdjustment(12);
		transportMedium.setTimeZone("UTC");
		transportMedium.setStopBits(1);
		transportMedium.setDataBits(8);
		transportMedium.setParity('N');
		transportMedium.setDoLogin(true);
		tm.add(transportMedium);
		plat1.transportMedia = tm;

		ApiPlatform plat = map(plat1);
		assertNotNull(plat);

		assertEquals(plat1.getAgency(), plat.getAgency());
		assertEquals(plat1.getDescription(), plat.getDescription());
		assertEquals(plat1.getPlatformDesignator(), plat.getDesignator());
		assertEquals(plat1.getId().getValue(), plat.getPlatformId());
		assertEquals(plat1.getConfig().getId().getValue(), plat.getConfigId());
		assertMatch(plat.getPlatformSensors(), plat1.getPlatformSensors());
		assertMatchMedium(plat.getTransportMedia(), plat1.getTransportMedia());
		assertEquals(plat1.getProperties(), plat.getProperties());
		assertEquals(plat1.getSite().getId().getValue(), plat.getSiteId());
		assertPlatMatch(plat1.getPlatformSensors(), plat.getPlatformSensors());
		assertMatch(plat1.getTransportMedia(), plat.getTransportMedia());
	}

	private void assertPlatMatch(Iterator<PlatformSensor> platSensors, List<ApiPlatformSensor> apiPlatSensors)
	{
		assertEquals(apiPlatSensors.size(), iterSize(platSensors));
		int i = 0;
		while (platSensors.hasNext())
		{
			PlatformSensor platSensor = platSensors.next();
			ApiPlatformSensor apiSensor = apiPlatSensors.get(i);

			assertEquals(platSensor.sensorNumber, apiSensor.getSensorNum());
			assertEquals(platSensor.site.getId().getValue(), apiSensor.getActualSiteId());
			assertEquals(platSensor.getProperties(), apiSensor.getSensorProps());
			assertEquals(platSensor.getUsgsDdno(), apiSensor.getUsgsDdno());

			i++;
		}
	}

	private void assertTransportPropertiesMatch(Iterator<TransportMedium> expectedTransportMedium, Properties transportMedium)
	{
		while(expectedTransportMedium.hasNext())
		{
			final TransportMedium tm = expectedTransportMedium.next();
			assertTrue(transportMedium.containsKey(tm.getMediumType()));
			assertEquals(tm.getMediumId(), transportMedium.getProperty(tm.getMediumType()));
		}
	}

	private void assertMatch(Iterator<TransportMedium> transportMed, List<ApiTransportMedium> apiTransportMed)
	{
		assertEquals(apiTransportMed.size(), iterSize(transportMed));
		int i = 0;
		while (transportMed.hasNext())
		{
			TransportMedium transportMedium = transportMed.next();
			ApiTransportMedium apiTransportMedium = apiTransportMed.get(i);

			assertEquals(transportMedium.getMediumType(), apiTransportMedium.getMediumType());
			assertEquals(transportMedium.getMediumId(), apiTransportMedium.getMediumId());
			assertEquals(transportMedium.scriptName, apiTransportMedium.getScriptName());
			assertEquals(transportMedium.channelNum, apiTransportMedium.getChannelNum());
			assertEquals(transportMedium.assignedTime, apiTransportMedium.getAssignedTime());
			assertEquals(transportMedium.transmitWindow, apiTransportMedium.getTransportWindow());
			assertEquals(transportMedium.transmitInterval, apiTransportMedium.getTransportInterval());
			assertEquals(transportMedium.getTimeZone(), apiTransportMedium.getTimezone());
			assertEquals(transportMedium.getBaud(), apiTransportMedium.getBaud());
			assertEquals(transportMedium.getStopBits(), apiTransportMedium.getStopBits());
			assertEquals(transportMedium.getDataBits(), apiTransportMedium.getDataBits());
			assertEquals(transportMedium.getParity(), apiTransportMedium.getParity().charAt(0));
			assertEquals(transportMedium.getUsername(), apiTransportMedium.getUsername());
			assertEquals(transportMedium.getPassword(), apiTransportMedium.getPassword());
			assertEquals(transportMedium.isDoLogin(), apiTransportMedium.getDoLogin());
			assertEquals(transportMedium.getTimeAdjustment(), apiTransportMedium.getTimeAdjustment());
			assertEquals(transportMedium.getLoggerType(), apiTransportMedium.getLoggerType());

			i++;
		}
	}

	@Test
	void testApiPlatformMap() throws Exception
	{
		ApiPlatform plat = new ApiPlatform();
		plat.setAgency("USGS");
		plat.setDescription("Platform 10");
		plat.setDesignator("Platform Designator");
		plat.setConfigId(33565L);
		plat.setPlatformId(556774L);
		ArrayList<ApiPlatformSensor> platSensors = new ArrayList<>();
		ApiPlatformSensor ps = new ApiPlatformSensor();
		ps.setMax(100.0);
		ps.setMin(0.0);
		ps.setSensorNum(22);
		ps.setActualSiteId(1234L);
		ps.setUsgsDdno(1534);
		Properties props = new Properties();
		props.setProperty("prop1", "value1");
		ps.setSensorProps(props);
		platSensors.add(ps);
		plat.setPlatformSensors(platSensors);
		plat.setSiteId(1234L);
		plat.setLastModified(Date.from(Instant.parse("2021-07-01T12:00:00Z")));
		plat.setProduction(true);
		ApiTransportMedium tm = new ApiTransportMedium();
		tm.setMediumType("serial");
		tm.setMediumId("1");
		tm.setScriptName("Testing Script");
		tm.setChannelNum(1);
		tm.setAssignedTime(12);
		tm.setTransportWindow(10);
		tm.setTransportInterval(5);
		tm.setTimezone("UTC");
		tm.setBaud(9600);
		tm.setStopBits(1);
		tm.setDataBits(8);
		tm.setParity("N");
		tm.setUsername("user");
		tm.setPassword("password");
		tm.setDoLogin(true);
		tm.setTimeAdjustment(12);

		plat.setTransportMedia(Collections.singletonList(tm));

		Platform result = map(plat);

		assertNotNull(result);
		assertEquals(plat.getAgency(), result.getAgency());
		assertEquals(plat.getDescription(), result.getDescription());
		assertEquals(plat.getDesignator(), result.getPlatformDesignator());
		assertEquals(plat.getPlatformId(), result.getId().getValue());
		assertEquals(plat.getConfigId(), result.getConfig().getId().getValue());
		assertMatch(plat.getPlatformSensors(), result.getPlatformSensors());
		assertMatchMedium(plat.getTransportMedia(), result.getTransportMedia());
		assertEquals(plat.getProperties(), result.getProperties());
		result.platformSensors.forEach(s -> assertSame(result, s.platform));
	}

	private static void assertMatch(List<ApiPlatformSensor> apiPlatformSensors, Iterator<PlatformSensor> platformSensors)
	{
		assertEquals(apiPlatformSensors.size(), iterSize(platformSensors));
		int i = 0;
		while (platformSensors.hasNext())
		{
			PlatformSensor platformSensor = platformSensors.next();
			ApiPlatformSensor apiPlatformSensor = apiPlatformSensors.get(i);

			assertEquals(platformSensor.sensorNumber, apiPlatformSensor.getSensorNum());
			assertEquals(platformSensor.site.getId().getValue(), apiPlatformSensor.getActualSiteId());
			assertEquals(platformSensor.getProperties(), apiPlatformSensor.getSensorProps());
			assertEquals(platformSensor.getUsgsDdno(), apiPlatformSensor.getUsgsDdno());

			i++;
		}
	}

	private static void assertMatchMedium(List<ApiTransportMedium> apiTransportMedium, Iterator<TransportMedium> transportMedium)
	{
		assertEquals(apiTransportMedium.size(), iterSize(transportMedium));
		int i = 0;
		while (transportMedium.hasNext())
		{
			TransportMedium tm = transportMedium.next();
			ApiTransportMedium atm = apiTransportMedium.get(i);

			assertEquals(tm.getMediumType(), atm.getMediumType());
			assertEquals(tm.getMediumId(), atm.getMediumId());
			assertEquals(tm.scriptName, atm.getScriptName());
			assertEquals(tm.channelNum, atm.getChannelNum());
			assertEquals(tm.assignedTime, atm.getAssignedTime());
			assertEquals(tm.transmitWindow, atm.getTransportWindow());
			assertEquals(tm.transmitInterval, atm.getTransportInterval());
			assertEquals(tm.getTimeZone(), atm.getTimezone());
			assertEquals(tm.getBaud(), atm.getBaud());
			assertEquals(tm.getStopBits(), atm.getStopBits());
			assertEquals(tm.getDataBits(), atm.getDataBits());
			assertEquals(tm.getParity(), atm.getParity().charAt(0));
			assertEquals(tm.getUsername(), atm.getUsername());
			assertEquals(tm.getPassword(), atm.getPassword());
			assertEquals(tm.isDoLogin(), atm.getDoLogin());
		}
	}

	@Test
	void testPlatformStatusMap() throws Exception
	{
		String siteName = "Platform location";
		String routingSpecName = "Routing Spec 1";

		DbKey scheduleEntryStatusId = DbKey.createDbKey(9987501L);
		DbKey routingSpecId = DbKey.createDbKey(9987502L);
		DbKey platformId = DbKey.createDbKey(9989900L);
		DbKey siteId = DbKey.createDbKey(9987504L);

		Site site = new Site();
		site.setId(siteId);
		SiteName sn = new SiteName(site, "site");
		sn.setNameValue(siteName);
		site.addName(sn);

		ScheduleEntry entry = new ScheduleEntry("Test Entry");
		entry.setRoutingSpecId(routingSpecId);

		doAnswer(invocation -> {
			Platform p = invocation.getArgument(0);
			assertEquals(platformId, p.getId());
			p.setSite(site);
			return null;
		}).when(dbIo).readPlatform(any(Platform.class));

		List<PlatformStatus> statuses = new ArrayList<>();
		PlatformStatus status = new PlatformStatus(platformId);
		status.setDesignator("Platform Designator");
		status.setAnnotation("Platform Annotation");
		status.setChecked(true);
		status.setLastContactTime(Date.from(Instant.parse("2021-07-02T12:00:00Z")));
		status.setSiteName(siteName);
		status.setLastFailureCodes("System Failure 2");
		status.setLastErrorTime(Date.from(Instant.parse("2021-07-01T12:00:00Z")));
		status.setLastRoutingSpecName(routingSpecName);
		status.setLastScheduleEntryStatusId(scheduleEntryStatusId);

		statuses.add(status);

		List<ApiPlatformStatus> apiStatus = statusListMap(dbIo, statuses);

		assertNotNull(apiStatus);
		assertFalse(apiStatus.isEmpty());
		assertEquals(1, apiStatus.size());
		ApiPlatformStatus apiStat = apiStatus.get(0);
		assertEquals(status.getAnnotation(), apiStat.getAnnotation());
		assertEquals(status.getLastContactTime(), apiStat.getLastContact());
		assertEquals(status.getPlatformId().getValue(), apiStat.getPlatformId());
		assertEquals(status.getLastRoutingSpecName(), apiStat.getRoutingSpecName());
		assertEquals(status.getLastScheduleEntryStatusId().getValue(), apiStat.getLastRoutingExecId());

		// Test with dbIo mocked to return a ScheduleEntry
		when(dbIo.makeScheduleEntryDAO()).thenReturn(scheduleEntryDAO);
		when(scheduleEntryDAO.readScheduleEntryByStatusId(scheduleEntryStatusId)).thenReturn(entry);
		doAnswer(invocation -> {
			RoutingSpec rs = invocation.getArgument(0);
			assertEquals(routingSpecId, rs.getId());
			rs.setName(routingSpecName);
			return null;
		}).when(dbIo).readRoutingSpec(any(RoutingSpec.class));

		statuses = new ArrayList<>();
		status = new PlatformStatus(platformId);
		status.setDesignator("Platform Designator");
		status.setAnnotation("Platform Annotation");
		status.setChecked(true);
		status.setLastContactTime(Date.from(Instant.parse("2021-07-02T12:00:00Z")));
		status.setSiteName(siteName);
		status.setLastFailureCodes("System Failure 2");
		status.setLastErrorTime(Date.from(Instant.parse("2021-07-01T12:00:00Z")));
		status.setLastScheduleEntryStatusId(scheduleEntryStatusId);

		statuses.add(status);

		apiStatus = statusListMap(dbIo, statuses);

		assertNotNull(apiStatus);
		assertFalse(apiStatus.isEmpty());
		assertEquals(1, apiStatus.size());
		apiStat = apiStatus.get(0);
		assertEquals(status.getAnnotation(), apiStat.getAnnotation());
		assertEquals(status.getLastContactTime(), apiStat.getLastContact());
		assertEquals(status.getPlatformId().getValue(), apiStat.getPlatformId());
		assertEquals(routingSpecName, apiStat.getRoutingSpecName());
		assertEquals(status.getLastScheduleEntryStatusId().getValue(), apiStat.getLastRoutingExecId());
		assertEquals(status.getPlatformName(), apiStat.getPlatformName());
		assertEquals(status.getPlatformId().getValue(), apiStat.getPlatformId());
		assertEquals(siteId.getValue(), apiStat.getSiteId());
	}

	private static int iterSize(Iterator<?> it)
	{
		int n = 0;
		while(it.hasNext())
		{
			it.next();
			n++;
		}
		return n;
	}
}