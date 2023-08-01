package decodes.tsdb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.TimeZone;

import ilex.net.BasicServer;
import ilex.net.BasicSvrThread;
import ilex.util.Logger;
import ilex.util.QueueLogger;
import ilex.util.TeeLogger;

public class CompEventSvr extends BasicServer
{
	QueueLogger eventQueue = null;
	
	public CompEventSvr(int port)
		throws IOException
	{
		super(port);
	}
	
	/**
	 * Tees the current logger into a queue. Starts a thread with a listening
	 * socket on the specified port. Accepts connections, and sends events
	 * to clients.
	 */
	public void startup()
	{
		// Current logger is typically a FileLogger
		Logger fl = Logger.instance();
		TimeZone tmpTz = fl.getTz();
		eventQueue = new QueueLogger(fl.getProcName(),fl.getMinLogPriority());
		TeeLogger tl = new TeeLogger(fl.getProcName(), fl, eventQueue);
		tl.setTimeZone(tmpTz);
		Logger.setLogger(tl);
		
		Logger.instance().info("Will listen for event clients on port " 
			+ getPort());
		
		// Logger is now a Tee, going to original logger and our eventQueue
		
		Thread t = 
			new Thread()
			{
				public void run()
				{
					try { listen(); }
					catch(IOException ex)
					{
						Logger.instance().failure(
							"EventServer Listening Socket Error: " + ex);
					}
				}
			};
		t.start();
	}
	
	@Override
	protected BasicSvrThread newSvrThread(Socket sock) throws IOException
	{
		InetAddress sockaddr = sock.getInetAddress();
		try { return new CompEventSvrThread(this, sock, eventQueue); }
		catch(IOException ex)
		{
			Logger.instance().warning(
				"Error accepting connection from " + sockaddr.toString()
				+ " (disconnecting): " + ex);
			try { sock.close(); }
			catch(IOException ex2) {}
			return null;
		}
	}
}
