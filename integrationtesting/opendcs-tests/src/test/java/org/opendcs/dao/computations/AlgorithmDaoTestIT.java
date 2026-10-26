package org.opendcs.dao.computations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
}
