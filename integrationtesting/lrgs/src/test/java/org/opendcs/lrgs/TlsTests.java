package org.opendcs.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.tls.TlsMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lrgs.ldds.LddsClient;
import lrgs.rtstat.hosts.LrgsConnection;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class TlsTests
{
    public final static Logger log = LoggerFactory.getLogger(TlsTests.class);

    @ParameterizedTest
    @EnumSource
    void test_tls_connection(TlsMode mode) throws Exception
    {
        final LrgsTestInstance lrgs =
            assertDoesNotThrow(() ->
            {
                File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
                System.setProperty("DCSTOOL_USERDIR", lrgsHome.getAbsolutePath());
                lrgsHome.mkdirs();
                return new LrgsTestInstance(lrgsHome, new File("src/test/resources/lrgs.jks"), "lrgstest", mode);
            });
        assertEquals(mode, lrgs.getConfig().getDdsServerTlsMode(), "Tls Mode was not set to the expected value.");
        final LddsClient client = new LddsClient("localhost", lrgs.getDdsPort(), LrgsConnection.socketFactory(cert->
        {
            X509Certificate[] chain = cert.getChain();
            assertTrue(chain.length > 0, "no certificates provided");
            assertTrue(chain[0].getSubjectX500Principal().getName().contains("OU=LRGS"), "cert does not have expect element");
            return true;
        }), mode);
        assertDoesNotThrow(() -> client.connect());

        if (mode == TlsMode.START_TLS)
        {
            log.info("Starting TLS Connection.");
            assertDoesNotThrow(() -> client.sendStartTls());
        }

        client.disconnect();
    }

}
