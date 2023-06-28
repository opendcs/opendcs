package ilex.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JavaLoggerFormatter
	extends Formatter
{
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
        String ret = record.getLoggerName() + ":" + msg + (causeMessage == null ? "" : ": " + causeMessage);
		return ret + System.lineSeparator();
	}
}
