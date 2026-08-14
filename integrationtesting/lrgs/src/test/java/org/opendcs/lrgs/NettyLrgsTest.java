package org.opendcs.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.opendcs.lrgs.dds.NettyDdsServerBuilder;
import org.opendcs.lrgs.dds.NettyDdsServer;

import lrgs.archive.XmlMsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.ldds.LddsClient;
import lrgs.ldds.ServerError;
import lrgs.lrgsmain.LrgsInputInterface;

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

        ddsServer = new NettyDdsServerBuilder().withLrgs(lrgs.getLrgsMain()).build();
        ddsServer.start().sync();

    }

    @Test
    void test_netty_server() throws Exception
    {
        final String msgData = "Test String.";
        final DcpMsg msgIn = new DcpMsg(DcpMsgFlag.MSG_TYPE_OTHER, msgData.getBytes(Charset.forName("UTF8")),msgData.length(),0);
        msgIn.setXmitTime(new Date());
        final DcpAddress addrIn = new DcpAddress("TEST");
        final LrgsInputInterface dataSource = lrgs.getLrgsInputs().get(0);
        msgIn.setDcpAddress(addrIn);
        lrgs.getArchive().archiveMsg(msgIn, dataSource);
        ((XmlMsgArchive)lrgs.getArchive()).checkpoint();
        var sp = lrgs.getArchive().getStatusProvider();
        assertNotNull(sp);
        assertTrue(sp.isUsable());
        LddsClient client = new LddsClient("127.0.0.1", ddsServer.getBindPort());
        client.connect();
        client.getSocket().setSoTimeout(0);
        client.sendHello("anonymous");
        assertResultWithinTimeFrame(value ->
        {
            try
            {
                var ret = client.getMsgBlockExt(500);
                if (ret == null)
                {
                    return false;
                }
                return ret.length >= 1;
            }
            catch (ServerError ex)
            {
                if (ex.getMessage().contains("End of Archive"))
                {
                    return false;
                }
                throw ex;
            }
        },
        3, TimeUnit.MINUTES,
        5, TimeUnit.SECONDS,
        "No Data returned within reasonable time frame");

        client.sendGoodbye();
        client.disconnect();
        assertFalse(client.isConnected());
    }

}
