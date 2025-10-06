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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		log.trace("con({}) executing for {}", ldds.getUniqueID(), ldds.getClientName());

		if (!ldds.getStatusProvider().isUsable())
		{
			String emsg = "This LRGS is not currently usable.";
			log.warn("Failing message retrieval from {} {}", ldds.getName(), emsg);
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
		debug(ldds, "MJM After seqnum search, numMessages={}, bugDone={}", numMessages, bufDone);
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
				log.atWarn().setCause(nsme).log("Bad message skipped. idx.Offset = {}", idx.getOffset());
				continue;
			}
			catch(UntilReachedException urex)
			{
				debug(ldds, "DDS Connection '{}'", urex, ldds.getName());
				if (numMessages == 0)
					throw urex;
				else // already have some data, drop down & return it.
					bufDone = true;
			}
			catch(EndOfArchiveException eoaex)
			{
				debug(ldds, "DDS Connection '{}'", eoaex, ldds.getName());
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
					log.atDebug()
					   .setCause(stex)
					   .log("{} aborting to service another client message from {}",
							cmdType(), ldds.getClientName());
					return 0;
				}
				else
				{
					log.atDebug()
					   .setCause(stex)
					   .log("DDS Connection '{}' aborting because of 45 second timeout", ldds.getClientName());
				}

				if (numMessages == 0)
					throw stex;     // Means 'try again'
				else
					bufDone = true; // return what we have so far.
			}
			catch(IOException ioex)
			{
				throw new ArchiveUnavailableException(
					"Internal server error constructin MsgBlockExt response: "
					+ ioex, LrgsErrorCode.DDDSINTERNAL, ioex);
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

		debug(ldds, "MJM CmdGetMsgBlockExt Successfully retrieved and sent {} DCP messages to {}, responselen={}",
			  numMessages, ldds.getClientName(), lm.MsgLength);
		return numMessages;
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdDcpBlockExt; }

	private void debug(LddsThread ldds, String msg, Object... args)
	{
		debug(ldds, msg, null, args);
	}
	private void debug(LddsThread ldds, String msg, Throwable cause, Object... args)
	{
		log.atTrace().setCause(cause).log("con({})" + msg, ldds.getUniqueID(), args);
	}
}
