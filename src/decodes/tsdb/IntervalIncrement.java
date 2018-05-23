/**
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2017/03/03 19:15:57  mmaloney
 * toMsec() to handle HOUR_OF_DAY.
 *
 * Revision 1.2  2017/02/09 17:26:42  mmaloney
 * Added toMsec method.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.7  2012/10/31 15:15:58  mmaloney
 * Remove unneeded imports.
 *
 * Revision 1.6  2012/10/31 15:15:24  mmaloney
 * Added Id & Log to file header.
 *
 */
package decodes.tsdb;

import ilex.util.Logger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expresses an Interval as a Calandar Constant and a count.
 * For example, the CWMS interval "15Minutes" is expressed as
 * calConstant = MINUTE
 * count = 15
 * This is used for the Calendar math needed to traverse intervals 
 * and aggregate periods.
 */
public class IntervalIncrement
{
	private int calConstant = Calendar.HOUR_OF_DAY;
	private int count = 1;
	private static final IntervalIncrement ONE_HOUR =
		new IntervalIncrement(Calendar.HOUR_OF_DAY, 1);
	
	// digits followed by a word, optionally separated by spaces
	private static final Pattern iipattern =
		Pattern.compile("(\\d+)\\s*([a-zA-Z]+)[\\s,]*");

	public IntervalIncrement(int calConstant, int count)
	{
		this.calConstant = calConstant;
		this.count = count;
	}
	
	/**
	 * Parse strings like "5 hour", "5h", "15m", "1 month".
	 * String must be a digit, optionally followed by whitespace, followed
	 * by a word representing the interval.
	 * @param s the string.
	 * @return corresponding IntervalIncrement or ONE_HOUR if parse error.
	 */
	public static IntervalIncrement parse(String s)
	{
		try
		{
			IntervalIncrement [] iia = parseMult(s);
			return iia[0];
		}
		catch(NoSuchObjectException ex)
		{
			Logger.instance().warning("Invalid Interval: " + ex.getMessage()
				+ " -- using 1 hour");
			return ONE_HOUR;
		}
	}
	
	/**
	 * Given a string like "h" "hour" "yr" "day" "min", return the
	 * corresponding Calendar constant
	 * @param s the string
	 * @return the Calendar constant or -1 if no match
	 */
	public static int str2CalConst(String x)
	{
		if (x == null)
			return -1;
		x = x.trim();
		if (x.length() == 0)
			return -1;
		x = x.toLowerCase();
		if (x.startsWith("s"))
			return Calendar.SECOND;
		else if (x.startsWith("mi"))
			return Calendar.MINUTE;
		else if (x.startsWith("h"))
			return Calendar.HOUR_OF_DAY;
		else if (x.startsWith("d"))
			return Calendar.DAY_OF_MONTH;
		else if (x.startsWith("w"))
			return Calendar.WEEK_OF_YEAR;
		else if (x.startsWith("mo"))
			return Calendar.MONTH;
		else if (x.startsWith("me"))  // spanish mes
			return Calendar.MONTH;
		else if (x.startsWith("y"))
			return Calendar.YEAR;
		else if (x.startsWith("an"))  // Spanish ano
			return Calendar.YEAR;
		else if (x.startsWith("m"))  // m by itself means minute
			return Calendar.MINUTE;
		else
			return -1;
	}
	
	/**
	 * Parse multiple interval increments on a single string like
	 * "5hours 15min", "1mon 3days 12 hours 10m, 15 s".
	 * White space is optional separating count from label.
	 * Intervals can be separated by whitespace or comma
	 * @param s the string
	 * @return The array of Interval Increments.
	 * @throws NoSuchObjectException on parse error.
	 */
	public static IntervalIncrement[] parseMult(String s)
		throws NoSuchObjectException
	{
		ArrayList<IntervalIncrement> aii = new ArrayList<IntervalIncrement>();
		Matcher iimatcher = iipattern.matcher(s);
		while(iimatcher.find())
		{
			if (iimatcher.groupCount() != 2)
			{
				String msg = "Invalid IntervalIncrement string '" + s + "'";
				Logger.instance().warning(msg);
				throw new NoSuchObjectException(msg);
			}
			String count = iimatcher.group(1);
			int icount = -1;
			try { icount = Integer.parseInt(count); }
			catch(NumberFormatException ex)
			{
				// Shouldn't happen, matcher already guarantees it is digits.
				String msg = "Bad count in Interval Increment String '"
					+ s + "': " + ex;
				Logger.instance().warning(msg);
				throw new NoSuchObjectException(msg);
			}
			int calConst = str2CalConst(iimatcher.group(2));
			if (calConst == -1)
			{
				String msg = "Bad time-interval string '"
					+ iimatcher.group(2) + "' in string '" + s + "'";
				Logger.instance().warning(msg);
				throw new NoSuchObjectException(msg);
			}
			aii.add(new IntervalIncrement(calConst, icount));
		}
		if (aii.size() == 0)
			throw new NoSuchObjectException("No valid intervals found in '" + s + "'");
		return aii.toArray(new IntervalIncrement[aii.size()]);
	}

	public String toString()
	{
		switch(calConstant)
		{
		case Calendar.YEAR: return "(" + count + " YR)";
		case Calendar.SECOND: return "(" + count + " SEC)";
		case Calendar.HOUR_OF_DAY: return "(" + count + " HR)";
		case Calendar.MINUTE: return "(" + count + " MIN)";
		case Calendar.DAY_OF_MONTH: return "(" + count + " DAY)";
		case Calendar.MONTH: return "(" + count + " MON)";
		default: return "(" + count + " calInc=" + calConstant + ")"; 
		}
	}
	/**
     * @return the calConstant
     */
    public int getCalConstant()
    {
    	return calConstant;
    }

	/**
     * @param calConstant the calConstant to set
     */
    public void setCalConstant(int calConstant)
    {
    	this.calConstant = calConstant;
    }

	/**
     * @return the count
     */
    public int getCount()
    {
    	return count;
    }

	/**
     * @param count the count to set
     */
    public void setCount(int count)
    {
    	this.count = count;
    }
    
    public boolean isLessThanDay()
    {
    	return calConstant == Calendar.HOUR_OF_DAY
    	 || calConstant == Calendar.SECOND
    	 || calConstant == Calendar.MINUTE;
    }
    
    public long toMsec()
    {
    	switch(calConstant)
    	{
    	case Calendar.MILLISECOND: return count;
    	case Calendar.SECOND: return count * 1000L;
    	case Calendar.MINUTE: return count * 60000L;
    	case Calendar.HOUR: 
    	case Calendar.HOUR_OF_DAY: 
    		return count * 3600000L;
    	case Calendar.DAY_OF_MONTH:
    	case Calendar.DAY_OF_WEEK:
    	case Calendar.DAY_OF_YEAR: return count * 3600000L * 24;
    	case Calendar.YEAR: return count * 3600000L * 24 * 365;
    	}

    	return count;
    }
    
    /**
     * Parse a multi-interval ISO 8601 period.
     * @param s
     * @return
     */
    public static IntervalIncrement[] parseIso8601(String s)
    {
		ArrayList<IntervalIncrement> aii = new ArrayList<IntervalIncrement>();
		
		boolean doingTime = false;
		int count = 0;
		for(int idx = 0; idx < s.length(); idx++)
		{
			char c = s.charAt(idx);
			if (Character.isDigit(c))
			{
				int start = idx++;
				while(idx < s.length() && Character.isDigit(s.charAt(idx)))
					idx++;
				count = Integer.parseInt(s.substring(start, idx));
				idx--;
			}
			else switch(c)
			{
			case 'P': break; // Start of Period indicator -- skip
			case 'Y':
				if (count != 0)
					aii.add(new IntervalIncrement(Calendar.YEAR, count));
				break;
			case 'M':
				if (count != 0)
					aii.add(new IntervalIncrement(doingTime ? Calendar.MINUTE : Calendar.MONTH, count));
				break;
			case 'W':
				if (count != 0)
					aii.add(new IntervalIncrement(Calendar.WEEK_OF_YEAR, count));
				break;
			case 'D':
				if (count != 0)
					aii.add(new IntervalIncrement(Calendar.DAY_OF_MONTH, count));
				break;
			case 'T':
				doingTime = true;
				break;
			case 'H':
				if (count != 0)
					aii.add(new IntervalIncrement(Calendar.HOUR_OF_DAY, count));
				break;
			case 'S':
				if (count != 0)
					aii.add(new IntervalIncrement(Calendar.SECOND, count));
				break;
			}
		}
		return aii.toArray(new IntervalIncrement[aii.size()]);
    }
    
    public static void main(String []args)
    {
    	while(true)
    	{
    		String s = System.console().readLine("%nEnter Interval Increment String: ");
    		try
    		{
    			IntervalIncrement iia[] = parseMult(s);
    			System.out.println("" + iia.length + " intervals found: ");
    			for(int i=0; i<iia.length; i++)
    				System.out.println("" + i + ": " + iia[i]);
    			
    			System.out.println("Parsed as single: " + parse(s));
    		}
    		catch(Exception ex)
    		{
    			System.out.println(ex);
    		}
    	}
    }
}
