/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/10/14 12:04:39  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/02/23 02:45:56  mmaloney
*  dev
*
*  Revision 1.4  2007/01/07 00:33:10  mmaloney
*  LRGS now uses SequenceFileLogger.
*
*  Revision 1.3  2004/08/30 14:50:21  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/03/27 21:17:55  mjmaloney
*  drgs dev
*
*  Revision 1.1  1999/11/18 17:05:29  mike
*  Initial implementation.
*
*/
package ilex.jni;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;

import ilex.util.Logger;

/**
* This class uses UNIX native code to trap signals. The signal
* definitions are in the interface SignalHandler.
* When one of these signals is seen it increments the public integer
* variables. The program can poll these variables periodically to
* tell if a signal occurred.
* Another way to handle signals is provided by the 'handle' functions.
* When you call one of these functions a thread is spawned to poll the
* variables and when a signal is detected, your handler is called.
*/
public class SignalTrapper
{
	static Thread ht;        // Handler Thread
	static LinkedList hmap;  // Handler Map

	/**
	* Checks if a particular signal was received.
	* @param sig the signal number.
	* @return true if signal was previously received.
	*/
	public static native boolean wasSignalSeen( int sig );

	/**
	* if yes_no == true, then trap signal, else don't
	* @param sig signal number
	* @param yes_no true if you want this signal trapped.
	*/
	public static native void trapSignal( int sig, boolean yes_no );

	/**
	* Resets a signal so it could be received again.
	* @param sig the signal number.
	*/
	public static native void resetSignal( int sig );

	/**
	* Set up a handler for a signal.
	* @param sig the signal number
	* @param handler the object to handle the signal
	* @see SignalHandler
	*/
	public static void setSignalHandler( int sig, SignalHandler handler )
	{
		addHandler(sig, handler);
		trapSignal(sig, true);
		if (ht == null)
		{
			ht = new Thread()
			{
				public void run()
				{
					while(true)
					{
    					// sleep 1 sec
						try { sleep(500L); }
						catch(InterruptedException ie) {}
						checkSignals();  // synchronized check signals
					}
				}
			};
			ht.start();
		}
	}

	/**
	* @param sig
	* @param handler
	*/
	private static synchronized void addHandler( int sig, SignalHandler handler )
	{
		if (hmap == null)
			hmap = new LinkedList();
		hmap.add(new HandlerMapEntry(sig, handler));
	}

	private static synchronized void checkSignals( )
	{
		ListIterator li = hmap.listIterator(0);
		while(li.hasNext())
		{
			HandlerMapEntry hme = (HandlerMapEntry)li.next();
			if (wasSignalSeen(hme.signal))
			{
				hme.handler.handleSignal(hme.signal);
				resetSignal(hme.signal);
			}
		}
	}

	static // Static initializer to load native library
	{
		String libname = "ilexjni." + getOsSuffix();
		Logger.instance().info("Loading native library " + libname);
		System.loadLibrary(libname);
	}
	
	public static String getOsSuffix()
	{
		String osn = System.getProperty("os.name");
		if (osn == null)
			return "unknown";
		osn = osn.toLowerCase();
		if (osn.startsWith("win"))
			return "win";
		else if (osn.startsWith("sunos"))
			return "sol10";
		try
		{
			Process uname = Runtime.getRuntime().exec("uname -rp");
			InputStreamReader isr = new InputStreamReader(
				uname.getInputStream());
			BufferedReader bis = new BufferedReader(isr);
			String line = bis.readLine();

			// RHEL3 is Kernel version 2.4.xxxxx
			if (line.startsWith("2.4")) 
				return "el3.32";
			int bits = 32;
			String n = System.getProperty("sun.arch.data.model");
			if (n != null && n.contains("64"))
				bits = 64;
			int rhelVersion = line.contains("el5") ? 5 : 4;
			return "el" + rhelVersion + "." + bits;
		}
		catch(IOException ex)
		{
			return "unknown";
		}
	}

}

class HandlerMapEntry
{
	int signal;
	SignalHandler handler;

	/**
	* @param s
	* @param h
	*/
	HandlerMapEntry( int s, SignalHandler h )
	{
		signal = s;
		handler = h;
	}
}
