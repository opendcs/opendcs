package org.opendcs.lrgs.messages;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.opendcs.lrgs.http.dto.DataSource;

import lrgs.common.ArchiveException;
import lrgs.common.DcpMsg;
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
        final List<org.opendcs.lrgs.http.dto.DcpMsg> messages = new ArrayList<>();
        try
        {
            final DcpMsgIndex dmi = new DcpMsgIndex();
            
            int idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 5000L);
            while(idx != -1 && messages.size() < maxSize)
            {
                final DcpMsg msgOut = dmi.getDcpMsg();
                if (msgOut != null)
                {
                    
                    final String type = "" + lrgs.getLrgsInputById(msgOut.getDataSourceId()).getType();
                    
                    final org.opendcs.lrgs.http.dto.DcpMsg msg = 
                        new org.opendcs.lrgs.http.dto.DcpMsg(
                            msgOut.getDcpAddress().toString(),
                            new DataSource(msgOut.getSource(), type),
                            ZonedDateTime.ofInstant(msgOut.getLocalReceiveTime().toInstant(), ZoneId.of("UTC")),
                            Base64.getEncoder().encodeToString(msgOut.getData())
                            );
                    messages.add(msg);
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

    /** The current message search design allows for there to be exception in the middle of valid responses */
    public record RetrieveResult(List<org.opendcs.lrgs.http.dto.DcpMsg> messages, ArchiveException ex)
    {

    }
}
