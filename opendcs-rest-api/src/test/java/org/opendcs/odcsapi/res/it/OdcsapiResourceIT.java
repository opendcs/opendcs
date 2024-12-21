package org.opendcs.odcsapi.res.it;


import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("integration")
@ExtendWith(DatabaseContextProvider.class)
final class OdcsapiResourceIT extends BaseIT
{
	private static SessionFilter sessionFilter;
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private Properties props;


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
		given()
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
			.body("", equalTo(getJsonPathFromResource("odcsapi_property_specs.json").getList("")))
		;
	}

	// TODO: Finish this test and add expected response data once the new site controller is implemented.
	@TestTemplate
	void testPostDecode() throws Exception
	{
		DecodeRequest request = getDtoFromResource("odcsapi_decode_request_dto.json", DecodeRequest.class);

		request.getRawmsg().setPlatformId(String.valueOf(storePlatform()));

		String decodeJson = MAPPER.writeValueAsString(request);

		given()
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
			.body("", equalTo(getJsonPathFromResource("odcsapi_decode_response.json").getList("")))
		;
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

		platform.setSiteId(storeSite());

		String platformJson = MAPPER.writeValueAsString(platform);

		ExtractableResponse<Response> response = given()
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
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		return response.body().jsonPath().getLong("platformId");
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
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		// TODO: This will not work until the new site controller is in place,
		//  since the id is not currently returned in the response.
		return response.body().jsonPath().getLong("siteId");
	}
}
