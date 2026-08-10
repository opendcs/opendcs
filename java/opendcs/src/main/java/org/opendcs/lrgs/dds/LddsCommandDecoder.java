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

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lrgs.ldds.CmdFactory;
import lrgs.ldds.LddsMessage;

/**
 *
 * LddsCommandDecoder
 *
 * Given the LddsMessage that should've been previously created, create an
 * appropriate {@link lrgs.ldds.LddsCommand} instance
 */
public class LddsCommandDecoder extends MessageToMessageDecoder<LddsMessage>
{
    private static final CmdFactory factory = new CmdFactory();

    @Override
    protected void decode(ChannelHandlerContext ctx, LddsMessage msg, List<Object> out) throws Exception
    {
        out.add(factory.makeCommand(msg));
    }
}
