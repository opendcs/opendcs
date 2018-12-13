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

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

/**
 * This class is an adapter to make the log messages from libraries that use 
 * "java.util.logging" visible in OpenDCS. It funnels log messages to the
 * OpenDCS logging infrastructure.
 * @author mmaloney
 */
public class JavaLoggerAdapter extends Handler
{
	private final static String globalName = java.util.logging.Logger.GLOBAL_LOGGER_NAME;
	private final static java.util.logging.Logger jLogger = 
		java.util.logging.Logger.getLogger(globalName);
	private static ilex.util.Logger ilexLogger = null;
	private static final JavaLoggerAdapter _instance = new JavaLoggerAdapter();
	private static SimpleFormatter simpleFormatter = null;
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
	 */
	public static void initialize(ilex.util.Logger ilexLogger)
	{
		if (initialized)
			return;
		initialized = true;
		JavaLoggerAdapter.ilexLogger = ilexLogger;
		jLogger.addHandler(_instance);
		LogManager.getLogManager().getLogger(globalName).setLevel(Level.ALL);
	}

	@Override
	public void publish(LogRecord record)
	{
		if (simpleFormatter == null)
			simpleFormatter = new SimpleFormatter();
		
		ilexLogger.log(mapPriority(record), simpleFormatter.format(record));
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
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws SecurityException
	{
		// TODO Auto-generated method stub

	}

	public static void main(String[] args)
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		Logger.instance().info("Before initialize -- message direct to Ilex Logger.");
		
		JavaLoggerAdapter.initialize(Logger.instance());
		
		Logger.instance().info("After initialize -- message direct to Ilex Logger.");
		
		java.util.logging.Logger globalLogger = 
			java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		
		globalLogger.log(Level.INFO, "INFO message sent to java global logger.");
		
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
		globalLogger.log(Level.FINEST, "FINEST to global logger.");
		

		// TODO Auto-generated method stub

	}

}
