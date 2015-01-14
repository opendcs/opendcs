package decodes.polling;

import java.io.FileWriter;
import java.io.IOException;
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

/**
 * This class manages a single polling session.
 */
public class PollingThread 
	implements Runnable
{
	public static final String module = "PollingThread";
	
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


	public PollingThread(PollingThreadController parent, PollingDataSource dataSource, 
		TransportMedium transportMedium)
	{
		super();
		this.parent = parent;
		this.dataSource = dataSource;
		this.transportMedium = transportMedium;
	}

	@Override
	public void run()
	{
		numTries++;
		Logger.instance().info(module + " polling " + 
			transportMedium.getMediumType() + ":" + transportMedium.getMediumId() + " attempt #" + numTries);
		PollingThreadState exitState = state;
		try
		{
			platformStatus = dataSource.getPlatformStatus(transportMedium); 

			if (!_shutdown)
				ioPort.connect(transportMedium);
			platformStatus.setLastContactTime(new Date());
			
			// Construct protocol according to transportMedium.loggerType
			if (!_shutdown)
				makeProtocol();
			
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
					FileWriter fw = new FileWriter(fn);
					pollSessionLogger = new PollSessionLogger(fw, p.getSiteName(false));
				}
				catch (IOException ex)
				{
					dataSource.log(Logger.E_WARNING, module + " Cannot open session log file: " + fn + ": " + ex);
					pollSessionLogger = null;
				}
				protocol.setPollSessionLogger(pollSessionLogger);
			}
			
			if (!_shutdown)
				protocol.login(ioPort, transportMedium);
			
			// Determine the since time from platform status, or use default.
			Date since = new Date(System.currentTimeMillis() - 3600000L * parent.getMaxBacklogHours());
			if (platformStatus.getLastContactTime() != null
			 && platformStatus.getLastContactTime().after(since))
				since = platformStatus.getLastContactTime();
			
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
			dataSource.log(Logger.E_FAILURE, msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
		}
		catch (ConfigException ex)
		{
			String msg = "Configuration error: " + ex;
			dataSource.log(Logger.E_FAILURE, msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
		}
		catch (LoginException ex)
		{
			String msg = "Error logging into the station: " + ex;
			dataSource.log(Logger.E_FAILURE, msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
		}
		catch (ProtocolException ex)
		{
			String msg = "Error communicating with station: " + ex;
			dataSource.log(Logger.E_FAILURE, msg);
			exitState = PollingThreadState.Failed;
			platformStatus.setLastErrorTime(new Date());
			platformStatus.setAnnotation(msg);
		}
		finally
		{
			if (ioPort != null)
				ioPort.disconnect();
			if (pollSessionLogger != null)
				pollSessionLogger.close();
			pollSessionLogger = null;
		}
		
Logger.instance().debug2(module + " writing final platform status for " + transportMedium);		
		dataSource.writePlatformStatus(platformStatus, transportMedium);
		
		// Parent will be calling getState(). Make sure the RS doesn't prematurely
		// exit because no states are waiting.
		state = exitState;
		parent.pollComplete(this);
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
}
