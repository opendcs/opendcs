package org.opendcs.lrgs.dds;

import java.util.Arrays;
import java.util.List;

import ilex.util.ArrayUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ProtocolError;

public class LddsMessageDecoder extends ByteToMessageDecoder
{
    static final byte[] validSync = { (byte) 'F', (byte) 'A', (byte) 'F', (byte) '0' };
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception 
    {
        if (in.readableBytes() < LddsMessage.ValidHdrLength)
        {
            return;
        }

        var header = in.readBytes(LddsMessage.ValidHdrLength);
        var hdr = header.array();
        if (hdr[0] != validSync[0] || 
            hdr[1] != validSync[1] ||
            hdr[2] != validSync[2] ||
            hdr[3] != validSync[3])
		{
			throw new ProtocolError("Could not read valid sync pattern ("+new String(hdr,"UTF8") +")");
		}

        var msg = new LddsMessage(hdr);
        var length = Integer.parseInt(new String(ArrayUtil.getField(hdr, 5, 5)));
        
        if (in.readableBytes() < length)
        {
            return;
        }
        msg.MsgData = Arrays.copyOf(in.readBytes(length).array(), length);
        out.add(msg);
    }
}
