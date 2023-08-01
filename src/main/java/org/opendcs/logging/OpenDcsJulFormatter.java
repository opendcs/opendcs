package org.opendcs.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class OpenDcsJulFormatter
	extends Formatter
{
    // DATE Level loggername [thread] Message cause lineseparator
    private final static String LOG_FMT="%s %s %s [THREAD:%s] %s %s%s";
    private final static ZoneId UTC = ZoneId.of("UTC");
	
    	@Override
	public String format(LogRecord record)
	{
    	String msg = record.getMessage();
        try 
        {
            Object parameters[] = record.getParameters();
            if ((parameters != null && parameters.length > 0)
             &&    (msg.indexOf("{0") >= 0 || msg.indexOf("{1") >=0 
                 || msg.indexOf("{2") >= 0 || msg.indexOf("{3") >=0))
            {
            	msg = java.text.MessageFormat.format(msg, parameters);
            }
        } catch (Exception ex) { msg = record.getMessage(); }

        Throwable t = record.getThrown();
        String causeMessage = null;
        if (t != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            causeMessage = sw.toString();
        }
        

        String ret = String.format(LOG_FMT,
                                   ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.getMillis()), UTC)
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                   record.getLevel().getName(),
                                   record.getLoggerName(),
                                   getThreadName(record.getThreadID()),
                                   msg,
                                   (causeMessage == null ? "" : ": " + causeMessage),
                                   System.lineSeparator());
		return ret;
	}
	

    private String getThreadName(long threadId) {
        
        return Thread.getAllStackTraces().keySet().stream()
             .filter(t -> t.getId() == threadId)
             .findFirst()
             .map(Thread::getName)
             .orElseGet(() -> "Thread-"+threadId);
    }
}
