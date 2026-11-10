package org.opendcs.dao.computations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.AlgorithmDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.ScriptType;

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
            var random = new Random(993443221); // this is not security code and is intented to be repeatable.
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

                if (i % 13 == 0)
                {
                    var beforeScript = new DbCompAlgorithmScript(algoIn,ScriptType.PY_BeforeTimeSlices);
                    var bigScriptBytes = new byte[i*300]; // intentionally uneven number to force edge case.
                    random.nextBytes(bigScriptBytes);
                    var hexString = HexFormat.of().formatHex(bigScriptBytes);
                    beforeScript.addToText(hexString);
                    algoIn.putScript(beforeScript);
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


    @Test
    void test_scripts() throws Exception
    {
        var algoDao = db.getDao(AlgorithmDao.class).orElseThrow();
        var random = new Random(53253); // This is not security code and is intended to be repeatable.
        try (var tx = db.newTransaction())
        {
            var algoIn = new DbCompAlgorithm("Test Algo");
            algoIn.setComment("I don't actually exist");
            algoIn.setExecClass("org.opendcs.Nothing");
            algoIn.setProperty("input_MISSING", "FAIL");
            algoIn.addParm(new DbAlgoParm("input", "i"));
            algoIn.addParm(new DbAlgoParm("output", "o"));

            var script = new DbCompAlgorithmScript(algoIn, ScriptType.PY_TimeSlice);
            script.addToText("One block, TimeSlice");
            algoIn.putScript(script);

            var beforeScript = new DbCompAlgorithmScript(algoIn,ScriptType.PY_BeforeTimeSlices);

            // We don't actually care what the text is here, we just
            // need it bigger than what OpenDCS's current table definition
            // defines as a single block.
            var bigScriptBytes = new byte[12530]; // intentionally uneven number to force edge case.
            random.nextBytes(bigScriptBytes);
            var hexString = HexFormat.of().formatHex(bigScriptBytes);
            beforeScript.addToText(hexString);
            algoIn.putScript(beforeScript);

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
}
