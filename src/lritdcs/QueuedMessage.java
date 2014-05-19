package lritdcs;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgIndex;

public class QueuedMessage
{
	/** the queued message */
	DcpMsg msg;
	
	/** the index of this message */
	DcpMsgIndex idx;
	
	/** the time that this entry was enqueued */
	long enqueueMsec;
	
	public QueuedMessage(DcpMsg msg, DcpMsgIndex idx)
	{
		this.msg = msg;
		this.idx = idx;
		enqueueMsec = System.currentTimeMillis();
	}
	
}
