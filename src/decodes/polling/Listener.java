package decodes.polling;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.Logger;

import java.io.IOException;
import java.net.Socket;

class Listener extends BasicServer implements Runnable
{
	boolean _shutdown = false;
	private ListeningPortPool portPool = null;
	
	public Listener(ListeningPortPool portPool, int port)
		throws IllegalArgumentException, IOException
	{
		super(port);
		this.portPool = portPool;
Logger.instance().debug1("Listener.ctor portQueue.hashCode=" + portPool.portQueue.hashCode());
	}

	@Override
	public void run()
	{
		Logger.instance().info(ListeningPortPool.module + " listener starting on port " + getPort());
Logger.instance().debug1("Listener.run portQueue.hashCode=" + portPool.portQueue.hashCode());
		while(!_shutdown)
		{
			try
			{
				super.listen();
			}
			catch (IOException ex)
			{
				Logger.instance().failure(ListeningPortPool.module + " Listen error: " + ex);
				_shutdown = true;
			}
			try { Thread.sleep(500L); } catch(InterruptedException ex) {}
		}
		Logger.instance().info(ListeningPortPool.module + " listener exiting.");
	}

	@Override
	protected void serviceNewClient(Socket sock) throws IOException
	{
		// Make an IO Port
		IOPort ioPort = new IOPort(portPool, portPool.clientNum, null);
		ioPort.setSocket(sock);
		ioPort.setIn(sock.getInputStream());
		ioPort.setOut(sock.getOutputStream());
		
		ioPort.setPortName("Client(" + portPool.clientNum + ") ip=" + sock.getInetAddress().toString());
		Logger.instance().debug1(ListeningPortPool.module + " New Client: " + ioPort.getPortName());
		ioPort.setConfigureState(PollingThreadState.Waiting);
		portPool.clientNum++;
		portPool.enqueueIoPort(ioPort);
	}

	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException
	{
		// This is never called because we override serviceNewClient above.
		return null;
	}
	
}