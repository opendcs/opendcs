package org.opendcs.dao.computations;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.ComputationDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb 
public class ComputationDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    void test_basics() throws Exception
    {
        var compDao = db.getDao(ComputationDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var all = compDao.getAll(tx, 0, 0);
            assertFalse(all.isEmpty());
        }
    }
}
