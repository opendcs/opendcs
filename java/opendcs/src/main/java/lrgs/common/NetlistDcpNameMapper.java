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
package lrgs.common;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;

/**
The NetlistDcpNameMapper implements DcpNameMapper.
It is used by the DDS server to convert symbolic DCP names into
Hex DCP addresses.
<p>
The mapper works somewhat like the Java Properties class. You can specify
a 'parent' mapper. If the name is not found by this mapper and a parent
has been specified, it delegates the call to the parent.
The DDS server uses this to maintain separate mappers for each user and
a global mapper shared by all.
*/
public class NetlistDcpNameMapper implements DcpNameMapper
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The directory containing the network list files. */
	private File netlistDir;

	/** Time that the files were last read. */
	private long lastLoadTime;

	/** Hashmap mapping lower-case names to DCP addresses. */
	private HashMap<String, DcpAddress> name2address;

	/** Optional Parent */
	private DcpNameMapper parent;

	/**
	 * Constructor.
	 * @param netlistDir the directory containing network lists.
	 * @param parent delegate to parent if mapping not found here (null if no parent)
	 */
	public NetlistDcpNameMapper(File netlistDir, DcpNameMapper parent)
	{
		this.netlistDir = netlistDir;
		this.parent = parent;
		name2address = new HashMap<String, DcpAddress>();
		load();
	}

	/**
	  Return numeric DCP address associated with name.
	  Return null if there is no mapping for this name.
	*/
	public synchronized DcpAddress dcpNameToAddress(String name)
	{
		DcpAddress addr = name2address.get(name.toLowerCase());
		log.debug("NetlistDcpNameMapper.dcpNameToAddress('{}') returning '{}'", name, addr);

		if (addr == null)
		{
			if (parent != null)
				return parent.dcpNameToAddress(name);
		}
		return addr;
	}

	/**
	 * Checks to see if any files in the directory have changed since the
	 * last load time. If so, the directory is loaded.
	 */
	public void check()
	{
		File files[] = netlistDir.listFiles();
		if (files == null)
		{
			log.warn("Cannot list netlist directory '{}'!", netlistDir.getPath());
			return;
		}
		for(int i=0; i<files.length; i++)
		{
			if (files[i].isDirectory())
				continue;
			if (!files[i].canRead())
			{
				log.warn("Cannot read file '{}'", files[i].getPath());
				continue;
			}
			if (files[i].lastModified() > lastLoadTime)
			{
				load();
				return;
			}
		}
	}

	/**
	 * Loads all network lists contained in the directory.
	 */
	public synchronized void load()
	{
		lastLoadTime = System.currentTimeMillis();
		name2address.clear();

		log.trace("Loading network lists from '{}'", netlistDir.getPath());

		if (!netlistDir.isDirectory())
		{
			log.warn("Netlist directory '{}' is not a directory!", netlistDir.getPath());
			return;
		}
		File files[] = netlistDir.listFiles();
		if (files == null)
		{
			log.warn("Cannot list netlist directory '{}'!", netlistDir.getPath());
			return;
		}

		for(int i=0; i<files.length; i++)
		{
			if (files[i].isDirectory())
				continue;
			if (!files[i].canRead())
			{
				log.warn("Cannot read file '{}'", files[i].getPath());
				continue;
			}
			if (files[i].getName().toLowerCase().endsWith(".nl"))
			{
				try
				{
					log.trace("Loading '{}'", files[i].getPath());
					NetworkList netlist = new NetworkList(files[i]);
					for(Iterator it = netlist.iterator(); it.hasNext(); )
					{
						NetworkListItem nli = (NetworkListItem)it.next();
						if (nli.name != null && nli.name.length() > 0)
							name2address.put(nli.name.toLowerCase(), nli.addr);
					}
				}
				catch(IOException ex)
				{
					log.atWarn().setCause(ex).log("Cannot parse network list '{}'", files[i].getPath());
				}
			}
		}
	}
}
