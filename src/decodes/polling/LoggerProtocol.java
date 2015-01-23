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

import java.util.Date;

import lrgs.common.DcpMsg;
import decodes.db.TransportMedium;

public abstract class LoggerProtocol
{
	protected PollSessionLogger pollSessionLogger = null;
	protected PollingThread pollingThread = null;

	public LoggerProtocol()
	{
	}
	
	/**
	 * Perform any initialization and then attempt to login to the station 
	 * using user/password info in the passed transport medium object.
	 * @param port that is connect to the remote station
	 * @param tm the transport medium containing login credentials
	 * @throws LoginException on login failure
	 */
	public abstract void login(IOPort port, TransportMedium tm)
		throws LoginException;

	/**
	 * Interacts with the remote station to retrieve data since the specified
	 * time. 
	 * @param port The IOPort connected to the remote station
	 * @param tm the Transport Medium linking to information about the station (Platform, Site, Sensors, etc.)
	 * @param since The time from which to start retrieval
	 * @return a formatted DcpMsg containing the retrieved data
	 * @throws PollException if the session fails.
	 */
	public abstract DcpMsg getData(IOPort port, TransportMedium tm, Date since)
		throws ProtocolException;
	
	/**
	 * Performs any final handshaking required
	 * @param port The IOPort connected to the remote station
	 * @param tm the Transport Medium linking to information about the station (Platform, Site, Sensors, etc.)
	 */
	public abstract void goodbye(IOPort port, TransportMedium tm);
	
	/**
	 * The logger may need access to the DataSourceExec object for logging and parameters.
	 */
	public abstract void setDataSource(PollingDataSource dataSource);

	public void setPollSessionLogger(PollSessionLogger pollSessionLogger)
	{
		this.pollSessionLogger = pollSessionLogger;
	}

	public PollingThread getPollingThread()
	{
		return pollingThread;
	}

	public void setPollingThread(PollingThread pollingThread)
	{
		this.pollingThread = pollingThread;
	}
	
	public void annotate(String msg)
	{
		if (pollSessionLogger != null)
			pollSessionLogger.annotate(msg);
	}
}
