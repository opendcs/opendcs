package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

class EngineeringUnitsDAOTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;
    
    @Test
    void test_unit_retrieve() throws Exception
    {
        var unitDao = db.getDao(EngineeringUnitDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var unit = unitDao.lookup(tx, "ft");
            assertTrue(unit.isPresent());
        }
    }

}
