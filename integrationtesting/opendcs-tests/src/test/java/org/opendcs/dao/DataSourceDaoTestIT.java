package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
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

        DataTransaction tx = null;
        try
        {
            tx = db.newTransaction();
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


            var daoByName = dao.getDataSource(tx, dsIn.getName());
            assertTrue(daoByName.isPresent());
            assertEquals(dsOut, daoByName.get());
        }
        finally
        {
            if (tx != null)
            {
                tx.rollback();
            }
        }
    }


    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(DataSourceDao.class).orElseThrow();
        DataTransaction tx = null;
        ArrayList<DataSource> sources = new ArrayList<>();
        try
        {

            tx = db.newTransaction();

            for (int i = 0; i < 30; i++)
            {
                final var name = String.format("aaaDS%03d", i);
                var dsTmp = new DataSource(name, "roundrobin");
                // entirely arbitrary, we just need a few groups to play havoc with the queries to validate
                // them.
                if (i > 3 && i % 7 == 0) 
                {
                    dsTmp.addGroupMember(0, sources.get(i-3));
                    dsTmp.addGroupMember(1, sources.get(i-2));
                }                
                var dsTmpOut = dao.save(tx, dsTmp);
                sources.add(dsTmpOut);
            }
            System.out.println(sources.size());

            var dsListOut = dao.getDataSources(tx, -1, -1);
            assertFalse(dsListOut.isEmpty(), "Unable to retrieve any sources");
            assertTrue(dsListOut.size() >= 30,
                       () -> "Expected number of sources was not present. Should be at least 30 however only " +
                             dsListOut.size() + " are present.");


            var dsListFirst10 = dao.getDataSources(tx, 10, 0); 
            assertFalse(dsListFirst10.isEmpty());
            var lastOfFirst10 = dsListFirst10.getLast();
            assertEquals("aaaDS009", lastOfFirst10.getName());
            assertTrue(lastOfFirst10.groupMembers.isEmpty());
            var seventh =dsListFirst10.get(7);
            assertFalse(seventh.groupMembers.isEmpty());


            var dsListSecond10 = dao.getDataSources(tx, 10, 10);
            assertFalse(dsListSecond10.isEmpty());
            var firstOfSecond10 = dsListSecond10.getFirst();
            assertEquals("aaaDS010", firstOfSecond10.getName());

        }
        finally
        {
            if (tx != null)
            {
                tx.rollback();
            }
        }
    }
}
