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

import java.util.Date;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DapsFailureCode;
import lrgs.lrgsmain.LrgsConfig;

import ilex.util.Logger;

/**
This class is used by the archive code to determine what to do with an
incoming DCP message.
*/
public class MergeFilter
{
	/** Result Code: save this new DCP message. */
	public static final int SAVE_DCPMSG = 0;

	/** Result Code: save DAPS Status message. */
	public static final int SAVE_STATMSG = 1;

	/** Result Code: overwrite the previously saved copy because of errors. */
	public static final int OVERWRITE_PREV_BAD = 2;

	/** Result Code: overwrite the previously saved copy because of priority. */
	public static final int OVERWRITE_PREV_GOOD = 3;

	/** Result Code: discard this copy. */
	public static final int DISCARD = 4;
	
	private int inputPriorities[];

	private MsgArchive msgArchive;

	public static final String module = "MergeFilter";

	public byte lastCode;
	private int mergeTimeThreshold = 5; // seconds

	/**
	 * Allow lengths to vary by this amount before one message is considered
	 * longer than another.
	 */
	public int allowLengthDifference = 1;
	
	private LrgsConfig cfg = null;

	/** Constructor. */
	public MergeFilter(MsgArchive msgArchive)
	{
		inputPriorities = new int[4];
		setInputPriorities();
		this.msgArchive = msgArchive;
		lastCode = (byte)0;
		cfg = LrgsConfig.instance();
		String smt = cfg.getMiscProp("mergeTimeThreshold");
		if (smt != null)
		{
			try { mergeTimeThreshold = Integer.parseInt(smt); }
			catch(Exception ex)
			{
				Logger.instance().warning(module + " bad 'mergeTimeThreshold' "
					+ "value in lrgs.conf '" + smt + "' -- ignored");
			}
		}
	}

	/**
	 * Examine configuration and set input priorities.
	 */
	public void setInputPriorities()
	{
		for(int i=0; i<4; i++) inputPriorities[i] = -1;
		LrgsConfig cfg = LrgsConfig.instance();
		int n = 0;
		if (cfg.mergePref1 != null && cfg.mergePref1.length() > 0)
			inputPriorities[0] = DcpMsgFlag.sourceName2Value(
				cfg.mergePref1.toUpperCase());
		if (cfg.mergePref2 != null && cfg.mergePref2.length() > 0)
			inputPriorities[1] = DcpMsgFlag.sourceName2Value(
				cfg.mergePref2.toUpperCase());
		if (cfg.mergePref3 != null && cfg.mergePref3.length() > 0)
			inputPriorities[2] = DcpMsgFlag.sourceName2Value(
				cfg.mergePref3.toUpperCase());
		if (cfg.mergePref4 != null && cfg.mergePref4.length() > 0)
			inputPriorities[3] = DcpMsgFlag.sourceName2Value(
				cfg.mergePref4.toUpperCase());
	}

	/**
	 * Determine if this message should be saved, and whether a duplicate
	 * should be overwritten (i.e. marked as deleted.)
	 * <p>If the result indicates that this message should be discarded
	 * because it is a duplicate, the 'origPtr' will contain the day &
	 * index number of the original copy of the message being retained.
	 * @param msg the newly arrived message.
	 * @param lastPtr hash entry pointing to last entry for this DCP.
	 * @return code indicating how msg should be handled.
	 */
	public int getMergeResult(DcpMsg msg, IndexPtr lastPtr, IndexPtr origPtr)
	{
		Date msgDate = msg.getDapsTime();
		int msgTimeT = (int)(msgDate.getTime() / 1000L);
		String headerstr = msg.getHeader();
		int dataLen = msg.getDcpDataLength(); // 5-digit length in msg.

		int now = (int)(System.currentTimeMillis() / 1000L);
		if (msgTimeT > now + 1800)
		{
			lastCode = 19;
			Logger.instance().warning(module 
				+ " discarding msg with future time stamp '"
				+ headerstr.substring(8, 8+11) + "'");
			return DISCARD;   // Always ignore dup status msgs.
		}

		char newFc = msg.getFailureCode();
		boolean isDapsStatus = msg.isDapsStatusMsg();
		int channel = msg.getGoesChannel();

		// Follow linked list back until match is found.
		int fileStartTime = lastPtr.indexFileStartTime;
		int idxNum = lastPtr.indexNumber;

		while(fileStartTime != 0)
		{
			// Retrieve the index
			MsgPeriodArchive mpa = 
				msgArchive.getPeriodArchive(fileStartTime, false);
			if (mpa == null || fileStartTime != mpa.startTime)
			{
//				Logger.instance().debug2(module 
//					+ " archive missing for start time " + fileStartTime);
				if (isDapsStatus)
				{
					lastCode = 1;
					return SAVE_STATMSG;
				}
				else
				{
					lastCode = 2;
					return SAVE_DCPMSG;
				}
			}

			DcpMsgIndex idx = mpa.getIndexEntrySync(idxNum);
			if (idx == null)
			{
//				Logger.instance().debug2(module + " Index " + idxNum 
//					+ " missing for file start time " + fileStartTime);
				if (isDapsStatus)
				{
					lastCode = 3;
					return SAVE_STATMSG;
				}
				else
				{
					lastCode = 4;
					return SAVE_DCPMSG;
				}
	 		}
			origPtr.indexFileStartTime = fileStartTime;
			origPtr.indexNumber = idxNum;
			origPtr.flagBits = idx.getFlagbits();

			if (isMatch(msgTimeT, channel, newFc, idx, msg))
			{
				// We already have a copy of this message in storage.
				// Now determine which is better, the old one or the new.
				char storedFc = idx.getFailureCode();

				if (newFc == storedFc)
				{
					if (isDapsStatus)
					{
//						Logger.instance().debug2(module 
//							+ " discarding dup DAPS Status Msg: " + headerstr);
						lastCode = 5;
						return DISCARD;   // Always ignore dup status msgs.
					}
					// else Real msgs -- equally good (or bad).
					int lendiff = idx.getMsgLength() > 0 ?
						dataLen - idx.getMsgLength() : 0;

					// Both good quality
					if (newFc == 'G')
					{
						// If different lengths, keep the longer.
						if (lendiff > allowLengthDifference)
						{
							lastCode = 7;
							mpa.deleteMsg(idxNum);
							return OVERWRITE_PREV_GOOD;
						}
						// Else if THIS has accurate carrier and PREV doesn't
						else if (DcpMsgFlag.hasAccurateCarrier(msg.flagbits)
						 && !DcpMsgFlag.hasAccurateCarrier(idx.getFlagbits()))
						{
							lastCode = 19;
							mpa.deleteMsg(idxNum);
							return OVERWRITE_PREV_GOOD;
						}
						else if (!cfg.archivePreferredGood)
						{
							lastCode = 8;
							return DISCARD;
						}
						// Else fall through and check input priorities
					}

					int thisPri = 5;
					int thisSrc = msg.flagbits & DcpMsgFlag.SRC_MASK;
					int prevPri = 5;
					int prevSrc = idx.getFlagbits() & DcpMsgFlag.SRC_MASK;
					for(int i=0; i<4; i++)
					{
						if (inputPriorities[i] == thisSrc)
							thisPri = i;
						if (inputPriorities[i] == prevSrc)
							prevPri = i;
					}
					if (thisPri < prevPri)
					{
//						Logger.instance().debug2("deleting index "
//							+ idxNum + " in file " + mpa.myname
//							+ " -- Priority Override for msg " + headerstr);
						mpa.deleteMsg(idxNum);
						if (newFc == 'G')
						{
							lastCode = 9;
							return OVERWRITE_PREV_GOOD;
						}
						else
						{
							lastCode = 10;
							return OVERWRITE_PREV_BAD;
						}
					}
					else
					{
//						Logger.instance().debug2(module +
//						 " discarding dup real msg same or worse priority: "
//							+ headerstr);
						lastCode = 11;
						return DISCARD;
					}
				}
				else if (newFc == 'G' && storedFc == '?')
				{
//					Logger.instance().debug2(module + " deleting index "
//						+ idxNum + " in file " + mpa.myname
//						+ " -- Better Quality '" + headerstr + "'");
					lastCode = 12;
					mpa.deleteMsg(idxNum);
					return OVERWRITE_PREV_BAD;
				}
				else if (newFc == '?' && storedFc == 'G')
				{
//					Logger.instance().debug2(module 
//						+ " discarding msg - already have copy with better "
//						+ "quality: " + headerstr);
					lastCode = 13;
					return DISCARD;
				}
				else if (newFc == 'M' 
				      && (storedFc == 'G' || storedFc == '?'))
				{
//					Logger.instance().debug2(module 
//						+ " discarding 'M' because already have real: "
//						+ headerstr);
					lastCode = 14;
					return DISCARD;
				}
				else if ((newFc == 'G' || newFc == '?')
				      && storedFc == 'M')
				{
					Logger.instance().debug2(module 
						+ " deleting stored 'Missing' index "
						+ idxNum + " in file " + mpa.myname
						+ " -- got real one '" + headerstr + "'");
					mpa.deleteMsg(idxNum);
					lastCode = 15;
					return SAVE_DCPMSG;
				}
			}
			// else not a match if it falls through to here

			// Stop when we get before the specified msg time, can't be there!
			// Allow half-hour fudge factor.
			if ((idx.getLocalRecvTime().getTime()/1000L) < msgTimeT - 1800)
			{
				if (isDapsStatus)
				{
					lastCode = 16;
					return SAVE_STATMSG;
				}
				else
				{
					lastCode = 17;
					return SAVE_DCPMSG;
				}
			}

			// Follow the linked list backward.
			fileStartTime = idx.getPrevFileThisDcp();
			idxNum = idx.getPrevIdxNumThisDcp();
		}
		lastCode = 18;
		return isDapsStatus ? SAVE_STATMSG : SAVE_DCPMSG;
	}

	/**
	 * Return true if this message is to be considered a match of the
	 * stored one.
	 * @param newTimeT time_t value of newly arrived message
	 * @param newChannel GOES channel of newly arrived message
	 * @param newFc Failure Code of newly arrived message
	 * @param storedIdx Index of previously stored message we test against
	 */
	public boolean isMatch(int newTimeT, int newChannel, char newFc,
		DcpMsgIndex storedIdx, DcpMsg newMsg)
	{
		// Iridium uses MOMSN (Mobile-Originated Msg Sequence Num),
		// which is guaranteed to be the same, regardless of ground system.
		if (DcpMsgFlag.isIridium(newMsg.flagbits)
		 && newMsg.getSequenceNum() == storedIdx.getSequenceNum())
			return true;
		
		int deltaT = newTimeT - (int)(storedIdx.getXmitTime().getTime()/1000L);
		int threshold = 
			(newFc == 'M' || storedIdx.getFailureCode() == 'M') ? 120 : 
			mergeTimeThreshold;

		if (deltaT > -threshold && deltaT < threshold
		 && (storedIdx.getFlagbits() & DcpMsgFlag.MSG_DELETED) == 0  // Not deleted
		 && newChannel == storedIdx.getChannel())                   // Same GOES Chan
			return true;

		return false;
	}
}
