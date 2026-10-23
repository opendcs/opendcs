/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
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

import java.nio.charset.StandardCharsets;
import java.io.InputStream;

import io.restassured.filter.log.LogDetail;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the computations import endpoint against a running REST API.
 */
final class ComputationsImportResourcesIT extends BaseApiIT
{
	private static final String COPYTEST_COMP_XML = "data/Comps/CopyTest/Test1/CopyTestComp.xml";

	@BeforeEach
	void setUp()
	{
		setUpCreds();
		authenticate();
	}

	private static String readFixture() throws Exception
	{
		try (InputStream is = ComputationsImportResourcesIT.class.getClassLoader().getResourceAsStream(COPYTEST_COMP_XML))
		{
			assertNotNull(is, "Missing test fixture " + COPYTEST_COMP_XML);
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void importStoresComputation() throws Exception
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_XML)
			.spec(authSpec)
			.body(readFixture())
		.when()
			.post("import/computations?createTimeSeries=true")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("importType", equalTo("computations"))
			.body("validateOnly", is(false))
			.body("success", is(true));

		given()
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.get("computationrefs")
		.then()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("findAll { it.name.toString() == 'Test1CopyTest' }.isEmpty()", is(false));
	}
}
