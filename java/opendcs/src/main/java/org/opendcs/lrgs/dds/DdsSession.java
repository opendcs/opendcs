package org.opendcs.lrgs.dds;

import java.util.ArrayList;
import java.util.List;

import org.opendcs.lrgs.dao.MsgArchive;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgRetriever;

/**
 * Data objects that may be required during a given DdsSession. DdsUserPrincipal is stored separately.
 * Additionally, items placed in here should make a best effort to disconnect from implementation assumptions.
 *
 * NOTE: seqNumMsgBuf* is clearly a failure in that regard and are currently left in for completeness of implementation.
 * Such data is required, though likely better in a simple Map<String,Object> that is part of the session.
 *
 * DdsSession
 * @param msgRetriever DcpMsgRetriever that is used to acquire messages.
 * @param ddsVersion which version we are operating under
 * @param archive MsgArchive for this LRGS instance.
 * @param seqNumMsgBufIdx current index for retrieval by sequence number
 * @param sequenceMessageBuf buffer of by sequence number messages
 */
public record DdsSession(DcpMsgRetriever msgRetriever, int ddsVersion, MsgArchive archive,
                         Integer seqNumMsgBufIdx, List<DcpMsg> sequenceMessageBuf)
{
    public DdsSession(DcpMsgRetriever msgRetriever, int ddsVersion, MsgArchive archive)
    {
        this(msgRetriever, ddsVersion, archive, 0, new ArrayList<>());
    }
}
