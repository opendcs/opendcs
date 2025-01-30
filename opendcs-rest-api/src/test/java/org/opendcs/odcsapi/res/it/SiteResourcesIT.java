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

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("integration")
@ExtendWith(DatabaseContextProvider.class)
final class SiteResourcesIT extends BaseIT
{
	private static Long siteId;
	private static SessionFilter sessionFilter;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@BeforeEach
	void setUp() throws Exception
	{
		sessionFilter = new SessionFilter();

		setUpCreds();
		authenticate(sessionFilter);

		String siteJson = getJsonFromResource("site_setup_dto.json");

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

		siteId = response.body().jsonPath().getLong("siteId");
	}

	@AfterEach
	void tearDown()
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
			.statusCode(is(HttpServletResponse.SC_OK))
		;

		logout(sessionFilter);
	}

	@TestTemplate
	void testGetSiteRefs()
	{
		JsonPath expected = getJsonPathFromResource("site_get_refs_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("siterefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("size()", greaterThan(0))
			.body("sitenames", equalTo(expected.get("sitenames")))
			.body("public-name", equalTo(expected.get("public-name")))
			.body("description", equalTo(expected.get("description")))
		;
	}

	@TestTemplate
	void testGetSite()
	{
		JsonPath expected = getJsonPathFromResource("site_get_expected.json");

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("siteid", siteId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("sitenames", equalTo(expected.get("sitenames")))
			.body("publicName", equalTo(expected.get("publicName")))
			.body("description", equalTo(expected.get("description")))
			.body("country", equalTo(expected.get("country")))
			.body("elevation", equalTo(expected.get("elevation")))
			.body("elevUnits", equalTo(expected.get("elevUnits")))
			.body("latitude", equalTo(expected.get("latitude")))
			.body("longitude", equalTo(expected.get("longitude")))
			.body("state", equalTo(expected.get("state")))
			.body("properties.pumpType", equalTo(expected.get("properties.pumpType")))
			.body("properties.pumpSize", equalTo(expected.get("properties.pumpSize")))
			.body("properties.pumpModel", equalTo(expected.get("properties.pumpModel")))
			.body("properties.pumpSerial", equalTo(expected.get("properties.pumpSerial")))
			.body("properties.pumpManufacturer", equalTo(expected.get("properties.pumpManufacturer")))
			.body("properties.pumpInstallDate", equalTo(expected.get("properties.pumpInstallDate").toString()))
			.body("properties.pumpLastMaintDate", equalTo(expected.get("properties.pumpLastMaintDate").toString()))
			.body("properties.pumpMaintInterval", equalTo(expected.get("properties.pumpMaintInterval").toString()))
			.body("properties.pumpMaintNotes", equalTo(expected.get("properties.pumpMaintNotes")))
			.body("properties.pumpMaintIntervalUnits", equalTo(expected.get("properties.pumpMaintIntervalUnits")))
			.body("active", equalTo(expected.get("active")))
		;
	}

	@TestTemplate
	void testPostAndDeleteSite() throws Exception
	{
		ApiSite site = getDtoFromResource("site_post_delete_dto.json", ApiSite.class);
		assertNotNull(site);
		String siteJson = OBJECT_MAPPER.writeValueAsString(site);

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

		Long id = response.body().jsonPath().getLong("siteId");

		ApiSite returnedSite = OBJECT_MAPPER.readValue(siteJson, ApiSite.class);

		assertSiteMatch(site, returnedSite);

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.contentType(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
			.queryParam("siteid", id)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;
	}

	private void assertSiteMatch(ApiSite expected, ApiSite actual)
	{
		// Do not compare the siteId, since it is generated by the db
		assertEquals(expected.getCountry(), actual.getCountry());
		assertEquals(expected.getDescription(), actual.getDescription());
		assertEquals(expected.getElevation(), actual.getElevation());
		assertEquals(expected.getLatitude(), actual.getLatitude());
		assertEquals(expected.getLongitude(), actual.getLongitude());
		assertEquals(expected.getState(), actual.getState());
		assertEquals(expected.getLastModified(), actual.getLastModified());
		assertEquals(expected.getPublicName(), actual.getPublicName());
		assertEquals(expected.getProperties(), actual.getProperties());
		assertEquals(expected.getElevUnits(), actual.getElevUnits());
		assertEquals(expected.getRegion(), actual.getRegion());
		assertEquals(expected.getTimezone(), actual.getTimezone());
		assertEquals(expected.isActive(), actual.isActive());
		assertEquals(expected.getLocationType(), actual.getLocationType());
		assertEquals(expected.getNearestCity(), actual.getNearestCity());
		for (Map.Entry<Object, Object> entry : expected.getProperties().entrySet())
		{
			assertEquals(entry.getValue(), actual.getProperties().get(entry.getKey()));
		}
	}
}