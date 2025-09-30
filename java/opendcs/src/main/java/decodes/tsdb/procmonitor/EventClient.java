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
package decodes.tsdb.procmonitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicClient;

public class EventClient extends BasicClient implements Runnable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private boolean _shutdown = false;
	private ProcessMonitorFrame frame = null;
	private String appName = null;
	private BufferedReader reader = null;
	private AppInfoStatus appInfoStatus = null;
	private long lastConnectAttempt = 0L;
	private boolean justStarted = true;

	public EventClient(int port, AppInfoStatus appInfoStatus, ProcessMonitorFrame frame)
	{
		super("placeholder", port);
		this.appInfoStatus = appInfoStatus;
		appName = appInfoStatus.getCompAppInfo().getAppName();
		appName = appName.trim();
		while (appName.length() < 10)
			appName = appName + " ";
		this.frame = frame;
	}

	public void shutdown() { _shutdown = true; }

	public void run()
	{
		while(!_shutdown)
		{
			if (!isConnected()
			 && appInfoStatus.getCompLock() != null
			 && !appInfoStatus.getCompLock().isStale()
			 && (System.currentTimeMillis() - lastConnectAttempt > 10000L))
			{
				try
				{
					setHost(appInfoStatus.getCompLock().getHost());
					int port = -1;
					String eps = appInfoStatus.getCompAppInfo().getProperty("EventPort");
					if (eps != null) // Legacy: If an EventPort property exists, honor it.
					{
						try { port = Integer.parseInt(eps); }
						catch(NumberFormatException ex)
						{
							log.atWarn()
							   .setCause(ex)
							   .log("EventClient: Bad EventPort property '{}' " +
							   		"-- should be integer. Ignored. Will try PID.", eps);
							port = -1;
						}
					}
					if (port == -1) // No EventPort property. Try deriving from PID.
					{
						port = 20000 + (appInfoStatus.getCompLock().getPID() % 10000);
					}

					setPort(port);
					connect();
					reader = new BufferedReader(new InputStreamReader(this.input));
				}
				catch (Exception ex)
				{
					// If the host name matches the local host name, also try the loopback connector.
					try
					{
						localEvent("Cannot connect to '" + getHost() + "': " + ex);
						String localHostName = InetAddress.getLocalHost().getHostName();
						if (this.getHost().equalsIgnoreCase(localHostName))
						{
							setHost("localhost");
							connect();
							reader = new BufferedReader(new InputStreamReader(this.input));
						}
					}
					catch(Exception ex2)
					{
						ex2.addSuppressed(ex);
						log.atError().setCause(ex).log("Cannot connect to {}", getHost());
						localEvent("Cannot connect to '" + getHost() + "': " + ex2);
					}
				}
				lastConnectAttempt = System.currentTimeMillis();
			}
			if (isConnected())
			{
				if (appInfoStatus.getCompLock() == null)
				{
					localEvent("Lock removed -- disconnecting events");
					disconnect();
				}
				else if (appInfoStatus.getCompLock().isStale())
				{
					localEvent("Lock stale -- disconnecting events");
					disconnect();
				}
				else
				{
					try
					{
						if (this.socket.isClosed())
						{
							localEvent("Socket was closed -- disconnecting events");
							disconnect();
						}
						else
							while (reader.ready())
							{
								// Insert the app name just after the priority.
								StringBuilder sb = new StringBuilder(reader.readLine());
								if (sb.length() >= 8)
									sb.insert(8, appName + " ");
								frame.addEvent(sb.toString());
							}
					}
					catch (IOException ex)
					{
						log.atError().setCause(ex).log("Error on event port.");
						localEvent("Error on event port: " + ex);
						disconnect();
					}
				}
			}
			if (justStarted)
			{
				if (!isConnected()
				 && (appInfoStatus.getCompLock() == null || appInfoStatus.getCompLock().isStale()))
					localEvent("is not currently running -- will periodically retry.");
				justStarted = false;
			}
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
		}
		disconnect();
		frame.addEvent(appName + " " + " Event retrieval shut down.");
	}

	public void disconnect()
	{
		reader = null;
		lastConnectAttempt = 0L; // retry right away
		super.disconnect();
		localEvent("Socket closed on event port.");
	}

	private void localEvent(String msg)
	{
		frame.addEvent(appName + " " + msg);
	}
}