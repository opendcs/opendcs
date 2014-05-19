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
*/
package decodes.dcpmon1;

/**
 * Definitions for the binary 'flags' value stored in each XmitRecord
 */
public class XmitRecordFlags
{
	/** This bit set means we don't have real msg. Just the missing notice. */
	public static final int IS_MISSING        = 0x00000001;

	/** This bit set means the message is NOT self-timed (i.e. random) */
	public static final int IS_RANDOM         = 0x00000004;

	/** This bit set means the message had a long preamble (100 baud only) */
	public static final int LONG_PREAMBLE     = 0x00000008;

	/** This bit set means the message is not in one of my groups. */
	public static final int NOT_MY_DCP        = 0x00000010;

	/** This bit set means that decoding failed. */
	public static final int DECODES_FAILED    = 0x00000020;

	/** This is the mask to determine spacecraft. */
	public static final int SC_MASK           = 0x000000c0;

	/** Not a satellite message (check for this value after anding SC_MASK). */
	public static final int SC_NONE           = 0x00000000;

	/** East GOES spacecraft. */
	public static final int SC_EAST           = 0x00000040;

	/** West GOES spacecraft. */
	public static final int SC_WEST           = 0x00000080;

	/** This is the mask to determine baud rate */
	public static final int BAUD_MASK         = 0x00000700;

	/** Unknown Baud Rate */
	public static final int BAUD_UNKNOWN      = 0x00000000;

	/** 100 Baud */
	public static final int BAUD_100          = 0x00000100;

	/** 300 Baud */
	public static final int BAUD_300          = 0x00000200;

	/** 1200 Baud */
	public static final int BAUD_1200         = 0x00000300;

	// Leaving 4 more possible values for future baud rates
	public static final int BAUD_OTHER1       = 0x00000400;
	public static final int BAUD_OTHER2       = 0x00000500;
	public static final int BAUD_OTHER3       = 0x00000600;
	public static final int BAUD_OTHER4       = 0x00000700;

	/** Carrier times considered accurate to millisecond. */
	public static final int CARRIER_TIME_MSEC = 0x00000800;

	/** Mask for the 8-bit HDR flag word at start of each msg. */
	public static final int HDR_FLAG_MASK     = 0x000ff000;
	/** # bits to shift HDR flag bits */
	public static final int HDR_FLAG_SHIFT    = 12;

	public static int getBaudRate(XmitRecord xr)
	{
		int flags = xr.getFlags() & BAUD_MASK;
		return flags == BAUD_100 ? 100 : 
		       flags == BAUD_300 ? 300 :
		       flags == BAUD_1200 ? 1200 : 300;
	}

	public static char getPreamble(XmitRecord xr)
	{
		if ((xr.getFlags() & LONG_PREAMBLE) != 0)
			return 'L';
		return 'S';
	}

	public static boolean testFlag(XmitRecord xr, int flag)
	{
		return (xr.getFlags() & flag) != 0;
	}

	public static byte getHdrFlagByte(XmitRecord xr)
	{
		return (byte)((xr.getFlags() & HDR_FLAG_MASK) >> HDR_FLAG_SHIFT);
	}

	public static void setHdrFlagByte(XmitRecord xr, byte f)
	{
		int x = xr.getFlags();
		xr.clearFlags();
		xr.addFlags(x | (((int)f&0xff) << HDR_FLAG_SHIFT));
	}
}
