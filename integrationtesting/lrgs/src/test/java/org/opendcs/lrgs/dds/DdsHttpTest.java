package org.opendcs.lrgs.dds;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.dadds.SnsMessageCreator;
import org.opendcs.fixtures.extensions.lrgs.LrgsConfig;
import org.opendcs.fixtures.extensions.lrgs.LrgsTestExtension;
import org.opendcs.fixtures.inet.InterceptingInetAddressResolver;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.lrgs.http.LrgsHttpInput;
import org.opendcs.lrgs.webhook.dadds.DaddsDataMessage;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Cookies;
import jakarta.ws.rs.core.Response;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsInputInterface;
import nl.altindag.ssl.SSLFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;

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
    void test_webhook(LrgsTestInstance lrgs) throws Exception
    {
        
        var messages = createMessages();               
        final String topicArn = "arn:aws:sns:us-east-1:000000000000:dadds-webhooks-messages-new";
        final var keyStorePassword = "awsmock".toCharArray(); // NOSONAR
        final var keyAlias = "awssigning";
        var keyStore = KeyStore.getInstance("JKS");
        try (var inputStream = DdsHttpTest.class.getResourceAsStream("/awsmock.jks"))
        {
            keyStore.load(inputStream, keyStorePassword);
        }
        assertTrue(keyStore.containsAlias("awssigning"));
        var privateKey = (PrivateKey)keyStore.getKey(keyAlias, keyStorePassword);
        assertNotNull(privateKey);
        var publicKey = keyStore.getCertificate(keyAlias).getPublicKey();

        byte[] publicBytes = publicKey.getEncoded();
        
        // Encode the bytes into Base64 format
        var encoder = Base64.getMimeEncoder(64, new byte[]{'\n'});
        String base64Encoded = encoder.encodeToString(publicBytes);
        
        // Wrap with standard X.509 Public Key headers and footers
        var publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + base64Encoded + "\n-----END PUBLIC KEY-----";

        var trust = SSLFactory.builder()
                              .withDefaultTrustMaterial()
                              .withSystemTrustMaterial()
                              .withTrustMaterial(keyStore)
                              .withInflatableTrustMaterial(Path.of("test.jks"), keyStorePassword, "PKCS12",
                                c ->
                            {
                                System.out.println("Host is: " + c.getHostname().orElse("No name?"));
                                return true;
                            } )
                              .build();
        SSLContext.setDefault(trust.getSslContext());

        var sslContext = SSLContext.getInstance("TLS");
        var kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keyStorePassword);
        sslContext.init(kmf.getKeyManagers(), null, null);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(63543), 0);
        var conf = new HttpsConfigurator(sslContext);
        server.setHttpsConfigurator(conf);
        server.setExecutor(null);
        server.createContext("/cert.pem", ctx ->
        {
            ctx.sendResponseHeaders(200, 0);
            ctx.getResponseBody().write(publicKeyPem.getBytes());
        });
        server.start();
        final int port = server.getAddress().getPort();

        InterceptingInetAddressResolver.registerIntercept("sns.us-east-1.amazonaws.com", Inet4Address.getLoopbackAddress());

        for (var message: messages)
        {
            given()
                .log().ifValidationFails(LogDetail.ALL, true)
                .header("x-amz-sns-message-type", "Notification")
                .header("x-amz-sns-topic-arn",topicArn)
                .body(SnsMessageCreator.createDaddsNotification(message, privateKey, topicArn, port))
            .when()
                .redirects().follow(true)
                .redirects().max(3)
                .post("webhook/dadds/{hookId}", "testHook")
            .then()
                .log().ifValidationFails(LogDetail.ALL, true)
            .assertThat()
                .statusCode(is(Response.Status.OK.getStatusCode()))
            ;
        }

        given()
            .log().ifValidationFails(LogDetail.ALL, true)
            .header("x-amz-sns-message-type", "Notification")
            .header("x-amz-sns-topic-arn",topicArn)
            .body("I don't matter.")
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
            .header("x-amz-sns-topic-arn", topicArn)
            .body(SnsMessageCreator.createDaddsNotification(messages.getFirst(), null, topicArn, port))
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

    List<DaddsDataMessage> createMessages()
    {
        ArrayList<DaddsDataMessage> ret = new ArrayList<>();
        for (int i = 0; i < 50; i++)
        {
            String data = "This is test data";
            String addr = String.format("%8s", HexFormat.of().toHexDigits(i)).replace(' ', '0');
            ret.add(new DaddsDataMessage(
                UUID.randomUUID(),
                addr, LocalDateTime.now(), "G", "Test", data, null,
                List.of(), LocalDateTime.now(), 300, 100.0f, 0.5f,
                .5f, 0.0f, 100, 88, "w", "W", false, 0, null, false, null, i,
                i, addr, LocalDateTime.now(), 35.4f, data.length(), "G", i, i, 1.3f, "R", "V", i)
            );
        }
        return ret;
    }
}
