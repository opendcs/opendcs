package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.IntervalDurationDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import opendcs.opentsdb.Interval;

/**
 * Intervals and Durations are very Implementation centric.
 * As such we do not attempt to create a single test to capture all logic.
 */
@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
class OpenDcsIntervalDurationDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_retrieve_intervals() throws Exception
    {
        final var dao = db.getDao(IntervalDurationDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var zero = dao.findByName(tx, "0")
                          .orElseGet(() -> fail("No zero interval available"));
            assertEquals(Calendar.MINUTE, zero.getCalConstant());
            assertEquals(0, zero.getCalMultiplier());
            var irreg = dao.findByName(tx, "irregular")
                           .orElseGet(() -> fail("no irregular interval available"));
            assertEquals(Calendar.MINUTE, irreg.getCalConstant());
            assertEquals(0, irreg.getCalMultiplier());
            var hour1 = dao.findByName(tx, "1Hour")
                           .orElseGet(() -> fail("No 1Hour interval available."));

            assertEquals(Calendar.HOUR_OF_DAY, hour1.getCalConstant());
            assertEquals(1, hour1.getCalMultiplier());
        }
    }
 
    
    @Test
    void test_saving_new_interval() throws Exception
    {
        final var dao = db.getDao(IntervalDurationDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var fiveWeeksIn = new Interval("5Weeks");
            fiveWeeksIn.setCalConstant(Calendar.WEEK_OF_YEAR);
            fiveWeeksIn.setCalMultiplier(5);

            var fiveWeeksOut = dao.saveInterval(tx, fiveWeeksIn);

            assertFalse(DbKey.isNull(fiveWeeksOut.getKey()));
            assertEquals(fiveWeeksIn.getCalConstant(), fiveWeeksOut.getCalConstant());
            assertEquals(fiveWeeksIn.getCalMultiplier(), fiveWeeksOut.getCalMultiplier());
            assertEquals(fiveWeeksIn.getName(), fiveWeeksOut.getName());

            dao.deleteInterval(tx, fiveWeeksOut.getKey());

            var fiveWeeksOut2 = dao.findById(tx, fiveWeeksOut.getKey());
            assertFalse(fiveWeeksOut2.isPresent());
        }
    }


    @Test
    void test_get_all() throws Exception
    {
        final var dao = db.getDao(IntervalDurationDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var intervals = dao.getAllIntervals(tx);
            assertFalse(intervals.isEmpty());

            var durations = dao.getAllDurations(tx);
            assertFalse(durations.isEmpty());

            assertEquals(intervals.size(), durations.size());
        }
    }
}
