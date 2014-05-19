/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2013/08/23 20:12:01  mmaloney
*  TeeLogger should always pass message to both logger and let each handle its own
*  priority levels.
*
*  Revision 1.2  2013/03/25 18:35:28  mmaloney
*  Use priority from first logger to ctor.
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/09/07 17:44:09  mjmaloney
*  dev
*
*  Revision 1.3  2004/08/30 15:44:01  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.2  2004/08/30 14:50:31  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/09/02 14:37:28  mjmaloney
*  Added TeeLogger. Added more control on msg format to Logger.
*  Added TextUtil.fixedLengthFields method.
*
*/
package ilex.util;

import java.io.*;

/**
* TeeLogger logs to two other Loggers, which it holds internally.
* The event priority is controlled by the first logger on the constructor.
*/
public class TeeLogger extends Logger
{
	Logger logger1;
	Logger logger2;

	/**
	* Application constructs the loggers independently and passes them
	* here to the constructor.
	* @param procName the process name
	* @param logger1 the first logger
	* @param logger2 the second logger
	*/
	public TeeLogger( String procName, Logger logger1, Logger logger2 )
	{
		super(procName);
		this.logger1 = logger1;
		this.logger2 = logger2;
	}

	/**
	* @return the first logger
	*/
	public Logger getLogger1( ) { return logger1; }

	/**
	* @return the second logger
	*/
	public Logger getLogger2( ) { return logger2; }

	/**
	* Sends a log message to both loggers.
	* @param priority the priority
	* @param text the text
	*/
	public void log( int priority, String text )
	{
		logger1.log(priority, text);
		logger2.log(priority, text);
	}

	/**
	* This method delegates to logger1. Logger2 remains unchanged.
	* @param minPriority minimum priority to log
	*/
	public void setMinLogPriority( int minPriority )
	{
		logger1.setMinLogPriority(minPriority);
//		logger2.setMinLogPriority(minPriority);
	}

	/**
	* @return minimum log priority for logger1
	*/
	public int getMinLogPriority( ) 
	{
  	    return logger1.getMinLogPriority();
  	}

	/**
	* @return process name associated with logger1.
	*/
	public String getProcName( ) 
	{
		return logger1.getProcName();
  	}

	/**
	* Sets the process name for both loggers.
	* @param nm the process name
	*/
	public void setProcName( String nm )
	{
		logger1.setProcName(nm);
		logger2.setProcName(nm);
	}

	/**
	* Delegates to both loggers.
	* @param tf true to cause priority string to appear in messages
	*/
	public void setUsePriority( boolean tf ) 
	{
		logger1.setUsePriority(tf);
		logger2.setUsePriority(tf);
	}

	/** Closes both loggers. */
  	public void close( )
	{
		logger1.close();
		logger2.close();
	}

	/**
	* Delegates to both loggers.
	* @param priority the priority
	* @param text the formatted text
	*/
	protected void doLog( int priority, String text )
	{
		logger1.doLog(priority, text);
		logger2.doLog(priority, text);
	}

	/**
	* @return log output from logger1.
	*/
	public PrintStream getLogOutput( )
	{
		return logger1.getLogOutput();
	}
}

