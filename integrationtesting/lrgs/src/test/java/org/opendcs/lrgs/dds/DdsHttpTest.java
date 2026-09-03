package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.extensions.lrgs.LrgsConfig;
import org.opendcs.fixtures.extensions.lrgs.LrgsTestExtension;
import org.opendcs.fixtures.inet.InterceptingInetAddressResolver;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.lrgs.http.LrgsHttpInput;

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
    LrgsInput.web.class=org.opendcs.lrgs.http.LrgsHttpInput
    LrgsInput.web.enabled=true
    LrgsInput.web.port=0
    LrgsInput.web.daddsWebHook_0=testHook
    LrgsInput.web.daddsWebHook_1=testHook2
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

    @Test
    void test_webhook(LrgsTestInstance lrgs)
    {

        final String message = """
                {
                    "Type" : "Notification",
                    "MessageId" : "b9e5d9cd-d2ca-5ec6-8745-bb281a27a287",
                    "TopicArn" : "arn:aws:sns:us-east-1:940482412785:dadds-webhooks-messages-new",
                    "Message" : "{\\"Id\\":\\"8d88e58d-a5ab-463c-8ede-7ffa2421ee04\\",\\"Address\\":\\"CE09A476\\",\\"Time\\":\\"2026-07-30T21:48:22.613\\",\\"InfoCd\\":\\"G\\",\\"GroupCd\\":\\"CESPL1\\",\\"Data\\":\\"\\\\u0000\\\\u0000\\\\u0000\\\\b\\\\u0013\\\\u0000\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\\\u0000X\\\\u0000\\",\\"DataHex\\":\\"200D8AB0AEB5382031B3AEB60D8AB0AEB5380D8AB0AEB5380D8AB0AEB5380D8AB0AEB5380D8AB0AEB5380D8AB0AEB5380D8AB0AEB5380D8A\\",\\"ArmCodes\\":[],\\"LockTime\\":\\"2026-07-30T21:48:20.285\\",\\"Baud\\":300,\\"SigStrength\\":41.9,\\"FreqDevStart\\":-0.7,\\"PhaseNoise\\":1.64,\\"GoodPhase\\":100,\\"ChannelId\\":88,\\"SatLocation\\":\\"W\\",\\"Source\\":\\"w\\",\\"NoEot\\":false,\\"Par\\":0,\\"NwsDescriptor\\":\\"SRAZ30\\",\\"IsNws\\":true,\\"NwsCenter\\":\\"KWAL\\",\\"PdtId\\":30785,\\"GroupId\\":2108,\\"AddressRecv\\":\\"CE09A476\\",\\"SyncTime\\":\\"2026-07-30T21:48:20.877\\",\\"Snr\\":30.9,\\"Length\\":112,\\"Quality\\":\\"G\\",\\"SatId\\":18,\\"Priority\\":4,\\"Duration\\":2.328,\\"FrameSync\\":\\"R\\",\\"FreqDevEnd\\":-0.5,\\"AddressCode\\":\\"V\\",\\"Ber\\":0}",
                    "Timestamp" : "2026-07-30T21:48:29.649Z",
                    "SignatureVersion" : "1",
                    "Signature" : "R/D7URvZ6kj6GTnNMlG/QdpBkVugnrvH+sHZndGcFVGBP4wbvKxR9IksIiD7c26aaT+9Iasp3H3+Og/89/FJHBUTV9HyVecJ1nMSWxpb47jlrROSEuCekqc+fmrMzWLwyrgTwRvVdflvbvEdokn0+UIXA/UdLhCsx1b2daHTBN6WlZd44RF1QQCmyKd6zPJB6ozRmBHBfxzp2fXY7ZlwtQKioN35w8pjL/l6A73BxEKeu+uJkNdhP/HnQPFhvZfqn9KbQoY2z/A7xdA9fMFxvV8p7rXJdoEYd+8j4OS5R2v1keCV0sTw9FMmBIe33w/93/5GrFFX3jgH4/QsuN0N7g==",
                    "SigningCertURL" : "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-7506a1e35b36ef5a444dd1a8e7cc3ed8.pem",
                    "UnsubscribeURL" : "https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:940482412785:dadds-webhooks-messages-new:6963c8ed-7d28-45bb-96ee-04dc10b89278"
                }
        """;
        InterceptingInetAddressResolver.registerIntercept("sns.us-east-1.amazonaws.com", Inet4Address.getLoopbackAddress());
        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .header("x-amz-sns-message-type", "Notification")
            .header("x-amz-sns-topic-arn","arn:aws:sns:us-east-1:940482412785:dadds-webhooks-messages-new")
            .body(message)
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .post("webhook/dadds/{hookId}", "testHook")
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.OK.getStatusCode()))
        ;

        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .header("x-amz-sns-message-type", "Notification")
            .header("x-amz-sns-topic-arn","arn:aws:sns:us-east-1:940482412785:dadds-webhooks-messages-new")
            .body(message)
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .post("webhook/dadds/{hookId}", "badHook")
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.NOT_FOUND.getStatusCode()))
        ;

        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .header("x-amz-sns-message-type", "Notification")
            .header("x-amz-sns-topic-arn","arn:aws:sns:us-east-1:940482412785:dadds-webhooks-messages-new")
            .body(message)
        .when()
            .redirects().follow(true)
            .redirects().max(3)
            .post("webhook/dadds/{hookId}", "testHook2")
        .then()
            .log().ifValidationFails(LogDetail.ALL, true)
        .assertThat()
            .statusCode(is(Response.Status.OK.getStatusCode()))
        ;
    }
}
