package org.opendcs.lrgs.dds.dds14;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import org.opendcs.lrgs.dds.LddsHelloHandler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.opentelemetry.api.trace.Span;
import lrgs.common.LrgsErrorCode;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.CmdGoodbye;
import lrgs.ldds.LddsCommand;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ServerError;

/**
 * Handles clients using DdsProtocol 14
 * DdsProtocol14Handler
 */
public class DdsProtocol14Handler extends ChannelInboundHandlerAdapter
{
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        var user = ctx.channel().attr(LddsHelloHandler.DDS_USER).get();
        var session = ctx.channel().attr(LddsHelloHandler.DDS_SESSION).get();
        try (var span = Span.current().setAttribute("ddsUser", user.getName()).makeCurrent())
        {
            if (msg instanceof CmdGetMsgBlockExt cmd)
            {
                var res = GetMsgBlockEx.process(cmd, session.msgRetriever(), user.getDdsVersion());
                ctx.writeAndFlush(res);
            }
            else if (msg instanceof CmdGoodbye)
            {
                var res = new LddsMessage(LddsMessage.IdGoodbye, "");
                ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
            }
            else if (msg instanceof LddsCommand cmd)
            {
                throw new ServerError("Invalid command sent " + cmd.getCommandCode(),
                                    LrgsErrorCode.DPARSEERROR, 0);
            }
            else
            {
                super.channelRead(ctx, msg);
            }
        }
    }
}
