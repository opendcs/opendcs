package org.opendcs.utils.logging;

import org.opendcs.logging.spi.LoggingEventProvider;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.util.RingBuffer;
import org.slf4j.Logger;

/**
 * Consumes Logging events from the given provider and store them
 * in a ring buffer to be consumed as needed.
 *
 */
public final class LoggingEventBuffer
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private final LoggingEventProvider eventProvider;
    private final RingBuffer<LoggingEvent> eventList;


    private LoggingEventBuffer(Builder builder)
    {
        this.eventProvider = Objects.requireNonNull(builder.provider, "A logging event provider must be given.");
        this.eventList = new RingBuffer<>(builder.defaultSize);
        this.eventProvider.attach(eventList);
    }

    /**
     * Sets the new size, allowing the buffer to grow, or forcing to shrink as required.
     * @param newSize
     */
    public void setSize(int newSize)
    {
        synchronized (eventList)
        {
            eventList.setSize(newSize);
        }
    }

    /**
     * Read only List backed by the RingBuffer.
     * @return
     */
    public List<LoggingEvent> getEvents()
    {
        return Collections.unmodifiableList(this.eventList);
    }

    /**
     * Return Publisher implemented by the Ring buffer.
     * For use by systems that either don't want to Poll or only require notification
     * of recent events.
     * @return
     */
    public Publisher<LoggingEvent> getPublisher()
    {
        return this.eventList;
    }

    public static class Builder
    {
        LoggingEventProvider provider;
        int defaultSize = 100_000;
        String threadName = "Logging-Thread";

        /**
         * Finalize the construction of this logging event buffer.
         * @return
         */
        public LoggingEventBuffer build()
        {
            return new LoggingEventBuffer(this);
        }

        /**
         * Use the given event provider
         * @param provider
         * @return
         */
        public Builder withProvider(LoggingEventProvider provider)
        {
            this.provider = Objects.requireNonNull(provider, "A logging event provider must be given.");
            return this;
        }

        public Builder withDefaultSize(int defaultSize)
        {
            if (defaultSize < 0)
            {
                throw new IllegalArgumentException("default size must be positive.");
            }
            this.defaultSize = defaultSize;
            return this;
        }

        public Builder withThreadName(String threadName)
        {
            this.threadName = threadName;
            return this;
        }
    }
}
