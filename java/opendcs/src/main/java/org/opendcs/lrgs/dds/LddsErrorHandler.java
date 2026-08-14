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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Catch all error handler the terminates the connection after logging the error.
 * LddsErrorHandler
 */
public class LddsErrorHandler extends ChannelInboundHandlerAdapter
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        log.atError().setCause(cause).log("Unable to process DDS Message");
        ctx.close();
    }
}
