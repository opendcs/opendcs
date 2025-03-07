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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

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

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@Tag("integration")
@ExtendWith(DatabaseContextProvider.class)
final class ConfigResourcesIT extends BaseIT
{
	private static SessionFilter sessionFilter;
	private static Long configId;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		sessionFilter = new SessionFilter();

		authenticate(sessionFilter);

		String configJson = getJsonFromResource("config_input_data.json");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(configJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		configId = response.body().jsonPath().getLong("configId");
	}

	@AfterEach
	void tearDown()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("configid", configId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		logout(sessionFilter);
	}

	@TestTemplate
	void testGetConfigRefs()
	{
		JsonPath expected = getJsonPathFromResource("config_refs_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("configrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("[0].name", equalTo(expected.getString("[0].name")))
			.body("[0].description", equalTo(expected.getString("[0].description")))
			.body("[0].numPlatforms", equalTo(expected.getInt("[0].numPlatforms")))
		;
	}

	@TestTemplate
	void testGetConfig()
	{
		JsonPath expected = getJsonPathFromResource("config_get_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("configid", configId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
			.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("name", equalTo(expected.getString("name")))
			.body("description", equalTo(expected.getString("description")))
			.body("numPlatforms", equalTo(expected.get("numPlatforms")))
			.body("scripts.size()", equalTo(expected.get("scripts.size()")))
			.body("configSensors.size()", equalTo(expected.get("configSensors.size()")))
			.body("configSensors[0].sensorNumber", equalTo(expected.get("configSensors[0].sensorNumber")))
			.body("configSensors[0].sensorName", equalTo(expected.get("configSensors[0].sensorName")))
			.body("configSensors[0].recordingMode", equalTo(expected.get("configSensors[0].recordingMode")))
			.body("configSensors[0].recordingInterval", equalTo(expected.get("configSensors[0].recordingInterval")))
			.body("configSensors[0].timeOfFirstSample", equalTo(expected.get("configSensors[0].timeOfFirstSample")))
			.body("configSensors[0].absoluteMin", equalTo(expected.get("configSensors[0].absoluteMin")))
			.body("configSensors[0].absoluteMax", equalTo(expected.get("configSensors[0].absoluteMax")))
			.body("configSensors[0].properties", equalTo(expected.get("configSensors[0].properties")))
			.body("configSensors[0].usgsStatCode", equalTo(expected.get("configSensors[0].usgsStatCode")))
			.body("configSensors[0].datatypes", equalTo(expected.get("configSensors[0].datatypes")))
			.body("scripts[0].name", equalTo(expected.getString("scripts[0].name")))
			.body("scripts[0].dataOrder", equalTo(expected.getString("scripts[0].dataOrder")))
			.body("scripts[0].headerType", equalTo(expected.getString("scripts[0].headerType")))
			.body("scripts[0].scriptSensors[0].sensorNumber", equalTo(expected.get("scripts[0].scriptSensors[0].sensorNumber")))
			.body("scripts[0].scriptSensors[0].unitConverter.fromAbbr", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.fromAbbr")))
			.body("scripts[0].scriptSensors[0].unitConverter.toAbbr", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.toAbbr")))
			.body("scripts[0].scriptSensors[0].unitConverter.algorithm", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.algorithm")))
			.body("scripts[0].scriptSensors[0].unitConverter.a", equalTo(expected.getFloat("scripts[0].scriptSensors[0].unitConverter.a")))
			.body("scripts[0].scriptSensors[0].unitConverter.b", equalTo(expected.getFloat("scripts[0].scriptSensors[0].unitConverter.b")))
		;
	}

	@TestTemplate
	void testPostAndDeleteConfig() throws Exception
	{
		String configJson = getJsonFromResource("config_post_delete_input_data.json");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(configJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		Long newConfigId = response.body().jsonPath().getLong("configId");

		JsonPath expected = getJsonPathFromResource("config_post_delete_expected.json");

		// Get the config and assert it matches expected JSON
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("name", equalTo(expected.getString("name")))
			.body("description", equalTo(expected.getString("description")))
			.body("numPlatforms", equalTo(expected.get("numPlatforms")))
			.body("scripts.size()", equalTo(expected.get("scripts.size()")))
			.body("configSensors.size()", equalTo(expected.get("configSensors.size()")))
			.body("configSensors[0].sensorNumber", equalTo(expected.get("configSensors[0].sensorNumber")))
			.body("configSensors[0].sensorName", equalTo(expected.get("configSensors[0].sensorName")))
			.body("configSensors[0].recordingMode", equalTo(expected.get("configSensors[0].recordingMode")))
			.body("configSensors[0].recordingInterval", equalTo(expected.get("configSensors[0].recordingInterval")))
			.body("configSensors[0].timeOfFirstSample", equalTo(expected.get("configSensors[0].timeOfFirstSample")))
			.body("configSensors[0].absoluteMin", equalTo(expected.get("configSensors[0].absoluteMin")))
			.body("configSensors[0].absoluteMax", equalTo(expected.get("configSensors[0].absoluteMax")))
			.body("configSensors[0].properties", equalTo(expected.get("configSensors[0].properties")))
			.body("configSensors[0].usgsStatCode", equalTo(expected.get("configSensors[0].usgsStatCode")))
			.body("configSensors[0].datatypes", equalTo(expected.get("configSensors[0].datatypes")))
			.body("scripts[0].name", equalTo(expected.getString("scripts[0].name")))
			.body("scripts[0].dataOrder", equalTo(expected.getString("scripts[0].dataOrder")))
			.body("scripts[0].headerType", equalTo(expected.getString("scripts[0].headerType")))
			.body("scripts[0].scriptSensors[0].sensorNumber", equalTo(expected.get("scripts[0].scriptSensors[0].sensorNumber")))
			.body("scripts[0].scriptSensors[0].unitConverter.fromAbbr", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.fromAbbr")))
			.body("scripts[0].scriptSensors[0].unitConverter.toAbbr", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.toAbbr")))
			.body("scripts[0].scriptSensors[0].unitConverter.algorithm", equalTo(expected.getString("scripts[0].scriptSensors[0].unitConverter.algorithm")))
			.body("scripts[0].scriptSensors[0].unitConverter.a", equalTo(expected.getFloat("scripts[0].scriptSensors[0].unitConverter.a")))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		// Retrieve and assert that the config is no longer there
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NOT_FOUND));
	}
}
