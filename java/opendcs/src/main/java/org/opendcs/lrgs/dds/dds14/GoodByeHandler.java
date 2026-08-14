package org.opendcs.lrgs.dds.dds14;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lrgs.ldds.CmdGoodbye;
import lrgs.ldds.LddsMessage;

/**
 * Simple handler that terminates the session.
 * Other handlers are responsible for Session variable instance cleanup.
 *
 * GoodByeHandler
 */
public class GoodByeHandler extends SimpleChannelInboundHandler<CmdGoodbye>
{
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CmdGoodbye msg) throws Exception
    {
        var res = new LddsMessage(LddsMessage.IdGoodbye, "");
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        ReferenceCountUtil.release(msg);
    }
}
