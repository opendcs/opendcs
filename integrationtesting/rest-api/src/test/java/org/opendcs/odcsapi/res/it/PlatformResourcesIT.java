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

import decodes.db.DatabaseException;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlatformResourcesIT extends BaseIT
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static Long platformId;
	private static Long siteId;
	private static Long netListId;
	private static Long scheduleId;
	private static Long scheduleStatusId;
	private static Long dataSourceId;
	private static Long routingSpecId;
	private static Long platformStatusId;
	private static Long configId;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		authenticate();

		// Insert platform config
		String configJson = getJsonFromResource("platform_config_insert_data.json");
		assertNotNull(configJson);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(configJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		configId = response.body().jsonPath().getLong("configId");

		String platformInsert = getJsonFromResource("platform_insert_data.json");
		assertNotNull(platformInsert);

		siteId = storeSite("platform_site_data.json").getSiteId();
		assertNotNull(siteId);

		String platformJson = platformInsert.replace("\"[SITE_ID]\"", siteId.toString());
		platformJson = platformJson.replace("\"[CONFIG_ID]\"", configId.toString());

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(platformJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		platformId = response.body().jsonPath().getLong("platformId");

		// Insert netlist
		String netlistJson = getJsonFromResource("platform_netlist_insert_data.json");
		assertNotNull(netlistJson);

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(netlistJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("netlist")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		netListId = response.body().jsonPath().getLong("netlistId");

		// Insert data source
		String dataSourceJson = getJsonFromResource("platform_datasource_insert_data.json");
		assertNotNull(dataSourceJson);

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(dataSourceJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		dataSourceId = response.body().jsonPath().getLong("dataSourceId");

		// Insert routing spec
		String routingSpecJson = getJsonFromResource("platform_routingspec_insert_data.json");
		assertNotNull(routingSpecJson);

		routingSpecJson = routingSpecJson.replace("\"[DATA_SOURCE_ID]\"", dataSourceId.toString());
		routingSpecJson = routingSpecJson.replace("[NETLIST_NAME]", "PlatformTest");
		routingSpecJson = routingSpecJson.replace("\"[PLATFORM_ID]\"", platformId.toString());

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(routingSpecJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("routing")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		routingSpecId = response.body().jsonPath().getLong("routingId");

		// Insert schedule entry
		String scheduleEntryJson = getJsonFromResource("platform_schedule_entry_insert_data.json");
		assertNotNull(scheduleEntryJson);

		scheduleEntryJson = scheduleEntryJson.replace("\"[ROUTING_SPEC_ID]\"", routingSpecId.toString());

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(scheduleEntryJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("schedule")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		scheduleId = response.body().jsonPath().getLong("schedEntryId");
	}

	@AfterEach
	void tearDown() throws DatabaseException
	{
		if (scheduleStatusId != null)
		{
			deleteScheduleEntryStatus(DbKey.createDbKey(scheduleStatusId));
		}
		if (platformStatusId != null)
		{
			deletePlatformStatus(DbKey.createDbKey(platformStatusId));
		}

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("scheduleid", scheduleId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("schedule")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("routingid", routingSpecId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("routing")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("datasourceid", dataSourceId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("netlistid", netListId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("netlist")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("platformid", platformId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("configid", configId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		tearDownSite(siteId);

	}

	@TestTemplate
	void testGetPlatFormRefs()
	{
		JsonPath expected = getJsonPathFromResource("platform_get_refs_expected.json");

		// Retrieve all platforms
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertNotNull(actual);
		var nameAndDesignator = expected.getString("name") + "-" + expected.getString("designator");
		final var actualPlatform = actual.setRootPath("find { it -> it.name == '" + nameAndDesignator + "'}");
		log.info("Platform JSON '{}'", actualPlatform.prettyPrint());
		// Name of platform is not stored in Platform object, could cause issues with collision
		assertEquals(expected.getString("description"), actualPlatform.get("description"));
		assertEquals(expected.getString("agency"), actualPlatform.get("agency"));
		assertEquals(expected.getString("designator"), actualPlatform.get("designator"));
		// The transportMedia map is interpreted by the JSON parser as a map of maps, but the actual JSON is a map of objects
		var transportMedia = actualPlatform.getMap("transportMedia");
		assertTrue(transportMedia.containsKey("goes"));
		assertEquals(expected.get("transportMedia.goes"), transportMedia.get("goes"));

		assertEquals(configId, ((Integer) actualPlatform.get("configId")).longValue());

		// Retrieve with a tmtype filter
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("tmtype", "goes")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		actual = response.body().jsonPath();
		nameAndDesignator = expected.getString("name") + "-" + expected.getString("designator");
		final var actualPlatform2 = actual.setRootPath("find { it -> it.name == '" + nameAndDesignator + "'}");

		assertEquals(expected.getString("description"), actualPlatform2.get("description"));
		assertEquals(expected.getString("agency"), actualPlatform2.get("agency"));
		assertEquals(expected.getString("designator"), actualPlatform2.get("designator"));
		// The transportMedia map is interpreted by the JSON parser as a map of maps, but the actual JSON is a map of objects
		transportMedia = actualPlatform2.getMap("transportMedia");
		assertTrue(transportMedia.containsKey("goes"));
		assertEquals(expected.get("transportMedia.goes"), transportMedia.get("goes"));

		assertEquals(configId, ((Integer) actualPlatform2.get("configId")).longValue());

		// Retrieve with an invalid tmtype to check filtering
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("tmtype", "NOT_VALID")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		actual = response.body().jsonPath();
		assertNotNull(actual);
		assertEquals(0, actual.getList("").size());
	}

	@TestTemplate
	void testGetPlatform()
	{
		JsonPath expected = getJsonPathFromResource("platform_get_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("platformid", platformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertNotNull(actual.getString(""));
		assertEquals(expected.getString("name"), actual.getString("name"));
		assertEquals(expected.getString("description"), actual.getString("description"));
		assertEquals(expected.getString("agency"), actual.getString("agency"));
		assertEquals(expected.getString("designator"), actual.getString("designator"));
		assertEquals(expected.getString("production"), actual.getString("production"));
		assertEquals(expected.getString("properties"), actual.getString("properties"));
		assertEquals(expected.getString("transportMedia.mediumType"), actual.getString("transportMedia.mediumType"));
		assertEquals(expected.getString("transportMedia.mediumId"), actual.getString("transportMedia.mediumId"));
		assertEquals(expected.getString("transportMedia.scriptName"), actual.getString("transportMedia.scriptName"));
		assertEquals(expected.getString("transportMedia.channelNum"), actual.getString("transportMedia.channelNum"));
		assertEquals(expected.getString("transportMedia.assignedTime"), actual.getString("transportMedia.assignedTime"));
		assertEquals(expected.getString("transportMedia.transportWindow"), actual.getString("transportMedia.transportWindow"));
		assertEquals(expected.getString("transportMedia.transportInterval"), actual.getString("transportMedia.transportInterval"));
		assertEquals(expected.getString("transportMedia.timeAdjustment"), actual.getString("transportMedia.timeAdjustment"));
		assertEquals(expected.getString("transportMedia.timezone"), actual.getString("transportMedia.timezone"));
		assertEquals(expected.getString("transportMedia.loggerType"), actual.getString("transportMedia.loggerType"));
		assertEquals(expected.getString("transportMedia.baud"), actual.getString("transportMedia.baud"));
		assertEquals(expected.getString("transportMedia.stopBits"), actual.getString("transportMedia.stopBits"));
		assertEquals(expected.getString("transportMedia.dataBits"), actual.getString("transportMedia.dataBits"));
		assertEquals(expected.getString("transportMedia.parity"), actual.getString("transportMedia.parity"));
		assertEquals(expected.getString("transportMedia.doLogin"), actual.getString("transportMedia.doLogin"));
		assertEquals(expected.getString("transportMedia.username"), actual.getString("transportMedia.username"));
		assertEquals(expected.getString("transportMedia.password"), actual.getString("transportMedia.password"));
	}

	@TestTemplate
	void testPostAndDeletePlatform() throws Exception
	{
		// Create platform config
		String platformConfigJson = getJsonFromResource("platform_create_delete_config_insert_data.json");
		assertNotNull(platformConfigJson);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(platformConfigJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		Long newConfigId = response.body().jsonPath().getLong("configId");

		String platformJson = getJsonFromResource("platform_post_delete_insert_data.json");
		Long newSiteId = storeSite("platform_site_post_delete_data.json").getSiteId();
		assertNotNull(newSiteId);
		platformJson = platformJson.replace("\"[SITE_ID]\"", newSiteId.toString());
		platformJson = platformJson.replace("\"[CONFIG_ID]\"", newConfigId.toString());

		// Create a new platform
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(platformJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		// Get platform ID and load expected JSON
		Long newPlatformId = response.body().jsonPath().getLong("platformId");
		JsonPath expected = getJsonPathFromResource("platform_post_delete_expected.json");

		// Get the platform and assert it matches expected JSON values
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("platformid", newPlatformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("name", equalTo(expected.get("name")))
			.body("description", equalTo(expected.get("description")))
			.body("agency", equalTo(expected.get("agency")))
			.body("platformType", equalTo(expected.get("platformType")))
			.body("designator", equalTo(expected.get("designator")))
			.body("production", equalTo(expected.get("production")))
			.body("properties", equalTo(expected.get("properties")))
		;

		// Delete the platform
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("platformid", newPlatformId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Assert the platform no longer exists
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("platformid", newPlatformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;

		// Delete the config
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
			.extract()
		;

		// Delete the site
		tearDownSite(newSiteId);
	}

	@TestTemplate
	void testGetPlatformStats() throws Exception
	{
		ScheduleEntryStatus scheduleEntryStatus = new ScheduleEntryStatus(DbKey.NullKey);
		scheduleEntryStatus.setHostname("localhost");
		scheduleEntryStatus.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
		scheduleEntryStatus.setScheduleEntryName("Test Schedule Entry");
		scheduleEntryStatus.setNumMessages(12);
		scheduleEntryStatus.setRunStart(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		scheduleEntryStatus.setRunStop(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
		scheduleEntryStatus.setRunStatus("Test Status");
		scheduleEntryStatus.setLastModified(Date.from(Instant.parse("2021-01-03T00:00:00Z")));
		scheduleEntryStatus.setScheduleEntryId(DbKey.createDbKey(scheduleId));

		// Store schedule entry status via extension, since no endpoint for this exists
		// 		and the DbImport class does not accept the ScheduleEntryStatus object
		storeScheduleEntryStatus(scheduleEntryStatus);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("scheduleentryid", scheduleId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("routingexecstatus")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		List<Map<String, Object>> responseList = response.body().jsonPath().getList("");
		assertFalse(responseList.isEmpty());
		scheduleStatusId = Long.parseLong(responseList.get(0).get("routingExecId").toString());

		PlatformStatus status = new PlatformStatus(DbKey.createDbKey(platformId));
		status.setSiteName("Platform 10");
		status.forceSetId(DbKey.NullKey);
		status.setPlatformId(DbKey.createDbKey(platformId));
		status.setLastRoutingSpecName("TestRoutingSpec");
		status.setLastScheduleEntryStatusId(DbKey.createDbKey(scheduleStatusId));
		status.setLastErrorTime(Date.from(Instant.parse("2021-01-03T00:00:00Z")));
		status.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
		status.setAnnotation("Test Annotation");
		status.setLastContactTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
		status.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));

		storePlatformStatus(status);
		platformStatusId = status.getId().getValue();

		JsonPath expected = getJsonPathFromResource("platform_get_stats_expected.json");
		assertNotNull(expected.getString(""));

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("netlistid", netListId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformstat")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		List<Map<String, Object>> expectedList = expected.getList("");
		Map<String, Object> expectedItem = expectedList.get(0);
		assertNotNull(actualList);
		boolean found = false;
		for (Map<String, Object> entry : actualList)
		{
			if (entry.get("platformName").equals(expectedItem.get("platformName")))
			{
				assertEquals(expectedItem.get("routingSpecName"), entry.get("routingSpecName"));
				assertEquals(expectedItem.get("lastError"), entry.get("lastError"));
				assertEquals(expectedItem.get("lastMessage"), entry.get("lastMessage"));
				assertEquals(expectedItem.get("annotation"), entry.get("annotation"));
				assertEquals(expectedItem.get("lastContact"), entry.get("lastContact"));
				found = true;
			}
		}
		assertTrue(found);
	}
}
