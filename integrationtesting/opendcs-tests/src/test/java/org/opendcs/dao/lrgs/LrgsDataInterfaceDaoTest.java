package org.opendcs.dao.lrgs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opendcs.data.lrgs.LrgsDataInterface;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceDao;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceTypeDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb("OpenDCS-Postgres")
class LrgsDataInterfaceDaoTest extends AppTestBase
{
    @ConfiguredField 
    OpenDcsDatabase db;
  
    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(LrgsDataInterfaceDao.class).orElseThrow();
        var typeDao = db.getDao(LrgsDataInterfaceTypeDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var typeIn = new LrgsDataInterfaceType(null, "testType", "provider", null, null);

            var typeOut = typeDao.save(tx, typeIn);

            var interfaceIn = new LrgsDataInterface(null, "test", typeOut, Map.of(), null, null );

            var interfaceOut = dao.save(tx, interfaceIn);

            assertNotNull(interfaceOut.id());
            assertNotNull(interfaceOut.createdAt());
            assertNotNull(interfaceOut.updatedAt());

            var interfaceOutId = dao.getById(tx, interfaceOut.id())
                               .orElseGet(() -> fail("could not retrieve by id"));
            assertEquals(interfaceOut, interfaceOutId);

            var interfaceOutName = dao.getByName(tx, interfaceOut.name())
                                 .orElseGet(() -> fail("could not retrieve by name"));
            assertEquals(interfaceOut, interfaceOutName);

            var interfaceInUpdate = new LrgsDataInterface(interfaceOut.id(), interfaceOut.name(), interfaceOut.type(), Map.of("test", "value"), null, null);
            Thread.sleep(20); // needs a brief amount of time to that the update_at is different
            var interfaceOutUpdate = dao.save(tx, interfaceInUpdate);
            assertNotEquals(interfaceOut, interfaceOutUpdate);
            assertEquals(interfaceOut.createdAt(), interfaceOutUpdate.createdAt());
            assertNotEquals(interfaceOut.updatedAt(), interfaceOutUpdate.updatedAt());
            var config = interfaceOutUpdate.config();
            assertNotNull(config);
            assertEquals("value", config.get("test"));

            dao.delete(tx, interfaceOut.id());

            dao.getById(tx, interfaceOut.id())
               .ifPresent(i ->
                {
                    fail("Interface was not deleted");
                });

            tx.rollback();
        }
    }

    @Test
    void test_paging() throws Exception
    {
        var dao = db.getDao(LrgsDataInterfaceDao.class).orElseThrow();
        var typeDao = db.getDao(LrgsDataInterfaceTypeDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var typeIn = new LrgsDataInterfaceType(null, "testType", "provider", null, null);
            var typeOut = typeDao.save(tx, typeIn);

            final int COUNT = 50;
            for (int i = 0; i < COUNT; i++)
            {
                var name = String.format("AAA-Interface-%03d", i);
                var interfaceIn = new LrgsDataInterface(null, name, typeOut, Map.of("value", i), null, null);
                dao.save(tx, interfaceIn);
            }

            var all = dao.getAll(tx, -1, -1);

            assertTrue(all.size() >= COUNT);

            var first10 = dao.getAll(tx, 10, 0);
            var second10 = dao.getAll(tx, 10, 10);

            assertEquals(all.getFirst(), first10.getFirst());
            assertEquals(all.get(9), first10.getLast());

            assertEquals(all.get(10), second10.getFirst());
            assertEquals(all.get(19), second10.getLast());

            assertEquals(25, all.get(25).config().get("value"));

            tx.rollback();
        }
    }
}
