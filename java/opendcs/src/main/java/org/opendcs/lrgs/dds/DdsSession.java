package org.opendcs.lrgs.dds;

import java.util.ArrayList;
import java.util.List;

import org.opendcs.lrgs.dao.MsgArchive;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgRetriever;

public record DdsSession(DcpMsgRetriever msgRetriever, int ddsVersion, MsgArchive archive,
                         Integer seqNumMsgBufIdx, List<DcpMsg> sequenceMessageBuf)
{
    public DdsSession(DcpMsgRetriever msgRetriever, int ddsVersion, MsgArchive archive)
    {
        this(msgRetriever, ddsVersion, archive, 0, new ArrayList<>());
    }
}
