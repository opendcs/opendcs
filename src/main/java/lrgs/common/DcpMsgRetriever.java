/*
*  $Id$
*/
package lrgs.common;

import ilex.util.EnvExpander;

import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Arrays;

import lrgs.lrgsmain.LrgsConfig;
import ilex.util.Logger;

/**
Provides a portable mechanism to retrieve DCP messages. 
The actual IO is delegated to an abstract DcpMsgSource that must be supplied.
The intent was to allow this class to be able to read DCP messages from a
variety of places.
*/
public class DcpMsgRetriever
{
	/** The search criteria to use to filter messages */
	protected SearchCriteria crit;

	/** The supplied message source */
	protected DcpMsgSource source;

	/** Maps DCP names to hex addresses */
	protected DcpNameMapper mapper;

	/** Aggregate of all selected DCP addresses from network lists, etc. */
	protected DcpAddress aggregateList[];

	/** Only pass messages since this time. */
	protected Date DapsSince;

	/** Only pass messages until this time. */
	protected Date DapsUntil;

	/** Username needed to find the sandbox to store searchcrit */
	protected String username;

	/** User sandbox directores are sub-directories under this. */
	private File userSandbox;

	/** True if the receiver is currently timed-out. */
	public boolean timedOut;

	/** used to test acceptable baud rate in incoming messages. */
	boolean incl100 = true;
	/** used to test acceptable baud rate in incoming messages. */
	boolean incl300 = true;
	/** used to test acceptable baud rate in incoming messages. */
	boolean incl1200 = true;

	/**
	  Constructor.
	  @throws IOException if search criteria references a netlist that can't
	   be read.
	  @throws SearchSyntaxException on other problems in the search crit.
	  @throws ArchiveUnavailableException if archive unavailable
	*/
	public DcpMsgRetriever()
	{
		this.crit = new SearchCriteria();
		this.source = null;
		timedOut = false;
		mapper = null;
		aggregateList = null;
		DapsSince = null;
		DapsUntil = null;
		username = null;
		userSandbox = null;
		timedOut = false;
		incl100 = true;
		incl300 = true;
		incl1200 = true;
	}

	/**
	 * Sets the 'source' interface to be used by this retriever.
	 * @param source the source
	 */
	public void setDcpMsgSource(DcpMsgSource source)
	{
		this.source = source;
	}

	/**
	  Sets the username used to determine the sandbox directory.
	  @param username the username
	*/
	public void setUsername(String username)
	{
		this.username = username;
	}

	/** 
	  User sandbox directors are sub-directories under this.
	  @param sandboxDir the parent of user directories
	*/
	public void setUserSandbox(File sandboxDir)
	{
		this.userSandbox = sandboxDir;
	}

	/**
	  The name mapper is used when DCP names are included in the search
	  criteria. It takes an ASCII name and maps it to a DCP address. You
	  must supply the mapper to the DcpMsgRetriever object.
	  @param mapper the mapper
	  @see DcpNameMapper
	*/
	public void setDcpNameMapper(DcpNameMapper mapper)
	{
		this.mapper = mapper;
	}

	/**
	  Change the search criteria object associated with this retriever.
	  @param crit the new criteria to use.
	*/
	public void setSearchCriteria(SearchCriteria crit)
		throws IOException, SearchSyntaxException, ArchiveUnavailableException
	{
		this.crit = crit == null ? new SearchCriteria() : crit;
		init();
	}

	/**
	  Initialize the retriever.
	  @throws IOException if unable to read a network list file.
	  @throws SearchSyntaxException if bad date format in Since/Until.
	  @throws ArchiveUnavailableException if not connected.
	*/
	public void init() 
		throws IOException, SearchSyntaxException, ArchiveUnavailableException
	{
		// Clear out any previously-set criteria.
		aggregateList = null;
		DapsSince = null;
		DapsUntil = null;
		if (source != null)
		{
			source.setSourceLrgsSinceTime(null);
			source.setSourceLrgsUntilTime(null);
		}

		// Load network lists into aggregate sorted list of DCP addresses.
		if (crit.NetlistFiles != null)
			for(String fn : crit.NetlistFiles)
			{
				File f = findNetworkListFile(fn);
				if (f != null)
					loadNetworkListFile(f);
				else
					throw new IOException("No such network list '" + fn + "'");
			}
		loadDcpAddresses();

		// sort all addresses in ascending order.
		if (aggregateList != null)
			java.util.Arrays.sort(aggregateList);

		// Initialize time ranges in source.
		String tstr = crit.getLrgsSince();
		if (tstr != null && source != null)
		{
			if (tstr.equalsIgnoreCase("last"))
				source.setSourceLrgsSinceLast();
			else
				source.setSourceLrgsSinceTime(crit.evaluateLrgsSinceTime());
		}

		if (source != null)
			source.setSourceLrgsUntilTime(crit.evaluateLrgsUntilTime());

		DapsSince = crit.evaluateDapsSinceTime();
		DapsUntil = crit.evaluateDapsUntilTime();

		if (crit.baudRates == null || crit.baudRates.trim().length() == 0)
		{
			incl100 = incl300 = incl1200 = true;
		}
		else
		{
			incl100 = crit.baudRates.contains("100");
			incl300 = crit.baudRates.contains("300");
			incl1200 = crit.baudRates.contains("1200");
		}
	}

	/**
	  Retrieve the next index that passes the loaded criteria.
	  @param idx the DcpMsgIndex structure to populate
	  @param stopSearchMsec msec value at which to stop searching.
	  @return index number (i.e. the index of the index)

	  @throws ArchiveUnavailableException on internal archiving error
	  @throws UntilReachedException if specified until time was reached
	  @throws SearchTimeoutException if stopSearchMsec reached & no msg rcv'd
	   (this essentially means 'try again')
	  @throws EndOfArchivException if all indexes have been checked with no
	   match.
	*/
	public int getNextPassingIndex(DcpMsgIndex idx, long stopSearchMsec)
		throws ArchiveUnavailableException, UntilReachedException, 
			SearchTimeoutException, EndOfArchiveException
	{
		throw new ArchiveUnavailableException(
			"This method overloaded by MessageArchiveRetriever. "
			+ "Should never be called!",
			LrgsErrorCode.DDDSINTERNAL);
	}

	/**
	  This is a pass-through function that simply calls the readMsg method
	  in the source.
	  @param idx the DcpMsgIndex structure to populate
	  @return the DcpMsg from the source
	  @see lrgs.common.DcpMsgSource
	*/
	public DcpMsg readMsg(DcpMsgIndex idx)
		throws ArchiveUnavailableException, NoSuchMessageException
	{
		return source.readMsgFromSource(idx);
	}


	//=====================================================================
	// Methods for testing DCP Message Index objects against the criteria.
	//=====================================================================
	/**
	  Test the passed message index against the search criteria loaded
	  here. Return true if the index passes the criteria, false if not.
	  @param msgidx the DcpMsgIndex structure to test
	  @return true if this index passes the criteria.
	*/
	public boolean testCriteria(DcpMsgIndex msgidx)
	{
	    /* DCP Address Checking: */
		boolean addrpass = false;
		boolean do_addr_chk = aggregateList != null && aggregateList.length>0;

		// Test the data source, if one is specified
		if (crit.numSources > 0)
		{
			int i;
			for(i=0; i<crit.numSources; i++)
			{
				if ((msgidx.getFlagbits() & DcpMsgFlag.SRC_MASK) == crit.sources[i])
					break;
				if (crit.sources[i] == DcpMsgFlag.SRC_IRIDIUM
				 && DcpMsgFlag.isIridium(msgidx.getFlagbits()))
					break;
				if (crit.sources[i] == DcpMsgFlag.SRC_NETDCP
				 && DcpMsgFlag.isNetDcp(msgidx.getFlagbits()))
					break;
				if (crit.sources[i] == DcpMsgFlag.MSG_TYPE_GOES_ST
				 && DcpMsgFlag.isGoesST(msgidx.getFlagbits()))
					break;
				if (crit.sources[i] == DcpMsgFlag.MSG_TYPE_GOES_RD
				 && DcpMsgFlag.isGoesRD(msgidx.getFlagbits()))
					break;
			}
			if (i == crit.numSources) /* fell through -- no match! */
				return false;
		}
		DcpAddress testaddr = msgidx.getDcpAddress();
		if (do_addr_chk && Arrays.binarySearch(aggregateList, testaddr) < 0)
			addrpass = false;
		else
			addrpass = true;
//Logger.instance().info("testCriteria: addrpass =" + addrpass + " address=" + testaddr);
	
		if (crit.channels == null || crit.channels.length <= 0)
		{
			if (!addrpass)
				return false;  /* addr fails & no channels specified. */
	
			/* else skip channel check in next block of code... */
		}
		else
		{
		    // Check channels: bit specifies how to combine with address crit.
			boolean no_ands = true;
			boolean chanpass = false;

			for(int i=0; i<crit.channels.length; i++)
			{
				int chan = crit.channels[i];

				if ((chan & SearchCriteria.CHANNEL_AND) != 0)
					no_ands = false;
	
			    if ((chan & (~SearchCriteria.CHANNEL_AND)) == msgidx.getChannel())
			    {
					if ((chan & SearchCriteria.CHANNEL_AND) != 0)
					{
			    		// Both addr checking AND channel number must match!
				    	if (addrpass)
						{
				    		chanpass = true;
							break;
						}
				    	else
							return false;
					}
					else // OR: Either addr checking or channel num must match
					{
						chanpass = true;
						break;
					}
			    }
			}
			if (chanpass || (no_ands && addrpass && do_addr_chk))
				;
			else
				return false;
		}
//Logger.instance().info("testCriteria: DapsSince='" + DapsSince 
//+ "', DapsUntil='" + DapsUntil + "'");
	
	    /* DAPS time range: */
		Date dapstime = msgidx.getXmitTime();
		if (DapsSince != null && dapstime.compareTo(DapsSince) < 0)
			return false;
		if (DapsUntil != null && dapstime.compareTo(DapsUntil) > 0)
			return false;
	
	    /* Retransmitted messages only: */
		boolean isRetrans = (msgidx.getFlagbits() & DcpMsgFlag.DUP_MSG) != 0;
		if (isRetrans && (crit.Retrans == SearchCriteria.REJECT 
		               || crit.Retrans == SearchCriteria.NO))
			return false;
		else if (!isRetrans && crit.Retrans == SearchCriteria.EXCLUSIVE)
			return false;
//Logger.instance().info("testCriteria: Checking daps status indicators");

	    // DAPS Status messages: (anything with failure code != 'G' or '?')
		boolean isDapsStat = 
			msgidx.getFailureCode() != 'G' && msgidx.getFailureCode() != '?';
		if (isDapsStat && (crit.DapsStatus == SearchCriteria.REJECT 
		                || crit.DapsStatus == SearchCriteria.NO))
			return false;
		else if (!isDapsStat && crit.DapsStatus == SearchCriteria.EXCLUSIVE)
			return false;

//Logger.instance().info("testCriteria: crit.spacecraft='"
//+crit.spacecraft + "', chan=" + msgidx.getChannel());

		// Particular spacecraft
		if (crit.spacecraft == SearchCriteria.SC_EAST
		 && (msgidx.getChannel() % 2) == 0)
			return false;
		else if (crit.spacecraft == SearchCriteria.SC_WEST
		 && (msgidx.getChannel() % 2) == 1)
			return false;
			
		// This part may seem counter-intuitive, but if a sequence range
		// is set, then we only want to pass messages that have NO 
		// sequence number. The reason is that CmdGetMsgBlockExt will
		// first use the separate sequence-data-structures to look for
		// messages matching the sequence range. Now we want to add messages
		// in the time range with NO sequence number.
//Logger.instance().info("crit.seqStart=" + crit.seqStart 
//+ ", crit.seqEnd=" + crit.seqEnd
//+ ", msgidx.SequenceNum=" + msgidx.getSequenceNum());
		if (crit.seqStart != -1 && crit.seqEnd != -1
		 && msgidx.getSequenceNum() >= 0)
			return false;

		int br = msgidx.getFlagbits() & DcpMsgFlag.BAUD_MASK;
		if (br == DcpMsgFlag.BAUD_100 && !incl100)
			return false;
		if (br == DcpMsgFlag.BAUD_300 && !incl300)
			return false;
		if (br == DcpMsgFlag.BAUD_1200 && !incl1200)
			return false;
		
		if (crit.parityErrors == SearchCriteria.REJECT
		 && msgidx.getFailureCode() == '?')
			return false;
		if (crit.parityErrors == SearchCriteria.EXCLUSIVE
		 && msgidx.getFailureCode() == 'G')
			return false;

//Logger.instance().info("crit Passed all criteria tests");
		return true;  /* passed all tests. */
	}
	
	private void loadNetworkListFile(File f) throws IOException
	{
		NetworkList nl = new NetworkList(f); // Might throw IOException

		int i = 0;
		if (aggregateList == null)
			aggregateList = new DcpAddress[nl.size()];
		else
		{
			// Make new array starting with contents of old one.
			DcpAddress t[] = new DcpAddress[aggregateList.length + nl.size()];
			for(i = 0; i<aggregateList.length; ++i)
				t[i] = aggregateList[i];
			aggregateList = t;
		}
		// Note: i now is at the end of the old list.

		// Now add new list onto end of existing int array.
		int n = nl.size();
		for(int j=0; j<n; ++j)
			aggregateList[i++] = ((NetworkListItem)nl.elementAt(j)).addr;
//System.out.println("After loadNetworkListFile(" + f.getPath()
//+ "' size=" + n);
//for(int j=0; j<n; ++j)
// System.out.println("" + j + ": " + aggregateList[j]);
	}

	private void loadDcpAddresses() 
		throws SearchSyntaxException
	{
		if (crit.DcpNames.size() > 0 && mapper == null)
			throw new SearchSyntaxException(
				"No mapper to convert DCP names to addresses.", 
				LrgsErrorCode.DNONAMELIST);

		// temporary array big enough to store all possible addresses.
		DcpAddress tmp[] = new DcpAddress[
			3 + crit.DcpNames.size() + crit.ExplicitDcpAddrs.size()];
		int n = 0;

		if (crit.DomsatEmail == SearchCriteria.ACCEPT
		 || crit.DomsatEmail == SearchCriteria.YES)
			tmp[n++] = new DcpAddress(DcpAddress.ElecMailAddr);

		if (crit.GlobalBul == SearchCriteria.ACCEPT
		 || crit.GlobalBul == SearchCriteria.YES)
			tmp[n++] = new DcpAddress(DcpAddress.GlobalBulletinAddr);

		if (crit.DcpBul == SearchCriteria.ACCEPT
		 || crit.DcpBul == SearchCriteria.YES)
			tmp[n++] = new DcpAddress(DcpAddress.DcpBulletinAddr);

		for(int i = 0; i < crit.DcpNames.size(); i++)
		{
			String t = crit.DcpNames.get(i).toString();
//System.out.println("Attempting to map addr for " + t);
			DcpAddress addr = mapper.dcpNameToAddress(t);
//System.out.println("Mapped name '" + t + "' to address " + addr);
			if (addr == null)
				throw new SearchSyntaxException("Unrecognized DCP name '"
					+ t + "'", LrgsErrorCode.DBADDCPNAME);
			tmp[n++] = addr;
		}

		for(int i = 0; i < crit.ExplicitDcpAddrs.size(); i++)
			tmp[n++] = (DcpAddress)crit.ExplicitDcpAddrs.get(i);

		// Now add this vector to aggregateList and resort.
		int oldlen = 0;
		if (aggregateList == null)
			aggregateList = new DcpAddress[n];
		else if (n > 0)
		{
			oldlen = aggregateList.length;
			DcpAddress newagg[] = new DcpAddress[oldlen+n];
			for(int i=0; i<oldlen; i++)
				newagg[i] = aggregateList[i];
			aggregateList = newagg;
		}
		for(int i = 0; i < n; )
			aggregateList[oldlen++] = tmp[i++];
	}

	/**
	 * Finds a network list file in one of several possible directories.
	 * If the name is a complete path (i.e. starts with a separatorChar)
	 * then use complete path as specified. Then, if necessary, look in:
	 * <ul>
	 * <li>Sandbox directory for current 'username'
	 * <li>DCP Data User directory for current username.
	 * <li>~lrgs/netlist
	 * <li>~lrgs/netlist/remote
	 * </ul>
	 * @param name the file name
	 * @return File object if a file was found, null if not.
	 */
	File findNetworkListFile(String name)
	{
		Logger.instance().debug3("Looking for list '" + name + "'");
		if (name == null || name.length() == 0)
			return null;

		String path, exp;
		File f;

		// If it is a complete path, just make sure file exists.
		if (name.charAt(0) == '~'
		 || name.indexOf('/') != -1 || name.indexOf('\\') != -1)
		{
			exp = EnvExpander.expand(name);
			Logger.instance().debug3("Looking at Explicit list '" + exp + "'");
			f = new File(exp);
			if (f.isFile())
				return f;
			else
			{
				/*
				  Path name may have been from remote system and not applicable
				  to this server. Strip off the path, and attempt to find the
				  file in the usual places.
				*/
				int idx = name.lastIndexOf('/');
				if (idx != -1)
					name = name.substring(idx+1);
				else if ((idx = name.lastIndexOf('\\')) != -1)
					name = name.substring(idx+1);
				else
					return null;
			}
		}

		// Look in user' directory under the Root for LDDS users 
		// specified in the config file:
		if (userSandbox != null)
		{
			f = new File(userSandbox, name);
			Logger.instance().debug3("Looking for user list '" 
				+ f.getPath() + "'");
			if (f.isFile())
				return f;

			// Try with the '.nl' extension
			f = new File(userSandbox, name + ".nl");
			Logger.instance().debug3("Looking for user list '" 
				+ f.getPath() + "'");
			if (f.isFile())
				return f;
		}

		// Look in shared netlist directory
		path = LrgsConfig.instance().ddsNetlistDir + "/" + name;
		exp = EnvExpander.expand(path);
		Logger.instance().debug3("Looking for shared list '" + exp + "'");
		f = new File(exp);
		if (f.isFile())
			return f;
		// Try with the '.nl' extension
		exp = exp + ".nl";
		f = new File(exp);
		Logger.instance().debug3("Looking for shared list '" + exp + "'");
		if (f.isFile())
			return f;

		// Look in ~lrgs/netlist
		path = "~" + File.separatorChar + "netlist" + File.separatorChar + name;
		exp = EnvExpander.expand(path);
		Logger.instance().debug3("Looking for shared list '" + exp + "'");
		f = new File(exp);
		if (f.isFile())
			return f;

		// Look in ~lrgs/netlist/remote
		path = "~" + File.separatorChar + "netlist" + File.separatorChar +
			"remote" + File.separatorChar + name;
		exp = EnvExpander.expand(path);
		Logger.instance().debug3("Looking for home list '" + exp + "'");
		f = new File(exp);
		if (f.isFile())
			return f;

		return null;
	}

	public int getAggregateListLength()
	{
		if (aggregateList == null)
			return 0;
		else
			return aggregateList.length;
	}
	
	public SearchCriteria getCrit() { return crit; }
}
