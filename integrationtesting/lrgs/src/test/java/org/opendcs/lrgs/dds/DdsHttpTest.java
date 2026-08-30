package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.extensions.lrgs.LrgsConfig;
import org.opendcs.fixtures.extensions.lrgs.LrgsTestExtension;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.lrgs.http.LrgsHttpInput;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookies;
import jakarta.ws.rs.core.Response;
import lrgs.lrgsmain.DcpMonFixtureInput;

@ExtendWith(LrgsTestExtension.class)
@LrgsConfig("""
    notimeout=true
    LrgsInput.web.class=org.opendcs.lrgs.http.LrgsHttpInput
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
        var lrgsInput = (LrgsHttpInput)lrgs.getLrgsInputs()
                                               .stream()
                                               .filter(i -> i != null)
                                               .peek(i -> System.out.println(i.getInputName()))
                                               .filter(i -> "HTTP:web:0".equals(i.getInputName()))
                                               .findFirst()
                                               .orElseThrow();
        port = lrgsInput.getPort();
        RestAssured.baseURI = "http://127.0.0.1:" + port;

        DcpMonFixtureInput fixtures = new DcpMonFixtureInput();
        fixtures.setMsgArchive(lrgs.getArchive());
        fixtures.setInterfaceName("test-fixtures");
        fixtures.initLrgsInput();
        fixtures.enableLrgsInput(true);
        try
        {
            Path netlist = Path.of(System.getProperty("LRGSHOME"), "netlist", "SWT.nl");
            Files.writeString(netlist, """
                CE1F40D4:NIMB Nimbus complete:u
                CE1F2532:BMOB Blue Mountain partial:u
                CE000001:PRTY Parity and low battery:u
                CE000002:MISS Missing transmissions:u
                CE000003:UNKN Missing PDT schedule:u
                """);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        var sp = lrgs.getArchive().getStatusProvider();
        assertNotNull(sp);
        assertTrue(sp.isUsable());
    }

    @Test
    void test_next(LrgsTestInstance lrgs) throws Exception
    {
        
        final AtomicReference<Cookies> session = new AtomicReference<>(null);
        assertResultWithinTimeFrame(value ->
        {
            var request =
                given()
                    .log().ifValidationFails(LogDetail.ALL, true);

            if (session.get() != null)
            {
                request.cookies(session.get());
            }

            var ret = request
                .when()
                    .redirects().follow(true)
                    .redirects().max(3)
                    .get("dds/data/next")
                .then()
                    .log().ifValidationFails(LogDetail.ALL, true)
                .using()
                    .statusCode(is(oneOf(Response.Status.OK.getStatusCode(), Response.Status.NO_CONTENT.getStatusCode())))
                .extract()
                ;
            if (session.get() == null)
            {
                session.set(ret.detailedCookies());
            }
            return ret.statusCode() == Response.Status.OK.getStatusCode();
        },
        3, TimeUnit.MINUTES,
        10, TimeUnit.SECONDS,
        "No Data returned within reasonable time frame");
    }

    @Test
    void test_query(LrgsTestInstance lrgs)
    {
        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .queryParam("dcpAddress", "CE1F40D4")
            .queryParam("source", "GOES")
            .queryParam("since", Instant.now().minusSeconds(24 * 60 * 60).toString())
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .get("dds/data/query")
            
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("total", is(24))
            .body("messages[0].messageType", is("GOES"))
            .body("messages[0].dcpAddress", is("CE1F40D4"))
            .body("messages[0].transmitTime", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$"))
            .body("messages[0].receiveTime", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$"))
            .body("messages[0].channel", is("162W"))
            .body("transmissionSlots.size()", is(24))
            .body("transmissionSlots.findAll { it.status == 'missing' }.size()", is(0))
        ;

        given()
            .queryParam("dcpAddress", "CE1F2532")
            .queryParam("source", "GOES")
            .queryParam("since", Instant.now().minusSeconds(24 * 60 * 60).toString())
        .when()
            .get("dds/data/query")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("messages.size()", is(12))
            .body("transmissionSlots.size()", is(24))
            .body("transmissionSlots.findAll { it.status == 'missing' }.size()", is(12));
    }

    @Test
    void test_sources(LrgsTestInstance lrgs)
    {
        given()
            .log().ifValidationFails(LogDetail.ALL, true)
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .get("dds/sources")
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("$.size()", not(is(0)))
            .body("find { it.name == 'HritFile' }.type", is("HRIT"))
            .body("find { it.name == 'HTTP:web:0' }.type", is("WEB"))
        ;
    }

    @Test
    void test_groups()
    {
        given()
        .when()
            .get("dds/groups")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("find { it.id == 'SWT' }.displayName", is("SWT"));
    }

    @Test
    void test_summary() throws Exception
    {
        assertResultWithinTimeFrame(value -> given()
                .queryParam("data-group", "SWT")
                .when().get("dds/data/summary")
                .jsonPath().getInt("counts.complete") == 1,
            30, TimeUnit.SECONDS,
            1, TimeUnit.SECONDS,
            "Fixture data was not available to the summary endpoint");

        given()
            .queryParam("data-group", "SWT")
        .when()
            .get("dds/data/summary")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("timestamp", matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$"))
            .body("durationHours", is(24))
            .body("counts.complete", is(1))
            .body("counts.partial", is(1))
            .body("counts.parity", is(1))
            .body("counts.missing", is(1))
            .body("counts.unknown", is(1))
            .body("counts.gps", is(1))
            .body("dcpSummaries.CE1F40D4.expectedMessageTotal", is(24))
            .body("dcpSummaries.CE1F40D4.identifiers.find { it.type == 'Description' }.id",
                is("Nimbus complete"))
            .body("dcpSummaries.CE000001.lowBattery", is(true))
            .body("dcpSummaries.CE000001.gpsSync", is(false))
            .body("dcpSummaries.CE1F40D4.gpsSync", is(true))
            .body("dcpSummaries.CE000002.gpsSync", nullValue());

        given()
            .queryParam("data-group", "swt")
        .when()
            .get("dds/data/summary")
        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("counts.complete", is(1));
    }

    @Test
    void test_missing_summary_group()
    {
        given().queryParam("data-group", "DOES-NOT-EXIST")
            .when().get("dds/data/summary")
            .then().statusCode(is(Response.Status.NOT_FOUND.getStatusCode()));
    }
}
