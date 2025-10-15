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
package lritdcs;

import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.net.*;
import java.io.IOException;

import ilex.net.*;

public class UIServer extends BasicServer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public UIServer(int port)
		throws IOException
	{
		super(port);
	}

	protected BasicSvrThread newSvrThread(Socket sock)
	{
		// verify that inet address of this client is authorized.
		InetAddress sockaddr = sock.getInetAddress();
		if (isOK(sockaddr))
		{
			try { return new UISvrThread(this, sock); }
			catch(IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("LRIT:{}- Error accepting connection from {} (disconnecting)",
				   		Constants.EVT_UI_LISTEN_ERR, sockaddr.toString());
			}
		}
		log.warn("LRIT:{}- Rejecting UI connection from {}",
				 Constants.EVT_UI_INVALID_HOST, sockaddr.toString());
		try { sock.close(); }
		catch(IOException ex) {}
		return null;
	}

	private boolean isOK(InetAddress sockaddr)
	{
		log.info("User Interface Socket connection from {}", sockaddr.toString());
		// Localhost is always OK.
		InetAddress testaddr=null;
		try
		{
			testaddr = InetAddress.getByName("127.0.0.1");
			if (sockaddr.equals(testaddr))
				return true;
		}
		catch(UnknownHostException ex)
		{
			log.atWarn().setCause(ex).log("Cannot resolve localhost.");
		}

		// Check each addr/name in the configuration.
		String ailist = LritDcsConfig.instance().getUIIPAddresses();
		if (ailist == null)
			return false;
		StringTokenizer st = new StringTokenizer(ailist);
		while(st.hasMoreTokens())
		{
			String s = st.nextToken();
			try { testaddr = InetAddress.getByName(s); }
			catch(UnknownHostException ex) { continue; }

			if (sockaddr.equals(testaddr))
				return true;
		}
		return false;
	}
}