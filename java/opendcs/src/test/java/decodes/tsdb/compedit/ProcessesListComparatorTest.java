package decodes.tsdb.compedit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.compedit.ProcessesListComparator;

class ProcessesListComparatorTest
{

    @Test 
    void test_list_comparator() throws Exception
    {
        CompAppInfo appInfoLeft = new CompAppInfo(DbKey.createDbKey(5));
        appInfoLeft.setAppName("Left");
        appInfoLeft.setComment("I'm on the left");
        appInfoLeft.setNumComputations(30);

        CompAppInfo appInfoRight = new CompAppInfo(DbKey.createDbKey(1));
        appInfoRight.setAppName("Right");
        appInfoRight.setComment("I'm on the right");
        appInfoRight.setNumComputations(5);
        
        ProcessesListComparator sortByKey = new ProcessesListComparator(0, Long.class);
        int ret = sortByKey.compare(appInfoLeft, appInfoRight);
        assertTrue(ret > 0);
        ret = sortByKey.compare(appInfoLeft, appInfoLeft);
        assertEquals(0, ret);


        ProcessesListComparator sortByName = new ProcessesListComparator(1, String.class);
        ret = sortByName.compare(appInfoLeft, appInfoRight);
        assertTrue(ret < 0);

        ProcessesListComparator sortByNumComps = new ProcessesListComparator(2, Integer.class);
        ret = sortByNumComps.compare(appInfoLeft, appInfoRight);
        assertTrue(ret > 0);

         // intentionally wrong class used below, forcing fall through behavior.
        ProcessesListComparator sortByComment = new ProcessesListComparator(3, Double.class);
        ret = sortByComment.compare(appInfoLeft, appInfoRight);
        assertTrue(ret < 0);

        appInfoRight.setAppId(appInfoLeft.getAppId());
        ret = sortByKey.compare(appInfoLeft, appInfoRight);
        assertEquals(0, ret);
        
    }
    
}
