/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.archive;

import java.util.ArrayList;
import java.util.Date;

import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;

public class DailyDomsatSeqMap
{
	/** One for each minute of day, points to 1st index entry for that minute */
	private int minuteStart[];

	/** time_t of beginning of day for this map. */
	int startTime;

	private ArrayList<DomsatSeq> domsatSeqs;
	private static final long MSEC_PER_DAY = 24 * 3600 * 1000L;
	private static final int MIN_PER_DAY = 24 * 60;
	private static final int SPD = 24 * 3600;

	private int earliestLoc;
	private int latestLoc;
	private MsgArchive msgArchive;

	/** Constructor */
	public DailyDomsatSeqMap(int startTime, MsgArchive msgArchive)
	{
		this.startTime = startTime;
		this.msgArchive = msgArchive;
		minuteStart = new int[MIN_PER_DAY];
		for(int i=0; i<MIN_PER_DAY; i++)
			minuteStart[i] = -1;
		domsatSeqs = new ArrayList<DomsatSeq>();
		earliestLoc = -1;
		latestLoc = -1;
	}


private int lastSeqNum = -1;

	/**
	 * Creates a new entry and adds it to the map.
	 * @param domsatTime Approximate time message was received over DOMSAT.
	 * @param seqNum DOMSAT Sequence Number
	 * @param dayNum Day number of index file where msg is stored.
	 * @param indexNum Index number within that day's archive file.
	 */
	public synchronized void 
		add(long domsatTime, int seqNum, short dayNum, int indexNum)
	{
		if (domsatTime <= 0L)
			return;
//if (seqNum == lastSeqNum)
//Logger.instance().info("MJM: SeqMap dup seq=" + seqNum + " indexNum=" + indexNum);
lastSeqNum = seqNum;


		// Add new entry with (next == -1), meaning end of the chain.
		int thisLoc = domsatSeqs.size();
		DomsatSeq thisDs = 
			new DomsatSeq(domsatTime, seqNum, dayNum, indexNum, -1);
		domsatSeqs.add(thisDs);

		// Find the minute this entry belongs to.
		int thisMin = (int)((domsatTime % MSEC_PER_DAY) / 60000L);

		// Also find the minute where the previous entry will belong.
		int prevMin = thisMin;
		if (minuteStart[thisMin] == -1
		 || domsatTime < domsatSeqs.get(minuteStart[thisMin]).getDomsatTime())
		{
			minuteStart[thisMin] = thisLoc;// This one is 1st in the minute.
			--prevMin; // start looking for previous entry in previous minute
		}

		// Now either this is the earliest one in the file, or there is one
		// before it. Find the entry prior to this one in domsat-time order.
		while(prevMin >= 0 && minuteStart[prevMin] == -1)
			prevMin--;   // skip any minutes that don't have any entries.

		if (prevMin < 0)
		{
			// The new entry is the earliest one in the file.
			if (earliestLoc != -1)
				thisDs.setNext(earliestLoc);
			earliestLoc = thisLoc;
			if (latestLoc == -1)
				latestLoc = thisLoc;
		}
		else // I have at least 1 minute with an entry before the inserted one.
		{
			// Optimization: Check to see if this is the latest.
			DomsatSeq latestDs = domsatSeqs.get(latestLoc);
			if (domsatTime >= latestDs.getDomsatTime())
			{
				latestDs.setNext(thisLoc);
				latestLoc = thisLoc;
			}
			else // have to start at top of minute and find insertion point
			{
				int prevLoc = minuteStart[prevMin];
				DomsatSeq prevDs = domsatSeqs.get(prevLoc);
				int x = prevLoc;
				DomsatSeq xds = prevDs;
				while(x != -1 && xds.getDomsatTime() <= domsatTime)
				{
					prevLoc = x;
					prevDs = xds;
					x = xds.getNext();
					if (x != -1)
						xds = domsatSeqs.get(x);
				}
	
				// Now prevLoc & prevDs are just before thisDs in time order.
				thisDs.setNext(prevDs.getNext());
				prevDs.setNext(thisLoc);
			}
		}
	}

	/**
	 * Retrieve messages by DOMSAT sequence number range.
	 * @param approxDomsatMsec approximate domsat time of outage.
	 * @param seqStart first missing sequence number
	 * @param seqEnd last missing sequence number
	 * @param msgs return messages by storing them here
	 * @return number of messages stored.
	 */
	public synchronized int getMsgsBySeqNum(long fromDomsatMsec, 
		long toDomsatMsec, int seqStart, int seqEnd, ArrayList<DcpMsg> msgs)
		throws ArchiveUnavailableException
	{
		// Search from 15 seconds before/after the approx time.
		fromDomsatMsec -= 15000L;
		toDomsatMsec += 15000L;

		// Convert 'from' msec value to unix time_t
		int tt = (int)(fromDomsatMsec / 1000L);
		if (tt < startTime)
			tt = startTime;
		int m = (tt - startTime) / 60;
		for(int i = 0; i < 4 && m < 60*24; i++, m++)
			if (minuteStart[m] != -1)
				break;
		int r = 0;
		if (m < 60*24 && minuteStart[m] != -1)
		{
			DomsatSeq domsatSeq = domsatSeqs.get(minuteStart[m]);
//Logger.instance().info("MJM trying HH:MM " + (m/60) + ":" + (m%60) +
//"domsatSeq = " + domsatSeq.toString());
			while(domsatSeq != null && domsatSeq.getDomsatTime()<=toDomsatMsec)
			{
				int sn = domsatSeq.getSeqNum();
				long dt = domsatSeq.getDomsatTime();

				if (dt >= fromDomsatMsec && sn >= seqStart && sn <= seqEnd)
				{
					DcpMsg msg = getMsg(domsatSeq.getDayNum(), 
						domsatSeq.getIndexNum());
					if (msg != null)
					{
//Logger.instance().info("MJM ... Seq " + sn + " Passes! Adding to buffer.");
						msg.setDomsatTime(new Date(domsatSeq.getDomsatTime()));
						msg.setSequenceNum(domsatSeq.getSeqNum());
						msg.flagbits &= (~DcpMsgFlag.MSG_NO_SEQNUM);
						msgs.add(msg);
						r++;
					}
//else Logger.instance().info("MJM ... Seq " + sn + " No Msg!");
				}
//else Logger.instance().info("MJM ... Seq " + sn + " not in range!");
				int x = domsatSeq.getNext();
				domsatSeq = x != -1 ? domsatSeqs.get(x) : null;
			}
		}
		return r;
	}

	private DcpMsg getMsg(short dayNum, int indexNum)
		throws ArchiveUnavailableException
	{
		int startTT = (int)dayNum * SPD;
		MsgPeriodArchive mpa = msgArchive.getPeriodArchive(startTT, false);
		if (mpa == null || mpa.startTime != startTT)
			return null;
		DcpMsgIndex dmi = mpa.getIndexEntrySync(indexNum);
		if (dmi == null)
			return null;
		if (dmi.getDcpMsg() == null)
			mpa.readMessage(dmi);
		return dmi.getDcpMsg();
	}

	/**
	 * Find the first DomsatSeq entry after the passed time.
	 * @param domsatTime the domsat time.
	 * @return the first DomsatSeq entry after the passed time.
	 */
//	public DomsatSeq firstAfterTime(long domsatTime)
//	{
//		int min = (int)((domsatTime % MSEC_PER_DAY) / 60000L);
//	}
}
