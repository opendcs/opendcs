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
package decodes.polling;

import ilex.util.PropertiesUtil;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.TransportMedium;

public class ListeningPortPool extends PortPool
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "ListeningPortPool";

	/** Set by the 'availablePorts' property, default=20, this is the maximum number of
	 * simultaneous clients that will be accepted.
	 */
	private int maxSockets = 50;

	/** Default listening socket port, set by 'listeningPort' property */
	private int listeningPort = 16050;

	int clientNum = 0;
	LinkedList<IOPort> portQueue = new LinkedList<IOPort>();

	private Listener listener = null;

	public ListeningPortPool()
	{
		super(module);
		log.debug("Constructing {}", module);
		log.debug("portQueue.hashCode={}", portQueue.hashCode());
	}

	@Override
	public void configPool(Properties dataSourceProps) throws ConfigException
	{
		String s = PropertiesUtil.getIgnoreCase(dataSourceProps, "availablePorts");
		if (s != null && s.trim().length() > 0)
		{
			try { maxSockets = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				throw new ConfigException(module + " invalid availablePorts value '" + s
					+ "'. Expected integer number of simultaneous clients.", ex);
			}
		}

		s = PropertiesUtil.getIgnoreCase(dataSourceProps, "listeningPort");
		if (s != null && s.trim().length() > 0)
		{
			try { listeningPort = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				throw new ConfigException(module + " invalid listeningPort value '" + s
					+ "'. Expected integer listening socket port number.");
			}
		}

		log.info("will listen on port {} and will allow {} simultaneous polling sessions.",
				 listeningPort, maxSockets);

		// start listening
		try
		{
			listener = new Listener(this, listeningPort);
			new Thread(listener).start();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("cannot listen on port {}", listeningPort);
		}
	}

	public synchronized void enqueueIoPort(IOPort ioPort)
	{
		portQueue.add(ioPort);
		log.debug("after add, there are {} socket connections in the queue.",
				  portQueue.size());
	}

	@Override
	public synchronized IOPort allocatePort()
	{
		return portQueue.isEmpty() ? null : portQueue.remove();
	}

	@Override
	public void releasePort(IOPort ioPort, PollingThreadState finalState,
		boolean wasConnectException)
	{
		try { ioPort.getIn().close(); }
		catch(IOException ex) {}

		try { ioPort.getOut().close(); }
		catch(IOException ex) {}

		try { ioPort.getSocket().close(); }
		catch(IOException ex) {}
	}

	@Override
	public int getNumPorts()
	{
		return maxSockets;
	}

	@Override
	public int getNumFreePorts()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void configPort(IOPort ioPort, TransportMedium tm) throws DialException
	{
		log.info("Configuring ioPort {} with" + ioPort.getPortNum()
			+ " with tm=" + tm.toString());
	}

	@Override
	public void close()
	{
		log.info("close()");
		if (listener != null)
		{
			listener.shutdown();
			listener._shutdown = true;
			listener = null;
			portQueue.clear();
		}

	}

}
