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

import lrgs.lrgsmain.LrgsConfig;
import ilex.net.BasicSvrThread;
import ilex.net.BasicServer;

/**
Handles the parsing of messages from the NOAAPORT socket.
*/
public class NoaaportSvrThread extends BasicSvrThread implements NoaaportConnection
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private NoaaportProtocol protocolHandler = null;

	NoaaportSvrThread(BasicServer parent, Socket socket,
		NoaaportRecv noaaportRecv)
		throws IOException
	{
		super(parent, socket);
		try { socket.setSoTimeout(0); }
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("BasicSvrThread Cannot set read timeout to 0.");
		}


		// PDI NOAAPORT interface requires special protocol handler
		String rcvType = LrgsConfig.instance().noaaportReceiverType;
		if (rcvType.toLowerCase().contains("pdi"))
			protocolHandler = new PdiNoaaportProtocol(socket.getInputStream(),
				noaaportRecv, this, getClientName());
		else
			protocolHandler = new NoaaportProtocol(socket.getInputStream(),
				noaaportRecv, this, getClientName());

		log.info("New connection from {}, receiver type={}", getClientName(), rcvType);
	}

	/**
	 * Repeatedly called from base-class until connection is broken.
	 */
	protected void serviceClient()
	{
		protocolHandler.read();
	}

	public void disconnect( )
	{
		log.info("Disconnecting from {}", getClientName());
		super.disconnect();
	}
}
