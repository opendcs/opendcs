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
*
*  $Log$
*  Revision 1.5  2009/12/09 18:31:04  mjmaloney
*  remove dbg message
*
*  Revision 1.4  2009/04/30 17:40:46  mjmaloney
*  dev
*
*  Revision 1.3  2009/04/30 15:22:11  mjmaloney
*  Iridium updates
*
*  Revision 1.2  2008/08/06 19:40:58  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2006/09/28 19:15:33  mmaloney
*  Javadoc cleanup
*
*  Revision 1.1  2005/06/24 15:57:28  mjmaloney
*  Java-Only-Archive implementation.
*
*/
package lrgs.common;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashMap;

import ilex.util.Logger;
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
Logger.instance().debug1("NetlistDcpNameMapper.dcpNameToAddress('"
+ name + "' returning '" + addr + "'");
		
		if (addr == null)
		{
			if (parent != null)
				return parent.dcpNameToAddress(name);
		}
		return addr;
	}

//	/**
//	  Return the String name for the passed DCP address.
//	  Return null if there is no mapping for this address.
//	*/
//	public String dcpAddressToName(long address)
//	{
//		return "unknown";
//	}

	/**
	 * Checks to see if any files in the directory have changed since the
	 * last load time. If so, the directory is loaded.
	 */
	public void check()
	{
		File files[] = netlistDir.listFiles();
		if (files == null)	
		{
			Logger.instance().warning("Cannot list netlist directory '"
				+ netlistDir.getPath() + "'!");
			return;
		}
		for(int i=0; i<files.length; i++)
		{
			if (files[i].isDirectory())
				continue;
			if (!files[i].canRead())
			{
				Logger.instance().warning("Cannot read file '"
					+ files[i].getPath() + "'");
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

		Logger.instance().debug3("Loading network lists from '"
			+ netlistDir.getPath() + "'");

		if (!netlistDir.isDirectory())
		{
			Logger.instance().warning("Netlist directory '"
				+ netlistDir.getPath() + "' is not a directory!");
			return;
		}
		File files[] = netlistDir.listFiles();
		if (files == null)	
		{
			Logger.instance().warning("Cannot list netlist directory '"
				+ netlistDir.getPath() + "'!");
			return;
		}

		for(int i=0; i<files.length; i++)
		{
			if (files[i].isDirectory())
				continue;
			if (!files[i].canRead())
			{
				Logger.instance().warning("Cannot read file '"
					+ files[i].getPath() + "'");
				continue;
			}
			if (files[i].getName().toLowerCase().endsWith(".nl"))
			{
				try
				{
					Logger.instance().debug3("Loading '" + 
						files[i].getPath() + "'");
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
					Logger.instance().warning("Cannot parse network list '"
						+ files[i].getPath() + "': " + ex);
				}
			}
		}
	}
}
