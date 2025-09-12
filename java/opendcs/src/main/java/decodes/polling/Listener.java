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

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

import java.io.IOException;
import java.net.Socket;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

class Listener extends BasicServer implements Runnable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	boolean _shutdown = false;
	private ListeningPortPool portPool = null;
	
	public Listener(ListeningPortPool portPool, int port)
		throws IllegalArgumentException, IOException
	{
		super(port);
		this.portPool = portPool;
		log.debug("Listener.ctor portQueue.hashCode={}", portPool.portQueue.hashCode());
	}

	@Override
	public void run()
	{
		log.info("listener starting on port {}", getPort());
		log.debug("Listener.run portQueue.hashCode={}", portPool.portQueue.hashCode());
		while(!_shutdown)
		{
			try
			{
				super.listen();
			}
			catch (IOException ex)
			{
				log.atError().setCause(ex).log("Listen error.");
				_shutdown = true;
			}
			try { Thread.sleep(500L); } catch(InterruptedException ex) {}
		}
		log.info("listener exiting.");
	}

	@Override
	protected void serviceNewClient(Socket sock) throws IOException
	{
		// Make an IO Port
		IOPort ioPort = new IOPort(portPool, portPool.clientNum, null);
		ioPort.setSocket(sock);
		ioPort.setIn(sock.getInputStream());
		ioPort.setOut(sock.getOutputStream());
		
		ioPort.setPortName("Client(" + portPool.clientNum + ") ip=" + sock.getInetAddress().toString());
		log.info("New Client: {}", ioPort.getPortName());
		ioPort.setConfigureState(PollingThreadState.Waiting);
		portPool.clientNum++;
		portPool.enqueueIoPort(ioPort);
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException
	{
		// This is never called because we override serviceNewClient above.
		return null;
	}
	
}