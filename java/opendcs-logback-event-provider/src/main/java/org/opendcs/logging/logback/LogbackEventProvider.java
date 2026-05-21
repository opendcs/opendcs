package org.opendcs.logging.logback;

import com.google.auto.service.AutoService;
import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.util.RingBuffer;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

@AutoService(LoggingEventProvider.class)
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
        Logger log = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender.setContext((LoggerContext)factory);
        appender.setName("MemoryBuffer");
        appender.start();
        //log.setLevel(Level.TRACE);

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext((LoggerContext)factory);
        asyncAppender.setName("MemoryBuffer-Async");
        asyncAppender.addAppender(appender);
        asyncAppender.setQueueSize(1024); // arbitrary default for us, logback default is 256
        
        asyncAppender.start();
        log.addAppender(asyncAppender);
        
    }

    @Override
    public void attach(RingBuffer<LoggingEvent> buffer)
    {
        appender.setTarget(buffer);
    }
    
}
