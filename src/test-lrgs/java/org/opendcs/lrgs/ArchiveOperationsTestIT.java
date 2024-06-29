package org.opendcs.regression_tests.lrgs;

import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

import lrgs.apistatus.AttachedProcess;
import lrgs.archive.MsgArchive;
import lrgs.archive.MsgFilter;
import lrgs.archive.SearchHandle;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpNameMapper;
import lrgs.common.EndOfArchiveException;
import lrgs.common.SearchCriteria;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsInputInterface;

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
        final DcpMsg msgIn = new DcpMsg(DcpMsgFlag.MSG_TYPE_OTHER, msgData.getBytes(Charset.forName("UTF8")),msgData.length(),0);
        msgIn.setXmitTime(new Date());
        final DcpAddress addrIn = new DcpAddress("TEST");
        final LrgsInputInterface dataSource = lrgs.getLrgsInputs().get(0);
        msgIn.setDcpAddress(addrIn);
        assertDoesNotThrow(() -> archive.archiveMsg(msgIn, dataSource));
        assertEquals(1, archive.getTotalMessageCount());
        archive.checkpoint();
        // Attempt to read back message.
        AttachedProcess ap = new AttachedProcess(1, "test", "test", "tester", 0, 0, 0, "running", (short)0);
        final MessageArchiveRetriever mar = new MessageArchiveRetriever(archive, ap);
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
        final DcpMsgIndex dmi = new DcpMsgIndex();
        assertResultWithinTimeFrame(value ->
        {
            try
            {
                int idx = mar.getNextPassingIndex(dmi, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
                if (idx == -1)
                {
                    return false;
                }
                final DcpMsg msgOut = dmi.getDcpMsg();
                if (msgOut != null)
                {
                    return msgData.equals(msgOut.getDataStr());
                }
                else
                {
                    return false;
                }
            }
            catch (EndOfArchiveException ex)
            {
                return false;
            }
        }, 3, TimeUnit.MINUTES, 5, TimeUnit.SECONDS,
        "Saved message was not found in the allotted time frame.");
    }
}
