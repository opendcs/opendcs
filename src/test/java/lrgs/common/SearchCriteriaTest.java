package lrgs.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lrgs.ldds.DdsVersion;

public class SearchCriteriaTest
{
    private SearchCriteria sc;

    @BeforeEach
    public void setup() throws Exception
    {
        sc = new SearchCriteria();
        sc.parityErrors = SearchCriteria.EXCLUSIVE;
        sc.DapsStatus = SearchCriteria.YES;
        sc.DcpNames = new ArrayList<>();
        sc.DcpNames.add("TEST1");
        sc.DcpNames.add("TEST2");
        sc.spacecraft = SearchCriteria.SC_EAST;
    }

    @Test
    public void test_copy() throws Exception
    {
        final SearchCriteria sc = new SearchCriteria();
        final SearchCriteria scCopy = new SearchCriteria(sc);
        final String scString = sc.toString(DdsVersion.version_14);
        final String scCopyString = scCopy.toString(DdsVersion.version_14);
        assertEquals(scString, scCopyString, "Criteria copy did not copy all elements.");
    }

    @Test
    public void test_write_to_disk_and_readback() throws Exception
    {
        final File critFile = File.createTempFile("dcstest", ".crit");
        critFile.deleteOnExit();
        sc.saveFile(critFile);

        final SearchCriteria scFile = new SearchCriteria(critFile);
        final String scMemoryStr = sc.toString(DdsVersion.version_14);
        final String scFileStr = scFile.toString(DdsVersion.version_14);
        assertEquals(scMemoryStr, scFileStr, "File was not saved or read back correctly.");
    }

    @Test
    public void test_time_evaluation() throws Exception
    {
        sc.setLrgsSince("now - 1 day");
        sc.setLrgsUntil("now");
        final Date since = sc.evaluateLrgsSinceTime();
        final Date nowMinus1Day = new Date(System.currentTimeMillis()-86400*1000);
        assertTrue(Math.abs(since.getTime() - nowMinus1Day.getTime()) < (3600*1000+2000), "since time not within reasonable deviation.");
        final Date now = new Date();
        final Date evalNow = sc.evaluateLrgsUntilTime();
        assertTrue(Math.abs(now.getTime() - evalNow.getTime())< 2000, "until time not within reasonable deviation.");
    }
}
