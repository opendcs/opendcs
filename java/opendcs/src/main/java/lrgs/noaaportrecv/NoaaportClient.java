package lrgs.noaaportrecv;

import java.io.IOException;

import ilex.net.BasicClient;
import ilex.util.Logger;

public class NoaaportClient 
	extends BasicClient 
	implements NoaaportConnection, Runnable
{
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
				Logger.instance().info(NoaaportRecv.module + " New connection to "
					+ getName());
				while(isConnected())
					protocolHandler.read();
			}
			catch(IOException ex)
			{
				Logger.instance().warning(NoaaportRecv.module
					+ " Error on connection to " + getName() + ": " + ex);
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
