package org.opendcs.lrgs.dds.dds14;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lrgs.ldds.CmdGetMsgBlockExt;

public class GetMessageBlockExHandler extends SimpleChannelInboundHandler<CmdGetMsgBlockExt>
 {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CmdGetMsgBlockExt msg) throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
    }
    
    
}
