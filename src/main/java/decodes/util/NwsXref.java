/**
 * $Id$
 * 
 * Open source software.
 * Author: Mike Maloney, Cove Software, LLC
 * 
 * $Log$
 * Revision 1.2  2015/03/19 17:56:53  mmaloney
 * If lock is taken, continue to spawn maintenance thread to check for file changes.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2013/03/28 19:19:32  mmaloney
 * User temp files are now placed under DCSTOOL_USERDIR which may be different
 * from DCSTOOL_HOME on linux/unix multi-user installations.
 *
 * Revision 1.1  2013/02/28 16:39:58  mmaloney
 * Created.
 *
 */
package decodes.util;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.ServerLock;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import lrgs.common.DcpAddress;

/**
 * This class holds the from the National Weather Service
 * Cross Reference File, downloadable from:
 * http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt
 */
public class NwsXref
{
	private HashMap<DcpAddress, NwsXrefEntry> dcpAddrMap
		= new HashMap<DcpAddress, NwsXrefEntry>();
	private HashMap<String, NwsXrefEntry> nwsNameMap
		= new HashMap<String, NwsXrefEntry>();
	private HashMap<String, NwsXrefEntry> usgsNumMap
		= new HashMap<String, NwsXrefEntry>();
	private static NwsXref _instance = null;
	private boolean _isLoaded = false;
	private int badLines = 0;
	private MaintenanceThread mthread = null;

	
	/** For singleton, use instance() method. */
	private NwsXref()
	{
	}

	/** @return the singleton instance of the PDT. */
	public static NwsXref instance()
	{
		if (_instance == null)
			_instance = new NwsXref();
		return _instance;
	}

	/**
	 * Loads a NwsXref file into memory.
	 * If unsuccessful, the internal collections are unchanged.
	 * @param file the file to load.
	 * @return true if load was successful, false if not.
	 */
	public synchronized boolean load(File file)
	{
		Logger.instance().info("Loading NwsXref from '" + file.getPath() + "'");

		ArrayList<NwsXrefEntry> entries = new ArrayList<NwsXrefEntry>();

		// the first few lines have column labels and terminates with a
		// line of hyphens.
		boolean afterHeader = false;
		badLines = 0;
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String line;
			while( (line = lnr.readLine() ) != null)
			{
				line = line.trim();
				if (!afterHeader)
				{
					if (line.startsWith("-"))
						afterHeader = true;
					continue;
				}
				
				NwsXrefEntry entry = NwsXrefEntry.fromFileLine(line);
				if (entry != null)
				{
					entries.add(entry);
				}
				else
					badLines++;
			}
			lnr.close();
		}
		catch(IOException ex)
		{
			Logger.instance().warning("IO Error reading NwsXref File '" 
				+ file.getPath() + "': " + ex + " -- Old NwsXref restored.");
			return false;
		}
		dcpAddrMap.clear();
		nwsNameMap.clear();
		usgsNumMap.clear();
		for(NwsXrefEntry entry : entries)
		{
			dcpAddrMap.put(new DcpAddress(entry.getGoesDcpAddr()), entry);
			nwsNameMap.put(entry.getNwsId(), entry);
			usgsNumMap.put(entry.getUsgsNum(), entry);
		}
		Logger.instance().info(
			"Parsed NwsXref File '" + file.getPath() + "' - " + entries.size()
			+ " entries.");
		_isLoaded = true;
		return true;
	}

	public boolean isLoaded() { return _isLoaded; }

	public synchronized NwsXrefEntry getByAddr(DcpAddress dcpAddr)
	{
		return dcpAddrMap.get(dcpAddr);
	}
	public synchronized NwsXrefEntry getByNwsName(String nwsName)
	{
		return nwsNameMap.get(nwsName);
	}
	public synchronized NwsXrefEntry getByUsgsNum(String usgsNum)
	{
		return usgsNumMap.get(usgsNum);
	}

	/**
	 * Starts a background thread the periodically downloads from
	 * a specified URL and independently checks a local file for changes.
	 * The file is checked for changes every 10 minutes.
	 * @param url the URL to download PDT from, null if no download.
	 * @param localfn - the local file to check periodically for changes.
	 */
	public void startMaintenanceThread(String url, String localfn)
	{
		if (mthread == null)
		{
			mthread = new MaintenanceThread(this, url, localfn);
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

	private class MaintenanceThread extends Thread
	{
		String localfn;
		File localfile;
		String url;
		long lastLoad = 0L;
		long lastDownload = 0L;
		NwsXref xref = null;
		boolean shutdown = false;
		Random random = new Random();

		MaintenanceThread(NwsXref xref, String url, String fn)
		{
			super("NwsXref.MaintenanceThread");
			this.xref = xref;
			this.localfn = fn;
			localfile = new File(EnvExpander.expand(fn));
			this.url = url;
		}

		public void run()
		{
			Logger.instance().debug1("Starting NwsXref Maintenance Thread, url='"
				+ url + "', localfile='" + localfn + "'"
				+ ", localpath=" + localfile.getPath());
			
			if (localfile.canRead())
				lastDownload = localfile.lastModified();

			while(!shutdown)
			{
				if (localfile.lastModified() > lastLoad)
				{
					lastLoad = System.currentTimeMillis();
					xref.load(localfile);
				}
				if (url != null && url.length() > 0 && !url.equals("-")
				 && System.currentTimeMillis() - lastDownload > Pdt.downloadIntervalMsec)
				{
					lastDownload = System.currentTimeMillis();
					DownloadNwsXrefThread lpt = 
						new DownloadNwsXrefThread(url, localfn, xref);
					lpt.start();
				}
				long interval = Pdt.fileCheckIntervalMsec + (random.nextInt() & 0x1f) * 1000L;
				try { sleep(interval); }
				catch(InterruptedException ex) {}
			}
		}
	}
	
	public void printMe()
	{
		System.out.println("Total Lines: "+ dcpAddrMap.size());
		ArrayList<NwsXrefEntry> values = new ArrayList<NwsXrefEntry>();
		for(NwsXrefEntry entry : nwsNameMap.values())
			values.add(entry);
		Collections.sort(values,
			new Comparator<NwsXrefEntry>()
			{
				@Override
				public int compare(NwsXrefEntry arg0, NwsXrefEntry arg1)
				{
					return arg0.getNwsId().compareTo(arg1.getNwsId());
				}
			});
		for(NwsXrefEntry entry : values)
			System.out.println(entry.toString());
	}
	
	public int size() { return dcpAddrMap.size(); }
	
	/**
	 * args: url localfile
	 */
	public static void main(String args[])
		throws Exception
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);

		NwsXref xref = instance();
		xref.startMaintenanceThread(args[0], args[1]);
		while(!xref._isLoaded)
		{
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
			System.out.println("Awaiting _isLoaded");
		}

			
			//"http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt", 
		System.out.println("Press enter to exit ...");
		System.console().readLine();
		xref.stopMaintenanceThread();
		
		System.exit(0);
	}
}
