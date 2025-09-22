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
import java.util.StringTokenizer;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.net.URL;

/**
The top level configuration for the Sync GUI.
*/
public class SyncConfig
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Private instance */
	private static SyncConfig _instance = null;

	/** The URL pointing to the hub root directory. */
	private String hubHome;

	/** The symbolic name for this hub */
	private String hubName = "Unknown";

	/** Vector of districts. */
	public Vector districts;

	/** @return singleton instance. */
	public static SyncConfig instance()
	{
		if (_instance == null)
			_instance = new SyncConfig();
		return _instance;
	}

	/** Private constructor -- use singleton instance() method. */
	private SyncConfig()
	{
		districts = new Vector();
	}

	/**
	 * Sets the URL that points to the HUB_HOME directory on the hub machine.
	 * @param hh the hub home URL string
	 */
	public void setHubHome(String hh)
	{
		hubHome= hh;
	}

	/** @return the URL pointing to the Hub Home directory. */
	public String getHubHome() { return hubHome; }

	/**
	 * Sets the Hub Name
	*/
	public void setHubName(String nm)
	{
		hubName = nm;
	}

	/** @return the hub name */
	public String getHubName() { return hubName; }

	/**
	 * Reads the Sync Config file from an input stream.
	 * Note: Stream is NOT closed on exit.
	 * @param strm the input stream to read.
	 */
	public void readConfig( InputStream strm )
		throws IOException
	{
		LineNumberReader lnr = new LineNumberReader(
			new InputStreamReader(strm));

		String line;
		districts.clear();
		while((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) == '#')
				continue;

			if (line.length() > 9
			 && line.substring(0,9).equalsIgnoreCase("HUB_NAME="))
			{
				hubName = line.substring(9);
				continue;
			}

			StringTokenizer stok = new StringTokenizer(line);
			if (stok.countTokens() < 4)
			{
				log.error("Sync Configuration line {} bad format. Should be " +
						  "name host user dir optional-description. -- skipped.",
						  lnr.getLineNumber());
				continue;
			}
			String nm = stok.nextToken();
			String host = stok.nextToken();
			String user = stok.nextToken();
			String dir = stok.nextToken();
			String desc = "";
			int qpos = line.indexOf('"');
			int eqpos = line.lastIndexOf('"');
			if (qpos != -1 && eqpos != -1 && qpos != eqpos)
				desc = line.substring(qpos+1, eqpos);
			districts.add(new District(nm, host, user, dir, desc));
		}
	}

	/** @return an iterator into a collection of District objects. */
	public Iterator iterator()
	{
		return districts.iterator();
	}

	/** Dumps configuration to stdout for testing. */
	public void dump()
	{
		for(Iterator it = districts.iterator(); it.hasNext(); )
		{
			District dist = (District)it.next();
			System.out.println(dist.getName() + " " + dist.getHost()
				+ " " + dist.getUser() + " " + dist.getDirectory()
				+ " \"" + dist.getDesc() + "\"");
		}
	}

	/**
	  Test main reads config file and dumps it.
	  The one and only argument is an URL pointing to the hub directory,
	  which should contain the file "hub.conf".
	  Any exceptions are thrown to shell.
	*/
	public static void main(String args[])
		throws Exception
	{
		SyncConfig.instance().setHubHome(args[0]);
		URL url = new URL(SyncConfig.instance().getHubHome() + "/hub.conf");
		try(InputStream fis = url.openStream())
		{
			SyncConfig.instance().readConfig(fis);
		}
		SyncConfig.instance().dump();
		SyncConfig sc = SyncConfig.instance();
		for(Iterator it = sc.iterator(); it.hasNext(); )
		{
			District d = (District)it.next();
			String distdir = sc.getHubHome() + "/" + d.getName();
			d.readSnapList(distdir);

			d.dump();
			for(Iterator snapit = d.iterator(); snapit.hasNext(); )
			{
				DistrictDBSnap snap = (DistrictDBSnap)snapit.next();
				url = new URL(distdir + "/" + snap.toString() + "/dblist.txt");
				try(InputStream fis = url.openStream())
				{
					snap.readFileList(fis);
				}
				url = new URL(distdir + "/" + snap.toString()
					+ "/platform/PlatformList.xml");
				try(InputStream fis = url.openStream())
				{
					snap.readPlatList(fis);
				}
				snap.dump();
			}
		}
	}
}
