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

package org.opendcs.odcsapi.appmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.opendcs.odcsapi.beans.ApiAppEvent;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiBasicClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiEventClient
	extends ApiBasicClient
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiEventClient.class);
	private Long appId = null;
	private long lastActivity = 0L;
	private String appName = null;
	private BufferedReader reader = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	private long pid = 0L;

	public ApiEventClient(Long appId, String hostname, int port, String appName, long pid)
	{
		super(hostname, port);
		this.appId = appId;
		this.appName = appName;
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.pid = pid;
		LOGGER.debug("ApiEventClient appId={}, host={}, port={}, name={}", appId, hostname, port, appName);
	}

	public void connect() 
		throws UnknownHostException, IOException
	{
		LOGGER.debug("Connecting to {}:{}", this.host, this.port);
		super.connect();
		reader = new BufferedReader(new InputStreamReader(this.input));
	}
	
	public ArrayList<ApiAppEvent> getNewEvents()
		throws IOException, WebAppException
	{
		LOGGER.debug("ApiEventClient.getNewEvents(), socket.isClosed={}, isConnected={}", socket.isClosed(), socket.isConnected());
		lastActivity = System.currentTimeMillis();
		ArrayList<ApiAppEvent> ret = new ArrayList<ApiAppEvent>();
		
		if (socket.isClosed() || !socket.isConnected())
		{
			disconnect();
			throw new WebAppException(ErrorCodes.IO_ERROR, "Event connection to " 
				+ appName + " was closed by app.");
		}
		else
		{
			LOGGER.debug("reading events, reader.ready={}",  reader.ready());
			int n = 0;
			while (reader.ready())
			{
				String line = reader.readLine();
				n++;
				String origLine = line;
				if (line == null)
				{
					disconnect();
					throw new WebAppException(ErrorCodes.IO_ERROR, "Event connection to " 
						+ appName + " was closed by app.");
				}
				// first word is priority, one of: WARNING, FAILURE, INFO, DEBUG1/2/3
				line = line.trim();
				int sp = line.indexOf(' ');
				if (sp < 0)
				{
					LOGGER.error("Read event line '{}' from {} with no priority -- skipped.", origLine, appName);
					continue;
				}
				ApiAppEvent ev = new ApiAppEvent();
				ev.setAppId(appId);
				ev.setAppName(appName);
				ev.setPriority(line.substring(0, sp));
			
				// Timestamp should now be on the left
				line = line.substring(sp).trim();
				if (line.length() < 17)
				{
					LOGGER.error("Read event line '{}' from {} with missing or short date/time field -- skipped.", origLine, appName);
					continue;
				}
				try
				{
					ev.setEventTime(sdf.parse(line));
				}
				catch(ParseException ex)
				{
					LOGGER.error("Read event line '{}' from {} with improper date/time field -- skipped.", origLine, appName);
					continue;
				}
				
				line = line.substring(17).trim();
				ev.setEventText(line);
				
				ret.add(ev);
			}
			LOGGER.debug("Read {} lines from socket.", n);
		}

		return ret;
	}

	public Long getAppId()
	{
		return appId;
	}

	public long getLastActivity()
	{
		return lastActivity;
	}

	public long getPid()
	{
		return pid;
	}
}
