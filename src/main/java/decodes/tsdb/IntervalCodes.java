/*
*  $Id: IntervalCodes.java,v 1.3 2020/01/31 19:40:29 mmaloney Exp $
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb;

import ilex.util.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import decodes.db.Constants;
import decodes.db.IntervalList;
import decodes.sql.DbKey;

import opendcs.opentsdb.Interval;

/**
Defines the interval codes used in HDB and NWIS.
*/
public class IntervalCodes
{
	/** Instanteous data */
	public static final String int_instant = "instant";

	/** Unspecified interval */
	public static final String int_other = "other";

	/** Hourly data */
	public static final String int_hour = "hour";

	/** Daily data */
	public static final String int_day = "day";

	/** Monthly data */
	public static final String int_month = "month";

	/** Yearly data */
	public static final String int_year = "year";

	/** 10-day */
	public static final String int_10day = "10day";
	/** Water-Year data */
	public static final String int_wy = "wy";

	/** Unit data (NWIS Synonym for int_instant) */
	public static final String int_unit = "unit";

	/** 5 minute data */
	public static final String int_5min = "5min";
	/** 6 minute data */
	public static final String int_6min = "6min";
	/** 10 minute data */
	public static final String int_10min = "10min";
	/** 15 minute data */
	public static final String int_15min = "15min";
	/** 20 minute data */
	public static final String int_20min = "20min";
	/** 30 minute data */
	public static final String int_30min = "30min";
	/** Irregular data */
	public static final String int_irregular = "irregular";

	/* the CWMS intervals  */
	/** Irregular data */
	public static final String int_cwms_irregular = "irregular";
	/** Zero == Irrigular */
	public static final String int_cwms_zero = "0";
	
	// For each of the CWMS codes there is a _nc version that starts with
	// a tilde. "nc" = no check. This turns off interval checking.
	
	/** 1 minute data */
	public static final String int_one_minute = "1Minute";
	public static final String int_one_minute_nc = "~1Minute";
	/** 2 minute data */
	public static final String int_two_minutes = "2Minutes";
	public static final String int_two_minutes_nc = "~2Minutes";
	/** 3 minute data */
	public static final String int_three_minutes = "3Minutes";
	public static final String int_three_minutes_nc = "~3Minutes";
	/** 4 minute data */
	public static final String int_four_minutes = "4Minutes";
	public static final String int_four_minutes_nc = "~4Minutes";
	/** 5 minute data */
	public static final String int_five_minutes = "5Minutes";
	public static final String int_five_minutes_nc = "~5Minutes";
	/** 6 minute data */
	public static final String int_six_minutes = "6Minutes";
	public static final String int_six_minutes_nc = "~6Minutes";
	/** 10 minute data */
	public static final String int_ten_minutes = "10Minutes";
	public static final String int_ten_minutes_nc = "~10Minutes";
	/** 12 minute data */
	public static final String int_twelve_minutes = "12Minutes";
	public static final String int_twelve_minutes_nc = "~12Minutes";
	/** 15 minute data */
	public static final String int_fifteen_minutes = "15Minutes";
	public static final String int_fifteen_minutes_nc = "~15Minutes";
	/** 20 minute data */
	public static final String int_twenty_minutes = "20Minutes";
	public static final String int_twenty_minutes_nc = "~20Minutes";
	/** 30 minute data */
	public static final String int_thirty_minutes = "30Minutes";
	public static final String int_thirty_minutes_nc = "~30Minutes";
	/** 1 hour    data */
	public static final String int_one_hour = "1Hour";
	public static final String int_one_hour_nc = "~1Hour";
	/** 2 hour    data */
	public static final String int_two_hours_nc = "~2Hours";
	public static final String int_two_hours = "2Hours";

	/** 3 hour    data */
	public static final String int_three_hours = "3Hours";
	/** 4 hour    data */
	public static final String int_four_hours = "4Hours";
	/** 6 hour    data */
	public static final String int_six_hours = "6Hours";
	/** 8 hour    data */
	public static final String int_eight_hours = "8Hours";
	/** 12 hour    data */
	public static final String int_twelve_hours = "12Hours";
	/** 1 Day     data */
	public static final String int_one_day = "1Day";
	public static final String int_two_days = "2Days";
	public static final String int_three_days = "3Days";
	public static final String int_four_days = "4Days";
	public static final String int_five_days = "5Days";
	public static final String int_six_days = "6Days";
	/** 1 week  data */
	public static final String int_one_week = "1Week";
	/** 1 month  data */
	public static final String int_one_month = "1Month";
	/** 1 year  data */
	public static final String int_one_year = "1Year";
	/** 1 decade  data */
	public static final String int_one_decade = "1Decade";

	/** The following are used for CWMS intervals that honor DST. */
	public static final String int_three_hours_dst = "~3Hours";
	public static final String int_four_hours_dst = "~4Hours";
	public static final String int_six_hours_dst = "~6Hours";
	public static final String int_eight_hours_dst = "~8Hours";
	public static final String int_twelve_hours_dst = "~12Hours";
	public static final String int_one_day_dst = "~1Day";
	public static final String int_two_days_dst = "~2Days";
	public static final String int_three_days_dst = "~3Days";
	public static final String int_four_days_dst = "~4Days";
	public static final String int_five_days_dst = "~5Days";
	public static final String int_six_days_dst = "~6Days";
	public static final String int_one_week_dst = "~1Week";
	public static final String int_one_month_dst = "~1Month";
	public static final String int_one_year_dst = "~1Year";
	public static final String int_one_decade_dst = "~1Decade";

	/** The singleton IntervalList instance holds intervals defined in the database */
	private static IntervalList dbIntervals = IntervalList.instance();
	
	/**
	 * This array holds built-in intervals for the calendar primitives. These cannot be changed.
	 */
	private static ArrayList<Interval> builtInIntervals = new ArrayList<Interval>();
	static
	{
		builtInIntervals.add(new Interval(Constants.undefinedId, "0", Calendar.MINUTE, 0));
		builtInIntervals.add(new Interval(Constants.undefinedId, "minute", Calendar.MINUTE, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "hour", Calendar.HOUR_OF_DAY, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "h", Calendar.HOUR_OF_DAY, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "day", Calendar.DAY_OF_MONTH, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "d", Calendar.DAY_OF_MONTH, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "week", Calendar.WEEK_OF_YEAR, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "month", Calendar.MONTH, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "m", Calendar.MONTH, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "y", Calendar.YEAR, 1));
		builtInIntervals.add(new Interval(Constants.undefinedId, "year", Calendar.YEAR, 1));
	}

	
	/**
	 * Get the number of seconds representing the interval.
	 * @param interval the interval
	 * @return number of seconds or 0 if interval not recognized.
	 */
	public static int getIntervalSeconds(String interval)
	{
		Interval intv = getInterval(interval);
		if (intv == null || intv.getCalMultiplier() == 0)
			return 0;
		switch(intv.getCalConstant())
		{
		case Calendar.MINUTE:
			return intv.getCalMultiplier() * 60;
		case Calendar.HOUR_OF_DAY:
			return intv.getCalMultiplier() * 3600;
		case Calendar.DAY_OF_MONTH:
			return intv.getCalMultiplier() * 3600 * 24;
		case Calendar.MONTH:
			return intv.getCalMultiplier() * 3600 * 24 * 31;
		case Calendar.YEAR:
			return intv.getCalMultiplier() * 3600 * 24 * 365;
		default:
			return 0;
		}
	}
	
	/**
	 * Passed a string of one of the following forms:
	 *    interval
	 *    interval * count
	 * Where 'interval' is a valid interval string in the underlying database
	 * and count is an integer > 0.
	 * Return IntervalIncrement containing Calendar constant and increment
	 * corresponding to the interval. 
	 * @param interval the interval name defined herein.
	 * @return IntervalIncrement containing Calendar constant and increment.
	 */
	public static IntervalIncrement getIntervalCalIncr(String interval)
	{
		if (interval == null)
			return null;
		
		// Allow strings like "hour*16"
		int starIdx = interval.indexOf('*');
		int count = 1;
		if (starIdx != -1)
		{
			try { count = Integer.parseInt(interval.substring(starIdx+1)); }
			catch(Exception ex)
			{
				Logger.instance().warning("Invalid interval '" + interval + "'");
				count = 1;
			}
			interval = interval.substring(0, starIdx);
		}
		
		Interval intv = getInterval(interval);
		if (intv == null)
			return null;
		return new IntervalIncrement(intv.getCalConstant(), intv.getCalMultiplier()*count);
	}
	
	/**
	 * Get String to use in a param delta indicating this interval.
	 * This is used to resolve automatic deltas where the delta-interval
	 * is inferred from the param interval.
	 * @param interval the interval
	 * @return normalized delta spec
	 */
	public static String getDeltaSpec(String interval)
	{
		if (interval.equalsIgnoreCase(int_irregular))
			return null;
		if (interval.equalsIgnoreCase("h")
		 || interval.equalsIgnoreCase("d")
		 || interval.equalsIgnoreCase("m")
		 || interval.equalsIgnoreCase("y"))
			return interval;
		if (interval.equalsIgnoreCase(int_hour))
			return "h";
		if (interval.equalsIgnoreCase(int_day))
			return "d";
		if (interval.equalsIgnoreCase(int_month))
			return "m";
		if (interval.equalsIgnoreCase(int_year))
			return "y";
		if (interval.equalsIgnoreCase(int_wy))
			return "y";
		if (interval.equalsIgnoreCase(int_unit))
			return null;
		if (interval.equalsIgnoreCase(int_instant))
			return null;
		if (interval.equalsIgnoreCase(int_other))
			return null;
		if (interval.equalsIgnoreCase(int_5min))
			return "05";
		if (interval.equalsIgnoreCase(int_6min))
			return "06";
		if (interval.equalsIgnoreCase(int_10min))
			return "10";
		if (interval.equalsIgnoreCase(int_15min))
			return "15";
		if (interval.equalsIgnoreCase(int_20min))
			return "20";
		if (interval.equalsIgnoreCase(int_30min))
			return "30";
		if (interval.equalsIgnoreCase(int_cwms_irregular)
		 || interval.equalsIgnoreCase(int_cwms_zero))
			return null;
		if (interval.equalsIgnoreCase(int_one_minute))
			return "01";
		if (interval.equalsIgnoreCase(int_two_minutes))
			return "02";
		if (interval.equalsIgnoreCase(int_three_minutes))
			return "03";
		if (interval.equalsIgnoreCase(int_four_minutes))
			return "04";
		if (interval.equalsIgnoreCase(int_five_minutes))
			return "05";
		if (interval.equalsIgnoreCase(int_six_minutes))
			return "06";
		if (interval.equalsIgnoreCase(int_ten_minutes))
			return "10";
		if (interval.equalsIgnoreCase(int_twelve_minutes))
			return "12";
		if (interval.equalsIgnoreCase(int_fifteen_minutes))
			return "15";
		if (interval.equalsIgnoreCase(int_twenty_minutes))
			return "20";
		if (interval.equalsIgnoreCase(int_thirty_minutes))
			return "30";
		if (interval.equalsIgnoreCase(int_one_hour))
			return "h";
		if (interval.equalsIgnoreCase(int_two_hours))
			return null;
		if (interval.equalsIgnoreCase(int_three_hours))
			return null;
		if (interval.equalsIgnoreCase(int_four_hours))
			return null;
		if (interval.equalsIgnoreCase(int_six_hours))
			return null;
		if (interval.equalsIgnoreCase(int_eight_hours))
			return null;
		if (interval.equalsIgnoreCase(int_twelve_hours))
			return null;
		if (interval.equalsIgnoreCase(int_one_day)
		 || interval.equalsIgnoreCase(int_one_day_dst))
			return "d";
		if (interval.equalsIgnoreCase(int_one_week))
			return null;
		if (interval.equalsIgnoreCase(int_one_month)
		 || interval.equalsIgnoreCase(int_one_month_dst))
			return "m";
		if (interval.equalsIgnoreCase(int_one_year)
		 || interval.equalsIgnoreCase(int_one_year_dst))
			return "y";
		if (interval.equalsIgnoreCase(int_one_decade)
		 || interval.equalsIgnoreCase(int_one_decade_dst))
			return null;
		return interval;
	}

	/**
	 * Given a unique name, return the Interval object.
	 * @param name the unique name
	 * @return the Interval object or null if not recognized.
	 */
	public static Interval getInterval(String name)
	{
		Interval ret = dbIntervals.getByName(name);
		if (ret != null)
			return ret;
		for(Interval eintv : builtInIntervals)
			if (eintv.getName().equalsIgnoreCase(name))
				return eintv;
		return null;
	}
	
	public static String getCalConstName(int calConst)
	{
		if (calConst == Calendar.MINUTE)
			return "minute";
		else if (calConst == Calendar.HOUR_OF_DAY)
			return "hour";
		else if (calConst == Calendar.DAY_OF_MONTH)
			return "day";
		else if (calConst == Calendar.WEEK_OF_YEAR)
			return "week";
		else if (calConst == Calendar.MONTH)
			return "month";
		else if (calConst == Calendar.YEAR)
			return "year";
		else return "unknown";
	}
}
