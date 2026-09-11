package org.opendcs.lrgs.messages;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.opendcs.lrgs.http.dto.DataSource;
import org.opendcs.lrgs.http.dto.GoesMessage;

import lrgs.common.ArchiveException;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpMsgRetriever;
import lrgs.lrgsmain.LrgsMain;

public final class MessageRetrieval
{
    private MessageRetrieval()
    {
        /* utility class */
    }
    
    /**
     * Given the already setup message archive retriver, get the messages bad on search criteria
     * @param mar
     * @param lrgs
     * @return
     */
    public static RetrieveResult getMessages(DcpMsgRetriever mar, LrgsMain lrgs, int maxSize)
    {
        final List<GoesMessage> messages = new ArrayList<>();
        try
        {
            final DcpMsgIndex dmi = new DcpMsgIndex();
            
            int idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 5000L);
            while(idx != -1 && messages.size() < maxSize)
            {
                final DcpMsg msgOut = dmi.getDcpMsg();
                if (msgOut != null)
                {
                    
                    if (msgOut.isGoesMessage())
                        messages.add(toGoesMessage(msgOut));
                }
                idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 500);
            }
            return new RetrieveResult(messages, null);
        }
        catch (ArchiveException ex)
        {
            return new RetrieveResult(messages, ex);
        }
    }

    public static GoesMessage toGoesMessage(DcpMsg message)
    {
        byte[] raw = message.getData();
        int headerLength = Math.min(37, raw.length);
        Date received = message.getLocalReceiveTime() != null
            ? message.getLocalReceiveTime() : message.getXmitTime();
        String cType = DcpMsgFlag.isGoesST(message.getFlagbits()) ? "g-s-t"
            : DcpMsgFlag.isGoesRD(message.getFlagbits()) ? "g-r" : "goes";
        String channel = String.format("%03d%s", message.getGoesChannel(), message.getGoesSpacecraft());
        String downlink = raw.length >= 32
            ? new String(raw, 30, 2, StandardCharsets.US_ASCII) : "";
        String payload = new String(raw, headerLength, raw.length - headerLength, StandardCharsets.UTF_8);
        return new GoesMessage(
            "GOES",
            message.getDcpAddress().toString(),
            DateTimeFormatter.ISO_INSTANT.format(message.getXmitTime().toInstant()),
            DateTimeFormatter.ISO_INSTANT.format(received.toInstant()),
            new DataSource(message.getSource(), "GOES"),
            cType,
            String.valueOf(message.getFailureCode()),
            String.valueOf(message.getSignalStrength()),
            String.valueOf(message.getFrequencyOffset()),
            String.valueOf(message.getModulationIndex()),
            String.valueOf(message.getDataQuality()),
            channel,
            downlink,
            Math.max(0, raw.length - headerLength),
            payload);
    }

    /** The current message search design allows for there to be exception in the middle of valid responses */
    public record RetrieveResult(List<GoesMessage> messages, ArchiveException ex)
    {

    }
}
