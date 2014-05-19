/*
 * Open source software by Cove Software, LLC
*/
package lrgs.iridium;

import java.io.IOException;
import java.net.Socket;

import ilex.util.Logger;
import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;

/**
This class is the TCP server that listens for connections from the Iridium
GSS (Gateway SBD Service).
*/
public class IridiumListener
	extends Thread
{
	private BasicServer tcpServer;
	private boolean _shutdown;
	private IridiumRecv iridiumRecv; // Parent LRGS Data Source
	int connectNum=0;

	public IridiumListener(IridiumRecv nr, int port)
		throws IOException
	{
		super("IridiumListener");
		this.iridiumRecv = nr;
		tcpServer = 
			new BasicServer(port)
			{
				protected BasicSvrThread newSvrThread(Socket sock)
					throws IOException
				{
					//sock.setTcpNoDelay(true);
					//sock.setSoTimeout(1800000); // 30 min timeout.
					iridiumRecv.setStatus("" 
						+ (mySvrThreads.size()+1) + " Connections");
					return new IridiumRecvThread(this, sock, iridiumRecv,
						connectNum++);
				}
			};
	}

	public synchronized void clientDisconnect()
	{
		iridiumRecv.setStatus("" + tcpServer.getNumSvrThreads() + " Connections");
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
		Logger.instance().info(iridiumRecv.module 
			+ " Listening for connections on port " + tcpServer.getPort());
		iridiumRecv.setStatus("Listening");
		while(!_shutdown)
		{
			try { tcpServer.listen(); }
			catch(IOException ex)
			{
				Logger.instance().failure(
					iridiumRecv.module + ":" + iridiumRecv.EVT_LISTEN_FAILED
					+ " Listening socket failed: " + ex);
				shutdown();
			}
		}
		Logger.instance().info(iridiumRecv.module + " Listener exiting.");
		tcpServer = null;
	}
}
