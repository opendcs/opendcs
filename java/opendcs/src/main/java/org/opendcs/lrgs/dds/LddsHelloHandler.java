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

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import org.opendcs.lrgs.dds.dds14.DdsProtocol14Handler;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lrgs.common.LrgsErrorCode;
import lrgs.ldds.CmdGetMsgBlockExt;
import lrgs.ldds.CmdGoodbye;
import lrgs.ldds.CmdHello;
import lrgs.ldds.DdsInternalException;
import lrgs.ldds.DdsVersion;
import lrgs.ldds.LddsCommand;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ServerError;

/**
 *
 * LddsCommandHandler
 *
 * Given an {@link lrgs.ldds.LddsCommand instance} process the message appropriately.
 *
 * NOTE: This is also where things like establishing session specific objects, like a MessageArchiveRetriever
 * or an implementation similar to {@link lrgs.ldds.LddsThread} (though far more generic) that wraps those instances.
 *
 * NOTE: DDS <b>is</b> a stateful protocol and that won't change so there are long running connections and state data
 * for that connection.
 *
 * As stated in {@see channelRead} below, this should be a light weight wrapper around some object dedicated to
 * taking an {@link lrgs.ldds.LddsCommand} and returning an appropriate {@link lrgs.ldds.LddsMessage} to send back
 * to the client.
 */
public class LddsHelloHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

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

            ctx.pipeline().remove("hellohandler");
            ctx.pipeline().addLast("dds14handler", new DdsProtocol14Handler());
            ctx.writeAndFlush(res);
        }
        else if (msg instanceof CmdGetMsgBlockExt)
        {
            log.info("Get message block msg");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(baos))
            {
                gzip.write("""
                    <MsgBlock>
                        <DcpMsg flags="0x0">
                            <AsciiMsg>Header              Data</AsciiMsg>
                            <CarrierStart>2026/131 00:00:00.000</CarrierStart>
                            <CarrierStop>2026/131 00:00:10.000</CarrierStop>
                            <DomsatTime>2026/131 00:00:00.000</DomsatTime>
                            <DomsatSeq>0</DomsatSeq>
                            <Baud>300</Baud>
                        </DcpMsg>
                    </MsgBlock>
                """.getBytes());
                gzip.finish();
            }
            var res = new LddsMessage(LddsMessage.IdDcpBlockExt, null);
            res.MsgData = baos.toByteArray();
            res.MsgLength = baos.size();
            ctx.writeAndFlush(res);
        }
        else if (msg instanceof CmdGoodbye)
        {
            log.atInfo().log("User Sent Goodbye");
            var res = new LddsMessage(LddsMessage.IdGoodbye, "");
            ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);
        }
        else if (msg instanceof LddsCommand cmd)
        {
            log.atInfo().log("Sending back response. Command was {}", cmd.cmdType());
            ctx.writeAndFlush(cmd);
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
