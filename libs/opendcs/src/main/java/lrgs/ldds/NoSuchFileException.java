package lrgs.ldds;

import lrgs.common.LrgsErrorCode;

/**
  Thrown on request for netlist or searchcrit file that does not exist.
*/
public class NoSuchFileException extends LddsRequestException
{
	/**
	  Constructor takes a String message.
	  @param msg the message
	*/
	public NoSuchFileException(String msg)
	{
		super(msg, LrgsErrorCode.DNOSUCHFILE, false);
	}
}

