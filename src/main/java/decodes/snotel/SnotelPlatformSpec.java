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
	
	/** Channels = Sensor Values */
	private int numChannels = 0;
	
	/** 
	 * Number of hours. Message will have one value for each channel 
	 * for each hour. Currently, most SNOTEL DCPs just have a single
	 * hour's data.
	 */
	private int numHours = 1;
	
	/**
	 * B=(signed) pseudobinary, A=ASCII
	 */
	private char dataFormat = 'B';
	
	public SnotelPlatformSpec(int stationId, String stationName, DcpAddress dcpAddress, int numChannels, int numHours,
			char dataFormat)
	{
		super();
		this.stationId = stationId;
		this.stationName = stationName;
		this.dcpAddress = dcpAddress;
		this.numChannels = numChannels;
		this.numHours = numHours;
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

	public int getNumChannels()
	{
		return numChannels;
	}

	public void setNumChannels(int numChannels)
	{
		this.numChannels = numChannels;
	}

	public int getNumHours()
	{
		return numHours;
	}

	public void setNumHours(int numHours)
	{
		this.numHours = numHours;
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
		return "" + stationId + "," + stationName + "," + dcpAddress
			+ "," + numChannels + "," + numHours + "," + dataFormat;
	}
}
