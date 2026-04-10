package org.opendcs.odcsapi.res.it;

import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.filters.LoggingFilter;

import io.restassured.filter.log.LogDetail;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

class TestLoggingFilter extends BaseApiIT
{
    @Test
    void test_invalid_trace_id_throws_error()
    {
        given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
            .header(LoggingFilter.HEADER_TRACE_ID, "bad trace id")
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }
}
