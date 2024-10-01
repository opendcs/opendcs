/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/27 20:50:30  mjmaloney
*  javadocs
*
*  Revision 1.4  2003/06/09 15:44:22  mjmaloney
*  dev
*
*  Revision 1.3  2002/10/29 02:19:44  mjmaloney
*  Implemented StdmsgFormatter.
*
*  Revision 1.2  2001/10/15 00:02:51  mike
*  PlatformConfig Screen changes.
*
*  Revision 1.1  2001/09/03 13:24:13  mike
*  Added from Kinch archive.
*
*/
package decodes.util;

/**
Several DECODES modules manipulate a time-of-day as a number of seconds
since midnight. This class contains utility methods.
*/
public class TimeOfDay
{
	/** Number of seconds per day. */
	public final static int SECS_PER_DAY = (3600*24);

	/**
	  Converts second-of-day to HH:MM:SS string.
	  @param sec second of day
	  @return HH:MM:SS
	 */
	public static String seconds2hhmmss(int sec)
	{
		return seconds2hhmmss(sec, true);
	}

	/**
	  Converts second-of-day to a string in the form HHMMSS or HH:MM:SS.
	  @param sec second of day
	  @param useColons true if you want HH:MM:SS, false if you want HHMMSS.
	  @return formatted time of day
	*/
	public static String seconds2hhmmss(int sec, boolean useColons)
	{
		int h = sec / 3600;
		sec %= 3600;
		int m = sec / 60;
		sec %= 60;
		StringBuffer sb = new StringBuffer();
		if (h < 10)
			sb.append('0');
		else
			sb.append((char)((int)'0' + (h/10)));
		sb.append((char)((int)'0' + (h%10)));

		if (useColons)
			sb.append(':');

		if (m < 10)
			sb.append('0');
		else
			sb.append((char)((int)'0' + (m/10)));
		sb.append((char)((int)'0' + (m%10)));

		if (useColons)
			sb.append(':');

		if (sec < 10)
			sb.append('0');
		else
			sb.append((char)((int)'0' + (sec/10)));
		sb.append((char)((int)'0' + (sec%10)));

		return sb.toString();
	}

	/**
	  Converts string in the form HH:MM:SS to an integer second-of-day.
	  @param s String in the format HH:MM:SS
	  @return integer second-of-day
	  @throws NumberFormatException if input is improperly formatted.
	*/
	public static int hhmmss2seconds(String s)
		throws NumberFormatException
	{
		int r = 0;
		if (s.length() < 8)
			throw new NumberFormatException("Invalid time '" + s + "' (should be HH:MM:SS)");
		int h = Integer.parseInt(s.substring(0,2));
		int m = Integer.parseInt(s.substring(3,5));
		int sec = Integer.parseInt(s.substring(6,8));
		return h * 3600 + m * 60 + sec;
	}

	/**
	  Convert a duration (integer number of seconds) into a string containing
	  a number of hours, minutes, and seconds.
	  @param seconds duration number of seconds
	  @return duration String.
	*/
	public static String durationString(int seconds)
	{
		if (seconds % 3600 == 0)
		{
			int hr = seconds/3600;
			return "" + hr + " hour" + (hr>1 ? "s" : "");
		}
		else if (seconds % 60 == 0)
		{
			int min = seconds/60;
			return "" + min + " minute" + (min>1 ? "s" : "");
		}
		else
			return seconds2hhmmss(seconds);
	}
}
