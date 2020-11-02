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
*  Revision 1.5  2009/10/26 14:08:30  mjmaloney
*  Don't hangup on large seq# gap.
*
*  Revision 1.4  2009/09/09 13:39:37  mjmaloney
*  timeout recovery for DPC
*
*  Revision 1.3  2009/07/21 17:17:40  mjmaloney
*  dev
*
*  Revision 1.2  2008/06/10 21:39:52  cvs
*  dev
*
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/10/10 19:44:24  mmaloney
*  dev
*
*  Revision 1.3  2005/08/09 18:20:00  mjmaloney
*  dev
*
*  Revision 1.2  2005/08/08 18:09:41  mjmaloney
*  HW Interface complete.
*
*  Revision 1.1  2005/08/07 20:42:46  mjmaloney
*  Created.
*
*/
package lrgs.domsatrecv;

/**
This defines the interface to code controlling the DOMSAT interface.
*/
public interface DomsatHardware
{
	/** 
	 * One-time initialization. 
	 * @return 0 if OK, -1 if error.
	 */
	public int init();

	/** 
	 * Enable/Disable the interface. 
	 * If not successful, generate appropriate events & return false.
	 * @return true if successful, -1 if not.
	 */
	public boolean setEnabled(boolean enabled);

	/**
	 * Retrieve a packet from the interface.
	 * @return length (>0) on success, 0 if no data available, 
	 *         -1 if recoverable packet error, -2 if hardware interface error.
	 */
	public int getPacket(byte[] packetbuf);

	/**
	 * @return last message for last error asserted by this device.
	 */
	public String getErrorMsg();

	/** Shuts down the interface. */
	public void shutdown();
	
//	/** Resets interface after an anomaly, like impossibly large gap. */
//	public void reset();
	
	/** gives the unit a chance to do something on timeout. */
	public void timeout();

}

