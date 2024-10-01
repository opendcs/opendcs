package decodes.tsdb;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class IntervalIncrementTest {

    @Test
    public void negative_increments_allowed(){
        IntervalIncrement inc = IntervalIncrement.parse("-1 Hour");
        assertTrue( inc.toMsec() < 0, "A negative increment was parsed as positive");
    }


    @Test
    public void normal_increments() {
        IntervalIncrement inc = null;


        inc = IntervalIncrement.parse("1 Hour");
        assertTrue(inc.isLessThanDay(), "1 hour increment appears to system as more than one day.");
        assertEquals(3600000, inc.toMsec(), "1 hour increment not converted to the correct seconds");

        inc = IntervalIncrement.parse("2 days");
        assertFalse(inc.isLessThanDay(), "2 day increment appears to system as less than one day");
        assertEquals(86400*2*1000, inc.toMsec(), "2 day increment incorrect");
    }
}
