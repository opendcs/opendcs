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

import java.net.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;

import ilex.net.*;

public class LqmInterfaceServer extends BasicServer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public LqmInterfaceServer(int port)
		throws IOException
	{
		super(port);
		log.warn("LRIT:{} No LQM Connected.", Constants.EVT_NO_LQM);
	}

	protected BasicSvrThread newSvrThread(Socket sock)
	{
		// verify that inet address of this client is authorized.
		InetAddress sockaddr = sock.getInetAddress();
		if (isOK(sockaddr))
		{
			try
			{
				BasicSvrThread ret = new LqmInterfaceThread(this, sock);
				log.info("LRIT:{} LQM Connected.", (-Constants.EVT_LQM_CON_FAILED));
				return ret;
			}
			catch(IOException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("LRIT:{} Error accepting connection from {} (disconnecting): ",
				    	Constants.EVT_LQM_CON_FAILED, sockaddr.toString());
				return null;
			}
		}
		try { sock.close(); }
		catch(IOException ex) {}
		return null;
	}

	private boolean isOK(InetAddress sockaddr)
	{
		log.info("LQM Interface Socket connection from {}", sockaddr.toString());
		// Localhost is always OK.

		// Check each addr/name in the configuration.
		String lqmhost = LritDcsConfig.instance().getLqmIPAddress();
		if (lqmhost == null)
			return false;
		lqmhost = lqmhost.trim();
		try
		{
			InetAddress testaddr = InetAddress.getByName(lqmhost);
			if (sockaddr.equals(testaddr))
			{
				log.info("LRIT:{} LQM Connected.", (-Constants.EVT_NO_LQM));
				return true;
			}
		}
		catch(UnknownHostException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("LRIT:{} LQM configuration address '{}' is invalid -- Cannot accept ANY LQM connections!",
			   		Constants.EVT_LQM_BAD_CONFIG, lqmhost);
		}

		log.warn("LRIT:{} Rejecting LQM connection from unauthorized host {}",
				 Constants.EVT_LQM_INVALID_HOST, sockaddr.toString());
		return false;
	}

	/**
	  Starts a listening thread & calls this server's listen method.
	*/
	public void startListeningThread()
	{
		final LqmInterfaceServer lqmServer = this;
		final int port = portNum;
		Thread lqmServerThread =
			new Thread()
			{
				public void run()
				{
					log.info("LRIT:{} LQM Interface Listening on port {}",
							(-Constants.EVT_LQM_LISTEN_ERR), port);
					try { lqmServer.listen(); }
					catch(IOException ex)
					{
						log.atError()
						   .setCause(ex)
						   .log("LRIT:{} LQM Interface Listening Socket Error: ", Constants.EVT_LQM_LISTEN_ERR);
					}
					String fss = LritDcsConfig.instance().getFileSenderState();
					if (!fss.equalsIgnoreCase("dormant"))
					{
						log.error("LRIT:{} LQM Interface Listening Socket Stopped.",
								  Constants.EVT_LQM_LISTEN_ERR);
					}
				}
			};
		lqmServerThread.start();
	}

	public void updateConfig()
	{
		LritDcsConfig cfg = LritDcsConfig.instance();
		if (listeningSocket != null)
		{
			if (portNum != cfg.getLqmPort() || !cfg.getEnableLqm())
				shutdown();
			else
			{
				for(int i=0; i<mySvrThreads.size(); i++)
				{
					BasicSvrThread bst = (BasicSvrThread)mySvrThreads.get(i);
					if (!isOK(bst.getSocket().getInetAddress()))
					{
						bst.disconnect();
						i--;
					}
				}
			}
		}

		if (listeningSocket == null && cfg.getEnableLqm())
		{
			portNum = cfg.getLqmPort();
			try
			{
				makeServerSocket();
				startListeningThread();
			}
			catch(Exception ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("LRIT:{} Error on LQM server:  -- no LQM connections will be accepted.",
				   		Constants.EVT_LQM_LISTEN_ERR);
			}
		}

	}

	public void updateConfigDormant()
	{
		LritDcsConfig cfg = LritDcsConfig.instance();
		if (listeningSocket != null)
		{

			if (portNum != cfg.getLqmPort() || !cfg.getEnableLqm())
			{
				log.info("LQM config changed. Closing old LQM connection ");
				shutdown();
			}
		}

		if (listeningSocket == null && cfg.getEnableLqm())
		{
			portNum = cfg.getLqmPort();
			try
			{
				log.info("LQM config changed. Opening new LQM connection ");
				makeServerSocket();
			}
			catch(Exception ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("LRIT:{} Error on LQM server -- no LQM connections will be accepted.",
				   		Constants.EVT_LQM_LISTEN_ERR);
			}
		}
	}


}
