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
package decodes.dcpmon;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import decodes.db.*;
import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
This class holds platform information necessary for DCP mon. The info
comes either from the PDT (preferred) or the DECODES Database.
*/
public class PlatformInfo
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	char preamble;
	int firstXmitSecOfDay;
	int windowLength;
	int xmitInterval;
	int baud;
	int stchan;
	boolean msgIsST;
	String platformDescription;

	PlatformInfo(char preamble, int firstXmitSecOfDay, int windowLength,
		int xmitInterval, int baud, int stchan, boolean msgIsST, 
		String platformDescription)
	{
		this.preamble = preamble;
		this.firstXmitSecOfDay = firstXmitSecOfDay;
		this.windowLength = windowLength;
		this.xmitInterval = xmitInterval;
		this.baud = baud;
		this.stchan = stchan;
		this.msgIsST = msgIsST;
		this.platformDescription = platformDescription;
	}

	/**
	 * Gets platform info from either PDT or DECODES database, or both.
	 * If both fail, mocks up a pseudo record. Guaranteed not to return null.
	 * @return PlatformInfo for the requested platform.
	 */
	public static synchronized PlatformInfo getPlatformInfo(RawMessage rawMsg)
	{
		DcpMsg dcpMsg = rawMsg.getOrigDcpMsg();
		DcpAddress dcpAddress = dcpMsg.getDcpAddress();
		PlatformInfo ret = null;

		PdtEntry pte = null;
		if ((pte = Pdt.instance().find(dcpAddress)) != null)
		{
			ret = new PlatformInfo('S', pte.st_first_xmit_sod, 
				pte.st_xmit_window, pte.st_xmit_interval, pte.baud, 
				pte.st_channel, dcpMsg.getGoesChannel() == pte.st_channel, 
				pte.description);
		}
		else
		{
			ret = new PlatformInfo('S', 0, 0, 0, 300, 0, false, 
				"Unknown platform '" + dcpAddress + "'");
		}

		// The DECODES Database will have a better description.
		// So even if we have a PDT entry, still try to lookup
		// in the DECODES Db.
		Platform plat = null;
		TransportMedium tm = null;
		try
		{
			if (rawMsg != null)
			{
				plat = rawMsg.getPlatform();
				tm = rawMsg.getTransportMedium();
			}
			else // rawMsg is null
			{
				plat = Database.getDb().platformList.getPlatform(
					Constants.medium_Goes, dcpAddress.toString());
				if (plat != null)
					tm = plat.getTransportMedium(Constants.medium_Goes);
			}
		}
		catch(UnknownPlatformException ex)
		{
			plat = null;
			tm = null;
		}
		catch(DatabaseException ex)
		{
			log.atWarn().setCause(ex).log("PlatformInfo: Error looking up info for {}", dcpAddress);
			plat = null;
			tm = null;
		}

		if (plat != null)
		{
			// Prefer DECODES database description to PDT.
			if (plat.description != null 
			 && plat.description.trim().length() > 0)
			{
				ret.platformDescription = plat.getBriefDescription();
			}

			// If no PDT, get schedule from DECODES DB.
			if (pte == null && tm != null)
			{
				ret.preamble = tm.getPreamble();
				ret.firstXmitSecOfDay = tm.assignedTime;
				ret.windowLength = tm.transmitWindow;
				ret.xmitInterval = tm.transmitInterval;
				ret.baud = ChannelMap.instance().getBaud(dcpMsg.getGoesChannel());
			}
		}

		
		if (plat == null && pte == null)
		{
			log.warn("No info about DCP Address '{}' in PDT or DECODES. -- Assuming random 300baud Xmit",
					 dcpAddress);
			return ret;
		}
		return ret;
	}
}
