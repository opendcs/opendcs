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

import java.security.Principal;

import org.opendcs.lrgs.dds.dds14.DdsProtocol14Handler;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import lrgs.apistatus.AttachedProcess;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.LrgsErrorCode;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.ldds.CmdHello;
import lrgs.ldds.DdsUser;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.LddsUser;
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
    public static final AttributeKey<DdsUserPrincipal> DDS_USER = AttributeKey.valueOf("ddsUser");
    public static final AttributeKey<DdsSession> DDS_SESSION = AttributeKey.valueOf("ddsSession");

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        ctx.channel().attr(DDS_USER).set(null);
        ctx.channel().attr(DDS_SESSION).set(null);
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
        var user = ctx.channel().attr(DDS_USER).get();
        if (msg instanceof CmdHello && user != null)
        {
            var res = new LddsMessage(LddsMessage.IdGoodbye, "hello already called.");
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        }
        else if (msg instanceof CmdHello hello)
        {
            if (hello.getDdsVersion() != 14)
            {
                throw new ServerError("Only DDS Protcoll Version 14 is supported on this system.", LrgsErrorCode.DDDSFATAL, 0);
            }
            var res = new LddsMessage(LddsMessage.IdHello, hello.getUserName() + " " + hello.getDdsVersion());
            user = new DdsUserPrincipal(hello.getUserName(), hello.getDdsVersion());
            ctx.channel().attr(DDS_USER).set(user);
            
            var lrgs = ctx.channel().attr(NettyDdsServer.LRGS_INSTANCE).get();
            var ap = new AttachedProcess();
            ap.user = user.getName();
            var retriever = new MessageArchiveRetriever((XmlMsgArchive)lrgs.msgArchive, ap);
            retriever.init();
            var session = new DdsSession(retriever);
            ctx.channel().attr(DDS_SESSION).set(session);

            ctx.pipeline().addLast("dds14handler", new DdsProtocol14Handler());
            ctx.writeAndFlush(res);
        }
        else
        {
            super.channelRead(ctx, msg);
        }
    }
}
