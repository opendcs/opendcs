package org.opendcs.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dds.DdsMessageSender;
import org.opendcs.lrgs.dds.LddsCommandDecoder;
import org.opendcs.lrgs.dds.LddsCommandHandler;
import org.opendcs.lrgs.dds.LddsMessageDecoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lrgs.ldds.LddsMessage;

class NettyLrgsTest
{
    private static LrgsTestInstance lrgs = null;
    private static EventLoopGroup boss = new NioEventLoopGroup();
    private static EventLoopGroup worker = new NioEventLoopGroup();
    private static ChannelFuture channel;
    private static int port;

    @BeforeAll
    static void setup() throws Exception
    {
        System.out.println("Before");
        assertDoesNotThrow(() ->
        {
            File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
            lrgsHome.mkdirs();
            lrgs = new LrgsTestInstance(lrgsHome);
        });


    
    
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
                    new LddsCommandHandler(null)
            );
            }   
         })
         .option(ChannelOption.SO_BACKLOG, 5)
         .childOption(ChannelOption.SO_KEEPALIVE, true);

         channel = b.bind(0).sync();
         port = ((InetSocketAddress)channel.channel().localAddress()).getPort();
         
    }

    @Test
    void test_netty_server() throws Exception
    {
        

    }

}
