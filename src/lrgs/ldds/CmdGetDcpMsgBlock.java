/*
*  $Id$
*/
package lrgs.ldds;

import java.io.IOException;

import ilex.util.Logger;
import lrgs.common.*;

/**
This implements the 'multi-mode' mechanism for receivining blocks
of DCP messages in a single message.
*/
public class CmdGetDcpMsgBlock extends LddsCommand
{
	//private static final int MAXSIZE = 40000;
	private static final int MAXSIZE = 8192;

	/** @return "CmdGetDcpMsgBlock"; */
	public String cmdType()
	{
		return "CmdGetDcpMsgBlock";
	}

	/**
	  Executes the command.
	  Check to make sure user is authenticated. If not throw 
	  UnknownUserException.

	  Retrieve as many DCP messages as will fit in the return message.
	  Limit return message size to no more than 90000.

	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"HELLO required before retrieving data.");
		if (ldds.msgretriever == null)
			throw new UnknownUserException(
				"Incomplete setup for user '" + ldds.user.name + "'");

		if (!ldds.getStatusProvider().isUsable())
		{
			String emsg = "This LRGS is not currently usable.";
			Logger.instance().warning(
				"Failing message retrieval from " + ldds.getName() + " "+emsg);
			throw new ArchiveException(emsg, LrgsErrorCode.DDDSINTERNAL, true);
		}

		byte[] buf = ldds.getBlockBuffer();
		int bufSize = 0;
		boolean bufDone = false;
		int numMessages = 0;

		// Get message, every 5 seconds, check for stop message.
		DcpMsgIndex idx = new DcpMsgIndex();
		debug(ldds, "GetNextDcpMsgBlock executing for " + ldds.getClientName());
		long start = System.currentTimeMillis();
		long stopSearchMsec = start + 45000L;
		while(bufDone == false)
		{
			int x = -1;
			try 
			{
				x = ldds.msgretriever.getNextPassingIndex(idx, stopSearchMsec); 

				// Must not deliver Iridium messages to pre DDS v10 clients.
				if (DcpMsgFlag.isIridium(idx.getFlagbits())
				 && ldds.user.getClientDdsVersionNum() < 10)
					continue;

				DcpMsg msg = ldds.msgretriever.readMsg(idx);
				byte msgdata[] = msg.getData();

				if (msgdata != null)
				{
					// MJM 20030216 - Need to calculate length in exactly
					// the same way that client will when parsing the
					// block... by using the length in the header.
					int len;
					try 
					{
						len = msg.getDcpDataLength(); 
						if (len <= 0)
							throw new Exception("Length zero or unparsable.");
						len += DcpMsg.IDX_DATA; 
					}
					catch(Exception ex) 
					{
						debug(ldds, "Bad length field in message '" 
							+ msg.getHeader() + "': " + ex + " -- skipped.");
						continue; 
					}
					for(int i=0; i < len && i < msgdata.length; i++)
						buf[bufSize++] = msgdata[i];
debug(ldds, "CmdGetBlock: added '" 
+ (new String(msg.getHeader())) + "' to return buffer, len now is " + bufSize);
				}
	
				ldds.lastSeqNum = msg.getSequenceNum();
				ldds.lastMsgTime = (int)(msg.getDapsTime().getTime() / 1000L);

				if (bufSize >= MAXSIZE)
					bufDone = true;
	
				numMessages++;
			}
			catch(NoSuchMessageException nsme)
			{
				Logger.instance().warning(LddsParams.module
					+ " Bad message skipped. idx.Offset = " 
					+ idx.getOffset() + ": " + nsme);
				continue;
			}
			catch(UntilReachedException urex)
			{
				debug(ldds, "DDS Connection '" + ldds.getName() + "' " + urex);
				if (bufSize == 0)
					throw urex;
				else // already have some data, drop down & return it.
					bufDone = true;
			}
			catch(EndOfArchiveException eoaex)
			{
				debug(ldds, "DDS Connection '" + ldds.getName()
					+ "' " + eoaex);
				if (bufSize == 0)
					// This means caught-up to end of storage.
					throw eoaex;
				else
					bufDone = true; // Just return what we have so far.
			}
			catch(SearchTimeoutException stex)
			{
				debug(ldds, "DDS Connection '" + ldds.getName()
					+ "' " + stex);

				if (ldds.ins.isMsgAvailable())
				{
					debug(ldds, cmdType() + 
						" aborting to service another client message from "
							+ ldds.getClientName());
					return 0;
				}

				// Terminate after 45 sec so that we stay reasonably 
				// responsive to client.
				if (System.currentTimeMillis() - start > 45000L)
				{
					if (bufSize == 0)
						throw stex;     // Means 'try again'
					else
					{
						bufDone = true; // return what we have so far.
						debug(ldds, "Aborting because response taking more than 45 sec.");
					}
				}
			}
			catch(NullPointerException npex)
			{
				// This can happen if user was disconnected while this command
				// was executing. Just return.
				return 0;
			}
			// Allow ArchiveUnavailable to propegate.
		}

		// end of while loop...
		LddsMessage lm = new LddsMessage(LddsMessage.IdDcpBlock, "");
		lm.MsgLength = bufSize;
		lm.MsgData = buf;
		ldds.send(lm);

		Logger.instance().log(Logger.E_DEBUG2,
			"CmdGetDcpMsgBlock Successfully retrieved and sent " + numMessages
			+ " DCP messages to " + ldds.getClientName()
			+ ", responselen=" + lm.MsgLength);
		return numMessages;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdDcpBlock; }


	private void debug(LddsThread ldds, String msg)
	{
		Logger.instance().debug3("con(" + ldds.getUniqueID() + ") "
			+ "GetNextDcpMsgBlock executing for " + ldds.getClientName()
			+ " " + msg);
	}
}
