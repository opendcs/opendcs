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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.odcsapi.beans.ApiPresentationGroup;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class PresentationResourcesIT extends BaseIT
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private Long parentPresentationId;
	private Long presentationId;

	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		authenticate();

		String presentationJson = getJsonFromResource("presentation_insert_parent_data.json");

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(presentationJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		parentPresentationId = response.jsonPath().getLong("groupId");

		ApiPresentationGroup presentationGroup = getDtoFromResource("presentation_insert_data.json",
				ApiPresentationGroup.class);
		presentationGroup.setInheritsFromId(parentPresentationId);
		presentationJson = MAPPER.writeValueAsString(presentationGroup);

		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(presentationJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		presentationId = response.jsonPath().getLong("groupId");
	}

	@AfterEach
	void tearDown()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", presentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", parentPresentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;
	}

	@Disabled("Will get fixed with PR: https://github.com/opendcs/opendcs/pull/928")
	@TestTemplate
	void testGetPresentationRefs()
	{
		JsonPath expected = getJsonPathFromResource("presentation_get_refs_expected.json");

		Map<String, Object> expectedItem1 = (Map<String, Object>) expected.getList("").get(0);
		Map<String, Object> expectedItem2 = (Map<String, Object>) expected.getList("").get(1);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("presentationrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.jsonPath();
		List<Map<String, Object>> actualList = actual.getList("");
		for (Map<String, Object> actualItem : actualList)
		{
			if (actualItem.get("name").equals(expectedItem1.get("name")))
			{
				assertEquals(expectedItem1.get("name"), actualItem.get("name"));
				assertEquals(expectedItem1.get("inheritsFrom"), actualItem.get("inheritsFrom"));
				assertEquals(expectedItem1.get("production"), actualItem.get("production"));
				assertEquals(expectedItem1.get("lastModified"), actualItem.get("lastModified"));
				assertEquals(-1, actualItem.get("inheritsFromId"));
			}
			else if (actualItem.get("name").equals(expectedItem2.get("name")))
			{
				assertEquals(expectedItem2.get("name"), actualItem.get("name"));
				assertEquals(expectedItem2.get("inheritsFrom"), actualItem.get("inheritsFrom"));
				assertEquals(expectedItem2.get("production"), actualItem.get("production"));
				assertEquals(expectedItem2.get("lastModified"), actualItem.get("lastModified"));
				assertNotEquals(-1, actualItem.get("inheritsFromId"));
				assertEquals((int) actualItem.get("inheritsFromId"), parentPresentationId.intValue());
			}
		}
	}

	@TestTemplate
	void testGetPresentation() throws Exception
	{
		ApiPresentationGroup presentationGroup = getDtoFromResource("presentation_insert_data.json",
				ApiPresentationGroup.class);
		presentationGroup.setInheritsFromId(parentPresentationId);

		String presentationJson = MAPPER.writeValueAsString(presentationGroup);
		JsonPath expected = new JsonPath(presentationJson);

		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", presentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.jsonPath();

		assertMatch(expected, actual);
	}

	@Disabled("Will get fixed with PR: https://github.com/opendcs/opendcs/pull/928")
	@TestTemplate
	void testPostAndDeletePresentation() throws Exception
	{
		String presentationJson = getJsonFromResource("presentation_post_delete_insert_data.json");

		// Store the new presentation group
		var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.body(presentationJson)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

		Long newPresentationId = response.jsonPath().getLong("groupId");

		// Retrieve the new presentation group and assert the data matches expected values
		response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newPresentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;

		JsonPath actual = response.jsonPath();

		JsonPath expected = new JsonPath(presentationJson);

		assertMatch(expected, actual);

		// Delete the new presentation group
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newPresentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		// Retrieve the presentation group and assert it was deleted
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("groupid", newPresentationId)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("presentation")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
		;
	}

	private void assertMatch(JsonPath expected, JsonPath actual)
	{
		Map<String, Object> actualItem = actual.getMap("");
		Map<String, Object> expectedMap = expected.getMap("");
		assertEquals(expectedMap.get("name"), actualItem.get("name"));
		assertEquals(expectedMap.get("inheritsFrom"), actualItem.get("inheritsFrom"));
		assertEquals(expectedMap.get("production"), actualItem.get("production"));
		List<Map<String, Object>> elements = (List<Map<String, Object>>) expectedMap.get("elements");
		List<Map<String, Object>> actualElements = (List<Map<String, Object>>) actualItem.get("elements");
		for(Map<String, Object> stringObjectMap : elements)
		{
			for(Map<String, Object> objectMap : actualElements)
			{
				if(stringObjectMap.get("dataTypeCode").equals(objectMap.get("dataTypeCode")))
				{
					assertEquals(stringObjectMap.get("dataTypeCode"), objectMap.get("dataTypeCode"));
					assertEquals(stringObjectMap.get("max"), objectMap.get("max"));
					assertEquals(stringObjectMap.get("min"), objectMap.get("min"));
					assertEquals(stringObjectMap.get("units"), objectMap.get("units"));
					assertEquals(stringObjectMap.get("dataTypeStd"), objectMap.get("dataTypeStd"));
					assertEquals(stringObjectMap.get("fractionalDigits"), objectMap.get("fractionalDigits"));
				}
			}
		}
	}
}
