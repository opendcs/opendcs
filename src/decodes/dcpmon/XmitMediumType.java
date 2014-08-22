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
	
	public static XmitMediumType flags2type(int flags)
	{
		if (DcpMsgFlag.isGOES(flags))
			return GOES;
		else if (DcpMsgFlag.isIridium(flags))
			return IRIDIUM;
		else
			return LOGGER;
			
	}
}
