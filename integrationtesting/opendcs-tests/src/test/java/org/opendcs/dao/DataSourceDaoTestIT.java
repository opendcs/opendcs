package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DataSourceDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.DataSource;
import decodes.sql.DbKey;

@EnableIfTsDb
class DataSourceDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(DataSourceDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var dsIn = new DataSource("SimpleTestSource", "lrgs");
            dsIn.setDataSourceArg("key1=value1, key2=value2");

            final var dsOut = dao.save(tx, dsIn);

            assertFalse(DbKey.isNull(dsOut.getId()));

            var dsInGroup = new DataSource("GroupSource", "roundrobin");
            dsInGroup.addGroupMember(0, dsOut);

            var dsGroupOut = dao.save(tx, dsInGroup);
            assertFalse(dsGroupOut.groupMembers.isEmpty());


            dao.delete(tx, dsGroupOut.getId());
            var deletedOut = dao.getDataSource(tx, "GroupSource");
            assertTrue(deletedOut.isEmpty());
        }
    }
}
