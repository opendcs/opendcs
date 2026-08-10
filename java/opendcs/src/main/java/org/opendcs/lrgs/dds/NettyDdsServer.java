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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import lrgs.lrgsmain.LrgsMain;

public final class NettyDdsServer
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public static final AttributeKey<LrgsMain> LRGS_INSTANCE = AttributeKey.valueOf("lrgs");

    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private ChannelFuture channel;
    private final InetAddress bindAddress;
    private final int bindPort;
    private final ServerBootstrap boostrap;
    private final LrgsMain lrgs;

    private NettyDdsServer(EventLoopGroup boss, EventLoopGroup worker, ServerBootstrap bootstrap,
                           InetAddress bindAddress, int bindPort, LrgsMain lrgs)
    {
        this.boss = boss;
        this.worker = worker;
        this.bindAddress = bindAddress;
        this.boostrap = bootstrap;
        this.bindPort = bindPort;
        this.lrgs = Objects.requireNonNull(lrgs, ":rgs instance is required");
    }

    /**
     * Start listening on the given port. The Netty channel future is returned if the caller needs
     * to wait for the startup to finish.
     * @return Netty ChannelFuture for the server.
     * @throws IllegalStateException if called after the server was previously shutdown.
     */
    public ChannelFuture start()
    {
        if (worker.isShutdown() || worker.isShuttingDown() || worker.isTerminated() ||
            boss.isShutdown() || boss.isShutdown() || boss.isTerminated())
        {
            throw new IllegalStateException("This instance is not usable, it has been previously shutdown.");
        }
        log.info("Starting DdsServer on {}:{}", bindAddress, bindPort);
        channel = boostrap.bind(bindAddress, bindPort);
        return channel;
    }

    /**
     * Shutdown the socket and stop listening. Returns immediately.
     * If you wish to wait for shutdown call .sync() on the returned ChannelFuture.
     * @return the ChannelFuture of this server;
     */
    public ChannelFuture stop()
    {
        log.info("Stoping DdsServer on {}:{}", bindAddress, bindPort);
        this.worker.shutdownGracefully();
        this.boss.shutdownGracefully();
        return channel;
    }

    /**
     * Retrieve port this DdsServer is bound to.
     * @return
     */
    public int getBindPort()
    {
        return this.bindPort;
    }

    /**
     * Return Address this DdsServer is bound to.
     * @return
     */
    public InetAddress getBindAddress()
    {
        return bindAddress;
    }

    /**
     * Builder to assign or override various components of the LRGS server
     * Builder
     */
    public static class Builder
    {
        int port = 16003;
        InetAddress bindAddr;
        LrgsMain lrgs = null;

        public Builder()
        {
            try
            {
                bindAddr = InetAddress.getByAddress(new byte[]{0,0,0,0});
            }
            catch (UnknownHostException ex)
            {
                throw new RuntimeException("Unable to create address object for any host.", ex);
            }
        }

        /**
         * Port to bind this DdsServer to. Default is <code>16003</code>
         * @param port
         * @return
         */
        public Builder withPort(int port)
        {
            this.port = port;
            return this;
        }

        /**
         * Address to bind this DdsServer to. Default is <code>0.0.0.0</code>
         * @param bindAddress
         * @return
         */
        public Builder bindTo(InetAddress bindAddress)
        {
            this.bindAddr = bindAddress;
            return this;
        }

        public Builder withLrgs(LrgsMain lrgs)
        {
            this.lrgs = lrgs;
            return this;
        }

        /**
         * Initialize DdsServer. Returned NettyDdsServer is ready to start when this returns, but has not been started.
         * @return
         */
        public NettyDdsServer build()
        {
            EventLoopGroup boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
            .channel(NioServerSocketChannel.class)
            .childAttr(LRGS_INSTANCE, lrgs)
            .childHandler(new ChannelInitializer<SocketChannel>()
            {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception
                {
                    ch.pipeline()
                        .addLast(
                            new LddsMessageDecoder(),
                            new LddsCommandDecoder(),
                            new LddsMessageEncoder())
                        .addLast(LddsHelloHandler.HANDLER_NAME, new LddsHelloHandler())
                        .addLast(new LddsErrorHandler());
                }
            })
            .option(ChannelOption.SO_BACKLOG, 5)
            .childOption(ChannelOption.SO_KEEPALIVE, true);

            return new NettyDdsServer(boss, worker, b, bindAddr, port, lrgs);
        }
    }
}
