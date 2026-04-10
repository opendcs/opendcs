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
package decodes.util.hads;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import lrgs.common.DcpAddress;

/**
 * This class creates HadsEntry records.
 * This class downloads the National Weather Service from
 * its web site and store the data in the hads array list.
 *
 */
public class Hads
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private ArrayList<HadsEntry> hads;
	private int badLines;
	private static Hads _instance = null;

	/** Private Constructor -- use instance() method. */
	private Hads()
	{
		hads = new ArrayList<HadsEntry>();
	}

	/** @return the singleton instance of the Hads. */
	public static Hads instance()
	{
		if (_instance == null)
			_instance = new Hads();
		return _instance;
	}

	/**
	 * Loads a USGS Hads file into memory.
	 * If an IO error occurs, the USGS Hads is restored to what is was before
	 * the call to this method.
	 * @param file the file to load.
	 * @return true if load was successful, false if not.
	 */
	public synchronized boolean load(File file)
	{
		log.info("Loading USGS Hads from '{}'", file.getPath());

		ArrayList<HadsEntry> tmphads = new ArrayList<HadsEntry>();

		badLines = 0;

		try (FileReader reader = new FileReader(file))
		{
			LineNumberReader lnr = new LineNumberReader(reader);
			String line;
			while( (line = lnr.readLine() ) != null)
			{
				try { tmphads.add(new HadsEntry(line)); }
				catch(BadHadsEntryException ex)
				{
					log.atWarn().setCause(ex).log("Bad USGS Hads line {}", lnr.getLineNumber());
					badLines++;
				}
			}
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("IO Error reading Hads File '{}' -- Old Hads restored.", file.getPath());
			return false;
		}
		Collections.sort(tmphads);
		hads = tmphads;
		log.info("Parsed Hads File '{}' - {} entries.", file.getPath(), hads.size());
		return true;
	}

	/**
	 * Retrieve the HadsEntry for the specified DCP address.
	 * @param dcpAddr the DCP Address
	 * @return the HadsEntry or null if there is none.
	*/
	public synchronized HadsEntry find(DcpAddress dcpAddr)
	{
		HadsEntry testhads = new HadsEntry();
		testhads.dcpAddress = dcpAddr;
		int idx = Collections.binarySearch(hads, testhads);
		if (idx >= 0)
			return hads.get(idx);
		else
			return null;
	}

	public synchronized HadsEntry getByName(String name)
	{
		for(HadsEntry he : hads)
			if (he.dcpName.equalsIgnoreCase(name))
				return he;
		return null;
	}

	public int size() { return hads.size(); }

	public ArrayList<HadsEntry> getHads() { return hads; }

	/**
	 * Starts a background thread the periodically downloads the USGS HADS from
	 * a specified URL and independently checks a local file for changes.
	 * The URL is downloaded every 2 hours.
	 * The file is checked for changes every 10 minutes.
	 * @param hadsUrl the URL to download Hads from, null if no download.
	 * @param localfn - the local file to check periodically for changes.
	 */
	public void startMaintenanceThread(String hadsUrl, String localfn)
	{
		MaintenanceThreadH mthread = new MaintenanceThreadH(hadsUrl, localfn);
		mthread.start();
	}

	private class MaintenanceThreadH extends Thread
	{
		String localfn;
		File hadsfile;
		String url;
		long lastLoad = 0L;
		long lastDownload = 0L;

		MaintenanceThreadH(String url, String fn)
		{
			this.localfn = fn;
			hadsfile = new File(EnvExpander.expand(fn));
			this.url = url;
		}

		public void run()
		{
			if (hadsfile.canRead())
				Hads.instance().load(hadsfile);
			while(true)
			{
				if (hadsfile.canRead() && hadsfile.lastModified() > lastLoad)
				{
					lastLoad = System.currentTimeMillis();
					Hads.instance().load(hadsfile);
				}
				if (url != null && url.length() > 0
				 && System.currentTimeMillis() - lastDownload > 2*3600*1000L)
				{
					lastDownload = System.currentTimeMillis();
					LoadHadsThread lht = new LoadHadsThread(url, localfn);
					lht.start();
				}
				try { sleep(600000L); }
				catch(InterruptedException ex) {}
			}
		}
	}

	/**
	 * Test main.
	 * Starts maintenance download thread.
	 * Reads DCP addresses from command line, spits out the Hads.
	 * Log messages go to stderr about loading activities.
	 */
	public static void main(String args[])
		throws Exception
	{
		Hads hads = Hads.instance();
		hads.startMaintenanceThread(
			//"http://www.weather.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt",
			"http://www.weather.gov/ohd/hads/compressed_defs/all_dcp_defs.txt",
			//"C:/DCSTOOLTest1/dcpmon/hads");
			"$HOME/hads");

		java.io.BufferedReader br =
			new java.io.BufferedReader(
				new java.io.InputStreamReader(System.in));
		while(true)
		{
			String s = br.readLine().trim();
			try
			{
				DcpAddress dcpaddr = new DcpAddress(s);
				HadsEntry he = hads.find(dcpaddr);
				if (he == null)
					System.out.println(s + " not found.");
				else
					System.out.println(he.toString());
			}
			catch (NumberFormatException ex)
			{
				System.out.println("Invalid DCP Address: " + ex);
			}
		}
	}
}