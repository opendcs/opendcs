package org.opendcs.lrgs.dds;

import io.netty.channel.ChannelInboundHandlerAdapter;

public class DdsNoOpHandler extends ChannelInboundHandlerAdapter
{
    public static final String NAME = "ddsHandler";
    /* Does nothing, exists as a placeholder with a name. */    
}
