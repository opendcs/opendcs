package org.opendcs.regression_tests;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import decodes.datasource.DataSourceEndException;
import decodes.datasource.RawMessage;
import decodes.datasource.WebAbstractDataSource;
import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import io.github.bucket4j.Bucket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/cwms-import.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
class WebSourceTest extends AppTestBase
{
    @ConfiguredField
    Database db;

    static final String PRINTABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+_$@!%\n";

    // This use of random is not used in a cryptographic context. It's use is to generate
    // sufficient fake data to make a test meaningful.
    static Random random = new Random(); // NOSONAR

    private static String generateMessage(int size)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < size; i++)
        {
            int index = random.nextInt(PRINTABLE.length());
            sb.append(PRINTABLE.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Tests with 1000/minute allowed with 2000 stations requested
     * Bit artificial but it allows the code to be well excercised without taking excessive wall clock time.
     * @throws Exception
     */
    @Test
    void test_abstract_web_source() throws Exception
    {
        final Bucket bucket = Bucket.builder()
                                    .addLimit(limit -> limit.capacity(1000)
                                                            .refillGreedy(1000, Duration.ofMinutes(1)))
                                    .build();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/data/", exchange ->
        {
            String body;
            int statusCode;
            if (bucket.tryConsume(1))
            {
                body = generateMessage(120);
                statusCode = 200;
            }
            else
            {
                body = "Too many requests.";
                statusCode = 429;
            }
            byte[] responseBytes = body.getBytes();
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(responseBytes);
            }
        });
        server.start();
        try
        {
            int port = server.getAddress().getPort();
            Properties props = new Properties();
            Vector<NetworkList> netlists = new Vector<>();

            final int MESSAGE_SIZE = 2000;
            NetworkList list = new NetworkList();
            for (int i = 0; i < MESSAGE_SIZE; i ++)
            {
                list.addEntry(new NetworkListEntry(list, "id"+i));
            }
            netlists.add(list);

            WebAbstractDataSource wads = new WebAbstractDataSource(null, db);
            props.setProperty("abstractUrl", "http://localhost:" + port + "/data/$MEDIUMID/$SINCE/$UNTIL");
            props.setProperty("header","other");
            props.setProperty("onemessagefile", "true");
            props.setProperty("rateLimit", "990");
            wads.setAllowNullPlatform(true);
            wads.init(props, "now - 2 hours", "now", netlists);


            final ArrayList<RawMessage> messages = new ArrayList<>();
            assertThrows(DataSourceEndException.class, () ->
            {
                RawMessage msg = null;
                while ((msg = wads.getRawMessage()) != null)
                {
                    assertNotNull(msg);
                    messages.add(msg);
                }
            });
            assertFalse(messages.isEmpty());
            assertEquals(MESSAGE_SIZE, messages.size());
        }
        finally
        {
            server.stop(0);
        }
    }

}
