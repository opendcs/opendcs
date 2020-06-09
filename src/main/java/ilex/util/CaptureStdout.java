/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:25  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/04/23 19:28:01  mjmaloney
*  Created CaptureStdout
*
*/
package ilex.util;

import java.io.*;


/*
  This class is useful when servers are started and released
  from the terminal. If an unexpected and uncaught exception 
  is thrown in any thread, the Runtime will send a message to
  stdout. Normally this is just discarded for background tasks.
  
  This class will capture any stdout/stderr output and convert
  it into E_FAILURE log messages.

  System.out and System.err are reset when you start this thead.
*/
public class CaptureStdout extends Thread
{
	ByteArrayOutputStream baos;
	public boolean shutdown;
	
	/** Constructor */
	public CaptureStdout( )
	{
	}

	/** The thread run method. */
	public void run( )
	{
		baos = new ByteArrayOutputStream();
		PrintStream myout = new PrintStream(baos);
		System.setOut(myout);
		System.setErr(myout);
		while(!shutdown)
		{
			try { sleep(1000L); }
			catch(InterruptedException ex) {}
			if (baos.size() > 0)
			{
				String s = baos.toString();
				baos.reset();
				Logger.instance().log(Logger.E_FAILURE, s);
			}
		}
	}

/*
	public static void main(String args[])
		throws IOException
	{
		FileLogger fl = new FileLogger("Test", "cap.log");
		Logger.setLogger(fl);

		Logger.instance().log(Logger.E_INFORMATION, "Starting");
		CaptureStdout cs = new CaptureStdout();
		cs.start();

		Logger.instance().log(Logger.E_INFORMATION, "After starting cs");

		Thread throwit = 
			new Thread()
			{
				public void run()
				{
					try { sleep(1000L); }
					catch(InterruptedException ex) {}

Logger.instance().log(Logger.E_INFORMATION, "Throwing ...");
					throw new IllegalStateException("Thrown from CaptureStdout.run!!!");
				}
			};
		Logger.instance().log(Logger.E_INFORMATION, "starting throwit");
		throwit.start();
	
		try { sleep(2000L); }
		catch(InterruptedException ex) {}
		throw new IllegalStateException("Thrown from main!!!");
	}
*/
}

