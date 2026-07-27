package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.NetworkListDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;

class NetworkListDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(NetworkListDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var list = new NetworkList("Test-List-1");
            list.transportMediumType = "goes";
            list.addEntry(new NetworkListEntry(list, "P1"));
            list.addEntry(new NetworkListEntry(list, "P2"));

            var listOut = dao.save(tx, list);

            assertFalse(listOut.networkListEntries.isEmpty());
        }
    }
    
}
