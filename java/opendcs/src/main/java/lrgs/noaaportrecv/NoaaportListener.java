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
package lrgs.noaaportrecv;

import java.io.IOException;
import java.net.Socket;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

/**
This class is the TCP server that listens for connections from the Marta
NOAAPORT receiver.
*/
public class NoaaportListener extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private BasicServer tcpServer;
	private boolean _shutdown;
	private NoaaportRecv noaaportRecv;

	public NoaaportListener(NoaaportRecv nr, int port)
		throws IOException
	{
		super("NOAAPORT");
		this.noaaportRecv = nr;
		tcpServer =
			new BasicServer(port)
			{
				protected BasicSvrThread newSvrThread(Socket sock)
					throws IOException
				{
					// Remove any existing clients - only allow 1.
					killAllSvrThreads();
					sock.setTcpNoDelay(true);
					sock.setSoTimeout(1800000); // 30 min timeout.
					noaaportRecv.setStatus("Connected");
					return new NoaaportSvrThread(this, sock, noaaportRecv);
				}
			};
	}

	public synchronized void shutdown()
	{
		_shutdown = true;
		if (tcpServer != null)
			tcpServer.shutdown();
		tcpServer = null;
	}

	public void run()
	{
		log.info("Listening for connections on port {}", tcpServer.getPort());
		noaaportRecv.setStatus("Listening");
		while(!_shutdown)
		{
			try { tcpServer.listen(); }
			catch(IOException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("{}:{} Listening socket failed",
				   		noaaportRecv.module, noaaportRecv.EVT_LISTEN_FAILED);
				shutdown();
			}
		}
		log.info("Listener exiting.");
		tcpServer = null;
	}
}
