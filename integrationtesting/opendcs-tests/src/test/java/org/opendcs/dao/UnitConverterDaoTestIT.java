package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.EngineeringUnit;
import decodes.sql.DbKey;

@EnableIfTsDb
class UnitConverterDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;
    
    @Test
    void test_unit_converter_retrieve() throws Exception
    {
        var ucDao = db.getDao(UnitConverterDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var uc = ucDao.getById(tx, DbKey.createDbKey(1));
            assertTrue(uc.isPresent());
        }
    }

    @Test
    void test_unit_retrieve_all() throws Exception
    {
        var unitDao = db.getDao(EngineeringUnitDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var units = unitDao.getEngineeringUnits(tx, -1, -1);
            assertFalse(units.isEmpty());
        }
    }

    /**
     * While normally I'd prefer to avoid such a contrieved test, this
     * seems better than needing to remember and update exactly which units end up where.
     * @throws Exception
     */
    @Test
    void test_unit_pagination() throws Exception
    {
        var unitDao = db.getDao(EngineeringUnitDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            for (int i = 0; i < 20; i++)
            {
                var unit = new EngineeringUnit(String.format("#aaaa.%03d", i), String.format("#aaaa,%03d", i), "univ", "nothing");
                unitDao.save(tx, unit);
            }

            var unitsAll = unitDao.getEngineeringUnits(tx, 20, 0);
            assertFalse(unitsAll.isEmpty());
            assertEquals("#aaaa.019", unitsAll.get(19).getAbbr(), () -> {
                return String.join(",", unitsAll.stream().map(Object::toString).toList());
            });

            var firstHalf = unitDao.getEngineeringUnits(tx, 10, 0);
            assertEquals("#aaaa.009", firstHalf.get(9).getAbbr());

            var secondHalf = unitDao.getEngineeringUnits(tx, 10, 10);
            assertEquals("#aaaa.019", secondHalf.get(9).getAbbr());

            tx.rollback();
        }
    }

    @Test
    void test_create_delete() throws Exception
    {
        var unitIn = new EngineeringUnit("flurgl", "Flurgle", "TotallyFake", "Length");
        var unitDao = db.getDao(EngineeringUnitDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var unitOut = unitDao.save(tx, unitIn);
            assertEquals(unitIn, unitOut);


            unitDao.delete(tx, unitIn.getAbbr());

            var unitShouldNotExist = unitDao.lookup(tx, unitIn.getName());
            assertFalse(unitShouldNotExist.isPresent());
        }
    }
}
