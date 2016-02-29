/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/05/16 00:17:15  mmaloney
*  dev
*
*  Revision 1.4  2004/09/02 13:09:04  mjmaloney
*  javadoc
*
*  Revision 1.3  2003/04/11 20:14:42  mjmaloney
*  dev
*
*  Revision 1.2  2003/04/09 19:38:03  mjmaloney
*  impl
*
*  Revision 1.1  2003/03/27 21:17:43  mjmaloney
*  drgs dev
*
*/
package lrgs.drgs;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

import ilex.util.Logger;
import ilex.net.BasicClient;

/**
This thread handles interaction with the DRGS events socket.
The client (me) sends a Poll sequence. The Server (DRGS) responds
with an event or a code indicating that no events are currently available.
*/
public class DrgsEvtThread 
	extends BasicClient
	implements Runnable 
{
	boolean configChanged;
	boolean running;
	boolean enabled;
	long lastConnectAttempt;
	static byte EventPoll[] = {(byte)'P', (byte)'\r', (byte)'\n' };

	private StringBuffer linebuf;
	private static SimpleDateFormat goesDateFormat = null;

	/**
	  Constructor.
	  Configuration is handled by the configure method.
	*/
	public DrgsEvtThread()
	{
		super("", 17011);
		configChanged = true;
		running = true;
		enabled = true;
		linebuf = new StringBuffer();

		if (goesDateFormat == null)
		{
			goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
			java.util.TimeZone jtz=java.util.TimeZone.getTimeZone("UTC");
			goesDateFormat.setCalendar(Calendar.getInstance(jtz));
		}
	}

	/**
	  Thread run method.
	  Until shutdown call is received, this method continually polls
	  for new events. If no events available, sleep 1 sec before trying
	  again.
	*/
	public void run()
	{
		try{ Thread.sleep(4000L); }         // sleep 4 sec. before going.
		catch(InterruptedException ex) {}
		while(running)
		{
			long now = System.currentTimeMillis();
			if (configChanged)
			{
				configChanged = false;
				disconnect();
				if (enabled)
					tryConnect();
			}

			if (enabled && !isConnected()
			 && now - getLastConnectAttempt() > 10000L)
				tryConnect();

			if (!isConnected())
			{
				try{ Thread.sleep(1000L); }
				catch(InterruptedException ex) {}
			}
			else
			{
				try 
				{ 
					DrgsEvent evt = getEvent();
					if (evt != null)
						logEvent(evt);
					else
					{	// 1 sec. pause waiting for more data to arrive.
						try { Thread.sleep(1000L); }
						catch(InterruptedException ex) {}
					}
				}
				catch(IOException ex)
				{
					log(Logger.E_WARNING,
						"Error on DAMS-NT Event Socket: " + ex);
					disconnect();
				}
			}
		}
		disconnect();
		if (enabled)
			log(Logger.E_INFORMATION, "Shutting down and exiting.");
	}

	/**
	  Configures the event thread.
	  @param host DRGS host name
	  @param port Port number for events
	  @param enable true if the events interface is enabled.
	*/
	void configure(String host, int port, boolean enable)
	{
		this.enabled = enable;
		if (!enabled)
			disconnect();

		if (!getHost().equalsIgnoreCase(host) || getPort() != port)
		{
			setHost(host);
			setPort(port);
			configChanged = true;
		}
		log(Logger.E_INFORMATION, " configured with " + host + ":" + port + " enabled=" + enabled);
	}

	/**
	  Tells this thread to shutdown and exit.
	*/
	void shutdown()
	{
		running = false;
	}

	private void tryConnect()
	{
		log(Logger.E_DEBUG1, "Attempting connection.");
		try { connect(); }
		catch(IOException ex)
		{
			log(Logger.E_WARNING, "Connection failed: " + ex);
			return;
		}
		log(Logger.E_INFORMATION, "Connected.");
	}

	/** Prints a log message with a host/port prefix. */
	private void log(int level, String text)
	{
		Logger.instance().log(level, getName() + ": " + text);
	}

	/** Returns string of the form "EvtSock(host:port)". */
	public String getName()
	{
		return "EvtSock(" + super.getName() + ")";
	}

	/**
	  Internal method that sends the poll and then gets the response.
	*/
	private DrgsEvent getEvent()
		throws IOException
	{
		output.write(EventPoll);
		output.flush();

		// Wait as long as 5 seconds for the response.
		linebuf.setLength(0);
		long now = System.currentTimeMillis();
		while(System.currentTimeMillis() - now < 5000L)
		{
			int avail = input.available();
			while(avail-- > 0)
			{
				char c = (char)input.read();
				if (c == '\n')
					return processLine(linebuf.toString());
				linebuf.append(c);
			}

			try { Thread.sleep(100L);}
			catch(InterruptedException ex) {}
		}
		// timeout waiting for response. disconnect to force re-sync.
		log(Logger.E_INFORMATION, 
			"Timeout waiting for response to poll -- disconnecting.");
		disconnect();
		return null;
	}

	/**
	  Logs the event to the current default logger.
	*/
	private void logEvent(DrgsEvent evt)
	{
		// For now, just log this event to the default logger.
		log(evt.priority, "DRGS Event Received: " + evt.text);
	}

	/** Processes the response from the DRGS. */
	private DrgsEvent processLine(String line)
	{
		StringTokenizer tokenizer = new StringTokenizer(line);
		int count = tokenizer.countTokens();
		if (count < 0)
		{
			log(Logger.E_WARNING, "Empty event response received.");
			return null;
		}

		String s = tokenizer.nextToken();
		if (s.equalsIgnoreCase("none"))
			return null;

		char c = s.charAt(0);
		if (!Character.isDigit(c))
		{
			log(Logger.E_WARNING, "Improper priority value in response '"
				+ line + "'");
			return null;
		}
		int priority = (int)c - (int)'0';
		// DRGS priorities are inverted from LRGS. Convert to one of the
		// value in Logger.
		priority = 7 - priority;
		if (priority < Logger.E_DEBUG3)
			priority = Logger.E_DEBUG3;
		if (priority > Logger.E_FATAL)
			priority = Logger.E_FATAL;

		if (!tokenizer.hasMoreTokens())
		{
			log(Logger.E_WARNING, "Missing timestamp value in response '"
				+ line + "'");
			return null;
		}

		s = tokenizer.nextToken();
		Date timestamp = goesDateFormat.parse(s, new ParsePosition(0));
		if (timestamp == null)
		{
			log(Logger.E_WARNING, "Invalid timestamp '" + s
				+ "' in response '" + line + "'");
			return null;
		}
		
		StringBuffer text = new StringBuffer();
		for(int i=0; tokenizer.hasMoreTokens(); i++)
		{
			if (i > 0)
				text.append(' ');
			text.append(tokenizer.nextToken());
		}

		return new DrgsEvent(priority, timestamp, text.toString());
	}
}
