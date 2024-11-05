/**
 * $Id$
 * 
 * Open source software.
 * Author: Mike Maloney, Cove Software, LLC
 * 
 * $Log$
 * Revision 1.1  2013/02/28 16:39:58  mmaloney
 * Created.
 *
 */
package decodes.util;

import java.util.StringTokenizer;

/**
 * This class holds a line in the file:
 * http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt
 */
public class NwsXrefEntry
{
	/** NWS Identifier, a.k.a. NWSHB5 name */
	private String nwsId = null;

	/** USGS Station Number */
	private String usgsNum = null;
	
	/** GOES DCP Address */
	private String goesDcpAddr = null;
	
	/** NWS HSA 3-char Identifier */
	private String nwsHsa = null;
	
	/** Latitude in dd mm ss format */
	private String latitude = null;
	
	/** longitude in +-ddd mm ss format */
	private String longitude = null;
	
	/** Longer descriptive name of location */
	private String locationName = null;
	
	private NwsXrefEntry()
	{
	}
	
	/**
	 * Parse a line in the downloaded file.
	 * Example Line:
	 * APTA2|15239900      |173A91AA|AFC|59 44 50|-151 45 11|ANCHOR R NR ANCHOR POINT AK
	 * All fields are required.
	 * @param line the line from the file
	 * @return new NwsXrefEntry or null if bad parse.
	 */
	public static NwsXrefEntry fromFileLine(String line)
	{
		NwsXrefEntry ret = new NwsXrefEntry();
		StringTokenizer st = new StringTokenizer(line, "|");
		if (!st.hasMoreTokens())
			return null;
		ret.nwsId = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.usgsNum = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.goesDcpAddr = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.nwsHsa = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.latitude = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.longitude = st.nextToken().trim();
		
		if (!st.hasMoreTokens())
			return null;
		ret.locationName = st.nextToken().trim();
		
		return ret;
	}

	public String getNwsId()
	{
		return nwsId;
	}

	public String getUsgsNum()
	{
		return usgsNum;
	}

	public String getGoesDcpAddr()
	{
		return goesDcpAddr;
	}

	public String getNwsHsa()
	{
		return nwsHsa;
	}

	public String getLatitude()
	{
		return latitude;
	}

	public String getLongitude()
	{
		return longitude;
	}

	public String getLocationName()
	{
		return locationName;
	}
	
	public String toString()
	{
		return nwsId + " | " + usgsNum + " | " + goesDcpAddr
			+ " | " + nwsHsa + " | " + latitude + " | " + longitude
			+ " | " + locationName;
	}
}
