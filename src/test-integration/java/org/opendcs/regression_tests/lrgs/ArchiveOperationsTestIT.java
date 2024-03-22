package org.opendcs.regression_tests.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;

public class ArchiveOperationsTestIT
{

    private static LrgsTestInstance lrgs = null;

    @BeforeAll
    public static void setup() throws Exception
    {
        System.out.println("Before");
        assertDoesNotThrow(() ->
        {
            File lrgsHome = Files.createTempDirectory("lrgshome").toFile();
            lrgsHome.mkdirs();
            lrgs = new LrgsTestInstance(lrgsHome);
        });
    }

    @Test
    public void test_save_and_read_back() throws Exception
    {
        System.out.println("test");
        
        assertNotNull(lrgs);
        final MsgArchive archive = lrgs.getArchive();
        final String msgData = "Test String.";
        final DcpMsg msgIn = new DcpMsg(0,msgData.getBytes(Charset.forName("UTF8")),msgData.length(),0);
        final DcpAddress addrIn = new DcpAddress("TEST");
        msgIn.setDcpAddress(addrIn);
        assertDoesNotThrow(() -> archive.archiveMsg(msgIn, null));
    }
}
