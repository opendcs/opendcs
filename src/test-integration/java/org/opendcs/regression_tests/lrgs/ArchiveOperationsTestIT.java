package org.opendcs.regression_tests.lrgs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

import lrgs.apistatus.AttachedProcess;
import lrgs.archive.MsgArchive;
import lrgs.archive.MsgFilter;
import lrgs.archive.SearchHandle;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpNameMapper;
import lrgs.common.SearchCriteria;
import lrgs.ddsserver.MessageArchiveRetriever;

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
        // Store message
        assertNotNull(lrgs);
        final MsgArchive archive = lrgs.getArchive();
        final String msgData = "Test String.";
        final DcpMsg msgIn = new DcpMsg(0,msgData.getBytes(Charset.forName("UTF8")),msgData.length(),0);
        final DcpAddress addrIn = new DcpAddress("TEST");
        msgIn.setDcpAddress(addrIn);
        assertDoesNotThrow(() -> archive.archiveMsg(msgIn, null));
        assertEquals(1, archive.getTotalMessageCount());
        archive.checkpoint();
        // Attempt to read back message.
        AttachedProcess ap = new AttachedProcess(1, "test", "test", "tester", 0, 0, 0, "running", (short)0);
        MessageArchiveRetriever mar = new MessageArchiveRetriever(archive, ap);
        SearchCriteria sc = new SearchCriteria();
        sc.addDcpName("TEST");
        sc.setLrgsSince("now - 1 day");
        sc.setLrgsUntil("now");
        mar.setDcpNameMapper(new DcpNameMapper()
        {
            @Override
            public DcpAddress dcpNameToAddress(String name)
            {
                return new DcpAddress(name);
            }
        });
        mar.setSearchCriteria(sc);
        DcpMsgIndex dmi = new DcpMsgIndex();
        int idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + 30000);
        assertNotEquals(-1, idx);
        final DcpMsg msgOut = dmi.getDcpMsg();
        assertNotNull(msgOut, "We got a valid msg index but could not get an actual message.");
        assertEquals(msgData.getBytes(Charset.forName("UTF8")), msgOut.getData(), "Saved message data was not returned.");
    }
}
