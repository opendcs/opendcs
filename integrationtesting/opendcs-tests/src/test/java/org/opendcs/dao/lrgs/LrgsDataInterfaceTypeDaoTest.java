package org.opendcs.dao.lrgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceTypeDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb("OpenDCS-Postgres")
class LrgsDataInterfaceTypeDaoTest extends AppTestBase
{
    @ConfiguredField 
    OpenDcsDatabase db;
  
    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(LrgsDataInterfaceTypeDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var typeIn = new LrgsDataInterfaceType(null, "testType", "provider", null, null);

            var typeOut = dao.save(tx, typeIn);

            assertNotNull(typeOut.id());
            assertNotNull(typeOut.createdAt());
            assertNotNull(typeOut.updatedAt());
            assertEquals("provider", typeOut.serviceProvider());
            assertEquals("testType", typeOut.name());

            var typeOutId = dao.getById(tx, typeOut.id())
                               .orElseGet(() -> fail("could not retrieve by id"));
            assertEquals(typeOut, typeOutId);

            var typeOutName = dao.getByName(tx, typeOut.name())
                                 .orElseGet(() -> fail("could not retrieve by name"));
            assertEquals(typeOut, typeOutName);

            var typeOutSp = dao.getByServiceProvider(tx, typeOut.serviceProvider())
                               .orElseGet(() -> fail("could not retrieve by service provider"));

            assertEquals(typeOut, typeOutSp);

            var typeInUpdate = new LrgsDataInterfaceType(typeOut.id(), "updated", typeOut.serviceProvider(), null, null);
            Thread.sleep(20); // needs a brief amount of time to that the update_at is different
            var typeOutUpdate = dao.save(tx, typeInUpdate);
            assertNotEquals(typeOut, typeOutUpdate);
            assertEquals(typeOut.createdAt(), typeOutUpdate.createdAt());
            assertNotEquals(typeOut.updatedAt(), typeOutUpdate.updatedAt());

            dao.delete(tx, typeOut.id());

            dao.getById(tx, typeOut.id())
               .ifPresent(type ->
                {
                    fail("Type was not deleted");
                });
            tx.rollback();
        }
        
    }

    @Test
    void test_paging() throws Exception
    {
        var dao = db.getDao(LrgsDataInterfaceTypeDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            final int COUNT = 50;
            for (int i = 0; i < COUNT; i++)
            {
                var typeName = String.format("AAA-Type-%03d", i);
                var providerName = String.format("AAA-Provider-%03d", i);
                var typeIn = new LrgsDataInterfaceType(null, typeName, providerName, null, null);
                dao.save(tx, typeIn);
            }

            var all = dao.getAll(tx, -1, -1);

            assertTrue(all.size() >= COUNT);

            var first10 = dao.getAll(tx, 10, 0);
            var second10 = dao.getAll(tx, 10, 10);

            assertEquals(all.getFirst(), first10.getFirst());
            assertEquals(all.get(9), first10.getLast());

            assertEquals(all.get(10), second10.getFirst());
            assertEquals(all.get(19), second10.getLast());

            tx.rollback();
        }
    }
}
