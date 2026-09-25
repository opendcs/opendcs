package org.opendcs.odcsapi.res.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiVersion;

import io.restassured.filter.log.LogDetail;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * NOTE: Unfortunately this doesn't test the case where showVersionNonAuth is true.
 * To do so here would require some method of resetting and reloading the database settings.
 * We could do something similar as the DaddsWebHook test, digging into the runtime information. However, the
 * logic here is some what simple so will except the situtation for know until reality proves otherwise.
 * VersionResourcesIT
 */
final class VersionResourcesIT extends BaseApiIT
{
	@BeforeEach
	void setUp()
	{
		setUpCreds();
		authenticate();
    }

    @Test 
    void test_non_auth()
    {
        var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("version")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;

        var version = response.as(ApiVersion.class);
        assertEquals("", version.getVersion());
        assertEquals("", version.getCommitHash());
    }

    @Test
    void test_with_auth()
    {
        var response = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
            .spec(authSpec)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("version")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.CREATED.getStatusCode()))
			.extract()
		;
        var version = response.as(ApiVersion.class);
        assertNotEquals("", version.getVersion());
        assertNotEquals("", version.getCommitHash());
    }
}
