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
package lrgs.ldds;

import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.*;

/**
This command implements single-mode: A single DCP message is returned
for each request.
*/
public class CmdGetNextDcpMsg extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** @return "CmdGetNextDcpMsg"; */
	public String cmdType()
	{
		return "CmdGetNextDcpMsg";
	}

	/**
	  Executes the command.

	  Retrieve the next DCP message available. Block while waiting
	  for a DCP message but periodically check to see if client has sent
	  a 'stop' message, which cancels this request. If a stop request
	  comes in, just terminate.

	  If I retrieve a DCP message, package it and send it to client.

	  @param ldds the server thread object holding connection to client.

	  @throws IllegalStateException if LRGS is down.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (ldds.user == null)
		{
			throw new UnknownUserException(
				"HELLO required before retrieving data.");
		}

		if (!ldds.getStatusProvider().isUsable())
		{
			String emsg = "This LRGS is not currently usable.";
			log.warn("Failing message retrieval from {} {}", ldds.getName(), emsg);
			throw new ArchiveException(emsg, LrgsErrorCode.DDDSINTERNAL, true);
		}

		// Get message, every 5 seconds, check for stop message.
		DcpMsgIndex idx = new DcpMsgIndex();
		log.trace("GetNextDcpMsg executing");
		long start = System.currentTimeMillis();
		long stopSearchMsec = start + 45000L;
		while(true)
		{
			try
			{
				int x = ldds.msgretriever.getNextPassingIndex(idx, stopSearchMsec);

				// Must not deliver Iridium messages to pre DDS v10 clients.
				if (DcpMsgFlag.isIridium(idx.getFlagbits())
				 && ldds.user.getClientDdsVersionNum() < 10)
					continue;

				DcpMsg msg = ldds.msgretriever.readMsg(idx);
				ldds.lastSeqNum = msg.getSequenceNum();

				byte md[] = new byte[40 + msg.getMsgLength()];
				byte fn[] = msg.makeFileName(idx.getSequenceNum()).getBytes();
				for(int i=0; i<fn.length; i++)
					md[i] = fn[i];
				md[fn.length] = (byte)0;
				byte msgdata[] = msg.getData();
				if (msgdata != null)
					for(int i=0; i<msgdata.length; i++)
						md[40+i] = msgdata[i];

				LddsMessage lm = new LddsMessage(LddsMessage.IdDcp, "");
				lm.MsgLength = md.length;
				lm.MsgData = md;
				ldds.send(lm);
				log.trace("con({}) Successfully retrieved and sent DCP {} message to {} idx={}",
						  ldds.getUniqueID(), msg.getDcpAddress(), ldds.getClientName(), x);
				return 1;
			}
			catch(NoSuchMessageException nsme)
			{
				// This means index was valid but couldn't read the message.
				// This can occur with the race condition if we're trying
				// to read the oldest messages and it got overwritten after
				// getting the index but before reading the message. If this
				// happens, just stay in the loop and get another index.
				log.atWarn().setCause(nsme).log("Bad message skipped. idx.offset = {}", idx.getOffset());
				continue;
			}
			catch(UntilReachedException | EndOfArchiveException ex)
			{
				log.atTrace().setCause(ex).log("DDS Connection '{}'", ldds.getName());
				throw ex; // just propagate
			}
			catch(SearchTimeoutException stex)
			{
				log.atDebug().setCause(stex).log("DDS Connection '{}'", ldds.getName());

				if (ldds.ins.isMsgAvailable())
				{
					log.trace("{} aborting to service another client message from {}",
							  cmdType(), ldds.getClientName());
					return 0;
				}
				// Terminate after 45 sec so that we stay reasonably
				// responsive to client.
				if (System.currentTimeMillis() - start > 45000L)
					throw stex;
			}
			catch(ArchiveException iae)
			{
				throw iae;  // just propagate
			}
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdDcp; }
}