/*
*  $Id$
*/
package decodes.dcpmon;

import ilex.util.Logger;
import lrgs.common.DcpAddress;
import decodes.db.*;
import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;

/**
This class holds platform information necessary for DCP mon. The info
comes either from the PDT (preferred) or the DECODES Database.
*/
public class PlatformInfo
{
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
	public static synchronized PlatformInfo getPlatformInfo(
		RawMessage rawMsg, DcpAddress dcpAddress, int chan)
	{
		PlatformInfo ret = null;

		PdtEntry pte = null;
		if ((pte = Pdt.instance().find(dcpAddress)) != null)
		{
			ret = new PlatformInfo('S', pte.st_first_xmit_sod, 
				pte.st_xmit_window, pte.st_xmit_interval, pte.baud, 
				pte.st_channel, chan == pte.st_channel, pte.description);
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
			Logger.instance().warning("PlatformInfo: Error looking up info "
				+ "for '" + dcpAddress + "': " + ex);
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
				ret.baud = ChannelMap.instance().getBaud(chan);
			}
		}

		//Code added for Dcp Monitor Ehancement Problem #2.
		//Reset the description so that we use the same description
		//in all dcp monitor web pages
		String tempDesc = 
			DcpMonitor.instance().getDcpNameDescResolver().getBestDescription(
				dcpAddress, plat);
		if (tempDesc != null && tempDesc.trim().length() > 0)
		{
			if (ret != null)
				ret.platformDescription = tempDesc;
		}
		
		if (plat == null && pte == null)
		{
			Logger.instance().warning("No info about DCP Address '"
				+ dcpAddress + "' in PDT or DECODES. -- "
				+ "Assuming random 300baud Xmit");
			return ret;
		}
		return ret;
	}
}
