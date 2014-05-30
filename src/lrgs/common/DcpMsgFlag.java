/*
*  $Id$
*
*  Proprietary version of LRGS DcpMsgFlag object.
*  This version will overwrite the open-source when we build the jar.
*/
package lrgs.common;

/**
  Contains static methods and constants relating to the 16-bit flag word
  present in a DCP message and index entry.
  These must be kept in sync with #define's in lrgscommon/dcp_msg_flag.h
*/
public class DcpMsgFlag
{
	public static final int MSG_PRESENT       = 0x0001;

	/** This msg should be ignored. */
	public static final int MSG_DELETED       = 0x0002;

	public static final int SRC_MASK          = 0x001C;
	public static final int SRC_DOMSAT        = 0x0000;
	public static final int SRC_NETDCP        = 0x0004;
	public static final int SRC_DRGS          = 0x0008;
	public static final int SRC_NOAAPORT      = 0x000C;
	public static final int SRC_LRIT          = 0x0010;
	public static final int SRC_DDS           = 0x0014;
	public static final int SRC_IRIDIUM       = 0x0018;
	public static final int SRC_OTHER         = 0x001C;

	public static final int FORCE_SAVE        = 0x0020;
	
	/** Message does NOT have _DOMSAT_ sequence num */
	public static final int MSG_NO_SEQNUM     = 0x0040;
	public static final int DUP_MSG           = 0x0080;

	/** Means Carrier times were estimated on DRGS link. */
	public static final int CARRIER_TIME_EST  = 0x0100;

	/** Message flagged as binary */
	public static final int BINARY_MSG        = 0x0200;
	
	/** Mask for baud-rate bits */
	public static final int BAUD_MASK         = 0x0C00;
	/** Message baud rate is unknown */
	public static final int BAUD_UNKNOWN      = 0x0000;
	/** Message baud rate is 100 */
	public static final int BAUD_100          = 0x0400;
	/** Message baud rate is 300 */
	public static final int BAUD_300          = 0x0800;
	/** Message baud rate is 1200 */
	public static final int BAUD_1200         = 0x0C00;

	/** Unused bits */
	public static final int HAS_CARRIER_TIMES = 0x1000;

//	/** Coopted for extended msg types (see below). */
//	public static final int MSG_TYPE_EXT      = 0x2000;

	/** DRGS Address was Corrected from original. */
	public static final int ADDR_CORRECTED    = 0x4000;

	/** Bit indicating GOES (default for legacy) or Iridium Msg. */
	public static final int MSG_TYPE_MASK            = 0x0003A000;
	// non-specific any GOES:
	public static final int MSG_TYPE_GOES            = 0x00000000;
	public static final int MSG_TYPE_IRIDIUM         = 0x00008000;
	public static final int MSG_TYPE_NETDCP          = 0x00002000;
	public static final int MSG_TYPE_OTHER           = 0x0000A000;

	// For flag rev 3, new types are added above 16 bits.
	public static final int MSG_TYPE_GOES_ST         = 0x00010000;
	public static final int MSG_TYPE_GOES_RD         = 0x00020000;
	// Goes international
	public static final int MSG_TYPE_GOES_INT        = 0x00030000;
	
	// Indicates a binary message that has detected errors
	public static final int HAS_BINARY_ERRORS        = 0x00040000;
	
	/** Revision number for interpreting flag bits */
	static public int myFlagRev = 0x4b;

	
	/** @return true if message is GOES */
	public static boolean isGOES(int f) 
	{
		int fm = f & MSG_TYPE_MASK;
		return fm == MSG_TYPE_GOES || fm == MSG_TYPE_GOES_ST
			|| fm == MSG_TYPE_GOES_RD || fm == MSG_TYPE_GOES_INT;
	}

	/** @return true if message is GOES Self Timed */
	public static boolean isGoesST(int f) 
	{
		int fm = f & MSG_TYPE_MASK;
		// If non-specific GOES, we have to match
		return fm == MSG_TYPE_GOES || fm == MSG_TYPE_GOES_ST;
	}

	/** @return true if message is GOES Random */
	public static boolean isGoesRD(int f) 
	{
		int fm = f & MSG_TYPE_MASK;
		// if non-specific GOES, we have to match
		return fm == MSG_TYPE_GOES || fm == MSG_TYPE_GOES_RD;
	}

	public static boolean isIridium(int f) 
	{ return (f & MSG_TYPE_MASK) == MSG_TYPE_IRIDIUM; }

	public static boolean isNetDcp(int f) 
	{ return (f & MSG_TYPE_MASK) == MSG_TYPE_NETDCP; }
	
	/**
	  Get data source name given a flag value.
	  @param v the flag value.
	  @return data source name given a flag value, or null if not recognized
	*/
	public static String sourceValue2Name(int v)
	{
		if ((v & MSG_TYPE_MASK) == MSG_TYPE_GOES_ST)
			return "GOES_SELFTIMED";
		else if ((v & MSG_TYPE_MASK) == MSG_TYPE_GOES_RD)
			return "GOES_RANDOM";

		switch(v & SRC_MASK)
		{
		case SRC_DOMSAT: return "DOMSAT";
		case SRC_NETDCP: return "NETDCP";
		case SRC_DRGS: return "DRGS";
		case SRC_NOAAPORT: return "NOAAPORT";
		case SRC_LRIT: return "LRIT";
		case SRC_OTHER: return "OTHER";
		case SRC_DDS: return "DDS";
		case SRC_IRIDIUM: return "IRIDIUM";
		}
		
		return null;
	}

	/**
	  Get data source flag value given a name.
	  @param nm the data source name.
	  @return data source flag value given a name, or -1 if not recognized.
	*/
	public static int sourceName2Value(String nm)
	{
		if (nm.equalsIgnoreCase("MASK")) return SRC_MASK;
		if (nm.equalsIgnoreCase("DOMSAT")) return SRC_DOMSAT;
		if (nm.equalsIgnoreCase("NETDCP")) return SRC_NETDCP;
		if (nm.equalsIgnoreCase("DRGS")) return SRC_DRGS;
		if (nm.equalsIgnoreCase("NOAAPORT")) return SRC_NOAAPORT;
		if (nm.equalsIgnoreCase("LRIT")) return SRC_LRIT;
		if (nm.equalsIgnoreCase("DDS")) return SRC_DDS;
		if (nm.equalsIgnoreCase("IRIDIUM")) return SRC_IRIDIUM;
		if (nm.equalsIgnoreCase("OTHER")) return SRC_OTHER;
		if (nm.equalsIgnoreCase("GOES_SELFTIMED")) return MSG_TYPE_GOES_ST;
		if (nm.equalsIgnoreCase("GOES_RANDOM")) return MSG_TYPE_GOES_RD;
		return -1;
	}

	public static void setFlagRev(int flagRev) { flagRev = myFlagRev; }

	/** @return true if the flag indicates a msg with accurate carrier times. */
	public static boolean hasAccurateCarrier(int f)
	{
		return (f & HAS_CARRIER_TIMES) != 0 && (f & CARRIER_TIME_EST) == 0;
	}
}
