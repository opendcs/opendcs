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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.opendcs.odcsapi.beans.ApiAlgorithm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

final class AlgorithmResourcesIT extends BaseIT
{
	@BeforeEach
	void setUp() throws Exception
	{
		setUpCreds();
		authenticate();
	}

	@TestTemplate
	void getAlgorithmRefs()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("algorithmrefs")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
				.body("", hasItem(getJsonPathFromResource("algorithm_ref_add_to_previous.json").getMap("")))
		;
	}

	@TestTemplate
	void roundTrip() throws Exception
	{
		ApiAlgorithm dto = getDtoFromResource("algorithm_tempstring_check.json", ApiAlgorithm.class);
		//The ID can't be set as it is generated on store
		dto.setAlgorithmId(null);
		authenticate();
		//Assert algorithm can be stored. Update with the new id
		dto = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.body(dto)
			.post("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_CREATED))
			.extract()
			.body()
			.as(ApiAlgorithm.class)
		;

		//Assert algorithm now exists
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("algorithmid", dto.getAlgorithmId())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;

		//Assert algorithm can be deleted
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("algorithmid", dto.getAlgorithmId())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		//Assert algorithm no longer exists
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
			.queryParam("algorithmid", dto.getAlgorithmId())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("algorithm")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NOT_FOUND))
		;
	}
}
