package org.opendcs.logging.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.util.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a specific Logger backend, convert log and return a Logging Event
 */
public interface LoggingEventProvider
{
    static final Logger log = LoggerFactory.getLogger(LoggingEventProvider.class);
    /**
     * Attach this provider to the log buffer instance.
     * @return
     */
    void attach(RingBuffer<LoggingEvent> buffer);

    public static LoggingEventProvider getProvider()
    {
        ServiceLoader<LoggingEventProvider> loader = ServiceLoader.load(LoggingEventProvider.class);
        loader.reload();
        
        Iterator<LoggingEventProvider> providers = loader.iterator();
        LoggingEventProvider provider = null;
        if (providers.hasNext())
        {
            provider = providers.next();
        }
        if (provider == null)
        {
            throw new IllegalStateException("No Logging event providers are available in this JVM.");
        }
        if (providers.hasNext())
        {
            log.warn("Multiple LoggingEventProviders are available. Using the first of type {}",
                     provider.getClass().getName());
        }
        return provider;
    }
}
