package org.opendcs.odcsapi.res.it;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import decodes.db.DatabaseException;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;
import org.opendcs.odcsapi.fixtures.DatabaseSetupExtension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration-opentsdb-only")
@ExtendWith(DatabaseContextProvider.class)
final class PlatformResourcesIT extends BaseIT
{
	private static SessionFilter sessionFilter;
	private static Long platformId;
	private static Long siteId;
	private static Long netListId;
	private static Long scheduleId;
	private static Long platformStatusId;
	private static Boolean dataLoaded;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		sessionFilter = new SessionFilter();
		authenticate(sessionFilter);

		scheduleId = DbKey.NullKey.getValue();

		if (dataLoaded == null || !dataLoaded)
		{
			String[] files = new String[]{
					"OPEN_TSDB/platform_netlist_insert.xml",
					"OPEN_TSDB/platform_datasource_insert.xml",
					"OPEN_TSDB/platform_routingspec_insert.xml",
					"OPEN_TSDB/platform_schedule_entry_insert.xml",
			};
			DatabaseSetupExtension.loadXMLDataIntoDb(files);

			// get netlistId
			ExtractableResponse<Response> resp = given()
				.log().ifValidationFails(LogDetail.ALL, true)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", authHeader)
				.filter(sessionFilter)
			.when()
				.redirects().follow(true)
				.redirects().max(3)
				.get("netlistrefs")
			.then()
				.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
				.statusCode(is(HttpServletResponse.SC_OK))
				.extract();

			JsonPath jsonPath = resp.body().jsonPath();

			List<Map<String, Object>> netLists = jsonPath.getList("");
			assertFalse(netLists.isEmpty());
			boolean found = false;
			for(Map<String, Object> entry : netLists)
			{
				if(entry.get("name").equals("PlatformTest"))
				{
					netListId = Long.parseLong(entry.get("netlistId").toString());
					found = true;
					break;
				}
			}
			assertTrue(found);

			// get schedule entry id
			resp = given()
				.log().ifValidationFails(LogDetail.ALL, true)
				.accept(MediaType.APPLICATION_JSON)
				.header("Authorization", authHeader)
				.filter(sessionFilter)
			.when()
				.redirects().follow(true)
				.redirects().max(3)
				.get("schedulerefs")
			.then()
				.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
				.statusCode(is(HttpServletResponse.SC_OK))
				.extract()
			;

			jsonPath = resp.body().jsonPath();
			assertNotNull(jsonPath);
			List<Map<String, Object>> scheduleEntries = jsonPath.getList("");
			assertNotNull(scheduleEntries);
			scheduleId = DbKey.NullKey.getValue();

			ScheduleEntryStatus scheduleEntryStatus = new ScheduleEntryStatus(DbKey.NullKey);
			scheduleEntryStatus.setHostname("localhost");
			scheduleEntryStatus.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
			scheduleEntryStatus.setScheduleEntryName("Test Schedule Entry");
			scheduleEntryStatus.setNumMessages(12);
			scheduleEntryStatus.setRunStart(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
			scheduleEntryStatus.setRunStop(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
			scheduleEntryStatus.setRunStatus("Test Status");
			scheduleEntryStatus.setLastModified(Date.from(Instant.parse("2021-01-03T00:00:00Z")));

			for(Map<String, Object> entry : scheduleEntries)
			{
				if(entry.get("name").equals(scheduleEntryStatus.getScheduleEntryName()))
				{
					scheduleId = Long.parseLong(entry.get("schedEntryId").toString());
					break;
				}
			}

			scheduleEntryStatus.setScheduleEntryId(DbKey.createDbKey(scheduleId));

			// Store schedule entry status via extension, since no endpoint for this exists
			// 		and the DbImport class does not accept the ScheduleEntryStatus object
			storeScheduleEntryStatus(scheduleEntryStatus);
		}

		String siteJson = getJsonFromResource("platform_insert_data.json");
		assertNotNull(siteJson);

		siteId = storeSite("platform_site_data.json");
		assertNotNull(siteId);

		String platformJson = siteJson.replace("\"[SITE_ID]\"", siteId.toString());

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.contentType(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
			.body(platformJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		platformId = response.body().jsonPath().getLong("platformId");

		if (dataLoaded == null || !dataLoaded)
		{
			PlatformStatus status = new PlatformStatus(DbKey.createDbKey(platformId));
			status.setSiteName("Platform 10");
			status.setLastRoutingSpecName("TestRoutingSpec");
			status.setLastScheduleEntryStatusId(DbKey.createDbKey(scheduleId));
			status.setLastErrorTime(Date.from(Instant.parse("2021-01-03T00:00:00Z")));
			status.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));
			status.setAnnotation("Test Annotation");
			status.setLastContactTime(Date.from(Instant.parse("2021-01-01T00:00:00Z")));
			status.setLastMessageTime(Date.from(Instant.parse("2021-01-02T00:00:00Z")));

			storePlatformStatus(status);
			platformStatusId = status.getId().getValue();
			dataLoaded = true;
		}
	}

	@AfterEach
	void tearDown() throws DatabaseException
	{
		deletePlatformStatus(DbKey.createDbKey(platformStatusId));
		deleteScheduleEntryStatus(DbKey.createDbKey(scheduleId));

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("platformid", platformId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		tearDownSite(siteId);

		logout(sessionFilter);
	}

	@TestTemplate
	void testGetPlatFormRefs()
	{
		JsonPath expected = getJsonPathFromResource("platform_get_refs_expected.json");

		// Retrieve all platforms
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertNotNull(actual);
		Map<String, Object> actualMap = actual.getMap("");
		actualMap = (Map<String, Object>) actualMap.get(expected.getString("name")); // Name of platform is not stored in Platform object, could cause issues with collision
		assertEquals(expected.get("description"), actualMap.get("description"));
		assertEquals(expected.get("agency"), actualMap.get("agency"));
		assertEquals(expected.get("designator"), actualMap.get("designator"));
		assertEquals(expected.get("production"), actualMap.get("production"));
		assertEquals(expected.get("configId"), actualMap.get("configId"));

		// Retrieve with a tmtype filter
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("tmtype", "goes")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		actual = response.body().jsonPath();
		actualMap = actual.getMap("");
		actualMap = (Map<String, Object>) actualMap.get(expected.getString("name")); // Name of platform is not stored in Platform object, could cause issues with collision
		assertEquals(expected.get("description"), actualMap.get("description"));
		assertEquals(expected.get("agency"), actualMap.get("agency"));
		assertEquals(expected.get("designator"), actualMap.get("designator"));
		assertEquals(expected.get("production"), actualMap.get("production"));
		assertEquals(expected.get("configId"), actualMap.get("configId"));

		// Retrieve with an invalid tmtype to check filtering
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("tmtype", "NOT_VALID")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		actual = response.body().jsonPath();
		assertNotNull(actual);
		assertEquals(0, ((Map<String, Object>) actual.get("")).size());
	}

	@TestTemplate
	void testGetPlatform()
	{
		JsonPath expected = getJsonPathFromResource("platform_get_expected.json");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("platformid", platformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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
	}

	@TestTemplate
	void testPostAndDeletePlatform() throws Exception
	{
		String platformJson = getJsonFromResource("platform_post_delete_insert_data.json");
		Long newSiteId = storeSite("platform_site_post_delete_data.json");
		assertNotNull(newSiteId);
		platformJson = platformJson.replace("\"[SITE_ID]\"", newSiteId.toString());

		// Create a new platform
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.contentType(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
			.body(platformJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		// Get platform ID and load expected JSON
		Long newPlatformId = response.body().jsonPath().getLong("platformId");
		JsonPath expected = getJsonPathFromResource("platform_post_delete_expected.json");

		// Get the platform and assert it matches expected JSON values
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("platformid", newPlatformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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
			.header("Authorization", authHeader)
			.queryParam("platformid", newPlatformId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		// Assert the platform no longer exists
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("platformid", newPlatformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NOT_FOUND))
		;
	}

	@TestTemplate
	void testGetPlatformStats()
	{
		JsonPath expected = getJsonPathFromResource("platform_get_stats_expected.json");
		assertNotNull(expected.getString(""));

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("netlistid", netListId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("platformstat")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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

	private Long storeSite(String jsonPath) throws Exception
	{
		assertNotNull(jsonPath);
		String siteJson = getJsonFromResource(jsonPath);
		assertNotNull(siteJson);

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.contentType(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
			.body(siteJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		return response.body().jsonPath().getLong("siteId");
	}

	private void tearDownSite(Long siteId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("siteid", siteId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("siteid", siteId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NOT_FOUND))
		;
	}
}
