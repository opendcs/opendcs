/*
*  $Id$
*/
package decodes.dcpmon_old;

import java.util.*;
import java.io.File;
import java.io.IOException;

import ilex.util.*;
import decodes.db.Database;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import lrgs.common.DcpAddress;

/**
The DCP Monitor uses both DECODES and LRGS Network Lists to map
DCPs to names and groups. 
<p>
A 'group' is one of these types of network
lists. If a DCP is in the list it is considered to be in the group.
<p>
@deprecated
*/
public class DcpGroupList
{
	/** Collection of groups */
	private ArrayList<DcpGroup> groups;

	/** Private one-and-only instance variable. */
	private static DcpGroupList _instance;

	/** Constructor */
	private DcpGroupList()
	{
		groups = new ArrayList<DcpGroup>();
	}

	/** 
	  Public instance method. 
	  @return reference to the singleton instance
	*/
	public static DcpGroupList instance()
	{
		if (_instance == null)
			_instance = new DcpGroupList();
		return _instance;
	}

	/**
	  Called from the configuration reader, this method adds a LRGS-style
	  network list.
	  @param groupName the name for this group (may be different from file name)
	  @param f the File
	  @param sortKey the sort key
	*/
	public synchronized void addLrgsNetworkList(String groupName, File f,
		String sortKey)
	{
		Logger.instance().debug1("Adding LRGS NL group '" + groupName + "'");
		boolean newGroup = false;
		DcpGroup theGroup = getGroup(groupName);
		if (theGroup == null)
		{
			newGroup = true;
			theGroup = new DcpGroup();
		}

		try { theGroup.loadFromLrgsNetworkList(f, groupName, sortKey); }
		catch(IOException ex)
		{
			Logger.instance().failure(
				"Cannot load LRGS-style network list '" + groupName 
				+ "': " + ex);
			return;
		}

		theGroup.checked = true;
		if (newGroup)
			groups.add(theGroup);
	}

	/**
	  Called from the configuration reader, this method adds a DECODES-style
	  network list.
	  @param groupName the DECODES network list name, also the group name
	  @param sortKey the sort key
	*/
	public synchronized void addDecodesNetworkList(String groupName, 
		String sortKey)
	{
		Logger.instance().debug1("Adding DECODES group '" + groupName + "'");
		boolean newGroup = false;
		DcpGroup theGroup = getGroup(groupName);
		if (theGroup == null)
		{
			newGroup = true;
			theGroup = new DcpGroup();
		}

		try { theGroup.loadFromDecodesNetworkList(groupName, sortKey); }
		catch(decodes.util.DecodesException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"No such DECODES-style network list '" + groupName
				+ "': " + ex);
			return;
		}

		theGroup.checked = true;
		if (newGroup)
			groups.add(theGroup);
	}
	
	/**
	  Returns a DcpGroup matching a given name.
	  @param name the name to search for
	  @return the DcpGroup or null if not found.
	*/
	public synchronized DcpGroup getGroup(String name)
	{
		for(DcpGroup grp : groups)
			if (grp.getGroupName().equalsIgnoreCase(name))
				return grp;
		return null;
	}

	/**
	  Checks all groups to see if they have been modified, and reloads
	  them if necessary. This method is called periodically (10 minutes)
	  from the main thread.
	  @return true if any groups have changed.
	*/
	public boolean checkGroups()
	{
		Logger.instance().info("Checking groups...");
		boolean anyChanged = false;
		for(DcpGroup grp : groups)
			if (grp.checkForChange())
				anyChanged = true;
		return anyChanged;
	}

	/**
	  Returns the dcp address for the passed name, from the first group
	  found that contains the DCP.
	  @param name the name to search for
	  @return the DCP address as a long integer or 0L if not found.
	*/
	public synchronized DcpAddress getDcpAddress(String name)
	{
		for(DcpGroup grp : groups)
		{
			DcpAddress ret = grp.getDcpAddress(name);
			if (ret != null)
				return ret;
		}
		return null;
	}

	/**
	 * @return true if this address is in at least one group.
	 */
	public synchronized boolean isInGroup(DcpAddress addr)
	{
		for(DcpGroup grp : groups)
			if (grp.getDcpName(addr) != null)
				return true;
		return false;
	}

	public synchronized ArrayList<String> getGroupNameList()
	{
		ArrayList<String> ret = new ArrayList<String>();
		for(DcpGroup grp : groups)
			ret.add(grp.getGroupName());
		return ret;
	}
	
	/** Facilitate loading. */
	public synchronized void uncheckAll()
	{
		for(Iterator<DcpGroup> it = groups.iterator(); it.hasNext(); )
		{
			DcpGroup grp = it.next();
			grp.checked = false;
		}
	}

	/**
	  After re-loading named lists in the configuration file, this method 
	  removes any that are no longer in the list.
	  @return true if any groups were removed.
	*/
	public synchronized boolean removeUnchecked()
	{
		boolean anyChanges = false;
		for(Iterator<DcpGroup> it = groups.iterator(); it.hasNext(); )
		{
			DcpGroup grp = it.next();
			if (!grp.checked)
			{
				it.remove();
				anyChanges = true;
Logger.instance().log(Logger.E_INFORMATION, "Removed '" + grp.groupName + "'");
			}
		}
		return anyChanges;
	}

	/**
	  Sorts the list by the sort-key, which is the key name from the
	  configuration properties file. Allows the user to control the order
	  in which groups are presented in the pull-down list.
	*/
	public void sort()
	{
		Collections.sort(groups);
	}

	/**
	 * IF we are using PDT, this method returns a list of all channels
	 * used by DCP in any of my groups. Else it returns null.
	 * @return list of channels used by our DCPs, or null if no PDT.
	 */
	public int[] getAggregateChannelList()
	{
		TreeSet<Integer> chans = new TreeSet<Integer>();
		Pdt pdt = Pdt.instance();
		for(DcpGroup grp : groups)
		{
			for(Iterator<DcpAddress> addrit = grp.getDcpAddresses(); 
				addrit.hasNext();)
			{
				DcpAddress addr = addrit.next();
				PdtEntry pdtEntry = pdt.find(addr);
				if (pdtEntry != null)
				{
					if (pdtEntry.st_channel > 0)
						chans.add(new Integer(pdtEntry.st_channel));
					if (pdtEntry.rd_channel > 0)
						chans.add(new Integer(pdtEntry.rd_channel));
				}
			}
		}
		int sz = chans.size();
		if (sz == 0)
			return null;
		int[] ret = new int[sz];
		Iterator<Integer> chanit = chans.iterator();
		for(int i=0; i<sz; i++)
			ret[i] = chanit.next().intValue();
		return ret;
	}

	/**
	 * @return the name for this DCP address or simply the address if none defined.
	 */
	public String getDcpName(DcpAddress addr)
	{
		for(DcpGroup grp : groups)
		{
			String n = grp.getDcpName(addr);
			if (n != null && n.length() > 0)
				return n;
		}
		return addr.toString();
	}
	
	/** Get a name for the first group where it's defined. */
	public synchronized String getDcpNameIfFound(DcpAddress addr)
	{
		for(DcpGroup grp : groups)
		{
			String n = grp.getDcpName(addr);
			if (n != null && n.length() > 0)
				return n;
		}
		return null;
	}
	
	public synchronized String getDcpDescription(DcpAddress addr)
	{
		for(DcpGroup grp : groups)
		{
			String n = grp.getDcpDescription(addr);
			if (n != null && n.length() > 0)
				return n;
		}
		return null;
	}
}
