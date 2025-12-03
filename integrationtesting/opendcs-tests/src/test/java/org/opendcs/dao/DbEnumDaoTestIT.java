package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.DbEnum;
import opendcs.dai.EnumDAI;

class DbEnumDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_write_enum() throws Exception
    {
        Optional<EnumDAI> dai = db.getDao(EnumDAI.class);
        assertTrue(dai.isPresent(), "Unable to retrieve EnumDAI instance from database.");
        EnumDAI enumDai = dai.get();
        try (DataTransaction tx = db.newTransaction())
        {
            DbEnum dbEnum = new DbEnum("TestEnum");
            dbEnum.addValue("item1", "the first test item", null, null).setSortNumber(1);
            dbEnum.addValue("item2", "the second test item", "exec", null).setSortNumber(2);
            dbEnum.addValue("item3", "the second test item", null, "edit").setSortNumber(3);
            dbEnum.addValue("item4", "the second test item", "exec", "edit").setSortNumber(4);
            dbEnum.addValue("item5", "the second test item", "exec", "edit").setSortNumber(5);
            enumDai.writeEnum(tx, dbEnum);
        }

        try (DataTransaction tx = db.newTransaction())
        {
            Optional<DbEnum> dbEnumResult = enumDai.getEnum(tx, "TestEnum");
            assertTrue(dbEnumResult.isPresent(), "Unable to retrieve created enum.");

            enumDai.deleteEnum(tx, dbEnumResult.get().getId());
        }
    }

    @Test
    @EnableIfTsDb
    void test_pagination() throws Exception
    {
        EnumDAI dai = db.getDao(EnumDAI.class)
                        .orElseGet(() -> fail("Unable to retrieve EnumDAI instance from database."));
        try (var tx = db.newTransaction())
        {
            for (int i = 0; i < 10; i++)
            {
                DbEnum dbEnum = new DbEnum("TestEnum"+i);
                dbEnum.addValue("item1", "the first test item of " + i, null, null).setSortNumber(1);
                dbEnum.addValue("item2", "the second test item of" + i, "exec", null).setSortNumber(2);
                dbEnum.addValue("item3", "the second test item of" + i, null, "edit").setSortNumber(3);
                dbEnum.addValue("item4", "the second test item of" + i, "exec", "edit").setSortNumber(4);
                dbEnum.addValue("item5", "the second test item of" + i, "exec", "edit").setSortNumber(5);
                dai.writeEnum(tx, dbEnum);
            }

            var all = dai.getEnums(tx, -1, -1);
            assertTrue(all.size() >= 10);

            var dbEnums = dai.getEnums(tx, 2, 0);
            dbEnums.forEach(System.out::println);
            assertEquals(2, dbEnums.size());

            var secondDbEnums = dai.getEnums(tx, 2, 2);
            assertEquals(2, secondDbEnums.size());

            assertNotEquals(dbEnums.toArray()[0], secondDbEnums.toArray()[0]);
            assertNotEquals(dbEnums.toArray()[1], secondDbEnums.toArray()[1]);

            tx.rollback();
        }
    }
}
