/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2007/05/01 00:57:26  mmaloney
*  dev
*
*  Revision 1.2  2004/11/24 16:02:26  mjmaloney
*  Reformatted with javadoc
*
*/
package ilex.util;

import java.util.Iterator;
import java.util.HashMap;
import java.io.*;


/**
* ThreadLogger segregates log messages from different threads to separate
* files. It holds a collection of Loggers. Every time the log()
* method is called, it examines the current thread. If a Logger already
* exists for this thread, its log method is called. If not and autoCreate is true,
* a new FileLogger is created with a set prefix (which may include a directory) 
* the name of the Thread, and a set suffix (e.g. ".log").
* <p>
* If !autoCreate, then the log message is sent to the default logger.
* Subordinate file loggers create files named: prefix + Thread.getName() + suffix
*/
public class ThreadLogger extends Logger
{
	/** Hash on Thread to get corresponding Logger. */
	private HashMap<String, Logger> loggers = new HashMap<String, Logger>();

	/** The maximum length for FileLoggers created by this object. */
	private int maxLength= 10000000; // 10 meg.

	/** The prefix used to create filenames for subordinate file loggers. */
	private String prefix = null;

	/** The suffix used to create filenames for subordinate file loggers. */
	private String suffix = null;

	/** Output goes here if there is a problem accessing a FileLogger. */
	private Logger defaultLogger = Logger.instance();

	/** True if we are allowed to create new loggers for unknown threads. */
	private boolean autoCreate;
	
	/** This is added before each message sent to the logger. */
	private String moduleName = null;

	/**
	* Constructs new ThreadLogger with set prefix and suffix.
	* The prefix may contain directory names.
	* @param procName the process name
	* @param prefix the prefix (e.g. directory path)
	* @param suffix the file-name suffix (e.g. ".log")
	* @param autoCreate true to create new files for unknown threads, false
	*        to use the default logger for unknown thread.
	*/
	public ThreadLogger( String procName, String prefix, String suffix,
		boolean autoCreate )
	{
		super(procName);
		this.prefix = prefix;
		this.suffix = suffix;
		this.autoCreate = autoCreate;
	}

	/**
	* Sets the default logger, which is used if a FileLogger cannot be
	* created for a thread.
	* You do not need to call this method. The default logger is set by
	* the constructor to an instance of StderrLogger.
	* @param lg logger
	*/
	public void setDefaultLogger( Logger lg )
	{
		defaultLogger = lg;
	}

	/**
	* Sets the maximum length of all log files.
	* @param maxLen the max length
	*/
	public void setMaxLength( int maxLen )
	{
		maxLength = maxLen;
		for(Iterator<Logger> it = loggers.values().iterator(); it.hasNext(); )
		{
			Logger lg = it.next();
			if (lg instanceof FileLogger)
				((FileLogger)lg).setMaxLength(maxLength);
		}
	}

	/**
	* Explicitly sets a logger for a thread. You do not need to call
	* this method normally. When a log message is generated, if no logger
	* yet exists for the current thread, a new FileLogger is created.
	* This method allows you to install some other kind of logger for
	* a given thread.
	* Call with null to have a formerly registered logger removed.
	* @param th the thread
	* @param lg the logger
	*/
	public void setLogger( Thread th, Logger lg )
	{
		if (lg != null)
			loggers.put(th.getName(), lg);
		else
			loggers.remove(th.getName());
	}

	/** Closes all subordinate loggers. */
	public void close( )
	{
		for(Iterator<Logger> it = loggers.values().iterator(); it.hasNext(); )
		{
			Logger lg = it.next();
			lg.close();
		}
		loggers = null;
	}

	/**
	* Logs a message by sending it to the current thread's Logger.
	* If no logger yet exists for the current thread, one is created.
	* @param priority the priority
	* @param text the formatted text
	*/
	protected synchronized void doLog( int priority, String text )
	{
		getCurrentThreadLogger().doLog(priority, 
			(moduleName != null ? (moduleName + " ") : "") + text);
	}

	/**
	* @return current thread's logger, creating one if necessary.
	*/
	protected Logger getCurrentThreadLogger( )
	{
		Thread th = Thread.currentThread();
		Logger lg = loggers.get(th.getName());
		if (lg != null)
			return lg;

		if (autoCreate)
		{
			String logname = prefix + th.getName() + suffix;
			try
			{
				lg = new FileLogger(procName, logname, maxLength);
				loggers.put(th.getName(), lg);
				return lg;
			}
			catch(FileNotFoundException ex)
			{
				defaultLogger.warning("Cannot create log file '" + logname
					+ "': " + ex);
			}
		}
		return defaultLogger;
	}

	/**
	* @return the output PrintStream for the logger in the current thread.
	*/
	public PrintStream getLogOutput( )
	{
		return getCurrentThreadLogger().getLogOutput();
	}

	public void setModuleName(String moduleName)
	{
		this.moduleName = moduleName;
	}
}

