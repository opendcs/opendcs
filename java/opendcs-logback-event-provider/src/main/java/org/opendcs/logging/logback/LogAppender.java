package org.opendcs.logging.logback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendcs.logging.LoggedThrowable;
import org.opendcs.logging.LoggingEvent;
import org.opendcs.util.RingBuffer;
import org.slf4j.event.KeyValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

public class LogAppender extends AppenderBase<ILoggingEvent>
{
    private RingBuffer<LoggingEvent> buffer = null;
    private long sequence = 0;

    @Override
    protected void append(ILoggingEvent event)
    {
        if (buffer != null)
        {
            buffer.add(map(event));
        }
    }


    public void setTarget(RingBuffer<LoggingEvent> buffer)
    {
        this.buffer = buffer;
    }

    private LoggingEvent map(ILoggingEvent eventIn)
    {
        List<KeyValuePair> kvpList = eventIn.getKeyValuePairs();
        Map<String, String> kvp = kvpList == null 
                                ? new HashMap<>() 
                                : (Map<String,String>)kvpList.stream()
                                         .collect(Collectors.toMap(kv -> kv.key, kv -> kv.value.toString()));

        return new LoggingEvent(
            sequence++, 
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(
                                            eventIn.getTimeStamp()),
                                    ZoneId.of("UTC")),
            eventIn.getLevel().levelStr,
            eventIn.getThreadName(),
            eventIn.getLoggerName(),
            eventIn.getLoggerContextVO().getPropertyMap(),
            eventIn.getMDCPropertyMap(),
            kvp,
            eventIn.getFormattedMessage(),
            map(eventIn.getThrowableProxy())
        );
    }

    private LoggedThrowable map(IThrowableProxy ex)
    {
        if (ex == null)
        {
            return null;// end of chain.
        }
        return new LoggedThrowable(ex.getClassName(), ex.getMessage(),
                                   map(ex.getStackTraceElementProxyArray()), map(ex.getCause()));
    }

    private List<LoggedThrowable.ThrowableStep> map(StackTraceElementProxy[] stackTrace)
    {
        ArrayList<LoggedThrowable.ThrowableStep> steps = new ArrayList<>();
        for(StackTraceElementProxy step: stackTrace)
        {
            StackTraceElement ste = step.getStackTraceElement();
            steps.add(
                new LoggedThrowable.ThrowableStep(
                    ste.getClassName(), ste.getMethodName(), 
                    ste.getFileName(), ste.getLineNumber()));
        }
        return steps;
    }
}
