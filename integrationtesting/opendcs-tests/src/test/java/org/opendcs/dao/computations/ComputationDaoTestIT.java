package org.opendcs.dao.computations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.AlgorithmDao;
import org.opendcs.database.dai.ComputationDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;

@EnableIfTsDb 
public class ComputationDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basics() throws Exception
    {
        var compDao = db.getDao(ComputationDao.class).orElseThrow();
        var algoDao = db.getDao(AlgorithmDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var all = compDao.getAll(tx, 0, 0);            
            assertFalse(all.isEmpty());
            var algo = algoDao.getByName(tx, "SubSample")
                              .orElseGet(() -> fail("Could not retrieve an algorithm for test."));
            var compIn = new DbComputation(DbKey.NullKey, "DAO Test Comp");
            compIn.setAlgorithm(algo);
            compIn.setAlgorithmId(algo.getId());

            compIn.addParm(new DbCompParm("input", DbKey.NullKey, "15Minutes", "15Minutes..", 0));
            compIn.addParm(new DbCompParm("outpu", DbKey.NullKey, "1Hour", "15Minutes..", 0));

            var compOut = compDao.save(tx, compIn);

            assertTrue(compOut.equalsNoId(compIn));

            
        }
    }
}
