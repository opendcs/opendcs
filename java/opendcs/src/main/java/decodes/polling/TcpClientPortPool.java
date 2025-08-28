/*
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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

import ilex.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.TransportMedium;

public class TcpClientPortPool extends PortPool
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private int maxSockets = 32;
	public static final String module = "TcpClientPortPool";
	private ArrayList<IOPort> ioPorts = new ArrayList<IOPort>();
	private int portNum = 0;

	public TcpClientPortPool()
	{
		super(module);
		log.debug("Constructing {}", module);
	}

	@Override
	public void configPool(Properties dataSourceProps)
		throws ConfigException
	{
		String s = PropertiesUtil.getIgnoreCase(dataSourceProps, "availablePorts");
		if (s != null && s.trim().length() > 0)
		{
			try { maxSockets = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				throw new ConfigException(module + " invalid availablePorts value '" + s
					+ "'. Expected integer number of simultaneous sockets.", ex);
			}
		}
		log.debug("will allow {} simultaneous polling sessions.", maxSockets);
	}

	@Override
	public IOPort allocatePort()
	{
		if (ioPorts.size() >= maxSockets)
			return null;
		IOPort iop = new IOPort(this, portNum++, new TcpDialer());
		ioPorts.add(iop);
		log.trace("allocating IOPort");
		return iop;
	}

	@Override
	public void releasePort(IOPort port, PollingThreadState finalState, boolean wasConnectException)
	{
		ioPorts.remove(port);
	}

	@Override
	public int getNumPorts()
	{
		return maxSockets;
	}

	@Override
	public int getNumFreePorts()
	{
		return maxSockets - ioPorts.size();
	}

	@Override
	public void configPort(IOPort ioPort, TransportMedium tm) throws DialException
	{
		// Nothing to do here.

	}

	@Override
	public void close()
	{
		// Nothing to do
	}

}
