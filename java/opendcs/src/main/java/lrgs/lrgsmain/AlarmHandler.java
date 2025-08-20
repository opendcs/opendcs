/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.2  2011/11/17 20:30:16  mmaloney
*  removed debug msg
*
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2007/08/22 14:23:17  mmaloney
*  dev
*
*  Revision 1.2  2005/09/11 21:40:32  mjmaloney
*  dev
*
*  Revision 1.1  2005/09/06 19:11:10  mjmaloney
*  dev
*
*/
package lrgs.lrgsmain;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.Vector;
import java.util.StringTokenizer;

import ilex.util.EnvExpander;
import ilex.util.IndexRangeException;
import ilex.util.Logger;
import ilex.util.QueueLogger;
import ilex.util.ProcWaiterThread;
import ilex.util.ProcWaiterCallback;

/**
This class is a thread that monitors the event queue for alarm assertions
and de-assertions. It also reads a configuration file containing alarm
numbers and processes to be executed. 
<p>
The config file has the format:
<p>
module:num command ...
<p>
The alarm 'num' is positive if the alarm is being asserted and negative
if it is being de-asserted. See alarm numbers defined within each module.
<p>
The configuration file is monitored for changes once every minute.
*/
public class AlarmHandler extends Thread
	implements ProcWaiterCallback
{
	/** This module's name. */
	public static final String module = "Alarm"; 

	/** The name of the configuration file */
	public static final String cfgFileName = "$LRGSHOME/alarm.conf";

	/** Maximum number of alarm processes that can run at a time. */
	public static final int maxAlarmProcs = 10;

	/** The last time (msec) the config file was loaded. */
	private long lastConfigLoad;

	/** shutdown flag. */
	private boolean isShutdown;

	/** The config file object. */
	private File configFile;

	/** Vector of alarm associations from the config file. */
	private Vector alarmAssoc;

	/** The Event Queue that we listen to. */
	private QueueLogger qLogger;

	/** Current number of running alarm processes. */
	private int runningProcs;

	/** Constructor. */
	public AlarmHandler(QueueLogger qLogger)
	{
		setName("AlarmHandler");
		lastConfigLoad = 0L;
		isShutdown = false;
		configFile = new File(EnvExpander.expand(cfgFileName));
		alarmAssoc = new Vector();
		this.qLogger = qLogger;
		runningProcs = 0;
	}

	public void shutdown()
	{
		isShutdown = true;
	}

	/** Thread run method. */
	public void run()
	{
		long lastConfigCheck = 0L;
		int qIndex = qLogger.getStartIdx();
		
		while(!isShutdown)
		{
			try { sleep(1000L); } catch(InterruptedException ex) {}
			long now = System.currentTimeMillis();
			if (now - lastConfigCheck >= 60000L)
			{
				lastConfigCheck = now;
				checkConfig();
			}
			try
			{
				String msg;
				while((msg = qLogger.getMsg(qIndex)) != null)
				{
					if (msg.length() == 0)
						continue;
					char c = msg.charAt(0);
					if (c != 'D')             // Ignore debug messages.
						processMsg(msg);
					qIndex++;
				}
			}
			catch(IndexRangeException ex)
			{
				Logger.instance().warning(module 
					+ " invalid event queue index " + qIndex + " -- resync.");
				qIndex = qLogger.getStartIdx();
			}
		}
	}

	/**
	 * Check to see if the configuration file has been modified since the
	 * last time it was loaded, and if so, reload it.
	 */
	private synchronized void checkConfig()
	{
		if (!configFile.canRead())
		{
			Logger.instance().debug2(module + 
				" config file '" + configFile.getPath() 
				+ "' does not exist or is not readable!");
			return;
		}
		if (configFile.lastModified() > lastConfigLoad)
		{
			Logger.instance().info(module + " Reading config file '"
				+ configFile.getPath() + "'");
			lastConfigLoad = System.currentTimeMillis();
			alarmAssoc.clear();
			LineNumberReader rdr = null;
			try
			{
				rdr = new LineNumberReader(new FileReader(configFile));
				String line;
				while((line = rdr.readLine()) != null)
				{
					line = line.trim();
					if (line.startsWith("#"))
						continue;
					int idx = line.indexOf(':');
					if (idx < 1 || idx == line.length()-1)
					{
						Logger.instance().warning(module + " "
							+ configFile.getPath() + ":" + rdr.getLineNumber()
							+ " bad syntax: no ':', need 'module:num cmd ...'");
						continue;
					}
					String alarmModule = line.substring(0, idx);
					int idx2 = line.indexOf(' ', idx);
					if (idx2 == -1)
					{
						Logger.instance().warning(module + " "
							+ configFile.getPath() + ":" + rdr.getLineNumber()
							+ " bad syntax, no ' ', need 'module:num cmd ...'");
						continue;
					}
					int alarmNum;
					try 
					{
						alarmNum = Integer.parseInt(
							line.substring(idx+1, idx2));
					}
					catch(NumberFormatException ex)
					{
						Logger.instance().warning(module + " "
							+ configFile.getPath() + ":" + rdr.getLineNumber()
							+ " bad syntax, no num, need 'module:num cmd ...'");
						continue;
					}
					String cmd = line.substring(idx2+1);
					Logger.instance().debug1(module + " Associating "
						+ alarmModule + ":" + alarmNum + " with cmd '"
						+ cmd + "'");
					alarmAssoc.add(new AlarmAssoc(alarmModule, alarmNum, cmd));
				}
			}
			catch(Exception ex)
			{
				Logger.instance().warning(module 
					+ " Error reading config file '" + configFile.getPath()
					+ "': " + ex);
			}
			finally
			{
				if (rdr != null)
				{
					try { rdr.close(); } catch(Exception ex) {}
				}
			}
		}
	}

	/**
	 * Process an event message.
	 * If the message is an alarm that we have a command-association for,
	 * execute the corresponding command.
	 */
	private synchronized void processMsg(String msg)
	{
		int len = msg.length();

		// fields are: PRIORITY DATE/TIME module:evtnum message ....
		// See if we can parse module and evtnum, and then look for a match.
		StringTokenizer st = new StringTokenizer(msg);
		String tok = "";
		for(int i=0; i<3; i++)
		{
			if (!st.hasMoreTokens())
				return;
			tok = st.nextToken();
		}
		int idx = tok.indexOf(':');
		if (idx == -1 || idx == 0 || idx >= tok.length()-1)
			return;
		String module = tok.substring(0, idx);
		int evtNum = 0;
		try 
		{
			String ens = tok.substring(idx+1);
			int slen = ens.length();
			if (ens.charAt(slen-1) == '-')
				ens = ens.substring(0, --slen);
			evtNum = Integer.parseInt(ens);
		}
		catch(NumberFormatException ex)
		{
			return;
		}
		for(int i=0; i<alarmAssoc.size(); i++)
		{
			AlarmAssoc aa = (AlarmAssoc)alarmAssoc.get(i);
			if (module.equalsIgnoreCase(aa.alarmModule))
			{
				if (evtNum == aa.alarmNum)
				{
					if (!aa.isRunning)
					{
						// Start the alarm process
						if (aa.cmd != null && aa.cmd.length() > 0)
						{
							String name = module + ":" + evtNum;
							if (runningProcs >= maxAlarmProcs)
							{
								Logger.instance().warning(module
									+ " could not start alarm process " + name
									+ ", cmd '" + aa.cmd+ "': Already " 
									+ runningProcs + " processes running.");
								return;
							}
							Logger.instance().info(module + " executing '"
								+ aa.cmd + "'");
							try 
							{
								ProcWaiterThread.runBackground(
									aa.cmd, name, this, aa);
								runningProcs++;
							}
							catch(IOException ex)
							{
								Logger.instance().warning(module
									+ " could not start alarm process " + name
									+ ", cmd '" + aa.cmd+ "': " + ex);
							}
						}
					}
					else
						Logger.instance().warning(module
							+ "Cannot start alarm " + module + ":" + evtNum
							+ " -- previous process has not yet finished.");
				}
			}
		}
	}

	/**
	 * Called from ProcWaiterThread when a alarm process has finished.
	 * @param procName of the form module:evtNum
	 * @param obj The AlarmAssoc object.
	 * @param exitStatus the exit status of the process.
	 */
	public synchronized void procFinished(String procName, Object obj, 
		int exitStatus)
	{
		runningProcs--;
		AlarmAssoc aa = (AlarmAssoc)obj;
		aa.isRunning = false;
		Logger.instance().info(module + " Alarm process '" + procName
			+ "' finished with exit status " + exitStatus);
	}

}

class AlarmAssoc
{
	String alarmModule;
	int alarmNum;
	String cmd;
	boolean isRunning;

	public AlarmAssoc(String alarmModule, int alarmNum, String cmd)
	{
		this.alarmModule = alarmModule;
		this.alarmNum = alarmNum;
		this.cmd = cmd;
		isRunning = false;
	}
}
