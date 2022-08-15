package opendcs.opentsdb.hydrojson.beans;

import decodes.db.NetworkListEntry;

/**
 * Adapter for decodes.db.NetworkListEntry
 * @author mmaloney
 */
public class NetListItem
{
	/** The Transport Medium ID (eg DCP Address). */
	public String transportId;

	/** The name of the platform (site name). */
	private String platformName = null;

	/** A description */
	private String description = null;
	
	/**
	 * Default ctor required for deserializing data in POST.
	 */
	public NetListItem()
	{
	}

	public NetListItem(NetworkListEntry nle)
	{
		this.transportId = nle.transportId;
		this.platformName = nle.getPlatformName();
		this.description = nle.getDescription();
	}

	public NetListItem(String transportId, String platformName, String description)
	{
		super();
		this.transportId = transportId;
		this.platformName = platformName;
		this.description = description;
	}

	public String getTransportId()
	{
		return transportId;
	}

	public void setTransportId(String transportId)
	{
		this.transportId = transportId;
	}

	public String getPlatformName()
	{
		return platformName;
	}

	public void setPlatformName(String platformName)
	{
		this.platformName = platformName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

}
