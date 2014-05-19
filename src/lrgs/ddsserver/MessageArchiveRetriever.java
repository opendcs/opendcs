/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.ddsserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ilex.util.IDateFormat;
import ilex.util.Logger;
import lrgs.apistatus.AttachedProcess;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.SearchSyntaxException;
import lrgs.common.SearchTimeoutException;
import lrgs.common.UntilReachedException;
import lrgs.common.EndOfArchiveException;
import lrgs.common.DcpMsgRetriever;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpMsg;
import lrgs.common.DcpAddress;
import lrgs.common.DcpNameMapper;
import lrgs.common.DcpMsgSource;
import lrgs.common.LrgsErrorCode;
import lrgs.common.SearchCriteria;
import lrgs.archive.MsgArchive;
import lrgs.archive.MsgFilter;
import lrgs.archive.SearchHandle;

/**
This class acts both as the retriever and the source for the new Java-Only-
Archive.
*/
public class MessageArchiveRetriever 
	extends DcpMsgRetriever 
	implements MsgFilter, DcpMsgSource
{
	private SearchHandle searchHandle;

	/** LrgsSince or null if none specified. */
	private Date LrgsSince;

	/** The LRGS until time. */
	private Date LrgsUntil;

	/** Name of attached client. */
	private String clientName;

	/** The archive from which to serve data. */
	private MsgArchive msgArchive;

	/** the status structure */
	private AttachedProcess attachedProcess;

	/** The LRGS-timestamp of the last message retrieved by this user. */
	private int lastMsgDrotTime;

	/** Path name of file to save last time in. */
	private String lastPath;

	/** If true, then force the legacy slower ascending only retrieval. */
	private boolean _forceAscending = false;
	
	/** The DDS Version of the client on the other end. */
	private int ddsVersionNum = 5;
	private String ddsVersion = "" + ddsVersionNum;
	
	/**
	 * Constructor.
	 */
	public MessageArchiveRetriever(MsgArchive msgArchive, AttachedProcess ap)
	{
		super();
		searchHandle = null;
		LrgsSince = null;
		LrgsUntil = null;
		this.msgArchive = msgArchive;
		attachedProcess = ap;
		attachedProcess.lastSeqNum = 0;
		lastMsgDrotTime = 0;
		lastPath = null;
		_forceAscending = false;
	}
	
	/**
	 * call 'testCriteria' from the base class.
	 */
	public boolean passes( DcpMsgIndex mie )
	{
		// Our init search gets us to the correct minute. But we still
		// have to check the send if an LRGS Since time was specified.
		if (LrgsSince != null
		 && mie.getLocalRecvTime().compareTo(LrgsSince) < 0)
			return false;
		if (LrgsUntil != null
		 && mie.getLocalRecvTime().compareTo(LrgsUntil) > 0)
			return false;
		
		// We must not deliver Iridium messages to pre-version-9 clients!
		if (ddsVersionNum < 10 
		 && DcpMsgFlag.isIridium(mie.getFlagbits()))
			return false;

		return testCriteria(mie);
	}
	
	/**
	 * If only DapsSince is specified use it, assume DapsTime <= LrgsTime.
	 * DapsSince is a Date attribute in the base class.
	 * @return since time for initializing the archive's retrieval.
	 */
	public Date getSinceTime( )
	{
		if (LrgsSince != null)
			return LrgsSince;
		return DapsSince;
	}
	
	/**
	 * If LrgsUntil is specified use it, else add a fudge factor to DapsUntil.
	 * Assumes LrgsTime is not too much later than DapsTime (e.g. 90 seconds).
	 * @return until time for terminating archive's retrieval.
	 */
	public Date getUntilTime( )
	{
		if (LrgsUntil != null)
			return LrgsUntil;
		else if (DapsUntil != null)
			return new Date(DapsUntil.getTime() + 90000L);
		return null;
	}

	/**
	 * The index should already contain the message, just return it.
	 */
	public DcpMsg readMsg( DcpMsgIndex idx )
	{
		lastMsgDrotTime = (int)(idx.getLocalRecvTime().getTime()/1000L);
		return idx.getDcpMsg();
	}
	
	/**
	 * Call super.init() to read netlists, addresses, & time ranges.
	 * Get searchHandle from Message Archive.
	 */
	public void init( )
		throws IOException, SearchSyntaxException, ArchiveUnavailableException
	{
		super.init();
		searchHandle = msgArchive.startSearch(this);
	}

	/**
	 * Template method to set explicit LRGS Until time.
	 * @param until the since time.
	 */
	protected void setLrgsUntilTime(Date until)
	{
		LrgsUntil = until;
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
		// If there's a next index in searchHandle.idxBuf, return it.
		DcpMsgIndex ret = searchHandle.getNextIndex();
		if (ret != null)
		{
			idx.copyFrom(ret);
			attachedProcess.lastMsgTime = (int)(idx.getXmitTime().getTime()/1000L);
			attachedProcess.lastSeqNum++;
			return 1;
		}
		else if (System.currentTimeMillis() > stopSearchMsec)
			throw new SearchTimeoutException("Search Time Expired");

		Logger.instance().debug3("Starting search...");
		int result = msgArchive.search(searchHandle, stopSearchMsec);
		Logger.instance().debug3("Search Result=" + result);
		if (result == MsgArchive.SEARCH_RESULT_MORE
		 || result == MsgArchive.SEARCH_RESULT_DONE
		 || result == MsgArchive.SEARCH_RESULT_TIMELIMIT)
		{
			ret = searchHandle.getNextIndex();
			if (ret != null)
			{
				idx.copyFrom(ret);
				attachedProcess.lastMsgTime = (int)(idx.getXmitTime().getTime()/1000L);
				attachedProcess.lastSeqNum++;
				return 1;
			}
			else if (result == MsgArchive.SEARCH_RESULT_DONE)
				throw new UntilReachedException();
			else
				throw new SearchTimeoutException("Search Time Expired");
		}
		else if (result == MsgArchive.SEARCH_RESULT_PAUSE)
		{
			throw new EndOfArchiveException();
		}
		// Should never get here.
		throw new ArchiveUnavailableException(
			"Bad return from msgArchive.search: " + result,
			LrgsErrorCode.DDDSINTERNAL);
	}
	

	public DcpAddress[] getDcpAddresses()
	{
		return aggregateList;
	}

	/**
	* From DcpMsgSource interface, does nothing.
	*/
	public void attachSource()
	{
	}

	/**
	* From DcpMsgSource interface, does nothing.
	*/
	public void detachSource()
	{
		if (lastMsgDrotTime != 0 && lastPath != null)
		{
			File lastFile = new File(lastPath);
			try
			{
				FileWriter fw = new FileWriter(lastFile);
				fw.write(IDateFormat.time_t2string(lastMsgDrotTime) + "\n");
				fw.close();
			}
			catch(IOException ex)
			{
				Logger.instance().warning("Cannot save last-msg-time in '"
					+ lastPath + "': " + ex);
			}
			lastMsgDrotTime = 0;
		}
		attachedProcess.name = "";
		attachedProcess.pid = -1;
	}

	/**
	* Sets the client name for this connection. This name is used in
	* subsequent log messages, status displays, etc.
	* @param name the name
	*/
	public void setClientName(String name)
	{
		this.clientName = name;
		attachedProcess.name = name;
	}
	
	public String getClientName() { return clientName; }
                                                                                
	/**
	* From DcpMsgSource interface, does nothing.
	* @param proctype process type
	* @param user user name
	*/
	public void setProcInfo(String proctype, String user)
	{
		attachedProcess.type = proctype;
		attachedProcess.user = user;
	}
                                                                                
	/**
	* From DcpMsgSource interface, does nothing.
	*/
	public int getNextIndex(DcpMsgIndex idx)
	{
		return -1;
	}
 
	/**
	* From DcpMsgSource interface, does nothing.
	*/
	public DcpMsg readMsgFromSource(DcpMsgIndex idx)
	{
		return null;
	}

	/**
	* Sets the since time for subsequent message retrievals.
	*/
	public boolean setSourceLrgsSinceTime(Date since)
	{
		LrgsSince = since;
		return true;
	}

	/**
	* Sets the until time for subsequent message retrievals.
	*/
	public void setSourceLrgsUntilTime(Date until)
	{
		setLrgsUntilTime(until);
	}

	/**
	* From DcpMsgSource interface.
	*/
	public boolean setSourceLrgsSinceLast() 
	{
		File lastFile = new File(lastPath);
		try
		{
			BufferedReader fr = new BufferedReader(new FileReader(lastFile));
			String lds = fr.readLine().trim();
			fr.close();
			LrgsSince = IDateFormat.parseJulianDate(lds);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot read last-msg-time from '"
				+ lastPath + "': " + ex);
		}

		return true;
	}

	/**
	Set option for saving last index.
	The Java version always saves the file, but instead of containing the
	index in binary form, it just contains the ASCII date/time of the 
	LRGS time of the last message retrieved by this user. 
	The save is done when the user disconnects.
	@param path name of file to save last index in
	@param option must be either SaveLastNever or SaveLastOnGetIndex.
    */
    public void setSaveLast(String path, int option)
	{
		lastPath = path;
	}

	/**
	  Set this process's status.
	  @param stat the status string
	*/
	public void setStatus(String status)
	{
		attachedProcess.status = status;
	}
                                                                                

	/**
	 * This is part of the MsgFilter interface. It tells MsgArchive whether
	 * or not to force the legacy ascending-only (slower) retrieval method.
	 */
	public boolean forceAscending() 
	{
		return _forceAscending || crit.getAscendingTimeOnly();
	}

	/** Part of MsgFilter interface */
	public boolean realTimeSettlingDelay()
	{
		return crit.getRealtimeSettlingDelay();
	}

	/**
	 * Sets flag to force (or not force) the legacy ascending-only behavior.
	 */
	public void setForceAscending(boolean tf) { _forceAscending = tf; }
	
	public void setProtocolVersion(String version) 
	{
		int i=0;
		while(i<version.length() && Character.isDigit(version.charAt(i)))
			i++;
		try 
		{
			ddsVersionNum = Integer.parseInt(version.substring(0, i)); 
			this.ddsVersion = version;
		}
		catch(NumberFormatException ex)
		{
			Logger.instance().warning("Invalid DDS Version '" + version + "'");
		}
	}	
}
