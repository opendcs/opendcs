/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2006/09/28 19:15:33  mmaloney
*  Javadoc cleanup
*
*  Revision 1.3  2005/06/23 15:47:17  mjmaloney
*  Java archive search algorithms.
*
*  Revision 1.2  2005/03/07 21:35:24  mjmaloney
*  dev
*
*  Revision 1.1  1999/09/27 20:17:39  mike
*  9/27/1999
*
*
*/
package lrgs.common;

import java.util.Date;

public interface DcpMsgSource
{
	/**
	 * Attaches & initializes the archive for a new client connection.
	 * @throws ArchiveUnavailableException on any error.
	 */
	public void attachSource()
		throws ArchiveUnavailableException;

	/**
	 * Sets the client name for this connection. This name is used in
	 * subsequent log messages, status displays, etc.
	 * @param name the name
	 */
	public void setClientName(String name);

	/**
	  Sets process info for this client. This will show up on
	  the rtstat displays.
	  @param proctype process type
	  @param user user name
	*/
	public void setProcInfo(String proctype, String user)
		throws ArchiveUnavailableException;

	/**
	  Get the next DCP message index in the archive. 
	  The index data is placed in the passed DcpMsgIndex
	  object.

	  @return index number, that is, the index of the index (>=0)
	
	  @throws ArchiveUnavailableException if not attached, LRGS is down,
	   or other IO error.
	  @throws UntilReachedException if all messages within the specified
	   timerange have already been returned.
	  @throws EndOfArchiveException if no more indexes in the archive, i.e.
	   this client is already at the end.
	*/
	public int getNextIndex(DcpMsgIndex idx) 
		throws ArchiveUnavailableException, UntilReachedException,
			EndOfArchiveException;

	/**
	  Given an index, read the actual message data. Throw exception if
	  unable to read the message.

	  @return new DcpMsg object if success.
	  @throws NoSuchMessageException if there is no such message in
	  storage (i.e. the passed DcpMsgIndex is no longer valid).
	  @throws ArchiveUnavailableException if the LRGS down or some other
	  internal error occurs.
	*/
	public DcpMsg readMsgFromSource(DcpMsgIndex idx) 
		throws ArchiveUnavailableException, NoSuchMessageException;

	/**
	  This function will do a binary search of the message index to find
	  the first message with a time >= the passed time. The next call to
	  getNextIndex will return this index.

	  Call with null to cancel any previous since time set.

	  @return true if at least 1 message exists in storage with a time that
	    is >= the passed time. Otherwise false.

	  @throws ArchiveUnavailableException if LRGS is down or other internal
	  error occurs.
	*/
	public boolean setSourceLrgsSinceTime(Date t) 
		throws ArchiveUnavailableException;

	/**
	  Use the last-index-file to determine the last message accessed.
	  Set the internal state such that the next index returned by
	  getNextIndex will be the message immediately following it.

	  @return true if at last was successfully set, false if either
	  there was no last file or the last index is invalid. In this
	  case retrieval will proceed from the oldes message currently
	  in storage.

	  @throws ArchiveUnavailableException if not attached to the LRGS or if
	  the LRGS is not currently running.
	*/
	public boolean setSourceLrgsSinceLast() throws ArchiveUnavailableException;

	/**
	  Set the end-time for message retrieval. No messages with a time
	  greater than the passed time will be returned by getNextIndex.

	  Call with null to cancel any previous until time set.
	  @throws ArchiveUnavailableException if not attached to the LRGS or if
	  the LRGS is not currently running.
	*/
	public void setSourceLrgsUntilTime(Date t) throws ArchiveUnavailableException;

	/**
	  Set option for saving last index.
	  @param fname name of file to save last index in, must be either 
	  SaveLastNever or SaveLastOnGetIndex.
	*/
	public void setSaveLast(String fname, int option);

	/** Argument to setSaveLast - last index is never saved. */
	public static final int SaveLastNever = 0;

	/** Argument to setSaveLast - last index is saved for each index returned
	    by getNexIndex.
	*/
	public static final int SaveLastOnGetIndex = 1;

	/**
	  Set this process's status.
	  @param stat the status string
	*/
	public void setStatus(String stat) 
		throws ArchiveUnavailableException;

	/**
	  Detach from the archive & discard any resources allocated for this
	  connection. All errors are handled silently.
	*/
	public void detachSource();
}
