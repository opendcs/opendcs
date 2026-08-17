package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.CompLockDao;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.tsdb.CompAppInfo;

@EnableIfTsDb
class CompLockDaoTestIT extends AppTestBase
{

    private static CompAppInfo appOne;
    private static CompAppInfo appTwo;

    @ConfiguredField
    static OpenDcsDatabase db;


    @BeforeAll
    static void setupApps() throws Exception
    {
        var appDao = db.getDao(LoadingAppDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var tmp = new CompAppInfo();
            tmp.setAppName("app1");
            appOne = appDao.save(tx, tmp);

            tmp.setAppName("app2");
            appTwo = appDao.save(tx, tmp);
        }
    }

    @Test
    void test_lock_operations() throws Exception
    {

        var lockDao = db.getDao(CompLockDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var lockOne = lockDao.obtainLock(tx, appOne, 0, "bob");
            assertTrue(lockOne.isSuccess());
            var lockOneFail = lockDao.obtainLock(tx, appOne, 0, "alice");
            assertTrue(lockOneFail.isFailure());

            Thread.sleep(1500); // NOSONAR otherwise the dates will match.

            var lockOneUpdate = lockDao.obtainLock(tx, appOne, 0, "bob");
            assertTrue(lockOneUpdate.isSuccess());
            var firstLock = lockOne.getSuccess();

            var updatedLock = lockOneUpdate.getSuccess();
            assertNotEquals(firstLock.getHeartbeat(), updatedLock.getHeartbeat());

            var lockTwo = lockDao.obtainLock(tx, appTwo, 0, "bob");
            assertTrue(lockTwo.isSuccess());

            var locks = lockDao.getAll(tx, -1, -1);
            assertTrue(locks.size() >= 2);

            assertTrue(locks.stream()
                            .anyMatch(p -> p.getAppId() == appOne.getAppId() || p.getAppId() == appTwo.getAppId()));

            lockDao.releaseLock(tx, updatedLock);

            assertFalse(lockDao.getLock(tx, updatedLock.getAppId()).isPresent());

            tx.rollback();
        }
    }
}
