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
        var ucDao = db.getDao(UnitConverterDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var unitConverter0to20 = ucDao.getUnitConverterDbs(tx, 20, 0);
            assertFalse(unitConverter0to20.isEmpty());

            var firstHalf = ucDao.getUnitConverterDbs(tx, 10, 0);
            assertEquals(unitConverter0to20.get(9), firstHalf.get(9));

            var secondHalf = ucDao.getUnitConverterDbs(tx, 10, 10);
            assertEquals(unitConverter0to20.get(19), secondHalf.get(9));

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
