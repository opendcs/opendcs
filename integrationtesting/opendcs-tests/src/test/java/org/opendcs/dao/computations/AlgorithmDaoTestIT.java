package org.opendcs.dao.computations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.AlgorithmDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;

class AlgorithmDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var algoDao = db.getDao(AlgorithmDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var algoIn = new DbCompAlgorithm("Test Algo");
            algoIn.setComment("I don't actually exist");
            algoIn.setExecClass("org.opendcs.Nothing");
            algoIn.setProperty("input_MISSING", "FAIL");
            algoIn.addParm(new DbAlgoParm("input", "i"));
            algoIn.addParm(new DbAlgoParm("output", "o"));

            var algoOut = algoDao.save(tx, algoIn);
            assertTrue(algoIn.equalsNoId(algoOut));

            var algoOutById = algoDao.getById(tx, algoOut.getId()).orElseThrow();
            assertEquals(algoOut, algoOutById);

            var algoOutByName = algoDao.getByName(tx, algoOut.getName()).orElseThrow();
            assertEquals(algoOut, algoOutByName);

            algoDao.delete(tx, algoOut.getId());

            algoDao.getById(tx, algoOut.getId()).ifPresent(algo -> fail("was not deleted."));

            tx.rollback();
        }
    }


    @Test
    void test_paging() throws Exception
    {
        var algoDao = db.getDao(AlgorithmDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var COUNT = 50;
            final var algosStored = new ArrayList<DbCompAlgorithm>();
            for (int i = 0; i < COUNT; i++)
            {
                var algoIn = new DbCompAlgorithm(String.format("AAA-Test Algo-%03d", i));
                algoIn.setComment("I don't actually exist");
                algoIn.setExecClass(String.format("org.opendcs.Nothing%d", i));
                algoIn.setProperty("input_MISSING", "FAIL");
                algoIn.addParm(new DbAlgoParm("input", "i"));
                algoIn.addParm(new DbAlgoParm("output", "o"));

                if (i % 7 == 0)
                {
                    algoIn.setProperty("inputDelta_EU", "ft");
                    algoIn.addParm(new DbAlgoParm("inputDelta", "id"));
                }

                if (i % 9 == 0)
                {
                    algoIn.setProperty("input_EU", "" + i);
                }

                var algoOut = algoDao.save(tx, algoIn);
                assertTrue(algoIn.equalsNoId(algoOut), "The algo at index " + i + " was not retrieved correctly");
                algosStored.add(algoOut);
            }

            final var all = algoDao.getAll(tx, -1, -1, true);
            assertTrue(all.size() >= COUNT);
            assertEquals(algosStored.getFirst(), all.getFirst(), () -> algosStored.getFirst().getName() + " != " + all.getFirst().getName());
            var allBasics = algoDao.getAll(tx, -1, -1, false);
            assertTrue(allBasics.size() >= COUNT);
            var firstBasic = allBasics.getFirst();
            assertNotEquals(all.getFirst(), firstBasic);

            assertTrue(firstBasic.getProperties().isEmpty());

            var first10 = algoDao.getAll(tx, 10, 0, true);
            assertEquals(10, first10.size());
            var second10 = algoDao.getAll(tx, 10, 10, true);
            assertEquals(10, second10.size());

            assertTrue(all.containsAll(first10));
            assertTrue(all.containsAll(second10));

            assertEquals(all.getFirst(), first10.getFirst());
            assertEquals(all.get(9), first10.getLast());

            assertEquals(all.get(10), second10.getFirst());
            assertEquals(all.get(19), second10.getLast());



            tx.rollback();
        }
    }
}
