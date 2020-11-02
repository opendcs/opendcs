/*
*  $Id$
*/
package lrgs.ldds;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;
import java.text.SimpleDateFormat;

import ilex.util.Base64;
import ilex.util.Logger;
import ilex.xml.XmlOutputStream;
import lrgs.common.*;
import lrgs.archive.MsgArchive;
import lrgs.ddsserver.DdsServer;

/**
This new implementation of the 'multi-mode' mechanism was added for LRGS 6.
Data is returned in compressed XML blocks as follows:
<pre>
	<MsgBlock>
		<DcpMsg flags=0xnnnn>
			<AsciiMsg> (DOMSAT Header and msg data) </AsciiMsg>
					... or ...
			<BinaryMsg> (DOMSAT Header and BASE64 msg data) </BinaryMsg>
			[<CarrierStart>YYYY/DDD HH:MM:SS.mmm</CarrierStart>]
			[<CarrierStop>YYYY/DDD HH:MM:SS.mmm</CarrierStop>]
			[<DomsatTime>YYYY/DDD HH:MM:SS.mmm</domsatTime>]
			[<DomsatSeq>NNNNN</domsatSeq>]
			[<Baud>NNNN</baud>]
		</DcpMsg>
		...
	</MsgBlock>
</pre>
The additional header fields will only be provided if they are defined in
the DcpMsg.
Before placing the response into the data portion of an LddsMessage the
data is GZipped.
*/
public class CmdGetMsgBlockExt extends LddsCommand
{
	private static final int MAXSIZE = 20000;
	private static final int MAXMSGS = 100;

	private SimpleDateFormat sdf;
	private ExtBlockXmlParser myXmlParser;

	public CmdGetMsgBlockExt()
	{
		sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		myXmlParser = new ExtBlockXmlParser(DcpMsgFlag.SRC_DDS);
	}

	/** @return "CmdGetMsgBlockExt"; */
	public String cmdType()
	{
		return "CmdGetMsgBlockExt";
	}

	/**
	  Executes the command.

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
		
		// Set the client's version number in my XML producer so that we don't
		// send anything the client can't handle.
		myXmlParser.setDdsVersion(ldds.getClientDdsVersion());
		
		DcpMsgRetriever msgretriever = ldds.msgretriever;
		
		if (msgretriever == null)
			throw new UnknownUserException(
				"Incomplete setup for user '" + ldds.user.name + "'");

		Logger.instance().debug3(
			"con(" + ldds.getUniqueID() + ") "
			+ "GetNextMsgBlockExt executing for " + ldds.getClientName());

		if (!ldds.getStatusProvider().isUsable())
		{
			String emsg = "This LRGS is not currently usable.";
			Logger.instance().warning(
				"Failing message retrieval from " + ldds.getName() + " "+emsg);
			throw new ArchiveException(emsg, LrgsErrorCode.DDDSINTERNAL, true);
		}

		// Pre-v11 clients will issue warning if they get LocalRecvTime block
		if (ldds.user.getClientDdsVersionNum() < DdsVersion.version_11)
			myXmlParser.setWriteLocalTime(false);

		// Use XML_OS ( GZIP_OS ( BA_OS ) ) to build response body.
		ByteArrayOutputStream baos = new ByteArrayOutputStream(MAXSIZE+1000);
		GZIPOutputStream gzos = new GZIPOutputStream(baos);
		XmlOutputStream xos = new XmlOutputStream(gzos, ExtBlockXmlParser.MsgBlockElem);
		xos.startElement(ExtBlockXmlParser.MsgBlockElem);

		// Special Case - client is doing a DOMSAT Sequence Range Retrieval.
		SearchCriteria sc = msgretriever.getCrit();
		Date since, until;
		int numMessages = 0;
		boolean didSeqSearch = false;
		boolean bufDone = false;
		String conName = "con(" + ldds.getUniqueID() + ")";
		if (sc != null && sc.seqStart >= 0 && sc.seqEnd >= 0 
		 && (since = sc.evaluateLrgsSinceTime()) != null
		 && (until = sc.evaluateLrgsUntilTime()) != null)
		{
debug(ldds, "MJM Trying seqnum search");
			didSeqSearch = true;

			if (ldds.seqNumMsgBuf == null)
			{
				DdsServer ddsServer = (DdsServer)ldds.getParent();
				MsgArchive marc = ddsServer.msgArchive;
				ldds.seqNumMsgBuf = new ArrayList<DcpMsg>();
				int n = marc.getMsgsBySeqNum(since.getTime(), 
					until.getTime(), sc.seqStart,
					sc.seqEnd, ldds.seqNumMsgBuf);
				ldds.seqNumMsgBufIdx = 0;
//Logger.instance().info("MJM Got " + n 
//+ " msgs from archive in sequence range ("
//+ sc.seqStart + "," + sc.seqEnd + ") at time " + since);
			}
			int sz = ldds.seqNumMsgBuf.size();
			while(ldds.seqNumMsgBufIdx < sz && numMessages < MAXMSGS)
			{
				if (myXmlParser.addMsg(xos, 
					ldds.seqNumMsgBuf.get(ldds.seqNumMsgBufIdx++), conName))
					numMessages++;
			}
			if (ldds.seqNumMsgBufIdx >= sz)
			{
				ldds.seqNumMsgBuf.clear();
				ldds.seqNumMsgBuf = null;
				// Modify searchcrit so it won't search seq nums again.
				sc.seqStart = -1;
				sc.seqEnd = -1;
			}
			if (numMessages >= MAXMSGS)
				bufDone = true;
		}
debug(ldds, 
"MJM After seqnum search, numMessages=" + numMessages + ",bufDone=" + bufDone);
		if (numMessages == 0)
			didSeqSearch = false;

		// Get message, every 5 seconds, check for stop message.
		DcpMsgIndex idx = new DcpMsgIndex();
		long start = System.currentTimeMillis();
		long stopSearchMsec = start + 45000L;
		int maxMsgs = MAXMSGS;
		if (sc.single)
			maxMsgs = 1;

		while(!bufDone)
		{
			int x = -1;
			try 
			{
				x = msgretriever.getNextPassingIndex(idx, stopSearchMsec); 

				// If we did a sequence search, then only accept messages
				// for the regular search that DO NOT have DOMSAT sequence nums.
				if (didSeqSearch
				 && (idx.getFlagbits() & DcpMsgFlag.MSG_NO_SEQNUM) == 0)
					continue;
				
				// Must not deliver Iridium messages to pre DDS v10 clients.
				if (DcpMsgFlag.isIridium(idx.getFlagbits())
				 && ldds.user.getClientDdsVersionNum() < 10)
					continue;

				DcpMsg msg = msgretriever.readMsg(idx);
				if (msg.getData() == null)
					continue;

				if (myXmlParser.addMsg(xos, msg, conName))
					numMessages++;

				if (baos.size() >= MAXSIZE         // byte size limit reached 
				 || numMessages >= maxMsgs)         // # msg limit reached
					bufDone = true;
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
				if (numMessages == 0)
					throw urex;
				else // already have some data, drop down & return it.
					bufDone = true;
			}
			catch(EndOfArchiveException eoaex)
			{
				debug(ldds, "DDS Connection '" + ldds.getName()
					+ "' " + eoaex);
				if (numMessages == 0)
					// This means caught-up to end of storage.
					throw eoaex;
				else
					bufDone = true; // Just return what we have so far.
			}
			catch(SearchTimeoutException stex)
			{
				if (ldds.ins.isMsgAvailable())
				{
					Logger.instance().debug1(cmdType() + 
						" aborting to service another client message from "
							+ ldds.getClientName());
					return 0;
				}
				else
					Logger.instance().debug1("DDS Connection '"+ldds.getClientName()
						+ "' aborting because of 45 second timeout: " + stex);

				if (numMessages == 0)
					throw stex;     // Means 'try again'
				else
					bufDone = true; // return what we have so far.
			}
			catch(IOException ioex)
			{
				String msg = cmdType() + " IOException: " + ioex;
				Logger.instance().warning(msg);
				System.err.println(ioex.toString());
				ioex.printStackTrace(System.err);
				throw new ArchiveUnavailableException(
					"Internal server error constructin MsgBlockExt response: "
					+ ioex, LrgsErrorCode.DDDSINTERNAL);
			}
			// Allow ArchiveUnavailable to propegate.
		}

		xos.endElement(ExtBlockXmlParser.MsgBlockElem);
		gzos.finish();
		try { gzos.close(); }
		catch(Exception ex) {}

		// end of while loop...
		LddsMessage lm = new LddsMessage(LddsMessage.IdDcpBlockExt, "");
		lm.MsgData = baos.toByteArray();
		lm.MsgLength = lm.MsgData.length;
		ldds.send(lm);

debug(ldds, "MJM CmdGetMsgBlockExt Successfully retrieved and sent " + numMessages
+ " DCP messages to " + ldds.getClientName()
+ ", responselen=" + lm.MsgLength);
		return numMessages;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdDcpBlockExt; }

	private void debug(LddsThread ldds, String msg)
	{
		Logger.instance().debug3("con(" + ldds.getUniqueID() + ") "
			+ "GetNextDcpMsgBlock executing for " + ldds.getClientName()
			+ " " + msg);
	}
}
