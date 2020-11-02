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
*  Revision 1.2  2009/09/25 15:28:29  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2006/05/23 19:29:35  mmaloney
*  dev
*
*  Revision 1.3  2005/07/21 15:17:44  mjmaloney
*  LRGS-5.0
*
*  Revision 1.2  2005/06/09 20:44:33  mjmaloney
*  Working on implementation of Java-Only Archive
*
*  Revision 1.1  2005/06/06 21:15:25  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

/**
MsgArchive keeps a hash of these object so that it can keep track of the
last message received by a particular platform. (The hash is on platform.)
*/
public class IndexPtr
{
	/**
	 * Start time of period. Uniquely identifies an Index File.
	 */
	int indexFileStartTime;
	
	/**
	 * Index number within the file.
	 */
	int indexNumber;
	
	/**
	 * The platform (i.e. DAPS) time stamp of the message.
	 */
	int msgTime;
	
	/**
	 * Flag bits indicating wither this is real msg or daps status.
	 * @see lrgs.common.DapsFailureCode
	 */
	int flagBits;

	/**
	 * The length of the message.
	 */
	int msgLength;

	/**
	 * Constructor.
	 * @param fst file start time (unit time_t) points to particular index file
	 * @param indexNumber number of the index within that file
	 * @param msgTime (Unix time_t) platform send time-stamp
	 */
	public IndexPtr(int fst, int indexNumber, int msgTime, int flagBits, 
		int msgLength)
	{
		indexFileStartTime = fst;
		this.indexNumber = indexNumber;
		this.msgTime = msgTime;
		this.flagBits = flagBits;
		this.msgLength = msgLength;
	}
	
	public String toString()
	{
		return "FileStart=" + indexFileStartTime + ", idxNum=" + 
			indexNumber + ", msgTime=" + msgTime;
	}
}
