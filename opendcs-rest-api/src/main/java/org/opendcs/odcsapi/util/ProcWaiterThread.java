/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
*  $Id: ProcWaiterThread.java,v 1.1 2023/05/29 16:21:14 mmaloney Exp $
*
*  $Log: ProcWaiterThread.java,v $
*  Revision 1.1  2023/05/29 16:21:14  mmaloney
*  Partial implementation of app status, events, start, & stop.
*  Fix bug with getting propspecs.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
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
package org.opendcs.odcsapi.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcWaiterThread.class);
	private static final String module = "ProcWaiterThread";
	
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
		debug("Executing '" + cmd + "' in background");
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
		debug("Executing '" + cmd + "' in foreground");
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
		debug("Executing '" + cmd + "'");
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
							debug("cmd(" + name + ") stdout returned(" 
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
							warning(
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
				warning("cmd(" + name + ") exit status "
					+ exitStatus);
			if (callback != null)
				callback.procFinished(name, callbackObj, exitStatus);
        }
        catch(InterruptedException ex)
        {
        }
	}

	/**
	* Test main. Usage: java ilex.util.ProcWaiterThread {@code<cmd> <name>}
	* @param args  the args
	* @throws Exception on any error, printing stack trace.
	*/
	public static void main( String[] args ) throws Exception
	{
		LOGGER.debug("Executing '{}' output: {}", args[0], runForeground(args[0], args[1]));
	}
	
	public static void debug(String msg)
	{
		LOGGER.debug("{} {}", module, msg);
	}
	public static void info(String msg)
	{
		LOGGER.info("{} {}", module, msg);
	}
	public static void warning(String msg)
	{
		LOGGER.warn("{} {}", module, msg);
	}

}
