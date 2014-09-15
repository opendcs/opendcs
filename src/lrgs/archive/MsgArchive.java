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
package lrgs.archive;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TimeZone;

import decodes.util.ChannelMap;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpAddress;
import lrgs.common.LrgsErrorCode;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.SearchTimeoutException;

import lrgs.lrgsmain.JavaLrgsStatusProvider;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;


/**
Top-level archive for the DCS Toolkit.
*/
public class MsgArchive
	implements MsgValidatee
{
	/** Used for log messages. */
	public static final String module = "Archive";

	/** Event Num for bad index file. */
	public static final int EVT_BAD_INDEX = 1;
	
	/** Event Num for bad minute file. */
	public static final int EVT_BAD_MINUTE_FILE = 2;

	/** Event Num for bad archive. */
	public static final int EVT_BAD_ARCHIVE = 3;

	/** Event Num for hash file. */
	public static final int EVT_BAD_HASH = 4;

	/** The directory in which archive files are saved/found.  */
	private File archiveDir;

	/** Each archive file will start with this name. */
	public static final String namePrefix = "arc-";
	public static final String lrgs6namePrefix = "arch-";
	public static final String lrgs7namePrefix = "archv-";

	/** Used to format & parse the date suffixes for archive files. */
	public static SimpleDateFormat nameDateFormat
		= new SimpleDateFormat("yyyyMMdd");
	static
	{
		nameDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/** Number of periods to keep (1 period = 1 index file).  */
	private int numPeriods;
	
	/** IndexPtr entries hashed by DCP address.  */
	IndexPtrHash lastMsgHash = new IndexPtrHash();
	
	/** time_t that the last message archive was called.  */
//	private int lastArchiveSec;
	
	/** Vector of MsgPeriodArchive objects.  */
	private PeriodArchiveVec periodArchives;

	/** The current period archive being written to.  */
	private MsgPeriodArchive currentArchive;
	
	/** Used to periodically save hash, and cleanup cache. */
	private CheckpointThread checkpointThread;

	/** Merge Filter */
	private MergeFilter mergeFilter;

	/** 
	 * Return value to search() when search is complete. 
	 * This means that no indexes have been returned and no further calls should
	 * be made.
	 */
	public static final int SEARCH_RESULT_DONE = 1;

	/** 
	 * Return value to search() meaning to keep calling for more data. 
	 * This means one or more indexes have been returned. When these
	 * are processed, call again for more.
	 */
	public static final int SEARCH_RESULT_MORE = 2;

	/** 
	 * Special case for real-time retrieval (no until time). No messages
	 * have been returned, and caller should pause before trying again.
	 */
	public static final int SEARCH_RESULT_PAUSE = 3;
	
	/**
	 * The 45 second timeout was reached before any messages were found.
	 */
	public static final int SEARCH_RESULT_TIMELIMIT = 4;

	/** We report the archive status to the status-provicer.  */
	private JavaLrgsStatusProvider statusProvider;

	private DailyDomsatSeqMap todaySeqMap;
	private DailyDomsatSeqMap yesterdaySeqMap;
	private static final int SPD = 3600*24;
	
	private MsgValidator validator = null;
	
	private HashMap<DcpAddress, Date> lastRealGoesTime = new HashMap<DcpAddress, Date>();

	/**
	 * Constructor -- set defaults.
	 * @param archiveDirName name of acrhive directory (may contain $env-var)
	 */
	public MsgArchive(String archiveDirName)
	{
		archiveDir = new File(EnvExpander.expand(archiveDirName));
		setPeriodParams(31);		// default is 31 day-files
		periodArchives = new PeriodArchiveVec();
		currentArchive = null;
		mergeFilter = new MergeFilter(this);
		statusProvider = null;
		todaySeqMap = null;
		yesterdaySeqMap = null;
	}

	/**
	 * Sets number of periods & period duration
	 * @param numPeriods number of periods (default = 31).
	 */
	public void setPeriodParams(int numPeriods)
	{
		this.numPeriods = numPeriods;
	}

	/**
	 * Sets the status provider.
	 * @param sp the status provider.
	 */
	public void setStatusProvider(JavaLrgsStatusProvider sp)
	{
		statusProvider = sp;
	}

	/**
	 * Called once after creation, and after loading configuration.
	 * Reload IndexPtr hashmap from disk.
	 * Construct a MsgPeriodArchive for each of numPeriods.
	 */
	public synchronized void init( )
		throws InvalidArchiveException
	{
		Logger.instance().debug1(module + " MsgArchive.init()");

		if (!archiveDir.isDirectory())
			if (!archiveDir.mkdirs())
				throw new InvalidArchiveException(
					"Archive Directory '" + archiveDir.getPath()
					+ "' does not exist and cannot be created.");

		String files[] = archiveDir.list();
		if (files == null) // means archiveDir is not a directory
			throw new InvalidArchiveException("Archive Directory '" 
				+ archiveDir.getPath() + "' cannot be listed.");

		int now = (int)(System.currentTimeMillis() / 1000L);
		for(int i=0; i<files.length; i++)
		{
			int npl = 0;
			if (files[i].startsWith(namePrefix))
				npl = namePrefix.length();
			else if (files[i].startsWith(lrgs6namePrefix))
				npl = lrgs6namePrefix.length();
			else if (files[i].startsWith(lrgs7namePrefix))
				npl = lrgs7namePrefix.length();
			else
				continue;

			if (files[i].endsWith(MsgPeriodArchive.MSG_EXT)
			 && files[i].length() == 
				npl + 8 + MsgPeriodArchive.MSG_EXT.length())
			{
				int startTime = 0;
				try
				{
					Date d=nameDateFormat.parse(files[i].substring(npl,npl+8));
					startTime = (int)(d.getTime() / 1000L);
				}
				catch(ParseException pex)
				{
					Logger.instance().warning(module + " File '" + files[i] 
						+ "' contains a date field that could not be parsed: "
						+ pex);
					continue;
				}
				String arcRootPath = archiveDir.getPath() + File.separator 
					+ files[i].substring(0, npl+8);
				Logger.instance().debug1(module 
					+ " Initializing archive with root '" + arcRootPath + "'");
				boolean isCurrent = 
					now - startTime < MsgPeriodArchive.periodDuration;
				try
				{
					MsgPeriodArchive arc=periodArchives.findByStart(startTime);
					if (arc != null)
					{
						Logger.instance().warning(module 
							+ " Archive with start time=" + startTime
							+ " already exists. Ignoring '"
							+ arcRootPath + "'");
						continue;
					}
			
					arc = new MsgPeriodArchive(arcRootPath, startTime, isCurrent);
					periodArchives.insert(arc);
					if (isCurrent)
					{
						Logger.instance().debug1(module + " Current archive is "
							+ arcRootPath);
						currentArchive = arc;
					}
				}
				catch(IOException ioex)
				{
					Logger.instance().failure(module + ":" + EVT_BAD_ARCHIVE
						+ "- Could not create archive for '"
						+ arcRootPath + "': " + ioex);
				}
			}
			else
				Logger.instance().debug1(module 
					+ " MsgArchive.init() skipping '" + files[i] + "'");
		}

		// Read the hash table into memory
		lastMsgHash.loadIndexPtrHash(archiveDir, "index-hash");

		// System may not have been shut-down cleanly and index-ptr hash may
		// not have been properly checkpointed. This thread will go through
		// the most recent archive and update the hash.
		if (currentArchive != null)
		{
			Logger.instance().info(module + " scanning current index.");
			DcpMsgIndex ibuf[] = new DcpMsgIndex[1024];
			for(int i=0; i<ibuf.length; i++)
				ibuf[i] = null;
			int idxnum = 0;
			int nr = 0;
			int curStart = currentArchive.startTime;
			try
			{
				int lastMsgPosition = 0;
				while( (nr = currentArchive.indexFile.readIndexes(
					idxnum, ibuf.length, ibuf)) > 0)
				{
					for(int i=0; i<nr; i++)
					{
						DcpAddress daddr = ibuf[i].getDcpAddress();
						IndexPtr lastPtr = lastMsgHash.get(daddr);
//if (daddr.toString().startsWith("30003401"))
//Logger.instance().info("MsgArchive.init indexNum " + (idxnum+i) + " addr=" 
//	+ daddr.toString()
//	+ " lastPtr=" + (lastPtr == null ? "null" : lastPtr.toString()));
						if (lastPtr == null
						 || lastPtr.indexFileStartTime < curStart
						 || (lastPtr.indexFileStartTime == curStart
							 && lastPtr.indexNumber < idxnum+i))
						{
							// We have a later one than is in the hash.
							lastPtr = new IndexPtr(curStart, idxnum+i,
								(int)(ibuf[i].getLocalRecvTime().getTime()/1000L),
								ibuf[i].getFlagbits(), 10000);
				
							lastMsgHash.put(daddr, lastPtr);
						}
						
						char fc = ibuf[i].getFailureCode();
						if ((ibuf[i].getFlagbits() & DcpMsgFlag.MSG_TYPE_MASK)
							== DcpMsgFlag.MSG_TYPE_GOES
						 && (fc == '?' || fc == 'G'))
						{
							Date xmitTime = ibuf[i].getXmitTime();
							Date z = lastRealGoesTime.get(ibuf[i].getDcpAddress());
							if (z == null || xmitTime.after(z))
								lastRealGoesTime.put(ibuf[i].getDcpAddress(), xmitTime);
						}
					}
					if (nr != ibuf.length)
						break;
					idxnum += nr;
					Logger.instance().debug1(module + " read " + idxnum + " indexes.");
				}
			}
			catch(IOException ex)
			{
				Logger.instance().warning(module + " IO Error scanning index: "+ex);
			}
			Logger.instance().info(module + " scan complete, " + idxnum + " indexes read.");
		}

		if (LrgsConfig.instance().getDoPdtValidation())
		{
			Logger.instance().info(module + " Loading PDT.");
			Pdt.instance().startMaintenanceThread(LrgsConfig.instance().getPdtUrl(),
				EnvExpander.expand("$LRGSHOME/pdt"));
			long start = System.currentTimeMillis();
//			while(System.currentTimeMillis() - start < 60000L
//			 && !Pdt.instance().isLoaded());
//			if (!Pdt.instance().isLoaded())
//				Logger.instance().warning(module 
//					+ " PDT still not loaded after 60 seconds "
//					+ "-- proceeding without it.");
	
			Logger.instance().info(module + " Loading Channel Map.");
			ChannelMap.instance().startMaintenanceThread(
				LrgsConfig.instance().getChannelMapUrl(),
				EnvExpander.expand("$LRGSHOME/cdt"));
			start = System.currentTimeMillis();
//			while(System.currentTimeMillis() - start < 10000L
//			 && !ChannelMap.instance().isLoaded());
//			if (!ChannelMap.instance().isLoaded())
//				Logger.instance().warning(module 
//					+ " ChannelMap still not loaded after 10 seconds "
//					+ "-- proceeding without it.");
		}		
		validator = new MsgValidator(this, Pdt.instance(), ChannelMap.instance());

		checkpointThread = new CheckpointThread(this);
		checkpointThread.start();
		
		if (LrgsConfig.instance().getDoPdtValidation())
			validator.startCheckMissingThread();
	}
	
	/**
	 * Saves IndexPtr to disk.
	 * Calls active MsgPeriodArchive's finish() method.
	 */
	public void shutdown( )
	{
		Pdt.instance().stopMaintenanceThread();
		ChannelMap.instance().stopMaintenanceThread();
		validator.shutdown();
		checkpointThread.shutdown = true;
		checkpoint();
		if (currentArchive != null)
			currentArchive.finish();
		currentArchive = null;
		periodArchives.closeAll();
	}

	/**
	 * Archive a DCP message.
	 * @param msg the message.
	 * @param src the input device that generated the msg.
	 * @return true if message was archived, false if it was discarded.
	 */
	public synchronized void archiveMsg( DcpMsg msg, LrgsInputInterface src)
	{
		// If this is an APR, and we're doing our own validation, discard it.
		if (LrgsConfig.instance().getDoPdtValidation())
		{
			if (msg.isDapsStatusMsg())
			{
				Logger.instance().debug2("Ignoring DAPS Status Msg from source "
					+ src.getInputName() + " with failure code '" 
					+ msg.getFailureCode() + "'");
				return;
			}
			if (msg.isGoesMessage())
			{
				ChannelMap cdt = ChannelMap.instance();
				boolean isRandom = cdt.isRandom(msg.getGoesChannel());
				msg.setFlagbits(
					(msg.getFlagbits() & (~DcpMsgFlag.MSG_TYPE_MASK))
					| (isRandom ? DcpMsgFlag.MSG_TYPE_GOES_RD : DcpMsgFlag.MSG_TYPE_GOES_ST));
			}
		}
		
		// Make sure I have a valid archive for the current time.
		Date now = new Date();
		checkCurrentArchive((int)(now.getTime()/1000L));
		if (!doArchiveMsg(msg, src, now))
			return;
		if (msg.isGoesMessage() && !msg.isDapsStatusMsg())
		{
			Date xmitTime = msg.getXmitTime();
			Date z = lastRealGoesTime.get(msg.getDcpAddress());
			if (z == null || xmitTime.after(z))
				lastRealGoesTime.put(msg.getDcpAddress(), xmitTime);
		}
		
		if (LrgsConfig.instance().getDoPdtValidation())
		{
			validator.validateMsg(msg, src, now);
		}
	}
	
	/** Callback for MsgValidator */
	public void useValidationResults(char failureCode, String explanation,
			DcpMsg msg, LrgsInputInterface src, Date t, PdtEntry pdtEntry)
	{
		if (failureCode == 'M' && msg.isGoesMessage())
		{
			Date too_old = new Date(
				System.currentTimeMillis() - 3600 * 48 * 1000L);
			DcpAddress dcpAddr = msg.getDcpAddress();
			Date z = lastRealGoesTime.get(dcpAddr);
			if (z != null && z.before(too_old))
			{
//				Logger.instance().debug1(
//					module + " DCP " + dcpAddr + " Missing squelched for inactive platform.");
				return;
			}
			else if (z == null)
			{
				
				IndexPtr lastPtr = lastMsgHash.get(dcpAddr);
	
				if (lastPtr == null)
				{
//					Logger.instance().debug1(
//						module + " DCP " + dcpAddr + " Missing squelched for never-seen platform.");
					lastRealGoesTime.put(dcpAddr, new Date(0L));
					return;
				}
	
				int fileStartTime = lastPtr.indexFileStartTime;
				int idxNum = lastPtr.indexNumber;
	
				// 48 hours ago
				Pdt pdt = Pdt.instance();
				while (fileStartTime > 0)
				{
					// Retrieve the index
					MsgPeriodArchive mpa = getPeriodArchive(fileStartTime, false);
					if (mpa == null || fileStartTime != mpa.startTime)
					{
						Logger.instance().debug1(
							module + " DCP " + dcpAddr
							+ " Inactive -- Fell off archive");
						pdtEntry.active_flag = 'N';
						return;
					}
					DcpMsgIndex idx = mpa.getIndexEntrySync(idxNum);
					if (idx == null)
					{
//						Logger.instance().debug1(
//							module + " Missing squelched -- invalid index ptr");
						pdtEntry.active_flag = 'N';
						return;
					}
					if (!dcpAddr.equals(idx.getDcpAddress()))
					{
//						Logger.instance().debug1(
//							module + " Missing squelched DCP " + dcpAddr
//							+ " -- invalid dcp addr");
						pdtEntry.active_flag = 'N';
						return;
					}
					if (idx.getXmitTime().before(too_old))
					{
//						Logger.instance().debug1(
//							module + " Missing squelched: DCP " + dcpAddr
//							+ " Inactive -- last msg > 48 hrs ago");
						pdtEntry.active_flag = 'N';
						return;
					}
					char fc = idx.getFailureCode();
					if (fc == 'G' || fc == '?')
					{
						lastRealGoesTime.put(dcpAddr, idx.getXmitTime());
//						Logger.instance().debug1(
//							module + " DCP " + dcpAddr
//							+ " Missing - last msg received at "
//							+ idx.getXmitTime());
						break; // We have a real msg within the last 48 hours!
					}
	
					fileStartTime = idx.getPrevFileThisDcp();
					idxNum = idx.getPrevIdxNumThisDcp();
				}
			}
		}
		DcpMsg statmsg = makeStatMsg(msg, failureCode, explanation);
		doArchiveMsg(statmsg, src, t);
	}
	
	/**
	 * This method does the actual archiving and merge-filtering.
	 * @param msg The message to archive
	 * @param src the input interface that is the source of the message
	 * @return true if message was archived, false if it didn't pass merge filter.
	 */
	private boolean doArchiveMsg(DcpMsg msg, LrgsInputInterface src, Date now)
	{
		int slot = src == null ? -1 : src.getSlot();
		int nowTT = (int)(now.getTime() / 1000L);
		msg.setLocalReceiveTime(now);

		// Determine the DCP address, assign special constant if none.
		DcpAddress addr = msg.getDcpAddress();

		IndexPtr lastPtr = lastMsgHash.get(addr);
		if (lastPtr == null)
		{
			lastPtr = new IndexPtr(0, 0, 0, 0, 0);
			lastMsgHash.put(addr, lastPtr);
		}
		
		int mergeResult = MergeFilter.SAVE_DCPMSG;
		msg.mergeFilterCode = 0;
		int seqNum = msg.getSequenceNum();
		IndexPtr origPtr = new IndexPtr(0, 0, 0, 0, 0);
		int domsatTT = 0;

		if ((msg.getFlagbits() & DcpMsgFlag.FORCE_SAVE) == 0)
			mergeResult = mergeFilter.getMergeResult(msg, lastPtr, origPtr);
		else // FORCE_SAVE was turned on. Turn it off in the archived copy.
			msg.setFlagbits(msg.getFlagbits() & (~DcpMsgFlag.FORCE_SAVE));
		
		msg.mergeFilterCode = mergeFilter.lastCode;
		domsatTT = msg.getDomsatTime() == null ? 0
			: (int)(msg.getDomsatTime().getTime()/1000L);

		// Get ptr to previous msg from this DCP.
		if (!msg.isGoesMessage())
		{
			Logger.instance().debug1(module + 
				(mergeResult == MergeFilter.DISCARD ? " NOT" : "")
				+ " Archiving NON-GOES message for address '" + addr.toString()
				+ "' from input slot " 
				+ slot + ", len=" + msg.getMsgLength() + ", timestamp="
				+ msg.getDapsTime()+ ", failcode=" 
				+ msg.getFailureCode() + ", lastPtr=" + lastMsgHash.get(addr)
				+ ", mergeResult=" + mergeResult
				+ ", mergeFilterCode=" + msg.mergeFilterCode
				+ ", seqNum=" + msg.getSequenceNum());
		}

		if (mergeResult != MergeFilter.DISCARD)
		{
			lastPtr.indexNumber = currentArchive.archiveMsg(msg, lastPtr);
			lastPtr.indexFileStartTime = currentArchive.startTime;
			lastPtr.msgTime = (int)(msg.getDapsTime().getTime() / 1000L);
			lastPtr.flagBits = msg.flagbits;
			lastPtr.msgLength = msg.getMsgLength();
			if (domsatTT > 0 && seqNum >= 0
			 && (msg.flagbits & DcpMsgFlag.MSG_NO_SEQNUM) == 0)
			{
//Logger.instance().info("MJM First mapping seq # " + seqNum);
				if (domsatTT >= todaySeqMap.startTime
				 && domsatTT < todaySeqMap.startTime + SPD)
					todaySeqMap.add(domsatTT*1000L, seqNum, 
						(short)(domsatTT / SPD), lastPtr.indexNumber);
				else if (domsatTT >= yesterdaySeqMap.startTime
				      && domsatTT < yesterdaySeqMap.startTime + SPD)
					yesterdaySeqMap.add(domsatTT*1000L, seqNum, 
						(short)(domsatTT / SPD), lastPtr.indexNumber);
			}
			
			// MJM 2011 11/16 If we get a real GOES msg, mark the pdt as active.
			if (msg.isGoesMessage())
			{
				char failureCode = msg.getFailureCode();
				if (failureCode == 'G' || failureCode == '?')
				{
					PdtEntry pe = Pdt.instance().find(msg.getDcpAddress());
					if (pe != null)
						pe.active_flag = 'Y';
				}
			}
		}
		else 
		{
			// This msg WAS discarded. If this one HAS a domsat seqnum and
			// the one we're replacing DOES NOT have one. THEN store this
			// seqnum refering to the msg already in storage.

//			Logger.instance().debug3(module + 
//				" Discarding duplicate or redundant msg from " + addr);

			if (domsatTT > 0 && seqNum >= 0
			 && (msg.flagbits & DcpMsgFlag.MSG_NO_SEQNUM) == 0)
			{
				if ((origPtr.flagBits & DcpMsgFlag.MSG_NO_SEQNUM) != 0)
				{
// Logger.instance().info("MJM RE mapping seq # " + seqNum +
// " because previously-saved msg does NOT have seq#. this.flags=0x"
// + Integer.toHexString(msg.flagbits) + ", prev.flags=0x" 
// + Integer.toHexString(origPtr.flagBits));

					short idxDayNum = (short)(origPtr.indexFileStartTime / SPD);
					if (domsatTT >= todaySeqMap.startTime
					 && domsatTT < todaySeqMap.startTime + SPD)
						todaySeqMap.add(domsatTT*1000L, seqNum, 
							idxDayNum, origPtr.indexNumber);
					else if (domsatTT >= yesterdaySeqMap.startTime
					      && domsatTT < yesterdaySeqMap.startTime + SPD)
						yesterdaySeqMap.add(domsatTT*1000L, seqNum, 
							idxDayNum, origPtr.indexNumber);

					// Now mark the old message as HAVING a sequence num.
				 	origPtr.flagBits &= (~DcpMsgFlag.MSG_NO_SEQNUM);
					MsgPeriodArchive mpa =
						getPeriodArchive(origPtr.indexFileStartTime, false);
					if (mpa != null)
					{
						mpa.addDomsatSequence(origPtr.indexNumber,
							seqNum, domsatTT*1000L);
					}
				}
				else
				{
// Logger.instance().info("MJM NOT mapping seq # " + seqNum +
// " because prev-saved msg already has seq#. this.flags=0x"
// + Integer.toHexString(msg.flagbits) + ", prev.flags=0x" 
// + Integer.toHexString(origPtr.flagBits));
				}
			}
		}

		if (slot != -1)
			statusProvider.receivedMsg(slot, nowTT, msg.getFailureCode(), 
				msg.getSequenceNum(), 
				mergeResult != MergeFilter.DISCARD ? lastPtr.indexNumber : -1,
				mergeResult);

		return mergeResult != MergeFilter.DISCARD;
	}
	
	/**
	 * A synchronized wrapper around checkCurrentArchive so it can be called
	 * from LrgsMain at the beginning of every day.
	 * This ensures that the new archive is created at the start of every day
	 * even if no messages are coming in.
	 */
	public synchronized void doCheckCurrentArchive()
	{
		checkCurrentArchive((int)(System.currentTimeMillis()/1000L));
	}
	
	/**
	 * Use current time and lastArchiveSec to determine if it's time to
	 * start a new MsgPeriodArchive. If so, do it. 
	 */
	private void checkCurrentArchive(int now)
	{
		if (currentArchive == null 
		 || now >= currentArchive.startTime + MsgPeriodArchive.periodDuration)
		{
			int startTime = (now / MsgPeriodArchive.periodDuration) 
				* MsgPeriodArchive.periodDuration;
			String arcRootPath = archiveDir.getPath() + File.separator 
				+ lrgs7namePrefix 
				+ nameDateFormat.format(new Date(startTime*1000L));
			try
			{
				if (currentArchive != null)
				{
					lastMsgHash.saveIndexPtrHash(currentArchive.getRootPath() 
						+ MsgPeriodArchive.IHASH_EXT, this);
					currentArchive.finish();
				}
				currentArchive = 
					new MsgPeriodArchive(arcRootPath, startTime, true);
				Logger.instance().info("Adding log with root path '"
					+ arcRootPath + "'");
				periodArchives.add(currentArchive);
			}
			catch(IOException ioex)
			{
				Logger.instance().failure(module + ":" + EVT_BAD_ARCHIVE
					+ "- Cannot create archive at '"
					+ arcRootPath + "': " + ioex);
				currentArchive = null;
			}
		}

		if (todaySeqMap == null)
		{
			todaySeqMap = new DailyDomsatSeqMap(currentArchive.startTime, this);
			yesterdaySeqMap = new DailyDomsatSeqMap(
				currentArchive.startTime - (3600 * 24), this);
		}
		else if (currentArchive.startTime > todaySeqMap.startTime)
		{
			Logger.instance().info(module 
				+ " Rotating DOMSAT Sequence Map");
			yesterdaySeqMap = todaySeqMap;
			todaySeqMap = new DailyDomsatSeqMap(currentArchive.startTime, this);
		}
	}
	
	/**
	 * Returns the specific MsgPeriodArchive that would contain the first
	 * message with a time equal or greater than the passed time.
	 * @param sinceSec Unix time_t of the message time.
	 * @param earliest true to return earliest period if sinceSec is before
	 *        all periods.
	 * @return period archive containing time, or null if not found.
	 */
	public MsgPeriodArchive getPeriodArchive(int sinceSec,
		boolean earliest)
	{
		// Look in the currentArchive first
		if (currentArchive != null
		 && sinceSec >= currentArchive.startTime)
			return currentArchive;

		return periodArchives.getPeriodArchive(sinceSec, earliest);
	}

	/**
	 * Initialize a search & return a new handle.
	 * There are two algorithms:
	 * <p>
	 * 1. SM_INDEXSEARCH: seek to starting minute in the period specified
	 *    by SINCE time and read forward to the UNTIL time, reading and
	 *    testing every index.
	 * <p>
	 * 2. SM_BACKREF: For each DCP specified, find the most recent message,
	 *    then seek backward through the linked list of indexes.
	 * <p>
	 * The first choice is better if there are many DCPs or if we are searching
	 * for a time range that occurred far in the past. 
	 */
	public SearchHandle startSearch( MsgFilter filter )
	{
		SearchHandle handle = new SearchHandle(filter);

		// The formula is:
		// if (n * 12 * (r/1440) * t < m) then SM_BACKREF, else INDEXSEARCH
		// where
		//  n = number of DCPs requested
		//  12 : Assume 12 messages per DCP per Day on average
		//  r = complete range in minutes: now - since
		//  1440 = minutes per day
		//  t = time coefficient: ratio of how long it takes to read
		//      a single index to reading a 1000-index buffer full.
		//  m = minutes requested = until - since

		DcpAddress[] addresses = filter.getDcpAddresses();
		int n = addresses == null ? 0 : addresses.length;
		Date until = filter.getUntilTime();
		Date since = filter.getSinceTime();

		// If no DCPs specified, or if this is a real-time retrieval,
		// Then we have to use INDEXSEARCH
		if (n == 0 || until == null || filter.forceAscending())
		{
			handle.searchMethod = SearchHandle.SM_INDEXSEARCH;
			startIndexSearch(handle);
			return handle;
		}

		// Compute pointer read range as a number of minutes.
		long now = System.currentTimeMillis();
		long sincel = (since == null) ? now - (numPeriods*24*60*60*1000L) 
			: since.getTime();
		int r = (int)((now - sincel) / 60000L);
		
		double t = .5;

		// Compute m = Number of minutes in since ... until
		// This will be the approx # of reads for an index search.
		int m = (int)((until.getTime() - sincel) / 60000L);

		// Weighted approx # read for a pointer search:
		int wnrp = (int)(n * 12 * (r/1440.0) * t);

		Logger.instance().debug2(module + " startSearch: n=" + n + ", t=" + t
			+ ", r=" + r + ", m=" + m
			+ ", n * 12 * (r/1440.0) * t = " + wnrp);
		if (wnrp < m)
		{
			Logger.instance().debug2(module 
				+ " Starting BACKWARD POINTER Search");
			handle.searchMethod = SearchHandle.SM_BACKREF;
		}
		else
		{
			Logger.instance().debug2(module + " Starting FORWARD INDEX Search");
			handle.searchMethod = SearchHandle.SM_INDEXSEARCH;
			startIndexSearch(handle);
		}

		return handle;
	}

	/**
	 * Initializes the handle for a forward-index search.
	 * Find the staring period index and call it to set the starting minute.
	 * @param handle the handle to initialize
	 */
	private void startIndexSearch(SearchHandle handle)
	{
		Date since = handle.filter.getSinceTime();
		int sinceSec = since == null ? 0 : (int)(since.getTime() / 1000L);

//String msg = module + " MsgArchive.startIndexSearch sinceSec=" 
//+ sinceSec + " since='" + since + "'";
//Logger.instance().debug1(msg);

		MsgPeriodArchive mpa = getPeriodArchive(sinceSec, true);
		if (mpa == null)
			handle.periodStartTime = 0;
		else
		{
			handle.periodStartTime = mpa.startTime;
			mpa.startIndexSearch(handle);
		}
	}

	/**
	 * Search for the next batch of max messages.
	 * Place retrieved indexes (each containing a message)
	 * In the array stored in the passed handle.
	 *
	 * @return SEARCH_RESULT_DONE, SEARCH_RESULT_MORE, SEARCH_RESULT_PAUSE,
	 *  or SEARCH_RESULT_TIMELIMIT
	 * @throws ArchiveUnavailableException if can't init search criteria
	 * @throws SearchTimeoutException if searchStopMsec reached with no results.
	 */
	public int search(SearchHandle handle, long stopSearchMsec)
		throws ArchiveUnavailableException, SearchTimeoutException
	{
		long start = System.currentTimeMillis();
		if (handle.searchMethod == SearchHandle.SM_INDEXSEARCH)
		{
//Logger.instance().info("Doing SM_INDEXSEARCH");
			MsgPeriodArchive mpa = 
				getPeriodArchive(handle.periodStartTime, true);
			if (mpa == null)
			{
				String msg = "No archive for start time="
					+ IDateFormat.time_t2string(handle.periodStartTime);
				Logger.instance().warning(module + " " + msg);
				throw new ArchiveUnavailableException(msg,
					LrgsErrorCode.DBADSINCE);
			}
			try 
			{
				int result;
				while((result = mpa.searchIndex(handle, stopSearchMsec)) 
					== MsgArchive.SEARCH_RESULT_MORE
					&& handle.capacity() > 0)
				{
					mpa = getPeriodArchive(handle.periodStartTime, true);
				}
				
//if (handle.filter.getClientName().contains("verizon.net"))
//Logger.instance().info("SM_INDEXSEARCH returning result " + result 
//+ ", handle.idxBufFillLength=" + handle.idxBufFillLength
//+ ", handle.nextIdxBufNum=" + handle.nextIdxBufNum);
				return result;
			}
			catch(ArchiveUnavailableException ex)
			{
				Logger.instance().warning(module + " Corrupt period "
					+ mpa.myname);
				throw ex;
			}
		}
		else if (handle.searchMethod == SearchHandle.SM_BACKREF)
		{
			return doBackRefSearch(handle, start);
		}

		throw new ArchiveUnavailableException("No Such Search Algorithm",
			LrgsErrorCode.DDDSINTERNAL);
	}

	/**
	 * Internal method to implement the backward pointer reference search.
	 */
	private int doBackRefSearch(SearchHandle handle, long searchStart)
		throws ArchiveUnavailableException, SearchTimeoutException
	{
		// client name is hostname plus unique numeric ID.
//boolean isTestClient = handle.filter.getClientName().contains("verizon.net");

		DcpAddress[] addresses = handle.filter.getDcpAddresses();
		Date since = handle.filter.getSinceTime();
		int sinceTT = since == null ? 0 : (int)(since.getTime() / 1000L);
		MsgPeriodArchive mpa = null;
		long searchEnd = searchStart + 45000L;

//if (isTestClient)
//Logger.instance().info("doBackRefSearch: addresses.length = "
//+ addresses.length + ", curaddr=" + handle.curaddr);

		while(handle.curaddr < addresses.length 
		   && handle.capacity() > 0
		   && System.currentTimeMillis() < searchEnd)
		{
			// nextIndexNum == -1 means to start the next address.
			if (handle.nextIndexNum == -1)
			{
				if (++handle.curaddr < addresses.length)
				{
					DcpAddress addr = addresses[handle.curaddr];
//if (isTestClient)
//Logger.instance().info(module 
//+ " Starting ptr search for '" + addr + "'");
					IndexPtr idxPtr = lastMsgHash.get(addr);
					if (idxPtr != null)
					{
						handle.periodStartTime = idxPtr.indexFileStartTime;
						handle.nextIndexNum = idxPtr.indexNumber;
//if (isTestClient)
//Logger.instance().info(module + " periodStartTime=" + handle.periodStartTime
//+ ", indexNum=" + handle.nextIndexNum);
					}
					else
						Logger.instance().debug1(
							module + " No idxPtr entry for '" + addr + "'");
				}
				continue;
			}

			if (mpa == null || handle.periodStartTime != mpa.startTime)
			{
				if (handle.periodStartTime == 0
				 || (mpa = getPeriodArchive(handle.periodStartTime, false)) 
						== null
				 || handle.periodStartTime != mpa.startTime)
				{
					handle.nextIndexNum = -1; // Done with this address.
//if (isTestClient)
//Logger.instance().debug1(module + " done with address '" 
//+ addresses[handle.curaddr] + "'");
					continue;
				}
			}
	
//if (isTestClient)
//Logger.instance().info(module + " Retrieving index "
//+ handle.nextIndexNum + " in file " + mpa.myname);
			DcpMsgIndex dmi = mpa.getIndexEntrySync(handle.nextIndexNum);
			if (dmi == null)
			{
				Logger.instance().warning(
					"Corrupt pointer for dcp '"
					+ addresses[handle.curaddr].toString() + "' in archive "
					+ mpa.myname + "' index num=" + handle.nextIndexNum);
				handle.nextIndexNum = -1; // Skip rest of this address.
				continue;
			}
				
			if (dmi.getXmitTime().getTime()/1000 < sinceTT)
			{
//if (isTestClient)
//Logger.instance().info("doBackRefSearch stopped because time before sinceTT");
				handle.nextIndexNum = -1; // Done with this address.
				continue;
			}

			if ((dmi.getFlagbits() & DcpMsgFlag.MSG_DELETED) == 0
			 && handle.filter.passes(dmi))
			{
				mpa.readMessage(dmi);
				handle.addIndex(dmi);
			}
//else if (isTestClient)
//Logger.instance().info("doBackRefSearch not including following index because it doesn't pass crit: " + dmi.toString());


			handle.periodStartTime = dmi.getPrevFileThisDcp();
			handle.nextIndexNum = dmi.getPrevIdxNumThisDcp();
		}
//if (isTestClient)
//Logger.instance().info("doBackRefSearch loop stopped: curaddr=" 
//+handle.curaddr + ", addresses.length=" + addresses.length
//+", capacity=" + handle.capacity() 
//+ ", curtime=" + System.currentTimeMillis()
//+ ",searchEnd=" + searchEnd);

		if (!handle.isEmpty() || handle.curaddr < addresses.length)
			return SEARCH_RESULT_MORE;
		else
			return SEARCH_RESULT_DONE;
	}
	
	/**
	 * Called periodically to checkpoint certain transient info to the disk.
	 */
	public void checkpoint()
	{
		Logger.instance().debug3(module + " MsgArchive.checkpoint()");
		lastMsgHash.saveIndexPtrHash(archiveDir + "/index-hash", this);

		int startTimeCutoff = 
			(int)(System.currentTimeMillis() / 1000L)
			- (numPeriods * MsgPeriodArchive.periodDuration);
		String cutoffStr = nameDateFormat.format(
			new Date(startTimeCutoff*1000L));
	
		periodArchives.checkpointAll(startTimeCutoff, cutoffStr);
	}

	/**
	 * @return total number of messages in all archives.
	 */
	public int getTotalMessageCount()
	{
		return periodArchives.getTotalMessageCount();
	}

	/**
	 * @return Unix time_t message time stamp of oldest message.
	 */
	public int getOldestDapsTime()
	{
		return periodArchives.getOldestDapsTime();
	}

	/**
	 * Retrieve messages by DOMSAT sequence number range.
	 * @param approxDomsatTime approximate domsat time of outage.
	 * @param seqStart first missing sequence number
	 * @param seqEnd last missing sequence number
	 * @param msgs return messages by storing them here
	 * @return number of messages stored.
	 */
	public int getMsgsBySeqNum(long fromDomsatTime, long untilDomsatTime,
		int seqStart, int seqEnd, ArrayList<DcpMsg> msgs)
		throws ArchiveUnavailableException
	{
		int r = 0;
		int tt = (int)(fromDomsatTime / 1000L) - 15;
		if (yesterdaySeqMap != null
		 && tt >= yesterdaySeqMap.startTime 
		 && tt < yesterdaySeqMap.startTime + SPD)
			r += yesterdaySeqMap.getMsgsBySeqNum(fromDomsatTime, untilDomsatTime,
				seqStart, seqEnd, msgs);
		tt = (int)(untilDomsatTime / 1000L) + 15;
		if (todaySeqMap != null
		 && tt >= todaySeqMap.startTime 
		 && tt < todaySeqMap.startTime + SPD)
			r += todaySeqMap.getMsgsBySeqNum(fromDomsatTime, untilDomsatTime,
				seqStart, seqEnd, msgs);
		return r;
	}
	
	/**
	 * Make a DAPS Status Message.
	 * @param origMsg the original real DCP message.
	 * @param failcode the failure code to use.
	 * @param expl and explanation of the failure placed in msg body.
	 */
	private DcpMsg makeStatMsg(DcpMsg origMsg, char failcode, String expl)
	{
		byte newdata[] = new byte[37 + expl.length()];
		byte origData[] = origMsg.getData();
		for(int i=0; i<32; i++)
			newdata[i] = origData[i];
		newdata[DcpMsg.IDX_FAILCODE] = (byte)failcode;
		byte[] explb = expl.getBytes();
		for(int i=0; i<explb.length; i++)
			newdata[37+i] = explb[i];
		String lenstr = validator.formatLength(explb.length);
		for(int i=0; i<5; i++)
			newdata[32+i] = (byte)lenstr.charAt(i);

		DcpMsg ret = new DcpMsg(newdata, newdata.length, 0);
		ret.setLocalReceiveTime(origMsg.getLocalReceiveTime());
		return ret;
	}

}

class CheckpointThread extends Thread
{
	boolean shutdown;
	MsgArchive archive;

	CheckpointThread(MsgArchive ma)
	{
		shutdown = false;
		archive = ma;
	}

	public void run()
	{
		Logger.instance().debug1(MsgArchive.module 
			+ " MsgArchive checkpoint thread starting.");
		while(!shutdown)
		{
			try{ sleep(60000L); }
			catch(InterruptedException ignore) {}
			if (!shutdown)
				archive.checkpoint();
		}
	}
}
