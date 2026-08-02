package org.opendcs.lrgs.dds;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lrgs.ddsserver.JLddsThread;
import lrgs.ldds.LddsCommand;
import lrgs.ldds.LddsMessage;

public class LddsCommandHandler extends ChannelInboundHandlerAdapter
{
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof LddsCommand cmd)
        {
            cmd.execute(new JLddsThread(null, null, 0, null, null, null, null)
            {
                @Override
                public void send(LddsMessage msg)
                {
                    ctx.writeAndFlush(msg);
                }
            });
        }
    }    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        super.exceptionCaught(ctx, cause);
    }
}
