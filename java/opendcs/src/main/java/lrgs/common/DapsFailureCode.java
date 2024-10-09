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
*  Revision 1.3  2010/04/30 14:52:16  mjmaloney
*  Refactored as an enum, and added the DCP Session Codes.
*
*  Revision 1.2  2008/09/16 13:48:48  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/06/09 20:44:33  mjmaloney
*  Working on implementation of Java-Only Archive
*
*/
package lrgs.common;

public enum DapsFailureCode
{
	GOOD_QUAL('G', "Good Quality"),
	PARITY_ERR('?', "Parity Errors"),
	WRONG_CHAN('W', "Received Wrong Channel"),
	DUPLICATE('D', "Duplicate"),
	ADDR_FIXED('A', "DCP Address Fixed"),
	BAD_ADDR('B', "Bad DCP Address"),
	OVERLAP_TIME('T', "Outside Timeslice"),
	UNEXPECTED('U', "Unexpected Message"),
	MISSING('M', "Missing Expected Message"),
	INVALID_ADDR('I', "Invalid DCP Address"),
	PDT_INCOMPLETE('N', "PDT Record Incomplete"),
	BAD_SIG_QUAL('Q', "Bad Signal Quality"),
	EXCESS_CARRIER('C', "Excessive Carrier"),
	LOW_SIGNAL('S', "Low Signal Strength"),
	EXCESS_FREQ_OFF('F', "Excessive Freq Offset"),
	BAD_MOD_IDX('X', "Bad Mod Index"),
	LOW_BATT('V', "Low Battery"),

	// the following codes were added for the DCP Session Interfaces
	// 'G' is used here for a good quality message
	// '?' is also used here for a message which may have failed a consistency check.
	BAD_CONFIG('b', "Bad session config"),
	AUTH_FAILED('a', "Authentication failure"), // Could not login to the DCP
	DEVICE_FAILED('d', "Device connection failure"),     // E.g. failed to open socket
	CALL_FAILED('l', "Logical connection failure"), // E.g. modem or radio connection failure
	PROTOCOL_FAILED('p', "Protocol failure"),
	SESSION_SUCCESS('s', "Session success");
	
	private char code;
	private String description;
	
	DapsFailureCode(char code, String description)
	{
		this.code = code;
		this.description = description;
	}

	public char getCode() { return code; }
	public String getDescription() { return description; }
	
	/**
	  Given a character code, return an explanatory string.
	  @param code the code
	  @return explanatory string.
	*/
	public static String failureCode2string(char code)
	{
		for(DapsFailureCode dfc : values())
			if (dfc.code == code)
				return dfc.description;
		
		return "Unknown code";
	}
}
