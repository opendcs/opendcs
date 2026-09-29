package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.alarm.Alarm;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmScreening;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;

class AlarmDaoTestIT extends AppTestBase
{
    /** ALARM_SCREENING.DATATYPE_ID has no foreign key, so a fixed value is used. */
    private static final DbKey DATATYPE_ID = DbKey.createDbKey(485_001L);

    /** A real time series, needed by the getAllCurrentAlarms test. TESTSITE1 comes from shared/test-sites.xml. */
    private static final String ALARM_TSID = "TESTSITE1.Stage.Inst.30Minutes.0.raw";

    @ConfiguredField
    private TimeSeriesDb tsDb;

    /**
     * getAllCurrentAlarms built an Alarm for every row and then never added it to the list it
     * returned, so it always came back empty and ShowAlarms always reported zero current alarms.
     * The limit set lookup after the loop was dead for the same reason.
     *
     * <p>This needs a real time series: the DAO skips any alarm whose TS_ID has no matching
     * identifier, so a made up key would leave the list empty either way.</p>
     *
     * <p>CWMS-Oracle is left out because its alarm tables lack LOADING_APPLICATION_ID in 7.0
     * (https://github.com/opendcs/opendcs/issues/485).</p>
     */
    @Test
    @EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
    @DecodesConfigurationRequired({"shared/test-sites.xml"})
    void test_get_all_current_alarms_returns_each_alarm() throws Exception
    {
        try (LoadingAppDAI appDao = tsDb.makeLoadingAppDAO();
             AlarmDAI alarmDao = tsDb.makeAlarmDAO();
             TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO())
        {
            CompAppInfo app = null;
            AlarmScreening screening = null;
            try
            {
                final TimeSeriesIdentifier tsid = tsDb.makeEmptyTsId();
                tsid.setUniqueString(ALARM_TSID);
                final DbKey tsKey = tsDao.createTimeSeries(tsid);

                app = writeApp(appDao, "alarm-current-it");
                screening = writeScreening(alarmDao, "alarm-current-it", app);

                final Alarm written = newAlarm(screening, app, tsKey);
                alarmDao.writeToCurrent(written);

                // Other tests may leave current alarms behind, so find ours rather than assert on the size.
                final Alarm found = alarmDao.getAllCurrentAlarms()
                    .stream()
                    .filter(a -> tsKey.equals(a.getTsidKey()))
                    .findFirst()
                    .orElse(null);

                assertNotNull(found, "getAllCurrentAlarms did not return the alarm that was just written.");
                assertNotNull(found.getTsid(), "The alarm's time series identifier was not resolved.");
                assertEquals(ALARM_TSID.toUpperCase(), found.getTsid().getUniqueString().toUpperCase());
                assertEquals(150.0, found.getDataValue(), 1e-6);
                assertNotNull(found.getLimitSet(), "The limit set was not assigned to the returned alarm.");
                assertEquals(screening.getLimitSets().get(0).getLimitSetId(), found.getLimitSet().getLimitSetId());
            }
            finally
            {
                // Deleting a screening also deletes its limit sets and their current and historical alarms.
                if (screening != null)
                {
                    alarmDao.deleteScreening(screening.getScreeningId());
                }
                if (app != null)
                {
                    appDao.deleteComputationApp(app);
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

    private static AlarmScreening writeScreening(AlarmDAI alarmDao, String name, CompAppInfo app)
        throws Exception
    {
        final AlarmLimitSet limits = new AlarmLimitSet();
        limits.setRejectHigh(100.0);

        final AlarmScreening screening = new AlarmScreening();
        screening.setScreeningName(name);
        screening.setDatatypeId(DATATYPE_ID);
        screening.setAppId(app.getAppId());
        screening.addLimitSet(limits);
        alarmDao.writeScreening(screening);

        assertFalse(DbKey.isNull(screening.getScreeningId()), "ALARM_SCREENING key was not generated.");
        assertFalse(DbKey.isNull(limits.getLimitSetId()), "ALARM_LIMIT_SET key was not generated.");
        return screening;
    }

    private static Alarm newAlarm(AlarmScreening screening, CompAppInfo app, DbKey tsKey)
    {
        final Date now = new Date();
        final Alarm alarm = new Alarm();
        alarm.setTsidKey(tsKey);
        alarm.setLimitSetId(screening.getLimitSets().get(0).getLimitSetId());
        alarm.setAssertTime(now);
        alarm.setDataTime(now);
        alarm.setDataValue(150.0);
        alarm.setMessage("Value 150.0 in REJECT_HIGH range.");
        alarm.setAppId(app.getAppId());
        return alarm;
    }
}
