package org.opendcs.logging.logback;

import java.util.logging.Logger;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.util.RingBuffer;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

public final class LogbackEventProvider implements LoggingEventProvider
{
    private final LogAppender appender;
    
    public LogbackEventProvider()
    {
        this.appender = new LogAppender();
        startProvider();
    }
    

    private void startProvider()
    {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        if (!(factory instanceof LoggerContext))
        {
            throw new IllegalStateException(
                String.format("ILoggerFactory factory is not of the expected type. Expected '%s', got '%s'",
                              LoggerContext.class.getName(), factory.getClass().getName()
                )
            );
        }
        LoggerContext ctx = (LoggerContext)factory;
        ctx.getLogger("ROOT").addAppender(appender);
    }

    @Override
    public void attach(RingBuffer<LoggingEvent> buffer)
    {
        appender.setTarget(buffer);
    }
    
}
