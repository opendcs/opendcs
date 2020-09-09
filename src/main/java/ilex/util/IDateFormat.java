/*
*  $Id$
*/

package ilex.util;

import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.StringTokenizer;


/**
The IDateFormat class contains several useful functions for parsing and
printing dates in various formats. The class handles string dates in
the following formats:
<pre>
[[[CC]YY] DDD] HH:MM[:SS] 
If seconds omitted, assume 0.
If Julian Days omitted, assume today
If YY omitted, assume current year
If CC omitted, assume 1900 if YY >= 70, 2000 if YY < 70
now [+- N units [N units] ... ]
This specifies a time relative to the current time. For
examples "now - 1 day", "now + 1 hour 10 minutes 3 seconds".
</pre>

*/
public class IDateFormat
{
	/** Set to true to have seconds always included in output format. */
	public static boolean alwaysIncludeSeconds = false;

	/**
	* Return true if the date string is of the form "now +- inc units ..."
	* @param date the date string
	* @return true if this is relative to "now"
	*/
	public static boolean isRelative( String date )
	{
		date = date.trim(); // Remove leading & trailing white space
		return date.substring(0, 3).equalsIgnoreCase("now");
	}
		
	/**
	* Parse a date string in one of the allowable formats. The
	* Date is returned. If the string specified a time relative to
	* now, the second argument 'isRelative' will be set.
	* @param date the date string
	* @return Date object
	* @throws IllegalArgumentException if parse error
	*/
	public static Date parse( String date ) throws IllegalArgumentException
	{
		date = date.trim(); // Remove leading & trailing white space
		if (date.length() == 0)
			return null;
		
		if (Character.isDigit(date.charAt(0)))
			return parseJulianDate(date);
		else if (!date.substring(0, 3).equalsIgnoreCase("now"))
			throw new IllegalArgumentException("Bad date format '"+date+"'");
			
			
		StringTokenizer st = new StringTokenizer(date," \t+-", true);
		Date ret = new Date();  // Initialize return value to current time.
		int sign = 1;
		int offset = 0;
		int inc = 0, units=0;
		
		for(int i = 0; st.hasMoreTokens(); )
		{
			String tok = st.nextToken();

			// Skip white space delimiters.
			if (tok.charAt(0) == ' ' || tok.charAt(0) == '\t')
				continue;
				
			if (i == 0)      // First token: "now"
			{
				if (!tok.equalsIgnoreCase("now"))
					throw new IllegalArgumentException("Bad date format '"+date+"'");
			}
			else if (i == 1) // Second token should be + or -
			{
				if (tok.equals("+"))
					sign = 1;
				else if (tok.equals("-"))
					sign = -1;
				else 
					throw new IllegalArgumentException(
						"Bad date format (no sign) '"+date+"'");
			}
			else if (Character.isDigit(tok.charAt(0)))
			{
				int x=1;
				for(; x < tok.length() && Character.isDigit(tok.charAt(x)); x++);
				if (x < tok.length())
				{
					// The user combined inc with units, like "1hour"
					inc = Integer.parseInt(tok.substring(0,x));
					units = getTimeUnitValue(tok.substring(x));
					if (units != 0)
						offset += (inc * units);
				}
				else // increment separate token from units, e.g. "3 hours"
					inc = Integer.parseInt(tok); 
			}
			else if ((units = getTimeUnitValue(tok)) != 0)
				offset += (inc * units);
			else
				throw new IllegalArgumentException("Bad date format '"+date+"'");
			
			++i;
		}
		
		ret.setTime( ((ret.getTime()/1000) + (offset*sign)) * 1000 );
		return ret;
	}
	
	/**
	* Parse a string in one of the the following forms
	*  YYYY/DDD/HH:MM:SS   (delim between DDD and HH may be space, tab, or slash
	*  YYYY/DDD/HH:MM      (seconds set to 0)
	*  DDD/HH:MM:SS        (assume current year)
	*  DDD/HH:MM
	*  HH:MM:SS            (assume current day)
	*  HH:MM
	* @param date the date string
	* @return the Date object
	* @throws IllegalArgumentException on parse error
	*/
	public static Date parseJulianDate( String date ) throws IllegalArgumentException
	{
		date = date.trim();
		
		// Separate date into number tokens. Keep track of position of 1st colon.
		String delims = " \t:/";
		StringTokenizer st = new StringTokenizer(date, delims, true);
		int num[] = new int[5];
		int n = 0, colon = -1;
		int numcolons = 0;
		while(st.hasMoreTokens() && n < 5)
		{
			String tok = st.nextToken();
			if (Character.isDigit(tok.charAt(0)))
				num[n++] = Integer.parseInt(tok);
			else if (tok.charAt(0) == ':')
			{
				numcolons++;
				if (colon == -1)
					colon = n;
			}
		}
		
		// Use a calendar to build the date object
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);
		
		// Get current values for defaults.
		cal.setTime(new Date());
		int year = cal.get(Calendar.YEAR);          // Default to current year
		int curDOY = cal.get(Calendar.DAY_OF_YEAR); // Default to current day
		cal.clear();

		// Check for legal formats.
		if (colon == 1 && (n == 2 || n == 3))
		{
			// HH:MM[:SS]
			cal.set(Calendar.DAY_OF_YEAR, curDOY);
			cal.set(Calendar.HOUR_OF_DAY, num[0]);
			cal.set(Calendar.MINUTE, num[1]);
			if (n > 2)
				cal.set(Calendar.SECOND, num[2]);
		}
		else if (colon == 2 && n >= 3)
		{
			// DDD HH:MM[:SS] [YYYY]
			cal.set(Calendar.DAY_OF_YEAR, num[0]);
			cal.set(Calendar.HOUR_OF_DAY, num[1]);
			cal.set(Calendar.MINUTE, num[2]);
			if (n == 4 && numcolons == 1)     // DDD/HH:MM YYYY
				year = num[3];
			else if (n == 4 && numcolons > 1) // DDD/HH:MM:SS
				cal.set(Calendar.SECOND, num[3]);
			else if (n == 5)                  // DDD/HH:MM:SS YYYY
			{
				cal.set(Calendar.SECOND, num[3]);
				year = num[4];
			}
		}
		else if (colon == 3)          // YYYY/DDD/HH:MM[:SS]
		{
			year = num[0];
			cal.set(Calendar.DAY_OF_YEAR, num[1]);
			cal.set(Calendar.HOUR_OF_DAY, num[2]);
			cal.set(Calendar.MINUTE, num[3]);
			if (n > 4)
				cal.set(Calendar.SECOND, num[4]);
		}
		else
			throw new IllegalArgumentException("Bad julian date format '"+date+"'");

		// Year may have been entered as 2 or 4 digits. Use window from 
		// 1970...2069
		if (year > 0 && year < 100)
		{
			if (year < 70)
				year += 2000;       // If less than 70, assume 2000
			else if (year < 100)
				year += 1900;       // Else 71...99, assume 1900
		}
		cal.set(Calendar.YEAR, year);

		return cal.getTime();
	}

	/**
	* Convert unix type_t value into string in the format CCYY DDD HH:MM:SS .
	* @param time_t the time_t value
	* @return formatted time string
	*/
	public static String time_t2string( int time_t )
	{
		return IDateFormat.toString(new Date((long)time_t*1000), false);
	}

	/**
	* Convert a java Date object into an string. If the 'relative'
	* argument is set, the string will be of the form
	* now +- [n] [unit] ...
	* @param d the Date object
	* @param relative true if you want a string relative to "now".
	* @return the formatted string.
	*/
	public static String toString( Date d, boolean relative )
	{
		// Seconds since EPOCH (unix time)
		int t = (int)d.getTime() / 1000;
		
		if (t == 0 && !relative) // Null time value?
			return "";

		StringBuffer ret = new StringBuffer();
		if (relative)
		{
			Date now = new Date();
			t -= (int)now.getTime() / 1000;
			
			ret.append("now ");

		    if (t < 0)
		    {
		    	ret.append("- ");
				t = -t;
		    }
		    else if (t > 0)
				ret.append("+");

	    	for(int i=0; t > 0 && i < 7; i++)
			{
				int n = t / UnitValue[i];
				if (n > 0)
			    {
			    	ret.append(" ");
			    	ret.append(n);
			    	ret.append(" ");
			    	ret.append(UnitLabel[i]);
			    	if (n > 1)
			    		ret.append('s');
					t -= (n*UnitValue[i]);
		    	}
			}
		}
		else // absolute time
		{
			TimeZone tz = TimeZone.getTimeZone("GMT");
			Calendar cal = Calendar.getInstance(tz);
			cal.setTime(d);
			ret.append(cal.get(Calendar.YEAR));
			ret.append("/");
			int doy = cal.get(Calendar.DAY_OF_YEAR);
			if (doy < 100)
				ret.append("0");
			if (doy < 10)
				ret.append("0");
			ret.append(doy);
			ret.append(" ");
			int x = cal.get(Calendar.HOUR_OF_DAY);
			if (x < 10)
				ret.append('0');
			ret.append(x);
			ret.append(":");
			x = cal.get(Calendar.MINUTE);
			if (x < 10)
				ret.append('0');
			ret.append(x);
			ret.append(":");
			x = cal.get(Calendar.SECOND);
			if (x < 10)
				ret.append('0');
			ret.append(x);
		}
			
		return ret.toString();
	}
	
	// Convenient labels for time units:
	public static final int SecondsPerDay = (60*60*24);
	public static final int SecondsPerWeek = SecondsPerDay*7;
	public static final int SecondsPerMonth = SecondsPerDay*30;
	public static final int SecondsPerYear = SecondsPerDay*365;
	
	public static String[] UnitLabel = new String[]{"year","month","week","day","hour","min","sec"};
	public static int[] UnitValue = new int[]{SecondsPerYear,SecondsPerMonth,SecondsPerWeek,SecondsPerDay,3600,60,1};
	
	/**
	* Looks up time units & returns corresponding number of seconds.
	* @param label time unit like "year", "day", etc.
	* @return number of seconds in one of those units
	*/
	public static int getTimeUnitValue( String label )
	{
		for(int i = 0; i < UnitLabel.length; ++i)
			if (label.startsWith(UnitLabel[i]))
				return UnitValue[i];
		// Fell through - return 0.
		return 0;
	}

	/**
	* Passed a string of one of the following forms:
	* <ul>
	* <li>HHMM (length 4)</li>
	* <li>HHMM (length 4)</li>
	* <li>HH:MM (length 5)</li>
	* <li>HHMMSS (length 6)</li>
	* <li>HH:MM:SS <length 8)</li>
	* </ul>
	* Returns the second of the day as an integer.
	* @param str string in one of the proscribed formats
	* @return  second of day (midnight == 0)
	* @throws IllegalArgumentException if string is not in an acceptable format.
	*/
	public static int getSecondOfDay( String str ) 
		throws IllegalArgumentException
	{
		int h, m, s;
		s = 0;
		byte[] b = str.getBytes();
		byte z = (byte)'0';

		int ms, ss;  // minute-start, seconds-start

		switch(b.length)
		{
		case 4: ms = 2; ss = -1; break;
		case 5: ms = 3; ss = -1; break;
		case 6: ms = 2; ss = 4; break;
		case 8: ms = 3; ss = 6; break;
		default:
			throw new IllegalArgumentException(
				"Wrong length: Time-of-day format requires HH:MM or HH:MM:SS");
		}

		h = (b[0] - z) * 10 + (b[1] - z);

		m = (b[ms] - z) * 10 + (b[ms+1] - z);

		if (ss != -1)
			s = (b[ss] - z) * 10 + (b[ss+1] - z);

		if (m == 60)
		{
			h++;
			m = 0;
		}

		if (h < 0 || h > 24
		 || m < 0 || m > 59
		 || s < 0 || s > 59)
			throw new IllegalArgumentException(
				"Bad format: Time-of-day format requires HH:MM or HH:MM:SS");

		return h * 60 * 60 + m * 60 + s;
	}

	/**
	* Passed a second of the day, returns string containing the value in
	* one of the formats: HHMM, HH:MM, HHMMSS, HH:MM:SS.
	* If 'useColons' is true, colon separators will be used.
	* The seconds field will only be printed if it is non-zero.
	* @param sod second of day
	* @param useColons true if you want colon separaters in the output string.
	* @return formatted time 
	* @throws IllegalArgumentException if the passed integer is out of range.
	*/
	public static String printSecondOfDay( long sod, boolean useColons ) 
	{
		/* MJM - allow values >= 24 hours. Necessary for EDL recording intervals. */
		// if (sod < 0 || sod >= 24*60*60)
		//	throw new IllegalArgumentException(
		//		"time-of-day out of range (" + sod + ")");
		sod %= (24*3600);
		int h = (int)sod / (60*60);
		sod -= (h*60*60);
		int m = (int)sod / 60;
		int s = (int)sod % 60;

		StringBuffer sb = new StringBuffer();
		if (h < 10) sb.append('0');
		sb.append(h);
		if (useColons)
			sb.append(':');

		if (m < 10) sb.append('0');
		sb.append(m);
		if (s != 0 || alwaysIncludeSeconds)
		{
			if (useColons)
				sb.append(':');
			if (s < 10) sb.append('0');
			sb.append(s);
		}
		return sb.toString();
	}
	
	/**
	* Test main.
	* @param args the args.
	*/
	public static void main( String[] args )
	{
//		int sod = getSecondOfDay(args[0]);
//		System.out.println("Second of day = " + sod + ", " +
//			printSecondOfDay(sod, true) + ", " +
//			printSecondOfDay(sod, false));
		TimeZone tz = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(tz);
		
		for(int i = 0; i < args.length; ++i)
		{
			Date d = IDateFormat.parse(args[i]);
			System.out.println(args[i]+": time_t = " + d.getTime()/1000);
			cal.setTime(d);
			System.out.println("From calendar: " + cal.get(Calendar.YEAR) + " "
				+ cal.get(Calendar.DAY_OF_YEAR) + " " + cal.get(Calendar.HOUR_OF_DAY)
				+ ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));
			System.out.println("From IDateFormat: " + IDateFormat.toString(d, false));
			System.out.println("From IDateFormat(r): " + IDateFormat.toString(d, true));
		}		
	}
}
