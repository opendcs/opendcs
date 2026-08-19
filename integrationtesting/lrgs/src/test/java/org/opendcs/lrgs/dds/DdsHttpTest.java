package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import io.restassured.http.Cookies;
import jakarta.ws.rs.core.Response;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsInputInterface;

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
        RestAssured.baseURI = "http://127.0.0.1:" + port;

        final String msgData = "Test String.";
        final DcpMsg msgIn = new DcpMsg(DcpMsgFlag.MSG_TYPE_OTHER, msgData.getBytes(StandardCharsets.UTF_8),msgData.length(),0);
        var msgTime = new Date();
        msgIn.setXmitTime(msgTime);
        msgIn.setDomsatTime(msgTime);
        msgIn.setLocalReceiveTime(msgTime);
        final DcpAddress addrIn = new DcpAddress("TEST");
        final LrgsInputInterface dataSource = lrgs.getLrgsInputs().get(0);
        msgIn.setDcpAddress(addrIn);
        lrgs.getArchive().archiveMsg(msgIn, dataSource);
        ((XmlMsgArchive)lrgs.getArchive()).checkpoint();

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
            .queryParam("dcpAddress", "TEST")
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .get("dds/data/query")
            
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body("$.size()", not(is("0")))
            .body("[0].id", is("TEST"))
        ;
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
            .body("[0]", is("HritFile"))
            .body("[1]", is("DDS-Recv:Main"))
            .body("[2]", is("DDS-Recv:Main(Secondary)"))
            .body("[3]", is("DRGS-Recv:Main"))
            .body("[4]", is("HTTP:web:0"))
        ;
    }
}
