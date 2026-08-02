package org.opendcs.lrgs.dds;

import java.util.Objects;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lrgs.ldds.LddsCommand;

public class LddsCommandHandler extends ChannelInboundHandlerAdapter
{
    private final DdsMessageSender messageSender;

    public LddsCommandHandler(DdsMessageSender messageSender)
    {
        this.messageSender = Objects.requireNonNull(messageSender, "Message sender instance is required.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof LddsCommand cmd)
        {
            cmd.execute(messageSender);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        super.exceptionCaught(ctx, cause);
    }
}
