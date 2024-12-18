package org.opendcs.odcsapi.res.it;

import java.sql.Date;
import java.time.Instant;
import java.util.HashMap;
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
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

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

		ApiSite site = new ApiSite();
		site.setPublicName("Pump Site");
		site.setElevUnits("m");
		site.setCountry("US");
		site.setLatitude("0.0");
		site.setLocationtype("PUMP");
		site.setLongitude("0.0");
		site.setActive(true);
		site.setDescription("Pump located in NM, USA");
		site.setElevation(56.3);
		site.setLastModified(Date.from(Instant.parse("2021-01-01T15:45:10Z")));
		site.setNearestCity("Albuquerque");
		site.setState("NM");
		site.setTimezone("UTC");
		HashMap<String, String> siteNames = new HashMap<>();
		siteNames.put("PUMP", "Pump Site");
		site.setSitenames(siteNames);

		String siteJson = OBJECT_MAPPER.writeValueAsString(site);

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
		;
	}

	@TestTemplate
	void testGetSite()
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
			.get("site")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;
	}

	@TestTemplate
	void testPostAndDeleteSite() throws Exception
	{
		ApiSite site = new ApiSite();
		site.setPublicName("Pool Site");
		site.setElevUnits("m");
		site.setCountry("US");
		site.setLatitude("38.56");
		site.setLongitude("-121.72");
		site.setLocationtype("LOCK");
		site.setActive(true);
		site.setDescription("LOCK Site");
		site.setElevation(56.3);
		site.setLastModified(Date.from(Instant.parse("2021-02-01T15:45:10Z")));
		site.setNearestCity("Davis");
		site.setTimezone("UTC");
		site.setState("CA");
		HashMap<String, String> siteNames = new HashMap<>();
		siteNames.put("en", "Test Lock");
		site.setSitenames(siteNames);
		site.setSiteId(18817L);

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
}