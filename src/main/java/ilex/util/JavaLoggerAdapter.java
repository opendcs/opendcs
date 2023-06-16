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
 * Revision 1.17  2019/01/18 16:10:27  mmaloney
 * dev
 *
 * Revision 1.16  2019/01/18 15:53:45  mmaloney
 * dev
 *
 * Revision 1.15  2019/01/18 15:49:43  mmaloney
 * dev
 *
 * Revision 1.14  2019/01/18 15:43:17  mmaloney
 * dev
 *
 * Revision 1.13  2019/01/17 20:32:27  mmaloney
 * dev
 *
 * Revision 1.12  2019/01/17 20:04:49  mmaloney
 * dev
 *
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
	private static boolean doForward = true;
	
	/** Singleton access only */
	public static JavaLoggerAdapter instance() { return _instance; }
	
	private JavaLoggerAdapter()
	{
		this.setLevel(Level.ALL);
	}

	/**
	 * Initialize the java.util.logging facility to funnel messages into the passed
	 * OpenDCS Logger.
	 * @param ilexLogger The OpenDCS Logger that will receive messages.
	 * @param _doForward set to true to forward log messages, false to squelch them.
	 * @param paths a list of root level logger paths to forward. Use the empty string "" to forward all.
	 */
	public static void initialize(ilex.util.Logger ilexLogger, boolean _doForward, String ... paths)
	{
		if (initialized)
			return;
		initialized = true;
		doForward = _doForward;
		JavaLoggerAdapter.ilexLogger = ilexLogger;
		
		java.util.logging.Logger globalLogger = LogManager.getLogManager().getLogger(globalName);
		
		globalLogger.addHandler(instance());		
	}

	@Override
	public void publish(LogRecord record)
	{
		if (!doForward)
			return;
		if (myFormatter == null)
			myFormatter = new JavaLoggerFormatter();
		String s = myFormatter.format(record);
//if (record.getLoggerName().contains("ConnectionPersistenceManager")
// || record.getLoggerName().contains("AbstractCwmsDbDao"))
//	System.err.println("JavaLoggerAdapter '" + s + "'");
		
		ilexLogger.log(mapPriority(record), s);
	}
	
	public static int mapPriority(LogRecord record)
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
