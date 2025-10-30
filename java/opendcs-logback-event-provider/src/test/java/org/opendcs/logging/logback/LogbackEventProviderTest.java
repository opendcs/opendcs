package org.opendcs.logging.logback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.util.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogbackEventProviderTest
{
    private static final Logger log = LoggerFactory.getLogger(LogbackEventProviderTest.class);
    @Test
    void test_event_provider()
    {
        RingBuffer<LoggingEvent> buffer = new RingBuffer<>(10);
        assertTrue(buffer.isEmpty());
        LoggingEventProvider eventProvider = LoggingEventProvider.getProvider();
        eventProvider.attach(buffer);
        final String msg = "Hello from test";
        log.info(msg);
        assertFalse(buffer.isEmpty());
        LoggingEvent le = buffer.get(0);
        assertEquals(msg, le.message);
    }
    
}
