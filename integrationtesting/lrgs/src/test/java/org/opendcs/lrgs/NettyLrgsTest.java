package org.opendcs.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dds.LddsCommandDecoder;
import org.opendcs.lrgs.dds.LddsHelloHandler;
import org.opendcs.lrgs.dds.LddsMessageDecoder;
import org.opendcs.lrgs.dds.LddsMessageEncoder;
import org.opendcs.lrgs.dds.NettyDdsServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lrgs.ldds.LddsClient;

class NettyLrgsTest
{
    private static LrgsTestInstance lrgs = null;
    private static NettyDdsServer ddsServer = null;

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

        ddsServer = new NettyDdsServer.Builder().build();
        ddsServer.start().sync();
         
    }

    @Test
    void test_netty_server() throws Exception
    {
        LddsClient client = new LddsClient("127.0.0.1", ddsServer.getBindPort());
        client.connect();
        client.sendHello("anonymous");
        var ret = client.getMsgBlockExt(0);
        assertNotNull(ret);
        assertEquals(1, ret.length);
        client.sendGoodbye();
        client.disconnect();
        assertFalse(client.isConnected());
    }

}
