/*
*  $Id$
*/
package ilex.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.opendcs.logging.JavaUtilLoggingBridge;

import java.io.*;


/**
* Concrete subclass of Logger that Logs messages to a file.
* This provides a facility to limit the size of the log file to some
* set maximum.  The default maximum size of a log file is 10 megabytes.
* <p>
* If a log file with the given name already exists, new messages are
* appended to the end of it.  When the log file reaches the maximum
* length, it will be closed, ".old" will be appended to its name, and
* then a new log file will be opened with the original name.
* </p>
* <p>
* This class is now a light weight wrapper around java.util.Logging and either
* a ConsoleHandler (/dev/stdout) or an Actual FileHandler
*/
public class FileLogger extends Logger
{
	/**
	* The Java Util Logging elements
	*/
	protected Handler handler = null;
	private static java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("");

	/**
	* Default length if none supplied by user.
	*/
	private static final int defaultMaxLength = 10000000;  // 10 meg.

	/**
	* User-settable maximum log file length.
	*/
	private int maxLength = defaultMaxLength;
	
	/**
	* Construct with a process name and a filename.
	* @param procName Name of this process, to appear in each log message.
	* @param filename Name of log file.
	* @throws FileNotFoundException if can't open file
	*/
	public FileLogger( String procName, String fileName )
		throws IOException
	{
		this(procName, fileName, defaultMaxLength);
	}

	public FileLogger(String procName, String fileName, int maxLength) throws IOException
	{
		this(procName,fileName,maxLength,2);
	}

	/**
	* Construct with a process name, a filename, and a maximum length.
	* When it reaches the maximum length, the log file will be closed,
	* ".old" will be appended to its name, and then a new log file will
	* be opened with the original name.
	* @param procName Name of this process, to appear in each log message.
	* @param filename Name of log file.
	* @param maxLength Maximum length of log file
	* @throws FileNotFoundException if can't open file
	*/
	public FileLogger( String procName, String filename, int maxLength, int maxCount )
		throws IOException
	{		
		super(procName);
		if("/dev/stdout".equalsIgnoreCase(filename))
		{
			// clear any existing console handlers
			for(Handler handler: julLogger.getHandlers())
			{
				if (handler instanceof ConsoleHandler)
				{
					julLogger.removeHandler(handler);
				}
			}
			handler = new ConsoleHandler();
		}
		else
		{
			final String filenameRoot = EnvExpander.expand(filename);
			this.maxLength = maxLength;
			handler = new FileHandler(filenameRoot+".%g",this.maxLength,maxCount,true);
		}
		
		handler.setFormatter(new JavaLoggerFormatter());
		handler.setLevel(Level.ALL); // Messages are filtered before arriving here
		julLogger.addHandler(handler);
	}

	/**
	* Close this log file.
	*/
	public void close( )
	{
	}

	/**
	* Logs a message.  The priority has already been checked to make sure
	* that this message should be logged.
	* This method is called from the base class log method.
	* @param priority the priority
	* @param text the formatted log message text
	*/
	public synchronized void doLog( int priority, String text )
	{
		Level lvl = JavaUtilLoggingBridge.mapPriorityToLevel(priority);
		julLogger.log(lvl, text);
	}

	/**
	 * Closes the current log, renames it with an aging extension, and
	 * opens a new log.
	 */
	public synchronized void rotateLogs()
	{
		// now do nothing. JUL fileHandler will rotate the logs.
	}

	

}
