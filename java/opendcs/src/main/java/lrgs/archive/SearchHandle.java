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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsgIndex;

/**
This object holds variables that are opaque to the application. They
are used by the archiving classes to keep track of the progress of a
search.
*/
public class SearchHandle
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		{
			log.warn("SearchHandle buf problem: buffer contains null at index={}", (nextIdxBufNum-1));
		}
		return ret;
	}

	/**
	 * Adds an index to the internal buffer.
	 * @param idx the index
	 */
	public void addIndex(DcpMsgIndex idx)
	{
		if (idx==null)
		{
			log.warn("SearchHandle addIndex called with null!");
		}
		idxBuf[idxBufFillLength++] = idx;
	}
}
