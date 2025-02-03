package org.opendcs.odcsapi.res;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
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
		Platform plat1Ref = platformList.getById(platId1);
		Platform plat2Ref = platformList.getById(platId2);
		assertNotNull(plat1Ref);
		assertNotNull(plat2Ref);

		assertEquals(plat1.getAgency(), plat1Ref.getAgency());
		assertEquals(plat1.getDescription(), plat1Ref.getDescription());
		assertEquals(plat1.getPlatformDesignator(), plat1Ref.getPlatformDesignator());
		assertEquals(plat1.getConfigName(), plat1Ref.getConfigName());
		assertEquals(plat1.getId().getValue(), plat1Ref.getId().getValue());
		assertEquals(plat1.getConfig().getId().getValue(), plat1Ref.getConfig().getId().getValue());
		assertEquals(plat1.getConfig().getEquipmentModel().getId().getValue(), plat1Ref.getConfig().getEquipmentModel().getId().getValue());
		assertEquals(plat1.getConfig().getEquipmentModel().getName(), plat1Ref.getConfig().getEquipmentModel().getName());
		assertPlatMatchIterators(plat1.getPlatformSensors(), plat1Ref.getPlatformSensors());
		assertTransportMatch(plat1.getTransportMedia(), plat1Ref.getTransportMedia());
		assertEquals(plat1.getProperties(), plat1Ref.getProperties());
		assertEquals(plat1.getBriefDescription(), plat1Ref.getBriefDescription());
		assertEquals(plat1.getDisplayName(), plat1Ref.getDisplayName());
		if (plat1.getSite() != null && plat1Ref.getSite() != null)
		{
			assertEquals(plat1.getSite().getId().getValue(), plat1Ref.getSite().getId().getValue());
			assertEquals(plat1.getSite().getDisplayName(), plat1Ref.getSiteName());
		}
		else
		{
			assertNull(plat1.getSite());
			assertNull(plat1Ref.getSite());
		}
		assertEquals(plat2.getAgency(), plat2Ref.getAgency());
		assertEquals(plat2.getDescription(), plat2Ref.getDescription());
		assertEquals(plat2.getPlatformDesignator(), plat2Ref.getPlatformDesignator());
		assertEquals(plat2.getConfigName(), plat2Ref.getConfigName());
		assertEquals(plat2.getId().getValue(), plat2Ref.getId().getValue());
		assertEquals(plat2.getConfig().getId().getValue(), plat2Ref.getConfig().getId().getValue());
		assertEquals(plat2.getConfig().getEquipmentModel().getId().getValue(), plat2Ref.getConfig().getEquipmentModel().getId().getValue());
		assertEquals(plat2.getConfig().getEquipmentModel().getName(), plat2Ref.getConfig().getEquipmentModel().getName());
		assertPlatMatchIterators(plat2.getPlatformSensors(), plat2Ref.getPlatformSensors());
		assertTransportMatch(plat2.getTransportMedia(), plat2Ref.getTransportMedia());
		assertEquals(plat2.getProperties(), plat2Ref.getProperties());
		assertEquals(plat2.getBriefDescription(), plat2Ref.getBriefDescription());
		assertEquals(plat2.getDisplayName(), plat2Ref.getDisplayName());
		if (plat2.getSite() != null && plat2Ref.getSite() != null)
		{
			assertEquals(plat2.getSite().getId().getValue(), plat2Ref.getSite().getId().getValue());
			assertEquals(plat2.getSite().getDisplayName(), plat2Ref.getSiteName());
		}
		else
		{
			assertNull(plat2.getSite());
			assertNull(plat2Ref.getSite());
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

	private void assertPlatMatchIterators(Iterator<PlatformSensor> platSensors, Iterator<PlatformSensor> otherPlatSensors)
	{
		assertEquals(iterSize(platSensors), iterSize(otherPlatSensors));

		while (platSensors.hasNext())
		{
			PlatformSensor sensorA = platSensors.next();
			PlatformSensor sensorB = otherPlatSensors.next();
			assertEquals(sensorA.sensorNumber, sensorB.sensorNumber);
			assertEquals(sensorA.site.getId().getValue(), sensorB.site.getId().getValue());
			assertEquals(sensorA.getProperties(), sensorB.getProperties());
			assertEquals(sensorA.getUsgsDdno(), sensorB.getUsgsDdno());
		}
	}

	private void assertTransportMatch(Iterator<TransportMedium> expectedTransportMedium, Iterator<TransportMedium> transportMedium)
	{
		assertEquals(iterSize(expectedTransportMedium), iterSize(transportMedium));

		while (expectedTransportMedium.hasNext())
		{
			TransportMedium expected = expectedTransportMedium.next();
			TransportMedium actual = transportMedium.next();
			assertEquals(expected.getMediumType(), actual.getMediumType());
			assertEquals(expected.getMediumId(), actual.getMediumId());
			assertEquals(expected.scriptName, actual.scriptName);
			assertEquals(expected.channelNum, actual.channelNum);
			assertEquals(expected.assignedTime, actual.assignedTime);
			assertEquals(expected.transmitWindow, actual.transmitWindow);
			assertEquals(expected.transmitInterval, actual.transmitInterval);
			assertEquals(expected.getTimeZone(), actual.getTimeZone());
			assertEquals(expected.getBaud(), actual.getBaud());
			assertEquals(expected.getStopBits(), actual.getStopBits());
			assertEquals(expected.getDataBits(), actual.getDataBits());
			assertEquals(expected.getParity(), actual.getParity());
			assertEquals(expected.getUsername(), actual.getUsername());
			assertEquals(expected.getPassword(), actual.getPassword());
			assertEquals(expected.isDoLogin(), actual.isDoLogin());
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
		when(scheduleEntryDAO.readScheduleEntry(scheduleEntryStatusId)).thenReturn(entry);
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