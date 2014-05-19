/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.6  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.dcpmon1;

import java.io.*;
import java.util.*;

import ilex.util.Logger;
import decodes.sql.DbKey;
import decodes.util.DecodesException;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import lrgs.common.DcpAddress;
import lrgs.common.NetworkListItem;

/**
Holds a group of DCPs. The user associates DCPs to a group via network
lists.
*/
public class DcpGroup
	implements Comparable<DcpGroup>
{
	private String module = "DcpGroup";
	
	/** The network list id from the SQL Database */
	private DbKey groupId;
	
	/** The group name. */
	String groupName;

	/** The group type (one of the defined GROUP_XXX constants */
	public int groupType;

	public static final int GROUP_NONE = 0;
	public static final int GROUP_LRGS_NETLIST = 1;
	public static final int GROUP_DECODES_NETLIST = 2;

	/** If this is an LRGS-style list, this is the File. */
	public File lrgsNetlistFile;

	/** Last time this list was loaded from the file or DECODES database */
	private long lastLoadTime;

	/** Maps addresses to names */
	HashMap<DcpAddress, String> addr2name;

	/** Maps names to addresses */
	HashMap<String, DcpAddress> name2addr;

	/** Maps addresses to description */
	HashMap<DcpAddress, String> addr2description;
	
	/** Internal flag to facilitate loading */
	boolean checked;

	/** Groups will be sorted by this key, which may be different from name. */
	private String sortKey;

	/** Constructor */
	public DcpGroup()
	{
		groupId = Constants.undefinedId;
		groupName = null;
		groupType = GROUP_NONE;
		lrgsNetlistFile = null;
		addr2name = new HashMap<DcpAddress, String>();
		name2addr = new HashMap<String, DcpAddress>();
		addr2description = new HashMap<DcpAddress, String>();
		sortKey = "";
	}

	/**
	  Loads this group from the passed LRGS-style Network List File.
	  @param file the File
	  @param groupName the group name
	  @param sortKey the sort-key for this group.
	*/
	public synchronized void loadFromLrgsNetworkList(
		File file, String groupName, String sortKey)
		throws IOException
	{
		lastLoadTime = System.currentTimeMillis();
		this.sortKey = sortKey;

		// The 'name' of this basin will be the netlist file name
		// minus the .nl extension.
		lrgsNetlistFile = file;
		this.groupName = groupName;
		groupType = GROUP_LRGS_NETLIST;
		addr2name.clear();
		name2addr.clear();
		addr2description.clear();
		
		lrgs.common.NetworkList lnl = 
			new lrgs.common.NetworkList(lrgsNetlistFile);

		//--------------------------------------
		//Create a network list in the Decodes SQL Database
		//so that we can use it to find dcp address when searching
		//for transmission records from the client (CGI or JSP)
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		String suffix = "";
		if (cfg != null)
			suffix = cfg.nlNamePrefix;
		String sqlNetwName = suffix + groupName;
		NetworkList netwList = new NetworkList(sqlNetwName);
		netwList.transportMediumType = Constants.medium_Goes;
		//-----------------------------------
		// construct a hashmaps for addresses to names.
		for(Iterator<NetworkListItem> it = lnl.iterator(); it.hasNext(); )
		{
			NetworkListItem nli = it.next();
			String name = nli.name;
			if (name == null || name.trim().length() == 0)
				name = nli.addr.toString();
			name = name.toUpperCase();
			addr2name.put(nli.addr, name);
			name2addr.put(name, nli.addr);
			String description = "";
			if (nli.description != null)
				description = nli.description;
			addr2description.put(nli.addr, description);
			//-----------------------------
			NetworkListEntry nle = 
				new NetworkListEntry(netwList,
						nli.addr.toString());
			nle.description = nli.description;
			nle.platformName = name;
			netwList.addEntry(nle);
			//-----------------------------
		}
		//--------------------------------
		try
		{
			netwList.write();
			//Get the network list id and set the groupId
			groupId = netwList.getId();
//System.out.println("group = " + groupName + " id = " + groupId);
		} catch (DatabaseException ex)
		{
			String msg =  module + 
			":loadFromLrgsNetworkList Can not create Decodes " +
			"Network List for " + groupName + " error = " + ex.getMessage();
			Logger.instance().log(Logger.E_WARNING, msg);
			throw new IOException(msg);
		}
		//------------------------------------
	}

	/**
	  Loads this group from a DECODES Network List.
	  @param name the network list name, also used as group name
	  @param sortKey the sort-key for this group.
	*/
	public synchronized void loadFromDecodesNetworkList(
		String name, String sortKey)
		throws DecodesException
	{
		this.sortKey = sortKey;
		lastLoadTime = System.currentTimeMillis();

		groupName = name;
		groupType = GROUP_DECODES_NETLIST;
		addr2name.clear();
		name2addr.clear();
		addr2description.clear();
		
		decodes.db.NetworkList dnl = 
			Database.getDb().networkListList.find(name);
		if (dnl == null)
		{
			throw new DecodesException(
				"No such DECODES-style network list '" + name
				+ "' -- ignored.");
		}

		// construct a Long->String hashmap for addresses to names.
		
		//Set the groupId 
		groupId = dnl.getId();
//System.out.println("group = " + name + " id = " + groupId);
		for(Iterator<NetworkListEntry> it = dnl.iterator(); it.hasNext(); )
		{
			NetworkListEntry nle = it.next();
			try
			{
				DcpAddress addr = new DcpAddress(nle.transportId);
				if (nle.platformName == null)
					nle.platformName = nle.transportId;
				addr2name.put(addr, nle.platformName.toUpperCase());
				name2addr.put(nle.platformName.toUpperCase(), addr);
				String description = ""; 
				if (nle.description != null)
					description = nle.description;
				addr2description.put(addr, description);
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"List contains illegal DCP address '" 
					+ nle.transportId + "' -- skipped.");
			}
		}
	}

	/** @return the group id **/
	public DbKey getGroupId()
	{
		return groupId;
	}
	
	/** @return the group name */
	public String getGroupName()
	{
		return groupName;
	}

	/**
	  Sets the group name.
	  @param nm the name
	*/
	public void setGroupName(String nm)
	{
		groupName = nm;
	}

	/**
	  Return name for this addr, or null if none found.
	  @param addr the DCP address
	  @return name for this addr, or null if none found.
	*/
	public synchronized String getDcpName(DcpAddress addr)
	{
		return addr2name.get(addr);
	}

	/**
	 * Return description for this addr, or null if none found.
	 * @param addr addr the DCP address as a long integer
	 * @return description for this addr, or null if none found.
	 */
	public synchronized String getDcpDescription(DcpAddress addr)
	{
		return addr2description.get(addr);
	}

	/**
	  Returns the DCP address for this name or null if not in list.
	  @param name the DCP name
	  @return the DCP address for this name or null if not in list.
	*/
	public synchronized DcpAddress getDcpAddress(String name)
	{
		return name2addr.get(name.toUpperCase());
	}

	/**
	  Checks to see if the source for this list has changed, and if so,
	  reloads it.
	  @return true if change has been detected.
	*/
	public boolean checkForChange()
	{
		checked = true;

		if (groupType == GROUP_LRGS_NETLIST)
		{
			if (lrgsNetlistFile.lastModified() > lastLoadTime)
			{
				try
				{
					Logger.instance().info("Reloading LRGS netlist group '" 
						+ groupName + "'");
					loadFromLrgsNetworkList(lrgsNetlistFile, groupName,sortKey);
				}
				catch(IOException ex)
				{
					Logger.instance().failure(
						"Cannot load network list from file '" +
						lrgsNetlistFile.getPath() + "': " + ex);
				}
				return true;
			}
			else
				return false;
		}
		else if (groupType == GROUP_DECODES_NETLIST)
		{
			decodes.db.NetworkList dnl = 
				Database.getDb().networkListList.find(groupName);
			if (dnl == null)
			{
				Logger.instance().failure(
					"No such DECODES-style network list '" + groupName
					+ "' -- ignored.");
				return false;
			}

			try
			{
				Date dblmt = 
				  decodes.db.Database.getDb().getDbIo().getNetworkListLMT(dnl);
//Logger.instance().info("LMT time on netlist " + dnl.name + ": " + dblmt
//+ "(" + dblmt.getTime() + ") last load=" + lastLoadTime);
				if (dblmt.getTime() > lastLoadTime)
				{
					Logger.instance().info("Reloading DECODES netlist group '"
						+ groupName + "'");
					dnl.read();
					loadFromDecodesNetworkList(groupName, sortKey);
					return true;
				}
				else
					return false;
			}
			catch(DecodesException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Error loading DECODES-style network list '" 
					+ groupName + "' -- ignored.");
			}
		}
		return false;
	}

	/**
	  Returns an iterator for cycling through the list of DCP addresses
	  in this group. Each entry in the iteration is a java.lang.Long object.
	  @return an iterator for cycling through the list of DCP addresses
	*/
	public Iterator<DcpAddress> getDcpAddresses()
	{
		return addr2name.keySet().iterator();
	}

	/**
	  From Comparable interface, for sorting the groups.
	*/
	public int compareTo(DcpGroup rhs)
	{
		return sortKey.compareTo(rhs.sortKey);
	}
}
