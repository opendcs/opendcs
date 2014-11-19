package decodes.dcpmon_old;

import ilex.util.StringPair;
import lrgs.common.DcpAddress;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.dcpmon.DcpMonitorConfig;
import decodes.dupdcpgui.DuplicateIo;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.util.hads.Hads;
import decodes.util.hads.HadsEntry;

/**
 * @deprecated
 */
public class DcpNameDescResolver
{
	DuplicateIo dupIo;

	/** Constructor */
	public DcpNameDescResolver()
	{
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		dupIo = new DuplicateIo(cfg.mergeDir, cfg.controlDistList);
		dupIo.readControllingDist();
	}
	
	public DuplicateIo getDuplicateIo() { return dupIo; }
	
	/**
	 * Finds out the dcp name to display on the Dcp Monitor web pages.
	 * 
	 * @param DcpAddress
	 * @return dcpName
	 */
	public String getBestName(DcpAddress dcpAddress, Platform p)
	{
		StringPair sp = getBestNameDesc(dcpAddress, p);
		return sp.first;
	}
	
	public StringPair getBestNameDesc(DcpAddress dcpAddress, Platform platform)
	{
		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		StringPair sp = new StringPair(null, null);
		PdtEntry pdtEntry = Pdt.instance().find(dcpAddress);
		DcpGroupList dgl = DcpGroupList.instance();
		
		// Platform from merged DB will be from controlling dist.
		if (platform != null)
		{
			Site pSite = platform.getSite();
			String pdesc = platform.getBriefDescription();
			if (pdesc.length() > 0)
				sp.second = getFirstLine(platform.description);
			else if (pSite != null && pSite.getBriefDescription().length() >0)
				sp.second = getFirstLine(pSite.getBriefDescription());
			else
			{
				sp.second = dgl.getDcpDescription(dcpAddress);
				if (sp.second == null)
				{
					if (pdtEntry != null)
						sp.second = pdtEntry.description;
					else
						sp.second = dcpAddress.toString();
				}
			}

			// Check if we have a dcpmon name type.
			if (pSite != null)
			{
				String dcpmonNameType = cfg.dcpmonNameType;
				SiteName sn = pSite.getName(dcpmonNameType);
				if (sn != null)
				{
					sp.first = sn.getNameValue();
					return sp;
				}
			}
		}

		// Else try to get name & desc from controlling dist record.
		String ctrlDistName = dupIo.getDistrictName(dcpAddress);
		if (ctrlDistName != null)
		{
			// Now find the dcpName from the controlling district group.
			DcpGroup grp = dgl.getGroup(ctrlDistName);
			if (grp == null)
				grp = dgl.getGroup(ctrlDistName + cfg.controlDistSuffix);
			if (grp != null)
			{
				sp.first = grp.getDcpName(dcpAddress);
				sp.second = grp.getDcpDescription(dcpAddress);
				if (sp.second == null)
				{
					sp.second = pdtEntry.description;
					if (sp.second == null)
						sp.second = dcpAddress.toString();
				}
				if (sp.first != null)
					return sp;
			}
		}


		// No ctrl district & no platform
		// Try to find any name/desc from any group.
		sp.first = dgl.getDcpNameIfFound(dcpAddress);
		sp.second = dgl.getDcpDescription(dcpAddress);
		if (sp.first != null && sp.second != null)
			return sp;

		// Second to last resort - use HADS
		if (cfg.hadsUse)
		{
			HadsEntry hadsEntry = Hads.instance().find(dcpAddress);
			if (hadsEntry != null && hadsEntry.dcpName != null)
			{
				if (sp.first == null)
					sp.first = hadsEntry.dcpName;
				if (sp.second == null)
					sp.second = hadsEntry.description;
				if (sp.first != null && sp.second != null)
					return sp;
			}
		}

		// No dcpname found anywhere - set it to dcp address
		if (sp.first == null)
			sp.first = dcpAddress.toString();
		if (pdtEntry != null && sp.second == null)
			sp.second = pdtEntry.description;
		if (sp.second == null)
			sp.second = dcpAddress.toString();
		return sp;
	}

	public DcpAddress name2dcpAddress(String name)
	{
		DcpGroupList dgl = DcpGroupList.instance();
		DcpAddress dcpAddress = dgl.getDcpAddress(name);
		if (dcpAddress != null)
			return dcpAddress;
		HadsEntry hadsEntry = Hads.instance().getByName(name);
		if (hadsEntry != null)
			return hadsEntry.dcpAddress;
		return null;
	}
	
	/**
	 * Finds out the dcp description to display on the Dcp Monitor web pages.
	 * @param daddr
	 * @return
	 */
	public String getBestDescription(DcpAddress dcpAddress, 
		Platform plat)
	{
		StringPair sp = getBestNameDesc(dcpAddress, plat);
		return sp.second;
	}

	private String getFirstLine(String tmp)
	{
		if (tmp == null)
			return "";
		int len = tmp.length();
		int ci = len;
		if (ci > 60)
			ci = 60;
		int i = tmp.indexOf('\r');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('\n');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('.');
		if (i > 0 && i < ci)
			ci = i;

		if (ci < len)
			return tmp.substring(0,ci);
		else
			return tmp;
	}
	
//	/**
//	 * Find a description based on a dcp address
//	 * @param siteDesc
//	 * @param dcpAddr
//	 * @return description
//	 */
//	private static String getPdtDescription(String siteDesc, 
//		DcpAddress dcpAddress)
//	{
//		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
//		PdtEntry pte;
//		String description = siteDesc;
//		if ((pte = Pdt.instance().find(dcpAddress)) != null)
//		{
//			if (pte.description != null 
//					 && pte.description.trim().length() > 0)
//				description = pte.description;
//		}
//		return description;
//	}
}
