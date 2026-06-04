package org.opendcs.database.impl.cwms.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.IntervalDurationDao;
import org.openide.util.lookup.ServiceProvider;

import decodes.sql.DbKey;
import decodes.tsdb.IntervalCodes;
import opendcs.opentsdb.Interval;

/**
 * While this class currently does no actual Database access, that is planned for the future.
 * Such updates will require sorting out how to get the cwms encoding of durations and intervals
 * into the Interval structure of OpenDCS. In the mean time, we continue to just hard code things.
 */
@ServiceProvider(service = IntervalDurationDao.class, path = "dao/CWMS-Oracle")
public class CwmsIntervalDurationDao implements IntervalDurationDao
{
    private static final List<Interval> intervals = new ArrayList<>();
    private static final List<Interval> durations = new ArrayList<>();
    private static final OpenDcsDataException INTERVALS_CONTROLLED = new OpenDcsDataException("CWMS Intervals are controlled by the CWMS Database.");
    private static final OpenDcsDataException DURATIONS_CONTROLLED = new OpenDcsDataException("CWMS Durations are controlled by the CWMS Database.");

    static
    {
        loadIntervals();
        loadDurations();
    }

    @Override
    public Optional<Interval> findIntervalByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return intervals.stream()
                        .filter(interval -> interval.getName().equals(name))
                        .findFirst();
    }

    @Override
    public Optional<Interval> findIntervalById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return intervals.stream()
                        .filter(interval -> interval.getKey().equals(id))
                        .findFirst();
    }

    @Override
    public Optional<Interval> findDurationByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return durations.stream()
                        .filter(duration -> duration.getName().equals(name))
                        .findFirst();
    }

    @Override
    public Optional<Interval> findDurationById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return durations.stream()
                        .filter(duration -> duration.getKey().equals(id))
                        .findFirst();
    }

    @Override
    public Interval saveInterval(DataTransaction tx, Interval interval) throws OpenDcsDataException
    {
        throw INTERVALS_CONTROLLED;
    }

    @Override
    public Interval saveDuration(DataTransaction tx, Interval interval) throws OpenDcsDataException
    {
        throw DURATIONS_CONTROLLED;
    }

    @Override
    public void deleteInterval(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        throw INTERVALS_CONTROLLED;
    }

    @Override
    public void deleteDuration(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        throw DURATIONS_CONTROLLED;
    }

    @Override
    public List<Interval> getAllIntervals(DataTransaction tx) throws OpenDcsDataException
    {
        return Collections.unmodifiableList(intervals);
    }

    @Override
    public List<Interval> getAllDurations(DataTransaction tx) throws OpenDcsDataException
    {
        return Collections.unmodifiableList(durations);
    }

    
    private static void loadDurations()
    {
        long id = 1L;
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_irregular, 0, 0));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_zero, 0, 0));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_other, Calendar.MINUTE, 5));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_5min, Calendar.MINUTE, 5));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_6min, Calendar.MINUTE, 6));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_10min, Calendar.MINUTE, 10));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_15min, Calendar.MINUTE, 15));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_20min, Calendar.MINUTE, 20));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_30min, Calendar.MINUTE, 30));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_minute, Calendar.MINUTE, 1));		
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_minutes, Calendar.MINUTE, 2));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_minutes, Calendar.MINUTE, 3));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_minutes, Calendar.MINUTE, 4));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_minutes, Calendar.MINUTE, 5));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_minutes, Calendar.MINUTE, 6));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_ten_minutes, Calendar.MINUTE, 10));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_minutes, Calendar.MINUTE, 12));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_fifteen_minutes, Calendar.MINUTE, 15));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twenty_minutes, Calendar.MINUTE, 20));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_thirty_minutes, Calendar.MINUTE, 30));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_hour, Calendar.HOUR_OF_DAY, 1));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_hours, Calendar.HOUR_OF_DAY, 2));

		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_hours, Calendar.HOUR_OF_DAY, 3));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_hours, Calendar.HOUR_OF_DAY, 4));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_hours, Calendar.HOUR_OF_DAY, 6));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_eight_hours, Calendar.HOUR_OF_DAY, 8));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_hours, Calendar.HOUR_OF_DAY, 12));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_day, Calendar.DAY_OF_MONTH, 1));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_days, Calendar.DAY_OF_MONTH, 2));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_days, Calendar.DAY_OF_MONTH, 3));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_days, Calendar.DAY_OF_MONTH, 4));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_days, Calendar.DAY_OF_MONTH, 5));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_days, Calendar.DAY_OF_MONTH, 6));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_week, Calendar.WEEK_OF_YEAR, 1));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_month, Calendar.MONTH, 1));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_year, Calendar.YEAR, 1));
		durations.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_decade, Calendar.YEAR, 10));
    }

    private static void loadIntervals()
    {
        long id = 1L;
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_irregular, 0, 0));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_zero, 0, 0));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_other, Calendar.MINUTE, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_5min, Calendar.MINUTE, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_6min, Calendar.MINUTE, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_10min, Calendar.MINUTE, 10));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_15min, Calendar.MINUTE, 15));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_20min, Calendar.MINUTE, 20));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_30min, Calendar.MINUTE, 30));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_minute, Calendar.MINUTE, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_minute_nc, Calendar.MINUTE, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_minutes, Calendar.MINUTE, 2));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_minutes_nc, Calendar.MINUTE, 2));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_minutes, Calendar.MINUTE, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_minutes_nc, Calendar.MINUTE, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_minutes, Calendar.MINUTE, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_minutes_nc, Calendar.MINUTE, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_minutes, Calendar.MINUTE, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_minutes_nc, Calendar.MINUTE, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_minutes, Calendar.MINUTE, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_minutes_nc, Calendar.MINUTE, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_ten_minutes, Calendar.MINUTE, 10));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_ten_minutes_nc, Calendar.MINUTE, 10));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_minutes, Calendar.MINUTE, 12));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_minutes_nc, Calendar.MINUTE, 12));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_fifteen_minutes, Calendar.MINUTE, 15));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_fifteen_minutes_nc, Calendar.MINUTE, 15));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twenty_minutes, Calendar.MINUTE, 20));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twenty_minutes_nc, Calendar.MINUTE, 20));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_thirty_minutes, Calendar.MINUTE, 30));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_thirty_minutes_nc, Calendar.MINUTE, 30));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_hour, Calendar.HOUR_OF_DAY, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_hour_nc, Calendar.HOUR_OF_DAY, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_hours, Calendar.HOUR_OF_DAY, 2));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_hours_nc, Calendar.HOUR_OF_DAY, 2));

		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_hours, Calendar.HOUR_OF_DAY, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_hours_dst, Calendar.HOUR_OF_DAY, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_hours, Calendar.HOUR_OF_DAY, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_hours_dst, Calendar.HOUR_OF_DAY, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_hours, Calendar.HOUR_OF_DAY, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_hours_dst, Calendar.HOUR_OF_DAY, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_eight_hours, Calendar.HOUR_OF_DAY, 8));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_eight_hours_dst, Calendar.HOUR_OF_DAY, 8));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_hours, Calendar.HOUR_OF_DAY, 12));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_hours_dst, Calendar.HOUR_OF_DAY, 12));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_day, Calendar.DAY_OF_MONTH, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_day_dst, Calendar.DAY_OF_MONTH, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_days, Calendar.DAY_OF_MONTH, 2));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_days_dst, Calendar.DAY_OF_MONTH, 2));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_days, Calendar.DAY_OF_MONTH, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_days_dst, Calendar.DAY_OF_MONTH, 3));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_days, Calendar.DAY_OF_MONTH, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_days_dst, Calendar.DAY_OF_MONTH, 4));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_days, Calendar.DAY_OF_MONTH, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_days_dst, Calendar.DAY_OF_MONTH, 5));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_days, Calendar.DAY_OF_MONTH, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_days_dst, Calendar.DAY_OF_MONTH, 6));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_week, Calendar.WEEK_OF_YEAR, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_week_dst, Calendar.WEEK_OF_YEAR, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_month, Calendar.MONTH, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_month_dst, Calendar.MONTH, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_year, Calendar.YEAR, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_year_dst, Calendar.YEAR, 1));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_decade, Calendar.YEAR, 10));
		intervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_decade_dst, Calendar.YEAR, 10));
    }

}
