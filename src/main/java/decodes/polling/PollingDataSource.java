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

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import opendcs.dai.PlatformStatusDAI;
import decodes.datasource.DataSourceEndException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;
import decodes.datasource.EdlPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.RawMessage;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.TransportMedium;
import decodes.routing.DacqEventLogger;
import decodes.tsdb.DbIoException;
import decodes.util.PropertySpec;

public class PollingDataSource extends DataSourceExec
{
	public static final String module = "PollingDataSource";
	
	/** Max number of messages that can be queued waiting for routing spec to process them. */
	private int MAX_QUEUED = 100;
	
	/** A subordinate thread will wait this many milliseconds for space to be available in the queue */
	private long ENQUEUE_WAIT_MS = 10000L;
	
	/** The queue holding complete messages waiting for the routing spec to process */
	private ArrayBlockingQueue<RawMessage> rawMessageQueue = new ArrayBlockingQueue<RawMessage>(MAX_QUEUED);
	
	private EdlPMParser edlPMParser = new EdlPMParser();
	
	private RawMessage endMarker = new RawMessage();
	
	private PropertySpec[] pollingPropSpecs = 
	{
		new PropertySpec("portType", PropertySpec.DECODES_ENUM + "PortType",
			"Associates to a Java class to handle a pool of IO Ports."),
		new PropertySpec("availablePorts", PropertySpec.STRING,
			"For modem: Comma separated list of port numbers and ranges availabe for use (e.g. 1-9,12-16)."
			+ " For TCP, the number of simultaneous client sockets to allow."),
		new PropertySpec("digiIpAddr", PropertySpec.HOSTNAME,
			"Host name or IP Address of the Digi ConnectPort device."),
		new PropertySpec("digiPortBase", PropertySpec.INT,
			"Base port number for TCP PassThrough interface on Digi Device (default=2100)"),
		new PropertySpec("digiUserName", PropertySpec.STRING,
			"User name to login to digi telnet (port 23) for port configuration."),
		new PropertySpec("digiPassword", PropertySpec.STRING,
			"Password for digi telnet (port 23) for port configuration."),
		new PropertySpec("saveSessionFile", PropertySpec.STRING,
			"Optional filename template for saving raw session data. Should contain wildcards "
			+ "like $MEDIUMID and $DATE(yyyyMMdd-HHmmss) to prevent overwrite of previous sessions."),
		new PropertySpec("pollNumTries", PropertySpec.INT,
			"(default=1) A polling session is tried this many times, total, before declaring the poll"
			+ " to have failed."),
		new PropertySpec("maxBacklogHours", PropertySpec.INT,
			"Default = 48. Normally the poll will retrieve data back to the last time that data was "
			+ "retrieved for each station, plus 1 hour. This property limits the backlog in cases "
			+ "where a station has never been contacted, or it has been a very long time."),
		new PropertySpec("minBacklogHours", PropertySpec.INT,
			"Default = 2. Normally the poll will retrieve data back to the last time that data was "
			+ "retrieved for each station, plus 1 hour. This property ensures that at least a certain"
			+ " number of hours is polled."),
		new PropertySpec("authenticateClient", PropertySpec.STRING,
			"for Listening port types, if clients are required to athenticate to the server"
			+ ", then this property specifies how. Choices are 'none' and 'password=prompt=value'."
			+ " That is, the literal word 'password' followed by =, then the prompt to send to the client,"
			+ " then =, then the password value required from a client. "
			+ " Example: password=\\r\\npw? =ToPsEcReT"),
		new PropertySpec("listeningPort", PropertySpec.INT, "if portType = TcpListen, set this"
			+ " property to the TCP listening port. If not set, the default is 16050.")
		
	};
	
	private String authenticateClient = null;
	
	private PollingThreadController controller = null;
	private PortPool portPool = null;
	private PlatformStatusDAI platformStatusDAO = null;

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public PollingDataSource(DataSource ds, Database db)
	{
		super(ds,db);
	}

	@Override
	public void processDataSource()
		throws InvalidDatabaseException
	{
	}

	@Override
	public void init(Properties routingSpecProps, String since, String until, 
		Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		// The args in the dataSource form the default settings that can be
		// overridden by routing spec properties.
		Properties aggProps = new Properties(dbDataSource.arguments);
		PropertiesUtil.copyProps(aggProps, routingSpecProps);
		
		// Build the aggregate TM list from the network lists provided.
		// Include each TM only once even though it may be in several lists.
		ArrayList<TransportMedium> aggTMList = new ArrayList<TransportMedium>();
		for(NetworkList nl : networkLists)
		{
		  nextNLE:
			for(NetworkListEntry nle : nl.values())
			{
				try
				{
					Platform p = db.platformList.getPlatform(nl.transportMediumType,
						nle.transportId);
					if (p == null)
					{
						Logger.instance().warning(module + " No platform found for transport medium ("
							+ nl.transportMediumType + "," + nle.transportId + " -- skipped.");
						continue;
					}
					TransportMedium platformTM = p.getTransportMedium(nl.transportMediumType);
					if (platformTM == null)
					{
						Logger.instance().warning(module + " No transport medium of type '"
							+ nl.transportMediumType + "' in platform '" + p.getDisplayName() + " -- skipped.");
						continue;
					}
					for(TransportMedium tm : aggTMList)
						if (tm == platformTM)
							continue nextNLE;
					aggTMList.add(platformTM);
				}
				catch (DatabaseException ex)
				{
					String msg = module + " Error reading platform for medium ("
						+ nl.transportMediumType + "," + nle.transportId + "): " + ex;
					Logger.instance().failure(msg);
					continue;
				}
			}
		}
		
		Properties allProps = new Properties(dbDataSource.arguments);
		PropertiesUtil.copyProps(allProps, routingSpecProps);
		for(Object key : allProps.keySet())
		{
			String propName = (String)key;
			String value = allProps.getProperty(propName);
			if (TextUtil.startsWithIgnoreCase(propName, "sc:DCP_NAME"))
			{
				Platform p = db.platformList.getByFileName(value);
				if (!p.isComplete())
					try
					{
						p.read();
					}
					catch (DatabaseException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				if (p != null)
					for (TransportMedium tm : p.transportMedia)
						if (tm.getMediumType().toLowerCase().startsWith("polled"))
						{
							aggTMList.add(tm);
							Logger.instance().debug1(module + " sc:DCP_NAME='" + value + "', plat id="
								+ p.getId() + ", pakBusTableName='" + p.getProperty("pakBusTableName") + "'");
							break;
						}
			}
		}


		if (aggTMList.size() == 0)
			throw new DataSourceException(module + " There are no valid transport media in the network lists.");
		else
			log(Logger.E_INFORMATION, module + ": There are " + aggTMList.size() 
				+ " stations to be polled.");
		
		// Determine the port type and construct the port pool
		String portType = PropertiesUtil.getIgnoreCase(aggProps, "portType");
		if (portType == null)
			throw new DataSourceException(module + " Missing required portType property.");
		DbEnum portTypeEnum = db.getDbEnum("PortType");
		if (portTypeEnum == null)
			throw new DataSourceException(module + " This database does not have a PortType enumeration."
				+ " Did you run dbimport on edit-db/enum/PortType.xml?");
		EnumValue ev = portTypeEnum.findEnumValue(portType);
		if (ev == null)
			throw new DataSourceException(module + " The PortType enumeration does not have a match for "
				+ "the portType property setting '" + portType + "'. "
				+ "Check the property setting or try running rledit and adding.");
		try
		{
			Class<?> portPoolClass = ev.getExecClass();
			portPool = (PortPool)portPoolClass.newInstance();
			portPool.configPool(aggProps);
		}
		catch (ClassNotFoundException ex)
		{
			throw new DataSourceException(module + " PortType '" + portType + "' corresponds to "
				+ "class name '" + ev.getExecClassName() + "', which cannot be loaded. "
				+ "(Try checking the class name in rledit.): " + ex);
		}
		catch (InstantiationException ex)
		{
			throw new DataSourceException(module + " PortType '" + portType + "' corresponds to "
				+ "class name '" + ev.getExecClassName() + "', which cannot be instantiated: " + ex);
		}
		catch (IllegalAccessException ex)
		{
			throw new DataSourceException(module + " PortType '" + portType + "' corresponds to "
				+ "class name '" + ev.getExecClassName() + "', which is not accessible. ("
					+ "Contact support@covesw.com): " + ex);
		}
		catch (ConfigException ex)
		{
			throw new DataSourceException(module + " Cannot configure portPool: " + ex);
		}

		// Construct the PollingThreadController with the list
		if (portPool instanceof ListeningPortPool)
			controller = new ListeningThreadController(this, aggTMList, (ListeningPortPool)portPool);
		else
			controller = new PollingThreadController(this, aggTMList, portPool);
		
		String s = PropertiesUtil.getIgnoreCase(aggProps, "pollNumTries");
		if (s != null && s.trim().length() > 0)
			try { controller.setPollNumTries(Integer.parseInt(s)); }
			catch(NumberFormatException ex)
			{
				log(Logger.E_WARNING, "Invalid pollNumTries property '" + s 
					+ "' -- must be integer. Using default of " + controller.getPollNumTries() + ".");
			}
		
		s = PropertiesUtil.getIgnoreCase(aggProps, "maxBacklogHours");
		if (s != null && s.trim().length() > 0)
			try { controller.setMaxBacklogHours(Integer.parseInt(s)); }
			catch(NumberFormatException ex)
			{
				log(Logger.E_WARNING, module + " Invalid maxBacklogHours property '" + s 
					+ "' -- must be integer. Using default of " + controller.getMaxBacklogHours() + ".");
			}
		s = PropertiesUtil.getIgnoreCase(aggProps, "minBacklogHours");
		if (s != null && s.trim().length() > 0)
			try { controller.setMinBacklogHours(Integer.parseInt(s)); }
			catch(NumberFormatException ex)
			{
				log(Logger.E_WARNING, module + " Invalid minBacklogHours property '" + s 
					+ "' -- must be integer. Using default of " + controller.getMinBacklogHours() + ".");
			}
	
		s = PropertiesUtil.getIgnoreCase(aggProps, "saveSessionFile");
		if (s != null && s.trim().length() > 0)
			controller.setSaveSessionFile(s);
	
		
		platformStatusDAO = db.getDbIo().makePlatformStatusDAO();
		
		if (routingSpecThread != null && routingSpecThread.getMyExec() != null)
			controller.setDacqEventLogger(routingSpecThread.getMyExec().getDacqEventLogger());
		
		authenticateClient = PropertiesUtil.getIgnoreCase(aggProps, "authenticateClient");
		
		controller.start();
	}

	@Override
	public void close()
	{
		if (portPool != null)
			portPool.close();
		if (controller != null)
			controller.shutdown();
		controller = null;
		if (platformStatusDAO != null)
			platformStatusDAO.close();
	}

	@Override
//	public synchronized RawMessage getRawMessage()
	public RawMessage getRawMessage()
		throws DataSourceException
	{
		RawMessage ret = rawMessageQueue.poll();
		if (ret == endMarker)
			throw new DataSourceEndException("Polling complete.");
		return ret;
	}
	
	/**
	 * Called from the subordinate threads when a complete message has been 
	 * retrieved. Place the message in the queue for retrieval from the getRawMessage()
	 * method.
	 * @param rawMessage the complete raw message
	 */
//	public synchronized void enqueueMsg(RawMessage rawMessage)
	public void enqueueMsg(RawMessage rawMessage)
	{
		try
		{
			edlPMParser.parsePerformanceMeasurements(rawMessage);
		}
		catch (HeaderParseException ex)
		{
			log(Logger.E_FAILURE, "Failed to parse raw message header: " + ex + " -- message discarded");
			return;
		}
		
		// This will block if the queue already has MAX_QUEUED messages.
		try
		{
			if (rawMessageQueue.offer(rawMessage, ENQUEUE_WAIT_MS, TimeUnit.MILLISECONDS))
				return;
		}
		catch (InterruptedException e)
		{
		}
	}
	
	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return pollingPropSpecs;
	}

	/**
	 * Base class return true for backward compatibility.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}

	/**
	 * @param tm the transport medium
	 * @return the stored PlatformStatus or allocate a new one if none currently stored.
	 */
	public PlatformStatus getPlatformStatus(TransportMedium tm)
	{
		PlatformStatus ps = null;
		try
		{
			ps = platformStatusDAO.readPlatformStatus(tm.platform.getId());
		}
		catch(DbIoException ex)
		{
			log(Logger.E_WARNING, "Cannot get platform status for " + tm.getTmKey() + ": " + ex);
		}
		if (ps == null)
			ps = new PlatformStatus(tm.platform.getId());
		return ps;
	}

	public void writePlatformStatus(PlatformStatus ps, TransportMedium tm)
	{
		try { platformStatusDAO.writePlatformStatus(ps); }
		catch(DbIoException ex)
		{
			log(Logger.E_WARNING, "Cannot write platform status for platformID=" 
				+ ps.getPlatformId() + ", tm=" + tm.getTmKey() + ": " + ex);
		}
	}
	
	public void pollingComplete()
	{
		// Enqueue special end-marker raw message as a signal to the routing spec that we're done.
		try { rawMessageQueue.put(endMarker); } catch(InterruptedException ex) {}
	}

	public PollingThreadController getController()
	{
		return controller;
	}

	public String getAuthenticateClient()
	{
		return authenticateClient;
	}

	
}
