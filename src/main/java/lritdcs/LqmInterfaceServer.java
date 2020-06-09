/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2009/10/16 12:39:00  mjmaloney
*  LRIT updates
*
*  Revision 1.2  2009/08/24 13:39:33  shweta
*  Code added to listen to messages from LQM even in dormant mode.
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2005/12/30 19:40:59  mmaloney
*  dev
*
*  Revision 1.2  2004/05/21 18:27:44  mjmaloney
*  Release prep.
*
*  Revision 1.1  2004/05/06 21:48:13  mjmaloney
*  Implemented Lqm Server. Modified GUI to read events over the net.
*
*/
package lritdcs;

import java.util.StringTokenizer;
import java.net.*;
import java.io.IOException;

import ilex.net.*;
import ilex.util.Logger;

public class LqmInterfaceServer extends BasicServer
{
	
	
	
	public LqmInterfaceServer(int port)
		throws IOException
	{
		super(port);
		Logger.instance().warning("LRIT:"
        	+ Constants.EVT_NO_LQM + " No LQM Connected.");
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
				Logger.instance().info("LRIT:" 
					+ (-Constants.EVT_LQM_CON_FAILED) + " LQM Connected.");
				return ret;
			}
			catch(IOException ex)
			{
				Logger.instance().warning("LRIT:" 
					+ Constants.EVT_LQM_CON_FAILED
					+ " Error accepting connection from " + sockaddr.toString()
					+ " (disconnecting): " + ex);
				return null;
			}
		}
		try { sock.close(); }
		catch(IOException ex) {}
		return null;
	}

	private boolean isOK(InetAddress sockaddr)
	{
		Logger.instance().info(
			"LQM Interface Socket connection from " + sockaddr.toString());
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
				Logger.instance().info("LRIT:"
        			+ (-Constants.EVT_NO_LQM) + " LQM Connected.");
				return true;
			}
		}
		catch(UnknownHostException ex) 
		{ 
			Logger.instance().failure("LRIT:"
				+ Constants.EVT_LQM_BAD_CONFIG
				+ " LQM configuration address '" + lqmhost + "' is invalid: "
				+ ex + " -- Cannot accept ANY LQM connections!");
		}

		Logger.instance().warning("LRIT:" + Constants.EVT_LQM_INVALID_HOST
			+ " Rejecting LQM connection from unauthorized host " 
			+ sockaddr.toString());
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
					Logger.instance().info(
						"LRIT:" + (-Constants.EVT_LQM_LISTEN_ERR)
						+ " LQM Interface Listening on port " + port);
					try { lqmServer.listen(); }
					catch(IOException ex)
					{
						Logger.instance().failure(
							"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
							+ " LQM Interface Listening Socket Error: " + ex);
					}
					String fss = LritDcsConfig.instance().getFileSenderState();
					if (!fss.equalsIgnoreCase("dormant"))
						Logger.instance().failure(
							"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
							+ " LQM Interface Listening Socket Stopped.");
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
				Logger.instance().failure(
					"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
					+ " Error on LQM server: "
					+ ex + " -- no LQM connections will be accepted.");
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
				Logger.instance().info("LQM config changed. Closing old LQM connection ");
				shutdown();
			}
		}

		if (listeningSocket == null && cfg.getEnableLqm())
		{
			portNum = cfg.getLqmPort();
			try 
			{
				Logger.instance().info("LQM config changed. Opening new LQM connection ");
				makeServerSocket(); 
				//startListeningThread();
			}
			catch(Exception ex)
			{
				Logger.instance().failure(
					"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
					+ " Error on LQM server: "
					+ ex + " -- no LQM connections will be accepted.");
			}
		}
	}
	
	
}
