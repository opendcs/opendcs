package org.opendcs.lrgs;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

import lrgs.ldds.LddsClient;
import lrgs.rtstat.hosts.LrgsConnection;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class TlsTests
{

    private static LrgsTestInstance lrgs = null;

    @BeforeAll
    public static void setup() throws Exception
    {
        
        System.out.println("Before");
        assertDoesNotThrow(() ->
        {
            File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
            System.setProperty("DCSTOOL_USERDIR", lrgsHome.getAbsolutePath());
            lrgsHome.mkdirs();
            lrgs = new LrgsTestInstance(lrgsHome, new File("src/test/resources/lrgs.jks"), "lrgstest");
        });
    }

    @Test
    void test_tls_connection() throws Exception
    {
        final LddsClient client = new LddsClient("localhost", lrgs.getDdsPort(), LrgsConnection.socketFactory(cert->
        {
            X509Certificate[] chain = cert.getChain();
            assertTrue(chain.length > 0, "no certificates provided");
            assertTrue(chain[0].getSubjectX500Principal().getName().contains("OU=LRGS"), "cert does not have expect element");
            return true;
        } ));
        assertDoesNotThrow(() -> client.connect());
    }

}
