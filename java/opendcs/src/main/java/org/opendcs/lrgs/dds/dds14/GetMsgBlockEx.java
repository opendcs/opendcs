package org.opendcs.lrgs.dds.dds14;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import lrgs.common.DcpMsgRetriever;
import lrgs.common.EndOfArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.common.NoSuchMessageException;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchTimeoutException;
import lrgs.common.UntilReachedException;
import lrgs.ddsserver.DdsServer;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.ExtBlockXmlParser;
import lrgs.ldds.LddsMessage;

public final class GetMsgBlockEx
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final int MAX_SIZE = 20000;
    private static final int MAX_MSGS = 100;

    public static final LddsMessage process(CmdGetMsgBlockExt cmd, DdsSession session) throws Exception
    {
        var msgRetriever = session.msgRetriever();
        var ddsVersion = session.ddsVersion();
        var seqNumMsgBuf = session.sequenceMessageBuf();
        var seqNumMsgBufIdx = session.seqNumMsgBufIdx();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/DDD HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	    ExtBlockXmlParser myXmlParser = new ExtBlockXmlParser(DcpMsgFlag.SRC_DDS);;
        myXmlParser.setDdsVersion(ddsVersion);

        // Use XML_OS ( GZIP_OS ( BA_OS ) ) to build response body.
		ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_SIZE+1000);

        try (GZIPOutputStream gzos = new GZIPOutputStream(baos))
        {
            XmlOutputStream xos = new XmlOutputStream(gzos, ExtBlockXmlParser.MsgBlockElem);
            xos.startElement(ExtBlockXmlParser.MsgBlockElem);

            // Special Case - client is doing a DOMSAT Sequence Range Retrieval.
            SearchCriteria sc = msgRetriever.getCrit();
            Date since, until;
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
                    int n = marc.getMsgsBySeqNum(since.getTime(),
                        until.getTime(), sc.seqStart,
                        sc.seqEnd, seqNumMsgBuf);
                    seqNumMsgBufIdx = 0;
                }
                int sz = seqNumMsgBuf.size();
                while(seqNumMsgBufIdx < sz && numMessages < MAX_MSGS)
                {
                    if (myXmlParser.addMsg(xos,
                        seqNumMsgBuf.get(seqNumMsgBufIdx++), conName))
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

            while(!bufDone)
            {
                try
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
                    continue;
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
        catch (ArchiveException ex)
        {
            String rs = "?" + ex.getErrorCode() + ",0," + ex.getMessage();
            if (!(ex instanceof UntilReachedException))
            {
                log.atTrace()
                   .setCause(ex)
                   .log("ArchiveException on Response='{}'", rs);
            }
            return new LddsMessage(cmd.getCommandCode(), rs);
        }

        // end of while loop...
        LddsMessage lm = new LddsMessage(LddsMessage.IdDcpBlockExt, "");
        lm.MsgData = baos.toByteArray();
        lm.MsgLength = lm.MsgData.length;

        return lm;
    }
}
