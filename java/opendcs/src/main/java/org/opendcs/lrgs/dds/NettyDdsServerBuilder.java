package org.opendcs.lrgs.dds;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lrgs.lrgsmain.LrgsMain;

/**
 * Builder to assign or override various components of the LRGS server
 * Builder
 */
public class NettyDdsServerBuilder
{
    int port = 16003;
    InetAddress bindAddr;
    LrgsMain lrgs = null;

    public NettyDdsServerBuilder()
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
    public NettyDdsServerBuilder withPort(int port)
    {
        this.port = port;
        return this;
    }

    /**
     * Address to bind this DdsServer to. Default is <code>0.0.0.0</code>
     * @param bindAddress
     * @return
     */
    public NettyDdsServerBuilder bindTo(InetAddress bindAddress)
    {
        this.bindAddr = bindAddress;
        return this;
    }

    public NettyDdsServerBuilder withLrgs(LrgsMain lrgs)
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
        .childAttr(NettyDdsServer.LRGS_INSTANCE, lrgs)
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
                    .addLast(DdsNoOpHandler.NAME, new DdsNoOpHandler())
                    .addLast(new LddsErrorHandler());
            }
        })
        .option(ChannelOption.SO_BACKLOG, 5)
        .childOption(ChannelOption.SO_KEEPALIVE, true);

        return new NettyDdsServer(boss, worker, b, bindAddr, port, lrgs);
    }
}