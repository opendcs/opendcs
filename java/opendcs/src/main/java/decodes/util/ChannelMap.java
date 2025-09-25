/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.util;

import java.io.*;
import java.net.*;
import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;

/**
Contains an array of channels, baud rates, and types.
*/
public class ChannelMap
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.trace("Loading channel map from URL '{}'", urlstr);

		try
		{
			URL channelMapURL = new URL(urlstr);
			try (InputStream istrm = channelMapURL.openStream())
			{
				load(new LineNumberReader(new InputStreamReader(istrm)));
			}
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot read channel map '{}'", urlstr);
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
		log.info("Loading Channel Map from '{}'", file.getPath());
		try (FileReader reader = new FileReader(file))
        {
	        return load(new LineNumberReader(reader));
        }
        catch (IOException ex)
        {
	        log.atWarn().setCause(ex).log("Cannot load channel map from '{}'", file.getPath());
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
					log.warn("Invalid channel number {} on line {} -- skipped.", chan, bfr.getLineNumber());
					continue;
				}

				line = line.substring(idx).trim();
				if (line.length() == 0)
				{
					log.warn("Empty channel number {} on line {} -- skipped.", chan, bfr.getLineNumber());
					continue;
				}

				channelTypes[chan] = line.charAt(0);

				line = line.substring(1).trim();

				if (line.length() == 0)
				{
					log.warn(" No baud for chan {} on line {} -- skipped.", chan, bfr.getLineNumber());
					continue;
				}
				if (line.length() > 1 && line.charAt(0) == ',')
					line = line.substring(2).trim();

				channelBauds[chan] = line;
			}
			bfr.close();
			log.info("Channel map successfully loaded.");
			_isLoaded = true;
			return true;
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot load channel map.");
			return false;
		}
	}

	/**
	  Return baud rate for a particular channel.
	  @param chan the channel number (1...max)
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
		return (chan < 0 || chan > channelTypes.length) ? true : channelTypes[chan] == 'R';
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

	private static class ChanMaintenanceThread extends Thread
	{
		private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.debug("Starting CDT Maintenance Thread, url='{}', localfile='{}', localpath={}",
					  url, localfn, channelsfile.getPath());

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
			log.debug("CDT Maintenance thread stopped.");
		}
	}

	/**
	 * Usage ChannelMap url localfile
	 * @param args
	 */
	public static void main(String args[])
	{
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
