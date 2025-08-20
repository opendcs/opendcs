package decodes.snotel;

import lrgs.common.DcpAddress;

/**
 * Bean class to hold a single entry in the ASRC/USDA Snotel Platform Spec File.
 */
public class SnotelPlatformSpec
{

	/** Unique ID for SNOTEL system */
	private int stationId = 0;
	
	/** Free form station name, may contain blanks */
	private String stationName = null;
	
	/** Unique GOES DCP identifier */
	private DcpAddress dcpAddress = null;
	
	/**
	 * A=legacy format, B=new format
	 */
	private char dataFormat = 'B';
	
	public SnotelPlatformSpec(int stationId, String stationName, DcpAddress dcpAddress, 
		char dataFormat)
	{
		super();
		this.stationId = stationId;
		this.stationName = stationName;
		this.dcpAddress = dcpAddress;
		this.dataFormat = dataFormat;
	}

	public int getStationId()
	{
		return stationId;
	}

	public void setStationId(int stationId)
	{
		this.stationId = stationId;
	}

	public String getStationName()
	{
		return stationName;
	}

	public void setStationName(String stationName)
	{
		this.stationName = stationName;
	}

	public DcpAddress getDcpAddress()
	{
		return dcpAddress;
	}

	public void setDcpAddress(DcpAddress dcpAddress)
	{
		this.dcpAddress = dcpAddress;
	}

	public char getDataFormat()
	{
		return dataFormat;
	}

	public void setDataFormat(char dataFormat)
	{
		this.dataFormat = dataFormat;
	}
	
	@Override
	public String toString()
	{
		return "" + stationId + "," + stationName + "," + dcpAddress + "," + dataFormat;
	}
}
