package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;

import decodes.db.DbEnum;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.EnumDAI;

class DbEnumDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @ConfiguredField
    private TimeSeriesDb tsDb;

    @Test
    void test_write_enum() throws Exception
    {
        Optional<EnumDAI> dai = db.getDao(EnumDAI.class);
        assertTrue(dai.isPresent(), "Unable to retrieve EnumDAI instance from database.");
        EnumDAI enumDai = dai.get();
        try (DataTransaction tx = db.newTransaction())
        {
            DbEnum dbEnum = new DbEnum("TestEnum");
            dbEnum.addValue("item1", "the first test item", null, null);
            dbEnum.addValue("item2", "the second test item", null, null);
            enumDai.writeEnum(tx, dbEnum);
        }

        try (DataTransaction tx = db.newTransaction())
        {
            Optional<DbEnum> dbEnumResult = enumDai.getEnum(tx, "TestEnum");
            assertTrue(dbEnumResult.isPresent(), "Unable to retrieve created enum.");
        }
    }
}
