/**
 * 
 */
package lrgs.ldds;

import ilex.util.Logger;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

import lrgs.ddsserver.DdsServer;

/**
 * @author mjmaloney
 *
 */
public class GetHostnameThread
    extends Thread
{
	private static int max=20;
	private LinkedBlockingQueue<LddsThread> ltq
		= new LinkedBlockingQueue<LddsThread>();
	public static final String module = "GetHostnameThread";
	private static GetHostnameThread _instance = null;
	
	public static GetHostnameThread instance()
	{
		if (_instance == null)
			_instance = new GetHostnameThread();
		return _instance;
	}
	
	public GetHostnameThread()
	{
		super("GetHostnameThread");
	}
	
	public synchronized void enqueue(LddsThread lt)
	{
		try
		{
Logger.instance().debug1(module + ".enqueue current size=" + ltq.size());
			while (ltq.size() >= max)
			{
				ltq.take();
			}
			ltq.put(lt);
Logger.instance().debug1(module + ".enqueue done, size=" + ltq.size());
		}
		catch(InterruptedException ex)
		{
			Logger.instance().warning(module + ".enqueue " + ex);
		}
	}
	
	private LddsThread dequeue()
	{
		try
        {
			Logger.instance().debug1(module +
				".dequeue getting LddsThread qsize=" + ltq.size()
				+ "...");
	        return ltq.take();
        }
        catch (InterruptedException ex)
        {
			Logger.instance().warning(module + ".dequeue " + ex);
			return null;
        }
	}


	public void run()
	{
		while(true)
		{
			LddsThread lt = dequeue();
			if (lt != null)
			{
				Socket sock = lt.getSocket();
				InetAddress ia = 
					sock != null ? sock.getInetAddress() : null;
				Logger.instance().debug1(module +
					" Trying name lookup for " + ia.toString());
					
				lt.setHostName(ia.getHostName());
				Logger.instance().debug1(module +
					" Done. Set name to '" + lt.getHostName() + "'");
			}
			else 
				Logger.instance().warning(module + ".dequeue returned null");
		}
	}
}
