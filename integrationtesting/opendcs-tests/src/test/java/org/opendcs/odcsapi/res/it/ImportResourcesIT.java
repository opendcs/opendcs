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
 * Exercises the DECODES import endpoint against a running REST API using a real
 * dbexport-style XML file (same fixture used to seed the other IT databases).
 */
final class ImportResourcesIT extends BaseApiIT
{
	private static final String MROI4_XML = "data/shared/MROI4.xml";

	@BeforeEach
	void setUp()
	{
		setUpCreds();
		authenticate();
	}

	private static String readFixture() throws Exception
	{
		try (InputStream is = ImportResourcesIT.class.getClassLoader().getResourceAsStream(MROI4_XML))
		{
			assertNotNull(is, "Missing test fixture " + MROI4_XML);
			return new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void validateOnlyDecodesImportSucceeds() throws Exception
	{
		String xml = readFixture();

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_XML)
			.spec(authSpec)
			.body(xml)
		.when()
			.post("import/decodes?validateOnly=true")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("importType", equalTo("decodes"))
			.body("validateOnly", is(true))
			.body("success", is(true));
	}

	@Test
	void decodesImportStoresPlatform() throws Exception
	{
		String xml = readFixture();

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_XML)
			.spec(authSpec)
			.body(xml)
		.when()
			.post("import/decodes")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("validateOnly", is(false))
			.body("success", is(true));

		given()
			.accept(MediaType.APPLICATION_JSON)
			.spec(authSpec)
		.when()
			.get("platformrefs")
		.then()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("findAll { it.name.toString().startsWith('MROI4') }.isEmpty()", is(false));
	}

	@Test
	void unsupportedImportTypeReturnsNotImplemented()
	{
		given()
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_XML)
			.spec(authSpec)
			.body("<Database/>")
		.when()
			.post("import/computations?validateOnly=true")
		.then()
			.statusCode(is(Response.Status.NOT_IMPLEMENTED.getStatusCode()));
	}
}
