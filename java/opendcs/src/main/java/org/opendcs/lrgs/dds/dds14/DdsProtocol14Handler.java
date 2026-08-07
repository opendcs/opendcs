package org.opendcs.lrgs.dds.dds14;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lrgs.ldds.LddsCommand;

/**
 * Handles clients using DdsProtocol 14
 * DdsProtocol14Handler
 */
public class DdsProtocol14Handler extends ChannelInboundHandlerAdapter
{
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof LddsCommand cmd)
        {
            // TODO
        }
        else
        {
            super.channelRead(ctx, msg);
        }
    }
}
