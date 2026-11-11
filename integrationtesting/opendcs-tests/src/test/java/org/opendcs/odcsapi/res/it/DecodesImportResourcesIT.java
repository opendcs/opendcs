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
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.ServletContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.InputStream;

import io.restassured.filter.log.LogDetail;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the DECODES import endpoint against a running REST API.
 */
final class DecodesImportResourcesIT extends BaseApiIT
{
    private static final String MROI4_XML = "data/shared/MROI4.xml";

    @BeforeEach
    void setUp()
    {
        setUpCreds();
        authenticate();
    }

    private static String readDecodesXml() throws Exception
    {
        try (InputStream is = DecodesImportResourcesIT.class.getClassLoader().getResourceAsStream(MROI4_XML))
        {
            assertNotNull(is, "Missing test fixture " + MROI4_XML);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void importStoresPlatform() throws Exception
    {
        String content = readDecodesXml();
        String uploadId = given()
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_XML)
            .spec(authSpec)
            .body(content)
        .when()
            .post("import/preview")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("importType", is("decodes"))
            .body("validateOnly", is(true))
            .body("counts.Platform", is(1))
            .extract().path("uploadId");

        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .accept(MediaType.APPLICATION_JSON)
            .spec(authSpec)
        .when()
            .post("import/" + uploadId + "/confirm")
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
    void pendingUploadsAreDeletedOnLogoutAndSessionExpiry() throws Exception
    {
        for (boolean logout : List.of(true, false))
        {
            var session = tomcat.getTestSessionManager().findSession(
                    io.restassured.specification.SpecificationQuerier.query(authSpec).getCookies().getValue("JSESSIONID"));
            Path directory = ((File) session.getSession().getServletContext().getAttribute(ServletContext.TEMPDIR))
                    .toPath().resolve("opendcs-imports");
            Set<Path> before = pendingFiles(directory);
            String id = given()
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_XML)
                .spec(authSpec)
                .body("<Database/>")
            .when()
                .post("import/preview")
            .then()
                .statusCode(200)
                .extract().path("uploadId");
            Set<Path> created = pendingFiles(directory);
            created.removeAll(before);
            assertEquals(1, created.size());
            if (logout)
            {
                given().spec(authSpec).when().delete("logout").then().statusCode(204);
            }
            else
            {
                session.expire();
            }
            for (Path file : created)
            {
                assertFalse(Files.exists(file), "Session cleanup should delete " + file);
            }
            authenticate();
            given().spec(authSpec).when().post("import/" + id + "/confirm").then().statusCode(404);
        }
    }

    private static Set<Path> pendingFiles(Path directory) throws Exception
    {
        try (var files = Files.list(directory))
        {
            return files.filter(file -> file.toString().endsWith(".upload")).collect(Collectors.toSet());
        }
    }

}
