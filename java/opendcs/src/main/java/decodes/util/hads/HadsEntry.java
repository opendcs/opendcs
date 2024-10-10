package decodes.util.hads;

import java.util.regex.PatternSyntaxException;

import lrgs.common.DcpAddress;

/**
 * This class stores a line of data from a USGS Hads file.
 *
 */
public class HadsEntry implements Comparable<HadsEntry>
{
	public String dcpName = null;		//NWS 5 CHAR Ident - NWSLI
	public String usgsStationNum = null;//Station Number
	public DcpAddress dcpAddress = null;		//Primary Key
	public String dcpAddressStr = null;
	public String nwsHsa = null;		//
	public String latitude = null;  	//format (dd mm ss)
	public String longitude = null; 	//format (ddd mm ss)
	public String description = null; 	//Dcp location name
	
	public String dcpOwner = null;
	public String stateLocation = null;
	public String hydroServiceArea = null;
	public String initialXmtime = null;
	public String dcpTransmissionInterval = null;
	
	/**
	 * Constructs a new empty HadsEntry.
	 * values must be then set manually.
	 */
	public HadsEntry()
	{
	}
	
	/**
	 * Constructs a new PDT entry from a line from the the pdt flat file.
	 */
	public HadsEntry(String hadsFileLine)
		throws BadHadsEntryException
	{
		this();
		assignFromLine(hadsFileLine);
	}

	/**
	 * This method parses out a Hads file line and 
	 * store the fields in this class.
	 * 
	 * @param line
	 * @throws BadHadsEntryException
	 */
	public void assignFromLine(String line)
		throws BadHadsEntryException
	{
		//Split the line - strArray will contain all the fields
		String[] strArray = null;
		try 
		{
			//This code parse this file:
			//http://www.weather.gov/ohd/hads/compressed_defs/all_dcp_defs.txt
			strArray = line.split("\\|");
			//DCP Address - GOES NESDIS ID
			if (strArray[0] != null)
			{
				strArray[0] = strArray[0].trim();
				dcpAddress = new DcpAddress(strArray[0]);
				dcpAddressStr = strArray[0];
			}
			//NWSSLI - NWSHB5 Code
			dcpName = strArray[1];
			if (dcpName != null)
				dcpName = dcpName.trim();
			//DCP Owner
			dcpOwner = strArray[2];
			if (dcpOwner != null)
				dcpOwner = dcpOwner.trim();
			//State Location
			stateLocation = strArray[3];
			if (stateLocation != null)
				stateLocation = stateLocation.trim();
			//Hydro Service
			hydroServiceArea = strArray[4];
			if (hydroServiceArea != null)
				hydroServiceArea = hydroServiceArea.trim();
			//lat - long
			latitude = strArray[5];//lat
			longitude = strArray[6];//long
			//initial transmission time UTC
			initialXmtime = strArray[7];
			if (initialXmtime != null)
				initialXmtime = initialXmtime.trim();
			//interval (minutes)
			dcpTransmissionInterval = strArray[8];
			if (dcpTransmissionInterval != null)
				dcpTransmissionInterval = dcpTransmissionInterval.trim();
			//DCP Locattion
			description = strArray[9];
			if (description != null)
				description = description.trim();
			
			//THIS was parsing this file: 
			//http://www.weather.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt
//			strArray = line.split("\\|");
//			dcpName = strArray[0];
//			if (dcpName != null)
//				dcpName = dcpName.trim();
//			usgsStationNum = strArray[1];
//			if (usgsStationNum != null)
//				usgsStationNum = usgsStationNum.trim();
//			
//			if (strArray[2] != null)
//			{
//				strArray[2] = strArray[2].trim();
//				DcpAddress da = new DcpAddress(strArray[2]);
//				dcpAddress = da.getAddr();
//				dcpAddressStr = strArray[2];
//			}
//			nwsHsa = strArray[3];//NWS HSA
//			if (nwsHsa != null)
//				nwsHsa = nwsHsa.trim();
//			latitude = strArray[4];//lat
//			longitude = strArray[5];//long
//			
//			description = strArray[6];
//			if (description != null)
//				description = description.trim();
		}
		catch(NumberFormatException ex)
		{
			throw new BadHadsEntryException("Bad DCP address '" + strArray[0]
				+ "'");
		}
		catch(PatternSyntaxException ex)  
		{
			throw new BadHadsEntryException("Cannot parse this line '" + 
					line + "' " + ex.getMessage());
		}
		catch(IndexOutOfBoundsException ex)  
		{
			throw new BadHadsEntryException("Cannot parse this line '" + 
					line + "' " + ex.getMessage());
		}
	}
	
	/**
	 * Compare HadsEntry by dcpAddress
	 */
	public int compareTo(HadsEntry hads)
	{
		return this.dcpAddress.compareTo(hads.dcpAddress);
	}
	
	/** For testing */
	public String toString()
	{
		return 
		   dcpName + ":"
		 + usgsStationNum + ":"
		 + dcpAddress + ":"
		 + nwsHsa + ":"
		 + latitude + ":"
		 + longitude + ":"
		 + description;
	}
}
