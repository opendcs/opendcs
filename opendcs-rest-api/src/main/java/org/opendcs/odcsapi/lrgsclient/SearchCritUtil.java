/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.lrgsclient;

import java.sql.SQLException;

import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiSearchCrit;
import org.opendcs.odcsapi.dao.ApiPlatformDAO;
import org.opendcs.odcsapi.dao.DbException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchCritUtil
{
	public static String module = "SearchCritUtil";
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchCritUtil.class);

	/**
	 * Preprocess the sc by translating names to IDs, etc., then
	 * convert to string and return. The passed searchcrit remains
	 * unchanged.
	 * @param sc the searchcrit
	 * @return a string version suitable for transmit over DDS.
	 * @throws SQLException 
	 */
	public static String sc2String(ApiSearchCrit sc, ApiPlatformDAO platformDAO) throws SQLException, DbException {
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
		LOGGER.debug("sc2String there are {} platform names.", sc.getPlatformNames().size());
		for(String platname : sc.getPlatformNames())
		{
			String tmid;
			try
			{
				tmid = platformDAO.platformName2transportId(platname);
				LOGGER.debug("translate name '{}' to ID={}", platname, tmid);
				if (tmid != null)
					ret.append("DCP_ADDRESS: " + tmid + lineSep);
				else
					ret.append("DCP_NAME: " + platname + lineSep);
			}
			catch (DbException ex)
			{
				throw new DbException(module, ex,
						String.format("SearchCritUtil.sc2String cannot lookup platname '%s'", platname));
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
