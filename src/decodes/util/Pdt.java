/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Collection;
import java.util.Random;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;

import decodes.db.Database;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.ServerLock;
import lrgs.common.DcpAddress;

/**
This class holds the Platform Description Table.
*/
public class Pdt
{
	/** Map of DCP Address to PDT Entry */
	private HashMap<DcpAddress, PdtEntry> pdtMap;
	private static Pdt _instance = null;
	public static long downloadIntervalMsec = 24 * 3600 * 1000L; // 24 hours
	public static long fileCheckIntervalMsec = 30 * 60 * 1000L;  // 30 minutes
	public static boolean useLockForDownload = false;
	private boolean _isLoaded = false;
	private PdtMaintenanceThread mthread = null;

	/** For singleton, use instance() method. */
	public Pdt()
	{
		pdtMap = new HashMap<DcpAddress, PdtEntry>();
	}

	/** @return the singleton instance of the PDT. */
	public static Pdt instance()
	{
		if (_instance == null)
			_instance = new Pdt();
		return _instance;
	}

	/**
	 * Loads a PDT file into memory.
	 * If an IO error occurs, the pdtSched is restored to what is was before
	 * the call to this method.
	 * @param file the file to load.
	 * @return true if load was successful, false if not.
	 */
	public synchronized boolean load(File file)
	{
		Logger.instance().debug1("Loading PDT from file '" + file.getPath() + "'");

		HashMap<DcpAddress, PdtEntry> tmpPdtMap = new HashMap<DcpAddress, PdtEntry>();

		int badLines = 0;
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String line;
			while( (line = lnr.readLine() ) != null)
			{
				if (line.length() > 0 && line.charAt(0) == '\f')
					line = line.substring(1);
				try
				{
					PdtEntry pdtEntry = new PdtEntry(line);
					tmpPdtMap.put(pdtEntry.dcpAddress, pdtEntry);
				}
				catch(BadPdtEntryException ex)
				{
					Logger.instance().warning(
						"Bad PDT line " + lnr.getLineNumber() + ": " + ex);
					badLines++;
				}
			}
			lnr.close();
		}
		catch(IOException ex)
		{
			Logger.instance().warning("IO Error reading PDT File '" 
				+ file.getPath() + "': " + ex + " -- Old PDT restored.");
			return false;
		}
		pdtMap = tmpPdtMap;
		Logger.instance().debug1(
			"Parsed PDT File '" + file.getPath() + "' - " + pdtMap.size()
			+ " entries, " + badLines + " unparsable lines.");
		_isLoaded = true;
		return true;
	}
	
	public boolean isLoaded() { return _isLoaded; }

	
	/**
	 * Adds a new entry to the hashmap.
	 * @param entry
	 */
	public void put(PdtEntry entry)
	{
		pdtMap.put(entry.dcpAddress, entry);
	}

	/**
	 * Retrieve the PdtSchedEntry for the specified DCP address.
	 * @param dcpAddr the DCP Address
	 * @return the PdtSchedEntry or null if there is none.
	*/
	public synchronized PdtEntry find(DcpAddress dcpAddr)
	{
		return pdtMap.get(dcpAddr);
	}

	public int size() { return pdtMap.size(); }
	
	/**
	 * Starts a background thread the periodically downloads the PDT from
	 * a specified URL and independently checks a local file for changes.
	 * The URL is downloaded every 2 hours.
	 * The file is checked for changes every 10 minutes.
	 * @param pdtUrl the URL to download PDT from, null if no download.
	 * @param localfn - the local file to check periodically for changes.
	 */
	public void startMaintenanceThread(String pdtUrl, String localfn)
	{
		if (mthread == null)
		{
			mthread = new PdtMaintenanceThread(this, pdtUrl, localfn);
			mthread.start();
		}
	}
	
	/**
	 * Stops the maintenance thread and destroys the singleton instance.
	 */
	public void stopMaintenanceThread()
	{
		Logger.instance().debug3("Pdt.stopMaintenanceThread()");
		if (mthread != null)
		{
			mthread.shutdown = true;
			mthread.interrupt();
		}
		mthread = null;
		_instance = null;
	}

	private class PdtMaintenanceThread extends Thread
	{
		String localfn;
		File pdtfile;
		String url;
		long lastLoad = 0L;
		long lastDownload = 0L;
		Pdt pdt = null;
		boolean shutdown = false;
		Random random = new Random();

		PdtMaintenanceThread(Pdt pdt, String url, String fn)
		{
			super("PdtMaintenanceThread");
			this.pdt = pdt;
			this.localfn = fn;
			pdtfile = new File(EnvExpander.expand(fn));
			this.url = url;
		}

		public void run()
		{
			if (pdtfile.canRead())
				lastDownload = pdtfile.lastModified();

			Logger.instance().debug1("Starting PDT Maintenance Thread, url='"
				+ url + "', localfile='" + localfn + "'"
				+ ", localpath=" + pdtfile.getPath() + ", lastDownload=" 
				+ new Date(lastDownload) + ", shutdown="+shutdown);
			
			while(!shutdown)
			{
				if (pdtfile.lastModified() > lastLoad)
				{
					lastLoad = System.currentTimeMillis();
					pdt.load(pdtfile);
				}
				if (url != null && url.length() > 0 && !url.equals("-")
				 && System.currentTimeMillis() - lastDownload > downloadIntervalMsec)
				{
					lastDownload = System.currentTimeMillis();
					DownloadPdtThread lpt = 
						new DownloadPdtThread(url, localfn, pdt);
					lpt.start();
				}
				
				// 30 min + random # seconds between 0...31
				long interval = fileCheckIntervalMsec + (random.nextInt() & 0x1f) * 1000L;
				try { sleep(interval); }
				catch(InterruptedException ex) {}
			}
		}
	}
	
	public void printMe()
	{
		System.out.println("Total Lines: "+ pdtMap.size());
		for(PdtEntry pdtEntry : pdtMap.values())
			System.out.println(pdtEntry.printToLine());
	}
	
	public synchronized void save(File myfile)
	{
		try
		{
			FileWriter mywriter = new FileWriter(myfile);
			int nlines = 0;
			for(PdtEntry pdtEntry : pdtMap.values())
			{
				mywriter.write(pdtEntry.printToLine());
				mywriter.write("\n");
				nlines++;
			}
			mywriter.close();
			Logger.instance().info(
			    "Pdt saved to '" + myfile.getPath() + "' -- " + nlines
			        + " entries.");
		}
		catch (Exception ex)
		{
			String msg = "Pdt.save failed: " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Returns the collection of PDT Entries, in no particular order.
	 * The collection here is stored inside a HashMap, so do not modify the 
	 * returned collection.
	 * @return the collection of PDT Entries, in no particular order.
	 */
	public Collection<PdtEntry> getEntries()
	{
		return pdtMap.values();
	}

	/**
	 * args: url localfile
	 */
	public static void main(String args[])
		throws Exception
	{
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		Pdt pdt = instance();
		pdt.startMaintenanceThread(args[0], args[1]);
		while(!pdt._isLoaded)
		{
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
			System.out.println("Awaiting _isLoaded");
		}
		
		System.out.println("Press enter to exit ...");
		System.console().readLine();
		pdt.stopMaintenanceThread();
		
		System.exit(0);
	}
}
