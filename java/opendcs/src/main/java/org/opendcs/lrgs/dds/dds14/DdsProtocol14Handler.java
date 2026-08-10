/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.lrgs.dds.dds14;

import org.opendcs.lrgs.dds.LddsHelloHandler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.opentelemetry.api.trace.Span;
import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.CmdGoodbye;
import lrgs.ldds.LddsCommand;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ServerError;

/**
 * Handles clients using DdsProtocol 14.
 *
 * Currently only implements MsgBlockExt and GoodBye. GoodByte should probably just be it's own
 * ChannelInboundHandlerAdapter.
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
                var res = GetMsgBlockEx.process(cmd, session);
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        var res = new LddsMessage(LddsMessage.IdDcpBlockExt, cause.getMessage());
        var future = ctx.writeAndFlush(res);
        if (!(cause instanceof ArchiveException aex) || aex.getHangup())
        {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
