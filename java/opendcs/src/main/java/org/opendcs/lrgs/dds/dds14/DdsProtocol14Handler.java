package org.opendcs.lrgs.dds.dds14;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
        if (msg instanceof CmdGetMsgBlockExt)
        {
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
