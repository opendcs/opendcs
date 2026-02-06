package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

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
    void test_open_url_https() throws Exception 
    {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(createSslContextFromKeystore()));

        try {
            server.createContext("/", httpExchange -> {
                String response = "test message";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                httpExchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            });
            server.start();

            int port = server.getAddress().getPort();
            String url = "https://localhost:" + port;

            assertThrows(SSLHandshakeException.class, () -> WebUtility.readStringFromURL(url));

            final String result = WebUtility.readStringFromURL(url, certs ->
                certs.getHostname().orElse("null").equals("localhost")
            );
            assertEquals("test message", result);
        } finally {
            server.stop(0);
        }
    }


    @Test
    void test_open_url_http() throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            server.createContext("/", httpExchange -> {
                String response = "test message";
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
                httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                httpExchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            });
            server.start();

            int port = server.getAddress().getPort();
            final String result = WebUtility.readStringFromURL("http://localhost:" + port);
            assertEquals("test message", result);
        } finally {
            server.stop(0);
        }
    }

    /**
     * Creates an SSLContext by loading the key and certificate from 'lrgs.jks'
     */
    private static SSLContext createSslContextFromKeystore() throws Exception 
    {
        char[] password = "lrgstest".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");

        try (InputStream is = WebUtilityTest.class.getResourceAsStream("/org/opendcs/util/lrgs.jks")) {
            if (is == null) {
                throw new RuntimeException("Cannot find 'lrgs.jks' in test resources.");
            }
            keyStore.load(is, password);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);


        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        return sslContext;
    }
}
