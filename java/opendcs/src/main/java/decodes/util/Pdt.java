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

import java.util.Date;
import java.util.HashMap;
import java.util.Collection;
import java.util.Random;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;

import ilex.util.EnvExpander;
import lrgs.common.DcpAddress;

/**
This class holds the Platform Description Table.
*/
public class Pdt
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Map of DCP Address to PDT Entry */
	private HashMap<DcpAddress, PdtEntry> pdtMap;
	private static Pdt _instance = null;
	public static long downloadIntervalMsec = 24 * 3600 * 1000L; // 24 hours
	public static long fileCheckIntervalMsec = 30 * 60 * 1000L;  // 30 minutes
	public static boolean useLockForDownload = false;
	private boolean _isLoaded = false;
	private PdtMaintenanceThread mthread = null;
	private PdtLoadListener pdtLoadListener = null;

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
		log.debug("Loading PDT from file '{}'", file.getPath());

		HashMap<DcpAddress, PdtEntry> tmpPdtMap = new HashMap<DcpAddress, PdtEntry>();

		int badLines = 0;
		try (FileReader reader = new FileReader(file))
		{
			LineNumberReader lnr = new LineNumberReader(reader);
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
					log.atWarn().setCause(ex).log("Bad PDT line {}", lnr.getLineNumber());
					badLines++;
				}
			}
			lnr.close();
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("IO Error reading PDT File '{}' -- Old PDT restored.", file.getPath());
			return false;
		}
		pdtMap = tmpPdtMap;
		log.debug("Parsed PDT File '{}' - {} entries, {} unparsable lines.",
				  file.getPath(), pdtMap.size(), badLines);
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
		log.trace("Pdt.stopMaintenanceThread()");
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

			log.debug("Starting PDT Maintenance Thread, url='{}', localfile='{}', " +
					  "localpath={}, lastDownload={}, shutdown={}",
					  url, localfn, pdtfile.getPath(), new Date(lastDownload), shutdown);
			
			while(!shutdown)
			{
				if (pdtfile.lastModified() > lastLoad)
				{
					lastLoad = System.currentTimeMillis();
					pdt.load(pdtfile);
				}
				long now = System.currentTimeMillis();
				if (url != null && url.length() > 0 && !url.equals("-")
				 && now - lastDownload > downloadIntervalMsec)
				{
					log.debug("Starting download. lastDownload={}, now={}, intv={}, dT={}",
							  new Date(lastDownload), new Date(now),
							  downloadIntervalMsec, (now - lastDownload));
					lastDownload = System.currentTimeMillis();
					DownloadPdtThread lpt = 
						new DownloadPdtThread(url, localfn, pdt);
					lpt.start();
				}
				else if (pdtLoadListener != null)
				{
					pdtLoadListener.pdtLoaded();
					pdtLoadListener = null;
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
		try (FileWriter mywriter = new FileWriter(myfile))
		{
			
			int nlines = 0;
			for(PdtEntry pdtEntry : pdtMap.values())
			{
				mywriter.write(pdtEntry.printToLine());
				mywriter.write("\n");
				nlines++;
			}
			mywriter.close();
			log.info("Pdt saved to '{}' -- { entries.", myfile.getPath(), nlines);
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Pdt.save failed.");
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

	public void setPdtLoadListener(PdtLoadListener pdtLoadListener)
	{
		this.pdtLoadListener = pdtLoadListener;
	}

	public PdtLoadListener getPdtLoadListener()
	{
		return pdtLoadListener;
	}
}
