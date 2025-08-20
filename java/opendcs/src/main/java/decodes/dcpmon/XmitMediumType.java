package decodes.dcpmon;

import lrgs.common.DcpMsgFlag;

public enum XmitMediumType
{
	GOES('G'),
	LOGGER('L'),
	IRIDIUM('I');

	char code;
	private XmitMediumType(char code)
	{
		this.code = code;
	}
	
	/** Derive the medium type char from the flags word. */
	public static XmitMediumType flags2type(int flags)
	{
		if (DcpMsgFlag.isGOES(flags))
			return GOES;
		else if (DcpMsgFlag.isIridium(flags))
			return IRIDIUM;
		else
			return LOGGER;
			
	}
	
	/** Derive the medium type char from a string transport medium type (e.g. from a netlist). */
	public static XmitMediumType transportMediumType2type(String mediumType)
	{
		mediumType = mediumType.toLowerCase();
		if (mediumType.startsWith("goes"))
			return GOES;
		else if (mediumType.startsWith("iridium"))
			return IRIDIUM;
		else
			return LOGGER;
}
	
	public char getCode() { return code; }
}
