package org.opendcs.odcsapi.lrgsclient;

import java.sql.SQLException;

import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiSearchCrit;
import org.opendcs.odcsapi.dao.ApiPlatformDAO;
import org.opendcs.odcsapi.dao.DbException;


public class SearchCritUtil
{
	/**
	 * Preprocess the sc by translating names to IDs, etc., then
	 * convert to string and return. The passed searchcrit remains
	 * unchanged.
	 * @param sc the searchcrit
	 * @return a string version suitable for transmit over DDS.
	 * @throws SQLException 
	 */
	public static String sc2String(ApiSearchCrit sc, ApiPlatformDAO platformDAO) throws SQLException
	{
		StringBuffer ret = new StringBuffer("#\n# API Search Criteria\n#\n");
		String lineSep = "\n";
		
		if (sc.getSince() != null)
			ret.append("DRS_SINCE: " + sc.getSince() + lineSep);
		if (sc.getUntil() != null)
			ret.append("DRS_UNTIL: " + sc.getUntil() + lineSep);

		// Note: The DDS session needs to send the netlists prior to sending searchcrit.
		for (String nl : sc.getNetlistNames())
		{
			ret.append("NETWORKLIST: " + nl + lineSep);
		}

		// Attempt to convert names to addrs, leave as-is if conversion fails.
System.out.println("sc2String there are " + sc.getPlatformNames().size() + " platform names.");
		for(String platname : sc.getPlatformNames())
		{
			String tmid;
			try
			{
				tmid = platformDAO.platformName2transportId(platname);
System.out.println("translate name '" + platname + "' to ID=" + tmid);
				if (tmid != null)
					ret.append("DCP_ADDRESS: " + tmid + lineSep);
				else
					ret.append("DCP_NAME: " + platname + lineSep);
			}
			catch (DbException ex)
			{
				System.err.println("SearchCritUtil.sc2String cannot lookup platname '" 
					+ platname + "': " + ex);
			}
		}

		for(String id : sc.getPlatformIds())
			ret.append("DCP_ADDRESS: " + id + lineSep);
		
		if (sc.isSettlingTimeDelay())
			ret.append("RT_SETTLE_DELAY: true" + lineSep);
		
		if (sc.isQualityNotifications())
			ret.append("DAPS_STATUS: A" + lineSep);
		
		for(int c : sc.getGoesChannels())
			ret.append("CHANNEL: |" + c + lineSep);
		
		if (sc.isParityCheck() && sc.getParitySelection() != null)
		{
			ret.append("PARITY_ERROR: " +
				(sc.getParitySelection().equalsIgnoreCase("good") ? "R" : "A") + lineSep);
		}
		
		if (sc.isGoesSpacecraftCheck() && sc.getGoesSpacecraftSelection() != null)
		{
			ret.append("SPACECRAFT: " +
				(sc.getGoesSpacecraftSelection().toLowerCase().startsWith("e") ? "E" : "W") + lineSep);
		}
		
		if (sc.isGoesSelfTimed())
			ret.append("SOURCE: GOES_SELFTIMED" + lineSep);
		if (sc.isGoesRandom())
			ret.append("SOURCE: GOES_RANDOM" + lineSep);
		if (sc.isNetworkDCP())
			ret.append("SOURCE: NETDCP" + lineSep);
		if (sc.isIridium())
			ret.append("SOURCE: IRIDIUM" + lineSep);

		return ret.toString(); // string containing complete searchcrit file.
	}

	public static String nl2String(ApiNetList nl)
	{
		StringBuffer ret = new StringBuffer("#\n# API Network List " + nl.getName() + "\n#\n");
		String lineSep = "\n";
		
		for(ApiNetListItem item : nl.getItems().values())
			ret.append(item.transportId + ":"
				+ (item.getPlatformName() == null ? "" : item.getPlatformName()+" ")
				+ (item.getDescription() == null ? "" : item.getDescription())
				+ lineSep);
		
		return ret.toString();
	}

}
