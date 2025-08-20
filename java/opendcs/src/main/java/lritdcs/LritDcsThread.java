/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2005/12/30 19:41:00  mmaloney
*  dev
*
*  Revision 1.2  2003/08/18 14:47:59  mjmaloney
*  bug fixes.
*
*  Revision 1.1  2003/08/11 01:33:58  mjmaloney
*  dev
*
*/
package lritdcs;

import java.util.*;
import ilex.util.*;

public abstract class LritDcsThread
	extends Thread
	implements Observer
{
	/// Stop the current execution, perhaps to reinit.
	protected boolean shutdownFlag;

	public LritDcsThread(String name)
	{
		super(name);
		shutdownFlag = false;
	}

	// Run method provided by sub-class. Should exit when shutdownFlag is set.

	public void shutdown()
	{
		shutdownFlag = true; 
	}

	public boolean isShutdown() { return shutdownFlag; }

	/**
	  Overloaded from the Oberserver pattern, this method is called whenever
	  the configuration has changed. It calls getConfigValues.
	*/
	public void update(Observable obs, Object obj)
	{
		getConfigValues(LritDcsConfig.instance());
	}

	protected void registerForConfigUpdates()
	{
		LritDcsConfig.instance().addObserver(this);
	}

	protected abstract void getConfigValues(LritDcsConfig cfg);

	public void debug1(String msg)
	{
		Logger.instance().log(Logger.E_DEBUG1,getName() + ": " + msg);
	}
	public void debug2(String msg)
	{
		Logger.instance().log(Logger.E_DEBUG2,getName() + ": " + msg);
	}
	public void debug3(String msg)
	{
		Logger.instance().log(Logger.E_DEBUG3,getName() + ": " + msg);
	}

	private String format(int evtnum, String msg)
	{
		String s = "LRIT";
		if (evtnum != 0)
			s = s + ":" + evtnum;
		if (msg.charAt(0) != '-')
			s = s + ' ';
		return s + msg + " (" + getName() + ")";
	}

	public void info(int evtnum, String msg)
	{
		Logger.instance().info(format(evtnum, msg));
	}
	public void warning(int evtnum, String msg)
	{
		Logger.instance().warning(format(evtnum, msg));
	}
	public void failure(int evtnum, String msg)
	{
		Logger.instance().failure(format(evtnum, msg));
	}
	public void fatal(int evtnum, String msg)
	{
		Logger.instance().fatal(format(evtnum, msg));
	}
}
