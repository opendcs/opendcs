/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.lrgsmain;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.StringTokenizer;

import ilex.util.EnvExpander;
import ilex.util.IndexRangeException;
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
public class AlarmHandler extends Thread implements ProcWaiterCallback
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atWarn().setCause(ex).log("invalid event queue index {} -- resync", qIndex);
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
			log.debug("config file '{}' does not exist or is not readable!", configFile.getPath());
			return;
		}
		if (configFile.lastModified() > lastConfigLoad)
		{
			log.info(" Reading config file '{}'", configFile.getPath());
			lastConfigLoad = System.currentTimeMillis();
			alarmAssoc.clear();
			LineNumberReader rdr = null;
			try (FileReader fr = new FileReader(configFile))
			{
				rdr = new LineNumberReader(fr);
				String line;
				while((line = rdr.readLine()) != null)
				{
					line = line.trim();
					if (line.startsWith("#"))
						continue;
					int idx = line.indexOf(':');
					if (idx < 1 || idx == line.length()-1)
					{
						log.warn("{}:{} bad syntax: no ':', need 'module:num cmd ...'",
								 configFile.getPath(), rdr.getLineNumber());
						continue;
					}
					String alarmModule = line.substring(0, idx);
					int idx2 = line.indexOf(' ', idx);
					if (idx2 == -1)
					{
						log.warn("{}:{} bad syntax, no ' ', need 'module:num cmd ...'" ,
								 configFile.getPath(), rdr.getLineNumber());
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
						log.atWarn()
						   .setCause(ex)
						   .log("{}:{} bad syntax, no num, need 'module:num cmd ...'",
						   		configFile.getPath(), rdr.getLineNumber());
						continue;
					}
					String cmd = line.substring(idx2+1);
					log.debug(" Associating {}:{} with cmd '{}'", alarmModule, alarmNum, cmd);
					alarmAssoc.add(new AlarmAssoc(alarmModule, alarmNum, cmd));
				}
			}
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Error reading config file '{}'", configFile.getPath());
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
								log.warn("could not start alarm process {}, cmd '{}': Already {} " +
										 "processes running.",
										 name, aa.cmd, runningProcs);
								return;
							}
							log.info("executing '{}'", aa.cmd);
							try
							{
								ProcWaiterThread.runBackground(
									aa.cmd, name, this, aa);
								runningProcs++;
							}
							catch(IOException ex)
							{
								log.atWarn()
								   .setCause(ex)
								   .log("Could not start alarm process {}, cmd '{}'", name, aa.cmd);
							}
						}
					}
					else
					{
						log.warn("Cannot start alarm {}:{} -- previous process has not yet finished.",
								 module, evtNum);
					}
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
		log.info(" Alarm process '{}' finished with exit status {}", procName, exitStatus);
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