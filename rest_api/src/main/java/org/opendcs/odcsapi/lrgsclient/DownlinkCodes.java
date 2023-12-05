package org.opendcs.odcsapi.lrgsclient;


public class DownlinkCodes
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
	public static final int DL_DDS_SECONDARY  = 16;
	/** DCP Session Manager */
	public static final int DL_SESSIONMGR = 17;
	/** EUMETSAT File Monitor (European LRIT) */
	public static final int DL_EUMETSAT = 18;
	/** EDL File Monitor */
	public static final int DL_EDL = 19;
	
	public static String type2str(int t)
	{
		switch(t)
		{
		case DL_UNUSED: return "UNUSED";
		case DL_DOMSAT: return "DOMSAT";
		case DL_NETBAK: return "NETBAK";
		case DL_RESERVED: return "RESERVED";
		case DL_DRGS: return "DRGS";
		case DL_DDS: return "DL_DDS";
		case DL_LRIT: return "HRIT";
		case DL_DDSCON: return "DDSCON";
		case DL_DRGSCON: return "DRGSCON";
		case DL_NOAAPORTCON: return "NOAAPORT";
		case DL_GR3110: return "GR3110";
		case DL_IRIDIUM: return "IRIDIUM";
		case DL_NETWORKDCP: return "NET-DCP";
		case DL_NETDCPPOLL: return "NET-DCP-POLL";
		case DL_NETDCPCONT: return "NET-DCP-CONT";
		case DL_DDS_SECONDARY: return "DDS-SECONDARY";
		case DL_SESSIONMGR: return "DCP_SESSION_MGR";
		case DL_EUMETSAT: return "EUMETSAT";
		case DL_EDL: return "EDL";
		default: return "UNKNOWN";
		}
	}

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
	
	public static String statcode2str(int statcode)
	{
		switch(statcode)
		{
		case DL_DISABLED: return "Disabled";
		case DL_INIT: return "Initializing";
		case DL_ACTIVE: return "Active";
		case DL_TIMEOUT: return "Timeout";
		case DL_ERROR: return "Error";
		
		default: return "UNKNOWN";
		}
	}

	/**Primary Group. */
	public static final String PRIMARY = "PRIMARY";
	
	/**Secondary GROUP. */
	public static final String SECONDARY = "SECONDARY";

}
