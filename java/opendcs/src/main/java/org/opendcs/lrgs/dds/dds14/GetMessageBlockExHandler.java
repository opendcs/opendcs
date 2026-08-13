package org.opendcs.lrgs.dds.dds14;

import org.opendcs.lrgs.dds.LddsHelloHandler;
import org.opendcs.lrgs.dds.commands.GetMsgBlockEx;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lrgs.common.ArchiveException;
import lrgs.common.UntilReachedException;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.LddsMessage;

public class GetMessageBlockExHandler extends SimpleChannelInboundHandler<CmdGetMsgBlockExt>
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CmdGetMsgBlockExt msg) throws Exception
    {
        var session = ctx.channel().attr(LddsHelloHandler.DDS_SESSION).get();
        var res = GetMsgBlockEx.process(msg, session);
        ctx.writeAndFlush(res);
        ReferenceCountUtil.release(msg);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (cause instanceof ArchiveException ex)
        {        
            String rs = "?" + ex.getErrorCode() + ",0," + ex.getMessage();
            if (!(ex instanceof UntilReachedException))
            {
                log.atTrace()
                   .setCause(ex)
                   .log("ArchiveException on Response='{}'", rs);
            }
            var f = ctx.writeAndFlush( new LddsMessage(LddsMessage.IdDcpBlockExt, rs));
            if (ex.getHangup())
            {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
