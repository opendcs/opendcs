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
*  Revision 1.2  2015/01/06 16:09:31  mmaloney
*  First cut of Polling Modules
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.11  2011/09/10 10:24:58  mmaloney
*  Implement EUMETSAT LRIT File Capability.
*
*  Revision 1.10  2010/01/29 21:25:17  mjmaloney
*  DCP Communications Classes Prototype
*
*  Revision 1.9  2010/01/20 18:29:05  shweta
*  Updated Real Time Status Screen with Secondary group
*
*  Revision 1.8  2010/01/07 21:38:46  shweta
*  Enhancements for multiple DDS Receive  group.
*
*  Revision 1.7  2008/09/23 15:27:58  mjmaloney
*  network DCPs
*
*  Revision 1.6  2008/09/21 18:01:11  mjmaloney
*  network DCPs
*
*  Revision 1.5  2008/09/21 16:08:20  mjmaloney
*  network DCPs
*
*  Revision 1.4  2008/09/08 19:14:03  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.3  2008/07/07 12:03:08  mjmaloney
*  dev
*
*  Revision 1.2  2008/04/19 15:06:33  cvs
*  added TW Sample
*
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2007/02/08 16:34:56  mmaloney
*  dev
*
*  Revision 1.6  2007/01/16 19:27:50  mmaloney
*  Implementing LRGS 6
*
*  Revision 1.5  2005/07/15 18:15:05  mjmaloney
*  Implemented quality logging.
*
*  Revision 1.4  2005/06/30 15:15:28  mjmaloney
*  Java Archive Development.
*
*  Revision 1.3  2005/06/28 17:37:02  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.2  2005/06/23 15:47:19  mjmaloney
*  Java archive search algorithms.
*
*  Revision 1.1  2005/06/09 20:44:34  mjmaloney
*  Working on implementation of Java-Only Archive
*
*/
package lrgs.lrgsmain;

import lrgs.apistatus.DownLink;

/**
All LRGS downlink interfaces (i.e. any process that supplies messages to
be archived) must implement this interface.
*/
public interface LrgsInputInterface
{
	/*
	 * The following constants are used in the 'type' field of a DownLink
	 * status structure.
	 */
	/** Unused slot */
	public static final int DL_UNUSED   = 0;
	/** DOMSAT Downlink */
	public static final int DL_DOMSAT   = 1;
	/** Legacy Network Backup Protocol Client */
	public static final int DL_NETBAK   = 2;
	/** Unused */
	public static final int DL_RESERVED = 3;
	/** GOES DRGS */
	public static final int DL_DRGS     = 4;
	/** DDS Client */
	public static final int DL_DDS      = 5;
	/** NOAAPORT */
	public static final int DL_NOAAPORT = 6;
	/** GOES LRIT */
	public static final int DL_LRIT     = 7;
	/** A specific DDS Receive connection. */
	public static final int DL_DDSCON   = 8;
	/** A specific DRGS Receive connection. */
	public static final int DL_DRGSCON  = 9;
	/** A specific NOAAPORT Receive connection. */
	public static final int DL_NOAAPORTCON = 10;
	/** A GR3110 connection. */
	public static final int DL_GR3110 = 11;
	/** Iridium SBD receiver. */
	public static final int DL_IRIDIUM = 12;
	/** DAMS-NT Network DCP. */
	public static final int DL_NETWORKDCP = 13;
	/** Polling thread */
	public static final int DL_NETDCPPOLL = 14;
	/** Continuous thread */
	public static final int DL_NETDCPCONT = 15;
	/** DDS Secondary Client */
	public static final int DL_DDS_SECONDRAY  = 16;
	/** DCP Session Manager */
	public static final int DL_SESSIONMGR = 17;
	/** EUMETSAT File Monitor (European LRIT) */
	public static final int DL_EUMETSAT = 18;
	/** EDL File Monitor */
	public static final int DL_EDL = 19;

	/** Type Name for DOMSAT */
	public static final String DL_DOMSAT_TYPESTR = "DOMSAT";

	/** Type Name for DRGS */
	public static final String DL_DRGS_TYPESTR = "DRGS";

	/** Type Name for DDS */
	public static final String DL_DDS_TYPESTR = "DDS";

	/** Type Name for DDS */
	public static final String DL_DDSMAIN_TYPESTR = "DDS-MAIN";

	/** Type Name for LRIT */
	public static final String DL_LRIT_TYPESTR = "HRIT";

	/** Type Name for DDSCON */
	public static final String DL_DDSCON_TYPESTR = "DDSCON";

	/** Type Name for DRGSCON */
	public static final String DL_DRGSCON_TYPESTR = "DRGSCON";

	/** Type Name for NOAAPORT */
	public static final String DL_NOAAPORT_TYPESTR = "NOAAPORT";

	/** Type name for legacy GR3110 DRGS interface */
	public static final String DL_GR3110_TYPESTR = "GR3110";

	/** Type name for Iridium interface */
	public static final String DL_IRIDIUM_TYPESTR = "IRIDIUM";

	/** Type name for Network DCP */
	public static final String DL_NETWORKDCP_TYPESTR = "NET-DCP";
	/** Polling thread */
	public static final String DL_NETDCPPOLL_TYPESTR = "NET-DCP-POLL";
	/** Continuous thread */
	public static final String DL_NETDCPCONT_TYPESTR = "NET-DCP-CONT";
	/** DCP Session Manager */
	public static final String DL_SESSIONMGR_TYPESTR = "DCP_SESSION_MGR";
	public static final String DL_EUMETSAT_TYPESTR = "EUMETSAT";
	public static final String DL_EDL_TYPESTR = "EDL";

	/*
	 * Definitions for numeric status codes.
	 * NOTE: These MUST be kept in sync with the definitions in LrgsStatus.idl
	 * int the lrgs/apistatus package!!
	 */
	/** Downlink is disabled. */
	public static final int DL_DISABLED  = 0;

	/** Downlink is initializing. */
	public static final int DL_INIT      = 1;

	/** Downlink is active. */
	public static final int DL_ACTIVE    = 2;

	/** Downlink is timed-out. */
	public static final int DL_TIMEOUT   = 3;

	/** Downlink is in an error state. */
	public static final int DL_ERROR     = 4;

	/** means that the status is in a string. */
	public static final int DL_STRSTAT = 5;

	/**Primary Group. */
	public static final String PRIMARY = "PRIMARY";
	
	/**Secondary GROUP. */
	public static final String SECONDARY = "SECONDARY";
	
	/**
	 * @return the type of this input interface.
	 */
	public int getType();

	/**
	 * All inputs must keep track of their 'slot', which is a unique index
	 * into the LrgsMain's vector of all input interfaces.
	 * @param slot the slot number.
	 */
	public void setSlot(int slot);
	
	/** @return the slot number that this interface was given at startup */
	public int getSlot();

	/**
	 * @return the name of this interface.
	 */
	public String getInputName(); 

	/**
	 * Initializes the interface.
	 * May throw LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException;

	/**
	 * Shuts down the interface.
	 * Any errors encountered should be handled within this method.
	 */
	public void shutdownLrgsInput();

	/**
	 * Enable or Disable the interface. 
	 * The interface should only attempt to archive messages when enabled.
	 * @param enabled true if the interface is to be enabled, false if disabled.
	 */
	public void enableLrgsInput(boolean enabled);

	/**
	 * @return true if this downlink can report a Bit Error Rate.
	 */
	public boolean hasBER();

	/**
	 * @return the Bit Error Rate as a string.
	 */
	public String getBER();

	/**
	 * @return true if this downlink assigns a sequence number to each msg.
	 */
	public boolean hasSequenceNums();

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode();

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus();

	/**
	 * @return the unique data source ID for this input interface.
	 */
	public int getDataSourceId();
	
	/** @return true if this interface receives APR messages */
	public boolean getsAPRMessages();
	
	/** @return the group for the DdsRecv Connection (primary or secondary)*/
	public String getGroup() ;
}
