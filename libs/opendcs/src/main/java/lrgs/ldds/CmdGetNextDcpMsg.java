/*
*  $Id$
*/
package lrgs.ldds;

import java.io.IOException;

import ilex.util.Logger;
import lrgs.common.*;

/**
This command implements single-mode: A single DCP message is returned
for each request.
*/
public class CmdGetNextDcpMsg extends LddsCommand
{
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
			Logger.instance().warning(
				"Failing message retrieval from " + ldds.getName() + " "+emsg);
			throw new ArchiveException(emsg, LrgsErrorCode.DDDSINTERNAL, true);
		}

		// Get message, every 5 seconds, check for stop message.
		DcpMsgIndex idx = new DcpMsgIndex();
		Logger.instance().debug3("GetNextDcpMsg executing");
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

				Logger.instance().log(Logger.E_DEBUG3,
					"con(" + ldds.getUniqueID() 
					+ ") Successfully retrieved and sent DCP " +
					msg.getDcpAddress() + " message to " + 
					ldds.getClientName() + "idx=" + x);
				return 1;
			}
			catch(NoSuchMessageException nsme)
			{
				// This means index was valid but couldn't read the message.
				// This can occur with the race condition if we're trying
				// to read the oldest messages and it got overwritten after
				// getting the index but before reading the message. If this
				// happens, just stay in the loop and get another index.
				Logger.instance().warning(LddsParams.module
					+ " Bad message skipped. idx.offset = " +
					 idx.getOffset() + ": " + nsme);
				continue;
			}
			catch(UntilReachedException urex)
			{
				Logger.instance().debug2("DDS Connection '" + ldds.getName()
					+ "' " + urex);
				throw urex; // just propegate
			}
			catch(EndOfArchiveException eoaex)
			{
				Logger.instance().debug2("DDS Connection '" + ldds.getName()
					+ "' " + eoaex);
				throw eoaex;  // just propegate
			}
			catch(SearchTimeoutException stex)
			{
				Logger.instance().debug1("DDS Connection '" + ldds.getName()
					+ "' " + stex);

				if (ldds.ins.isMsgAvailable())
				{
					Logger.instance().debug3(cmdType() + 
						" aborting to service another client message from "
							+ ldds.getClientName());
					return 0;
				}
				// Terminate after 45 sec so that we stay reasonably 
				// responsive to client.
				if (System.currentTimeMillis() - start > 45000L)
					throw stex;
			}
			catch(ArchiveException iae)
			{
				Logger.instance().debug1("DDS Connection '" + ldds.getName()
					+ "' " + iae);
				throw iae;  // just propegate
			}
		}
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdDcp; }
}
