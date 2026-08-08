package org.opendcs.lrgs.dds;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

public final class NettyDdsServer
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private final EventLoopGroup boss;
    private final EventLoopGroup worker;
    private ChannelFuture channel;
    private final InetAddress bindAddress;
    private final int bindPort;
    private final ServerBootstrap boostrap;


    private NettyDdsServer(EventLoopGroup boss, EventLoopGroup worker, ServerBootstrap bootstrap,
                           InetAddress bindAddress, int bindPort)
    {
        this.boss = boss;
        this.worker = worker;
        this.bindAddress = bindAddress;
        this.boostrap = bootstrap;
        this.bindPort = bindPort;
    }

    /**
     * Start listening on the given port. The Netty channel future is returned if the caller needs
     * to wait for the startup to finish.
     * @return Netty ChannelFuture for the server.
     */
    public ChannelFuture start()
    {
        channel = boostrap.bind(bindAddress, bindPort);
        return channel;
    }

    /**
     * Shutdown the socket and stop listening. Returns immediately.
     */
    public void stop()
    {
        try 
        {
            stop(false);
        }
        catch (InterruptedException ex)
        {
            log.atError().setCause(ex).log("Exception thrown, that shouldn't have been.");
        }
    }

    /**
     * Shutdown the socket and stop listening. Optionally waiting for the operation to finish.
     * @param wait whether to wait until shutdown is complete.
     * @throws InterruptedException if there are any issues waiting.
     */
    public void stop(boolean wait) throws InterruptedException
    {
        this.worker.shutdownGracefully();
        this.boss.shutdownGracefully();        
        if (wait)
        {
            channel.sync();
        }
    }

    public int getBindPort()
    {
        return this.bindPort;
    }

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

        public Builder()
        {
            try
            {
                bindAddr = Inet4Address.getByAddress(new byte[]{0,0,0,0});
            }
            catch (UnknownHostException ex)
            {
                throw new RuntimeException("Unable to create address object for any host.", ex);
            }
        }

        public Builder withPort(int port)
        {
            this.port = port;
            return this;
        }

        public Builder bindTo(InetAddress bindAddress)
        {
            this.bindAddr = bindAddress;
            return this;
        }

        public NettyDdsServer build()
        {
            EventLoopGroup boss = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            EventLoopGroup worker = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
            .channel(NioServerSocketChannel.class)
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
                        .addLast(LddsHelloHandler.HANDLER_NAME, new LddsHelloHandler());
                }   
            })
            .option(ChannelOption.SO_BACKLOG, 5)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            return new NettyDdsServer(boss, worker, b, bindAddr, port);
        }
    }
}


