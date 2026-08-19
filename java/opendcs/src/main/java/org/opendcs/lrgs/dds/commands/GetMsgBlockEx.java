/*
* Where Applicable, Copyright ? - 2026 OpenDCS Consortium and/or its contributors
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
package org.opendcs.lrgs.dds.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import org.opendcs.lrgs.dds.DdsSession;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.xml.XmlOutputStream;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.ArchiveException;
import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.EndOfArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.common.NoSuchMessageException;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchTimeoutException;
import lrgs.common.UntilReachedException;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.ExtBlockXmlParser;
import lrgs.ldds.LddsMessage;

/**
 * This intentionally duplicates {@link lrgs.ldds.CmdGetMsgBlockExt}
 * in order to start isolating the "session" components that would be required.
 *
 * Goal is to get the actual retrieve/build message logic into a central handler that is shared by
 * both the new and original DdsServer implementations.
 *
 * After a few more command implementations and rearrangments of the Netty Channel Pipeline we should
 * have a better sense of what should be where.
 *
 * GetMsgBlockEx
 */
public final class GetMsgBlockEx
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final int MAX_SIZE = 20000;
    private static final int MAX_MSGS = 100;

    private GetMsgBlockEx()
    {
        /* utility class */
    }

    @SuppressWarnings({"java:S3776", "java:S138"}) // to be dealt with in future cleanup.
    public static LddsMessage process(CmdGetMsgBlockExt cmd, DdsSession session) throws IOException, ArchiveException
    {
        log.info("Getting message {}", session);
        var msgRetriever = session.msgRetriever();
        var ddsVersion = session.ddsVersion();
        var seqNumMsgBuf = session.sequenceMessageBuf();
        var seqNumMsgBufIdx = session.seqNumMsgBufIdx();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	    ExtBlockXmlParser myXmlParser = new ExtBlockXmlParser(DcpMsgFlag.SRC_DDS);
        myXmlParser.setDdsVersion(ddsVersion);

        // Use XML_OS ( GZIP_OS ( BA_OS ) ) to build response body.
		ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_SIZE+1000);

        try (GZIPOutputStream gzos = new GZIPOutputStream(baos))
        {
            XmlOutputStream xos = new XmlOutputStream(gzos, ExtBlockXmlParser.MsgBlockElem);
            xos.startElement(ExtBlockXmlParser.MsgBlockElem);

            // Special Case - client is doing a DOMSAT Sequence Range Retrieval.
            SearchCriteria sc = msgRetriever.getCrit();
            Date since;
            Date until;
            int numMessages = 0;
            boolean didSeqSearch = false;
            boolean bufDone = false;
            String conName = "con(-1)";
            if (sc != null && sc.seqStart >= 0 && sc.seqEnd >= 0
            && (since = sc.evaluateLrgsSinceTime()) != null
            && (until = sc.evaluateLrgsUntilTime()) != null)
            {
                didSeqSearch = true;

                if (seqNumMsgBuf.isEmpty())
                {
                    var marc = (XmlMsgArchive)session.archive();
                    marc.getMsgsBySeqNum(since.getTime(),
                        until.getTime(), sc.seqStart,
                        sc.seqEnd, seqNumMsgBuf);
                    seqNumMsgBufIdx = 0;
                }
                int sz = seqNumMsgBuf.size();
                while(seqNumMsgBufIdx < sz && numMessages < MAX_MSGS)
                {
                    if (myXmlParser.addMsg(xos,
                        seqNumMsgBuf.get(seqNumMsgBufIdx++), conName)) // NOSONAR
                        numMessages++;
                }
                if (seqNumMsgBufIdx >= sz)
                {
                    seqNumMsgBuf.clear();

                    // Modify searchcrit so it won't search seq nums again.
                    sc.seqStart = -1;
                    sc.seqEnd = -1;
                }
                if (numMessages >= MAX_MSGS)
                    bufDone = true;
            }
            if (numMessages == 0)
            {
                didSeqSearch = false;
            }
            // Get message, every 5 seconds, check for stop message.
            DcpMsgIndex idx = new DcpMsgIndex();
            long start = System.currentTimeMillis();
            long stopSearchMsec = start + 45000L;
            int maxMsgs = MAX_MSGS;
            if (sc.single)
            {
                maxMsgs = 1;
            }

            while(!bufDone) // NOSONAR
            {
                try // NOSONAR
                {
                    msgRetriever.getNextPassingIndex(idx, stopSearchMsec);

                    // If we did a sequence search, then only accept messages
                    // for the regular search that DO NOT have DOMSAT sequence nums.
                    if (didSeqSearch
                    && (idx.getFlagbits() & DcpMsgFlag.MSG_NO_SEQNUM) == 0)
                        continue;

                    DcpMsg msg = msgRetriever.readMsg(idx);
                    if (msg.getData() == null)
                    {
                        continue;
                    }

                    if (myXmlParser.addMsg(xos, msg, conName))
                    {
                        numMessages++;
                    }
                    // byte size limit reached   # msg limit reached
                    if (baos.size() >= MAX_SIZE || numMessages >= maxMsgs)
                    {
                        bufDone = true;
                    }
                }
                catch(NoSuchMessageException nsme)
                {
                    log.atWarn().setCause(nsme).log("Bad message skipped. idx.Offset = {}", idx.getOffset());
                }
                catch(UntilReachedException urex)
                {
                    if (numMessages == 0)
                        throw urex;
                    else // already have some data, drop down & return it.
                        bufDone = true;
                }
                catch(EndOfArchiveException eoaex)
                {
                    if (numMessages == 0)
                        // This means caught-up to end of storage.
                        throw eoaex;
                    else
                        bufDone = true; // Just return what we have so far.
                }
                catch(SearchTimeoutException stex)
                {
                    log.atDebug()
                        .setCause(stex)
                        .log("DDS Connection aborting because of 45 second timeout");

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
        }
        log.info("returning data.");
        // end of while loop...
        LddsMessage lm = new LddsMessage(LddsMessage.IdDcpBlockExt, "");
        lm.MsgData = baos.toByteArray();
        lm.MsgLength = lm.MsgData.length;

        return lm;
    }
}
