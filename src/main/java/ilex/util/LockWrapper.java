/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:28  mjmaloney
*  Javadocs
*
*  Revision 1.4  2000/09/07 14:33:56  mike
*  Remove debug println
*
*  Revision 1.3  2000/09/06 15:24:45  mike
*  Improved diagnostic messages.
*
*  Revision 1.2  2000/09/02 13:43:48  mike
*  Improved diagnostic.
*
*  Revision 1.1  2000/08/31 18:36:23  mike
*  Created
*
*
*/
package ilex.util;

import java.io.IOException;

/**
This class contains a main method that will execute a specified command 
wrapped with a ServerLock.
*/
public class LockWrapper
{
	/**
	* Main method. Syntax: java ilex.util.LockWraper lock-name command ...
	* @param args the arguments
	* @throws IOException if can't get the lock
	*/
	public static void main( String[] args ) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("Usage: LockWrapper <lock-name> <command...>");
			System.exit(1);
		}

		ServerLock mylock = new ServerLock(args[0]);
		if (mylock.obtainLock() == false)
			System.exit(0);
		mylock.releaseOnExit();

		String cmd = "";
		for(int i = 1; i < args.length; i++)
			cmd = cmd + args[i] + " ";

		// System.out.println("Executing '" + cmd + "'");
		Process proc = Runtime.getRuntime().exec(cmd);
		Runtime.getRuntime().addShutdownHook(new KillThread(proc));
		try { proc.waitFor(); }
		catch (InterruptedException ie) {}
		System.exit(0);
	}
}

class KillThread extends Thread
{
	private Process proc;
	/**
	* @param proc
	*/
	KillThread( Process proc )
	{
		this.proc = proc;
	}
	public void run( )
	{
		System.out.println("LockWrapper: Killing child process");
		proc.destroy(); 
	}
}
