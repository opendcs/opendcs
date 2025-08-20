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
*
*  $Log$
*  Revision 1.4  2012/05/15 15:13:02  mmaloney
*  Null pointer bug fix.
*
*  Revision 1.3  2008/09/05 13:03:34  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.2  2008/08/19 15:04:37  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/02/14 15:38:20  mmaloney
*  Implemented Outage Recovery
*
*  Revision 1.4  2007/01/12 21:45:18  mmaloney
*  Archive file changes for LRGS Version 6.
*
*  Revision 1.3  2006/11/11 16:11:53  mmaloney
*  dev
*
*  Revision 1.2  2006/10/18 18:11:48  mmaloney
*  dev
*
*  Revision 1.1  2005/12/14 21:20:24  mmaloney
*  Fix the 'synchronizing' in MsgArchive, particularly at GMT midnight.
*
*/
package lrgs.archive;

import java.util.Date;
import java.util.Vector;
import java.util.Iterator;

import ilex.util.Logger;
import lrgs.common.DcpMsgIndex;

public class PeriodArchiveVec
{
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
//Logger.instance().info("PeriodArchiveVec.getPeriodArchive("
//+ sinceSec + ", " + earliest + ") earliest archive starts at "
//+ mpa.startTime);

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
				Logger.instance().info(module + " Deleting old archive "
					+ arc.myname + ", start=" + arc.startTime
					+ ", cutoff=" + cutoffStr + " (" + cutoffStr + ")");
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
