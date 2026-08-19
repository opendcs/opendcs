package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.extensions.lrgs.LrgsConfig;
import org.opendcs.fixtures.extensions.lrgs.LrgsTestExtension;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.lrgs.http.LrgsHttpInterface;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import jakarta.ws.rs.core.Response;

@ExtendWith(LrgsTestExtension.class)
@LrgsConfig("""
    notimeout=true
    LrgsInput.web.class=org.opendcs.lrgs.http.LrgsHttpInterface
    LrgsInput.web.enabled=true
    LrgsInput.web.port=0
    """)
final class DdsHttpTest
{
    static int port;
    static MsgArchive archive;

    @BeforeAll
    static void setup(LrgsTestInstance lrgs)
    {
        var lrgsInput = (LrgsHttpInterface)lrgs.getLrgsInputs()
                                               .stream()
                                               .filter(i -> i != null)
                                               .peek(i -> System.out.println(i.getInputName()))
                                               .filter(i -> "HTTP:web:0".equals(i.getInputName()))
                                               .findFirst()
                                               .orElseThrow();
        port = lrgsInput.getPort();

    }

    @Test
    void test_next(LrgsTestInstance lrgs)
    {
        RestAssured.baseURI = "http://127.0.0.1:" + port;
        given()
			.log().ifValidationFails(LogDetail.ALL, true)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("dds/data/next")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(oneOf(Response.Status.OK.getStatusCode(), Response.Status.NO_CONTENT.getStatusCode())))
		;
    }
}
