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

import ilex.net.BasicClient;
import decodes.db.TransportMedium;

/**
 * Implements Dialer for TCP Socket Connections.
 * In a TCP Connection, 'Dialing' involves opening the socket. It is passed
 * an IOPort with no connection established. The Transport Medium ID must be of
 * the form host[:port], where host can be a resolvable hostname or an IP address.
 * If port is omitted, 23 is assumed (the standard Telnet Port used by many
 * network DCPs.)
 */
public class TcpDialer extends Dialer
{
	private BasicClient basicClient = null;

	@Override
	public void connect(IOPort ioPort, TransportMedium tm, PollingThread pollingThread)
		throws DialException
	{
		// Medium ID should be of the form host[:port], where host can be
		// a resolvable hostname or an IP address.
		// Port defaults to 23, which is the standard telnet port.
		String host = tm.getMediumId();
		int port = 23;
		int colon = host.indexOf(':');
		if (colon > 0)
		{
			try
			{
				port = Integer.parseInt(host.substring(colon+1).trim());
				host = host.substring(0,colon);
			}
			catch(NumberFormatException ex)
			{
				throw new DialException("Invalid host string '" + host + "' -- expected port number after colon.");
			}
		}
		basicClient = new BasicClient(host, port);
		try
		{
			basicClient.connect();
		}
		catch (IOException ex)
		{
			throw new DialException("Cannot connect to '" + tm.getMediumId() + "': " + ex);
		}
		ioPort.setIn(basicClient.getInputStream());
		ioPort.setOut(basicClient.getOutputStream());
	}

	@Override
	public void disconnect(IOPort ioPort)
	{
		if (basicClient != null)
			basicClient.disconnect();
		basicClient = null;
		ioPort.setIn(null);
		ioPort.setOut(null);
	}

}
