package decodes.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class NetworkListTest {
    
    private static final Logger log = LoggerFactory.getLogger(NetworkListTest.class);

    @Test
    public void NetworkListEqualsTest() 
    {
        NetworkList list1 = new NetworkList("MVR-RIVERGAGES-DAS","goes");
        list1.siteNameTypePref="nwshb5";

        NetworkList list2 = new NetworkList("MVR-RIVERGAGES-DAS","goes");
        list2.siteNameTypePref="nwshb5";

        NetworkListEntry entry1 = new NetworkListEntry(null, "DD31104C");
        entry1.setPlatformName("WDMI4");
        entry1.setDescription("Raccoon River near West Des Moines, IA (USGS)");
        
        NetworkListEntry entry2 = new NetworkListEntry(null, "CE45A8E4");
        entry2.setPlatformName("LUSM7");
        entry2.setDescription("Mississippi River at Louisiana, MO (MVS)");        

        assertTrue(list1.equals(list2),"two empty lists with the same properties");

        list1.addEntry(entry1);
        assertFalse(list1.equals(list2),"list with one item compared to empty list");
        assertFalse(list2.equals(list1),"empty list compared to list with one item");
        
    }
}
