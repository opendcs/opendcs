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


import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.DecodeRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class OdcsapiResourceIT extends BaseIT
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private Properties props;
	private Long platformId;
	private Long siteId;


	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		
		authenticate();

		Properties properties = new Properties();
		properties.setProperty("key", "value");
		properties.setProperty("key2", "value");
		properties.setProperty("key3", "value");
		properties.setProperty("key4", "value");

		props = properties;

		String propertiesJson = MAPPER.writeValueAsString(properties);

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(propertiesJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("tsdb_properties")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
		;

		assertPropertiesInDB(properties);
	}

	@AfterEach
	void tearDown()
	{
		if (platformId != null)
		{
			deletePlatform(platformId);
		}
		if (siteId != null)
		{
			deleteSite(siteId);
		}
	}

	@TestTemplate
	void testGetPropertySpecs()
	{
		JsonPath expected = getJsonPathFromResource("odcsapi_property_specs.json");
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("class", "decodes.datasource.WebDirectoryDataSource")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("propspecs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.jsonPath();
		for (int i = 0; i < expected.getList("").size(); i++)
		{
			Map<String, Object> actualItem = actual.get("[" + i + "]");
			Map<String, Object> expectedItem = expected.get("[" + i + "]");
			assertEquals(expectedItem.get("name"), actualItem.get("name"));
			assertEquals(expectedItem.get("type"), actualItem.get("type"));
			assertEquals(expectedItem.get("description"), actualItem.get("description"));
		}
	}

	@TestTemplate
	void testPostDecode() throws Exception
	{
		DecodeRequest request = getDtoFromResource("odcsapi_decode_request_dto.json", DecodeRequest.class);

		platformId = storePlatform();

		String decodeJson = MAPPER.writeValueAsString(request);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("script", request.getConfig().getScripts().get(0).getName())
			.body(decodeJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("decode")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract();

		JsonNode responseNode = MAPPER.readTree(response.body().asString());
		assertDoesNotThrow(() -> ZonedDateTime.parse(responseNode.get("messageTime").asText()),
				"messageTime in the actual response is not parseable to an Instant");
		JsonNode expected = MAPPER.readTree(getJsonFromResource("odcsapi_decode_response.json"));
		((ObjectNode)expected).remove("messageTime");
		((ObjectNode)responseNode).remove("messageTime");
		assertEquals(expected, responseNode, "The decoded response does not match the expected response");
	}

	private void assertPropertiesInDB(Properties properties)
	{
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsdb_properties")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		// check only the values we've added.
		Properties dbProperties = response.as(Properties.class);
		properties.forEach((k,v) ->
		{
			assertEquals(properties.get(k), dbProperties.get(k));
		});
	}

	private Long storePlatform() throws Exception
	{

		ApiPlatform platform = getDtoFromResource("odcsapi_platform_dto.json", ApiPlatform.class);

		siteId = storeSite();

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

		long configId = response.body().jsonPath().getLong("configId");
		platform.setConfigId(configId);
		platform.setSiteId(siteId);

		String platformJson = MAPPER.writeValueAsString(platform);

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

		Long newPlatformId = response.body().jsonPath().getLong("platformId");

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
		;

		return newPlatformId;
	}

	private void deletePlatform(Long platformId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("platformid", platformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;
	}

	private Long storeSite() throws Exception
	{
		String siteJson = getJsonFromResource("odcsapi_site_dto.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(siteJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		return response.body().jsonPath().getLong("siteId");
	}

	private void deleteSite(Long siteId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("siteid", siteId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;
	}
}
