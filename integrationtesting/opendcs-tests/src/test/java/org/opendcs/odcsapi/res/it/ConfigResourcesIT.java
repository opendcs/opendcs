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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiEquipmentModel;
import org.opendcs.odcsapi.beans.ApiPlatformConfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ConfigResourcesIT extends BaseApiIT
{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static Long configId;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();

		authenticate();

		String configJson = getJsonFromResource("config_input_data.json");

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
	}

	@AfterEach
	void tearDown()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", configId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		logout();
	}

	@Test
	void testGetConfigRefs()
	{
		JsonPath expected = getJsonPathFromResource("config_refs_expected.json");
		final String name = expected.getString("[0].name");
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("configrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("find { it -> it.name == '"+ name +"'}.description", equalTo(expected.getString("[0].description")))
			.body("find { it -> it.name == '"+ name +"'}.numPlatforms", equalTo(expected.getInt("[0].numPlatforms")))
		;
	}

	@Test
	void testGetConfig()
	{
		JsonPath expected = getJsonPathFromResource("config_get_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", configId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
			.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
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

	@Test
	void testPostAndDeleteConfig() throws Exception
	{
		String configJson = getJsonFromResource("config_post_delete_input_data.json");

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

		Long newConfigId = response.body().jsonPath().getLong("configId");

		JsonPath expected = getJsonPathFromResource("config_post_delete_expected.json");

		// Get the config and assert it matches expected JSON
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
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
			.body("configSensors[0].dataTypes", equalTo(expected.get("configSensors[0].dataTypes")))
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
		;

		// Retrieve and assert that the config is no longer there
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", newConfigId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()));
	}

	@Test
	void testConfigEquipmentModelAssignAndClear() throws Exception
	{
		ApiEquipmentModel model = new ApiEquipmentModel();
		model.setName("Config IT Logger");
		model.setEquipmentType("DCP");
		Long equipmentId = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(OBJECT_MAPPER.writeValueAsString(model))
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("equipment")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract().jsonPath().getLong("equipmentId");

		ApiPlatformConfig config = getDtoFromResource("config_post_delete_input_data.json", ApiPlatformConfig.class);
		config.setEquipmentId(equipmentId);
		Long newConfigId = postConfig(config);
		assertEquals(equipmentId, getConfig(newConfigId).getLong("equipmentId"));

		config.setConfigId(newConfigId);
		config.setEquipmentId(null);
		postConfig(config);
		assertNull(getConfig(newConfigId).get("equipmentId"));

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.queryParam("configid", newConfigId)
		.when()
			.delete("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.queryParam("equipmentid", equipmentId)
		.when()
			.delete("equipment")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()));
	}

	private Long postConfig(ApiPlatformConfig config) throws Exception
	{
		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(OBJECT_MAPPER.writeValueAsString(config))
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract().jsonPath().getLong("configId");
	}

	private JsonPath getConfig(Long id)
	{
		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("configid", id)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("config")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract().jsonPath();
	}
}
