package org.opendcs.logging;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

/**
 * Based on https://logback.qos.ch/manual/encoders.html#JsonEncoder as it is sufficiently
 * complete to allow any logger backend to map to this structure
 */
public final class LoggingEvent {
    public final Long sequence;
    public final ZonedDateTime timestamp;
    public final String level;
    public final String threadName;
    public final String loggerName;
    public final Map<String, String> context;
    public final Map<String, String> diagnosticContext;
    public final Map<String, String> keyValuePairs;
    public final String message;
    public final LoggedThrowable throwable;


    public LoggingEvent(Long sequence, ZonedDateTime timestamp, String level, String threadName, String loggerName,
                        Map<String, String> context, Map<String, String> diagnosticContext,
                        Map<String, String> keyValuePairs, String message, LoggedThrowable throwable)
    {
        this.sequence = sequence;
        this.timestamp = timestamp;
        this.level = level;
        this.threadName = threadName;
        this.loggerName = loggerName;
        this.context = Collections.unmodifiableMap(context);
        this.diagnosticContext = Collections.unmodifiableMap(diagnosticContext);
        this.keyValuePairs = Collections.unmodifiableMap(keyValuePairs);
        this.message = message;
        this.throwable = throwable;
    }

    @Override
    public String toString()
    {
        return String.format("%s %s [%s] %s %s", timestamp.format(DateTimeFormatter.ISO_ZONED_DATE_TIME), level, threadName, loggerName, message);
    }
}
