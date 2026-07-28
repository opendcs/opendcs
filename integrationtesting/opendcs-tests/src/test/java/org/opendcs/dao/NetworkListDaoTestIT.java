package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.NetworkListDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;

@EnableIfTsDb
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
            list.siteNameTypePref = "CWMS";
            list.addEntry(new NetworkListEntry(list, "P1"));
            list.addEntry(new NetworkListEntry(list, "P2"));

            var listOut = dao.save(tx, list);

            assertFalse(listOut.networkListEntries.isEmpty());

            listOut.removeEntry("P1");
            var listOut2 = dao.save(tx, listOut);
            assertEquals(1, listOut2.networkListEntries.size());

            dao.delete(tx, listOut.getId());
        }

        try (var tx = db.newTransaction())
        {
            assertTrue(dao.getByName(tx, "Test-List-1").isEmpty());
        }
    }

    @Test
    void test_get_all() throws Exception
    {
        var dao = db.getDao(NetworkListDao.class).orElseThrow();

        // This is test code. reports of vulernabilities due to "insecure" random will be dutifully ignored.
        // The specific intend is *to* be repeatable.
        Random random = new Random(5);

        try (var tx = db.newTransaction())
        {
            List<String> types = List.of("goes", "iridium", "logger");
            HashMap<String, Integer> numListByType = new HashMap<>();
            HashMap<String, Integer> numEntriesByList = new HashMap<>();

            for (int i = 0; i < 100; i++)
            {
                var list = new NetworkList(String.format("Test-List-%04d", i));
                final String type = types.get(random.nextInt(types.size()));
                list.transportMediumType = type;
                list.siteNameTypePref = "CWMS";
                for (int j = 0; j < random.nextInt(10); j++)
                {
                    list.addEntry(new NetworkListEntry(list, String.format("P%02d", j)));
                
                }

                dao.save(tx, list);
                var current = numListByType.computeIfAbsent(type, t -> 0);
                numListByType.put(type, current + 1);
                
                numEntriesByList.put(list.name, list.networkListEntries.size());
            }

            for (var type: types)
            {
                var typeSize = numListByType.get(type);
                var listByType = dao.getAll(tx, -1, -1, type, false);
                assertEquals(typeSize, listByType.size());
            }

            var all = dao.getAll(tx, -1, -1, null, true);
            assertEquals(100, all.size());

            var firstTen = dao.getAll(tx, 10, 0, null, true);
            assertEquals(10, firstTen.size());
            assertEquals("Test-List-0009", firstTen.getLast().name);

            var nextTen = dao.getAll(tx, 10, 10, null, true);
            assertEquals("Test-List-0010", nextTen.getFirst().name);
            assertEquals("Test-List-0019", nextTen.getLast().name);

            for (var list: all)
            {
                var entries = numEntriesByList.get(list.name);
                assertNotNull(entries);
                assertEquals(entries, list.networkListEntries.size(),
                             () -> "List " + list.name + " contained incorrect number of entries.");
            }

            for (var list: firstTen)
            {
                var entries = numEntriesByList.get(list.name);
                assertNotNull(entries);
                assertEquals(entries, list.networkListEntries.size(),
                             () -> "List " + list.name + " contained incorrect number of entries.");
            }

            for (var list: nextTen)
            {
                var entries = numEntriesByList.get(list.name);
                assertNotNull(entries);
                assertEquals(entries, list.networkListEntries.size(),
                             () -> "List " + list.name + " contained incorrect number of entries.");
            }

            tx.rollback();
        }
    }
}
