package org.opendcs.lrgs;

import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.extensions.lrgs.LrgsConfig;
import org.opendcs.fixtures.extensions.lrgs.LrgsTestExtension;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;

import lrgs.apistatus.AttachedProcess;
import lrgs.archive.XmlMsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpNameMapper;
import lrgs.common.EndOfArchiveException;
import lrgs.common.SearchCriteria;
import lrgs.ddsserver.MessageArchiveRetriever;
import lrgs.lrgsmain.LrgsInputInterface;

@ExtendWith(LrgsTestExtension.class)
@LrgsConfig("noTimeout=true")
class ArchiveOperationsTestIT
{
    private static final String MSG_DATA = "Test String.";

    @BeforeAll
    static void setup(LrgsTestInstance lrgs) throws Exception
    {
        // Store message
        assertNotNull(lrgs);
        final var archive = (XmlMsgArchive)lrgs.getArchive();
        final DcpMsg msgIn = new DcpMsg(DcpMsgFlag.MSG_TYPE_OTHER, MSG_DATA.getBytes(StandardCharsets.UTF_8), MSG_DATA.length(),0);
        msgIn.setXmitTime(new Date());
        final DcpAddress addrIn = new DcpAddress("TEST");
        final LrgsInputInterface dataSource = lrgs.getLrgsInputs().get(0);
        msgIn.setDcpAddress(addrIn);
        assertDoesNotThrow(() -> archive.archiveMsg(msgIn, dataSource));
        assertEquals(1, archive.getTotalMessageCount());
        archive.checkpoint();
    }

    @Test
    void test_read_specific_dcp(LrgsTestInstance lrgs) throws Exception
    {
        final var archive = (XmlMsgArchive)lrgs.getArchive();
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
                    return MSG_DATA.equals(msgOut.getDataStr());
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
        }, 15, TimeUnit.SECONDS, 5, TimeUnit.SECONDS,
        "Saved message was not found in the allotted time frame.");
    }

    /**
     * Created to verify some behavior with the Netty DdsServer. Even though the archive is valid,
     * the {@link lrgs.ddsserver.MessageArchiveRetriever} doesn't always return any results immediately.
     * @param lrgs
     * @throws Exception
     */
    @Test
    void test_read_all(LrgsTestInstance lrgs) throws Exception
    {
        final var archive = (XmlMsgArchive)lrgs.getArchive();
        // Attempt to read back message.
        AttachedProcess ap = new AttachedProcess(1, "test", "test", "tester", 0, 0, 0, "running", (short)0);
        final MessageArchiveRetriever mar = new MessageArchiveRetriever(archive, ap);
        SearchCriteria sc = new SearchCriteria();
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
                    return MSG_DATA.equals(msgOut.getDataStr());
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
