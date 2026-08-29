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
import lrgs.ldds.LddsMessage;

/**
 * Handles clients using DdsProtocol 14.
 *
 * Currently only implements MsgBlockExt and GoodBye. GoodByte should probably just be it's own
 * ChannelInboundHandlerAdapter.
 * DdsProtocol14Handler
 */
public class DdsProtocol14Handler extends ChannelInboundHandlerAdapter
{
    public static final String NAME = "dds14Handler";

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception
    {
        ctx.channel()
           .pipeline()
           .addAfter(NAME, "msgblockext", new GetMessageBlockExHandler())
           .addAfter(NAME, "goodbyte", new GoodByeHandler())
           // add the rest of the appropriate handlers
        ;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        var user = ctx.channel().attr(LddsHelloHandler.DDS_USER).get();
        try (var span = Span.current().setAttribute("ddsUser", user.getName()).makeCurrent())
        {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        var res = new LddsMessage(LddsMessage.IdGoodbye, cause.getMessage());
        ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
    }
}
