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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("integration")
@ExtendWith(DatabaseContextProvider.class)
final class OdcsapiResourceIT extends BaseIT
{
	private static SessionFilter sessionFilter;
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private Properties props;
	private Long platformId;
	private Long siteId;


	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		sessionFilter = new SessionFilter();
		authenticate(sessionFilter);

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
			.header("Authorization", authHeader)
			.body(propertiesJson)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("tsdb_properties")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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

		logout(sessionFilter);
	}

	@TestTemplate
	void testGetAllProperties()
	{
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsdb_properties")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		Properties dbProperties = response.as(Properties.class);

		assertEquals(props, dbProperties);
	}

	@TestTemplate
	void testGetPropertySpecs()
	{
		JsonPath expected = getJsonPathFromResource("odcsapi_property_specs.json");
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("class", "decodes.datasource.WebDirectoryDataSource")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("propspecs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("script", request.getConfig().getScripts().get(0).getName())
			.body(decodeJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("decode")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
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
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("tsdb_properties")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		Properties dbProperties = response.as(Properties.class);

		assertEquals(properties, dbProperties);
	}

	private Long storePlatform() throws Exception
	{

		ApiPlatform platform = getDtoFromResource("odcsapi_platform_dto.json", ApiPlatform.class);

		siteId = storeSite();

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

		long configId = response.body().jsonPath().getLong("configId");
		platform.setConfigId(configId);
		platform.setSiteId(siteId);

		String platformJson = MAPPER.writeValueAsString(platform);

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
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

		Long newPlatformId = response.body().jsonPath().getLong("platformId");

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
		;

		return newPlatformId;
	}

	private void deletePlatform(Long platformId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("platformid", platformId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("platform")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;
	}

	private Long storeSite() throws Exception
	{
		String siteJson = getJsonFromResource("odcsapi_site_dto.json");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(siteJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		return response.body().jsonPath().getLong("siteId");
	}

	private void deleteSite(Long siteId)
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("siteid", siteId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;
	}
}
