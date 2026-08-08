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
