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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TsdbCompLock;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import opendcs.dai.LoadingAppDAI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AppResourcesIT extends BaseApiIT
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static Long appid = null;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		authenticate();

		String appJson = getJsonFromResource("app_insert_data.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(appJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(jakarta.ws.rs.core.Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		appid = response.body().jsonPath().getLong("appId");
	}

	@AfterEach
	void tearDown()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("appid", appid)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

	}

	@Test
	void testGetAppRefs()
	{
		JsonPath expected = getJsonPathFromResource("app_get_refs_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("apprefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		String expectedAppName = expected.get("appName").toString();

		boolean found = false;
		for(Map<String, Object> actualApp : actualList)
		{
			String actualAppName = actualApp.get("appName").toString();
			if(actualAppName.equalsIgnoreCase(expectedAppName))
			{
				assertEquals(actualAppName, expectedAppName);
				assertEquals(actualApp.get("appType").toString(),
						expected.get("appType").toString());
				assertEquals(actualApp.get("comment").toString(),
						expected.get("comment").toString());
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	@Test
	void testGetApp()
	{
		JsonPath expected = getJsonPathFromResource("app_get_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("appid", appid)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("appName", equalTo(expected.get("appName")))
			.body("appType", equalTo(expected.get("appType")))
			.body("comment", equalTo(expected.get("comment")))
			.body("lastModified", notNullValue())
		;
	}

	@Test
	void testPostAndDeleteApp() throws Exception
	{
		String appJson = getJsonFromResource("app_post_delete_insert_data.json");

		// Store app
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(appJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract();

		// Get app ID from response
		Long appId = response.body().jsonPath().getLong("appId");

		JsonPath expected = getJsonPathFromResource("app_post_delete_expected.json");

		// Retrieve the app
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("appid", appId)
			
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("appName", equalTo(expected.get("appName")))
			.body("appType", equalTo(expected.get("appType")))
			.body("comment", equalTo(expected.get("comment")))
			.body("lastModified", notNullValue())
			.body("manualEditingApp", equalTo(expected.get("manualEditingApp")))
			.body("properties.startCmd", equalTo(expected.get("properties.startCmd")))
		;

		// Delete app
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.queryParam("appid", appId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Assert that the app was deleted
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("appid", appId)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}

	@Test
	void testGetAppStatus() throws Exception
	{
		ApiAppStatus status = getDtoFromResource("app_stat_expected.json", ApiAppStatus.class);
		ObjectMapper mapper = new ObjectMapper();
		String appJson = mapper.writeValueAsString(status);
		JsonPath expected = new JsonPath(appJson);
		// Create App lock
		CompAppInfo compAppInfo = new CompAppInfo();
		compAppInfo.setAppId(DbKey.createDbKey(appid));
		compAppInfo.setAppName("Computation Application");
		int pid = status.getPid().intValue();
		String host = status.getHostname();
		TsdbCompLock compLock = getCompLock(compAppInfo, pid, host);
		assertNotNull(compLock);

		// Get app status, assert that it matches the expected JSON
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("appstat")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		// Extract response and app ID, assert that the app matches the expected JSON
		JsonPath actual = response.body().jsonPath();
		log.debug("{}", actual.prettyPrint());
		log.debug("Looking for {}", status.getAppName());
		actual = actual.setRootPath("find { it -> it.appName == '" + status.getAppName() + "'}");
		assertNotNull(actual.get("appId"), "Application status was not returned.");
		// ID not compared as we don't actually care what it is.
		assertEquals(host, actual.get("hostname"));
		assertEquals(expected.get("eventPort"), (Integer)actual.get("eventPort"));
		assertEquals(expected.get("status"), (String)actual.get("status"));
		assertEquals(expected.get("appName"), (String)actual.get("appName"));
		assertEquals(expected.get("pid"), (Integer)actual.get("pid"));
		// App type is not returned by the API

		// Release app lock
		releaseCompLock(compLock);

		// assert that the app is stopped
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("appstat")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("findAll { it -> it.name=='" + status.getAppName() + "'}.size()", is(0))
		;
	}

	TsdbCompLock getCompLock(CompAppInfo compAppInfo, int pid, String host) throws DatabaseException
	{
		try (LoadingAppDAI dai = getTsdb().makeLoadingAppDAO())
		{
			return dai.obtainCompProcLock(compAppInfo, pid, host);
		}
		catch (Throwable thr)
		{
			throw new DatabaseException("Error getting comp lock", thr);
		}
	}

	void releaseCompLock(TsdbCompLock compLock) throws DatabaseException
	{
		try (LoadingAppDAI dai = getTsdb().makeLoadingAppDAO())
		{
			dai.releaseCompProcLock(compLock);
		}
		catch (Throwable thr)
		{
			throw new DatabaseException("Error releasing comp lock", thr);
		}
	}
}
