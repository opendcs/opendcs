/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 14:50:18  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/02/28 15:57:17  mike
*  Don't add a runnable object to run queue if it's already present.
*
*  Revision 1.2  2000/05/22 20:32:16  mike
*  dev
*
*  Revision 1.1  2000/05/22 20:22:27  mike
*  created
*
*/
package ilex.gui;

/**
* This class periodically runs an 'updater' in the Corba thread.
*/
public class BackgroundCorbaUpdateThread extends Thread
{
	/** Run this periodically from the CORBA thread. */
	Runnable updater;
	/** Name of property that specifies update period. */
	String periodName;
	/** True until the die() method is called. */
	boolean running;

	/**
	* Constructs new thread for background CORBA updates.
	* Pass the Runnable updater, which will be called periodically.
	* Also pass the name of the property that specifies the period.
	* @param updater Runnable to call periodically.
	* @param periodName Period at which to call it.
	*/
	public BackgroundCorbaUpdateThread( Runnable updater, String periodName )
	{
		this.updater = updater;
		this.periodName = periodName;
		running = true;
	}

	/**
	* Do not call this method.
	* Call the Thread.start() on this object. The run() method will
	* continue until you call the die() method on this object, then
	* it will exit gracefully.
	*/
	public void run( )
	{
		// Do initial update immediately.
		GuiApp.getCorbaQueue().add(updater);
		while(running)
		{
			int sleepsecs = GuiApp.getIntProperty(periodName, 5);
			try { sleep(sleepsecs == 0 ? 5000L : (long)sleepsecs * 1000L); }
			catch (InterruptedException ie) {}
			if (sleepsecs != 0 && running)
			{
				GuiApp.getCorbaQueue().add(updater);
			}
		}
	}

	/**
	* Terminates this thread gracefully.
	*/
	public void die( )
	{
		running = false;
	}
}
