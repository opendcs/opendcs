/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;

import lrgs.common.DcpMsg;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import decodes.datasource.RawMessage;
import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.TransportMedium;
import decodes.routing.DacqEventLogger;

/**
 * This class manages a single polling session.
 */
public class PollingThread 
	implements Runnable
{
	private String module = "PollingThread";
	
	/** The controller that owns this thread */
	private PollingThreadController parent;
	
	/** The data source is used for writing messages & setting status */
	private PollingDataSource dataSource;
	
	/** The IO Port that the controller allocated for this polling session */
	private IOPort ioPort = null;
	
	/** This thread will construct a protocol module according to transport medium's
	 * loggerType value.
	 */
	private LoggerProtocol protocol = null;
	
	/** Specifies the platform that this session will poll */
	private TransportMedium transportMedium = null;
	
	private PollingThreadState state = PollingThreadState.Waiting;
	
	private int numTries = 0;
	
	private boolean _shutdown = false;
	
	private PlatformStatus platformStatus = null;
	private String saveSessionFile = null;
	private PollSessionLogger pollSessionLogger = null;
	public static PrintStream staticSessionLogger = null;
	private PollException terminatingException = null;
	public static int backlogOverrideHours = 0;
	private DacqEventLogger dacqEventLogger = null;
	private int pollPriority = 3;
	private Date threadStart = null;

	public PollingThread(PollingThreadController parent, PollingDataSource dataSource, 
		TransportMedium transportMedium)
	{
		super();
		this.parent = parent;
		this.dataSource = dataSource;
		this.transportMedium = transportMedium;
		if (transportMedium.platform != null)
		{
			module = "Poll(" + transportMedium.platform.getSiteName(false) + ")";
			String pps = transportMedium.platform.getProperty("pollPriority");
			if (pps != null)
			{
				try 
				{
					pollPriority = Integer.parseInt(pps.trim());
					info("pollPriority set to " + pollPriority);
				}
				catch(NumberFormatException ex)
				{
					warning("Invalid pollPriority property '" + pps + "' -- ignored.");
				}
			}
		}
		dacqEventLogger = parent.getDacqEventLogger();
	}

	@Override
	public void run()
	{
		threadStart = new Date();
		numTries++;
		terminatingException = null;
		makeSessionLogger();
		String s = module + " " + 
			transportMedium.getMediumType() + ":" + transportMedium.getMediumId()
			+ " type=" + transportMedium.getLoggerType()
			+ " attempt #" + numTries;
		info(s);
		annotate(s);
		PollingThreadState exitState = state;
		try
		{
			platformStatus = dataSource.getPlatformStatus(transportMedium); 

			// Determine the since time from platform status, or use default.
			Date since = new Date(System.currentTimeMillis() - 3600000L * parent.getMaxBacklogHours());
			
			if (backlogOverrideHours != 0)
			{
				// backlog Override is used by poll GUI and command line utility.
				since = new Date(System.currentTimeMillis() - 3600000L * backlogOverrideHours);
			}
			else if (platformStatus.getLastMessageTime() != null
				  && platformStatus.getLastMessageTime().after(since))
			{
				// Backup 15 min fudge factor
				since = new Date(platformStatus.getLastMessageTime().getTime() - 15*60000L);
			}
			// Get at least the minimum number of hours.
			if (System.currentTimeMillis() - since.getTime() < parent.getMinBacklogHours() * 3600000L)
				since = new Date(System.currentTimeMillis() - parent.getMinBacklogHours() * 3600000L);
			
			debug1("Since time set to " + since);
			annotate("Since time set to " + since);

			if (!_shutdown)
				ioPort.connect(transportMedium, this);
			platformStatus.setLastContactTime(new Date());
			
			// Construct protocol according to transportMedium.loggerType
			if (!_shutdown)
				makeProtocol();
			protocol.setPollSessionLogger(pollSessionLogger);
			
			
			if (!_shutdown)
				protocol.login(ioPort, transportMedium);
			
			if (!_shutdown)
			{
				DcpMsg dcpMsg = protocol.getData(ioPort, transportMedium, since);
				RawMessage ret = new RawMessage(dcpMsg.getData());
				ret.setOrigDcpMsg(dcpMsg);
				ret.setPlatform(transportMedium.platform);
				ret.setTimeStamp(dcpMsg.getXmitTime());
				dataSource.enqueueMsg(ret);
				platformStatus.setLastMessageTime(new Date());
				platformStatus.setLastFailureCodes("" + dcpMsg.getFailureCode());
				platformStatus.setAnnotation("");
			}
			
			protocol.goodbye(ioPort, transportMedium);
			exitState = PollingThreadState.Success;
		}
		catch (DialException ex)
		{
			String msg = "Cannot connect IOPort: " + ex;
			if (numTries < parent.getPollNumTries())
				info(msg);
			else
				failure(msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
			terminatingException = ex;
		}
		catch (ConfigException ex)
		{
			String msg = "Configuration error: " + ex;
			failure(msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
			terminatingException = ex;
		}
		catch (LoginException ex)
		{
			String msg = "Error logging into the station: " + ex;
			failure(msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
			terminatingException = ex;
		}
		catch (ProtocolException ex)
		{
			String msg = "Error communicating with station: " + ex;
			failure(msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
			terminatingException = ex;
		}
		finally
		{
			if (ioPort != null)
				ioPort.disconnect();
			if (pollSessionLogger != null)
				pollSessionLogger.close();
			pollSessionLogger = null;
		}
		
		debug2("writing final platform status for " + transportMedium);		
		dataSource.writePlatformStatus(platformStatus, transportMedium);
		
		// Parent will be calling getState(). Make sure the RS doesn't prematurely
		// exit because no states are waiting.
		state = exitState;
		parent.pollComplete(this);
	}
	
	private void makeSessionLogger()
	{
		if (saveSessionFile != null)
		{
			Properties props = new Properties(System.getProperties());
			props.setProperty("MEDIUMID", transportMedium.getMediumId());
			Platform p = transportMedium.platform;
			if (p != null)
				props.setProperty("SITENAME", p.getSiteName(false));
			String fn = EnvExpander.expand(saveSessionFile, props);
			try
			{
				PrintStream ps = staticSessionLogger != null ? staticSessionLogger : new PrintStream(fn);
				pollSessionLogger = new PollSessionLogger(ps, p.getSiteName(false));
			}
			catch (IOException ex)
			{
				warning("Cannot open session log file: " + fn + ": " + ex);
				pollSessionLogger = null;
			}
		}

	}
	
	private void makeProtocol()
		throws ConfigException
	{
		String loggerType = transportMedium.getLoggerType();
		if (loggerType == null || loggerType.trim().length() == 0)
			throw new ConfigException("TransportMedium '" + transportMedium.getTmKey()
				+ "' has an undefined loggerType. This is required to determine how to "
				+ "communicate with the device.");
		DbEnum dbe = Database.getDb().getDbEnum("LoggerType");
		if (dbe == null)
			throw new ConfigException("Database does not have a LoggerType enumeration. "
				+ "Run dbimport on $DCSTOOL_HOME/edit-db/enum/LoggerType.xml");
		EnumValue ev = dbe.findEnumValue(loggerType);
		if (ev == null)
			throw new ConfigException("LoggerType enumeration does not have a value matching '"
				+ loggerType + "', which is used in tranposrt medium '" + transportMedium.getTmKey()
				+ "'. Correct the TM or add the loggerType value with the rledit command.");
		try
		{
			Class<?> protClass = ev.getExecClass();
			protocol = (LoggerProtocol)protClass.newInstance();
			protocol.setDataSource(dataSource);
			protocol.setPollingThread(this);
		}
		catch(ClassNotFoundException ex)
		{
			throw new ConfigException("LoggerType enumeration specifies exec class '"
				+ ev.getExecClassName() + "', which cannot be loaded. Check that the appropriate"
				+ " jar file is accessible, or correct the name with the rledit command: " + ex);
		}
		catch(Exception ex)
		{
			throw new ConfigException("LoggerType enumeration specifies exec class '"
				+ ev.getExecClassName() + "', which cannot be instantiated: " + ex);
		}
	}
	
	/** Prepare to rerun after a failure */
	public void reset()
	{
		state = PollingThreadState.Waiting;
		_shutdown = false;
	}

	public IOPort getIoPort()
	{
		return ioPort;
	}

	public void setIoPort(IOPort ioPort)
	{
		this.ioPort = ioPort;
		if (transportMedium.platform != null)
			module = "Poll(" + transportMedium.platform.getSiteName(false) + ":" + ioPort.getPortNum() + ")";

	}

	public PollingThreadState getState()
	{
		return state;
	}

	public int getNumTries()
	{
		return numTries;
	}

	public TransportMedium getTransportMedium()
	{
		return transportMedium;
	}
	
	public void shutdown()
	{
		_shutdown = true;
		if (protocol != null)
			protocol.setAbnormalShutdown(new AbortException("PollingThread.shutdown()"));
	}

	public PlatformStatus getPlatformStatus()
	{
		return platformStatus;
	}

	public void setSaveSessionFile(String saveSessionFile)
	{
		this.saveSessionFile = saveSessionFile;
	}

	public void setState(PollingThreadState state)
	{
		this.state = state;
	}
	
	public void info(String msg)
	{
		dataSource.log(Logger.E_INFORMATION, module + " " + msg);
		if (dacqEventLogger != null)
		{
			DacqEvent evt = new DacqEvent();
			evt.setPlatformId(transportMedium.platform.getId());
			evt.setSubsystem("Polling");
			evt.setEventPriority(Logger.E_INFORMATION);
			evt.setEventText(msg);
			dacqEventLogger.writeDacqEvent(evt);
		}
	}
	public void warning(String msg)
	{
		dataSource.log(Logger.E_WARNING, module + " " + msg);
		if (dacqEventLogger != null)
		{
			DacqEvent evt = new DacqEvent();
			evt.setPlatformId(transportMedium.platform.getId());
			evt.setSubsystem("Polling");
			evt.setEventPriority(Logger.E_WARNING);
			evt.setEventText(msg);
			dacqEventLogger.writeDacqEvent(evt);
		}
	}
	public void failure(String msg)
	{
		dataSource.log(Logger.E_FAILURE, module + " " + msg);
		if (dacqEventLogger != null)
		{
			DacqEvent evt = new DacqEvent();
			evt.setPlatformId(transportMedium.platform.getId());
			evt.setSubsystem("Polling");
			evt.setEventPriority(Logger.E_FAILURE);
			evt.setEventText(msg);
			dacqEventLogger.writeDacqEvent(evt);
		}
	}
	public void debug1(String msg) { dataSource.log(Logger.E_DEBUG1, module + " " + msg); }
	public void debug2(String msg) { dataSource.log(Logger.E_DEBUG2, module + " " + msg); }
	public void debug3(String msg) { dataSource.log(Logger.E_DEBUG3, module + " " + msg); }
	
	public void annotate(String msg)
	{
		if (pollSessionLogger != null)
			pollSessionLogger.annotate(msg);
	}

	public String getModule()
	{
		return module;
	}

	public PollException getTerminatingException()
	{
		return terminatingException;
	}

	public int getPollPriority()
	{
		return pollPriority;
	}

	public Date getThreadStart()
	{
		return threadStart;
	}
	
}
