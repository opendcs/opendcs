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
package org.opendcs.lrgs.dds;

import org.opendcs.lrgs.dds.dds14.DdsProtocol14Handler;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lrgs.common.LrgsErrorCode;
import lrgs.ldds.CmdHello;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ServerError;

/**
 *
 * LddsCommandHandler
 *
 * Given an {@link lrgs.ldds.LddsCommand instance} of {@link lrgs.ldds.CmdHello} or {@link lrgs.ldds.CmdAuthHello}
 * process the message appropriately and setup the desired DdsServer version.
 *
 */
public class LddsHelloHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public static final String HANDLER_NAME = "helloHandler";

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        log.atInfo().log("Channel deactivated. Client disconnected");
    }

    /**
     * The command reponse implementation details here present as placeholders to verify the generic request/response
     * handling with the original LddsClient works. The idea is not to implement the entire functionality here.
     * Due the the tight integration with The BasicServerThread concept directly, it was not pratical
     * to reuse the existing implementations. Next step is extracting the operations code from the LddsCommand
     * implementations so that that logic can be reused where appropriate and creating new Dds IO operations
     * classes that can then be used by both this handler and the original implementation.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof CmdHello hello)
        {
            log.info("hello msg");
            if (hello.getDdsVersion() != 14)
            {
                throw new ServerError("Only DDS Protcoll Version 14 is supported on this system.", LrgsErrorCode.DDDSFATAL, 0);
            }
            var res = new LddsMessage(LddsMessage.IdHello, hello.getUserName() + " " + hello.getDdsVersion());

            ctx.pipeline().remove(HANDLER_NAME);
            ctx.pipeline().addLast("dds14handler", new DdsProtocol14Handler());
            ctx.writeAndFlush(res);
        }
        else
        {
            log.atInfo().log("did not receive an LddsCommand instance, was " + msg.getClass().getName());
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        log.atError().setCause(cause).log("Unable to process DDS Message");
        ctx.close();
    }
}
