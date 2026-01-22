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
import io.restassured.filter.log.LogDetail;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.beans.ApiSite;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ComputationResourcesIT extends BaseApiIT
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private Long compId;
	private Long siteId;
	private Long appId;
	private Long algId;

	@BeforeEach
	void init() throws Exception
	{
		setUpCreds();
		authenticate();

		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		ApiSite site = getDtoFromResource("computation_site_data.json", ApiSite.class);

		ApiLoadingApp app = getDtoFromResource("computation_app_data.json", ApiLoadingApp.class);
		String appJson = MAPPER.writeValueAsString(app);

		// Insert the app
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(appJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		appId = response.body().jsonPath().getLong("appId");

		// Insert Algorithm
		ApiAlgorithm alg = getDtoFromResource("computation_algorithm_data.json", ApiAlgorithm.class);

		String algJson = MAPPER.writeValueAsString(alg);

		// Insert the algorithm
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(algJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		algId = response.body().jsonPath().getLong("algorithmId");

		String siteJson = MAPPER.writeValueAsString(site);

		// Insert the site
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
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

		siteId = response.body().jsonPath().getLong("siteId");

		comp.getParmList().get(0).setSiteName(site.getPublicName());
		comp.getParmList().get(0).setSiteId(siteId);
		comp.setApplicationName(app.getAppName());
		comp.setAppId(appId);
		comp.setAlgorithmName(alg.getName());
		comp.setAlgorithmId(algId);

		String compJson = MAPPER.writeValueAsString(comp);

		// Insert the computation
		 response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(compJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		compId = response.body().jsonPath().getLong("computationId");
	}

	@AfterEach
	void cleanup()
	{
		// Delete the computation
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", compId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Delete the app
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("appid", appId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("app")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Delete the algorithm
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("algorithmid", algId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Delete the site
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
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

		logout();
	}

	@Test
	void testGetComputationRefs()
	{
		JsonPath expectedJson = getJsonPathFromResource("computation_refs_expected.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;
		final var actualItem = response.body()
									   .jsonPath()
									   .setRootPath("find { it -> it.name == '" + expectedJson.getString("name") + "'}");

		assertNotNull(actualItem);
		assertEquals(expectedJson.getString("name"), actualItem.getString("name"));
		assertEquals(expectedJson.getString("description"), actualItem.getString("description"), () -> "Return was: " + response.asPrettyString());
		assertEquals(expectedJson.getBoolean("enabled"), actualItem.getBoolean("enabled"));
		assertEquals(expectedJson.getString("groupName"), actualItem.getString("groupName"));
		assertEquals(expectedJson.getString("algorithmName"), actualItem.getString("algorithmName"));
		assertEquals(expectedJson.getString("processName"), actualItem.getString("processName"));
	}

	@Test
	void testGetComputationRefsWithFilters() throws Exception
	{
		JsonPath expected = getJsonPathFromResource("computation_refs_expected.json");

		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", siteId)
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", algId)
			.queryParam("process", appId)
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		List<Map<String, Object>> actualList = response.body().jsonPath().getList("");
		assertFalse(actualList.isEmpty());
		Map<String, Object> actualItem = actualList.get(0);
		assertEquals(expected.getString("name"), actualItem.get("name"));
		assertEquals(expected.getString("description"), actualItem.get("description"));
		assertEquals(expected.getBoolean("enabled"), actualItem.get("enabled"));
		assertEquals(expected.getString("groupName"), actualItem.get("groupName"));
		assertEquals(expected.getString("algorithmName"), actualItem.get("algorithmName"));
		assertEquals(expected.getString("processName"), actualItem.get("processName"));
	}

	@Test
	void testGetComputationRefsWithNoMatchingFilters() throws Exception
	{
		ApiComputation comp = getDtoFromResource("computation_insert_data.json", ApiComputation.class);

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", "test group")
			.queryParam("algorithm", comp.getAlgorithmId())
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", false)
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", comp.getAlgorithmId())
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", "bi-annual")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("size()", is(0))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("site", comp.getParmList().get(0).getSiteId())
			.queryParam("datatype", comp.getParmList().get(0).getDataTypeId())
			.queryParam("group", comp.getGroupId())
			.queryParam("algorithm", "25")
			.queryParam("process", comp.getAppId())
			.queryParam("enabled", comp.isEnabled())
			.queryParam("interval", comp.getParmList().get(0).getInterval())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
		;
	}

	@Test
	void testGetComputation()
	{
		JsonPath expected = getJsonPathFromResource("computation_insert_data.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", compId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(expected.getString("name"), actual.getString("name"));
		assertEquals(expected.getString("description"), actual.getString("description"));
		assertEquals(expected.getString("enabled"), actual.getString("enabled"));
		assertEquals(expected.getString("groupName"), actual.getString("groupName"));
		assertEquals(expected.getString("applicationName"), actual.getString("applicationName"));
		assertEquals("Test Algorithm", actual.getString("algorithmName"));
		assertEquals(algId, actual.getLong("algorithmId"));
		assertEquals(appId, actual.getLong("appId"));
		assertEquals(siteId, actual.getLong("parmList[0].siteId"));
		assertEquals(expected.getString("parmList[0].siteName"), actual.getString("parmList[0].siteName"));
		assertEquals(expected.getString("parmList[0].dataType"), actual.getString("parmList[0].dataType"));
		assertEquals(expected.getString("parmList[0].interval"), actual.getString("parmList[0].interval"));
		assertEquals(expected.getString("comment"), actual.getString("comment"));
	}

	@Test
	void testPostAndDeleteComputation() throws Exception
	{
		ApiComputation comp = getDtoFromResource("computation_post_delete_insert_data.json",
				ApiComputation.class);

		String compJson = MAPPER.writeValueAsString(comp);

		// Store the new computation
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(compJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		Long newCompId = response.body().jsonPath().getLong("computationId");

		// Get the new computation and assert it matches the expected data
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.body().jsonPath();
		assertEquals(comp.getName(), actual.getString("name"));
		assertEquals(comp.isEnabled(), actual.getBoolean("enabled"));
		assertEquals(comp.getGroupName(), actual.getString("groupName"));
		assertEquals(comp.getApplicationName(), actual.getString("applicationName"));
		assertEquals(comp.getAlgorithmName(), actual.getString("algorithmName"));
		assertEquals(comp.getComment(), actual.getString("comment"));
		List<Map<String, Object>> actualParmList = actual.getList("parmList");
		int i = 0;
		for (ApiCompParm parm : comp.getParmList())
		{
			Map<String, Object> actualItem = actualParmList.get(i);
			assertEquals(parm.getSiteName(), actualItem.get("siteName"));
			assertEquals(parm.getDataType(), actualItem.get("dataType"));
			assertEquals(parm.getInterval(), actualItem.get("interval"));
			i++;
		}

		// Delete the new computation
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Get the new computation and assert it is not found
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.spec(authSpec)
			.accept(MediaType.APPLICATION_JSON)
			.queryParam("computationid", newCompId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("computation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}
}
