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
package decodes.syncgui;

import java.util.Iterator;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.io.*;
import java.net.URL;

public class District
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The district name */
	private String name;

	/** A description */
	private String desc;

	/** The host name or IP Address for this district's DECODES database */
	private String host;

	/** The directory for the database on the host */
	private String dir;

	/** The user name managing the database (used for SSH transfers) */
	private String user;

	/** Vector of DistrictDBSnap objects found under this district */
	private Vector snaps;

	/**
	 * Construct District.
	 * @param name district name.
	 */
	public District(String name, String host, String user, String dir,
		String desc)
	{
		this.name = name;
		this.host = host;
		this.dir = dir;
		this.user = user;
		this.desc = desc;
		this.snaps = new Vector();
	}

	/**
	 * This method is used to name the node of the tree.
	 * @return district name
	 */
	public String toString( )
	{
		return desc == null ? name : name + ": " + desc;
	}

	/** @return the short name */
	public String getName( ) { return name; }

	/** @return the host name (or IP address) */
	public String getHost( ) { return host; }

	/** @return the user name */
	public String getUser( ) { return user; }

	/** @return the directory */
	public String getDirectory( ) { return dir; }

	/** @return the description */
	public String getDesc( ) { return desc == null ? "" : desc; }


	/**
	 * Reads the list of snapshots from the passed input stream.
	 * The stream is a file containing a list of directory names.
	 * Each name should be in the format of a date: YYYY-MM-DD.HHMM
	 * @param distDir the partial URL pointing to the district directory.
	 */
	public void readSnapList( String distDir)
		throws IOException
	{
		snaps.clear();
		URL url = new URL(distDir + "/snaplist.txt");
		InputStream strm = url.openStream();
		LineNumberReader lnr = new LineNumberReader(
			new InputStreamReader(strm));
		String line;
		while((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.equals("current")
			 || line.startsWith("backup"))
			{
				String dateStr = readDateFile(distDir, line);
				snaps.add(new DistrictDBSnap(this, line, dateStr));
			}
		}
		strm.close();
		Collections.sort(snaps,
			new Comparator()
			{
				public int compare(Object o1, Object o2)
				{
					DistrictDBSnap snap1 = (DistrictDBSnap)o1;
					DistrictDBSnap snap2 = (DistrictDBSnap)o2;
					String s1 = snap1.getDirName();
					String s2 = snap2.getDirName();
					int x = (int)s1.charAt(0) - (int)s2.charAt(0);
					if (x == 0)
						x = -s1.compareTo(s2);
					return x;
				}
				public boolean equals(Object o)
				{
					return false;
				}
			});
	}

	private String readDateFile(String distDir, String subdir)
	{
		String urlstr = distDir + "/" + subdir + "/date.txt";
		String ret = "";
		try
		{
			URL url = new URL(urlstr);
			InputStream strm = url.openStream();
			LineNumberReader lnr = new LineNumberReader(
				new InputStreamReader(strm));
			ret = lnr.readLine();
			if (ret != null)
				ret = ret.trim();
			strm.close();
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot read date file from '{}'", urlstr);
			ret = "";
		}
		return ret;
	}

	/** Return true if this district was already loaded. */
	public boolean isLoaded()
	{
		return snaps.size() > 0;
	}

	/** @return an iterator into the snapshots. */
	public Iterator iterator()
	{
		return snaps.iterator();
	}

	/** Dumps list to stdout for testing. */
	public void dump()
	{
		System.out.println("Snap shots for district " + name);
		for(Iterator it = snaps.iterator(); it.hasNext(); )
		{
			System.out.println("\t" + it.next());
		}
	}
}
