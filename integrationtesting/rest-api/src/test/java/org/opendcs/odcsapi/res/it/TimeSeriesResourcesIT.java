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

package org.opendcs.odcsapi.res.it;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import decodes.cwms.CwmsTsId;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.VarFlags;
import ilex.var.TimedVariable;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TimeSeriesResourcesIT extends BaseIT
{
	private Long tsGroupId;
	private TimeSeriesIdentifier tsId;
	private TimeSeriesIdentifier tsId2;
	private Long siteId;
	private Long siteId2;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		authenticate();

		// create a site
		Site tsSite = map(storeSite("ts_site_insert_data.json"));
		siteId = tsSite.getId().getValue();

		Site tsSite2 = map(storeSite("ts_site_insert_data2.json"));
		siteId2 = tsSite2.getId().getValue();

		// Create an active time series
		CwmsTsId identifier = new CwmsTsId();
		identifier.setUniqueString(String.format("%s.%s.%s.%s.%s.%s", tsSite.getDisplayName(),
				"Area", "Inst", "5Minutes", "0", "Calc"));
		identifier.setSite(tsSite);
		identifier.setStorageUnits("ft2");
		identifier.setActive(true);
		identifier.setInterval("5Minutes");
		identifier.setDuration("0");
		identifier.setDescription("Area at TS test site");
		CTimeSeries ts = new CTimeSeries(identifier);
		tsId = identifier;
		ts.setUnitsAbbr("ft2");

		TimedVariable tv = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:25:00Z")), 2.2, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:30:00Z")), 3.3, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:35:00Z")), 18, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);
		tv = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:40:00Z")), 20, 0);
		tv.setFlags(VarFlags.TO_WRITE);
		ts.addSample(tv);

		storeTimeSeries(ts);

		// Inactive time series to test reference filtering
		CwmsTsId identifier2 = new CwmsTsId();
		identifier2.setUniqueString(String.format("%s.%s.%s.%s.%s.%s", tsSite2.getDisplayName(),
				"Depth", "Total", "30Minutes", "2Hours", "Final"));
		identifier2.setStorageUnits("ft");
		identifier2.setSite(tsSite2);
		identifier2.setActive(false);
		identifier2.setDescription("Depth at TS test site 2");
		identifier2.setDuration("2Hours");
		identifier2.setInterval("30Minutes");
		tsId2 = identifier2;
		CTimeSeries ts2 = new CTimeSeries(identifier2);
		ts2.setUnitsAbbr("ft");

		TimedVariable tv2 = new TimedVariable(Date.from(Instant.parse("2025-02-01T11:45:00Z")), 2.5, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:15:00Z")), 6.7, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2025-02-01T12:45:00Z")), 9.675, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2025-02-01T13:15:00Z")), 10.22, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);
		tv2 = new TimedVariable(Date.from(Instant.parse("2025-02-01T13:45:00Z")), 12.73, 0);
		tv2.setFlags(VarFlags.TO_WRITE);
		ts2.addSample(tv2);

		storeTimeSeries(ts2);

		// get TS DbKeys and save to the identifiers
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		assertFalse(actualList.isEmpty());
		boolean found = false;
		boolean foundInactive = false;
		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("uniqueString").equals(identifier.getUniqueString()))
			{
				tsId.setKey(DbKey.createDbKey(((Integer) actualMap.get("key")).longValue()));
				found = true;
			}
			else if (actualMap.get("uniqueString").equals(identifier2.getUniqueString()))
			{
				tsId2.setKey(DbKey.createDbKey(((Integer) actualMap.get("key")).longValue()));
				foundInactive = true;
			}
		}
		assertTrue(found);
		assertTrue(foundInactive);

		String intervalJson = getJsonFromResource("ts_interval_insert_data.json");

		// create interval
 		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(intervalJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("interval")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
		;

		String groupJson = getJsonFromResource("ts_group_insert_data.json");

		groupJson = groupJson.replace("[TS_1]", tsId.getKey().toString());
		groupJson = groupJson.replace("[TS_2]", tsId2.getKey().toString());
		groupJson = groupJson.replace("[SITE_ID]", siteId.toString());
		groupJson = groupJson.replace("[SITE_ID_2]", siteId2.toString());

		// create ts group
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(groupJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		tsGroupId = response.body().jsonPath().getLong("groupId");
	}

	@AfterEach
	void tearDown() throws Exception
	{
		// Delete the ts group
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", tsGroupId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		deleteTimeSeries(tsId);
		deleteTimeSeries(tsId2);

		tearDownSite(siteId);
		tearDownSite(siteId2);
	}

	@TestTemplate
	void testGetTSRefs()
	{
		JsonPath expected = getJsonPathFromResource("ts_ref_expected.json");
		JsonPath expected2 = getJsonPathFromResource("ts_ref_expected_inactive.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		assertFalse(actualList.isEmpty());

		boolean found = false;
		boolean foundInactive = false;
		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("uniqueString").equals(expected.get("uniqueString")))
			{
				assertEquals(expected.get("description"), actualMap.get("description"));
				assertEquals(expected.get("storageUnits"), actualMap.get("storageUnits"));
				assertEquals(expected.get("active"), actualMap.get("active"));
				found = true;
			}
			else if (actualMap.get("uniqueString").equals(expected2.get("uniqueString")))
			{
				assertEquals(expected2.get("description"), actualMap.get("description"));
				assertEquals(expected2.get("storageUnits"), actualMap.get("storageUnits"));
				assertEquals(expected2.get("active"), actualMap.get("active"));
				foundInactive = true;
			}
		}
		assertTrue(found);
		assertTrue(foundInactive);

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("active", true)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		actual = response.body().jsonPath();
		actualList = actual.getList("");
		found = false;
		foundInactive = false;
		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("uniqueString").equals(expected.get("uniqueString")))
			{
				assertEquals(expected.get("uniqueString"), actualMap.get("uniqueString"));
				assertEquals(expected.get("description"), actualMap.get("description"));
				assertEquals(expected.get("storageUnits"), actualMap.get("storageUnits"));
				assertEquals(expected.get("active"), actualMap.get("active"));
				found = true;
			} else if (actualMap.get("uniqueString").equals(expected2.get("uniqueString")))
			{
				foundInactive = true;
			}
		}
		assertTrue(found);
		assertFalse(foundInactive);
	}

	@TestTemplate
	void testGetTSSpec()
	{
		JsonPath expected = getJsonPathFromResource("ts_spec_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("key", tsId.getKey().getValue())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsspec")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(expected.getString("tsid.uniqueString"), actual.getString("tsid.uniqueString"));
		assertEquals(expected.getString("tsid.description"), actual.getString("tsid.description"));
		assertEquals(expected.getString("tsid.storageUnits"), actual.getString("tsid.storageUnits"));
		assertEquals(expected.getBoolean("tsid.active"), actual.getBoolean("tsid.active"));
		assertEquals(expected.getString("location"), actual.getString("location"));
		assertEquals(expected.getString("interval"), actual.getString("interval"));
		assertEquals(expected.getString("duration"), actual.getString("duration"));
		assertEquals(expected.getString("version"), actual.getString("version"));
	}

	@TestTemplate
	void testGetTSData()
	{
		// CWMS database will return data in default units for the parameter.
		// There are two expected JSON files to account for these differences
		JsonPath expected = getJsonPathFromResource("ts_data_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("key", tsId.getKey().getValue())
			.queryParam("start", "2000/365/12:00:00")
			.queryParam("end", "2030/360/12:45:00")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsdata")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(expected.getString("tsid.uniqueString"), actual.getString("tsid.uniqueString"));
		assertEquals(expected.getString("tsid.description"), actual.getString("tsid.description"));
		assertEquals(expected.getString("tsid.storageUnits"), actual.getString("tsid.storageUnits"));
		assertEquals(expected.getBoolean("tsid.active"), actual.getBoolean("tsid.active"));
		List<Map<String, Object>> expectedData = expected.getList("values");
		List<Map<String, Object>> actualData = actual.getList("values");
		assertEquals(expectedData.size(), actualData.size());
		for (int i = 0; i < expectedData.size(); i++)
		{
			assertEquals(expectedData.get(i).get("sampleTime"), actualData.get(i).get("sampleTime"));
			assertEquals(expectedData.get(i).get("value"), actualData.get(i).get("value"));
			assertEquals(expectedData.get(i).get("flags"), actualData.get(i).get("flags"));
		}

		// Test with an invalid key
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("key", DbKey.NullKey.getValue())
			.queryParam("start", "2000/365/12:00:00")
			.queryParam("end", "2030/360/12:45:00")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsdata")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;

	}

	@TestTemplate
	void testGetIntervals()
	{
		JsonPath expected = getJsonPathFromResource("ts_interval_insert_data.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("intervals")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();

		List<Map<String, Object>> actualList = actual.getList("");
		Map<String, Object> expectedMap = expected.getMap("");

		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("name").equals(expectedMap.get("name")))
			{
				assertEquals(expectedMap.get("name"), actualMap.get("name"));
				assertEquals(expectedMap.get("calConstant").toString().toUpperCase(),
						actualMap.get("calConstant").toString().toUpperCase());
				assertEquals(expectedMap.get("calMultiplier"), actualMap.get("calMultiplier"));
			}
		}
	}

	@TestTemplate
	void testPostAndRetrieveInterval() throws Exception
	{
		String intervalJson = getJsonFromResource("ts_interval_insert_data.json");
		JsonPath expected = new JsonPath(intervalJson);

		// create interval
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(intervalJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("interval")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		Long newIntervalId = response.body().jsonPath().getLong("intervalId");

		// retrieve intervals and assert it is in the list
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("id", newIntervalId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("intervals")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath(); // this will be a list

		List<Map<String, Object>> actualList = actual.getList("");
		Map<String, Object> expectedMap = expected.getMap("");

		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("name").equals(expectedMap.get("name")))
			{
				assertEquals(expectedMap.get("name"), actualMap.get("name"));
				assertEquals(expectedMap.get("calConstant").toString().toUpperCase(),
						actualMap.get("calConstant").toString().toUpperCase());
				assertEquals(expectedMap.get("calMultiplier"), actualMap.get("calMultiplier"));
			}
		}
	}

	@TestTemplate
	void testGetTSGroupRefs()
	{
		JsonPath expected = getJsonPathFromResource("ts_group_ref_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsgrouprefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		assertFalse(actualList.isEmpty());
		boolean found = false;
		for (Map<String, Object> actualMap : actualList)
		{
			if (actualMap.get("groupName").equals(expected.get("groupName")))
			{
				assertEquals(expected.get("description"), actualMap.get("description"));
				assertEquals(expected.get("groupType"), actualMap.get("groupType"));
				found = true;
			}
		}
		assertTrue(found);
	}

	@TestTemplate
	void testGetTSGroup()
	{
		JsonPath expected = getJsonPathFromResource("ts_group_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", tsGroupId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(expected.getString("groupName"), actual.getString("groupName"));
		assertEquals(expected.getString("description"), actual.getString("description"));
		assertEquals(expected.getString("groupType"), actual.getString("groupType"));
		List<Map<String, Object>> expectedTsList = expected.getList("tsIds");
		List<Map<String, Object>> actualTsList = actual.getList("tsIds");
		assertTrue(expectedTsList.size() <= actualTsList.size());
		int tsCount = 0;
		for (int i = 0; i < actualTsList.size(); i++)
		{
			if (expectedTsList.get(i).get("uniqueString").equals(actualTsList.get(i).get("uniqueString")))
			{
				assertEquals(expectedTsList.get(i).get("description"), actualTsList.get(i).get("description"));
				assertEquals(expectedTsList.get(i).get("storageUnits"), actualTsList.get(i).get("storageUnits"));
				tsCount++;
				// cannot compare active flag because it is not returned in the TimeSeriesIdentifier
			}
		}
		assertEquals(expectedTsList.size(), tsCount);

		List<Map<String, Object>> expectedSiteList = expected.getList("groupSites");
		List<Map<String, Object>> actualSiteList = actual.getList("groupSites");
		assertTrue(expectedSiteList.size() <= actualSiteList.size());
		int siteCount = 0;
		for (int i = 0; i < actualSiteList.size(); i++)
		{
			if (expectedSiteList.get(i).get("publicName").equals(actualSiteList.get(i).get("publicName")))
			{
				assertEquals(expectedSiteList.get(i).get("description"), actualSiteList.get(i).get("description"));
				siteCount++;
			}
		}
		assertEquals(expectedSiteList.size(), siteCount);
	}

	@TestTemplate
	void testPostAndDeleteTSGroup() throws Exception
	{
		String groupJson = getJsonFromResource("ts_group_post_delete_insert_data.json");

		// create ts group
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(groupJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		Long newTSGroupId = response.body().jsonPath().getLong("groupId");

		// retrieve ts group and assert data matches
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newTSGroupId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		JsonPath expected = getJsonPathFromResource("ts_group_post_delete_expected.json");

		assertEquals(expected.getString("groupName"), actual.getString("groupName"));
		assertEquals(expected.getString("description"), actual.getString("description"));
		assertEquals(expected.getString("groupType"), actual.getString("groupType"));

		// delete ts group
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newTSGroupId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// retrieve ts group and assert not found
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newTSGroupId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsgroup")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}
}
