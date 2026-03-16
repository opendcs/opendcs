package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EngineeringUnitDao;
import org.opendcs.database.dai.UnitConverterDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.Constants;
import decodes.db.EngineeringUnit;
import decodes.db.UnitConverterDb;
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


            var ucFtToM = ucDao.findUnitConverterFor(tx, "ft", "m").orElseThrow();
            var meters = ucFtToM.execConverter.convert(3.28084);
            assertEquals(1, meters, 0.0001);

        }
    }

    @Test
    void test_unit_retrieve_all() throws Exception
    {
        var unitDao = db.getDao(UnitConverterDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var unitConverters = unitDao.getUnitConverterDbs(tx, -1, -1);
            assertFalse(unitConverters.isEmpty());

            for (var unitConvert: unitConverters)
            {
                if ("raw".equalsIgnoreCase(unitConvert.fromAbbr))
                {
                    fail("Unit conversion from raw was found in retrieve all list.");
                }
            }
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
        var ucIn = new UnitConverterDb("raw", "ft");
        ucIn.algorithm = "linear";
        ucIn.coefficients = new double[]{10.0, 0.0, Constants.undefinedDouble, Constants.undefinedDouble, Constants.undefinedDouble, Constants.undefinedDouble};

        
        var ucDao = db.getDao(UnitConverterDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var ucOut = ucDao.save(tx, ucIn);
            assertEquals(ucIn, ucOut);


            ucDao.delete(tx, ucOut.getId());

            var unitShouldNotExist = ucDao.getById(tx, ucOut.getId());
            assertFalse(unitShouldNotExist.isPresent());
        }
    }
}
