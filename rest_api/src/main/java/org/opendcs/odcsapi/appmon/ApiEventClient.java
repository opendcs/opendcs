package org.opendcs.odcsapi.appmon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.opendcs.odcsapi.beans.ApiAppEvent;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiBasicClient;

public class ApiEventClient
	extends ApiBasicClient
{
	private Long appId = null;
	private long lastActivity = 0L;
	private String appName = null;
	private BufferedReader reader = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	private long pid = 0L;

	public void connect() 
		throws UnknownHostException, IOException
	{
System.out.println("Connecting to " + this.host + ":" + this.port);
		super.connect();
		reader = new BufferedReader(new InputStreamReader(this.input));
	}
	
	public ApiEventClient(Long appId, String hostname, int port, String appName, long pid)
	{
		super(hostname, port);
		this.appId = appId;
		this.appName = appName;
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.pid = pid;
System.out.println("ApiEventClient appId=" + appId + ", host=" + hostname + ", port=" + port + ", name=" + appName);
super.debug = System.out;
	}
	
	public ArrayList<ApiAppEvent> getNewEvents()
		throws IOException, WebAppException
	{
System.out.println("ApiEventClient.getNewEvents(), socket.isClosed=" + socket.isClosed() + ", isConnected=" + socket.isConnected());
		lastActivity = System.currentTimeMillis();
		ArrayList<ApiAppEvent> ret = new ArrayList<ApiAppEvent>();
		
		if (socket.isClosed() || !socket.isConnected())
		{
			disconnect();
			throw new WebAppException(ErrorCodes.IO_ERROR, "Event connection to " 
				+ appName + " was closed by app.");
		}
		else
		{
System.out.println("reading events, reader.ready=" + reader.ready());
int n = 0;
			while (reader.ready())
			{
				String line = reader.readLine();
n++;
				String origLine = line;
				if (line == null)
				{
					disconnect();
					throw new WebAppException(ErrorCodes.IO_ERROR, "Event connection to " 
						+ appName + " was closed by app.");
				}
				// first word is priority, one of: WARNING, FAILURE, INFO, DEBUG1/2/3
				line = line.trim();
				int sp = line.indexOf(' ');
				if (sp < 0)
				{
					System.err.print("Read event line '" + origLine + "' from " + appName + " with no priority -- skipped.");
					continue;
				}
				ApiAppEvent ev = new ApiAppEvent();
				ev.setAppId(appId);
				ev.setAppName(appName);
				ev.setPriority(line.substring(0, sp));
			
				// Timestamp should now be on the left
				line = line.substring(sp).trim();
				if (line.length() < 17)
				{
					System.err.print("Read event line '" + origLine + "' from " + appName 
						+ " with missing or short date/time field -- skipped.");
					continue;
				}
				try { ev.setEventTime(sdf.parse(line)); }
				catch(ParseException ex)
				{
					System.err.print("Read event line '" + origLine + "' from " + appName 
							+ " with improper date/time field -- skipped.");
					continue;
				}
				
				line = line.substring(17).trim();
				ev.setEventText(line);
				
				ret.add(ev);
			}
System.out.println("Read " + n + " lines from socket.");
		}

		return ret;
	}

	public Long getAppId()
	{
		return appId;
	}

	public long getLastActivity()
	{
		return lastActivity;
	}

	public long getPid()
	{
		return pid;
	}
}
