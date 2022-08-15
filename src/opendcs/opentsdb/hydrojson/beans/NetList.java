package opendcs.opentsdb.hydrojson.beans;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlRootElement;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.sql.DbKey;

/**
 * Adapter for decodes.db.NetworkList
 * @author mmaloney
 *
 */
@XmlRootElement
public class NetList
{
	/** Unique surrogate key ID of this network list */
	private long netlistId = DbKey.NullKey.getValue();

	/** Unique name of this network list. */
	private String name;

	/** Type of transport medium stored in this network list. */
	private String transportMediumType;

	/** Preferred name type for this network list. */
	private String siteNameTypePref;

	/** Time that this network list was last modified in the database. */
	private Date lastModifyTime;

	/**
	 * This HashMap stores the NetworkListEntry objects, indexed by their
	 * transportId's.  The transportId's are converted to uppercase before
	 * being used as a key in this HashMap.
	 * This data member is never null.
	 */
	private HashMap<String, NetListItem> items = new HashMap<String, NetListItem>();

	/**
	 * Default ctor required for POST method.
	 */
	public NetList()
	{
		name = null;
		transportMediumType = null;
		siteNameTypePref = null;
	}
	
	public NetList(NetworkList nl)
	{
		this.netlistId = nl.getId().getValue();
		this.name = nl.name;
		this.transportMediumType = nl.transportMediumType;
		this.siteNameTypePref = nl.siteNameTypePref;
		for(Iterator<NetworkListEntry> nleit = nl.iterator(); nleit.hasNext(); )
		{
			NetworkListEntry nle = nleit.next();
			items.put(nle.transportId, new NetListItem(nle));
		}
	}
	
	/**
	 * Converts this JSON data structure to a DECODES NetworkList object.
	 * @return the netlist object, which will have a null DbKey ID.
	 */
	public NetworkList toNetworkList()
	{
		NetworkList ret = new NetworkList(name, transportMediumType);
		ret.siteNameTypePref = siteNameTypePref;
		
		for(NetListItem nli : items.values())
		{
			NetworkListEntry nle = new NetworkListEntry(ret, nli.transportId);
			nle.setPlatformName(nli.getPlatformName());
			nle.setDescription(nli.getDescription());
			ret.addEntry(nle);
		}

		return ret;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTransportMediumType()
	{
		return transportMediumType;
	}

	public void setTransportMediumType(String transportMediumType)
	{
		this.transportMediumType = transportMediumType;
	}

	public String getSiteNameTypePref()
	{
		return siteNameTypePref;
	}

	public void setSiteNameTypePref(String siteNameTypePref)
	{
		this.siteNameTypePref = siteNameTypePref;
	}

	public Date getLastModifyTime()
	{
		return lastModifyTime;
	}

	public void setLastModifyTime(Date lastModifyTime)
	{
		this.lastModifyTime = lastModifyTime;
	}

	public HashMap<String, NetListItem> getItems()
	{
		return items;
	}

	public void setItems(HashMap<String, NetListItem> items)
	{
		this.items = items;
	}

	public long getNetlistId()
	{
		return netlistId;
	}

	public void setNetlistId(long netlistId)
	{
		this.netlistId = netlistId;
	}

}
