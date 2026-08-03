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

import java.nio.charset.StandardCharsets;
import java.util.List;

import ilex.util.ArrayUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lrgs.ldds.LddsMessage;
import lrgs.ldds.ProtocolError;

/**
 * LddsMessageDecoder
 *
 * Isolates the work of getting an LddsMessage ready to be processed into a command.
 *
 * If a complete message is not available nothing is added to out, and the channel bytes
 * are not "consumed/discarded", when more bytes come in this method is called again.
 * Once sufficient bytes have been retrieved, we put the message into the out list.
 *
 * Netty will then automatically discard all of the bytes we have actually used to create
 * that object.
 *
 */
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

        var hdr = new byte[LddsMessage.ValidHdrLength];
        in.readBytes(hdr, 0, LddsMessage.ValidHdrLength);
        if (hdr[0] != validSync[0] ||
            hdr[1] != validSync[1] ||
            hdr[2] != validSync[2] ||
            hdr[3] != validSync[3])
		{
			throw new ProtocolError("Could not read valid sync pattern ("+new String(hdr, StandardCharsets.UTF_8) +")");
		}

        var msg = new LddsMessage(hdr);
        var length = Integer.parseInt(new String(ArrayUtil.getField(hdr, 5, 5)));

        if (in.readableBytes() < length)
        {
            return;
        }
        msg.MsgData = new byte[length];
        in.readBytes(msg.MsgData, 0, length);
        out.add(msg);
    }
}
