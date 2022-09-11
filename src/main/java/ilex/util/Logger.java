/*
*  $Id$
*/
package ilex.util;

import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/**
* Abstract logging mechanism.  Most code can be written to write log
* messages to this interface.  The 'main' should establish a concrete
* logger and register it herein.
* <p>
* Example of code to log a message:
* </p>
* <p>
* In main:
* <pre>  Logger.setLogger(new StderrLogger("MyProcName"));</pre>
* </p>
* <p>
* In some subroutine:
* <pre>  Logger.instance().log(Logger.E_WARNING, "Uh-oh ...");</pre>
* </p>
* <p>
* There are six priorities defined.  In order from lowest to highest,
* they are E_DEBUG3, E_DEBUG2, E_DEBUG1, E_INFORMATION, E_WARNING,
* E_FAILURE, and E_FATAL.
* </p>
*/
public abstract class Logger
{
	protected static TimeZone tz;
	/** Process name to be included in log messages (optional). */
	protected String procName;

	/** Messages with priority lower than this will not be logged. */
	protected int minLogPriority;

	/** singleton instance for logging */
	private static Logger theLogger = null;

	/** used to format dates (default = "MM/DD/YYYY HH:MM:SS") */
	protected static DateFormat dateFormat
		= new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	static
	{
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		tz = TimeZone.getTimeZone("UTC");
	}

	// Constants for minLogPriority:

	/** Most verbose debug level (trace) */
	public static final int E_DEBUG3 = 0;

	/** More verbose debug level */
	public static final int E_DEBUG2 = 1;

	/** Minimum debug info included in log */
	public static final int E_DEBUG1 = 2;

	/** Informational messages that are not errors */
	public static final int E_INFORMATION = 3;

	/** Recoverable error messages. */
	public static final int E_WARNING = 4;

	/** Indicates failure of a requested or attempted operation. */
	public static final int E_FAILURE = 5;

	/** Fatal error: Indicates that the sending process will terminate. */
	public static final int E_FATAL = 6;

    /**
	* This indicates the default value for the minimum logging priority.
	*/
	public static final int E_DEFAULT_MIN_LOG_PRIORITY = E_INFORMATION;

	/** If true, include priority in output messages. */
	protected boolean usePriority;

	/** If true, include date/time stamp in output messages. */
	protected boolean useDateTime;

	/** If true, include process name in output messages. */
	protected boolean useProcName;
	
	protected boolean insideLog = false;

    /**
	* The priority names.
	* The index of this array matches the constants defined as the
	* priority numbers.
	*/
	public static final String[] priorityName = 
		{"DBG3   ","DBG2   ","DBG1   ","INFO   ","WARNING","FAILURE","FATAL "};

    /**
	* Construct with a process name.
	* The process name can be the empty string, if desired.
	* @param procName the process name to appear in log messages.
	*/
	protected Logger( String procName )
	{
		this.procName = procName;
		minLogPriority = E_DEFAULT_MIN_LOG_PRIORITY;
		usePriority = true;
		useDateTime = true;
		useProcName = false;
	}

	/**
	* Sets the singleton instance.
	* @param logger the new singleton Logger
	*/
	public static void setLogger( Logger logger )
	{
		theLogger = logger;
	}

	/**
	* Retrieves the singleton instance.
	* If no Logger has been set as the singleton Logger, then a
	* StderrLogger is created with a blank process name.
	* @return the singleton Logger
	*/
	public static Logger instance( )
	{
		if (theLogger == null)
		{
			theLogger = new StderrLogger("");
		}
		return theLogger;
	}

	/**
	* Logs a message if the priority is greater than or equal to
	* minLogPriority.
	* If priority is less than minLogPriority the message will be
	* discarded.
	* @param priority the message priority
	* @param text the message
	*/
	public void log( int priority, String text )
	{
		// Guard against endless recursion that could be called by a log message
		// being generated inside the doLog method.
		if (insideLog)
			return;
		insideLog = true;
		try
		{
			if (priority >= minLogPriority)
				doLog(priority, text);
		}
		finally
		{
			insideLog = false;
		}
	}

	/**
	* Sets the minimum log priority
	* @param minPriority minimu priority
	*/
	public void setMinLogPriority( int minPriority )
	{
		minLogPriority = minPriority;
	}

	/**
	* Retrieves minimum log priority
	* @return minimum priority
	*/
	public int getMinLogPriority( ) {
  	    return minLogPriority;
  	}

	/**
	* Sets the date formatter.
	* @param df DateFormat to use in formatting log messages
	*/
	public static void setDateFormat( DateFormat df )
	{
		dateFormat = df;
	}

	/**
	* Sets the time-zone used in message time-tags.
	* @param tz the TimeZone object
	*/
	public void setTimeZone( TimeZone tZone )
	{
		dateFormat.setTimeZone(tZone);
		tz = tZone;
	}

	/**
	 * Formats time into a string.
	 */
	public String formatTime(Date d)
	{
		return dateFormat.format(d);
	}

	/**
	* Retrieves process name associated with this logger.
	* @return the process name
	*/
	public String getProcName( ) {
  	    return procName;
  	}

	/**
	* Sets process name to be used in log messages.
	* @param nm the process name
	*/
	public void setProcName( String nm )
	{
		procName = nm;
	}

	/**
	* Call with false to omit the priority string in log messages.
	* @param tf true if you want priority string in formatted log messages.
	*/
	public void setUsePriority( boolean tf ) { usePriority = tf; }

	/**
	* Call with false to omit date/time stamp from log messages
	* @param tf true if you want date/time stamps in formatted log messages.
	*/
	public void setUseDateTime( boolean tf ) { useDateTime = tf; }

	/**
	* Call with true to include process name in log messages.
	* @param tf true to include process name in log messages
	*/
	public void setUseProcName( boolean tf ) { useProcName = tf; }

	/**
	* This must be defined in the subclass:
	*/
	public abstract void close( );

	/**
	* This must be defined in the subclass.
	* This should always log the message; the priority has already
	* been checked.
	* @param priority message priority
	* @param text formatted message text
	*/
	public abstract void doLog( int priority, String text );

	/**
	* This produces the "standard message" for a given text string.
	* The format is "<priority> <date> <time> <message>".
	* This is used by subclass' doLog() methods to produce the output
	* string.
	* @param priority the priority
	* @param text the text
	* @return formatted string ready for logging
	*/
	public String standardMessage( int priority, String text )
	{
		String ret = "";

		if (usePriority)
		{
			String pn = priority >= 0 && priority < priorityName.length ?
				priorityName[priority] : ("Priority " + priority);
			ret = pn + " ";
		}
		if (useDateTime)
		{
			String ds = dateFormat.format(new Date());
			ret = ret + ds + " ";
		}

		ret = ret + text;
		return ret;
	}

	/**
	* Returns the print-stream being used for output if there is one.
	* This method should not be used unless you are sure that the concrete
	* class supports direct output to its print stream.
	* The base class simply returns System.err.
	* @return PrintStream for direct log output.
	*/
	public PrintStream getLogOutput( )
	{
		return System.err;
	}

	/**
	* Convenience method, sends E_DEBUG3 level message.
	* @param msg the message
	*/
	public void debug3( String msg )
	{
		log(E_DEBUG3, msg);
	}

	/**
	* Convenience method, sends E_DEBUG2 level message.
	* @param msg the message
	*/
	public void debug2( String msg )
	{
		log(E_DEBUG2, msg);
	}

	/**
	* Convenience method, sends E_DEBUG1 level message.
	* @param msg the message
	*/
	public void debug1( String msg )
	{
		log(E_DEBUG1, msg);
	}

	/**
	* Convenience method, sends E_INFORMATION level message.
	* @param msg the message
	*/
	public void info( String msg )
	{
		log(E_INFORMATION, msg);
	}

	/**
	* Convenience method, sends E_WARNING level message.
	* @param msg the message
	*/
	public void warning( String msg )
	{
		log(E_WARNING, msg);
	}

	/**
	* Convenience method, sends E_FAILURE level message.
	* @param msg the message
	*/
	public void failure( String msg )
	{
		log(E_FAILURE, msg);
	}

	/**
	* Convenience method, sends E_FATAL level message.
	* @param msg the message
	*/
	public void fatal( String msg )
	{
		log(E_FATAL, msg);
	}
	
	/**
	 * Get the Time Zone used by the Logger.
	 * @return time zone
	 */
	public TimeZone getTz()
	{
		return tz;
	}
}

