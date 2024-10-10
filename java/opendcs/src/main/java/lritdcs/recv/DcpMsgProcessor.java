package lritdcs.recv;

import lrgs.common.DcpMsg;

/**
Interface for processing DCP messages.
*/
public interface DcpMsgProcessor
{
	/** Process a new DCP message */
	public void processMsg(DcpMsg msg);
}
