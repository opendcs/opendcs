package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.alarm.Alarm;
import decodes.tsdb.alarm.AlarmEvent;
import decodes.tsdb.alarm.AlarmGroup;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.alarm.ProcessMonitor;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dao.DaoBase;

/**
 * Writes alarm groups, screenings, and current and historical alarms for two loading applications.
 * This needs a key from each alarm table's sequence and the LOADING_APPLICATION_ID columns, both of
 * which were missing from the CWMS-Oracle schema (https://github.com/opendcs/opendcs/issues/485).
 */
@EnableIfTsDb
class AlarmDaoTestIT extends AppTestBase
{
    /** ALARM_SCREENING.DATATYPE_ID and the alarm TS_ID columns have no foreign keys, so fixed values are used. */
    private static final DbKey DATATYPE_ID = DbKey.createDbKey(485_001L);
    private static final DbKey TS_ID = DbKey.createDbKey(485_002L);

    @ConfiguredField
    private TimeSeriesDb tsDb;

    @Test
    void test_screenings_and_alarms_per_loading_application() throws Exception
    {
        try (LoadingAppDAI appDao = tsDb.makeLoadingAppDAO();
             AlarmDAI alarmDao = tsDb.makeAlarmDAO())
        {
            final DaoBase dao = (DaoBase) alarmDao;
            CompAppInfo appA = null;
            CompAppInfo appB = null;
            AlarmGroup group = null;
            AlarmScreening screeningA = null;
            AlarmScreening screeningB = null;
            try
            {
                appA = writeApp(appDao, "alarm-dao-it-a");
                appB = writeApp(appDao, "alarm-dao-it-b");

                group = new AlarmGroup(DbKey.NullKey);
                group.setName("alarm-dao-it-group");
                final ProcessMonitor monitor = new ProcessMonitor(appA.getAppId());
                final AlarmEvent event = new AlarmEvent(DbKey.NullKey);
                event.setPattern("alarm-dao-it");
                monitor.getDefs().add(event);
                group.getProcessMonitors().add(monitor);
                alarmDao.write(group);
                assertFalse(DbKey.isNull(group.getAlarmGroupId()), "ALARM_GROUP key was not generated.");
                assertFalse(DbKey.isNull(event.getAlarmEventId()), "ALARM_EVENT key was not generated.");

                // Same site, datatype, and start time. Only allowed because the loading applications differ.
                screeningA = writeScreening(alarmDao, "alarm-dao-it-a", appA, group);
                screeningB = writeScreening(alarmDao, "alarm-dao-it-b", appB, group);

                final ArrayList<AlarmScreening> forAppA =
                    alarmDao.getScreenings(DbKey.NullKey, DATATYPE_ID, appA.getAppId());
                assertEquals(1, forAppA.size(), "Expected only the screening for loading application A.");
                assertEquals(screeningA.getScreeningId(), forAppA.get(0).getScreeningId());

                // writeToCurrent and moveToHistory log database errors instead of throwing, so check the rows directly.
                final Alarm alarmA = newAlarm(screeningA, appA);
                alarmDao.writeToCurrent(alarmA);
                alarmDao.writeToCurrent(newAlarm(screeningB, appB));
                assertEquals(1, countAlarms(dao, "alarm_current", appA));
                assertEquals(1, countAlarms(dao, "alarm_current", appB),
                             "A second loading application's alarm on the same time series was not saved.");

                alarmA.setEndTime(new Date());
                alarmDao.moveToHistory(alarmA);
                assertEquals(0, countAlarms(dao, "alarm_current", appA));
                assertEquals(1, countAlarms(dao, "alarm_history", appA));
                assertEquals(1, countAlarms(dao, "alarm_current", appB));
            }
            finally
            {
                // Deleting a screening also deletes its limit sets and their current and historical alarms.
                if (screeningA != null)
                {
                    alarmDao.deleteScreening(screeningA.getScreeningId());
                }
                if (screeningB != null)
                {
                    alarmDao.deleteScreening(screeningB.getScreeningId());
                }
                if (group != null)
                {
                    alarmDao.deleteAlarmGroup(group.getAlarmGroupId());
                }
                if (appA != null)
                {
                    appDao.deleteComputationApp(appA);
                }
                if (appB != null)
                {
                    appDao.deleteComputationApp(appB);
                }
            }
        }
    }

    private static CompAppInfo writeApp(LoadingAppDAI appDao, String name) throws Exception
    {
        final CompAppInfo app = new CompAppInfo();
        app.setAppName(name);
        appDao.writeComputationApp(app);
        assertFalse(DbKey.isNull(app.getAppId()), "Loading application key was not generated.");
        return app;
    }

    private static AlarmScreening writeScreening(AlarmDAI alarmDao, String name, CompAppInfo app, AlarmGroup group)
        throws Exception
    {
        final AlarmLimitSet limits = new AlarmLimitSet();
        limits.setRejectHigh(100.0);

        final AlarmScreening screening = new AlarmScreening();
        screening.setScreeningName(name);
        screening.setDatatypeId(DATATYPE_ID);
        screening.setAppId(app.getAppId());
        screening.setAlarmGroupId(group.getAlarmGroupId());
        screening.addLimitSet(limits);
        alarmDao.writeScreening(screening);

        assertFalse(DbKey.isNull(screening.getScreeningId()), "ALARM_SCREENING key was not generated.");
        assertFalse(DbKey.isNull(limits.getLimitSetId()), "ALARM_LIMIT_SET key was not generated.");
        return screening;
    }

    private static Alarm newAlarm(AlarmScreening screening, CompAppInfo app)
    {
        final Date now = new Date();
        final Alarm alarm = new Alarm();
        alarm.setTsidKey(TS_ID);
        alarm.setLimitSetId(screening.getLimitSets().get(0).getLimitSetId());
        alarm.setAssertTime(now);
        alarm.setDataTime(now);
        alarm.setDataValue(150.0);
        alarm.setMessage("Value 150.0 in REJECT_HIGH range.");
        alarm.setAppId(app.getAppId());
        return alarm;
    }

    private static int countAlarms(DaoBase dao, String table, CompAppInfo app) throws Exception
    {
        return dao.getSingleResult(
            "select count(*) from " + table + " where ts_id = ? and loading_application_id = ?",
            rs -> rs.getInt(1),
            TS_ID, app.getAppId());
    }
}
