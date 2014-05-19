/**
 * $Id$
 * 
 * Open Source Software.
 * Author: Mike Maloney, Cove Software, LLC.
 * 
 * $Log$
 * Revision 1.2  2013/03/03 15:46:09  mmaloney
 * Implement local filters for when talking to legacy servers.
 *
 * Revision 1.1  2013/02/28 16:48:28  mmaloney
 * Created.
 *
 */
package lrgs.ldds;

import java.io.File;

import decodes.util.ChannelMap;
import decodes.util.DecodesSettings;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.SearchCriteria;

/**
 * When talking to a legacy server, some of the search-criteria
 * may need to be evaluated locally. This class serves that purpose.
 * 
 * When sending a searchcrit to a server, LddsClient will call
 * SearchCriter.toString(protoVersion), which builds a string representation
 * of the criteria that the specified server version can understand.
 * It also builds a SearchCritLocalEditor if necessary which can
 * be retrieved with SearchCritera.getLocalFilter() immediately
 * after calling toString.
 */
public class SearchCritLocalFilter
{
	private SearchCriteria searchCrit = null;
	private int serverVersion = 0;
	private ChannelMap channelMap = ChannelMap.instance();
	boolean goesST = false, goesRD = false;
	
	public SearchCritLocalFilter(SearchCriteria searchCrit,
		int serverVersion)
	{
		this.searchCrit = searchCrit;
		this.serverVersion = serverVersion;
		for(int i = 0; i< searchCrit.numSources; i++)
			if (searchCrit.sources[i] == DcpMsgFlag.MSG_TYPE_GOES_ST)
				goesST = true;
			else if (searchCrit.sources[i] == DcpMsgFlag.MSG_TYPE_GOES_RD)
				goesRD = true;
		if ((goesST || goesRD) && ! (goesST && goesRD)
			&& !channelMap.isLoaded())
		{
			channelMap.load(new File(DecodesSettings.instance().cdtLocalFile));
		}
	}
	
	public boolean passesCrit(DcpMsg msg)
	{
		if (searchCrit.parityErrors == SearchCriteria.REJECT
		 && msg.getFailureCode() == '?')
			return false;
		if (searchCrit.parityErrors == SearchCriteria.EXCLUSIVE
		 && msg.getFailureCode() == 'G')
			return false;

		// If only want self timed, but this idx is random
		if (goesST && !goesRD && channelMap.isLoaded()
		 && channelMap.isRandom(msg.getGoesChannel()))
			return false;
		// If only want random, but this idx is self timed
		if (!goesST && goesRD && channelMap.isLoaded()
		 && !channelMap.isRandom(msg.getGoesChannel()))
			return false;
		
		// Passes all tests
		return true;
	}
}
