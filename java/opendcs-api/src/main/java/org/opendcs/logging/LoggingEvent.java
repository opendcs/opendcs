package org.opendcs.logging;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Based on https://logback.qos.ch/manual/encoders.html#JsonEncoder as it is sufficiently
 * complete to allow any logger backend to map to this structure
 */
public record LoggingEvent (
    Long sequence, ZonedDateTime timestamp, String level, String threadName, String loggerName,
    Map<String, String> context, Map<String, String> diagnosticContext,
    Map<String, String> keyValuePairs, String message, LoggedThrowable throwable)
{

    @Override
    public String toString()
    {
        return String.format("%s %s [%s] %s %s", timestamp.format(DateTimeFormatter.ISO_ZONED_DATE_TIME), level, threadName, loggerName, message);
    }

    public static final LoggingEvent of(String msg)
    {
        return of(msg, null);
    }

    public static final LoggingEvent of(String msg, Throwable throwable)
    {
        LoggedThrowable lt = LoggedThrowable.from(throwable);
        return new LoggingEvent(0L, ZonedDateTime.now(), "INFO", Thread.currentThread().getName(),
                         "manual", null, null, null, msg, lt);
    }
}
