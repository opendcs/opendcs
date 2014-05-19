/*
*  $Id$
*/
package decodes.decoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;

/**
RecordedTimeStamp is used in the decoding process to set date and time
components and to retrieve the time values for stamping individual
sensor samples.
*/
public class RecordedTimeStamp
{
	/** The calendar used internally to set and convert times. */
	private GregorianCalendar cal;

	/** Symbolic constant meaning that recorded time is empty. */
	public static final int NOTHING = 0;

	/** Symbolic constant meaning time-of-day is set but not date or year. */
	public static final int TIME_OF_DAY = 1;

	/**
	  Symbolic constant meaning time-of-day and day-of-year are set but
	  the year is still unknown.
	*/
	public static final int TIME_OF_YEAR = 2;

	/** Symbolic constant meaning that recorded time is completely specified. */
	public static final int COMPLETE = 3;

	/**
	  One of the symbolic constants, NOTHING, TIME_OF_DAY, TIME_OF_YEAR,
	  or COMPLETE; indicating how much information this time-stamp currently
	  contains.
	*/
	private int status;

	/*
	  The following are for internal tracking of individual date fields:
	  Transition from TIME_OF_DAY to TIME_OF_YEAR when 
		(haveMonth && haveMDay) | haveYDay;
	  Transition from COMPLETE when 
		(haveYear && ((haveMonth && haveMDay) | haveYDay);
	*/
	private boolean haveYear;
	private boolean haveMonth;
	private boolean haveMDay;
	private boolean haveYDay;
	private int currentDOY = -1;
	private int currentMonth = -1;
	private int currentDOM = -1;

	/**
	  We don't use the AM/PM features of Calendar. Instead, keep a PM
	  flag internally and translate hour numbers. This allows the use
	  of only the HOUR_OF_DAY field for all hours parsed out of the data.
	  This is necessary because we don't know what order PM will be set in.
	  It may be set after we think we're being given a 24-hour clock value.
	*/
	private boolean pm;
	private boolean pmWasSet = false;

	private boolean tzIsManual;

	boolean dayJustSet;
	
	private SimpleDateFormat logsdf = 
		new SimpleDateFormat("yyyy MMM/dd HH:mm:ss z");

	/**
	  Constructor.
	  @param  tzName name of time zone
	*/
	public RecordedTimeStamp(String tzName)
	{
		this();
		setTimeZoneName(tzName);
	}

	/** default constructor */
	public RecordedTimeStamp()
	{
		cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		tzIsManual = false;
		reset();
	}

	/** Resets internal variables to defaults. */
	public void reset()
	{
		status = NOTHING;
		pm = false;
		cal.clear();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		haveYear = false;
		haveMonth = false;
		haveMDay = false;
		haveYDay = false;
		dayJustSet = false;
		currentDOY = -1;
		currentMonth = -1;
		currentDOM = -1;
	}

	/**
	  Sets TZ from String in one of three forms, Standard, Custom, or USGS
	  Minute Offset with daylight flag.
	  @param tzName the time zone name
	*/
	public void setTimeZoneName(String tzName)
	{
		String tzs = tzName.trim();
		if ( tzs.matches("GMT[+-].*[MAYN]$") ) {
			tzs = tzs.substring(0,tzs.length()-1);
		}
		tzs = tzs.trim();
		TimeZone tz = TimeZone.getTimeZone(tzs);
		cal.setTimeZone(tz);
		logsdf.setTimeZone(tz);
	}
	/**
	  Sets status to complete without changing the internal calendar,
	  by re-assigning the current year.
	*/
	public void setComplete()
	{
		setYear(cal.get(Calendar.YEAR));
		status = COMPLETE;
	}

	/**
	  Sets to complete supplied date.
	  @param d the supplied date
	*/
	public void setComplete(Date d)
	{
		cal.setTime(d);
		setComplete();
	}

	/**
	  Sets the year. Handles 2 or 4 digit values.
	  Values from 0...69 assume 2000. Values from 70...99, assume 1900.
	  @param y the year value, may be 2 or 4 digits
	  @return new status after updating internal calendar.
	*/


	public int setYear(int y)
	{
		int origY = y;
		if (y < 70)
			y += 2000;
		else if (y < 100)
			y += 1900;

		int doy = 0;
		if( haveYDay ) {
			/* If the day-of-year was used, before changing the year,
			 get the current value, which reflects the absolute day 
			 offset of this day.  When the year is changed, this day
			 will be reset to handle any leap-year conversions */
			 doy = cal.get(Calendar.DAY_OF_YEAR);
		}
		cal.set(Calendar.YEAR, y);
		if ( haveYDay ) 
			cal.set(Calendar.DAY_OF_YEAR, doy);
		else if (haveMonth && haveMDay) {
			cal.set(Calendar.DAY_OF_MONTH, currentDOM);
			cal.set(Calendar.MONTH, (currentMonth-1) + Calendar.JANUARY);
		}
		haveYear = true;
		if (haveYDay || (haveMonth && haveMDay))
			status = COMPLETE;
		return status;
	}

	public boolean getHaveYDay() { return haveYDay; }

	public int getYear() { return cal.get(Calendar.YEAR); }

	/**
	  Sets the month number (1=Jan ... 12=Dec).
	  @param month the month number (1=Jan)
	  @return new status after updating internal calendar.
	*/
	public int setMonth(int month)
	{
		currentMonth = month;
		cal.set(Calendar.MONTH, (month-1) + Calendar.JANUARY);
		haveMonth = true;
		if (haveMDay && status < TIME_OF_YEAR)
			status = haveYear ? COMPLETE : TIME_OF_YEAR;

		// NOTE: Don't print calendar value here, it will cause month
		// to be evaluated. Thus if it was 8/31 and we set MN to 9,
		// the print causes date (9/31) to be evaluated as 10/1 which
		// messes up subsequent time stamps.

		return status;
	}

	/**
	  Sets the day of month (1...31)
	  @param dom the day of the month (1...31)
	  @return new status after updating internal calendar.
	*/
	public int setDayOfMonth(int dom)
	{
		currentDOM = dom;
		cal.set(Calendar.DAY_OF_MONTH, dom);
		haveMDay = true;
		if (haveMonth && status < TIME_OF_YEAR)
			status = haveYear ? COMPLETE : TIME_OF_YEAR;
		return status;
	}

	/**
	  Sets the day-of-year (1=Jan 1 ... 365=Dec 31 (on non-leap-years).
	  @param doy the day of the year (1=Jan1)
	  @return new status after updating internal calendar.
	*/
	public int setDayOfYear(int doy)
	{
		currentDOY = doy;
		haveYDay = true;
		if ( !haveYear && doy == 366 ) {
			/*  If the year has not been set, it has defaulted to the EPOCH
					year ( usually 1970 ) which is not a leap year.  So a doy of
					366, will be normalized to day 1 of the next year which is not
					good.  So, force the year to be a leap year by incrementing the
					year until the year is a leap year so that day 366 exists. It 
					is assumed that the correct leap year will eventually be set.
			*/
			while ( !cal.isLeapYear(cal.get(Calendar.YEAR) ) )
				cal.add(Calendar.YEAR,1);
		}
		cal.set(Calendar.DAY_OF_YEAR, doy);
		if (status < TIME_OF_YEAR)
			status = haveYear ? COMPLETE : TIME_OF_YEAR;
			
		return status;
	}
	public void incrementDay() 
	{
		if (haveYDay)
			cal.add(Calendar.DAY_OF_YEAR, 1);
		else
			cal.add(Calendar.DAY_OF_MONTH, 1);
	}
	public void decrementDay() 
	{
		if (haveYDay)
			cal.add(Calendar.DAY_OF_YEAR, -1);
		else
			cal.add(Calendar.DAY_OF_MONTH, -1);
	}
	public void incrementYear() 
	{
		cal.add(Calendar.YEAR, 1);
    }
    public void decrementYear() {
        cal.add(Calendar.YEAR, -1);
    }
	/**
	  Sets the hour, if PM was set previously, assume 12-hour clock.
	  Hour 24 bumps the clock to midnight on the next day.
	  @param hr the hour
	  @return new status after updating internal calendar.
	*/
	public int setHour(int hr)
	{
		int origHr = hr;
		if (pm && hr < 12)
			hr += 12;
		else if (pmWasSet && !pm && hr == 12)
			hr = 0;
		if ( hr == 24 ) {
			int doy = cal.get(Calendar.DAY_OF_YEAR);
			doy++;
			int oldStatus = status;
			setDayOfYear(doy);
			status = oldStatus;
			hr = 0;
		}
		cal.set(Calendar.HOUR_OF_DAY, hr);
		if (status == NOTHING)
			status = TIME_OF_DAY;
		return status;
	}

	/**
	  Sets the minute.
	  @param min the minute
	  @return new status after updating internal calendar.
	*/
	public int setMinute(int min)
	{
		cal.set(Calendar.MINUTE, min);
		return status;
	}
	
	public int getMinute() { return cal.get(Calendar.MINUTE); }

	/**
	  Sets the second.
	  @param sec the second
	  @return new status after updating internal calendar.
	*/
	public int setSecond(int sec)
	{
		cal.set(Calendar.SECOND, sec);
		return status;
	}

	/**
	  Changes hour values to PM or AM, may change previously set hour value.
	  Do not call this method at all if 24-hour clock is used.
	  @param newpm true if this is PM.
	  @return new status after updating internal calendar.
	*/
	public int setPM(boolean newpm)
	{
		int hr = cal.get(Calendar.HOUR_OF_DAY);
		if (newpm)
		{
			if (hr < 12)
				cal.set(Calendar.HOUR_OF_DAY, hr+12);
		}
		else// if (this.pm) // Was PM, now switching to AM.
		{
			if (hr >= 12)
				cal.set(Calendar.HOUR_OF_DAY, hr-12);
		}
		this.pm = newpm;
		pmWasSet = true;
		return status;
	}

	/**
	  @return current value of time, Depending on the current status this is
	  either a time-of-day, time-of-year, or a complete value.
	*/
	public long getMsec()
	{
		// Note: Calendar.getTimeInMillis() is protected in JRE 1.3.1
		return cal.getTime().getTime();
	}

	/**
	  @return one of the symbolic constants, NOTHING, TIME_OF_DAY, TIME_OF_YEAR,
	  or COMPLETE; indicating how much information this time-stamp currently
	  contains.
	*/
	public int getStatus()
	{
		return status;
	}

	/**
	  Call this method to adjust previously recorded partial times after
	  completing the time-stamp.
	  @return the offset to the start of the year in this time-stamp.
	*/
	public long getYearMsecOffset()
	{
		Calendar tcal = (Calendar)cal.clone();
		tcal.set(Calendar.DAY_OF_YEAR, 1);
		tcal.set(Calendar.HOUR_OF_DAY, 0);
		tcal.set(Calendar.MINUTE, 0);
		tcal.set(Calendar.SECOND, 0);
		// Note: Calendar.getTimeInMillis() is protected in JRE 1.3.1
		return tcal.getTime().getTime();
	}

	/**
	  Call this method to adjust previously recorded partial times after
	  completing the time-stamp.
	  @return the offset to the start of the day in the current day/year 
	  in this time-stamp.
	*/
	public long getDayMsecOffset()
	{
		Calendar tcal = (Calendar)cal.clone();
		tcal.set(Calendar.HOUR_OF_DAY, 0);
		tcal.set(Calendar.MINUTE, 0);
		tcal.set(Calendar.SECOND, 0);
		/*
		  If year not yet set, return true time-off year, disregarding timezone.
		  Example 1AM on Jan 1 should yield 3600000, regardless of timezone. 
		  If year IS set, then I'm completing the time stamp. The msec offset
		  must then be a true UTC time value.
		*/
		// Note: Calendar.getTimeInMillis() is protected in JRE 1.3.1
		long ret = tcal.getTime().getTime();  // Force calendar to re-evaluate.
		if (!tcal.isSet(Calendar.YEAR))
			return (tcal.get(Calendar.DAY_OF_YEAR)-1) * 24L * 3600L * 1000L;
		return ret;
	}

	/** @return the time zone name. */
	public String getTimeZoneName()
	{
		return cal.getTimeZone().getDisplayName();
	}

	/** @return the time zone object. */
	public TimeZone getTimeZone()
	{
		return cal.getTimeZone();
	}

	/** Returns string representation of the internal calendar object. */
	public String toString()
	{
		return cal.toString();
	}

	/** Return true if TZ was set manually */
	public boolean isTzManual() { return tzIsManual; }

	/** Return internal calandar object */
	public GregorianCalendar getCalendar() { return cal; }

	public int getDayOfYear()
	{
		return cal.get(Calendar.DAY_OF_YEAR);
	}
	public Date getTime() {
		return cal.getTime();
	}
}
