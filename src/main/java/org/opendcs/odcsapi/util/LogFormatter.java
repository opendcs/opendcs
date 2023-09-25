package org.opendcs.odcsapi.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formats a log message in the same way as the opendcs ilex.util.Logger system.
 * @author mmaloney
 */
public class LogFormatter
	extends Formatter
{
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private String nl = System.getProperty("line.separator");
	
	public LogFormatter()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	public String format(LogRecord record)
	{
		return record.getLevel().toString() + " " 
			+ sdf.format(new Date(record.getMillis())) + " " + record.getMessage() + nl;
	}
}
