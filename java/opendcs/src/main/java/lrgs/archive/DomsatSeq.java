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

import java.util.Date;
import ilex.util.IDateFormat;

/**
This class holds a single DOMSAT Sequence Number association.
*/
public class DomsatSeq
{
	/** Approximate time message was received over DOMSAT. */
	private long domsatTime;

	/** DOMSAT Sequence Number */
	private int seqNum;

	/** Day number (day 0 = Jan 1, 1970) - specifies an archive file. */
	private short dayNum;

	/** Index number within that day's archive file. */
	private int indexNum;

	/** Internal linked list -- index of the next (time order) DomsatSeq */
	private int nextDomsatSeq;

	public String toString()
	{
		return "DomsatSeq seqNum=" + seqNum + ", dayNum=" + dayNum
			+ ", idxNum=" + indexNum + ", domsatTime="
			+ IDateFormat.toString(new Date(domsatTime), false);
	}


	/** Default Constructor */
	public DomsatSeq()
	{
		domsatTime = 0L;
		seqNum = -1;
		dayNum = 0;
		indexNum = -1;
		nextDomsatSeq = -1;
	}

	/** Constructor for when all values are known. */
	public DomsatSeq(long domsatTime, int seqNum, short dayNum,
		int indexNum, int nextDomsatSeq)
	{
		this.domsatTime = domsatTime;
		this.seqNum = seqNum;
		this.dayNum = dayNum;
		this.indexNum = indexNum;
		this.nextDomsatSeq = nextDomsatSeq;
	}


	public void setDomsatTime(long x)
	{
		domsatTime = x;
	}
	public void setSeqNum(int x)
	{
		seqNum = x;
	}
	public void setDayNum(short x)
	{
		dayNum = x;
	}
	public void setIndexNum(int x)
	{
		indexNum = x;
	}
	public void setNext(int x)
	{
		nextDomsatSeq = x;
	}



	public long getDomsatTime()
	{
		return domsatTime;
	}
	public int getSeqNum()
	{
		return seqNum;
	}
	public short getDayNum()
	{
		return dayNum;
	}
	public int getIndexNum()
	{
		return indexNum;
	}
	public int getNext()
	{
		return nextDomsatSeq;
	}
}
