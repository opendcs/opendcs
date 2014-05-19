/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.13  2013/04/18 15:07:17  mmaloney
 * Fixed event thread reconnect issue.
 *
 * Revision 1.12  2013/04/18 15:02:53  mmaloney
 * dev
 *
 * Revision 1.11  2013/04/18 14:54:07  mmaloney
 * dev
 *
 * Revision 1.10  2013/04/18 14:49:33  mmaloney
 * dev
 *
 * Revision 1.9  2013/04/18 14:34:10  mmaloney
 * dev
 *
 * Revision 1.8  2013/04/18 13:53:28  mmaloney
 * Event socket bug fix.
 *
 * Revision 1.7  2013/04/17 15:50:57  mmaloney
 * dev
 *
 * Revision 1.6  2013/04/17 15:46:04  mmaloney
 * Insert app name just after the priority.
 *
 * Revision 1.5  2013/04/17 15:38:55  mmaloney
 * Insert app name just after the priority.
 *
 * Revision 1.4  2013/03/25 19:21:15  mmaloney
 * cleanup
 *
 * Revision 1.3  2013/03/25 17:50:54  mmaloney
 * dev
 *
 * Revision 1.2  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.1  2013/03/25 15:02:20  mmaloney
 * dev
 *
 */
package decodes.tsdb.procmonitor;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ilex.net.BasicClient;

public class EventClient
	extends BasicClient
	implements Runnable
{
	private boolean _shutdown = false;
	private ProcessMonitorFrame frame = null;
	private String appName = null;
	private BufferedReader reader = null;
	private AppInfoStatus appInfoStatus = null;
	private long lastConnectAttempt = 0L;
	
	public EventClient(int port, AppInfoStatus appInfoStatus, ProcessMonitorFrame frame)
	{
		super("placeholder", port);
		this.appInfoStatus = appInfoStatus;
		appName = appInfoStatus.getCompAppInfo().getAppName();
		appName = appName.trim();
		while (appName.length() < 10)
			appName = appName + " ";
		this.frame = frame;
	}
	
	public void shutdown() { _shutdown = true; }
	
	public void run()
	{
		while(!_shutdown)
		{
			if (!isConnected()
			 && appInfoStatus.getCompLock() != null
			 && !appInfoStatus.getCompLock().isStale()
			 && (System.currentTimeMillis() - lastConnectAttempt > 10000L))
			{
				try
				{
					setHost(appInfoStatus.getCompLock().getHost());
					String eps = appInfoStatus.getCompAppInfo().getProperty("EventPort");
					if (eps == null)
						throw new Exception("No 'EventPort' property. Cannot connect.");
					try { setPort(Integer.parseInt(eps)); }
					catch(NumberFormatException ex)
					{
						throw new Exception("Bad EventPort property '" + eps
							+ "' -- should be integer.");
					}
					connect();
					reader = new BufferedReader(new InputStreamReader(this.input));
				}
				catch (Exception ex)
				{
					localEvent("Cannot connect: " + ex);
				}
				lastConnectAttempt = System.currentTimeMillis();
			}
			if (isConnected())
			{
				if (appInfoStatus.getCompLock() == null)
				{
					localEvent("Lock removed -- disconnecting events");
					disconnect();
				}
				else if (appInfoStatus.getCompLock().isStale())
				{
					localEvent("Lock stale -- disconnecting events");
					disconnect();
				}
				else
				{
					try
					{
						if (this.socket.isClosed())
						{
							localEvent("Socket was closed -- disconnecting events");
							disconnect();
						}
						else
							while (reader.ready())
							{
								// Insert the app name just after the priority.
								StringBuilder sb = new StringBuilder(reader.readLine());
								if (sb.length() >= 8)
									sb.insert(8, appName + " ");
								frame.addEvent(sb.toString());
							}
					}
					catch (IOException ex)
					{
						localEvent("Error on event port: " + ex);
						disconnect();
					}
				}
			}
//else
//{
//if (appInfoStatus.getCompLock() == null)
//	localEvent("Not connected, complock is null.");
//else localEvent("Not connected, complock is "
//+ (!appInfoStatus.getCompLock().isStale() ? "NOT " : "") + "stale.");
//localEvent("Time since last connect attempt="
//+ (System.currentTimeMillis()-lastConnectAttempt));
//}
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
		}
		disconnect();
		frame.addEvent(appName + " " + " Event retrieval shut down.");
	}
	
	public void disconnect()
	{
		reader = null;
		lastConnectAttempt = 0L; // retry right away
		super.disconnect();
		localEvent("Socket closed on event port.");
	}
	
	private void localEvent(String msg)
	{
		frame.addEvent(appName + " " + msg);
	}
}
