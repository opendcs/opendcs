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
		String[] strArray = line.split("\\|");;
		try
		{
			//This code parse this file:
			//http://www.weather.gov/ohd/hads/compressed_defs/all_dcp_defs.txt
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

		}
		catch(NumberFormatException ex)
		{
			throw new BadHadsEntryException("Bad DCP address '" + strArray[0] + "'", ex);
		}
		catch(PatternSyntaxException ex)
		{
			throw new BadHadsEntryException("Cannot parse this line '" + line + "'", ex);
		}
		catch(IndexOutOfBoundsException ex)
		{
			throw new BadHadsEntryException("Cannot parse this line '" + line + "'", ex);
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