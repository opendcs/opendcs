/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import decodes.db.ConfigSensor;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformSensor;
import ilex.var.TimedVariable;
import ktarbet.usgs.waterdata.Parameter;
import ktarbet.usgs.waterdata.Statistic;
import ktarbet.usgs.waterdata.TestSite;
import ktarbet.usgs.waterdata.TimeSeriesMetadata;

/**
 * Tests for {@link UsgsWaterDataAdapter} using the in-memory
 * {@link TestSite} (site 88888888) — no HTTP calls are made.
 */
class UsgsWaterDataAdapterTest
{
	private static final int DAILY_INTERVAL = 86400;
	private static final int FIFTEEN_MINUTES = 900;

	private final List<TimeSeriesMetadata> metadata = buildTestMetadata();

	/**
	 * Build metadata matching TestSite's four time series.
	 * Mirrors TestSite.generateMetadata() but is accessible from this package.
	 */
	private static List<TimeSeriesMetadata> buildTestMetadata()
	{
		List<TimeSeriesMetadata> list = new ArrayList<>();

		TimeSeriesMetadata daily = new TimeSeriesMetadata();
		daily.id = TestSite.DAILY_DISCHARGE_TS_ID;
		daily.monitoringLocationId = TestSite.MONITORING_LOCATION_ID;
		daily.parameterCode = Parameter.DISCHARGE;
		daily.parameterName = "Discharge";
		daily.statisticId = Statistic.MEAN;
		daily.unitOfMeasure = "ft^3/s";
		daily.computationPeriodIdentifier = "Daily";
		daily.computationIdentifier = "Mean";
		daily.begin = LocalDate.of(2020, 1, 1);
		daily.end = LocalDate.of(2030, 1, 1);
		daily.primary = "Primary";
		list.add(daily);

		TimeSeriesMetadata gage = new TimeSeriesMetadata();
		gage.id = TestSite.CONTINUOUS_GAGE_TS_ID;
		gage.monitoringLocationId = TestSite.MONITORING_LOCATION_ID;
		gage.parameterCode = Parameter.STAGE;
		gage.parameterName = "Gage height";
		gage.statisticId = null;
		gage.unitOfMeasure = "ft";
		gage.computationPeriodIdentifier = "Instantaneous";
		gage.computationIdentifier = "Instantaneous";
		gage.begin = LocalDate.of(2020, 1, 1);
		gage.end = LocalDate.of(2030, 1, 1);
		gage.primary = "Primary";
		list.add(gage);

		TimeSeriesMetadata tempLeft = new TimeSeriesMetadata();
		tempLeft.id = TestSite.CONTINUOUS_TEMP_LEFT_TS_ID;
		tempLeft.monitoringLocationId = TestSite.MONITORING_LOCATION_ID;
		tempLeft.parameterCode = Parameter.WATER_TEMPERATURE;
		tempLeft.parameterName = "Temperature, water";
		tempLeft.statisticId = null;
		tempLeft.unitOfMeasure = "deg C";
		tempLeft.computationPeriodIdentifier = "Instantaneous";
		tempLeft.computationIdentifier = "Instantaneous";
		tempLeft.sublocationIdentifier = "Left Bank";
		tempLeft.begin = LocalDate.of(2020, 1, 1);
		tempLeft.end = LocalDate.of(2030, 1, 1);
		tempLeft.primary = "Primary";
		list.add(tempLeft);

		TimeSeriesMetadata tempRight = new TimeSeriesMetadata();
		tempRight.id = TestSite.CONTINUOUS_TEMP_RIGHT_TS_ID;
		tempRight.monitoringLocationId = TestSite.MONITORING_LOCATION_ID;
		tempRight.parameterCode = Parameter.WATER_TEMPERATURE;
		tempRight.parameterName = "Temperature, water";
		tempRight.statisticId = null;
		tempRight.unitOfMeasure = "deg C";
		tempRight.computationPeriodIdentifier = "Instantaneous";
		tempRight.computationIdentifier = "Instantaneous";
		tempRight.sublocationIdentifier = "Right Bank";
		tempRight.begin = LocalDate.of(2020, 1, 1);
		tempRight.end = LocalDate.of(2030, 1, 1);
		tempRight.primary = "Primary";
		list.add(tempRight);

		return list;
	}

	/** Build a Platform with the given ConfigSensors. */
	private static Platform buildPlatform(ConfigSensor... sensors)
	{
		PlatformConfig pc = new PlatformConfig("test-config");
		for (ConfigSensor cs : sensors)
		{
			cs.platformConfig = pc;
			pc.addSensor(cs);
		}
		Platform p = new Platform();
		p.setConfig(pc);
		return p;
	}

	/** Create a ConfigSensor with a USGS parameter code.
	 *  Uses "usgs" as the DataType standard to match the default
	 *  dataTypeStandard in UsgsWaterDataSource. */
	private static ConfigSensor makeSensor(int num, String paramCode,
		int intervalSec, String statCode)
	{
		ConfigSensor cs = new ConfigSensor(null, num);
		cs.sensorName = "Sensor-" + num;
		cs.recordingInterval = intervalSec;
		cs.addDataType(new DataType("usgs", paramCode));
		if (statCode != null)
			cs.setUsgsStatCode(statCode);
		return cs;
	}

	@Test
	void testDailyDischarge() throws Exception
	{
		ConfigSensor cs = makeSensor(1, Parameter.DISCHARGE,
			DAILY_INTERVAL, Statistic.MEAN);
		Platform p = buildPlatform(cs);

		Date since = Date.from(Instant.parse("2025-03-10T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-03-14T00:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertEquals(1, results.size(), "expected one sensor result");

		UsgsWaterDataAdapter.SensorResult sr = results.get(0);
		assertTrue(sr.daily, "should be daily data");
		assertEquals("ft^3/s", sr.getUnitOfMeasure());

		List<TimedVariable> tvs = sr.toTimedVariables();
		assertEquals(5, tvs.size(), "5 days from Mar 10-14");

		double[] expected = {10, 11, 12, 13, 14};
		for (int i = 0; i < expected.length; i++)
		{
			assertEquals(expected[i], tvs.get(i).getDoubleValue(),
				"day " + (i + 10));
		}
	}

	@Test
	void testContinuousStage() throws Exception
	{
		ConfigSensor cs = makeSensor(1, Parameter.STAGE,
			FIFTEEN_MINUTES, null);
		Platform p = buildPlatform(cs);

		Date since = Date.from(Instant.parse("2025-07-01T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-07-01T03:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertEquals(1, results.size());

		UsgsWaterDataAdapter.SensorResult sr = results.get(0);
		assertFalse(sr.daily, "should be continuous data");
		assertEquals("ft", sr.getUnitOfMeasure());

		List<TimedVariable> tvs = sr.toTimedVariables();
		assertEquals(12, tvs.size(), "3 hours at 15-min = 12 values");

		// values = hour of day: 0,0,0,0, 1,1,1,1, 2,2,2,2
		double[] expected = {0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2};
		for (int i = 0; i < expected.length; i++)
		{
			assertEquals(expected[i], tvs.get(i).getDoubleValue(),
				"value at index " + i);
		}
	}

	// --- Continuous temperature with sublocation ---

	@Test
	void testContinuousTemperatureLeftBank() throws Exception
	{
		ConfigSensor cs = makeSensor(1, Parameter.WATER_TEMPERATURE,
			FIFTEEN_MINUTES, null);
		Platform p = buildPlatform(cs);

		PlatformSensor ps = new PlatformSensor(p, 1);
		ps.setProperty("usgsSubLocation", "Left Bank");
		p.addPlatformSensor(ps);

		Date since = Date.from(Instant.parse("2025-07-01T12:00:00Z"));
		Date until = Date.from(Instant.parse("2025-07-01T13:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertEquals(1, results.size());

		UsgsWaterDataAdapter.SensorResult sr = results.get(0);
		assertFalse(sr.daily);
		assertEquals("deg C", sr.getUnitOfMeasure());

		List<TimedVariable> tvs = sr.toTimedVariables();
		assertEquals(4, tvs.size(), "1 hour at 15-min = 4 values");

		for (TimedVariable tv : tvs)
		{
			assertEquals(12.0, tv.getDoubleValue(), "hour 12 => value 12");
		}
	}

	@Test
	void testContinuousTemperatureRightBank() throws Exception
	{
		ConfigSensor cs = makeSensor(1, Parameter.WATER_TEMPERATURE,
			FIFTEEN_MINUTES, null);
		Platform p = buildPlatform(cs);

		PlatformSensor ps = new PlatformSensor(p, 1);
		ps.setProperty("usgsSubLocation", "Right Bank");
		p.addPlatformSensor(ps);

		Date since = Date.from(Instant.parse("2025-07-01T12:00:00Z"));
		Date until = Date.from(Instant.parse("2025-07-01T13:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertEquals(1, results.size());

		UsgsWaterDataAdapter.SensorResult sr = results.get(0);
		assertFalse(sr.daily);
		assertEquals("deg C", sr.getUnitOfMeasure());

		List<TimedVariable> tvs = sr.toTimedVariables();
		assertEquals(4, tvs.size(), "1 hour at 15-min = 4 values");

		for (TimedVariable tv : tvs)
		{
			assertEquals(12.0, tv.getDoubleValue(), "hour 12 => value 12");
		}
	}

	// --- Combined: all four sensors on one platform ---

	@Test
	void testAllSensorsCombined()
	{
		ConfigSensor discharge = makeSensor(1, Parameter.DISCHARGE,
			DAILY_INTERVAL, Statistic.MEAN);
		ConfigSensor stage = makeSensor(2, Parameter.STAGE,
			FIFTEEN_MINUTES, null);
		ConfigSensor tempLeft = makeSensor(3, Parameter.WATER_TEMPERATURE,
			FIFTEEN_MINUTES, null);
		ConfigSensor tempRight = makeSensor(4, Parameter.WATER_TEMPERATURE,
			FIFTEEN_MINUTES, null);

		Platform p = buildPlatform(discharge, stage, tempLeft, tempRight);

		PlatformSensor psLeft = new PlatformSensor(p, 3);
		psLeft.setProperty("usgsSubLocation", "Left Bank");
		p.addPlatformSensor(psLeft);

		PlatformSensor psRight = new PlatformSensor(p, 4);
		psRight.setProperty("usgsSubLocation", "Right Bank");
		p.addPlatformSensor(psRight);

		Date since = Date.from(Instant.parse("2025-03-10T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-03-12T00:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertEquals(4, results.size(), "all 4 sensors should return data");

		UsgsWaterDataAdapter.SensorResult dailyResult = results.stream()
			.filter(r -> r.daily).findFirst().orElseThrow();
		assertEquals(1, dailyResult.sensorNumber);
		assertEquals("ft^3/s", dailyResult.getUnitOfMeasure());

		long continuousCount = results.stream()
			.filter(r -> !r.daily).count();
		assertEquals(3, continuousCount, "3 continuous sensors");
	}


	@Test
	void testOmittedSensorSkipped()
	{
		ConfigSensor cs = makeSensor(1, Parameter.DISCHARGE,
			DAILY_INTERVAL, Statistic.MEAN);
		cs.setProperty("omit", "true");
		Platform p = buildPlatform(cs);

		Date since = Date.from(Instant.parse("2025-03-10T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-03-14T00:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertTrue(results.isEmpty(), "omitted sensor should produce no results");
	}

	@Test
	void testNoConfigReturnsEmpty()
	{
		Platform p = new Platform();  // no config set

		Date since = Date.from(Instant.parse("2025-01-01T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-01-02T00:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertTrue(results.isEmpty());
	}

	@Test
	void testSensorWithNoMatchingDataTypeSkipped()
	{
		ConfigSensor cs = new ConfigSensor(null, 1);
		cs.sensorName = "NoUsgsType";
		cs.recordingInterval = DAILY_INTERVAL;
		cs.addDataType(new DataType("SHEF-PE", "HG"));
		Platform p = buildPlatform(cs);

		Date since = Date.from(Instant.parse("2025-03-10T00:00:00Z"));
		Date until = Date.from(Instant.parse("2025-03-14T00:00:00Z"));

		List<UsgsWaterDataAdapter.SensorResult> results =
			UsgsWaterDataAdapter.fetchPlatformData(
				p, TestSite.SITE_NUMBER, since, until, "usgs", metadata);

		assertTrue(results.isEmpty(), "no USGS data type => no results");
	}
}
