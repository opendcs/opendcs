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
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import lrgs.common.DcpMsgIndex;

public class PeriodArchiveVec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static final String module = "ArchiveVec";

	private Vector<MsgPeriodArchive> avec;

	public PeriodArchiveVec()
	{
		avec = new Vector<MsgPeriodArchive>();
	}

	public synchronized void insert(MsgPeriodArchive arc)
	{
		int pos = 0;
		for(; pos<avec.size(); pos++)
		{
			MsgPeriodArchive mpa = avec.get(pos);
			if (arc.startTime < mpa.startTime)
				break;
		}
		avec.add(pos, arc);
	}

	public synchronized void closeAll()
	{
		for(Iterator<MsgPeriodArchive> it = avec.iterator(); it.hasNext(); )
		{
			MsgPeriodArchive mpa = it.next();
			mpa.close();
		}
	}

	public synchronized void add(MsgPeriodArchive arc)
	{
		avec.add(arc);
	}

	synchronized MsgPeriodArchive findByStart(int startTime)
	{
		for(MsgPeriodArchive mpa : avec)
			if (mpa.startTime == startTime)
				return mpa;
		return null;
	}

	/**
	 * Return the earliest period containing the specified time.
	 * @param sinceSec the time to search for as a unix time_t.
	 * @param earliest true to return earliest period if sinceSec
	 *        is before all periods.
	 */
	public synchronized MsgPeriodArchive getPeriodArchive(int sinceSec,
		boolean earliest)
	{
		if (avec.size() == 0)
			return null;

		MsgPeriodArchive mpa = avec.get(0);

		// Find the earliest period that contains since.
		// Assume archives are in time order in the vector.
		if (sinceSec < mpa.startTime)
			// before the earliest period -- don't have it!
			return earliest ? mpa : null;

		for(Iterator<MsgPeriodArchive> it = avec.iterator(); it.hasNext(); )
		{
			mpa = it.next();
			if (sinceSec < (mpa.startTime+MsgPeriodArchive.periodDuration))
				return mpa;
		}

		// Since is past all of the periods, return the last in list.
		return mpa;
	}

	public synchronized void checkpointAll(int cutoffTT, String cutoffStr)
	{
		for(Iterator<MsgPeriodArchive> it = avec.iterator(); it.hasNext(); )
		{
			MsgPeriodArchive arc = it.next();
			if (arc.startTime < cutoffTT)
			{
				log.info(" Deleting old archive {}, start={}, cutoff={}({})",
						 arc.myname, arc.startTime, cutoffStr, cutoffTT);
				arc.delete();
				it.remove();
			}
			else
			{
				arc.checkpoint();
			}
		}
	}

	/**
	 * @return total number of messages in all archives.
	 */
	public synchronized int getTotalMessageCount()
	{
		int total = 0;
		for(MsgPeriodArchive arc : avec)
			total += arc.getNumIndexes();
		return total;
	}

	/**
	 * @return Unix time_t message time stamp of oldest message.
	 */
	public synchronized int getOldestDapsTime()
	{
		if (avec.size() > 0)
		{
			MsgPeriodArchive arc = avec.get(0);
			DcpMsgIndex idx = arc.getIndexEntrySync(0);
			if (idx == null)
				return 0;
			Date d = idx.getXmitTime();
			if (d == null)
				return 0;
			return (int)(d.getTime()/1000L);
		}
		return 0;
	}
}