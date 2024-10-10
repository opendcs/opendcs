/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2004/06/01 15:26:37  mjmaloney
*  Created.
*
*/
package lrgs.lrgsmon;

import java.util.*;
import ilex.util.*;

/**
ThreadBase is the base class for threads in the LRGS Monitor
application. It provides convenience methods for logging and shutdown
control.
*/
public abstract class ThreadBase
	extends Thread
{
	/// Stop the current execution, perhaps to reinit.
	protected boolean shutdownFlag;

	/// Base class specifies name.
	public ThreadBase(String name)
	{
		super(name);
		shutdownFlag = false;
	}

	/// Call this method to shutdown this thread.
	public void shutdown()
	{
		shutdownFlag = true; 
	}

	/// Returns true if this thread has been shutdown.
	public boolean isShutdown() { return shutdownFlag; }

	/// Sends a debug level-1 log message with name of thread as prefix.
	public void debug1(String msg)
	{
		Logger.instance().debug1(getName() + ": " + msg);
	}

	/// Sends a debug level-2 log message with name of thread as prefix.
	public void debug2(String msg)
	{
		Logger.instance().debug2(getName() + ": " + msg);
	}

	/// Sends a debug level-3 log message with name of thread as prefix.
	public void debug3(String msg)
	{
		Logger.instance().debug3(getName() + ": " + msg);
	}

	/// Sends an informational log message with name of thread as prefix.
	public void info(String msg)
	{
		Logger.instance().info(getName() + ": " + msg);
	}

	/// Sends a warning log message with name of thread as prefix.
	public void warning(String msg)
	{
		Logger.instance().warning(getName() + ": " + msg);
	}

	/// Sends a failure log message with name of thread as prefix.
	public void failure(String msg)
	{
		Logger.instance().failure(getName() + ": " + msg);
	}

	/// Sends a fatal log message with name of thread as prefix.
	public void fatal(String msg)
	{
		Logger.instance().fatal(getName() + ": " + msg);
	}
}
