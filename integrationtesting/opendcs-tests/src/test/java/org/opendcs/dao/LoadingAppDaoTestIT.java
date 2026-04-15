package org.opendcs.dao;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;


@EnableIfTsDb
class LoadingAppDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(LoadingAppDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var appInfo = new CompAppInfo();
            appInfo.setAppName("test-app");
            appInfo.setManualEditApp(false);
            appInfo.setComment("Basic operations");
            appInfo.setProperty("test-prop-1", "test-value-1");
            appInfo.setProperty("test-prop-2", "test-value-2");

            final var appInfoOut = dao.save(tx, appInfo);
            assertTrue(appInfo.equalsNoId(appInfoOut));

            final var id = appInfoOut.getAppId();
            assertFalse(DbKey.isNull(id));

            final var appInfoOutId = dao.getById(tx, id);
            assertTrue(appInfoOutId.isPresent());
            assertTrue(appInfo.equalsNoId(appInfoOutId.get()));

            final var appInfoOutName = dao.getByName(tx, appInfoOut.getAppName());
            assertTrue(appInfoOutName.isPresent());
            assertTrue(appInfo.equalsNoId(appInfoOutName.get()));

            dao.delete(tx, appInfoOut.getId());

            final var appInfoEmpty = dao.getById(tx, appInfoOut.getId());
            assertTrue(appInfoEmpty.isEmpty());
        }
    }

    @Test
    void test_pagination_operations() throws Exception
    {
        var dao = db.getDao(LoadingAppDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            final var numApps = 50;
            for (int i = 0; i < numApps; i++)
            {
                var appIn = new CompAppInfo();
                appIn.setAppName(String.format("AAATest-App-%02d", i));
                appIn.setComment("Hello from test app " + i);
                if (i % 7 == 0)
                {
                    appIn.setProperty("test_prop_1", "test_val_1");
                    appIn.setProperty("test_prop_2", "test_val_2");
                }

                dao.save(tx, appIn);
            }

            var allApps = dao.getAll(tx, -1, -1);
            assertTrue(allApps.size() >= numApps);

            var first50 = dao.getAll(tx, numApps, 0);
            assertEquals(numApps, first50.size());

            

            final var first10 = dao.getAll(tx, 10, 0);
            assertEquals(10, first10.size());

            assertEquals("AAATest-App-09", first10.getLast().getAppName(), () ->
            {
                return String.join(",", first10.stream().map(ai -> ai.getAppName()).toList());
            });

            final var idx7 = first10.get(7);
            assertEquals("test_val_1", idx7.getProperty("test_prop_1"));

            var second10 = dao.getAll(tx, 10, 10);
            assertEquals(10, second10.size());

            assertEquals("AAATest-App-19", second10.getLast().getAppName());

            tx.rollback();
        }
    }
    
}
