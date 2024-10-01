/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.noaaportrecv;

import java.io.IOException;
import java.net.Socket;

import ilex.util.Logger;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

/**
This class is the TCP server that listens for connections from the Marta
NOAAPORT receiver.
*/
public class NoaaportListener
	extends Thread
{
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
		Logger.instance().info(noaaportRecv.module 
			+ " Listening for connections on port " + tcpServer.getPort());
		noaaportRecv.setStatus("Listening");
		while(!_shutdown)
		{
			try { tcpServer.listen(); }
			catch(IOException ex)
			{
				Logger.instance().failure(
					noaaportRecv.module + ":" + noaaportRecv.EVT_LISTEN_FAILED
					+ " Listening socket failed: " + ex);
				shutdown();
			}
		}
		Logger.instance().info(noaaportRecv.module + " Listener exiting.");
		tcpServer = null;
	}
}
