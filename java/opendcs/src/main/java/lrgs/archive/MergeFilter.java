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

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.lrgsmain.LrgsConfig;

/**
This class is used by the archive code to determine what to do with an
incoming DCP message.
*/
public class MergeFilter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atWarn()
				   .setCause(ex)
				   .log("bad 'mergeTimeThreshold' value in lrgs.conf '{}' -- ignored", smt);
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
			log.warn("discarding msg with future time stamp '{}'", headerstr.substring(8, 8+11));
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
						lastCode = 11;
						return DISCARD;
					}
				}
				else if (newFc == 'G' && storedFc == '?')
				{
					lastCode = 12;
					mpa.deleteMsg(idxNum);
					return OVERWRITE_PREV_BAD;
				}
				else if (newFc == '?' && storedFc == 'G')
				{
					lastCode = 13;
					return DISCARD;
				}
				else if (newFc == 'M'
				      && (storedFc == 'G' || storedFc == '?'))
				{
					lastCode = 14;
					return DISCARD;
				}
				else if ((newFc == 'G' || newFc == '?')
				      && storedFc == 'M')
				{
					log.trace("deleting stored 'Missing' index {} in file {} -- got real one '{}'",
							  idxNum, mpa.myname, headerstr);
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
		if (newMsg.isIridium()
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
