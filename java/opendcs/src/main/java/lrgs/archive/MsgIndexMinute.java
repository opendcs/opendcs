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
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2005/06/24 15:57:28  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.1  2005/06/06 21:15:27  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

/**
A period archive stores one of these for each minute in the period. It is
used to speed up time-range searches.
*/
public class MsgIndexMinute
{
	/**
	 * Index number for first entry archived in this minute. -1 if none.
	 */
	int startIndexNum;
	
	/**
	 * Index number of last index archived in this minute, or -1 if none.
	 */
	int reserved;
	
	/**
	 * Lowest (earliest) DAPS time of any message within this minute.
	 * Integer.MAX_VALUE means unassigned.
	 */
	int oldestDapsTime;

	/** Constructor */
	public MsgIndexMinute()
	{
		this.startIndexNum = -1;
		this.reserved= -1;
		this.oldestDapsTime = Integer.MAX_VALUE;
	}

	/** @return true if this minute is empty. */
	public boolean isEmpty()
	{
		return oldestDapsTime == Integer.MAX_VALUE;
	}
}
