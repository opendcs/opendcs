/**
 * @(#) MsgArch.java
 */

package lritdcs.recv;

import java.util.Date;
import lrgs.common.DcpMsg;
import ilex.util.Logger;

public class MsgArch implements DcpMsgProcessor
{
	private static final int numPeriods = 24;
	
	private static final int periodDuration = 3600;
	
	private int lastArchiveSec;
	
	private MsgPerArch currentArch;

	public void MsgArch()
	{
	}
	
	public void processMsg( DcpMsg msg )
	{
		int recvTime = (int)(System.currentTimeMillis() / 1000L);
		msg.setLocalReceiveTime(new Date(recvTime*1000L));
		int startTime = (recvTime / periodDuration) * periodDuration;
		if (currentArch == null
		 || startTime != currentArch.getStartTime())
		{
			if (currentArch != null)
				currentArch.finish();
			try { currentArch = new MsgPerArch(startTime, periodDuration); }
			catch(Exception ex)
			{
				Logger.instance().failure("Cannot open archive: " + ex);
				return;
			}
		}
		try 
		{
			currentArch.add(msg); 
			lastArchiveSec = recvTime;
		}
		catch(Exception ex)
		{
			Logger.instance().failure("Cannot archive message: " + ex);
			return;
		}
	}

	public void init( )
	{
		lastArchiveSec = 0;
		currentArch = null;
	}
	
	public void shutdown( )
	{
		if (currentArch != null)
			currentArch.finish();
	}
}
