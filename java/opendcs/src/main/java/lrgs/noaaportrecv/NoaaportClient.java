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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicClient;

public class NoaaportClient extends BasicClient implements NoaaportConnection, Runnable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private NoaaportProtocol protocolHandler = null;
	private boolean _shutdown = false;
	private NoaaportRecv noaaportRecv = null;

	public NoaaportClient(String host, int port, NoaaportRecv noaaportRecv)
	{
		super(host, port);
		this.noaaportRecv = noaaportRecv;
	}

	public void run()
	{
		while(!_shutdown)
		{
			try
			{
				try { Thread.sleep(1000L); } catch(InterruptedException ex2) {}
				connect();
				protocolHandler = new NoaaportProtocol(socket.getInputStream(),
					noaaportRecv, this, getName());
				log.info("New connection to {}", getName());
				while(isConnected())
					protocolHandler.read();
			}
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("Error on connection to {}", getName());
				try { Thread.sleep(5000L); } catch(InterruptedException ex2) {}
			}
		}
	}

	public void shutdown()
	{
		_shutdown = true;
		disconnect();
	}
}
