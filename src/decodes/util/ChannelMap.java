/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2015/03/19 17:56:53  mmaloney
*  If lock is taken, continue to spawn maintenance thread to check for file changes.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.6  2013/03/28 19:19:32  mmaloney
*  User temp files are now placed under DCSTOOL_USERDIR which may be different
*  from DCSTOOL_HOME on linux/unix multi-user installations.
*
*  Revision 1.5  2013/02/28 16:40:21  mmaloney
*  dev
*
*  Revision 1.4  2011/11/16 19:28:21  mmaloney
*  Fix for new channel map file that DADDS is producing.
*
*  Revision 1.3  2009/10/30 18:53:32  mjmaloney
*  Switch to https://dcs1 urls
*
*  Revision 1.2  2008/09/12 15:41:29  mjmaloney
*  Mods for DCP Monitor 7.5
*
*  Revision 1.1  2008/09/08 19:14:03  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/12/04 14:28:33  mmaloney
*  added code to download channels from url
*
*  Revision 1.7  2005/10/10 19:45:48  mmaloney
*  dev
*
*  Revision 1.6  2005/09/28 21:56:11  mmaloney
*  Implement stream load method.
*
*  Revision 1.5  2005/09/06 15:29:51  mjmaloney
*  Added decins-6-4.xml build file
*
*  Revision 1.4  2004/09/23 13:41:53  mjmaloney
*  javadoc clean-up
*
*  Revision 1.3  2004/03/31 14:16:33  mjmaloney
*  Updates to DCP Monitor.
*
*  Revision 1.2  2004/03/18 16:18:41  mjmaloney
*  Working server version beta 01
*
*  Revision 1.1  2004/02/29 20:48:23  mjmaloney
*  Alpha version of server complete.
*
*/
package decodes.util;

import java.io.*;
import java.net.*;
import java.util.*;

import ilex.util.EnvExpander;
import ilex.util.Logger;
//import ilex.util.ServerLock;

/**
Contains an array of channels, baud rates, and types.
*/
public class ChannelMap
{
	private static ChannelMap _instance = null;

	/** Array of baud rates, indexed by channel number. */
	private String channelBauds[];
	
	public static final int NUM_CHANNELS = 1000;

	/** Array of chars for each channel: S=SelfTimed, R=Random */
	private char channelTypes[];

	private boolean _isLoaded = false;

	private ChanMaintenanceThread mthread = null;

	
	public static final String module = "ChannelMap";

	/** Constructor called from DcpMonitorConfig. */
	public ChannelMap()
	{
		channelBauds = new String[NUM_CHANNELS];
		channelTypes = new char[NUM_CHANNELS];
		setMapToDefaults();
	}
	
	/** @return the singleton instance of the ChannelMap. */
	public static ChannelMap instance()
	{
		if (_instance == null)
			_instance = new ChannelMap();
		return _instance;
	}

	public boolean isLoaded() { return _isLoaded; }

	/** Initializes the map to all 100-baud, self-timed channels. */
	public void setMapToDefaults()
	{
		for(int i=0; i<channelBauds.length; i++)
		{
			channelBauds[i] = null;
			channelTypes[i] = 'S';
		}
	}

	/** 
	  Loads the map from the specified file. 
	  Log messages will be generated for any exceptions encountered.
	*/
	public synchronized void loadFromUrl(String urlstr)
	{
		Logger.instance().debug3("Loading channel map from URL '"+urlstr+"'");

		try
		{
			URL channelMapURL = new URL(urlstr);
			InputStream istrm = channelMapURL.openStream();
			load(new LineNumberReader(new InputStreamReader(istrm)));
		}
		catch(Exception ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Cannot read channel map '" + urlstr
				+ "': " + ex);
		}
	}

	/**
	 * Loads the channel map from the passed input stream.
	 * The stream will be closed after the map is loaded.
	 * @param istrm the input stream
	 * @param name the name of the stream for any error messages.
	 */
	public boolean load(File file)
	{
		Logger.instance().info(
			"Loading Channel Map from '" + file.getPath() + "'");
		try
        {
	        return load(new LineNumberReader(new FileReader(file)));
        }
        catch (FileNotFoundException ex)
        {
	        Logger.instance().warning("Cannot load channel map from '"
	        	+ file.getPath() + "': " + ex);
	        return false;
        }
	}
	
	private synchronized boolean load(LineNumberReader bfr)
	{
		try
		{
			String line;
			while((line = bfr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (!Character.isDigit(line.charAt(0)))
					continue;
				// find non-digit after the channel number
				int idx = 1;
				for(; idx<line.length() && Character.isDigit(line.charAt(idx)); idx++);

				int chan;
				try { chan = Integer.parseInt(line.substring(0, idx)); }
				catch(Exception ex) { continue; }
				if (chan < 0)
				{
					Logger.instance().warning(module + " Invalid channel number "
						+ chan + " on line " + bfr.getLineNumber() + " -- skipped.");
					continue;
				}
				
				line = line.substring(idx).trim();
				if (line.length() == 0)
				{
					Logger.instance().warning(module + " Empty channel number "
						+ chan + " on line " + bfr.getLineNumber() + " -- skipped.");
					continue;
				}
				
				channelTypes[chan] = line.charAt(0);
				
				line = line.substring(1).trim();

				if (line.length() == 0)
				{
					Logger.instance().warning(module + " No baud for chan "
						+ chan + " on line " + bfr.getLineNumber() + " -- skipped.");
					continue;
				}
				if (line.length() > 1 && line.charAt(0) == ',')
					line = line.substring(2).trim();
				
				channelBauds[chan] = line;
			}
			bfr.close();
			Logger.instance().info("Channel map successfully loaded.");
			_isLoaded = true;
			return true;
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot load channel map: " + ex);
			return false;
		}
	}

	/**
	  Return baud rate for a particular channel.
	  @param chan the channel number (1...266)
	  @return baud rate for a particular channel.
	*/
	public int getBaud(int chan)
	{
		if (chan < 0 || chan > channelBauds.length)
			return 0;
		String s = channelBauds[chan];
		if (s == null)
			return 0;
		try { return Integer.parseInt(s); }
		catch(Exception ex)
		{
			return 0;
		}
	}
	
	/**
	  Return true if the specified channel is a Random channel.
	  @param chan the channel number
	  @return true if the specified channel is a Random channel.
	*/
	public boolean isRandom(int chan)
	{
		return (chan < 0 || chan > 266) ? true : channelTypes[chan] == 'R';
	}

	/**
	  Dumps the map to a writer.
	  This is NOT just for debug. The DCP Monitor uses it to send the
	  map to the Perl CGI script, so do not alter the format!
	  @param output the writer.
	*/
	public void dumpMap(Writer output)
		throws IOException
	{
		for(int i=0; i<channelBauds.length; i++)
			output.write("channel[" + i + "]: " + channelTypes[i]
				+ " " + channelBauds[i] + "\n");
	}
	
	/**
	 * Starts a background thread the periodically downloads the Channel Map
	 * File from https://dcs1.noaa.gov/chans_by_baud.txt
	 * URL and independently checks a local file for changes.
	 * The URL is downloaded every 16 hours.
	 * The file is checked for changes every 10 minutes.
	 * @param channelsUrl the URL to download Channels from, 
	 * null if no download.
	 * @param localfn - the local file to check periodically for changes.
	 */
	public void startMaintenanceThread(String channelsUrl, String localfn)
	{
		if (mthread == null)
		{
			mthread = new ChanMaintenanceThread(this, channelsUrl, localfn);
			mthread.start();
		}
	}
	
	/**
	 * Shuts down the maintenance thread and destroys the singleton instance.
	 */
	public void stopMaintenanceThread()
	{
		if (mthread != null)
		{
			mthread.shutdown = true;
			mthread.interrupt();
		}
		mthread = null;
		_instance = null;
	}

	private class ChanMaintenanceThread extends Thread
	{
		String localfn;
		File channelsfile;
		String url;
		long lastLoad = 0L;
		long lastDownload = 0L;
		private ChannelMap cmap;
		boolean shutdown = false;
		Random random = new Random();

		ChanMaintenanceThread(ChannelMap cmap, String url, String fn)
		{
			this.cmap = cmap;
			this.localfn = fn;
			channelsfile = new File(EnvExpander.expand(fn));
			this.url = url;
		}

		public void run()
		{
			Logger.instance().debug1("Starting CDT Maintenance Thread, url='"
				+ url + "', localfile='" + localfn + "'"
				+ ", localpath=" + channelsfile.getPath());

			if (channelsfile.canRead())
				lastDownload = channelsfile.lastModified();

			while(!shutdown)
			{
				if (channelsfile.canRead() && 
					channelsfile.lastModified() > lastLoad)
				{
					lastLoad = System.currentTimeMillis();
					cmap.load(channelsfile);
				}
				if (url != null && url.length() > 0 && !url.equals("-")
				 && System.currentTimeMillis() - lastDownload > Pdt.downloadIntervalMsec)
				{
					lastDownload = System.currentTimeMillis();
					DownloadChannelMapThread downloadThread = 
						new DownloadChannelMapThread(url, localfn, cmap);
					downloadThread.start();
				}
				long interval = Pdt.fileCheckIntervalMsec + (random.nextInt() & 0x1f) * 1000L;
				try { sleep(interval); }
				catch(InterruptedException ex) {}
			}
			Logger.instance().debug1("CDT Maintenance thread stopped.");
		}
	}
	
	/**
	 * Usage ChannelMap url localfile
	 * @param args
	 */
	public static void main(String args[])
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		ChannelMap cm = instance();
		cm.startMaintenanceThread(args[0], args[1]);
		while(!cm._isLoaded)
		{
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
			System.out.println("Awaiting _isLoaded");
		}
		for(int i=0; i<cm.channelBauds.length; i++)
			if (cm.channelBauds[i] != null)
				System.out.println("channel[" + i + "]: " + cm.channelTypes[i]
					+ " " + cm.channelBauds[i]);
		cm.stopMaintenanceThread();
		System.exit(0);
	}
}
