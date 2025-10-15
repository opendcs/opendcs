/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lritdcs.recv;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpMsg;

public class MsgArch implements DcpMsgProcessor
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atError().setCause(ex).log("Cannot open archive.");
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
			log.atError().setCause(ex).log("Cannot archive message.");
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
