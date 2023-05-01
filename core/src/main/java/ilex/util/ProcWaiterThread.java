/*
*  $Id$
*
*  $Log$
*  Revision 1.6  2009/10/26 17:08:42  mjmaloney
*  Log wrap issues
*
*  Revision 1.5  2009/09/09 14:19:52  mjmaloney
*  dev
*
*  Revision 1.4  2009/09/09 14:05:44  mjmaloney
*  dev
*
*  Revision 1.3  2009/09/09 14:00:05  mjmaloney
*  dev
*
*  Revision 1.2  2009/08/27 20:26:55  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/08/29 15:02:39  mjmaloney
*  Dev
*
*  Revision 1.3  2005/08/29 00:09:12  mjmaloney
*  Added callback mechanism so caller will know when child is done.
*
*  Revision 1.2  2004/08/30 14:50:29  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/07/07 14:26:22  mjmaloney
*  Created.
*
*/
package ilex.util;

import java.io.IOException;
import java.io.InputStream;

/**
You have to be careful when spawning processes from within Java to process
the stdout and stderr. Otherwise the sub-process could hang waiting for
IO to complete.
<p>
This class is a thread that waits for a background process to complete.
Any output from the process is converted to log messages.
</p>
*/
public class ProcWaiterThread extends Thread
{
	/**
	* The Process I'm waiting for
	*/
	private Process proc;

	/**
	* Name used for log messages
	*/
	private String name;

	/**
	 * Optional callback.
	 */
	private ProcWaiterCallback callback;

	/**
	 * Optional object to pass back to the callback.
	 */
	private Object callbackObj;
	
	public String cmdOutput = null;


	/**
	* Execute a command in the background, starting a ProcWaiterThread to
	* wait for the process and convert any output to log messages.
	* @param cmd the command
	* @param name the name for log messages
	* @throws IOException if the command could not be executed.
	*/
	public static void runBackground( String cmd, String name ) 
		throws IOException
	{
		Logger.instance().debug1("Executing '" + cmd + "' in background");
		Process proc = Runtime.getRuntime().exec(cmd);
		ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
		pwt.start();
	}
	
	/**
	 * Runs a command in the foreground and returns the output as a string.
	 * @param cmd
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public static String runForeground(String cmd, String name)
		throws IOException
	{
		Logger.instance().debug1("Executing '" + cmd + "' in foreground");
		Process proc = Runtime.getRuntime().exec(cmd);
		ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
		pwt.run();
		return pwt.cmdOutput;
	}

	/**
	* Execute a command in the background, starting a ProcWaiterThread to
	* wait for the process and convert any output to log messages.
	* @param cmd the command
	* @param name the name for log messages
	* @param callback object to notify when process exits.
	* @param callbackObj opaque object to pass to the callback.
	* @throws IOException if the command could not be executed.
	*/
	public static void runBackground( String cmd, String name, 
		ProcWaiterCallback callback, Object callbackObj)
		throws IOException
	{
		Logger.instance().debug1("Executing '" + cmd + "'");
		Process proc = Runtime.getRuntime().exec(cmd);
		ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
		pwt.setCallback(callback, callbackObj);
		pwt.start();
	}

	/**
	* Construct a ProcWaiterThread for a particular process.
	* @param proc the process
	* @param name the name for log messages
	*/
	private ProcWaiterThread( Process proc, String name )
	{
		this.proc = proc;
		this.name = name;
		callback = null;
		callbackObj = null;
	}

	/**
	 * Sets the optional callback.
	 */
	public void setCallback(ProcWaiterCallback callback, Object callbackObj)
	{
		this.callback = callback;
		this.callbackObj = callbackObj;
	}

	/**
	* Public run method
	*/
	public void run( )
	{
		// Start a separate thread to read the input stream.
		final InputStream is = proc.getInputStream();
		Thread isr = 
			new Thread()
			{
				public void run()
				{
					try
					{
						byte cmdOutBuf[] = new byte[4096];
						int cmdOutLen = 0;
						cmdOutLen = is.read(cmdOutBuf);
						if (cmdOutLen > 0)
						{
							cmdOutput = new String(cmdOutBuf, 0, cmdOutLen);
							Logger.instance().debug1(
								"cmd(" + name + ") stdout returned(" 
								+ cmdOutLen + ") '" + cmdOutput + "'");
						}
						else
							cmdOutput = null;
					}
					catch(IOException ex) {}
				}
			};
		isr.start();

		// Likewise for the stderr stream
		final InputStream es = proc.getErrorStream();
		Thread esr =
			new Thread()
			{
				public void run()
				{
					try
					{
						byte buf[] = new byte[1024];
						int n = es.read(buf);
						if (n > 0)
							Logger.instance().warning(
								"cmd(" + name + ") stderr returned(" + n + ") '"
								+ new String(buf, 0, n) + "'");
					}
					catch(IOException ex) {}
				}
			};
		esr.start();
 
		// Finally, wait for process and catch its exit code.
		try
		{
            int exitStatus = proc.waitFor();
            // Race-condition, after process ends, wait a half sec for
            // reads in isr & esr above to finish.
            sleep(500L);
			if (exitStatus != 0)
				Logger.instance().warning("cmd(" + name + ") exit status "
					+ exitStatus);
			if (callback != null)
				callback.procFinished(name, callbackObj, exitStatus);
        }
        catch(InterruptedException ex)
        {
        }
	}

	/**
	* Test main. Usage: java ilex.util.ProcWaiterThread <cmd> <name>
	* @param args  the args
	* @throws Exception on any error, printing stack trace.
	*/
	public static void main( String[] args ) throws Exception
	{
		System.out.println("Executing '" + args[0] + "' output: ");
		System.out.println(runForeground(args[0], args[1]));
	}
}
