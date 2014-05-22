package decodes.cwms;

import java.util.Calendar;

import decodes.db.IntervalList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import opendcs.dai.IntervalDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.Interval;

/**
 * Read/Write Interval objects in CWMS
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class CwmsIntervalDAO extends DaoBase implements IntervalDAI
{
	String dbOfficeId = null;

	// TODO: Read these from the database CWMS_V_INTERVAL view
	private static final String[] validIntervalCodes = 
	{
	    IntervalCodes.int_cwms_irregular, 
	    IntervalCodes.int_cwms_zero,
	    IntervalCodes.int_one_minute,
	    IntervalCodes.int_one_minute_nc,
	    IntervalCodes.int_two_minutes, 
	    IntervalCodes.int_two_minutes_nc, 
	    IntervalCodes.int_three_minutes,
	    IntervalCodes.int_three_minutes_nc,
	    IntervalCodes.int_four_minutes, 
	    IntervalCodes.int_four_minutes_nc, 
	    IntervalCodes.int_five_minutes,
	    IntervalCodes.int_five_minutes_nc,
	    IntervalCodes.int_six_minutes, 
	    IntervalCodes.int_six_minutes_nc, 
	    IntervalCodes.int_ten_minutes,
	    IntervalCodes.int_ten_minutes_nc,
	    IntervalCodes.int_twelve_minutes, 
	    IntervalCodes.int_twelve_minutes_nc, 
	    IntervalCodes.int_fifteen_minutes,
	    IntervalCodes.int_fifteen_minutes_nc,
	    IntervalCodes.int_twenty_minutes, 
	    IntervalCodes.int_twenty_minutes_nc, 
	    IntervalCodes.int_thirty_minutes,
	    IntervalCodes.int_thirty_minutes_nc,
	    IntervalCodes.int_one_hour, 
	    IntervalCodes.int_one_hour_nc, 
	    IntervalCodes.int_two_hours,
	    IntervalCodes.int_two_hours_nc,
	    
	    IntervalCodes.int_three_hours, 
	    IntervalCodes.int_three_hours_dst, 
	    IntervalCodes.int_four_hours,
	    IntervalCodes.int_four_hours_dst,
	    IntervalCodes.int_six_hours, 
	    IntervalCodes.int_six_hours_dst, 
	    IntervalCodes.int_eight_hours,
	    IntervalCodes.int_eight_hours_dst,
	    IntervalCodes.int_twelve_hours, 
	    IntervalCodes.int_twelve_hours_dst, 
	    IntervalCodes.int_one_day,
	    IntervalCodes.int_one_day_dst,
	    IntervalCodes.int_two_days,
	    IntervalCodes.int_two_days_dst,
	    IntervalCodes.int_three_days,
	    IntervalCodes.int_three_days_dst,
	    IntervalCodes.int_four_days,
	    IntervalCodes.int_four_days_dst,
	    IntervalCodes.int_five_days,
	    IntervalCodes.int_five_days_dst,
	    IntervalCodes.int_six_days,
	    IntervalCodes.int_six_days_dst,
	    IntervalCodes.int_one_week, 
	    IntervalCodes.int_one_week_dst,
	    IntervalCodes.int_one_month,
	    IntervalCodes.int_one_month_dst,
	    IntervalCodes.int_one_year, 
	    IntervalCodes.int_one_year_dst,
	    IntervalCodes.int_one_decade,
	    IntervalCodes.int_one_decade_dst
	};
	
	// TODO: Read these from the database CWMS_V_DURATION view
	private static final String[] validDurationCodes = 
	{
	    IntervalCodes.int_cwms_irregular, 
	    IntervalCodes.int_cwms_zero,
	    IntervalCodes.int_one_minute,
	    IntervalCodes.int_two_minutes, 
	    IntervalCodes.int_three_minutes,
	    IntervalCodes.int_four_minutes, 
	    IntervalCodes.int_five_minutes,
	    IntervalCodes.int_six_minutes, 
	    IntervalCodes.int_ten_minutes,
	    IntervalCodes.int_twelve_minutes, 
	    IntervalCodes.int_fifteen_minutes,
	    IntervalCodes.int_twenty_minutes, 
	    IntervalCodes.int_thirty_minutes,
	    IntervalCodes.int_one_hour, 
	    IntervalCodes.int_two_hours,
	    IntervalCodes.int_three_hours, 
	    IntervalCodes.int_four_hours,
	    IntervalCodes.int_six_hours, 
	    IntervalCodes.int_eight_hours,
	    IntervalCodes.int_twelve_hours, 
	    IntervalCodes.int_one_day,
	    IntervalCodes.int_two_days,
	    IntervalCodes.int_three_days,
	    IntervalCodes.int_four_days,
	    IntervalCodes.int_five_days,
	    IntervalCodes.int_six_days,
	    IntervalCodes.int_one_week, 
	    IntervalCodes.int_one_month,
	    IntervalCodes.int_one_year, 
	    IntervalCodes.int_one_decade
	};

	
	public CwmsIntervalDAO(DatabaseConnectionOwner tsdb, String dbOfficeId)
	{
		super(tsdb, "CwmsIntervalDAO");
		this.dbOfficeId = dbOfficeId;
	}

	@Override
	public void loadAllIntervals() throws DbIoException
	{
		// TODO Read the intervals from the CWMS_V_INTERVAL (name?) view
		// Convert the minute count into calendar constants.
		
		// Temporary solution: add the known CWMS intervals as constants:
		IntervalList dbIntervals = IntervalList.instance();
		long id = 1L;
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_irregular, 0, 0));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_cwms_zero, 0, 0));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_other, Calendar.MINUTE, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_5min, Calendar.MINUTE, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_6min, Calendar.MINUTE, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_10min, Calendar.MINUTE, 10));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_15min, Calendar.MINUTE, 15));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_20min, Calendar.MINUTE, 20));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_30min, Calendar.MINUTE, 30));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_minute, Calendar.MINUTE, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_minute_nc, Calendar.MINUTE, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_minutes, Calendar.MINUTE, 2));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_minutes_nc, Calendar.MINUTE, 2));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_minutes, Calendar.MINUTE, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_minutes_nc, Calendar.MINUTE, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_minutes, Calendar.MINUTE, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_minutes_nc, Calendar.MINUTE, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_minutes, Calendar.MINUTE, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_minutes_nc, Calendar.MINUTE, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_minutes, Calendar.MINUTE, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_minutes_nc, Calendar.MINUTE, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_ten_minutes, Calendar.MINUTE, 10));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_ten_minutes_nc, Calendar.MINUTE, 10));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_minutes, Calendar.MINUTE, 12));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_minutes_nc, Calendar.MINUTE, 12));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_fifteen_minutes, Calendar.MINUTE, 15));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_fifteen_minutes_nc, Calendar.MINUTE, 15));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twenty_minutes, Calendar.MINUTE, 20));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twenty_minutes_nc, Calendar.MINUTE, 20));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_thirty_minutes, Calendar.MINUTE, 30));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_thirty_minutes_nc, Calendar.MINUTE, 30));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_hour, Calendar.HOUR_OF_DAY, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_hour_nc, Calendar.HOUR_OF_DAY, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_hours, Calendar.HOUR_OF_DAY, 2));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_hours_nc, Calendar.HOUR_OF_DAY, 2));

		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_hours, Calendar.HOUR_OF_DAY, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_hours_dst, Calendar.HOUR_OF_DAY, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_hours, Calendar.HOUR_OF_DAY, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_hours_dst, Calendar.HOUR_OF_DAY, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_hours, Calendar.HOUR_OF_DAY, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_hours_dst, Calendar.HOUR_OF_DAY, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_eight_hours, Calendar.HOUR_OF_DAY, 8));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_eight_hours_dst, Calendar.HOUR_OF_DAY, 8));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_hours, Calendar.HOUR_OF_DAY, 12));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_twelve_hours_dst, Calendar.HOUR_OF_DAY, 12));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_day, Calendar.DAY_OF_MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_day_dst, Calendar.DAY_OF_MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_days, Calendar.DAY_OF_MONTH, 2));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_two_days_dst, Calendar.DAY_OF_MONTH, 2));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_days, Calendar.DAY_OF_MONTH, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_three_days_dst, Calendar.DAY_OF_MONTH, 3));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_days, Calendar.DAY_OF_MONTH, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_four_days_dst, Calendar.DAY_OF_MONTH, 4));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_days, Calendar.DAY_OF_MONTH, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_five_days_dst, Calendar.DAY_OF_MONTH, 5));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_days, Calendar.DAY_OF_MONTH, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_six_days_dst, Calendar.DAY_OF_MONTH, 6));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_week, Calendar.WEEK_OF_YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_week_dst, Calendar.WEEK_OF_YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_month, Calendar.MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_month_dst, Calendar.MONTH, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_year, Calendar.YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_year_dst, Calendar.YEAR, 1));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_decade, Calendar.YEAR, 10));
		dbIntervals.add(new Interval(DbKey.createDbKey(id++), IntervalCodes.int_one_decade_dst, Calendar.YEAR, 10));
	}

	@Override
	public void writeInterval(Interval intv) throws DbIoException
	{
//		warning("Cannot write Intervals to CWMS");
	}

	@Override
	public String[] getValidIntervalCodes()
	{
		return validIntervalCodes;
	}

	@Override
	public String[] getValidDurationCodes()
	{
		return validDurationCodes;
	}

}
