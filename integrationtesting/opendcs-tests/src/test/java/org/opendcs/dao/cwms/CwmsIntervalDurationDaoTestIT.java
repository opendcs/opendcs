package org.opendcs.dao.cwms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.IntervalDurationDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb("CWMS-Oracle")
public class CwmsIntervalDurationDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_retrieve_interval_and_durations() throws Exception
    {
        var dao = db.getDao(IntervalDurationDao.class).orElseThrow();

        assertThrows(OpenDcsDataException.class, () -> dao.saveDuration(null, null));
        assertThrows(OpenDcsDataException.class, () -> dao.saveInterval(null, null));
        assertThrows(OpenDcsDataException.class, () -> dao.deleteInterval(null, null));
        assertThrows(OpenDcsDataException.class, () -> dao.deleteDuration(null, null));


        var oneHour = dao.findIntervalByName(null, "1Hour").orElseGet(() -> fail("No 1 hour interval for CWMS?"));
        assertEquals(Calendar.HOUR_OF_DAY, oneHour.getCalConstant());
        assertEquals(1, oneHour.getCalMultiplier());


        var threeHourDuration = dao.findDurationByName(null, "3Hours").orElseGet(() -> fail ("No 3 hour duration for CWMS?"));
        assertEquals(Calendar.HOUR_OF_DAY, threeHourDuration.getCalConstant());
        assertEquals(3, threeHourDuration.getCalMultiplier());

        var allIntervals = dao.getAllIntervals(null);
        var allDurations = dao.getAllDurations(null);
        assertFalse(allIntervals.isEmpty());
        assertFalse(allDurations.isEmpty());
        assertNotEquals(allIntervals.size(), allDurations.size());
    }
}
