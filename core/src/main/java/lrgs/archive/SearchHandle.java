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
*
*  $Log$
*  Revision 1.4  2008/09/15 17:55:29  mjmaloney
*  dev
*
*  Revision 1.3  2008/09/12 19:30:26  mjmaloney
*  dev
*
*  Revision 1.2  2008/09/05 13:03:34  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/11/21 19:13:41  mmaloney
*  LRGS 5.4 prep
*
*  Revision 1.3  2005/06/28 17:37:00  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.2  2005/06/23 15:47:16  mjmaloney
*  Java archive search algorithms.
*
*  Revision 1.1  2005/06/06 21:15:27  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

import lrgs.common.DcpMsgIndex;

import ilex.util.Logger;

/**
This object holds variables that are opaque to the application. They
are used by the archiving classes to keep track of the progress of a 
search.
*/
public class SearchHandle
{
	/** Indicates the method to be used in the search (opaque to app) */
	int searchMethod;

	/** Undefined search method */
	public static final int SM_UNDEFINED   = 0;

	/** Search method for forward search of index file from since to until */
	public static final int SM_INDEXSEARCH = 1;

	/** Search method for backward search from dcp specific index pointer */
	public static final int SM_BACKREF     = 2;

	/** Uniquely specifies a period archive */
	int periodStartTime;
	
	/** The next index number to return in that archive */
	int nextIndexNum;
	
	/** For standard searches, this is the minute number currently used. */
	int minIdx;

	/** Filter being used in this search */
	MsgFilter filter;
	
	/** For reverse ptr search, index of current address being retrieved. */
	int curaddr;

	/** An opaque variable used by the searching algorithm. */
	Object methodVar;

	/**
	 * Each handle contains a buffer of indexes. The fill-length is the
	 * number of indexes currently in the buffer. The nextIdxNum is the
	 * next one to retrieve. This facilitates the 'getNextPassingIndex'
	 * method used by DDS commands.
	 */
	DcpMsgIndex[] idxBuf;

	/** The number of indexes to allocate in the buffer. */
	public static final int INDEX_BUF_MAX = 64;
	
	/**
	 * Number of valid indexes currently in the buffer.
	 */
	int idxBufFillLength;
	
	/**
	 * Next index in the buffer to retrieve from.
	 */
	int nextIdxBufNum;

	/** Default constructor. */
	public SearchHandle(MsgFilter filter)
	{
		searchMethod = SM_UNDEFINED;
		methodVar = null;
		periodStartTime = 0;
		nextIndexNum = -1;
		minIdx = -1;
		this.filter = filter;
		curaddr = -1;

		idxBuf = new DcpMsgIndex[INDEX_BUF_MAX];
		flushBuffer();
	}

	/**
	 * Empties the cache and resets the pointers.
	 */
	public void flushBuffer()
	{
		idxBufFillLength = 0;
		nextIdxBufNum = 0;
		for(int i=0; i<INDEX_BUF_MAX; i++)
			idxBuf[i] = null;
	}

	public boolean isEmpty()
	{
		return nextIdxBufNum >= idxBufFillLength;
	}

	/** @return remaining capacity, i.e. number of indexes that can be held. */
	public int capacity()
	{
		return INDEX_BUF_MAX - idxBufFillLength;
	}

	/**
	 * Return next index in buffer, or null if none.
	 * @return next index in buffer, or null if none.
	 */
	public DcpMsgIndex getNextIndex()
	{
		if (nextIdxBufNum >= idxBufFillLength)
		{
			flushBuffer();
			return null;
		}
		DcpMsgIndex ret = idxBuf[nextIdxBufNum];
		idxBuf[nextIdxBufNum++] = null;
if (ret == null)
Logger.instance().warning("SearchHandle buf problem: buffer contains null at index=" + (nextIdxBufNum-1));
		return ret;
	}

	/**
	 * Adds an index to the internal buffer.
	 * @param idx the index
	 */
	public void addIndex(DcpMsgIndex idx)
	{
if (idx==null)
Logger.instance().warning("SearchHandle addIndex called with null!");
//else
//Logger.instance().info("Adding index[" + idxBufFillLength + "] to handle: " + idx.toString());
		idxBuf[idxBufFillLength++] = idx;
	}
}
