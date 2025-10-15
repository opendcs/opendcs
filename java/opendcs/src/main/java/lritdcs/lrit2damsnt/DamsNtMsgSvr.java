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
package lritdcs.lrit2damsnt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

public class DamsNtMsgSvr extends BasicServer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static final byte[] none = "NONE\r\n".getBytes();
	private Lrit2DamsNt parent = null;

	public DamsNtMsgSvr(int port, InetAddress bindaddr, Lrit2DamsNt parent)
		throws IOException
	{
		super(port, bindaddr);
		this.parent = parent;
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket sock)
		throws IOException
	{
		return new DamsNtMsgSvrThread(this, sock);
	}

	/**
	 * Send the NONE message to all currently connect clients.
	 */
	public void sendNone()
	{
		log.debug("Sending NONE to clients.");
		sendToClients(none);
	}

	public void distribute(byte[] msgData)
	{
		log.debug("Distributing DCP Message '{}'", new String(msgData));
		sendToClients(msgData);
	}

	private void sendToClients(byte [] data)
	{
		ArrayList<DamsNtMsgSvrThread> badClients = new ArrayList<DamsNtMsgSvrThread>();
		for(BasicSvrThread bst : mySvrThreads)
		{
			DamsNtMsgSvrThread damsNtMsgSvrThread = (DamsNtMsgSvrThread)bst;
			try
			{
				damsNtMsgSvrThread.sendToClient(data);
			}
			catch (IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Error sending data to client '{}' -- will disconnect.",
				   		damsNtMsgSvrThread.getClientName());
				badClients.add(damsNtMsgSvrThread);
			}
		}

		// Can't do this in above loop because it would modify the collection that
		// I'm iterating.
		for(DamsNtMsgSvrThread client : badClients)
			client.disconnect();
	}
}
