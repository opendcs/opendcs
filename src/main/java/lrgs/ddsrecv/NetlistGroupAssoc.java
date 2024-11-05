package lrgs.ddsrecv;

import lrgs.common.NetworkList;

/**
 * Encapsulates an association between a network list (either DECODES or LRGS
 * and a DDS Receive Group (e.g. "primary", "secondary", etc.)
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
public class NetlistGroupAssoc
{
	/** the default group name */
	public static final String DEFAULT_GROUP = "primary";

	/** The name of the network list */
	private String netlistName = null;
	
	/** The name of the group */
	private String groupName = null;
	
	/** After evaluation, this will hold the network list data */
	private NetworkList networkList = null;
	
	NetlistGroupAssoc(String netlistName, String groupName)
	{
		this.netlistName = netlistName;
		this.groupName = groupName;
	}
	public NetworkList getNetworkList()
	{
		return networkList;
	}
	public void setNetworkList(NetworkList networkList)
	{
		this.networkList = networkList;
	}
	public String getNetlistName()
	{
		return netlistName;
	}
	public String getGroupName()
	{
		return groupName;
	}
}
