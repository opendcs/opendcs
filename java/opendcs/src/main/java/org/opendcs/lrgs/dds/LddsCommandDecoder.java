package org.opendcs.lrgs.dds;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lrgs.ldds.CmdFactory;
import lrgs.ldds.LddsMessage;

public class LddsCommandDecoder extends MessageToMessageDecoder<LddsMessage>
{
    private static final CmdFactory factory = new CmdFactory();

    @Override
    protected void decode(ChannelHandlerContext ctx, LddsMessage msg, List<Object> out) throws Exception
    {
        out.add(factory.makeCommand(msg));
    }    
}
