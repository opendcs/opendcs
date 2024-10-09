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
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2007/03/06 21:04:47  mmaloney
*  LRGS 6.0 Release Prep
*
*  Revision 1.1  2005/06/28 17:37:00  mjmaloney
*  Java-Only-Archive implementation.
*
*/
package lrgs.common;

import lrgs.statusxml.LrgsStatusSnapshotExt;

/**
  Interface LrgsStatusProvider defines the methods needed by DDS for
  retrieving LRGS status.
*/
public interface LrgsStatusProvider
{
	/** Attach to the status provider. */
	public void attach();

	/** Detach from the status provider. */
	public void detach();

	/**
	 * @return a structure with a snapshot of current status.
	 */
	public LrgsStatusSnapshotExt getStatusSnapshot();

	/** @return true if this LRGS is currently usable, false if not. */
	public boolean isUsable();
}
