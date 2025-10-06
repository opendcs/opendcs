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
package lrgs.archive;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.SearchTimeoutException;
import lrgs.common.LrgsErrorCode;

/**
An archive for a specific period.
*/
public class MsgPeriodArchive
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String module = "PerArch";

	/**
	 * time_t of start of period covered by this file.
	 * Each file is uniquely identified by a startTime.
	 */
	int startTime;

	/** String Date YYYY/MM/DD corresponding to startTime */
	String myname;

	/** Static date formatter used for date string. */
	static SimpleDateFormat startFmt =
		new SimpleDateFormat("yyyy/MM/dd");

	/** Static date formatter used for debug messages. */
	static SimpleDateFormat debugFmt =
		new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	static
	{
		startFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		debugFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Time duration of this file (1 day).
	 */
	public static final int periodDuration = 3600*24;

	private static final int INDEX_BUF_SIZE = 100;

	/** Object used to read/write the indexes */
	IndexFile indexFile;

	private MsgIndexMinute[] minuteIndex;

	public static final int MIN_PER_DAY = 60*24;

	/** Cache of DcpMsgIndex objects. */

	class Cache extends Vector<DcpMsgIndex>
	{
		private int startIndexNum;
		public static final int CACHE_MAX_SIZE = 25000; // About 2 hours worth
		public static final int CACHE_INCR = 5000;

		public Cache(int startIndexNum)
		{
			super();
			this.startIndexNum = startIndexNum;
		}

		public int getStartIndexNum() { return startIndexNum; }

		/** Remove entries up to but not including the specified index. */
		private void rmBefore(int idx)
		{
			removeRange(0,idx);
			startIndexNum += idx;
		}

		public synchronized boolean add(DcpMsgIndex idx)
		{
			if (size() >= CACHE_MAX_SIZE)
				rmBefore(CACHE_MAX_SIZE-CACHE_INCR);
			return super.add(idx);
		}

		public synchronized void clear() { super.clear(); }

		public synchronized DcpMsgIndex getByIndexNum(int idxNum)
		{
			if (idxNum >= startIndexNum
			 && idxNum < startIndexNum + size())
				return super.get(idxNum - startIndexNum);
			else
				return null;
		}
	};
	private Cache cache;


	/** Object used to read/write the messages */
	private MsgFile msgFile;

	/** true if this archive is the current (writable) archive. */
	private boolean isCurrent;

	private String rootPath;
	public static final String MSG_EXT =    ".msg";
	public static final String MINUTE_EXT = ".min";
	public static final String INDEX_EXT =  ".idx";
	public static final String IHASH_EXT = ".ihash";

	/**
	 * Constructor.
	 * Load MsgIndexMinute array from disk.
	 * Initialize indexBuf to an array of objects.
	 * The 'rootPath' argument determines the location and base name for the
	 * three files making up this period. The message file will have extension
	 * '.msg', the index file '.idx', and the minute file '.min'.
	 *
	 * @param rootPath the root path name for files for this period.
	 * @param startTime unique start time for this period (unix time_t)
	 * @param isCurrent if true, open files read/write. Else read-only.
	 */
	public MsgPeriodArchive(String rootPath, int startTime, boolean isCurrent)
		throws IOException
	{
		module = module + "(" + startFmt.format(new Date(startTime * 1000L)) + ")";
		log.info("New archive '{}' start time={}, isCurrent={}", rootPath, startTime, isCurrent);

		this.rootPath = rootPath;
		this.startTime = startTime;
		this.myname = "DayArchive(" +
			startFmt.format(new Date(startTime * 1000L)) + ")";
		this.isCurrent = isCurrent;

		// Make a MsgIndexMinute entry for each minute in this period.
		int numMinutes = periodDuration/60;
		minuteIndex = new MsgIndexMinute[numMinutes];
		for(int i=0; i<numMinutes; i++)
			minuteIndex[i] = new MsgIndexMinute();
		try
		{
			MinuteFile.load(rootPath + MINUTE_EXT, minuteIndex);
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot initialize minute index. Assuming new period.");
		}

		// Instantiate the read/write objects for the message & index files.
		try { indexFile = new IndexFile(rootPath + INDEX_EXT, isCurrent); }
		catch(IOException ioex)
		{
			throw new IOException("Cannot open index file ''" + rootPath + INDEX_EXT +"'", ioex);
		}

		try { msgFile = new MsgFile(new File(rootPath + MSG_EXT), isCurrent); }
		catch(IOException ioex)
		{
			throw new IOException(" Cannot open message file '" + rootPath + MSG_EXT + "'", ioex);
		}

		// The cache is used for recent entries for very fast retrieval.
		// It is a vector of DcpMsgIndex objects.
		cache = null;
		if (isCurrent)
		{
			cache = new Cache(indexFile.getNumEntries());
			log.debug(" Cache starting at index number {}", cache.getStartIndexNum());
		}
	}

	/**
	 * Archives the message in this period and either creates a new
	 * index or resets an existing index.
	 * @param prevPtr points to the previous index for this DCP.
	 * @return the index number (i.e. the index of the newly saved index.
	 */
	public synchronized int archiveMsg(DcpMsg msg, IndexPtr prevPtr)
	{
		// Archive msg & put it in the cache.
		int idxNum = 0;
		try
		{
			int loc = (int)msgFile.writeMsg(msg);

			DcpMsgIndex idx = new DcpMsgIndex(msg,
				prevPtr.indexFileStartTime, prevPtr.indexNumber, loc);
			idxNum = addIndex(idx);

			return idxNum;
		}
		catch(IOException ioex)
		{
			log.atWarn().setCause(ioex).log("{}- Could not add message to archive: ", MsgArchive.EVT_BAD_INDEX);
		}
		return -1;
	}

	/**
	 * Marks an existing message as 'deleted'. This is done when a better
	 * quality copy of the same message is archived.
	 * @param indexNum the number of the index of the message to delete.
	 */
	public synchronized void deleteMsg(int indexNum)
	{
		DcpMsgIndex idx = getIndexEntry(indexNum);
		if (idx == null)
		{
			log.warn("{}- deletemsg(indexNum={}) Could not read index.", MsgArchive.EVT_BAD_INDEX, indexNum);
			return;
		}

		idx.setFlagbits(idx.getFlagbits() | DcpMsgFlag.MSG_DELETED);
		try { indexFile.writeIndex(idx, indexNum); }
		catch(IOException ioex)
		{
			log.atWarn()
			   .setCause(ioex)
			   .log("{}- deletemsg(indexNum={}) Could not write deleted index.",
			   		MsgArchive.EVT_BAD_INDEX, indexNum);
			return;
		}

		if (idx.getDcpMsg() != null)
			idx.getDcpMsg().flagbits |= DcpMsgFlag.MSG_DELETED;
		try { msgFile.markMsgDeleted(idx.getOffset()); }
		catch(IOException ioex)
		{
			log.atWarn()
			   .setCause(ioex)
			   .log("{}- deletemsg(indexNum={}) Could not mark message deleted at offset={}",
			   		MsgArchive.EVT_BAD_INDEX, indexNum, idx.getOffset());
		}
	}

	public synchronized void addDomsatSequence(int indexNum, int seqNum,
		long domsatTime)
	{
		if (!isCurrent)
			return;

		DcpMsgIndex idx = null;
		try
		{
			idx = getIndexEntry(indexNum);
			if (idx == null)
				return;
			idx.setFlagbits(idx.getFlagbits() & (~DcpMsgFlag.MSG_NO_SEQNUM));
			idx.setSequenceNum(seqNum);
			indexFile.writeIndex(idx, indexNum);
		}
		catch(IOException ioex)
		{
			log.atWarn()
			   .setCause(ioex)
			   .log("{}- Index File for '{}' Could not add Domsat Seq# to index number {}",
			   		MsgArchive.EVT_BAD_INDEX, rootPath, indexNum);
		}
		try
		{
			DcpMsg dcpMsg = idx.getDcpMsg();
			if (dcpMsg != null)
			{
				dcpMsg.flagbits &= (~DcpMsgFlag.MSG_NO_SEQNUM);
				dcpMsg.setDomsatTime(new Date(domsatTime));
				dcpMsg.setSequenceNum(seqNum);
			}
			msgFile.addDomsatSequence(idx.getOffset(), seqNum, domsatTime);
		}
		catch(IOException ioex)
		{
			log.atWarn()
			   .setCause(ioex)
			   .log("{}- Msg File for '{}' Could not add Domsat Seq# to index number {}",
			   		MsgArchive.EVT_BAD_INDEX, rootPath, indexNum);
		}
	}

	/**
	 * Adds an index entry to the archive and cache.
	 * @param idx the index entry to add.
	 * @return the index number.
	 */
	private int addIndex( DcpMsgIndex idx )
	{
		int indexNum = getNumIndexes();
		try { indexFile.writeIndex(idx, indexNum); }
		catch(IOException ioex)
		{
			log.atError()
			   .setCause(ioex)
			   .log("{}- Cannot save index number {} index file will be corrupt: ",
			   		MsgArchive.EVT_BAD_INDEX, indexNum);
		}
		cache.add(idx);
		updateIndexMinute(indexNum,
			(int)(idx.getLocalRecvTime().getTime()/1000L),
			(int)(idx.getXmitTime().getTime()/1000L));
		return indexNum;
	}

	/**
	 * @returns the total number of indexes in this period archive.
	 */
	public int getNumIndexes()
	{
		if (cache != null)
			return cache.getStartIndexNum() + cache.size();
		else
			return indexFile.getNumEntries();
	}

	/**
	 * Determine the 'minute' from recvTime and retrieve the
	 * MsgIndexMinute from the array. If the new dapsTime &lt; the one
	 * in the entry, update and save.
	 * @param indexNum the new index number
	 * @param recvTime the unix time_t that this message was received at.
	 * @param dapsTime the DAPS (or platform) time.
	 */
	public void updateIndexMinute(int indexNum, int recvTime, int dapsTime )
	{
		int minuteNum = (recvTime - startTime) / 60;
		if (minuteNum < 0 || minuteNum >= MIN_PER_DAY)
			return;

		synchronized(minuteIndex)
		{
			if (minuteIndex[minuteNum].isEmpty()
			 || indexNum < minuteIndex[minuteNum].startIndexNum)
				minuteIndex[minuteNum].startIndexNum = indexNum;
			if (dapsTime < minuteIndex[minuteNum].oldestDapsTime)
				minuteIndex[minuteNum].oldestDapsTime = dapsTime;
		}
	}

	/**
	 * Called on the current archive when a new period is starting and
	 * this period is finished (i.e. end of day).
	 * Flushes the minute archive to disk.
	 * Re-opens index and message files read-only.
	 */
	public synchronized void finish( )
	{
		log.info("Archive {} is finished ...  re-opening read-only.", myname);
		try { MinuteFile.save(rootPath + MINUTE_EXT, minuteIndex); }
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot save minute index.");
		}
		indexFile.close();
		indexFile = null;
		msgFile.close();
		msgFile = null;

		isCurrent = false;
		try { indexFile = new IndexFile(rootPath + INDEX_EXT, false); }
		catch(IOException ioex)
		{
			log.atWarn().setCause(ioex).log("Cannot open index file '{}{}'", rootPath, INDEX_EXT);
		}
		try { msgFile = new MsgFile(new File(rootPath + MSG_EXT), false); }
		catch(IOException ioex)
		{
			log.atWarn().setCause(ioex).log(" Cannot open message file '{}{}'", rootPath, MSG_EXT);
		}
	}

	/**
	 * Called prior to shutdown to close all files and release resources.
	 */
	public void close()
	{
		indexFile.close();
		minuteIndex = null;
		cache = null;
		msgFile.close();
	}

	/**
	 * This method is called periodically to checkpoint critical info to disk
	 * and other periodic maintenance, like flushing old entries from the
	 * cache.
	 */
	public synchronized void checkpoint()
	{
		log.trace("MsgPeriodArchive.checkpoint()");
		if (isCurrent)
		{
			try { MinuteFile.save(rootPath + MINUTE_EXT, minuteIndex); }
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("Cannot checkpoint minute index.");
			}
		}
		if (cache != null)
		{
			// Keep this cache until an hour after the day is over.
			if (!isCurrent
			 && cache != null
			 && System.currentTimeMillis()/1000L >
				startTime + periodDuration + 3600L)
			{
				// More than 1 hr past end of period: no more caching.
				cache.clear();
				cache = null;
			}
		}
	}

	/**
	  Called when this period has fallen off the archive because it's more
	  then the max number of days old. Deletes the files from the disk.
	*/
	public void delete()
	{
		log.info("Deleting Day Archive '{}'", rootPath);
		close();
		File f = new File(rootPath + MSG_EXT);
		if (f.exists())
			f.delete();
		f = new File(rootPath + MINUTE_EXT);
		if (f.exists())
			f.delete();
		f = new File(rootPath + INDEX_EXT);
		if (f.exists())
			f.delete();
		f = new File(rootPath + IHASH_EXT);
		if (f.exists())
			f.delete();
	}

	/**
	 * Initialize an Index search. Use the since time to calculate the earliest
	 * minute that can contain a message. Get it's startIndexNum from
	 * the MsgIndexMinute array.
	 */
	public void startIndexSearch( SearchHandle handle )
	{
		Date d = handle.filter.getSinceTime();
		if (d == null)
		{
			log.debug("{} startIndexSearch with null since time.", myname);
			handle.nextIndexNum = 0;
		}
		else
		{
			int tt = (int)(d.getTime() / 1000L);
			handle.minIdx = (tt - startTime) / 60;
			if (handle.minIdx < 0)
				handle.minIdx = 0;
			else if (handle.minIdx >= MIN_PER_DAY)
				handle.minIdx = MIN_PER_DAY-1;

			// There may not be any data in the specified minute.
			// Scroll forward until we find some.
			handle.nextIndexNum = minuteIndex[handle.minIdx].startIndexNum;
			while(handle.nextIndexNum == -1 && handle.minIdx < MIN_PER_DAY-1)
			{
				handle.nextIndexNum =
					minuteIndex[++handle.minIdx].startIndexNum;
			}
			if (handle.nextIndexNum == -1 && handle.minIdx == MIN_PER_DAY-1)
			{
				// Fell through -- no indexes this day. Set to # indexes which
				// will cause searchIndex to cycle to the next period.
				log.debug("No indexes since specified time. Will drop through to next day.");
				handle.nextIndexNum = getNumIndexes();
			}

			log.debug("{} startIndexSearch sincetime={}, minIdx={}, start idx={}, num indexes in file={}",
					  myname, d, handle.minIdx, handle.nextIndexNum, getNumIndexes());

		}
		handle.flushBuffer();
		handle.methodVar = null; // Not used for this type of search
	}

	/**
	 * Efficiently read a bunch of indexes from this archive, delegated
	 * from MsgArchive. This method handles the forward index-file search
	 * algorithm.
	 * @param handle the search handle
	 * @param stopSearchMsec time to stop searching.
	 * @return one of the SEARCH_RESULT codes defined in MsgArchive.
	 * @see lrgs.archive.MsgArchive.search(SearchHandle handle)
	 */
	public synchronized int searchIndex(SearchHandle handle, long stopSearchMsec)
		throws ArchiveUnavailableException, SearchTimeoutException
	{
		int numIndexes = getNumIndexes();
		Date untilD = handle.filter.getUntilTime();
		int untilTt = Integer.MAX_VALUE;
		if (untilD != null)
			untilTt = (int)(untilD.getTime() / 1000L);
		int numAdded = 0;
		int skipped = 0;

		DcpMsgIndex indexBuf[] = new DcpMsgIndex[INDEX_BUF_SIZE];
		for(int i=0;i<INDEX_BUF_SIZE; i++) indexBuf[i] = null;

		boolean settlingDelayHit = false;

	  retrieve_loop:
		while(handle.nextIndexNum < numIndexes        // indexes left in file
		   && handle.capacity() > 0                   // room for more in handle
		   && System.currentTimeMillis() < stopSearchMsec) // haven't timed out
		{
			// Is this the start of a new minute?
			int minuteEndIndexNum = getMinuteEndIndexNum(handle.minIdx);

			if (handle.minIdx < MIN_PER_DAY-1
			 && handle.nextIndexNum > minuteEndIndexNum)
			{
				// Now in a new minute!

				// We're now either at EOF or in a new minute. Set handle next:
				if (handle.minIdx < MIN_PER_DAY)
				{
					handle.nextIndexNum = minuteIndex[handle.minIdx].startIndexNum;
					minuteEndIndexNum = getMinuteEndIndexNum(handle.minIdx);
				}
				else // fell off end of file.
				{
					handle.nextIndexNum = indexFile.getNumEntries();
					minuteEndIndexNum = handle.nextIndexNum;
				}
			}

			// Read a buffer's worth or to the end of this minute.
			int n = (minuteEndIndexNum+1) - handle.nextIndexNum;
			if (handle.nextIndexNum == indexFile.getNumEntries())
				n = 0;
			else if (n > INDEX_BUF_SIZE)
	 			n = INDEX_BUF_SIZE;
			int nr = 0;
			try
			{
				nr = indexFile.readIndexes(handle.nextIndexNum, n, indexBuf);
			}
			catch(IOException ex)
			{
				String msg = myname + " Corrupt index file.";
				log.atError().setCause(ex).log("{}- {}", MsgArchive.EVT_BAD_INDEX, msg);
				throw new ArchiveUnavailableException(msg, LrgsErrorCode.DARCFILEIO, ex);
			}
			long now = System.currentTimeMillis();
			for(int i = 0; i<nr && handle.capacity() > 0; i++)
			{
				DcpMsgIndex dmi = indexBuf[i];

				// If this is a RealTime retrieval, and 'settlingTime'
				// is selected and this msg rcv-time is within 30 seconds
				// of now, then break;
				if (dmi != null
				 && untilTt == Integer.MAX_VALUE
				 && handle.filter.realTimeSettlingDelay()
				 && (dmi.getLocalRecvTime().getTime() > now-30000L))
				{
					settlingDelayHit = true;
					break retrieve_loop;
				}

				// We are going to process this message.
				handle.nextIndexNum++;
				indexBuf[i] = null;

				if (dmi != null
				 && (dmi.getFlagbits() & DcpMsgFlag.MSG_DELETED) == 0
				 && handle.filter.passes(dmi)
				 && dmi.getOffset() >= 0L)
				{
					// Passes criteria -- read the message & add to handle buf.
					try
					{
						dmi.setDcpMsg(msgFile.readMsg(dmi.getOffset()));
						handle.addIndex(dmi);
						numAdded++;
					}
					catch(IOException ex)
					{ // shouldn't happen unless someone deleted msg file.
						log.atError()
						   .setCause(ex)
						   .log("{} Corrupt index or message file idxnum={}", myname,  (handle.nextIndexNum-1));
						continue;
					}
				}
			}
		}

		// Four possibilities for breaking out of loop:
		//   1. Handle's buffer is full (capacity == 0)
		//   2. Out of time & must return whatever results I have now.
		//   3. Real-Time Settling delay hit
		//   4. I hit the end of this archive.

		// Fell through: Either handle-full or End of this archive reached.
		if (handle.capacity() == 0)
		{
			return MsgArchive.SEARCH_RESULT_MORE;
		}

		// Out of time & must return results to client.
		if (System.currentTimeMillis() >= stopSearchMsec)
		{
			return MsgArchive.SEARCH_RESULT_TIMELIMIT;
		}

		if (settlingDelayHit)
		{
			// No until specified -- tell caller to pause before trying again.
			return MsgArchive.SEARCH_RESULT_PAUSE;
		}

		// Otherwise, I hit the end of this file.

		// If I'm not the current archive, jump to the next archive.
		if (!isCurrent)
		{
			handle.periodStartTime = startTime + periodDuration;
			handle.nextIndexNum = 0;
			handle.minIdx = 0;

			return MsgArchive.SEARCH_RESULT_MORE;
		}

		// Otherwise, end of current archive.
		//if (handle.isEmpty())
		if (numAdded == 0)
		{
			if (untilTt == Integer.MAX_VALUE)
			{
				// No until specified -- tell caller to pause before trying again.
				return MsgArchive.SEARCH_RESULT_PAUSE;
			}
			else
			{
				return MsgArchive.SEARCH_RESULT_DONE;
			}
		}
		else
		{
			return MsgArchive.SEARCH_RESULT_MORE;
		}

/*
TODO:
1. Incorporate cache.
2. Incorporate Lrgs UNTIL time cutting off a search.
3. Above algorithm doesn't allow UNTIL time in future. Fix this.
4. Handle IO exceptions from index & msg file.
*/
	}

	/**
	 * Public synchronized version for call from outside the package. The actual
	 * method is not synchronized because it's called internally within other
	 * synchronized blocks.
	 */
	public synchronized DcpMsgIndex getIndexEntrySync(int idxNum)
	{
		return getIndexEntry(idxNum);
	}

	/**
	 * Returns the index from the number. If currently in the cache, just
	 * return the reference. Else, read it from disk, construct, & return it.
	 * @return the index entry
	 */
	private DcpMsgIndex getIndexEntry( int idxNum)
	{
		if (idxNum < 0)
			return null;

		DcpMsgIndex dmi = (DcpMsgIndex)null;
		if (cache != null
		 && (dmi = cache.getByIndexNum(idxNum)) != null)
		{
			return dmi;
		}
		else
		{
			DcpMsgIndex ret[] = new DcpMsgIndex[1];
			ret[0] = new DcpMsgIndex();
			try
			{
				int nr = indexFile.readIndexes(idxNum, 1, ret);
				if (nr == 0)
					return null;
				return ret[0];
			}
			catch(IOException ex)
			{
				log.atWarn().setCause(ex).log("Archive {} error reading index {}", myname, idxNum);
				return null;
			}
		}
	}

	/**
	 * Reads the message corresponding to the index, and places it inside
	 * the index data structure.
	 * @param dmi the index
	 * @throws ArchiveUnavailableException if error reading message file.
	 */
	public void readMessage(DcpMsgIndex dmi)
		throws ArchiveUnavailableException
	{
		if (dmi.getDcpMsg() != null)
			return;

		try { dmi.setDcpMsg(msgFile.readMsg(dmi.getOffset())); }
		catch(IOException ex)
		{
			throw new ArchiveUnavailableException(
				"Corrupt archive " + myname
				+ " cannot read message for existing index at offset "
				+ dmi.getOffset(), LrgsErrorCode.DARCFILEIO, ex);
		}
	}

	/**
	 * Internal method to get the end index number for the specified minute.
	 * This is normally the start of the next index minus one. But this method
	 * accounts for the last minute of the day, and the case where subsequent
	 * minutes don't have any data.
	 * @return end index for specified minute, or -1 if index file is empty.
	 */
	private int getMinuteEndIndexNum(int min)
	{
		int nextMin = min+1;
		for(; nextMin<MIN_PER_DAY && minuteIndex[nextMin].isEmpty(); nextMin++);
		if (nextMin >= MIN_PER_DAY)
			return indexFile.getNumEntries() - 1;
		else
			return minuteIndex[nextMin].startIndexNum - 1;
	}

	public String getRootPath() { return rootPath; }
}