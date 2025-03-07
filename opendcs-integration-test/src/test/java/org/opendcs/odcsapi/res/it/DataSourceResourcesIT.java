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
import org.opendcs.odcsapi.beans.ApiDataSource;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
@ExtendWith(DatabaseContextProvider.class)
final class DataSourceResourcesIT extends BaseIT
{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static SessionFilter sessionFilter;
	private static Long sourceId;
	private static Long memberSourceId;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		sessionFilter = new SessionFilter();
		authenticate(sessionFilter);

		ApiDataSource source = getDtoFromResource("datasource_insert_parent_data.json", ApiDataSource.class);

		String dsJson = OBJECT_MAPPER.writeValueAsString(source);

		// Create a new data source member
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(dsJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		memberSourceId = response.body().jsonPath().getLong("dataSourceId");

		ApiDataSource ds = getDtoFromResource("datasource_insert_data.json", ApiDataSource.class);
		ds.getGroupMembers().get(0).setDataSourceId(memberSourceId);

		dsJson = OBJECT_MAPPER.writeValueAsString(ds);

		// Create a new parent data source
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(dsJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		sourceId = response.body().jsonPath().getLong("dataSourceId");
	}

	@AfterEach
	void tearDown()
	{
		// Clean up the data source member
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("datasourceid", memberSourceId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		// Clean up the parent data source
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("datasourceid", sourceId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		logout(sessionFilter);
	}

	@TestTemplate
	void testDataSourceRefs()
	{
		Map<String, Object> expected = getJsonPathFromResource("datasource_get_refs_expected.json").getMap("");
		Map<String, Object> memberExpected = getJsonPathFromResource("datasource_get_refs_member_expected.json").getMap("");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("datasourcerefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract();

		List<Map<String, Object>> actualList = response.body().jsonPath().getList("");
		boolean found = false;
		boolean memberFound = false;
		for (Map<String, Object> actual : actualList)
		{
			if (actual.get("name").equals(expected.get("name")))
			{
				assertEquals(expected.get("name"), actual.get("name"));
				assertEquals(expected.get("type"), actual.get("type"));
				assertEquals(expected.get("arguments"), actual.get("arguments"));
				assertEquals(expected.get("usedBy"), actual.get("usedBy"));
				found = true;
			}
			else if (actual.get("name").equals(memberExpected.get("name")))
			{
				assertEquals(memberExpected.get("name"), actual.get("name"));
				assertEquals(memberExpected.get("type"), actual.get("type"));
				assertEquals(memberExpected.get("arguments"), actual.get("arguments"));
				assertEquals(memberExpected.get("usedBy"), actual.get("usedBy"));
				memberFound = true;
			}
		}
		assertTrue(found && memberFound);
	}

	@TestTemplate
	void testGetDataSource()
	{
		Map<String, Object> expected = getJsonPathFromResource("datasource_insert_data.json").getMap("");

		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("datasourceid", sourceId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		Map<String, Object> actual = response.body().jsonPath().getMap("");
		assertEquals(expected.get("name"), actual.get("name"));
		assertEquals(expected.get("type"), actual.get("type"));
		assertEquals(expected.get("usedBy"), actual.get("usedBy"));
		assertEquals(expected.get("props"), actual.get("props"));
		assertEquals(expected.get("groupMembers[0].dataSourceName"), actual.get("groupMembers[0].dataSourceName"));
	}

	@TestTemplate
	void testPostAndDeleteDataSource() throws Exception
	{
		ApiDataSource dsGroupMem = getDtoFromResource("datasource_post_delete_insert_member_data.json", ApiDataSource.class);
		String dsJson = OBJECT_MAPPER.writeValueAsString(dsGroupMem);

		// Create a new data source member
		ExtractableResponse<Response> response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(dsJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		Long memberDSId = response.body().jsonPath().getLong("dataSourceId");

		ApiDataSource ds = getDtoFromResource("datasource_post_delete_insert_data.json", ApiDataSource.class);
		ds.getGroupMembers().get(0).setDataSourceId(memberDSId);
		dsJson = OBJECT_MAPPER.writeValueAsString(ds);

		// Create a new data source
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.body(dsJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
		;

		Long newSourceId = response.body().jsonPath().getLong("dataSourceId");

		Map<String, Object> expected = getJsonPathFromResource("datasource_post_delete_insert_data.json")
				.getMap("");

		// Get the new data source and assert it matches the expected data
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("datasourceid", newSourceId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.extract()
		;

		Map<String, Object> actual = response.body().jsonPath().getMap("");
		assertEquals(expected.get("name"), actual.get("name"));
		assertEquals(expected.get("type"), actual.get("type"));
		assertEquals(expected.get("usedBy"), actual.get("usedBy"));
		assertEquals(expected.get("props"), actual.get("props"));
		assertEquals(expected.get("groupMembers[0].dataSourceName"), actual.get("groupMembers[0].dataSourceName"));

		// Delete the new data source
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.queryParam("datasourceid", newSourceId)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		// Attempt to retrieve the new data source and assert it is not found
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Authorization", authHeader)
			.filter(sessionFilter)
			.queryParam("datasourceid", newSourceId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("datasource")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NOT_FOUND))
		;
	}
}