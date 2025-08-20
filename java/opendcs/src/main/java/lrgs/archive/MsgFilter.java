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
*  Revision 1.3  2008/12/26 15:47:01  mjmaloney
*  bugfix for sparse forward index search taking to long.
*
*  Revision 1.2  2008/06/10 21:39:52  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2007/12/05 15:46:34  mmaloney
*  dev
*
*  Revision 1.1  2005/06/06 21:15:26  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

import java.util.Date;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpAddress;

/**
Used by the archive search routines to determine if a given message passes
a set of criteria.
*/
public interface MsgFilter
{
	/** @return the 'since' time specified in the filter, or null if none. */
	Date getSinceTime( );
	
	/** @return the 'until' time specified in the filter, or null if none. */
	Date getUntilTime( );
	
	/**
	 * Return true if the passed index passes the filter.
	 * @param mie the message index entry
	 * @return true if the passed index passes the filter.
	 */
	boolean passes( DcpMsgIndex mie );

	/**
	 * @return array of DCP addresses to filter on, or null if this filter
	 * passes all addresses.
	 */
	public DcpAddress[] getDcpAddresses();

	/**
	 * @return true if this session requires output sorted in ascending
	 *  time order. This is the legacy behavior and is slower.
	 */
	public boolean forceAscending();

	/**
	 * @return true if this session wants a 10-second real-time settling delay.
	 * That means when real-time retrievals are being done, the server
	 * will not serve out data until it's at least 10 seconds old.
	 * This allows time for the merging to 'settle'.
	 */
	public boolean realTimeSettlingDelay();
	
	public String getClientName();
}
