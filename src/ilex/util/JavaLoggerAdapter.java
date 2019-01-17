/**
 * $Id$
 * 
 * Copyright 2018 United States Government
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this 
 * file except in compliance with the License. You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under 
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF 
 * ANY KIND, either express or implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * $Log$
 * Revision 1.11  2019/01/17 20:00:17  mmaloney
 * dev
 *
 * Revision 1.10  2018/12/20 20:44:45  mmaloney
 * Add throwable info if one is present.
 *
 * Revision 1.9  2018/12/19 15:43:05  mmaloney
 * Format message with parameters if any are present.
 *
 * Revision 1.8  2018/12/18 16:14:50  mmaloney
 * Only capture specific loggers, otherwise you get tons of messages from X.
 *
 * Revision 1.7  2018/12/13 23:00:47  mmaloney
 * dev
 *
 * Revision 1.6  2018/12/13 22:53:12  mmaloney
 * dev
 *
 * Revision 1.5  2018/12/13 22:36:29  mmaloney
 * dev
 *
 * Revision 1.4  2018/12/13 18:00:58  mmaloney
 * dev
 *
 * Revision 1.3  2018/12/13 17:54:55  mmaloney
 * dev
 *
 * Revision 1.2  2018/12/13 16:22:17  mmaloney
 * dev
 *
 * Revision 1.1  2018/12/13 15:55:46  mmaloney
 * Created.
 *
 */
package ilex.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;

/**
 * This class is an adapter to make the log messages from libraries that use 
 * "java.util.logging" visible in OpenDCS. It funnels log messages to the
 * OpenDCS logging infrastructure.
 * @author mmaloney
 */
public class JavaLoggerAdapter extends Handler
{
	private final static String globalName = java.util.logging.Logger.GLOBAL_LOGGER_NAME;
	private static ilex.util.Logger ilexLogger = null;
	private static final JavaLoggerAdapter _instance = new JavaLoggerAdapter();
	private static Formatter myFormatter = null;
	private static boolean initialized = false;
	
	/** Singleton access only */
	public static JavaLoggerAdapter instance() { return _instance; }
	
	private JavaLoggerAdapter()
	{
	}

	/**
	 * Initialize the java.util.logging facility to funnel messages into the passed
	 * OpenDCS Logger.
	 * @param ilexLogger The OpenDCS Logger that will receive messages.
	 * @param forwardGlobal set to true to forward the global logger.
	 * @param paths a list of root level logger paths to forward. Use the empty string "" to forward all.
	 */
	public static void initialize(ilex.util.Logger ilexLogger, boolean forwardGlobal, String ... paths)
	{
		if (initialized)
			return;
		initialized = true;
		JavaLoggerAdapter.ilexLogger = ilexLogger;
		
//System.err.println("\nConfiguring globalLogger");
		java.util.logging.Logger globalLogger = LogManager.getLogManager().getLogger(globalName);
		if (forwardGlobal)
		{
			Handler handlers[] = globalLogger.getHandlers();
			if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler)
				globalLogger.removeHandler(handlers[0]);
			globalLogger.addHandler(instance());
			globalLogger.setLevel(Level.ALL);
		}
		
//System.err.println("\nConfiguring rootLogger");
		for(String path : paths)
		{
			java.util.logging.Logger logger = java.util.logging.Logger.getLogger(path);
			if (logger == null)
				System.err.println("logger is null for path '" + path + "'.");
			else if (logger == globalLogger)
				continue;
			else
			{
				Handler handlers[] = logger.getHandlers();
				if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler)
					logger.removeHandler(handlers[0]);
				logger.addHandler(instance());
				logger.setLevel(Level.ALL);
			}
		}
	}

	@Override
	public void publish(LogRecord record)
	{
		if (myFormatter == null)
			myFormatter = new JavaLoggerFormatter();
		
		ilexLogger.log(mapPriority(record), myFormatter.format(record));
	}
	
	private int mapPriority(LogRecord record)
	{
		if (record.getLevel() == Level.SEVERE) return Logger.E_FAILURE;
		if (record.getLevel() == Level.WARNING) return Logger.E_WARNING;
		if (record.getLevel() == Level.INFO
		 || record.getLevel() == Level.CONFIG) return Logger.E_INFORMATION;
		if (record.getLevel() == Level.FINE) return Logger.E_DEBUG1;
		if (record.getLevel() == Level.FINER) return Logger.E_DEBUG2;
		if (record.getLevel() == Level.FINEST) return Logger.E_DEBUG3;
		return Logger.E_INFORMATION;
	}

	@Override
	public void flush()
	{
		// Nothing to do
	}

	@Override
	public void close() throws SecurityException
	{
		// Nothing to do
	}

	public static void main(String[] args)
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		Logger.instance().info("Before initialize -- message direct to Ilex Logger.");
		
		JavaLoggerAdapter.initialize(Logger.instance(), true, "");
		
		Logger.instance().info("After initialize -- message direct to Ilex Logger.");
		
		java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
		
		rootLogger.log(Level.INFO, "INFO message sent to java root logger.");
		
		String cname = JavaLoggerAdapter.class.getName();
		java.util.logging.Logger myLogger = java.util.logging.Logger.getLogger(cname);
		myLogger.setLevel(Level.FINEST);
		myLogger.log(Level.SEVERE, "SEVERE for cname=" + cname);
		myLogger.log(Level.WARNING, "WARNING for cname=" + cname);
		myLogger.log(Level.INFO, "INFO for cname=" + cname);
		myLogger.log(Level.CONFIG, "CONFIG for cname=" + cname);
		myLogger.log(Level.FINE, "FINE for cname=" + cname);
		myLogger.log(Level.FINER, "FINER for cname=" + cname);
		myLogger.log(Level.FINEST, "FINEST for cname=" + cname);
		Logger.instance().debug3("Direct DEBUG_3 message to IlexLogger.");
	}
}

class JavaLoggerFormatter
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
        if (t != null)
        	msg = msg + " (" + t.toString() + ")";
        
        String ret = record.getLoggerName() + ": " + msg;
//System.err.println("JavaLoggerFormatter returning '" + ret + "'");
		return ret;
	}
}
