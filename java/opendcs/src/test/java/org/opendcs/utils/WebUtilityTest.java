package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.MediaType;

import ilex.util.EnvExpander;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;


@ExtendWith(MockServerExtension.class)
@ExtendWith(SystemStubsExtension.class)
class WebUtilityTest
{
    SystemProperties properties = null ;

    @TempDir
    File userDir;

    @BeforeEach
    void setup(SystemProperties properties)
    {
        this.properties = properties;
        properties.set("DCSTOOL_USERDIR", userDir.getAbsolutePath());
    }

    @Test
    void test_open_url_https(MockServerClient client) throws Exception
    {
        properties.execute(() ->
        {
            System.out.println(EnvExpander.expand("$DCSTOOL_USERDIR"));
            client.reset();
            client.when(request().withMethod("GET"))
                  .respond(response().withBody("test message")
                                     .withContentType(MediaType.TEXT_PLAIN));

            assertThrows(SSLHandshakeException.class, () -> WebUtility.readStringFromURL("https://localhost:" + client.getPort()));

            final String result = WebUtility.readStringFromURL("https://localhost:" + client.getPort(), certs ->
            {
                System.out.println(certs.getHostname().orElse("No present"));
                return certs.getHostname().orElse("null").equals("localhost");
            });
            assertEquals("test message", result);
        });   
    }

    @Test
    void test_open_url_http(MockServerClient client) throws Exception
    {        
        client.reset();
        client.when(request().withMethod("GET"))
                .respond(response().withBody("test message")
                                    .withContentType(MediaType.TEXT_PLAIN));

        final String result = WebUtility.readStringFromURL("http://localhost:" + client.getPort());
        assertEquals("test message", result);
    }
}
