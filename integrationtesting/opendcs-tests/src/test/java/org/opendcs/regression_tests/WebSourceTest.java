package org.opendcs.regression_tests;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.configuration.Configuration;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.netty.MockServer;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.slf4j.event.Level;

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
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;


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

    @Test
    void test_abstract_web_source() throws Exception
    {
        final Bucket bucket = Bucket.builder()
                                    .addLimit(limit -> limit.capacity(1000)
                                                            .refillGreedy(1000, Duration.ofHours(1)))
                                    .build();
        Configuration conf = Configuration.configuration();
        Level currentLevel = conf.logLevel();
        Configuration.configuration().logLevel(Level.ERROR);
        try (ClientAndServer server = new ClientAndServer(0))
        {
            server.when(request().withPath("/data/.*"))
                  .respond(request ->
                  {
                    if(bucket.tryConsume(1))
                    {
                        return response().withBody(generateMessage(120));
                    }
                    else
                    {
                        return response().withStatusCode(429).withBody("Too many requests.");
                    }
                  }
                  );
            Properties props = new Properties();
            Vector<NetworkList> netlists = new Vector<>();
        
            final int MESSAGE_SIZE = 200;
            NetworkList list = new NetworkList();
            for (int i = 0; i < MESSAGE_SIZE; i ++)
            {
                list.addEntry(new NetworkListEntry(list, "id"+i));
            }
            netlists.add(list);

            WebAbstractDataSource wads = new WebAbstractDataSource(null, db);
            props.setProperty("abstractUrl", "http://localhost:" + server.getPort() + "/data/$MEDIUMID/$SINCE/$UNTIL");
            props.setProperty("header","other");
            props.setProperty("onemessagefile", "true");
            props.setProperty("rateLimit", "15");
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
            conf.logLevel(currentLevel);
        }
    }

}
