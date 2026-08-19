package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
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
        RestAssured.baseURI = "http://127.0.0.1:" + port;
        final AtomicReference<Cookies> session = new AtomicReference<>(null);
        assertResultWithinTimeFrame(value ->
        {
            var request =
                given()
                    .log().all() //ifValidationFails(LogDetail.ALL, true)
                    ;
            System.out.println("Cookies (stored)=" + session.get());
            if (session.get() != null)
            {
                request.cookies(session.get());
            }
            System.out.println(request.toString());
            var ret = request
                .when()
                    .redirects().follow(true)
                    .redirects().max(3)
                    .get("dds/data/next")
                .then()
                    .log().all() //ifValidationFails(LogDetail.ALL, true)
                .extract()
                ;
            if (session.get() == null)
            {
                session.set(ret.detailedCookies());
            }
            System.out.println("response: " + ret.statusCode() + " " + ret.statusLine());
            System.out.println(ret.asPrettyString());
            System.out.println("Message count = " + lrgs.getArchive().getTotalMessageCount());
            System.out.println("Cookies: " + ret.cookies());
            return ret.statusCode() == Response.Status.OK.getStatusCode();
        },
        3, TimeUnit.MINUTES,
        10, TimeUnit.SECONDS,
        "No Data returned within reasonable time frame");
    }
}
